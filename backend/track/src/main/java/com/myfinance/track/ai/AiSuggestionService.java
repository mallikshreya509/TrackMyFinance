package com.myfinance.track.ai;

import com.myfinance.track.ai.AiProviderException.Kind;
import com.myfinance.track.ai.AiSuggestionStore.StoredInsight;
import com.myfinance.track.dashboard.DashboardService;
import com.myfinance.track.dashboard.DashboardSummaryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Orchestration only. It never talks to Gemini directly (that's the AiInsightProvider's job).
 * Deliberately NOT @Transactional: no database connection is held while waiting for the AI.
 */
@Service
public class AiSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(AiSuggestionService.class);

    private final DashboardService dashboard;
    private final AiInsightProvider provider;
    private final AiSuggestionStore store;
    private final AiRateLimiter limiter;
    private final RuleBasedInsightGenerator fallbackGenerator;
    private final AiServiceProperties props;
    private final JsonMapper mapper;
    private final Clock clock;

    public AiSuggestionService(DashboardService dashboard, AiInsightProvider provider,
                               AiSuggestionStore store, AiRateLimiter limiter,
                               RuleBasedInsightGenerator fallbackGenerator,
                               AiServiceProperties props, JsonMapper mapper, Clock clock) {
        this.dashboard = dashboard;
        this.provider = provider;
        this.store = store;
        this.limiter = limiter;
        this.fallbackGenerator = fallbackGenerator;
        this.props = props;
        this.mapper = mapper;
        this.clock = clock;
    }

    public InsightResponse suggest(Long userId, LocalDate from, LocalDate to, boolean refresh) {
        DashboardSummaryDto summary = dashboard.summary(userId, from, to);   // validates the dates too
        InsightRequest request = toRequest(summary);

        if (summary.expenseCount() < props.minExpenses()) {
            return fallback(request, "Add at least " + props.minExpenses()
                    + " expenses in this period to get AI suggestions. Showing basic tips instead.", null);
        }

        String hash = dataHash(request);
        if (!refresh) {
            Optional<StoredInsight> cached = store.findByHash(userId, hash);
            if (cached.isPresent()) {
                return response(InsightResponse.Source.CACHED, cached.get(), false, null, null);
            }
        }

        Duration wait = limiter.tryStartUserAttempt(userId);
        if (!wait.isZero()) {
            return degrade(userId, request,
                    "New AI suggestions can be requested again in " + describe(wait) + ".", wait);
        }

        try {
            InsightResult result = callWithRetries(userId, request);
            // Only one provider exists today, so its results are labelled GEMINI in the database.
            StoredInsight saved = store.save(userId, request.from(), request.to(), hash, result, AiSource.GEMINI);
            return response(InsightResponse.Source.GEMINI, saved, false, null, null);
        } catch (AiProviderException e) {
            log.warn("AI suggestions degraded for user {}: {} ({})", userId, e.kind(), e.getMessage());
            return degrade(userId, request, noticeFor(e), e.retryAfter());
        }
    }

    /** The last stored result, with no API call at all. */
    public Optional<InsightResponse> latest(Long userId) {
        return store.findLatest(userId)
                .map(s -> response(InsightResponse.Source.CACHED, s, false, null, null));
    }

    // ---------- calling the provider ----------

    private InsightResult callWithRetries(Long userId, InsightRequest request) {
        Instant deadline = clock.instant().plusSeconds(props.totalBudgetSeconds());
        int attempt = 0;
        while (true) {
            Duration globalWait = limiter.tryAcquireGlobal();
            if (!globalWait.isZero()) {
                if (attempt == 0) limiter.cancelUserAttempt(userId);   // no API call happened
                throw new AiProviderException(Kind.RATE_LIMITED, "App-side request limit reached", globalWait);
            }
            try {
                return provider.generate(request);
            } catch (AiProviderException e) {
                Duration delay = retryDelay(e, attempt);
                if (delay == null || clock.instant().plus(delay).isAfter(deadline)) {
                    throw e;
                }
                log.info("Retrying the AI call (attempt {}) in {} ms", attempt + 2, delay.toMillis());
                sleep(delay, e);
                attempt++;
            }
        }
    }

    /** null = do not retry. */
    private Duration retryDelay(AiProviderException e, int attempt) {
        boolean transientFailure = e.kind() == Kind.RATE_LIMITED || e.kind() == Kind.UNAVAILABLE;
        if (!transientFailure || attempt >= props.maxRetries()) return null;

        Duration delay = e.retryAfter() != null
                ? e.retryAfter()
                : Duration.ofMillis(props.retryBaseMillis() * (1L << attempt)
                        + ThreadLocalRandom.current().nextLong(0, Math.max(1, props.retryBaseMillis() / 2)));
        // If the server wants a long wait, don't hold the user's request open: degrade now.
        return delay.compareTo(Duration.ofSeconds(props.maxRetryWaitSeconds())) > 0 ? null : delay;
    }

    private static void sleep(Duration d, AiProviderException original) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw original;
        }
    }

    // ---------- degrading gracefully ----------

    private InsightResponse degrade(Long userId, InsightRequest request, String reason, Duration retry) {
        Long retrySeconds = retry == null ? null : ceilSeconds(retry);
        Optional<StoredInsight> stale = store.findLatestForPeriod(userId, request.from(), request.to());
        if (stale.isPresent()) {
            return response(InsightResponse.Source.CACHED, stale.get(), true,
                    reason + " Showing your last AI suggestions for this period.", retrySeconds);
        }
        return fallback(request, reason + " Showing basic tips instead.", retrySeconds);
    }

    private InsightResponse fallback(InsightRequest request, String notice, Long retrySeconds) {
        InsightResult r = fallbackGenerator.generate(request);
        return new InsightResponse(InsightResponse.Source.FALLBACK, false, clock.instant(),
                request.from(), request.to(), r.summary(), r.insights(), r.suggestions(),
                notice, retrySeconds, InsightResponse.DISCLAIMER);
    }

    private InsightResponse response(InsightResponse.Source source, StoredInsight s, boolean stale,
                                     String notice, Long retrySeconds) {
        InsightResult r = s.result();
        return new InsightResponse(source, stale, s.createdAt(), s.from(), s.to(), r.summary(),
                r.insights() == null ? List.of() : r.insights(),
                r.suggestions() == null ? List.of() : r.suggestions(),
                notice, retrySeconds, InsightResponse.DISCLAIMER);
    }

    /** Plain-language reasons. The technical details stay in the server log. */
    private static String noticeFor(AiProviderException e) {
        return switch (e.kind()) {
            case RATE_LIMITED -> "The AI service is busy or its free quota is used up for now.";
            case UNAVAILABLE -> "The AI service did not respond. Please try again later.";
            case BAD_RESPONSE -> "The AI service returned an unusable answer.";
            case CONFIG -> "AI suggestions are not set up correctly on the server.";
        };
    }

    // ---------- helpers ----------

    private InsightRequest toRequest(DashboardSummaryDto s) {
        return new InsightRequest(
                s.period().from(), s.period().to(), props.currency(),
                s.totalSpent(), s.expenseCount(), s.previousPeriodTotal(), s.changePercent(),
                s.byCategory().stream()
                        .map(c -> new InsightRequest.CategoryLine(c.name(), c.total(), c.percentage())).toList(),
                s.dailyTotals().stream()
                        .map(d -> new InsightRequest.DailyLine(d.date(), d.total())).toList());
    }

    /** Same data + same model + same prompt version = same hash = reuse the stored answer. */
    private String dataHash(InsightRequest request) {
        try {
            String canonical = mapper.writeValueAsString(request)
                    + "|" + provider.modelName() + "|" + InsightPrompt.VERSION;
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static long ceilSeconds(Duration d) {
        return (d.toMillis() + 999) / 1000;
    }

    private static String describe(Duration d) {
        long s = ceilSeconds(d);
        return s < 120 ? s + " seconds" : ((s + 59) / 60) + " minutes";
    }
}