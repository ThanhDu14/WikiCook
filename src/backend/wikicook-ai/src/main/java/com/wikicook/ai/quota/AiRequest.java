package com.wikicook.ai.quota;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One row of {@code ai_requests} (data-model.md section 1).
 *
 * @param id          {@code null} before insert
 * @param constraints JSON text of the generation constraints, or {@code null}
 */
public record AiRequest(
        Long id,
        AiFeature feature,
        Long userId,
        Long recipeId,
        Long actorId,
        AiTrigger trigger,
        AiOutcome outcome,
        boolean calledLlm,
        boolean counted,
        LocalDate weekStart,
        Integer requestedSlots,
        Integer removedPicks,
        String constraints,
        String reason,
        Instant createdAt) {
}
