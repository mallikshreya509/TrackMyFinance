package com.myfinance.track.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InsightResult(String summary, List<Insight> insights,
                            List<Suggestion> suggestions, String model) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Insight(String title, String detail, String severity) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Suggestion(String title, String detail, BigDecimal estimatedMonthlySaving) {}
}