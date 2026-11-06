package com.wikicook.ai.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wikicook.ai.AiUnavailableException;
import com.wikicook.ai.gemini.GeminiHttp;
import java.time.Duration;
import java.util.Objects;

/**
 * Gemini {@code generateContent} over REST (research R2): JSON mode with the request's schema, key in the
 * {@code x-goog-api-key} header. Neither the key nor the provider's error text reaches logs or exceptions.
 */
public class GeminiLlmClient implements LlmClient, AutoCloseable {

    private final GeminiHttp http;
    private final String model;
    private final ObjectMapper json = new ObjectMapper();

    public GeminiLlmClient(String baseUrl, String apiKey, String model) {
        this.http = new GeminiHttp(baseUrl, apiKey);
        this.model = Objects.requireNonNull(model, "model");
    }

    @Override
    public LlmResult complete(LlmRequest request, Duration timeout, CancellationToken token) {
        ObjectNode body = body(request);
        JsonNode reply = http.post("/models/{model}:generateContent", model, body, timeout, token);
        return new LlmResult(text(reply), reply.path("modelVersion").asText(model));
    }

    private ObjectNode body(LlmRequest request) {
        ObjectNode body = json.createObjectNode();
        if (request.systemPrompt() != null) {
            body.putObject("systemInstruction").putArray("parts").addObject().put("text", request.systemPrompt());
        }
        ObjectNode content = body.putArray("contents").addObject();
        content.put("role", "user");
        content.putArray("parts").addObject().put("text", request.userPrompt());
        ObjectNode config = body.putObject("generationConfig");
        config.put("temperature", request.temperature());
        config.put("responseMimeType", "application/json");
        if (request.responseSchema() != null) {
            config.set("responseSchema", schema(request.responseSchema()));
        }
        return body;
    }

    private JsonNode schema(String schema) {
        try {
            return json.readTree(schema);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("responseSchema is not valid JSON", e);
        }
    }

    private static String text(JsonNode reply) {
        JsonNode candidate = reply.path("candidates").path(0);
        if (candidate.isMissingNode()) {
            throw new AiUnavailableException("Gemini returned no candidate");
        }
        String finishReason = candidate.path("finishReason").asText("STOP");
        if (!"STOP".equals(finishReason)) {
            throw new AiUnavailableException("Gemini stopped early: " + finishReason);
        }
        StringBuilder text = new StringBuilder();
        candidate.path("content").path("parts").forEach(part -> text.append(part.path("text").asText("")));
        return text.toString();
    }

    @Override
    public void close() {
        http.close();
    }

    @Override
    public String toString() {
        return "GeminiLlmClient[model=" + model + "]";
    }
}
