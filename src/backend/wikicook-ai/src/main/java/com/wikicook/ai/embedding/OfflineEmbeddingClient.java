package com.wikicook.ai.embedding;

import com.wikicook.ai.AiUnavailableException;

/** Always unavailable; retrieval then skips the vector step (research R4). */
public class OfflineEmbeddingClient implements EmbeddingClient {

    private final int dimensions;
    private final String model;

    public OfflineEmbeddingClient(int dimensions, String model) {
        this.dimensions = dimensions;
        this.model = model;
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
        throw new AiUnavailableException("AI is in offline mode");
    }
}
