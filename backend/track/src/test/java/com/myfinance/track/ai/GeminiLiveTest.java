package com.myfinance.track.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** Calls the real Gemini API with made-up data. Skipped unless GEMINI_API_KEY is in the environment. */
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class GeminiLiveTest {

    @Test
    void realCallReturnsUsableInsights() {
        String model = System.getenv("GEMINI_MODEL");
        if (model == null || model.isBlank()) model = "gemini-3.1-flash-lite";

        AiProperties props = new AiProperties(System.getenv("GEMINI_API_KEY"), model,
                "https://generativelanguage.googleapis.com", 60, 0.3, 4096);
        InsightResult result = new GeminiInsightProvider(props, JsonMapper.builder().build())
                .generate(TestData.sampleRequest());

        System.out.println("MODEL:   " + result.model());
        System.out.println("SUMMARY: " + result.summary());
        result.insights().forEach(i -> System.out.println("INSIGHT [" + i.severity() + "] " + i.title() + " - " + i.detail()));
        result.suggestions().forEach(s -> System.out.println("SUGGESTION " + s.title() + " (saving: " + s.estimatedMonthlySaving() + ")"));

        assertThat(result.summary()).isNotBlank();
        assertThat(result.insights()).isNotEmpty();
    }
}