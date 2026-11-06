package com.wikicook.aiplanner.domain;

import com.wikicook.mealplan.domain.DietType;

/** What a draft shows of a recipe (openapi {@code DraftItem}). */
public record RecipeCard(long recipeId, String title, String coverImageUrl, Integer totalTimeMinutes,
                         DietType dietType) {
}
