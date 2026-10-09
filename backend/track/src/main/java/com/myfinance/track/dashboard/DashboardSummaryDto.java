package com.myfinance.track.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardSummaryDto(
        Period period,
        BigDecimal totalSpent,
        long expenseCount,
        BigDecimal previousPeriodTotal,
        BigDecimal changePercent,          // null when the previous period had no spending
        List<CategoryShare> byCategory,
        List<DailyTotal> dailyTotals) {

    public record Period(LocalDate from, LocalDate to) {}

    public record CategoryShare(Long categoryId, String name, String color,
                                BigDecimal total, BigDecimal percentage) {}
}