package com.wikicook.mealplan.dto;

import com.wikicook.mealplan.domain.ItemSource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * openapi {@code ReplaceItemRequest}: another recipe in the same place (FR-016).
 *
 * @param servings {@code null} keeps the old servings
 * @param version  the version of the dish the client saw (research R4)
 */
public record ReplaceItemRequest(
        @NotNull Long recipeId,
        @Min(1) @Max(20) Integer servings,
        ItemSource source,
        @NotNull Integer version) {
}
