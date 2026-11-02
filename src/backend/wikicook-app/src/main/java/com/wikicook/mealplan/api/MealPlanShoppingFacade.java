package com.wikicook.mealplan.api;

import java.time.LocalDate;

/** What the Shopping List (040) reads of a user's week (contracts/integration.md 2.1). */
public interface MealPlanShoppingFacade {

    /** Available dishes of the week and the number of unavailable ones skipped (FR-024, FR-025). */
    ShoppingHandoff getHandoff(long userId, LocalDate weekStart);
}
