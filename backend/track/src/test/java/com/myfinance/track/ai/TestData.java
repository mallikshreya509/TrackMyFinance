package com.myfinance.track.ai;

import com.myfinance.track.ai.InsightRequest.CategoryLine;
import com.myfinance.track.ai.InsightRequest.DailyLine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.myfinance.track.dashboard.DailyTotal;
import com.myfinance.track.dashboard.DashboardSummaryDto;

import com.myfinance.track.ai.InsightRequest;

/** Made-up numbers only. Never put real expenses in tests. */
final class TestData {

    private TestData() {}

    static InsightRequest sampleRequest() {
        return new InsightRequest(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), "INR",
                new BigDecimal("200.00"), 3,
                new BigDecimal("160.00"), new BigDecimal("25.0"),
                List.of(new CategoryLine("Food", new BigDecimal("150.00"), new BigDecimal("75.0")),
                        new CategoryLine("Transport", new BigDecimal("50.00"), new BigDecimal("25.0"))),
                List.of(new DailyLine(LocalDate.of(2026, 9, 5), new BigDecimal("150.00")),
                        new DailyLine(LocalDate.of(2026, 9, 10), new BigDecimal("50.00"))));
    }
    
    static DashboardSummaryDto sampleSummary(long expenseCount) {
    return new DashboardSummaryDto(
            new DashboardSummaryDto.Period(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)),
            new BigDecimal("200.00"), expenseCount,
            new BigDecimal("160.00"), new BigDecimal("25.0"),
            List.of(new DashboardSummaryDto.CategoryShare(1L, "Food", "#f59e0b",
                            new BigDecimal("150.00"), new BigDecimal("75.0")),
                    new DashboardSummaryDto.CategoryShare(2L, "Transport", "#3b82f6",
                            new BigDecimal("50.00"), new BigDecimal("25.0"))),
            List.of(new DailyTotal(LocalDate.of(2026, 9, 5), new BigDecimal("150.00")),
                    new DailyTotal(LocalDate.of(2026, 9, 10), new BigDecimal("50.00"))));
}
}