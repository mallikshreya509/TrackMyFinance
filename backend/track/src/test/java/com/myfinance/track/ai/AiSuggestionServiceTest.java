package com.myfinance.track.ai;

import com.myfinance.track.ai.AiProviderException.Kind;
import com.myfinance.track.ai.AiSuggestionStore.StoredInsight;
import com.myfinance.track.ai.InsightResult.Insight;
import com.myfinance.track.dashboard.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiSuggestionServiceTest {

    private static final Long USER = 1L;
    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 30);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-13T10:00:00Z"));
    private final DashboardService dashboard = mock(DashboardService.class);
    private final AiInsightProvider provider = mock(AiInsightProvider.class);
    private final AiSuggestionStore store = mock(AiSuggestionStore.class);
    // cooldown 120s, generous global limits, 2 retries, 1ms backoff base so tests stay fast
    private final AiServiceProperties props =
            new AiServiceProperties("INR", 10, 120, 100, 1000, 2, 1, 10, 60);

    private AiSuggestionService service;

    @BeforeEach
    void setUp() {
        when(provider.modelName()).thenReturn("test-model");
        when(dashboard.summary(USER, FROM, TO)).thenReturn(TestData.sampleSummary(12));
        when(store.findByHash(eq(USER), anyString())).thenReturn(Optional.empty());
        when(store.findLatestForPeriod(USER, FROM, TO)).thenReturn(Optional.empty());
        when(store.save(any(), any(), any(), any(), any(), any())).thenAnswer(inv ->
                new StoredInsight(inv.getArgument(4), AiSource.GEMINI, clock.instant(), FROM, TO));

        service = new AiSuggestionService(dashboard, provider, store,
                new AiRateLimiter(clock, props), new RuleBasedInsightGenerator(),
                props, JsonMapper.builder().build(), clock);
    }

    private InsightResult geminiResult() {
        return new InsightResult("Gemini summary", List.of(new Insight("Title", "Detail", "INFO")),
                List.of(), "test-model");
    }

    private StoredInsight stored() {
        return new StoredInsight(geminiResult(), AiSource.GEMINI, clock.instant(), FROM, TO);
    }

    private InsightResponse suggest(boolean refresh) {
        return service.suggest(USER, FROM, TO, refresh);
    }

    @Test
    void tooFewExpensesSkipsGemini() {
        when(dashboard.summary(USER, FROM, TO)).thenReturn(TestData.sampleSummary(3));

        InsightResponse r = suggest(false);

        assertThat(r.source()).isEqualTo(InsightResponse.Source.FALLBACK);
        assertThat(r.notice()).contains("at least 10");
        verify(provider, never()).generate(any());
    }

    @Test
    void cacheHitMakesNoApiCall() {
        when(store.findByHash(eq(USER), anyString())).thenReturn(Optional.of(stored()));

        InsightResponse r = suggest(false);

        assertThat(r.source()).isEqualTo(InsightResponse.Source.CACHED);
        assertThat(r.stale()).isFalse();
        verify(provider, never()).generate(any());
    }

    @Test
    void freshSuccessIsStoredAndLabelledGemini() {
        when(provider.generate(any())).thenReturn(geminiResult());

        InsightResponse r = suggest(false);

        assertThat(r.source()).isEqualTo(InsightResponse.Source.GEMINI);
        assertThat(r.summary()).isEqualTo("Gemini summary");
        verify(store).save(eq(USER), eq(FROM), eq(TO), anyString(), any(), eq(AiSource.GEMINI));
    }

    @Test
    void refreshSkipsTheCache() {
        when(store.findByHash(eq(USER), anyString())).thenReturn(Optional.of(stored()));
        when(provider.generate(any())).thenReturn(geminiResult());

        InsightResponse r = suggest(true);

        assertThat(r.source()).isEqualTo(InsightResponse.Source.GEMINI);
        verify(store, never()).findByHash(any(), any());
    }

    @Test
    void cooldownBlocksASecondCallThenReleases() {
        when(provider.generate(any())).thenReturn(geminiResult());
        suggest(false);                                          // first call goes through

        when(store.findLatestForPeriod(USER, FROM, TO)).thenReturn(Optional.of(stored()));
        InsightResponse blocked = suggest(true);                 // straight away: blocked

        assertThat(blocked.source()).isEqualTo(InsightResponse.Source.CACHED);
        assertThat(blocked.stale()).isTrue();
        assertThat(blocked.retryAfterSeconds()).isEqualTo(120L);
        assertThat(blocked.notice()).contains("again in 2 minutes");
        verify(provider, times(1)).generate(any());

        clock.advance(Duration.ofMinutes(3));
        suggest(true);                                           // cooldown over
        verify(provider, times(2)).generate(any());
    }

    @Test
    void rateLimitedOnceThenSucceeds() {
        when(provider.generate(any()))
                .thenThrow(new AiProviderException(Kind.RATE_LIMITED, "429", Duration.ofMillis(1)))
                .thenReturn(geminiResult());

        InsightResponse r = suggest(false);

        assertThat(r.source()).isEqualTo(InsightResponse.Source.GEMINI);
        verify(provider, times(2)).generate(any());
    }

    @Test
    void persistentServerErrorsRetryThenFallBack() {
        when(provider.generate(any())).thenThrow(new AiProviderException(Kind.UNAVAILABLE, "503"));

        InsightResponse r = suggest(false);

        assertThat(r.source()).isEqualTo(InsightResponse.Source.FALLBACK);
        assertThat(r.notice()).contains("did not respond");
        verify(provider, times(3)).generate(any());              // 1 try + 2 retries
    }

    @Test
    void configErrorsAreNotRetried() {
        when(provider.generate(any())).thenThrow(new AiProviderException(Kind.CONFIG, "403"));

        InsightResponse r = suggest(false);

        assertThat(r.source()).isEqualTo(InsightResponse.Source.FALLBACK);
        assertThat(r.notice()).contains("not set up correctly");
        verify(provider, times(1)).generate(any());
    }

    @Test
    void badAnswerIsNotRetriedAndPrefersTheStaleResult() {
        when(provider.generate(any())).thenThrow(new AiProviderException(Kind.BAD_RESPONSE, "junk"));
        when(store.findLatestForPeriod(USER, FROM, TO)).thenReturn(Optional.of(stored()));

        InsightResponse r = suggest(false);

        assertThat(r.source()).isEqualTo(InsightResponse.Source.CACHED);
        assertThat(r.stale()).isTrue();
        verify(provider, times(1)).generate(any());
    }

    @Test
    void aLongRetryAfterDegradesImmediately() {
        when(provider.generate(any()))
                .thenThrow(new AiProviderException(Kind.RATE_LIMITED, "429", Duration.ofSeconds(60)));

        InsightResponse r = suggest(false);

        assertThat(r.source()).isEqualTo(InsightResponse.Source.FALLBACK);
        assertThat(r.retryAfterSeconds()).isEqualTo(60L);
        verify(provider, times(1)).generate(any());
    }

    @Test
    void latestIsEmptyWhenNothingIsStored() {
        assertThat(service.latest(USER)).isEmpty();
    }
}