package com.wikicook.aiplanner.domain;

import com.wikicook.mealplan.domain.Meal;
import java.util.List;
import java.util.Set;

/**
 * A recipe the LLM may pick for a meal, lives only during one generation (data-model 3).
 *
 * @param meals               meals the recipe suits
 * @param mainIngredientIds   first five ingredients in recipe order
 * @param mainIngredientNames names of {@code mainIngredientIds}, same order
 * @param rank                position in the meal's candidate list, 0 = best match
 */
public record Candidate(
        long recipeId,
        String title,
        Set<Meal> meals,
        Integer totalTimeMinutes,
        Long cuisineId,
        String cuisineName,
        Long categoryId,
        List<Long> mainIngredientIds,
        List<String> mainIngredientNames,
        int rank) {

    public Candidate withRank(int newRank) {
        return new Candidate(recipeId, title, meals, totalTimeMinutes, cuisineId, cuisineName, categoryId,
                mainIngredientIds, mainIngredientNames, newRank);
    }
}
