package com.wikicook.mealplan.port;

import com.wikicook.mealplan.domain.DietType;
import java.util.Set;

/**
 * What the planners read from a profile (030 contracts/integration.md 1).
 *
 * @param householdSize      {@code null} → use the recipe's servings (030 FR-012)
 * @param maxCookTimeMinutes {@code null} = no limit
 */
public record PlannerProfile(
        Integer householdSize,
        DietType dietType,
        Integer maxCookTimeMinutes,
        Set<Long> allergenIds,
        Set<Long> excludedIngredientIds,
        Set<Long> preferredCuisineIds) {

    public PlannerProfile {
        dietType = dietType == null ? DietType.NONE : dietType;
        allergenIds = allergenIds == null ? Set.of() : Set.copyOf(allergenIds);
        excludedIngredientIds = excludedIngredientIds == null ? Set.of() : Set.copyOf(excludedIngredientIds);
        preferredCuisineIds = preferredCuisineIds == null ? Set.of() : Set.copyOf(preferredCuisineIds);
    }

    public static PlannerProfile empty() {
        return new PlannerProfile(null, DietType.NONE, null, Set.of(), Set.of(), Set.of());
    }
}
