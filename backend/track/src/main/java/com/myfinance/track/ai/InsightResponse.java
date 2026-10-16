package com.myfinance.track.ai;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record InsightResponse(
        Source source,
        boolean stale,
        Instant generatedAt,
        LocalDate from,
        LocalDate to,
        String summary,
        List<InsightResult.Insight> insights,
        List<InsightResult.Suggestion> suggestions,
        String notice,               // plain-language explanation when we degraded; null otherwise
        Long retryAfterSeconds,      // when a fresh generation will be allowed again; null otherwise
        String disclaimer) {

    public enum Source { GEMINI, CACHED, FALLBACK }

    public static final String DISCLAIMER = "General guidance, not professional financial advice.";
}