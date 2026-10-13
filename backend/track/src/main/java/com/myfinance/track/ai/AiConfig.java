package com.myfinance.track.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    /** The one place that decides which provider the app uses. Swapping providers means changing this bean. */
    @Bean
    AiInsightProvider aiInsightProvider(AiProperties props, JsonMapper mapper) {
        return new GeminiInsightProvider(props, mapper);
    }
}