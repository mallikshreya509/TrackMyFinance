package com.myfinance.track.ai;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AiRateLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-13T10:00:00Z"));

    private AiRateLimiter limiter(int perMinute, int perDay, int cooldownSeconds) {
        return new AiRateLimiter(clock,
                new AiServiceProperties("INR", 10, cooldownSeconds, perMinute, perDay, 2, 1, 10, 60));
    }

    @Test
    void userCooldownBlocksThenReleases() {
        AiRateLimiter l = limiter(10, 100, 120);
        assertThat(l.tryStartUserAttempt(1L)).isEqualTo(Duration.ZERO);

        clock.advance(Duration.ofSeconds(30));
        assertThat(l.tryStartUserAttempt(1L)).isEqualTo(Duration.ofSeconds(90));
        assertThat(l.tryStartUserAttempt(2L)).isEqualTo(Duration.ZERO);   // another user is unaffected

        clock.advance(Duration.ofSeconds(90));
        assertThat(l.tryStartUserAttempt(1L)).isEqualTo(Duration.ZERO);
    }

    @Test
    void cancellingFreesTheUser() {
        AiRateLimiter l = limiter(10, 100, 120);
        assertThat(l.tryStartUserAttempt(1L)).isEqualTo(Duration.ZERO);
        l.cancelUserAttempt(1L);
        assertThat(l.tryStartUserAttempt(1L)).isEqualTo(Duration.ZERO);
    }

    @Test
    void perMinuteLimitBlocksThenReleases() {
        AiRateLimiter l = limiter(2, 100, 0);
        assertThat(l.tryAcquireGlobal()).isEqualTo(Duration.ZERO);
        assertThat(l.tryAcquireGlobal()).isEqualTo(Duration.ZERO);
        assertThat(l.tryAcquireGlobal()).isEqualTo(Duration.ofSeconds(60));

        clock.advance(Duration.ofSeconds(61));
        assertThat(l.tryAcquireGlobal()).isEqualTo(Duration.ZERO);
    }

    @Test
    void perDayLimitBlocksUntilTomorrow() {
        AiRateLimiter l = limiter(100, 2, 0);
        assertThat(l.tryAcquireGlobal()).isEqualTo(Duration.ZERO);
        assertThat(l.tryAcquireGlobal()).isEqualTo(Duration.ZERO);
        assertThat(l.tryAcquireGlobal()).isPositive();

        clock.advance(Duration.ofHours(24));
        assertThat(l.tryAcquireGlobal()).isEqualTo(Duration.ZERO);
    }
}