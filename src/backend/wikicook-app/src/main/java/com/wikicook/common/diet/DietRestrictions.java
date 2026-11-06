package com.wikicook.common.diet;

import com.wikicook.mealplan.domain.DietType;
import java.util.Objects;
import java.util.Set;

/**
 * Hard rules of one person or one request: allergens, excluded ingredients and diet type (031 FR-009).
 *
 * @param allergenIds           {@code allergens.id}
 * @param excludedIngredientIds {@code ingredients.id}
 */
public record DietRestrictions(Set<Long> allergenIds, Set<Long> excludedIngredientIds, DietType dietType) {

    public DietRestrictions {
        allergenIds = Set.copyOf(allergenIds);
        excludedIngredientIds = Set.copyOf(excludedIngredientIds);
        Objects.requireNonNull(dietType, "dietType");
    }

    public static DietRestrictions none() {
        return new DietRestrictions(Set.of(), Set.of(), DietType.NONE);
    }
}
