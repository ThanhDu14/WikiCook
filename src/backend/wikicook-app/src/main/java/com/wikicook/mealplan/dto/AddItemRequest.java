package com.wikicook.mealplan.dto;

import com.wikicook.mealplan.domain.ItemSource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * openapi {@code AddItemRequest}: a dish for the end of a meal (FR-004, FR-007, FR-008).
 *
 * @param servings {@code null}: household size or recipe servings (FR-012)
 * @param source   {@code null} = MANUAL; AI when picked from 031's suggestions (031 FR-038)
 */
public record AddItemRequest(
        @NotNull Long recipeId,
        @Min(1) @Max(20) Integer servings,
        ItemSource source) {
}
