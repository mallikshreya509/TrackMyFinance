package com.myfinance.track.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiServiceProperties(
        String currency,
        int minExpenses,
        int cooldownSeconds,
        int maxRequestsPerMinute,
        int maxRequestsPerDay,
        int maxRetries,
        long retryBaseMillis,
        int maxRetryWaitSeconds,
        int totalBudgetSeconds) {}