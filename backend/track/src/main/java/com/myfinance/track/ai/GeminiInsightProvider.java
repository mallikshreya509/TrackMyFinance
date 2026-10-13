
package com.myfinance.track.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.myfinance.track.ai.AiProviderException.Kind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** The only class in the app that knows Gemini's URL, auth header and response format. */
public class GeminiInsightProvider implements AiInsightProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiInsightProvider.class);
    private static final Pattern SAFE_MODEL = Pattern.compile("[A-Za-z0-9._-]+");
    private static final Set<String> SEVERITIES = Set.of("INFO", "WARNING", "POSITIVE");
    private static final int MAX_ITEMS = 5;

    private final AiProperties props;
    private final JsonMapper mapper;
    private final HttpClient http;

    public GeminiInsightProvider(AiProperties props, JsonMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public String modelName() {
        return props.model();
    }

    @Override
    public InsightResult generate(InsightRequest request) {
        requireConfigured();
        HttpResponse<String> response = send(request);
        int status = response.statusCode();

        if (status == 200) {
            return parse(response.body());
        }
        String detail = abbreviate(response.body());
        if (status == 429) {
            log.warn("Gemini rate limit or quota reached (HTTP 429)");
            throw new AiProviderException(Kind.RATE_LIMITED,
                    "Gemini rate limit or quota reached", retryAfter(response));
        }
        if (status >= 500) {
            log.warn("Gemini server error (HTTP {}): {}", status, detail);
            throw new AiProviderException(Kind.UNAVAILABLE, "Gemini is unavailable (HTTP " + status + ")");
        }
        // 400/401/403/404 mean OUR configuration is wrong. Retrying cannot help, so be loud.
        log.error("Gemini rejected the request (HTTP {}). Check GEMINI_API_KEY and GEMINI_MODEL. {}",
                status, detail);
        throw new AiProviderException(Kind.CONFIG, "Gemini rejected the request (HTTP " + status + ")");
    }

    // ---------- request ----------

    private void requireConfigured() {
        if (isBlank(props.apiKey())) {
            throw new AiProviderException(Kind.CONFIG, "GEMINI_API_KEY is not set");
        }
        if (isBlank(props.baseUrl())) {
            throw new AiProviderException(Kind.CONFIG, "Gemini base URL is not set");
        }
        if (isBlank(props.model()) || !SAFE_MODEL.matcher(props.model()).matches()) {
            throw new AiProviderException(Kind.CONFIG, "GEMINI_MODEL is missing or has invalid characters");
        }
    }

    private Map<String, Object> buildBody(InsightRequest request) {
        String data = mapper.writeValueAsString(InsightPrompt.userPayload(request));
        return Map.of(
                "systemInstruction", Map.of("parts", List.of(
                        Map.of("text", InsightPrompt.systemInstruction(request.currency())))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(
                        Map.of("text", "Spending data:\n" + data)))),
                "generationConfig", Map.of(
                        "temperature", props.temperature(),
                        "maxOutputTokens", props.maxOutputTokens(),
                        "responseMimeType", "application/json",
                        "responseSchema", InsightPrompt.responseSchema()));
    }

    private URI endpoint() {
        String base = props.baseUrl().replaceAll("/+$", "");
        return URI.create(base + "/v1beta/models/" + props.model() + ":generateContent");
    }

    private HttpResponse<String> send(InsightRequest request) {
        String json;
        try {
            json = mapper.writeValueAsString(buildBody(request));
        } catch (RuntimeException e) {
            throw new AiProviderException(Kind.CONFIG, "Could not build the Gemini request", e);
        }

        HttpRequest httpRequest = HttpRequest.newBuilder(endpoint())
                .timeout(Duration.ofSeconds(props.timeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", props.apiKey())     // header, never in the URL
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        try {
            return http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {   // includes timeouts
            log.warn("Gemini request failed: {}", e.getClass().getSimpleName());
            throw new AiProviderException(Kind.UNAVAILABLE,
                    "Could not reach Gemini (" + e.getClass().getSimpleName() + ")", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiProviderException(Kind.UNAVAILABLE, "Interrupted while calling Gemini", e);
        }
    }

    // ---------- response ----------

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeminiResponse(List<Candidate> candidates, PromptFeedback promptFeedback) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(Content content, String finishReason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Content(List<Part> parts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Part(String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PromptFeedback(String blockReason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ModelOutput(String summary, List<InsightResult.Insight> insights,
                       List<InsightResult.Suggestion> suggestions) {}

    private InsightResult parse(String body) {
        GeminiResponse response;
        try {
            response = mapper.readValue(body, GeminiResponse.class);
        } catch (RuntimeException e) {
            throw bad("Gemini returned a reply that is not valid JSON", e);
        }
        if (response == null) {
            throw bad("Gemini returned an empty reply", null);
        }
        if (response.promptFeedback() != null && !isBlank(response.promptFeedback().blockReason())) {
            throw bad("Gemini blocked the request (" + response.promptFeedback().blockReason() + ")", null);
        }
        if (response.candidates() == null || response.candidates().isEmpty()) {
            throw bad("Gemini returned no candidates", null);
        }

        Candidate candidate = response.candidates().get(0);
        if (!isBlank(candidate.finishReason()) && !"STOP".equals(candidate.finishReason())) {
            // e.g. MAX_TOKENS means the JSON is cut off; SAFETY means it was blocked
            throw bad("Gemini stopped early (" + candidate.finishReason() + ")", null);
        }
        if (candidate.content() == null || candidate.content().parts() == null) {
            throw bad("Gemini returned no content", null);
        }

        StringBuilder text = new StringBuilder();
        for (Part part : candidate.content().parts()) {
            if (part != null && part.text() != null) text.append(part.text());
        }
        if (isBlank(text.toString())) {
            throw bad("Gemini returned empty text", null);
        }

        ModelOutput output;
        try {
            output = mapper.readValue(text.toString(), ModelOutput.class);
        } catch (RuntimeException e) {
            throw bad("Gemini's answer did not match the expected JSON structure", e);
        }
        return sanitize(output);
    }

    /** Never trust model output: check it, cap its size, and normalise its values. */
    private InsightResult sanitize(ModelOutput out) {
        if (out == null || isBlank(out.summary())) {
            throw bad("Gemini's answer had no summary", null);
        }
        List<InsightResult.Insight> insights = out.insights() == null ? List.of() :
                out.insights().stream()
                        .filter(Objects::nonNull)
                        .filter(i -> !isBlank(i.title()))
                        .limit(MAX_ITEMS)
                        .map(i -> new InsightResult.Insight(
                                cut(i.title(), 120), cut(i.detail(), 500), severity(i.severity())))
                        .toList();
        List<InsightResult.Suggestion> suggestions = out.suggestions() == null ? List.of() :
                out.suggestions().stream()
                        .filter(Objects::nonNull)
                        .filter(s -> !isBlank(s.title()))
                        .limit(MAX_ITEMS)
                        .map(s -> new InsightResult.Suggestion(
                                cut(s.title(), 120), cut(s.detail(), 500), saving(s.estimatedMonthlySaving())))
                        .toList();
        return new InsightResult(cut(out.summary(), 600), insights, suggestions, props.model());
    }

    // ---------- helpers ----------

    private static AiProviderException bad(String message, Throwable cause) {
        log.warn("Gemini bad response: {}", message);
        return cause == null
                ? new AiProviderException(Kind.BAD_RESPONSE, message)
                : new AiProviderException(Kind.BAD_RESPONSE, message, cause);
    }

    private static String severity(String s) {
        String upper = s == null ? "" : s.trim().toUpperCase();
        return SEVERITIES.contains(upper) ? upper : "INFO";
    }

    private static BigDecimal saving(BigDecimal v) {
        return (v == null || v.signum() < 0) ? null : v;
    }

    private static String cut(String s, int max) {
        if (s == null) return "";
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }

    private static Duration retryAfter(HttpResponse<String> response) {
        return response.headers().firstValue("Retry-After").map(v -> {
            try {
                return Duration.ofSeconds(Long.parseLong(v.trim()));
            } catch (NumberFormatException e) {
                return null;   // an HTTP-date format; we simply don't use it
            }
        }).orElse(null);
    }

    private static String abbreviate(String body) {
        if (body == null) return "";
        String flat = body.replaceAll("\\s+", " ");
        return flat.length() <= 200 ? flat : flat.substring(0, 200) + "…";
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}