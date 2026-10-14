package com.myfinance.track.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({AiProperties.class, AiServiceProperties.class})
public class AiConfig {

    /** The one place that decides which provider the app uses. */
    @Bean
    AiInsightProvider aiInsightProvider(AiProperties props, JsonMapper mapper) {
        return new GeminiInsightProvider(props, mapper);
    }

    /** Injected so tests can control time. */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}