package com.wikicook.ai.quota;

/**
 * Result of {@link AiQuotaService#reserve}.
 *
 * @param requestRowId the {@code ai_requests} row to {@link AiQuotaService#finish finish}
 * @param llmAllowed   {@code false} when the feature's daily cap is used up: take the non-AI path
 */
public record Reservation(long requestRowId, boolean llmAllowed) {
}
