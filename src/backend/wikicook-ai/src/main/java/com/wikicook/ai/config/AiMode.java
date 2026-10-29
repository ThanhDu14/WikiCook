package com.wikicook.ai.config;

/** {@code wikicook.ai.mode}, same switch as {@code LLM_MODE} in the project plan. */
public enum AiMode {
    /** Calls the real provider; needs {@code WIKICOOK_AI_API_KEY}. */
    LIVE,
    /** Canned answers and hash-based vectors; CI and demos without a key. */
    MOCK,
    /** Every AI call fails, to exercise the fallback paths. */
    OFFLINE
}
