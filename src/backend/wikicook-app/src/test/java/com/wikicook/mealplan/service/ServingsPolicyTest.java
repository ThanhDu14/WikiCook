package com.wikicook.mealplan.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.port.PlannerProfile;
import com.wikicook.mealplan.port.RecipeStatus;
import com.wikicook.mealplan.port.RecipeSummary;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** T029: default servings of a new dish (FR-012, research R9). */
class ServingsPolicyTest {

    private static PlannerProfile household(Integer size) {
        return new PlannerProfile(size, DietType.NONE, null, Set.of(), Set.of(), Set.of());
    }

    private static RecipeSummary recipe(Integer servings) {
        return new RecipeSummary(1, "Phở bò", null, 45, servings, RecipeStatus.PUBLISHED, false, DietType.NONE);
    }

    @Test
    void householdSizeWins() { // US1-5
        assertThat(ServingsPolicy.defaultServings(household(4), recipe(2))).isEqualTo(4);
    }

    @Test
    void withoutHouseholdTheRecipeServings() { // US1-6
        assertThat(ServingsPolicy.defaultServings(household(null), recipe(6))).isEqualTo(6);
    }

    @Test
    void clampedToOneTwenty() {
        assertThat(ServingsPolicy.defaultServings(household(null), recipe(50))).isEqualTo(20);
        assertThat(ServingsPolicy.defaultServings(household(null), recipe(null))).isEqualTo(1);
        assertThat(ServingsPolicy.defaultServings(household(null), recipe(0))).isEqualTo(1);
    }
}
