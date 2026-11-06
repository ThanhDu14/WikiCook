package com.wikicook.aiplanner.dto;

import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.domain.ItemSource;
import com.wikicook.mealplan.domain.Meal;
import java.time.DayOfWeek;

/**
 * One dish of a draft (openapi {@code DraftItem}), shaped like 030's {@code PlanItem}.
 *
 * @param servings {@code null} = as in the recipe
 * @param dietType the strictest diet the recipe's ingredients allow
 */
public record DraftItemDto(
        DayOfWeek day,
        Meal meal,
        long recipeId,
        String recipeName,
        String coverImageUrl,
        Integer totalTimeMinutes,
        Integer servings,
        ItemSource source,
        DietType dietType) {
}
