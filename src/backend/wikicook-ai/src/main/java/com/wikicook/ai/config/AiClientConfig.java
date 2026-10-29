package com.wikicook.ai.config;

import com.wikicook.ai.embedding.EmbeddingClient;
import com.wikicook.ai.embedding.GeminiEmbeddingClient;
import com.wikicook.ai.embedding.MockEmbeddingClient;
import com.wikicook.ai.embedding.OfflineEmbeddingClient;
import com.wikicook.ai.llm.GeminiLlmClient;
import com.wikicook.ai.llm.LlmClient;
import com.wikicook.ai.llm.MockLlmClient;
import com.wikicook.ai.llm.OfflineLlmClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Picks the LLM and embedding clients from {@code wikicook.ai.mode}. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiProperties.class)
public class AiClientConfig {

    @Bean
    LlmClient llmClient(AiProperties properties) {
        return switch (properties.mode()) {
            case LIVE -> new GeminiLlmClient(properties.baseUrl(), requireKey(properties), properties.llm().model());
            case MOCK -> new MockLlmClient();
            case OFFLINE -> new OfflineLlmClient();
        };
    }

    @Bean
    EmbeddingClient embeddingClient(AiProperties properties) {
        int dimensions = properties.embedding().dimensions();
        return switch (properties.mode()) {
            case LIVE -> new GeminiEmbeddingClient(properties.baseUrl(), requireKey(properties),
                    properties.embedding().model(), dimensions, properties.embedding().timeout());
            case MOCK -> new MockEmbeddingClient(dimensions);
            case OFFLINE -> new OfflineEmbeddingClient(dimensions, properties.embedding().model());
        };
    }

    private static String requireKey(AiProperties properties) {
        if (!properties.hasApiKey()) {
            throw new IllegalStateException("wikicook.ai.mode=live needs WIKICOOK_AI_API_KEY");
        }
        return properties.apiKey();
    }
}
