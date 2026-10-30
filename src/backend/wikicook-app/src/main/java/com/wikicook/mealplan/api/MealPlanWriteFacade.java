package com.wikicook.mealplan.api;

import com.wikicook.mealplan.domain.SlotKey;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * What the AI planner (031) may read and write in a user's week (030 contracts/integration.md 2.2). Implemented
 * by 030; until then the dev stubs ({@code com.wikicook.devstub}) keep plans in memory.
 */
public interface MealPlanWriteFacade {

    /** Empty slots of the week, so 031 fills only those (031 FR-007). */
    Set<SlotKey> emptySlots(long userId, LocalDate weekStart);

    /** Distinct recipe ids already planned in the week (031 FR-034). */
    Set<Long> plannedRecipeIds(long userId, LocalDate weekStart);

    /**
     * Saves many dishes in one transaction (031 FR-018, FR-019). Skips slots that already have a dish and recipes
     * no longer PUBLISHED; returns how many were skipped.
     */
    BatchSaveResult saveDraft(long userId, LocalDate weekStart, List<DraftItem> items);
}
