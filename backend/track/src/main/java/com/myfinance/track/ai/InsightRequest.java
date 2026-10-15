package com.myfinance.track.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Aggregated spending data only. Nothing personal belongs in here. */
public record InsightRequest(
        LocalDate from,
        LocalDate to,
        String currency,
        BigDecimal totalSpent,
        long expenseCount,
        BigDecimal previousPeriodTotal,
        BigDecimal changePercent,          // null when there was no spending in the previous period
        List<CategoryLine> categories,
        List<DailyLine> dailyTotals) {

    public record CategoryLine(String name, BigDecimal total, BigDecimal percentage) {}

    public record DailyLine(LocalDate date, BigDecimal total) {}
}