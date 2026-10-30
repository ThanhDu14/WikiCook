package com.wikicook.mealplan.api;

/** Outcome of {@link MealPlanWriteFacade#saveDraft} (031 FR-019). */
public record BatchSaveResult(int saved, int skippedOccupied, int skippedUnavailable) {
}
