package com.myfinance.track.ai;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/** In-memory limits: a per-user cooldown and a global cap kept below the free-tier quota. */
@Component
public class AiRateLimiter {

    private final Clock clock;
    private final AiServiceProperties props;
    private final ConcurrentHashMap<Long, Instant> lastUserAttempt = new ConcurrentHashMap<>();
    private final Deque<Instant> recentCalls = new ArrayDeque<>();   // guarded by "this"
    private LocalDate day;                                           // guarded by "this"
    private int callsToday;                                          // guarded by "this"

    public AiRateLimiter(Clock clock, AiServiceProperties props) {
        this.clock = clock;
        this.props = props;
    }

    /** ZERO = allowed (and recorded). Otherwise, how long until this user may try again. */
    public Duration tryStartUserAttempt(Long userId) {
        Instant now = clock.instant();
        Duration cooldown = Duration.ofSeconds(props.cooldownSeconds());
        AtomicReference<Duration> wait = new AtomicReference<>(Duration.ZERO);
        lastUserAttempt.compute(userId, (id, last) -> {       // atomic check-and-set
            if (last != null) {
                Duration elapsed = Duration.between(last, now);
                if (elapsed.compareTo(cooldown) < 0) {
                    wait.set(cooldown.minus(elapsed));
                    return last;
                }
            }
            return now;
        });
        return wait.get();
    }

    /** Used when we decided not to call the API after all, so the user isn't penalised. */
    public void cancelUserAttempt(Long userId) {
        lastUserAttempt.remove(userId);
    }

    /** ZERO = allowed (and recorded). Otherwise, how long until the global budget frees up. */
    public synchronized Duration tryAcquireGlobal() {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, clock.getZone());
        if (!today.equals(day)) {
            day = today;
            callsToday = 0;
        }
        if (callsToday >= props.maxRequestsPerDay()) {
            Instant midnight = today.plusDays(1).atStartOfDay(clock.getZone()).toInstant();
            return Duration.between(now, midnight);
        }

        Instant windowStart = now.minusSeconds(60);
        while (!recentCalls.isEmpty() && !recentCalls.peekFirst().isAfter(windowStart)) {
            recentCalls.pollFirst();
        }
        if (recentCalls.size() >= props.maxRequestsPerMinute()) {
            Instant oldest = recentCalls.peekFirst();
            return oldest == null ? Duration.ofSeconds(60) : Duration.between(now, oldest.plusSeconds(60));
        }
        recentCalls.addLast(now);
        callsToday++;
        return Duration.ZERO;
    }
}