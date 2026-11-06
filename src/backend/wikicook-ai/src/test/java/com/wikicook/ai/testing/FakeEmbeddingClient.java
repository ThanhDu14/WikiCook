package com.wikicook.ai.testing;

import com.wikicook.ai.AiUnavailableException;
import com.wikicook.ai.embedding.EmbeddingClient;
import com.wikicook.ai.embedding.EmbeddingTask;
import com.wikicook.ai.embedding.MockEmbeddingClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Embeddings for tests: fixed vectors for chosen texts (so tests decide what is "close"), hash-based vectors for
 * the rest; can be switched to failing. Records every text it was asked to embed.
 */
public class FakeEmbeddingClient implements EmbeddingClient {

    public static final String MODEL = "fake-embedding";

    private final int dimensions;
    private final MockEmbeddingClient fallback;
    private final Map<String, float[]> fixed = new HashMap<>();
    private final List<String> texts = new ArrayList<>();
    private volatile boolean failing;

    public FakeEmbeddingClient(int dimensions) {
        this.dimensions = dimensions;
        this.fallback = new MockEmbeddingClient(dimensions);
    }

    /** Uses {@code values} (padded with zeros to the configured size) whenever {@code text} is embedded. */
    public synchronized FakeEmbeddingClient vector(String text, float... values) {
        float[] vector = new float[dimensions];
        System.arraycopy(values, 0, vector, 0, values.length);
        fixed.put(text, vector);
        return this;
    }

    public FakeEmbeddingClient failing(boolean value) {
        failing = value;
        return this;
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    @Override
    public String model() {
        return MODEL;
    }

    @Override
    public synchronized float[] embed(String text, EmbeddingTask task) {
        texts.add(text);
        if (failing) {
            throw new AiUnavailableException("Gemini returned HTTP 503");
        }
        float[] vector = fixed.get(text);
        return vector != null ? vector.clone() : fallback.embed(text, task);
    }

    public synchronized List<String> texts() {
        return List.copyOf(texts);
    }
}
