package com.wikicook.ai.llm;

/**
 * Reply of the LLM.
 *
 * @param text  the raw reply (JSON when the request had a schema)
 * @param model model name that produced it, stored with the result for traceability
 */
public record LlmResult(String text, String model) {
}
