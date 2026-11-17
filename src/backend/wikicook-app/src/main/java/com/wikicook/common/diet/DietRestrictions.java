package com.wikicook.common.diet;

import com.wikicook.mealplan.domain.DietType;
import java.util.Objects;
import java.util.Set;

/**
 * Rules of one person or one request: allergens, excluded ingredients and diet type. For a person's own profile only
 * the first two are hard (011 FR-020 - FR-022): {@link #hardRules} is what 020, 030 and 031 use; the diet type becomes a
 * rule only when the caller passes it, as 031 does after the person confirmed it in the dialog (FR-022).
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

    /**
     * The hard rules of FR-020: the allergens and the excluded ingredients of a profile, never its diet type, so a
     * Vegetarian profile alone removes no recipe (FR-022).
     */
    public static DietRestrictions hardRules(Set<Long> allergenIds, Set<Long> excludedIngredientIds) {
        return new DietRestrictions(allergenIds, excludedIngredientIds, DietType.NONE);
    }
}
