package com.wikicook.mealplan.api;

import com.wikicook.mealplan.domain.Meal;
import java.time.DayOfWeek;

/** One available dish of the week; {@code mealPlanItemId} goes to {@code shopping_list_item_sources}. */
public record HandoffItem(long mealPlanItemId, long recipeId, int servings, DayOfWeek day, Meal meal) {
}
