package com.wikicook.aiplanner.dto;

import com.wikicook.common.diet.DietRestrictions;
import com.wikicook.mealplan.domain.DietType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * Constraints of one generation (data-model 3, openapi {@code GenerationConstraints}); stored as JSON in
 * {@code ai_requests.constraints}.
 *
 * @param servings           1–20, or {@code null} for "as in the recipe"
 * @param maxCookTimeMinutes {@code null} or at least 10
 */
public record GenerationConstraints(
        @NotNull Set<@NotNull Long> allergenIds,
        @NotNull Set<@NotNull Long> excludedIngredientIds,
        @NotNull DietType dietType,
        @Min(1) @Max(20) Integer servings,
        @Min(10) Integer maxCookTimeMinutes,
        @NotNull Set<@NotNull Long> preferredCuisineIds) {

    public DietRestrictions restrictions() {
        return new DietRestrictions(allergenIds, excludedIngredientIds, dietType);
    }
}
