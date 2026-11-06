package com.wikicook.mealplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.wikicook.mealplan.api.HandoffItem;
import com.wikicook.mealplan.api.ShoppingHandoff;
import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.domain.ItemSource;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.MealPlan;
import com.wikicook.mealplan.domain.MealPlanItem;
import com.wikicook.mealplan.port.DayNutritionPort;
import com.wikicook.mealplan.port.DietaryProfilePort;
import com.wikicook.mealplan.port.RecipeStatus;
import com.wikicook.mealplan.port.RecipeSummary;
import com.wikicook.mealplan.port.RecipeSummaryPort;
import com.wikicook.mealplan.repository.MealPlanItemRepository;
import com.wikicook.mealplan.repository.MealPlanRepository;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** T076: what the week hands to the Shopping List (US5, FR-024, FR-025). */
class ShoppingHandoffServiceTest {

    private static final long USER = 7;
    private static final LocalDate WEEK = LocalDate.of(2026, 10, 19);

    private final MealPlanRepository plans = mock(MealPlanRepository.class);
    private final MealPlanItemRepository items = mock(MealPlanItemRepository.class);
    private final RecipeSummaryPort recipes = mock(RecipeSummaryPort.class);
    private final Map<Long, RecipeSummary> catalogue = new HashMap<>();
    private final List<MealPlanItem> stored = new ArrayList<>();
    private ShoppingHandoffService service;
    private MealPlan plan;

    @BeforeEach
    void setUp() throws Exception {
        MealPlanService weeks = new MealPlanService(plans, items, recipes, mock(DietaryProfilePort.class),
                mock(DayNutritionPort.class), Clock.systemUTC());
        service = new ShoppingHandoffService(plans, items, weeks);
        var constructor = MealPlan.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        plan = constructor.newInstance();
        set(plan, "id", 100L);
        when(recipes.findByIds(any())).thenAnswer(call -> {
            Map<Long, RecipeSummary> found = new HashMap<>();
            for (Long id : call.<Collection<Long>>getArgument(0)) {
                if (catalogue.containsKey(id)) {
                    found.put(id, catalogue.get(id));
                }
            }
            return found;
        });
        when(items.findByMealPlanId(100L)).thenReturn(stored);
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private void recipe(long id, RecipeStatus status) {
        catalogue.put(id, new RecipeSummary(id, "Món " + id, null, 30, 2, status, false, DietType.NONE));
    }

    private long item(DayOfWeek day, Meal meal, int position, Long recipeId, int servings) throws Exception {
        MealPlanItem item = new MealPlanItem(plan, day, meal, position, recipeId == null ? 0 : recipeId,
                "Món " + recipeId, servings, ItemSource.MANUAL);
        if (recipeId == null) {
            set(item, "recipeId", null); // ON DELETE SET NULL
        }
        long id = 500L + stored.size();
        set(item, "id", id);
        stored.add(item);
        return id;
    }

    @Test
    void onlyAvailableDishesAreHandedOverInWeekOrder() throws Exception { // US5-1, US5-3
        recipe(1, RecipeStatus.PUBLISHED);
        recipe(2, RecipeStatus.PUBLISHED);
        recipe(3, RecipeStatus.ARCHIVED);
        when(plans.findByUserIdAndWeekStart(USER, WEEK)).thenReturn(Optional.of(plan));
        long fridayCa = item(DayOfWeek.FRIDAY, Meal.DINNER, 0, 2L, 4);
        long mondayCanh = item(DayOfWeek.MONDAY, Meal.DINNER, 1, 1L, 3);
        long mondayPho = item(DayOfWeek.MONDAY, Meal.BREAKFAST, 0, 1L, 2);
        item(DayOfWeek.TUESDAY, Meal.LUNCH, 0, 3L, 2); // archived
        item(DayOfWeek.WEDNESDAY, Meal.LUNCH, 0, null, 2); // recipe deleted

        ShoppingHandoff handoff = service.getHandoff(USER, WEEK);

        assertThat(handoff.weekStart()).isEqualTo(WEEK);
        assertThat(handoff.items()).containsExactly(
                new HandoffItem(mondayPho, 1, 2, DayOfWeek.MONDAY, Meal.BREAKFAST),
                new HandoffItem(mondayCanh, 1, 3, DayOfWeek.MONDAY, Meal.DINNER),
                new HandoffItem(fridayCa, 2, 4, DayOfWeek.FRIDAY, Meal.DINNER));
        assertThat(handoff.skippedUnavailable()).isEqualTo(2);
    }

    @Test
    void aWeekWithoutPlanHandsOverNothing() { // US5-2
        when(plans.findByUserIdAndWeekStart(USER, WEEK)).thenReturn(Optional.empty());

        ShoppingHandoff handoff = service.getHandoff(USER, WEEK);

        assertThat(handoff.items()).isEmpty();
        assertThat(handoff.skippedUnavailable()).isZero();
    }
}
