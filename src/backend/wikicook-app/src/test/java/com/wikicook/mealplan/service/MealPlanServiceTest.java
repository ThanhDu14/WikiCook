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
import com.wikicook.mealplan.dto.PatchServingsRequest;
import com.wikicook.mealplan.dto.PlanItemDto;
import com.wikicook.mealplan.dto.AddItemRequest;
import com.wikicook.mealplan.dto.PlacedItem;
import com.wikicook.mealplan.dto.ReplaceItemRequest;
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
import com.wikicook.mealplan.service.exception.MealFullException;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** T111 (was T030): reading a week and editing it dish by dish, up to 4 per meal (US1, FR-004). */
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
        when(plans.findOrCreateLocked(USER, WEEK)).thenReturn(plan);
        when(plans.lockByUserIdAndWeekStart(USER, WEEK)).thenReturn(Optional.of(plan));
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
        when(items.findMeal(any(), any(), any())).thenAnswer(call -> stored.stream()
                .filter(i -> i.getDay() == call.getArgument(1) && i.getMeal() == call.getArgument(2))
                .sorted(Comparator.comparingInt(MealPlanItem::getPosition)).toList());
        when(items.findByIdAndMealPlanId(anyLong(), any())).thenAnswer(call -> stored.stream()
                .filter(i -> i.getId().equals(call.<Long>getArgument(0))).findFirst());
        org.mockito.Mockito.doAnswer(call -> stored.remove(call.<MealPlanItem>getArgument(0))).when(items)
                .delete(any());
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
        int position = (int) stored.stream().filter(i -> i.getDay() == day && i.getMeal() == meal).count();
        MealPlanItem item = new MealPlanItem(plan, day, meal, position, recipeId, "Món " + recipeId, servings,
                ItemSource.MANUAL);
        set(item, "id", 500L + stored.size());
        stored.add(item);
        return item;
    }

    private static AddItemRequest add(long recipeId) {
        return new AddItemRequest(recipeId, null, null);
    }

    private PlacedItem add(DayOfWeek day, Meal meal, long recipeId) {
        return service.addItem(USER, WEEK, day, meal, add(recipeId));
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
            assertThat(day.slots()).allSatisfy(slot -> assertThat(slot.items()).isEmpty());
        });
        verify(plans, never()).findOrCreateLocked(anyLong(), any());
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

        PlanItemDto pho = week.days().get(0).slots().get(1).items().getFirst();
        assertThat(pho.recipeName()).isEqualTo("Phở bò");
        assertThat(pho.coverImageUrl()).isEqualTo("cover-1");
        assertThat(pho.totalTimeMinutes()).isEqualTo(30);
        assertThat(pho.servings()).isEqualTo(4);
        assertThat(pho.available()).isTrue();
        PlanItemDto archived = week.days().get(4).slots().get(2).items().getFirst();
        assertThat(archived.available()).isFalse();
        assertThat(archived.recipeName()).isEqualTo("Món 2");
        assertThat(archived.coverImageUrl()).isNull();
        assertThat(week.empty()).isFalse();
        assertThat(week.availableItemCount()).isEqualTo(1);
        verify(recipes).findByIds(Set.of(1L, 2L)); // one call for the whole week
    }

    @Test
    void aMealListsItsDishesByPosition() throws Exception { // US1-11
        recipe(1, "Canh chua", RecipeStatus.PUBLISHED, false);
        recipe(2, "Cá kho", RecipeStatus.PUBLISHED, false);
        recipe(3, "Rau muống xào", RecipeStatus.PUBLISHED, false);
        when(plans.findByUserIdAndWeekStart(USER, WEEK)).thenReturn(Optional.of(plan));
        existing(DayOfWeek.MONDAY, Meal.DINNER, 1, 2);
        existing(DayOfWeek.MONDAY, Meal.DINNER, 2, 2);
        existing(DayOfWeek.MONDAY, Meal.DINNER, 3, 2);
        when(items.findByMealPlanId(100L)).thenReturn(List.of(stored.get(2), stored.get(0), stored.get(1)));

        List<PlanItemDto> dinner = service.getWeek(USER, WEEK).days().get(0).slots().get(2).items();

        assertThat(dinner).extracting(PlanItemDto::recipeName).containsExactly("Canh chua", "Cá kho",
                "Rau muống xào");
        assertThat(dinner).extracting(PlanItemDto::position).containsExactly(0, 1, 2);
    }

    @Test
    void addingToAnEmptyMealCreatesAManualFirstDish() { // US1-2
        recipe(1, "x".repeat(120), RecipeStatus.PUBLISHED, false);

        PlacedItem saved = add(DayOfWeek.TUESDAY, Meal.BREAKFAST, 1);

        assertThat(saved.day()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(saved.meal()).isEqualTo(Meal.BREAKFAST);
        assertThat(saved.item().source()).isEqualTo(ItemSource.MANUAL);
        assertThat(saved.item().servings()).isEqualTo(2); // recipe servings, no household size
        assertThat(saved.item().position()).isZero();
        assertThat(stored.getFirst().getRecipeNameSnapshot()).hasSize(100);
        verify(plans).findOrCreateLocked(USER, WEEK); // plan row locked: two adds cannot both take position 3
    }

    @Test
    void addingGoesToTheEndOfTheMealUpToFourDishes() { // US1-11, FR-004
        for (long id = 1; id <= 5; id++) {
            recipe(id, "Món " + id, RecipeStatus.PUBLISHED, false);
        }
        for (long id = 1; id <= 4; id++) {
            assertThat(add(DayOfWeek.MONDAY, Meal.LUNCH, id).item().position()).isEqualTo((int) id - 1);
        }

        assertThatThrownBy(() -> add(DayOfWeek.MONDAY, Meal.LUNCH, 5)).isInstanceOf(MealFullException.class);
        assertThat(stored).hasSize(4);
        assertThat(add(DayOfWeek.MONDAY, Meal.DINNER, 5).item().position()).isZero(); // other meals are separate
    }

    @Test
    void householdSizeIsTheDefaultServings() { // US1-5
        recipe(1, "Phở bò", RecipeStatus.PUBLISHED, false);
        when(profiles.findByUserId(USER)).thenReturn(new PlannerProfile(4, DietType.NONE, null, Set.of(), Set.of(),
                Set.of()));

        assertThat(add(DayOfWeek.MONDAY, Meal.LUNCH, 1).item().servings()).isEqualTo(4);
        assertThat(service.addItem(USER, WEEK, DayOfWeek.MONDAY, Meal.DINNER,
                new AddItemRequest(1L, 6, ItemSource.AI)).item().servings()).isEqualTo(6);
        assertThat(stored.get(1).getSource()).isEqualTo(ItemSource.AI);
    }

    @Test
    void recipesNotPublishedSnacksAndUnknownAreRefused() { // FR-010, spec Assumptions
        recipe(1, "Bản nháp", RecipeStatus.DRAFT, false);
        recipe(2, "Bánh tráng trộn", RecipeStatus.PUBLISHED, true);

        assertThatThrownBy(() -> add(DayOfWeek.MONDAY, Meal.LUNCH, 1)).isInstanceOf(RecipeNotAvailableException.class);
        assertThatThrownBy(() -> add(DayOfWeek.MONDAY, Meal.LUNCH, 2)).isInstanceOf(RecipeNotAvailableException.class);
        assertThatThrownBy(() -> add(DayOfWeek.MONDAY, Meal.LUNCH, 3)).isInstanceOf(RecipeNotAvailableException.class);
        assertThat(stored).isEmpty();
    }

    @Test
    void replacingKeepsServingsAndPositionAndChecksTheVersion() throws Exception { // US3-5, research R4
        recipe(1, "Phở bò", RecipeStatus.PUBLISHED, false);
        recipe(2, "Bún chả", RecipeStatus.PUBLISHED, false);
        existing(DayOfWeek.MONDAY, Meal.LUNCH, 1, 2);
        MealPlanItem second = existing(DayOfWeek.MONDAY, Meal.LUNCH, 1, 5);

        assertThatThrownBy(() -> service.replaceItem(USER, WEEK, second.getId(),
                new ReplaceItemRequest(2L, null, null, 3)))
                .isInstanceOf(SlotChangedException.class);

        PlacedItem replaced = service.replaceItem(USER, WEEK, second.getId(),
                new ReplaceItemRequest(2L, null, null, 0));

        assertThat(replaced.item().id()).isEqualTo(second.getId());
        assertThat(replaced.item().recipeName()).isEqualTo("Bún chả");
        assertThat(replaced.item().servings()).isEqualTo(5);
        assertThat(replaced.item().position()).isEqualTo(1);
        assertThat(replaced.item().version()).isEqualTo(1);
        assertThat(replaced.item().source()).isEqualTo(ItemSource.MANUAL);
    }

    @Test
    void replacingWithAnUnavailableRecipeIsRefused() throws Exception {
        recipe(1, "Phở bò", RecipeStatus.PUBLISHED, false);
        MealPlanItem pho = existing(DayOfWeek.MONDAY, Meal.LUNCH, 1, 2);

        assertThatThrownBy(() -> service.replaceItem(USER, WEEK, pho.getId(), new ReplaceItemRequest(9L, null, null,
                0))).isInstanceOf(RecipeNotAvailableException.class);
    }

    @Test
    void anItemOutsideTheCallersWeekIsSlotChanged() { // FR-006: another user's id looks like a vanished dish
        recipe(1, "Phở bò", RecipeStatus.PUBLISHED, false);

        assertThatThrownBy(() -> service.replaceItem(USER, WEEK, 4242L, new ReplaceItemRequest(1L, null, null, 0)))
                .isInstanceOf(SlotChangedException.class);
        assertThatThrownBy(() -> service.removeItem(USER, WEEK, 4242L, 0)).isInstanceOf(SlotChangedException.class);

        when(plans.lockByUserIdAndWeekStart(USER, WEEK)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.patchServings(USER, WEEK, 4242L, new PatchServingsRequest(3, 0)))
                .isInstanceOf(SlotChangedException.class);
    }

    @Test
    void patchServingsSavesTheNewValue() throws Exception { // US1-7
        recipe(1, "Phở bò", RecipeStatus.PUBLISHED, false);
        MealPlanItem pho = existing(DayOfWeek.MONDAY, Meal.LUNCH, 1, 2);

        PlacedItem saved = service.patchServings(USER, WEEK, pho.getId(), new PatchServingsRequest(6, 0));

        assertThat(saved.item().servings()).isEqualTo(6);
        assertThat(pho.getServings()).isEqualTo((short) 6);
        assertThatThrownBy(() -> service.patchServings(USER, WEEK, pho.getId(), new PatchServingsRequest(3, 0)))
                .isInstanceOf(SlotChangedException.class); // version is 1 now
    }

    @Test
    void removingADishMovesTheLaterOnesUp() throws Exception { // FR-016
        recipe(1, "Món 1", RecipeStatus.PUBLISHED, false);
        MealPlanItem first = existing(DayOfWeek.MONDAY, Meal.DINNER, 1, 2);
        MealPlanItem second = existing(DayOfWeek.MONDAY, Meal.DINNER, 1, 2);
        MealPlanItem third = existing(DayOfWeek.MONDAY, Meal.DINNER, 1, 2);

        assertThatThrownBy(() -> service.removeItem(USER, WEEK, first.getId(), 7))
                .isInstanceOf(SlotChangedException.class);
        service.removeItem(USER, WEEK, first.getId(), 0);

        assertThat(stored).containsExactly(second, third);
        assertThat(second.getPosition()).isZero();
        assertThat(third.getPosition()).isEqualTo(1);
    }

    @Test
    void clearingADayDeletesItsDishes() {
        when(plans.findByUserIdAndWeekStart(USER, WEEK)).thenReturn(Optional.of(plan));
        when(items.deleteByMealPlanIdAndDay(100L, DayOfWeek.MONDAY)).thenReturn(3);

        service.clearDay(USER, WEEK, DayOfWeek.MONDAY);

        verify(items).deleteByMealPlanIdAndDay(100L, DayOfWeek.MONDAY);
    }

    @Test
    void householdChangesDoNotTouchSavedDishes() throws Exception { // FR-013
        recipe(1, "Phở bò", RecipeStatus.PUBLISHED, false);
        existing(DayOfWeek.MONDAY, Meal.LUNCH, 1, 2);
        when(plans.findByUserIdAndWeekStart(USER, WEEK)).thenReturn(Optional.of(plan));
        when(items.findByMealPlanId(100L)).thenReturn(stored);
        when(profiles.findByUserId(USER)).thenReturn(new PlannerProfile(6, DietType.NONE, null, Set.of(), Set.of(),
                Set.of()));

        assertThat(service.getWeek(USER, WEEK).days().get(0).slots().get(1).items().getFirst().servings())
                .isEqualTo(2);
    }

    @Test
    void theSameRecipeMayBeInTwoMealsOrTwiceInOne() { // edge case
        recipe(1, "Phở bò", RecipeStatus.PUBLISHED, false);

        add(DayOfWeek.MONDAY, Meal.LUNCH, 1);
        add(DayOfWeek.MONDAY, Meal.LUNCH, 1);
        add(DayOfWeek.TUESDAY, Meal.LUNCH, 1);

        assertThat(stored).hasSize(3).allSatisfy(item -> assertThat(item.getRecipeId()).isEqualTo(1L));
    }
}
