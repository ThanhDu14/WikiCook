package com.wikicook.mealplan.api;

import com.wikicook.mealplan.domain.SlotKey;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * What the AI planner (031) may read and write in a user's week (030 contracts/integration.md 2.2, 2026-11-02:
 * several dishes per meal). A {@link SlotKey} is a meal {@code (day, meal)}. Implemented by
 * {@code com.wikicook.mealplan.service.MealPlanWriteService}.
 */
public interface MealPlanWriteFacade {

    /** Meals of the week without any dish, so 031 fills only those (031 FR-007). */
    Set<SlotKey> emptySlots(long userId, LocalDate weekStart);

    /** Distinct recipe ids already planned in the week (031 FR-034). */
    Set<Long> plannedRecipeIds(long userId, LocalDate weekStart);

    /**
     * Saves many dishes in one transaction (031 FR-018, FR-019). Dishes of the same {@code (day, meal)} keep their
     * list order inside the meal, at most 4 per meal (later ones are dropped). Skips the whole group of a meal that
     * already has a dish ({@code skippedOccupied}) and recipes no longer PUBLISHED ({@code skippedUnavailable}).
     */
    BatchSaveResult saveDraft(long userId, LocalDate weekStart, List<DraftItem> items);
}
