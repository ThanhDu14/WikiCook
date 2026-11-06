package com.wikicook.aiplanner.embedding;

import java.util.List;

/**
 * What goes into a recipe's embedding (research R10), read from {@code recipes}, its lookups and
 * {@code recipe_ingredients}.
 *
 * @param category         {@code categories.name}, or {@code null}
 * @param cuisine          {@code cuisines.name}, or {@code null}
 * @param totalTimeMinutes {@code recipes.total_time_minutes}, or {@code null}
 * @param ingredients      ingredient names in recipe order
 */
public record RecipeEmbeddingSource(
        long recipeId,
        String title,
        String category,
        String cuisine,
        boolean forBreakfast,
        boolean forLunch,
        boolean forDinner,
        Integer totalTimeMinutes,
        List<String> ingredients) {
}
