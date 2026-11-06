package com.wikicook.mealplan.service;

import com.wikicook.mealplan.port.PlannerProfile;
import com.wikicook.mealplan.port.RecipeSummary;

/** Servings of a new dish when the client sends none (FR-012, research R9); 031 uses it when confirming. */
public final class ServingsPolicy {

    public static final int MIN = 1;
    public static final int MAX = 20;

    private ServingsPolicy() {
    }

    /** Household size, else the recipe's servings, clamped to 1–20 (recipes allow up to 50). */
    public static int defaultServings(PlannerProfile profile, RecipeSummary recipe) {
        Integer value = profile.householdSize() != null ? profile.householdSize() : recipe.servings();
        return value == null ? MIN : Math.clamp(value, MIN, MAX);
    }
}
