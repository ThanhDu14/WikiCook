package com.wikicook.ai.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wikicook.ai.AiUnavailableException;
import com.wikicook.ai.gemini.GeminiHttp;
import com.wikicook.ai.llm.CancellationToken;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Gemini embeddings (research R3): {@code outputDimensionality} cut to the {@code VECTOR(n)} column, and the
 * result normalised to unit length because Gemini only normalises the full-size vector.
 */
public class GeminiEmbeddingClient implements EmbeddingClient, AutoCloseable {

    /** Most texts {@code batchEmbedContents} accepts in one request. */
    static final int MAX_BATCH = 100;

    private final GeminiHttp http;
    private final String model;
    private final int dimensions;
    private final Duration timeout;
    private final ObjectMapper json = new ObjectMapper();

    public GeminiEmbeddingClient(String baseUrl, String apiKey, String model, int dimensions, Duration timeout) {
        if (dimensions <= 0) {
            throw new IllegalArgumentException("dimensions must be positive");
        }
        this.http = new GeminiHttp(baseUrl, apiKey);
        this.model = Objects.requireNonNull(model, "model");
        this.dimensions = dimensions;
        this.timeout = Objects.requireNonNull(timeout, "timeout");
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public float[] embed(String text, EmbeddingTask task) {
        JsonNode reply = http.post("/models/{model}:embedContent", model, request(text, task), timeout,
                CancellationToken.none());
        return vector(reply.path("embedding"));
    }

    @Override
    public List<float[]> embedBatch(List<String> texts, EmbeddingTask task) {
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (int from = 0; from < texts.size(); from += MAX_BATCH) {
            List<String> chunk = texts.subList(from, Math.min(from + MAX_BATCH, texts.size()));
            ObjectNode body = json.createObjectNode();
            chunk.forEach(text -> body.withArray("requests").add(request(text, task)));
            JsonNode embeddings = http.post("/models/{model}:batchEmbedContents", model, body, timeout,
                    CancellationToken.none()).path("embeddings");
            if (embeddings.size() != chunk.size()) {
                throw new AiUnavailableException(
                        "Gemini returned " + embeddings.size() + " vectors for " + chunk.size() + " texts");
            }
            embeddings.forEach(embedding -> vectors.add(vector(embedding)));
        }
        return vectors;
    }

    private ObjectNode request(String text, EmbeddingTask task) {
        ObjectNode request = json.createObjectNode();
        request.put("model", "models/" + model);
        request.putObject("content").putArray("parts").addObject().put("text", text);
        request.put("taskType", task.name());
        request.put("outputDimensionality", dimensions);
        return request;
    }

    private float[] vector(JsonNode embedding) {
        JsonNode values = embedding.path("values");
        if (values.size() != dimensions) {
            throw new AiUnavailableException("Gemini returned " + values.size() + " dimensions, expected " + dimensions);
        }
        float[] vector = new float[dimensions];
        double norm = 0;
        for (int i = 0; i < dimensions; i++) {
            vector[i] = (float) values.get(i).asDouble();
            norm += vector[i] * vector[i];
        }
        if (norm == 0) {
            throw new AiUnavailableException("Gemini returned a zero vector");
        }
        float scale = (float) (1.0 / Math.sqrt(norm));
        for (int i = 0; i < dimensions; i++) {
            vector[i] *= scale;
        }
        return vector;
    }

    @Override
    public void close() {
        http.close();
    }

    @Override
    public String toString() {
        return "GeminiEmbeddingClient[model=" + model + ", dimensions=" + dimensions + "]";
    }
}
