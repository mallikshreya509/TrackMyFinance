package com.myfinance.track.ai;

import com.myfinance.track.ai.InsightRequest.DailyLine;
import com.myfinance.track.ai.InsightResult.Insight;
import com.myfinance.track.ai.InsightResult.Suggestion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Plain arithmetic on the same aggregates Gemini would get. Always works. */
@Component
public class RuleBasedInsightGenerator {

    public static final String MODEL = "rule-based";
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public InsightResult generate(InsightRequest r) {
        String cur = r.currency();

        if (r.expenseCount() == 0 || r.totalSpent().signum() == 0) {
            return new InsightResult(
                    "No expenses were recorded between " + r.from() + " and " + r.to() + ".",
                    List.of(),
                    List.of(new Suggestion("Start logging your expenses",
                            "Add expenses as you spend. After a few weeks you will see useful patterns.", null)),
                    MODEL);
        }

        List<Insight> insights = new ArrayList<>();
        List<Suggestion> suggestions = new ArrayList<>();
        BigDecimal change = r.changePercent();

        StringBuilder summary = new StringBuilder("You spent ")
                .append(money(cur, r.totalSpent())).append(" across ").append(r.expenseCount())
                .append(r.expenseCount() == 1 ? " expense" : " expenses")
                .append(" between ").append(r.from()).append(" and ").append(r.to()).append(".");
        if (change != null) {
            String abs = pct(change.abs());
            if (change.signum() > 0) {
                summary.append(" That is ").append(abs).append("% more than the previous period (")
                        .append(money(cur, r.previousPeriodTotal())).append(").");
            } else if (change.signum() < 0) {
                summary.append(" That is ").append(abs).append("% less than the previous period (")
                        .append(money(cur, r.previousPeriodTotal())).append(").");
            } else {
                summary.append(" That matches the previous period.");
            }
        }

        Optional<InsightRequest.CategoryLine> top =
                r.categories().stream().max(Comparator.comparing(InsightRequest.CategoryLine::total));
        top.ifPresent(c -> insights.add(new Insight(
                c.name() + " is your biggest category",
                c.name() + " took " + pct(c.percentage()) + "% (" + money(cur, c.total()) + ") of your spending.",
                c.percentage().compareTo(BigDecimal.valueOf(50)) >= 0 ? "WARNING" : "INFO")));

        if (change != null) {
            if (change.compareTo(BigDecimal.valueOf(20)) >= 0) {
                insights.add(new Insight("Spending is up " + pct(change) + "%",
                        "You spent more than in the previous period (" + money(cur, r.previousPeriodTotal()) + ").",
                        "WARNING"));
            } else if (change.compareTo(BigDecimal.valueOf(-10)) <= 0) {
                insights.add(new Insight("Spending is down " + pct(change.abs()) + "%",
                        "You spent less than in the previous period (" + money(cur, r.previousPeriodTotal()) + ").",
                        "POSITIVE"));
            } else {
                insights.add(new Insight("Spending is steady",
                        "Your total is close to the previous period (" + money(cur, r.previousPeriodTotal()) + ").",
                        "INFO"));
            }
        }

        if (r.dailyTotals().size() >= 3) {
            r.dailyTotals().stream().max(Comparator.comparing(DailyLine::total)).ifPresent(d -> insights.add(new Insight(
                    "Your biggest spending day was " + d.date(),
                    money(cur, d.total()) + " was spent that day, "
                            + share(d.total(), r.totalSpent()) + "% of the period's total.",
                    "INFO")));
        }

        if (r.dailyTotals().size() >= 4) {
            BigDecimal weekend = r.dailyTotals().stream()
                    .filter(d -> d.date().getDayOfWeek() == DayOfWeek.SATURDAY
                            || d.date().getDayOfWeek() == DayOfWeek.SUNDAY)
                    .map(DailyLine::total)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal weekendShare = new BigDecimal(share(weekend, r.totalSpent()));
            if (weekendShare.compareTo(BigDecimal.valueOf(40)) >= 0) {
                insights.add(new Insight("Weekends carry a lot of your spending",
                        weekendShare.toPlainString() + "% of your spending happened on Saturdays and Sundays.",
                        "INFO"));
            }
        }

        top.ifPresent(c -> {
            long days = ChronoUnit.DAYS.between(r.from(), r.to()) + 1;
            BigDecimal monthlySaving = c.total()
                    .multiply(new BigDecimal("0.10"))
                    .multiply(BigDecimal.valueOf(30))
                    .divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
            suggestions.add(new Suggestion("Set a limit for " + c.name(),
                    "Trimming " + c.name() + " by 10% would save about " + money(cur, monthlySaving) + " per month.",
                    monthlySaving));
        });
        if (change != null && change.compareTo(BigDecimal.valueOf(20)) >= 0) {
            suggestions.add(new Suggestion("Look at what changed",
                    "Spending rose " + pct(change) + "%. Compare category totals with the previous period to find what grew.",
                    null));
        }
        if (r.expenseCount() < 10) {
            suggestions.add(new Suggestion("Log expenses consistently",
                    "Fewer than 10 expenses were recorded, so these patterns may be misleading.", null));
        }

        return new InsightResult(summary.toString(), insights, suggestions, MODEL);
    }

    private static String money(String currency, BigDecimal v) {
        return currency + " " + v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String pct(BigDecimal v) {
        return v.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    private static String share(BigDecimal part, BigDecimal whole) {
        return part.multiply(HUNDRED).divide(whole, 1, RoundingMode.HALF_UP).toPlainString();
    }
}