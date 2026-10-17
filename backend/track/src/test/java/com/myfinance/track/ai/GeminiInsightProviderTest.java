package com.myfinance.track.ai;

import com.myfinance.track.ai.AiProviderException.Kind;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiInsightProviderTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private HttpServer server;
    private volatile int status = 200;
    private volatile String responseBody = "";
    private volatile String retryAfterHeader = null;
    private volatile long delayMillis = 0;
    private volatile String lastPath;
    private volatile String lastKeyHeader;
    private volatile String lastBody;
    private final AtomicInteger calls = new AtomicInteger();

    @BeforeEach
    void startFakeGemini() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            lastPath = exchange.getRequestURI().toString();
            lastKeyHeader = exchange.getRequestHeaders().getFirst("x-goog-api-key");
            lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (delayMillis > 0) {
                try { Thread.sleep(delayMillis); } catch (InterruptedException ignored) { /* test shutdown */ }
            }
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            if (retryAfterHeader != null) exchange.getResponseHeaders().add("Retry-After", retryAfterHeader);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        });
        server.start();
    }

    @AfterEach
    void stopFakeGemini() {
        server.stop(0);
    }

    // ---------- helpers ----------

    private GeminiInsightProvider provider(String apiKey, int timeoutSeconds) {
        AiProperties props = new AiProperties(apiKey, "test-model",
                "http://127.0.0.1:" + server.getAddress().getPort(), timeoutSeconds, 0.3, 1024);
        return new GeminiInsightProvider(props, MAPPER);
    }

    private String geminiReply(String modelText, String finishReason) {
        return MAPPER.writeValueAsString(Map.of("candidates", List.of(Map.of(
                "finishReason", finishReason,
                "content", Map.of("parts", List.of(Map.of("text", modelText)))))));
    }

    private String goodModelJson(String severity) {
        return MAPPER.writeValueAsString(Map.of(
                "summary", "Spending is up 25% versus last period, driven by Food.",
                "insights", List.of(Map.of("title", "Food is your top category",
                        "detail", "Food is 75% of total spending.", "severity", severity)),
                "suggestions", List.of(Map.of("title", "Cap eating out",
                        "detail", "Set a weekly limit for Food.", "estimatedMonthlySaving", 500))));
    }

    private AiProviderException failure(GeminiInsightProvider p) {
        try {
            p.generate(TestData.sampleRequest());
        } catch (AiProviderException e) {
            return e;
        }
        throw new AssertionError("Expected an AiProviderException");
    }

    // ---------- tests ----------

    @Test
    void successfulCallReturnsParsedResult() {
        responseBody = geminiReply(goodModelJson("INFO"), "STOP");

        InsightResult result = provider("test-key", 5).generate(TestData.sampleRequest());

        assertThat(result.summary()).contains("25%");
        assertThat(result.insights()).hasSize(1);
        assertThat(result.insights().get(0).severity()).isEqualTo("INFO");
        assertThat(result.suggestions()).hasSize(1);
        assertThat(result.suggestions().get(0).estimatedMonthlySaving()).isEqualByComparingTo("500");
        assertThat(result.model()).isEqualTo("test-model");
    }

    @Test
    void keyGoesInHeaderNotUrl_andOnlyAggregatesAreSent() {
        responseBody = geminiReply(goodModelJson("INFO"), "STOP");

        provider("test-key", 5).generate(TestData.sampleRequest());

        assertThat(lastPath).isEqualTo("/v1beta/models/test-model:generateContent");
        assertThat(lastPath).doesNotContain("key");
        assertThat(lastKeyHeader).isEqualTo("test-key");
        assertThat(lastBody).contains("totalSpent").contains("responseSchema");
        assertThat(lastBody).doesNotContain("test-key");   // the key must never be in the body either
    }

    @Test
    void unknownSeverityBecomesInfo() {
        responseBody = geminiReply(goodModelJson("CRITICAL"), "STOP");

        InsightResult result = provider("test-key", 5).generate(TestData.sampleRequest());

        assertThat(result.insights().get(0).severity()).isEqualTo("INFO");
    }

    @Test
    void malformedModelJsonIsBadResponse() {
        responseBody = geminiReply("this is not json {{{", "STOP");

        assertThat(failure(provider("test-key", 5)).kind()).isEqualTo(Kind.BAD_RESPONSE);
    }

    @Test
    void truncatedAnswerIsBadResponse() {
        responseBody = geminiReply("{\"summary\":\"cut off", "MAX_TOKENS");

        assertThat(failure(provider("test-key", 5)).kind()).isEqualTo(Kind.BAD_RESPONSE);
    }

    @Test
    void blockedPromptIsBadResponse() {
        responseBody = MAPPER.writeValueAsString(Map.of("promptFeedback", Map.of("blockReason", "SAFETY")));

        assertThat(failure(provider("test-key", 5)).kind()).isEqualTo(Kind.BAD_RESPONSE);
    }

    @Test
    void http429IsRateLimitedAndReadsRetryAfter() {
        status = 429;
        retryAfterHeader = "7";
        responseBody = "{\"error\":{\"message\":\"quota\"}}";

        AiProviderException e = failure(provider("test-key", 5));

        assertThat(e.kind()).isEqualTo(Kind.RATE_LIMITED);
        assertThat(e.retryAfter()).isEqualTo(Duration.ofSeconds(7));
    }

    @Test
    void http500IsUnavailable() {
        status = 500;
        responseBody = "{\"error\":{\"message\":\"boom\"}}";

        assertThat(failure(provider("test-key", 5)).kind()).isEqualTo(Kind.UNAVAILABLE);
    }

    @Test
    void http403IsConfigError() {
        status = 403;
        responseBody = "{\"error\":{\"message\":\"API key not valid\"}}";

        assertThat(failure(provider("test-key", 5)).kind()).isEqualTo(Kind.CONFIG);
    }

    @Test
    void blankKeyFailsBeforeAnyNetworkCall() {
        AiProviderException e = failure(provider("", 5));

        assertThat(e.kind()).isEqualTo(Kind.CONFIG);
        assertThat(calls.get()).isZero();
    }

    @Test
    void slowServerTimesOutAsUnavailable() {
        delayMillis = 3000;
        responseBody = geminiReply(goodModelJson("INFO"), "STOP");

        assertThat(failure(provider("test-key", 1)).kind()).isEqualTo(Kind.UNAVAILABLE);
    }
}