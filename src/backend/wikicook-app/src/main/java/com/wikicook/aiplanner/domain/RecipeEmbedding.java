package com.wikicook.aiplanner.domain;

import java.time.Instant;

/** One row of {@code recipe_embeddings}: the unit-length vector of a published recipe. */
public record RecipeEmbedding(long recipeId, float[] embedding, String model, String contentHash, Instant updatedAt) {
}
