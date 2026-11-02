package com.wikicook.mealplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wikicook.mealplan.domain.ItemSource;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.MealPlan;
import com.wikicook.mealplan.domain.MealPlanItem;
import com.wikicook.mealplan.dto.MealItems;
import com.wikicook.mealplan.dto.MoveItemRequest;
import com.wikicook.mealplan.dto.MoveResult;
import com.wikicook.mealplan.dto.PlanItemDto;
import com.wikicook.mealplan.port.DietaryProfilePort;
import com.wikicook.mealplan.port.RecipeSummaryPort;
import com.wikicook.mealplan.repository.MealPlanItemRepository;
import com.wikicook.mealplan.repository.MealPlanRepository;
import com.wikicook.mealplan.service.exception.MealFullException;
import com.wikicook.mealplan.service.exception.SlotChangedException;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** T112 (replaces T057): move a dish to another meal or reorder it inside one (US3, FR-014, FR-015). */
class ItemMoveServiceTest {

    private static final long USER = 7;
    private static final LocalDate WEEK = LocalDate.of(2026, 10, 19);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T05:00:00Z"), ZoneOffset.UTC);

    private final MealPlanRepository plans = mock(MealPlanRepository.class);
    private final MealPlanItemRepository items = mock(MealPlanItemRepository.class);
    private final RecipeSummaryPort recipes = mock(RecipeSummaryPort.class);
    private final List<MealPlanItem> stored = new ArrayList<>();
    private ItemMoveService service;
    private MealPlan plan;

    @BeforeEach
    void setUp() throws Exception {
        var constructor = MealPlan.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        plan = constructor.newInstance();
        set(plan, "id", 100L);
        set(plan, "userId", USER);
        set(plan, "weekStart", WEEK);
        when(plans.lockByUserIdAndWeekStart(USER, WEEK)).thenReturn(Optional.of(plan));
        when(recipes.findByIds(any())).thenReturn(Map.of());
        when(items.findByIdAndMealPlanId(anyLong(), any())).thenAnswer(call -> stored.stream()
                .filter(i -> i.getId().equals(call.<Long>getArgument(0))).findFirst());
        when(items.findMeal(any(), any(), any())).thenAnswer(call -> stored.stream()
                .filter(i -> i.getDay() == call.getArgument(1) && i.getMeal() == call.getArgument(2))
                .sorted(Comparator.comparingInt(MealPlanItem::getPosition)).toList());
        MealPlanService dtos = new MealPlanService(plans, items, recipes, mock(DietaryProfilePort.class), CLOCK);
        service = new ItemMoveService(items, dtos, CLOCK);
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private MealPlanItem dish(DayOfWeek day, Meal meal, String name, ItemSource source) throws Exception {
        int position = (int) stored.stream().filter(i -> i.getDay() == day && i.getMeal() == meal).count();
        MealPlanItem item = new MealPlanItem(plan, day, meal, position, 1L, name, 2, source);
        set(item, "id", 500L + stored.size());
        stored.add(item);
        return item;
    }

    private MealPlanItem dish(DayOfWeek day, Meal meal, String name) throws Exception {
        return dish(day, meal, name, ItemSource.MANUAL);
    }

    private static List<String> names(MealItems meal) {
        return meal.items().stream().map(PlanItemDto::recipeName).toList();
    }

    private MoveResult move(MealPlanItem item, DayOfWeek day, Meal meal, Integer position) {
        return service.move(USER, WEEK, item.getId(), new MoveItemRequest(day, meal, position, item.getVersion()));
    }

    @Test
    void movingToAnotherMealGoesToItsEndAndTheSourceCloses() throws Exception { // US3-1
        MealPlanItem canh = dish(DayOfWeek.MONDAY, Meal.LUNCH, "Canh chua");
        MealPlanItem ca = dish(DayOfWeek.MONDAY, Meal.LUNCH, "Cá kho");
        dish(DayOfWeek.TUESDAY, Meal.DINNER, "Bún chả");

        MoveResult result = move(canh, DayOfWeek.TUESDAY, Meal.DINNER, null);

        assertThat(result.meals()).hasSize(2);
        assertThat(result.meals().get(0).day()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(names(result.meals().get(0))).containsExactly("Cá kho");
        assertThat(result.meals().get(1).meal()).isEqualTo(Meal.DINNER);
        assertThat(names(result.meals().get(1))).containsExactly("Bún chả", "Canh chua");
        assertThat(ca.getPosition()).isZero();
        assertThat(canh.getDay()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(canh.getPosition()).isEqualTo(1);
    }

    @Test
    void movingToAPositionPushesTheOthersDown() throws Exception { // US3-2
        MealPlanItem canh = dish(DayOfWeek.MONDAY, Meal.LUNCH, "Canh chua");
        dish(DayOfWeek.TUESDAY, Meal.DINNER, "Bún chả");
        dish(DayOfWeek.TUESDAY, Meal.DINNER, "Rau muống");

        MoveResult result = move(canh, DayOfWeek.TUESDAY, Meal.DINNER, 0);

        assertThat(names(result.meals().get(0))).isEmpty();
        assertThat(names(result.meals().get(1))).containsExactly("Canh chua", "Bún chả", "Rau muống");
        assertThat(result.meals().get(1).items()).extracting(PlanItemDto::position).containsExactly(0, 1, 2);
    }

    @Test
    void aPositionPastTheEndMeansTheEnd() throws Exception {
        MealPlanItem canh = dish(DayOfWeek.MONDAY, Meal.LUNCH, "Canh chua");
        dish(DayOfWeek.TUESDAY, Meal.DINNER, "Bún chả");

        MoveResult result = move(canh, DayOfWeek.TUESDAY, Meal.DINNER, 3);

        assertThat(names(result.meals().get(1))).containsExactly("Bún chả", "Canh chua");
        assertThat(canh.getPosition()).isEqualTo(1);
    }

    @Test
    void reorderingInsideAMealReturnsThatMealOnly() throws Exception { // US3-4 "Lên trên" / "Xuống dưới"
        dish(DayOfWeek.MONDAY, Meal.DINNER, "Canh chua");
        MealPlanItem ca = dish(DayOfWeek.MONDAY, Meal.DINNER, "Cá kho");
        dish(DayOfWeek.MONDAY, Meal.DINNER, "Rau muống");

        MoveResult up = move(ca, DayOfWeek.MONDAY, Meal.DINNER, 0);

        assertThat(up.meals()).hasSize(1);
        assertThat(names(up.meals().getFirst())).containsExactly("Cá kho", "Canh chua", "Rau muống");

        MoveResult last = move(ca, DayOfWeek.MONDAY, Meal.DINNER, null);
        assertThat(names(last.meals().getFirst())).containsExactly("Canh chua", "Rau muống", "Cá kho");
    }

    @Test
    void aFullMealAcceptsNothingAndNothingChanges() throws Exception { // FR-004, NFR-005
        MealPlanItem canh = dish(DayOfWeek.MONDAY, Meal.LUNCH, "Canh chua");
        for (int i = 0; i < 4; i++) {
            dish(DayOfWeek.TUESDAY, Meal.DINNER, "Món " + i);
        }

        assertThatThrownBy(() -> move(canh, DayOfWeek.TUESDAY, Meal.DINNER, null))
                .isInstanceOf(MealFullException.class);
        assertThat(canh.getDay()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(canh.getPosition()).isZero();
        verify(items, never()).saveAndFlush(any());
    }

    @Test
    void aFullMealCanStillBeReordered() throws Exception {
        List<MealPlanItem> dinner = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            dinner.add(dish(DayOfWeek.TUESDAY, Meal.DINNER, "Món " + i));
        }

        MoveResult result = move(dinner.get(3), DayOfWeek.TUESDAY, Meal.DINNER, 0);

        assertThat(names(result.meals().getFirst())).containsExactly("Món 3", "Món 0", "Món 1", "Món 2");
    }

    @Test
    void aVersionTheClientDidNotSeeIsSlotChanged() throws Exception { // research R4
        MealPlanItem canh = dish(DayOfWeek.MONDAY, Meal.LUNCH, "Canh chua");

        assertThatThrownBy(() -> service.move(USER, WEEK, canh.getId(),
                new MoveItemRequest(DayOfWeek.TUESDAY, Meal.DINNER, null, 4)))
                .isInstanceOf(SlotChangedException.class);
        assertThatThrownBy(() -> service.move(USER, WEEK, 9999L,
                new MoveItemRequest(DayOfWeek.TUESDAY, Meal.DINNER, null, 0)))
                .isInstanceOf(SlotChangedException.class);
        when(plans.lockByUserIdAndWeekStart(USER, WEEK)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> move(canh, DayOfWeek.TUESDAY, Meal.DINNER, null))
                .isInstanceOf(SlotChangedException.class);
        assertThat(canh.getDay()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    void theDishKeepsItsSourceAndServings() throws Exception { // FR-015
        MealPlanItem ai = dish(DayOfWeek.MONDAY, Meal.LUNCH, "Canh chua", ItemSource.AI);

        move(ai, DayOfWeek.SUNDAY, Meal.BREAKFAST, null);

        assertThat(ai.getSource()).isEqualTo(ItemSource.AI);
        assertThat(ai.getServings()).isEqualTo((short) 2);
        assertThat(ai.getRecipeNameSnapshot()).isEqualTo("Canh chua");
    }
}
