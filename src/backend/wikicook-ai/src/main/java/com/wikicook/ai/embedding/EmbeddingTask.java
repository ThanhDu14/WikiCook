package com.wikicook.ai.embedding;

/** Gemini embedding task types (research R3): recipes are documents, user requests are queries. */
public enum EmbeddingTask {
    RETRIEVAL_DOCUMENT,
    RETRIEVAL_QUERY
}
