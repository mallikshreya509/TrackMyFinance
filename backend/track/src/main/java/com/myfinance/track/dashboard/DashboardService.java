package com.myfinance.track.dashboard;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class DashboardService {

    private static final long MAX_DAYS = 366;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final DashboardRepository repo;

    public DashboardService(DashboardRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDto summary(Long userId, LocalDate from, LocalDate to) {
        if ((from == null) != (to == null)) {
            throw bad("Provide both 'from' and 'to', or neither");
        }
        LocalDate start = from;
        LocalDate end = to;
        if (start == null) {                                  // default: current month
            LocalDate today = LocalDate.now();
            start = today.withDayOfMonth(1);
            end = today.withDayOfMonth(today.lengthOfMonth());
        }
        if (start.isAfter(end)) {
            throw bad("'from' must not be after 'to'");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > MAX_DAYS) {
            throw bad("Date range must not exceed " + MAX_DAYS + " days");
        }

        // The comparison period: same length, ending the day before this one starts
        LocalDate prevEnd = start.minusDays(1);
        LocalDate prevStart = prevEnd.minusDays(days - 1);

        BigDecimal total = zeroIfNull(repo.sumAmount(userId, start, end));
        long count = repo.countExpenses(userId, start, end);
        BigDecimal previous = zeroIfNull(repo.sumAmount(userId, prevStart, prevEnd));

        BigDecimal change = previous.signum() == 0
                ? null
                : total.subtract(previous).multiply(HUNDRED).divide(previous, 1, RoundingMode.HALF_UP);

        List<DashboardSummaryDto.CategoryShare> byCategory =
                repo.totalsByCategory(userId, start, end).stream()
                        .map(t -> new DashboardSummaryDto.CategoryShare(
                                t.categoryId(), t.name(), t.color(), t.total(), percentOf(t.total(), total)))
                        .toList();

        return new DashboardSummaryDto(
                new DashboardSummaryDto.Period(start, end),
                total, count, previous, change, byCategory,
                repo.totalsByDay(userId, start, end));
    }

    private static BigDecimal percentOf(BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) return BigDecimal.ZERO;
        return part.multiply(HUNDRED).divide(whole, 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal zeroIfNull(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}