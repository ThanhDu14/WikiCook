package com.wikicook.ai.llm;

import java.util.Objects;

/**
 * One call to the LLM.
 *
 * @param systemPrompt   instructions that do not depend on the user
 * @param userPrompt     the request, including the candidate recipes
 * @param responseSchema JSON schema the reply must follow (Gemini {@code responseSchema}), or {@code null}
 * @param temperature    0.0 – 2.0
 */
public record LlmRequest(String systemPrompt, String userPrompt, String responseSchema, double temperature) {

    public LlmRequest {
        Objects.requireNonNull(userPrompt, "userPrompt");
        if (temperature < 0.0 || temperature > 2.0) {
            throw new IllegalArgumentException("temperature must be between 0.0 and 2.0");
        }
    }
}
