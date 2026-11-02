package com.wikicook.mealplan.port;

import com.wikicook.mealplan.domain.DietType;

/**
 * What a slot needs of a recipe (contracts/integration.md 1).
 *
 * @param dietType the strictest diet the ingredient kinds allow (031 FR-009a), not 090's AI labels
 */
public record RecipeSummary(long id, String title, String coverImageUrl, Integer totalTimeMinutes,
                            Integer servings, RecipeStatus status, boolean snack, DietType dietType) {

    /** Whether it can go into a plan: published and not a snack (FR-010). */
    public boolean available() {
        return status == RecipeStatus.PUBLISHED && !snack;
    }
}
