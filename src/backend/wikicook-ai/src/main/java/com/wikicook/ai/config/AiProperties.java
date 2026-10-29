package com.wikicook.ai.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * {@code wikicook.ai.*}. The API key comes from the environment ({@code WIKICOOK_AI_API_KEY}) and is never logged.
 */
@Validated
@ConfigurationProperties("wikicook.ai")
public record AiProperties(
        @NotNull @DefaultValue("mock") AiMode mode,
        String apiKey,
        @Valid @NotNull @DefaultValue Llm llm,
        @Valid @NotNull @DefaultValue Embedding embedding) {

    public record Llm(
            @NotBlank @DefaultValue("gemini-2.5-flash") String model,
            @NotNull @DefaultValue("60s") Duration timeout) {
    }

    public record Embedding(
            @NotBlank @DefaultValue("gemini-embedding-001") String model,
            @Positive @DefaultValue("768") int dimensions) {
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Keeps the key out of logs and error messages. */
    @Override
    public String toString() {
        return "AiProperties[mode=" + mode + ", apiKey=" + (hasApiKey() ? "***" : "<unset>")
                + ", llm=" + llm + ", embedding=" + embedding + "]";
    }
}
