package com.wikicook.aiplanner.dto;

import com.wikicook.mealplan.domain.Meal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.util.Set;
import java.util.UUID;

/**
 * Body of {@code POST …/generations} (openapi {@code GenerateRequest}, FR-003, FR-005).
 *
 * @param requestId      made by the client, used to cancel
 * @param avoidRecipeIds recipes to leave out when there is enough choice without them, e.g. the dishes of the draft
 *                       "Tạo lại" replaces; optional, never a hard rule
 */
public record GenerateRequest(
        @NotNull UUID requestId,
        @NotEmpty @Size(max = 7) Set<@NotNull DayOfWeek> days,
        @NotEmpty @Size(max = 3) Set<@NotNull Meal> meals,
        @Valid @NotNull GenerationConstraints constraints,
        @Size(max = MAX_AVOID) Set<@NotNull Long> avoidRecipeIds) {

    /** Four dishes in each of the 21 meals of a week. */
    public static final int MAX_AVOID = 84;

    public GenerateRequest {
        avoidRecipeIds = avoidRecipeIds == null ? Set.of() : avoidRecipeIds;
    }

    public GenerateRequest(UUID requestId, Set<DayOfWeek> days, Set<Meal> meals, GenerationConstraints constraints) {
        this(requestId, days, meals, constraints, Set.of());
    }
}
