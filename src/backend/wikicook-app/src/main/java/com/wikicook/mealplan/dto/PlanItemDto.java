package com.wikicook.mealplan.dto;

import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.domain.ItemSource;

/**
 * openapi {@code PlanItem}: one dish of a meal (proposal 4.6 + servings and position, FR-030).
 *
 * @param position   place inside the meal, 0 first
 * @param recipeName the recipe title, or the saved snapshot when it is no longer available (FR-020)
 * @param available  {@code false} when the recipe was archived or deleted
 */
public record PlanItemDto(
        long id,
        int version,
        int position,
        Long recipeId,
        String recipeName,
        String coverImageUrl,
        Integer totalTimeMinutes,
        int servings,
        ItemSource source,
        boolean available,
        DietType dietType) {
}
