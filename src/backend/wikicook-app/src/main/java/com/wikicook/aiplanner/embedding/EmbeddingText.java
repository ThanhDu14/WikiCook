package com.wikicook.aiplanner.embedding;

/**
 * @param contentHash SHA-256 of {@code text} in hex, stored in {@code recipe_embeddings.content_hash}
 */
public record EmbeddingText(String text, String contentHash) {
}
