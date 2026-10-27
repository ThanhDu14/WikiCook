package com.wikicook.ai.embedding;

import com.wikicook.ai.AiUnavailableException;
import java.util.List;

/** Turns text into unit-length vectors stored in {@code recipe_embeddings.embedding}. */
public interface EmbeddingClient {

    /** Number of dimensions of every vector; must match the {@code VECTOR(n)} column. */
    int dimensions();

    /** Name stored in {@code recipe_embeddings.model}; a new name means every vector must be rebuilt. */
    String model();

    /** @throws AiUnavailableException when the provider cannot be used */
    float[] embed(String text, EmbeddingTask task);

    /** Same order as {@code texts}. @throws AiUnavailableException when the provider cannot be used */
    default List<float[]> embedBatch(List<String> texts, EmbeddingTask task) {
        return texts.stream().map(text -> embed(text, task)).toList();
    }
}
