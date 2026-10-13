package com.myfinance.track.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.gemini")
public record AiProperties(String apiKey, String model, String baseUrl,
                           int timeoutSeconds, double temperature, int maxOutputTokens) {}