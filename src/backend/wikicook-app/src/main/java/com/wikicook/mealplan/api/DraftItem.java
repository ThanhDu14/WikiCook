package com.wikicook.mealplan.api;

import com.wikicook.mealplan.domain.ItemSource;
import com.wikicook.mealplan.domain.Meal;
import java.time.DayOfWeek;

/**
 * One dish to save through {@link MealPlanWriteFacade#saveDraft}.
 *
 * @param servings {@code null} → the recipe's servings
 */
public record DraftItem(DayOfWeek day, Meal meal, long recipeId, Integer servings, ItemSource source) {
}
