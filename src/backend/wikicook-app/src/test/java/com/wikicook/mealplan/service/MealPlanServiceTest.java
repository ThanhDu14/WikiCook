package com.wikicook.mealplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.domain.ItemSource;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.MealPlan;
import com.wikicook.mealplan.domain.MealPlanItem;
import com.wikicook.mealplan.dto.DayPlanDto;
import com.wikicook.mealplan.dto.ExpectedSlotState;
import com.wikicook.mealplan.dto.PatchServingsRequest;
import com.wikicook.mealplan.dto.PlanItemDto;
import com.wikicook.mealplan.dto.PutSlotRequest;
import com.wikicook.mealplan.dto.SlotDto;
import com.wikicook.mealplan.dto.WeekPlanResponse;
import com.wikicook.mealplan.port.DietaryProfilePort;
import com.wikicook.mealplan.port.PlannerProfile;
import com.wikicook.mealplan.port.RecipeStatus;
import com.wikicook.mealplan.port.RecipeSummary;
import com.wikicook.mealplan.port.RecipeSummaryPort;
import com.wikicook.mealplan.repository.MealPlanItemRepository;
import com.wikicook.mealplan.repository.MealPlanRepository;
import com.wikicook.mealplan.service.exception.RecipeNotAvailableException;
import com.wikicook.mealplan.service.exception.SlotChangedException;
import com.wikicook.mealplan.service.exception.SlotOccupiedException;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** T030: reading and writing a week (US1), with the repositories and ports mocked. */
class MealPlanServiceTest {

    private static final long USER = 7;
    private static final LocalDate WEEK = LocalDate.of(2026, 10, 19);
    /** Wednesday 2026-10-21, 12:00 in Vietnam. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T05:00:00Z"), ZoneOffset.UTC);

    private final MealPlanRepository plans = mock(MealPlanRepository.class);
    private final MealPlanItemRepository items = mock(MealPlanItemRepository.class);
    private final RecipeSummaryPort recipes = mock(RecipeSummaryPort.class);
    private final DietaryProfilePort profiles = mock(DietaryProfilePort.class);
    private final Map<Long, RecipeSummary> catalogue = new HashMap<>();
    private final List<MealPlanItem> stored = new ArrayList<>();
    private MealPlanService service;
    private MealPlan plan;

    @BeforeEach
    void setUp() throws Exception {
        service = new MealPlanService(plans, items, recipes, profiles, CLOCK);
        plan = newPlan(100L);
        when(recipes.findByIds(any())).thenAnswer(call -> {
            Map<Long, RecipeSummary> found = new HashMap<>();
            for (Long id : call.<Collection<Long>>getArgument(0)) {
                if (catalogue.containsKey(id)) {
                    found.put(id, catalogue.get(id));
                }
            }
            return found;
        });
        when(profiles.findByUserId(anyLong())).thenReturn(PlannerProfile.empty());
        when(plans.findOrCreate(USER, WEEK)).thenReturn(plan);
        when(items.saveAndFlush(any())).thenAnswer(call -> {
            MealPlanItem item = call.getArgument(0);
            if (item.getId() == null) {
                set(item, "id", 1000L + stored.size());
                stored.add(item);
            } else {
                set(item, "version", item.getVersion() + 1);
            }
            return item;
        });
        when(items.findSlot(any(), any(), any())).thenAnswer(call -> stored.stream()
                .filter(i -> i.getDay() == call.getArgument(1) && i.getMeal() == call.getArgument(2)).findFirst());
    }

    private static MealPlan newPlan(long id) throws Exception {
        var constructor = MealPlan.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        MealPlan plan = constructor.newInstance();
        set(plan, "id", id);
        set(plan, "userId", USER);
        set(plan, "weekStart", WEEK);
        return plan;
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private RecipeSummary recipe(long id, String title, RecipeStatus status, boolean snack) {
        RecipeSummary summary = new RecipeSummary(id, title, "cover-" + id, 30, 2, status, snack, DietType.VEGAN);
        catalogue.put(id, summary);
        return summary;
    }

    private MealPlanItem existing(DayOfWeek day, Meal meal, long recipeId, int servings) throws Exception {
        MealPlanItem item = new MealPlanItem(plan, day, meal, recipeId, "Món " + recipeId, servings, ItemSource.MANUAL);
        set(item, "id", 500L + stored.size());
        stored.add(item);
        return item;
    }

    private static PutSlotRequest add(long recipeId) {
        return new PutSlotRequest(recipeId, null, null, null);
    }

    @Test
    void weekWithoutPlanIsSevenEmptyDaysAndCreatesNothing() { // US1-1, research R3
        when(plans.findByUserIdAndWeekStart(USER, WEEK)).thenReturn(Optional.empty());

        WeekPlanResponse week = service.getWeek(USER, WEEK);

        assertThat(week.weekStart()).isEqualTo(WEEK);
        assertThat(week.weekEnd()).isEqualTo(WEEK.plusDays(6));
        assertThat(week.empty()).isTrue();
        assertThat(week.days()).extracting(DayPlanDto::day).containsExactly(DayOfWeek.values());
        assertThat(week.days()).allSatisfy(day -> {
            assertThat(day.slots()).extracting(SlotDto::meal).containsExactly(Meal.values());
            assertThat(day.slots()).allSatisfy(slot -> assertThat(slot.item()).isNull());
        });
        verify(plans, never()).findOrCreate(anyLong(), any());
    }

    @Test
    void todayIsSetOnlyForTheWeekContainingIt() {
        when(plans.findByUserIdAndWeekStart(anyLong(), any())).thenReturn(Optional.empty());

        assertThat(service.getWeek(USER, WEEK).today()).isEqualTo(LocalDate.of(2026, 10, 21));
        assertThat(service.getWeek(USER, WEEK.plusWeeks(1)).today()).isNull();
    }

    @Test
    void weekShowsDishesWithRecipeDataAndUnavailableOnes() throws Exception { // US1-5, FR-020
        recipe(1, "Phở bò", RecipeStatus.PUBLISHED, false);
        recipe(2, "Bò lúc lắc", RecipeStatus.ARCHIVED, false);
        when(plans.findByUserIdAndWeekStart(USER, WEEK)).thenReturn(Optional.of(plan));
        existing(DayOfWeek.MONDAY, Meal.LUNCH, 1, 4);
        existing(DayOfWeek.FRIDAY, Meal.DINNER, 2, 2);
        when(items.findByMealPlanId(100L)).thenReturn(stored);

        WeekPlanResponse week = service.getWeek(USER, WEEK);

        PlanItemDto pho = week.days().get(0).slots().get(1).item();
        assertThat(pho.recipeName()).isEqualTo("Phở bò");
        assertThat(pho.coverImageUrl()).isEqualTo("cover-1");
        assertThat(pho.totalTimeMinutes()).isEqualTo(30);
        assertThat(pho.servings()).isEqualTo(4);
        assertThat(pho.available()).isTrue();
        PlanItemDto archived = week.days().get(4).slots().get(2).item();
        assertThat(archived.available()).isFalse();
        assertThat(archived.recipeName()).isEqualTo("Món 2");
        assertThat(archived.coverImageUrl()).isNull();
        assertThat(week.empty()).isFalse();
        assertThat(week.availableItemCount()).isEqualTo(1);
        verify(recipes).findByIds(Set.of(1L, 2L)); // one call for the whole week
    }

    @Test
    void addingToAnEmptySlotCreatesAManualItem() { // US1-2
        recipe(1, "x".repeat(120), RecipeStatus.PUBLISHED, false);

        PlanItemDto saved = service.putSlot(USER, WEEK, DayOfWeek.TUESDAY, Meal.BREAKFAST, add(1));

        assertThat(saved.source()).isEqualTo(ItemSource.MANUAL);
        assertThat(saved.servings()).isEqualTo(2); // recipe servings, no household size
        MealPlanItem item = stored.getFirst();
        assertThat(item.getRecipeNameSnapshot()).hasSize(100);
        assertThat(item.getDay()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(item.getMeal()).isEqualTo(Meal.BREAKFAST);
    }

    @Test
    void householdSizeIsTheDefaultServings() { // US1-5
        recipe(1, "Phở bò", RecipeStatus.PUBLISHED, false);
        when(profiles.findByUserId(USER)).thenReturn(new PlannerProfile(4, DietType.NONE, null, Set.of(), Set.of(),
                Set.of()));

        assertThat(service.putSlot(USER, WEEK, DayOfWeek.MONDAY, Meal.LUNCH, add(1)).servings()).isEqualTo(4);
        assertThat(service.putSlot(USER, WEEK, DayOfWeek.MONDAY, Meal.DINNER,
                new PutSlotRequest(1L, 6, ItemSource.AI, null)).servings()).isEqualTo(6);
        assertThat(stored.get(1).getSource()).isEqualTo(ItemSource.AI);
    }

    @Test
    void recipesNotPublishedSnacksAndUnknownAreRefused() { // FR-010, spec Assumptions
        recipe(1, "Bản nháp", RecipeStatus.DRAFT, false);
        recipe(2, "Bánh tráng trộn", RecipeStatus.PUBLISHED, true);

        assertThatThrownBy(() -> service.putSlot(USER, WEEK, DayOfWeek.MONDAY, Meal.LUNCH, add(1)))
                .isInstanceOf(RecipeNotAvailableException.class);
        assertThatThrownBy(() -> service.putSlot(USER, WEEK, DayOfWeek.MONDAY, Meal.LUNCH, add(2)))
                .isInstanceOf(RecipeNotAvailableException.class);
        assertThatThrownBy(() -> service.putSlot(USER, WEEK, DayOfWeek.MONDAY, Meal.LUNCH, add(3)))
                .isInstanceOf(RecipeNotAvailableException.class);
        assertThat(stored).isEmpty();
    }

    @Test
    void expectingADishInAnEmptySlotIsSlotChanged() {
        recipe(1, "Phở bò", RecipeStatus.PUBLISHED, false);

        assertThatThrownBy(() -> service.putSlot(USER, WEEK, DayOfWeek.MONDAY, Meal.LUNCH,
                new PutSlotRequest(1L, null, null, new ExpectedSlotState(5L, 0))))
                .isInstanceOf(SlotChangedException.class);
    }

    @Test
    void expectingAnEmptySlotThatHasADishIsSlotOccupied() throws Exception { // US2-3
        recipe(1, "Phở bò", RecipeStatus.PUBLISHED, false);
        recipe(2, "Bún chả", RecipeStatus.PUBLISHED, false);
        MealPlanItem pho = existing(DayOfWeek.MONDAY, Meal.LUNCH, 1, 2);

        assertThatThrownBy(() -> service.putSlot(USER, WEEK, DayOfWeek.MONDAY, Meal.LUNCH, add(2)))
                .isInstanceOfSatisfying(SlotOccupiedException.class, e -> {
                    assertThat(e.currentItem().id()).isEqualTo(pho.getId());
                    assertThat(e.currentItem().recipeName()).isEqualTo("Phở bò");
                });
    }

    @Test
    void replacingKeepsTheServingsAndChecksTheVersion() throws Exception { // US3-4, research R4
        recipe(1, "Phở bò", RecipeStatus.PUBLISHED, false);
        recipe(2, "Bún chả", RecipeStatus.PUBLISHED, false);
        MealPlanItem pho = existing(DayOfWeek.MONDAY, Meal.LUNCH, 1, 5);

        assertThatThrownBy(() -> service.putSlot(USER, WEEK, DayOfWeek.MONDAY, Meal.LUNCH,
                new PutSlotRequest(2L, null, null, new ExpectedSlotState(pho.getId(), 3))))
                .isInstanceOf(SlotChangedException.class);

        PlanItemDto replaced = service.putSlot(USER, WEEK, DayOfWeek.MONDAY, Meal.LUNCH,
                new PutSlotRequest(2L, null, null, new ExpectedSlotState(pho.getId(), 0)));

        assertThat(replaced.id()).isEqualTo(pho.getId());
        assertThat(replaced.recipeName()).isEqualTo("Bún chả");
        assertThat(replaced.servings()).isEqualTo(5);
        assertThat(replaced.version()).isEqualTo(1);
        assertThat(replaced.source()).isEqualTo(ItemSource.MANUAL);
    }

    @Test
    void patchServingsSavesTheNewValue() throws Exception { // US1-7
        recipe(1, "Phở bò", RecipeStatus.PUBLISHED, false);
        MealPlanItem pho = existing(DayOfWeek.MONDAY, Meal.LUNCH, 1, 2);
        when(plans.findByUserIdAndWeekStart(USER, WEEK)).thenReturn(Optional.of(plan));

        PlanItemDto saved = service.patchServings(USER, WEEK, DayOfWeek.MONDAY, Meal.LUNCH,
                new PatchServingsRequest(6, new ExpectedSlotState(pho.getId(), 0)));

        assertThat(saved.servings()).isEqualTo(6);
        assertThat(pho.getServings()).isEqualTo((short) 6);
        assertThatThrownBy(() -> service.patchServings(USER, WEEK, DayOfWeek.MONDAY, Meal.LUNCH,
                new PatchServingsRequest(3, new ExpectedSlotState(pho.getId(), 0))))
                .isInstanceOf(SlotChangedException.class); // version is 1 now
        assertThatThrownBy(() -> service.patchServings(USER, WEEK, DayOfWeek.MONDAY, Meal.DINNER,
                new PatchServingsRequest(3, new ExpectedSlotState(pho.getId(), 1))))
                .isInstanceOf(SlotChangedException.class); // empty slot
    }

    @Test
    void patchServingsWithoutAPlanIsSlotChanged() {
        when(plans.findByUserIdAndWeekStart(USER, WEEK)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.patchServings(USER, WEEK, DayOfWeek.MONDAY, Meal.LUNCH,
                new PatchServingsRequest(3, new ExpectedSlotState(1L, 0))))
                .isInstanceOf(SlotChangedException.class);
    }

    @Test
    void householdChangesDoNotTouchSavedDishes() throws Exception { // FR-013
        recipe(1, "Phở bò", RecipeStatus.PUBLISHED, false);
        existing(DayOfWeek.MONDAY, Meal.LUNCH, 1, 2);
        when(plans.findByUserIdAndWeekStart(USER, WEEK)).thenReturn(Optional.of(plan));
        when(items.findByMealPlanId(100L)).thenReturn(stored);
        when(profiles.findByUserId(USER)).thenReturn(new PlannerProfile(6, DietType.NONE, null, Set.of(), Set.of(),
                Set.of()));

        assertThat(service.getWeek(USER, WEEK).days().get(0).slots().get(1).item().servings()).isEqualTo(2);
    }

    @Test
    void theSameRecipeMayBeInTwoSlots() { // edge case
        recipe(1, "Phở bò", RecipeStatus.PUBLISHED, false);

        service.putSlot(USER, WEEK, DayOfWeek.MONDAY, Meal.LUNCH, add(1));
        service.putSlot(USER, WEEK, DayOfWeek.TUESDAY, Meal.LUNCH, add(1));

        assertThat(stored).hasSize(2).allSatisfy(item -> assertThat(item.getRecipeId()).isEqualTo(1L));
    }
}
