package com.myfinance.track.ai;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedInsightGeneratorTest {

    private final RuleBasedInsightGenerator generator = new RuleBasedInsightGenerator();

    @Test
    void summarisesTotalAndChange() {
        InsightResult r = generator.generate(TestData.sampleRequest());

        assertThat(r.summary()).contains("INR 200.00").contains("25.0% more");
        assertThat(r.model()).isEqualTo("rule-based");
    }

    @Test
    void flagsADominantCategory() {
        InsightResult r = generator.generate(TestData.sampleRequest());

        assertThat(r.insights()).anySatisfy(i -> {
            assertThat(i.title()).isEqualTo("Food is your biggest category");
            assertThat(i.severity()).isEqualTo("WARNING");
            assertThat(i.detail()).contains("75.0%");
        });
    }

    @Test
    void estimatesMonthlySavingFromTheTopCategory() {
        InsightResult r = generator.generate(TestData.sampleRequest());

        // Food 150.00 over 30 days: 10% of it, scaled to a 30-day month = 15.00
        assertThat(r.suggestions().get(0).estimatedMonthlySaving()).isEqualByComparingTo("15.00");
    }

    @Test
    void handlesAPeriodWithNoSpending() {
        InsightRequest empty = new InsightRequest(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "INR",
                BigDecimal.ZERO, 0, BigDecimal.ZERO, null, List.of(), List.of());

        InsightResult r = generator.generate(empty);

        assertThat(r.summary()).contains("No expenses");
        assertThat(r.insights()).isEmpty();
        assertThat(r.suggestions()).hasSize(1);
    }
}