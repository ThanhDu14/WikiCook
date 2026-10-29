package com.wikicook.ai.embedding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Deterministic vectors derived from a hash of the text: the same text always gives the same vector,
 * so retrieval tests are stable. The vectors carry no meaning.
 */
public class MockEmbeddingClient implements EmbeddingClient {

    public static final String MODEL = "mock";

    private final int dimensions;

    public MockEmbeddingClient(int dimensions) {
        if (dimensions <= 0) {
            throw new IllegalArgumentException("dimensions must be positive");
        }
        this.dimensions = dimensions;
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
    public float[] embed(String text, EmbeddingTask task) {
        Random random = new Random(seed(task.name() + ":" + text));
        float[] vector = new float[dimensions];
        double norm = 0;
        for (int i = 0; i < dimensions; i++) {
            vector[i] = (float) random.nextGaussian();
            norm += vector[i] * vector[i];
        }
        float scale = (float) (1.0 / Math.sqrt(norm));
        for (int i = 0; i < dimensions; i++) {
            vector[i] *= scale;
        }
        return vector;
    }

    private static long seed(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            long seed = 0;
            for (int i = 0; i < Long.BYTES; i++) {
                seed = (seed << 8) | (hash[i] & 0xFF);
            }
            return seed;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
