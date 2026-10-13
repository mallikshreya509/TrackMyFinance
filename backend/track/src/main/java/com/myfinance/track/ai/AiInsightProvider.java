package com.myfinance.track.ai;

public interface AiInsightProvider {

    /** @throws AiProviderException for every failure, so callers only handle one exception type */
    InsightResult generate(InsightRequest request);

    String modelName();
}