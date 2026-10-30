package com.wikicook.devstub;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.TUESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.wikicook.aiplanner.repository.EligibleRecipeQuery;
import com.wikicook.common.diet.DietRestrictions;
import com.wikicook.mealplan.api.BatchSaveResult;
import com.wikicook.mealplan.api.DraftItem;
import com.wikicook.mealplan.domain.ItemSource;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.SlotKey;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** In-memory weeks standing in for 030's MealPlanWriteFacade (031 FR-007, FR-018, FR-019). */
class InMemoryMealPlanFacadeTest {

    private static final LocalDate WEEK = LocalDate.of(2026, 11, 2);
    private static final long ARCHIVED = 99;

    private final EligibleRecipeQuery eligible = mock(EligibleRecipeQuery.class);
    private InMemoryMealPlanFacade facade;

    @BeforeEach
    void setUp() {
        when(eligible.stillEligible(anyCollection(), eq(DietRestrictions.none()))).thenAnswer(call -> {
            Set<Long> ids = new HashSet<>(call.<Collection<Long>>getArgument(0));
            ids.remove(ARCHIVED);
            return ids;
        });
        facade = new InMemoryMealPlanFacade(eligible);
    }

    private static DraftItem item(java.time.DayOfWeek day, Meal meal, long recipeId) {
        return new DraftItem(day, meal, recipeId, 4, ItemSource.AI);
    }

    @Test
    void newWeekHas21EmptySlots() {
        assertThat(facade.emptySlots(1, WEEK)).hasSize(21).first().isEqualTo(new SlotKey(MONDAY, Meal.BREAKFAST));
        assertThat(facade.plannedRecipeIds(1, WEEK)).isEmpty();
    }

    @Test
    void savedSlotsAreNoLongerEmpty() {
        BatchSaveResult result = facade.saveDraft(1, WEEK,
                List.of(item(MONDAY, Meal.BREAKFAST, 10), item(TUESDAY, Meal.DINNER, 11)));

        assertThat(result).isEqualTo(new BatchSaveResult(2, 0, 0));
        assertThat(facade.emptySlots(1, WEEK)).hasSize(19)
                .doesNotContain(new SlotKey(MONDAY, Meal.BREAKFAST), new SlotKey(TUESDAY, Meal.DINNER));
        assertThat(facade.plannedRecipeIds(1, WEEK)).containsExactlyInAnyOrder(10L, 11L);
    }

    @Test
    void skipsOccupiedSlotsAndUnavailableRecipes() {
        facade.saveDraft(1, WEEK, List.of(item(MONDAY, Meal.BREAKFAST, 10)));

        BatchSaveResult result = facade.saveDraft(1, WEEK, List.of(
                item(MONDAY, Meal.BREAKFAST, 12),   // occupied (US2-6)
                item(MONDAY, Meal.LUNCH, ARCHIVED), // no longer PUBLISHED
                item(MONDAY, Meal.DINNER, 10)));    // same recipe in another slot is allowed

        assertThat(result).isEqualTo(new BatchSaveResult(1, 1, 1));
        assertThat(facade.plannedRecipeIds(1, WEEK)).containsExactly(10L);
        assertThat(facade.emptySlots(1, WEEK)).contains(new SlotKey(MONDAY, Meal.LUNCH));
    }

    @Test
    void twoItemsForTheSameSlotKeepTheFirst() {
        BatchSaveResult result = facade.saveDraft(1, WEEK,
                List.of(item(MONDAY, Meal.LUNCH, 10), item(MONDAY, Meal.LUNCH, 11)));

        assertThat(result).isEqualTo(new BatchSaveResult(1, 1, 0));
        assertThat(facade.plannedRecipeIds(1, WEEK)).containsExactly(10L);
    }

    @Test
    void weeksAreKeptPerUserAndWeek() {
        facade.saveDraft(1, WEEK, List.of(item(MONDAY, Meal.BREAKFAST, 10)));

        assertThat(facade.emptySlots(2, WEEK)).hasSize(21);
        assertThat(facade.emptySlots(1, WEEK.plusWeeks(1))).hasSize(21);
    }

    @Test
    void emptyBatchSavesNothing() {
        assertThat(facade.saveDraft(1, WEEK, List.of())).isEqualTo(new BatchSaveResult(0, 0, 0));
    }

    @Test
    void concurrentSavesNeverFillASlotTwice() throws Exception {
        List<Future<BatchSaveResult>> results = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            for (int i = 0; i < 8; i++) {
                long recipe = 100 + i;
                results.add(pool.submit(() -> facade.saveDraft(1, WEEK, List.of(item(MONDAY, Meal.BREAKFAST, recipe)))));
            }
        }
        int saved = 0;
        for (Future<BatchSaveResult> result : results) {
            saved += result.get().saved();
        }
        assertThat(saved).isEqualTo(1);
        assertThat(facade.plannedRecipeIds(1, WEEK)).hasSize(1);
    }
}
