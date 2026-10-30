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
 * @param requestId made by the client, used to cancel
 */
public record GenerateRequest(
        @NotNull UUID requestId,
        @NotEmpty @Size(max = 7) Set<@NotNull DayOfWeek> days,
        @NotEmpty @Size(max = 3) Set<@NotNull Meal> meals,
        @Valid @NotNull GenerationConstraints constraints) {
}
