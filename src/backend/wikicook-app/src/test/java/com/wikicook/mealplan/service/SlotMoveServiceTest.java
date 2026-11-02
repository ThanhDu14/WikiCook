package com.wikicook.mealplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wikicook.mealplan.domain.ItemSource;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.MealPlan;
import com.wikicook.mealplan.domain.MealPlanItem;
import com.wikicook.mealplan.dto.ExpectedSlotState;
import com.wikicook.mealplan.dto.MoveRequest;
import com.wikicook.mealplan.dto.MoveResult;
import com.wikicook.mealplan.dto.PlacedItem;
import com.wikicook.mealplan.port.DietaryProfilePort;
import com.wikicook.mealplan.port.RecipeSummaryPort;
import com.wikicook.mealplan.repository.MealPlanItemRepository;
import com.wikicook.mealplan.repository.MealPlanRepository;
import com.wikicook.mealplan.service.exception.SlotChangedException;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** T057: move into an empty slot, swap with a full one, and refuse when the client saw something else (US3). */
class SlotMoveServiceTest {

    private static final long USER = 7;
    private static final LocalDate WEEK = LocalDate.of(2026, 10, 19);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T05:00:00Z"), ZoneOffset.UTC);

    private final MealPlanRepository plans = mock(MealPlanRepository.class);
    private final MealPlanItemRepository items = mock(MealPlanItemRepository.class);
    private final RecipeSummaryPort recipes = mock(RecipeSummaryPort.class);
    private final List<MealPlanItem> stored = new ArrayList<>();
    private SlotMoveService service;
    private MealPlan plan;

    @BeforeEach
    void setUp() throws Exception {
        var constructor = MealPlan.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        plan = constructor.newInstance();
        set(plan, "id", 100L);
        set(plan, "userId", USER);
        set(plan, "weekStart", WEEK);
        when(plans.findByUserIdAndWeekStart(USER, WEEK)).thenReturn(Optional.of(plan));
        when(recipes.findByIds(any())).thenReturn(Map.of());
        when(items.findSlotItemId(any(), any(), any())).thenAnswer(call -> stored.stream()
                .filter(i -> i.getDay() == call.getArgument(1) && i.getMeal() == call.getArgument(2))
                .map(MealPlanItem::getId).findFirst());
        when(items.lockItemsByIdsOrdered(any())).thenAnswer(call -> {
            Collection<Long> ids = call.getArgument(0);
            return stored.stream().filter(i -> ids.contains(i.getId())).toList();
        });
        when(items.saveAndFlush(any())).thenAnswer(call -> {
            MealPlanItem item = call.getArgument(0);
            set(item, "version", item.getVersion() + 1);
            return item;
        });
        MealPlanService dtos = new MealPlanService(plans, items, recipes, mock(DietaryProfilePort.class), CLOCK);
        service = new SlotMoveService(plans, items, dtos, CLOCK);
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private MealPlanItem existing(DayOfWeek day, Meal meal, long recipeId, int servings, ItemSource source)
            throws Exception {
        MealPlanItem item = new MealPlanItem(plan, day, meal, recipeId, "Món " + recipeId, servings, source);
        set(item, "id", 500L + stored.size());
        stored.add(item);
        return item;
    }

    private static MoveRequest move(MealPlanItem from, DayOfWeek day, Meal meal, ExpectedSlotState expected) {
        return new MoveRequest(new MoveRequest.From(from.getDay(), from.getMeal(), from.getId(), from.getVersion()),
                new MoveRequest.To(day, meal, expected));
    }

    private static ExpectedSlotState saw(MealPlanItem item) {
        return new ExpectedSlotState(item.getId(), item.getVersion());
    }

    @Test
    void moveToAnEmptySlotChangesTheSlotOfTheItem() throws Exception { // US3-1
        MealPlanItem pho = existing(DayOfWeek.MONDAY, Meal.LUNCH, 1, 2, ItemSource.AI);

        MoveResult result = service.move(USER, WEEK, move(pho, DayOfWeek.FRIDAY, Meal.DINNER, null));

        assertThat(result.kind()).isEqualTo(MoveResult.Kind.MOVED);
        assertThat(result.items()).singleElement().satisfies(placed -> {
            assertThat(placed.day()).isEqualTo(DayOfWeek.FRIDAY);
            assertThat(placed.meal()).isEqualTo(Meal.DINNER);
            assertThat(placed.item().id()).isEqualTo(pho.getId());
            assertThat(placed.item().source()).isEqualTo(ItemSource.AI);
        });
        assertThat(pho.getDay()).isEqualTo(DayOfWeek.FRIDAY);
        assertThat(pho.getMeal()).isEqualTo(Meal.DINNER);
        verify(items).saveAndFlush(pho);
    }

    @Test
    void moveOntoADishSwapsTheContentAndKeepsTheIds() throws Exception { // US3-2, research R5
        MealPlanItem pho = existing(DayOfWeek.MONDAY, Meal.LUNCH, 1, 2, ItemSource.AI);
        MealPlanItem ca = existing(DayOfWeek.TUESDAY, Meal.DINNER, 2, 5, ItemSource.MANUAL);
        long phoId = pho.getId();
        long caId = ca.getId();

        MoveResult result = service.move(USER, WEEK, move(pho, DayOfWeek.TUESDAY, Meal.DINNER, saw(ca)));

        assertThat(result.kind()).isEqualTo(MoveResult.Kind.SWAPPED);
        assertThat(pho.getId()).isEqualTo(phoId);
        assertThat(pho.getDay()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(pho.getRecipeId()).isEqualTo(2L);
        assertThat(pho.getRecipeNameSnapshot()).isEqualTo("Món 2");
        assertThat(pho.getServings()).isEqualTo((short) 5);
        assertThat(pho.getSource()).isEqualTo(ItemSource.MANUAL);
        assertThat(ca.getId()).isEqualTo(caId);
        assertThat(ca.getRecipeId()).isEqualTo(1L);
        assertThat(ca.getServings()).isEqualTo((short) 2);
        assertThat(ca.getSource()).isEqualTo(ItemSource.AI);
        assertThat(result.items()).extracting(PlacedItem::day, PlacedItem::meal, p -> p.item().recipeId())
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(DayOfWeek.MONDAY, Meal.LUNCH, 2L),
                        org.assertj.core.groups.Tuple.tuple(DayOfWeek.TUESDAY, Meal.DINNER, 1L));
    }

    @Test
    void staleSourceIsRefused() throws Exception {
        MealPlanItem pho = existing(DayOfWeek.MONDAY, Meal.LUNCH, 1, 2, ItemSource.MANUAL);
        MoveRequest request = new MoveRequest(new MoveRequest.From(DayOfWeek.MONDAY, Meal.LUNCH, pho.getId(), 9),
                new MoveRequest.To(DayOfWeek.FRIDAY, Meal.DINNER, null));

        assertThatThrownBy(() -> service.move(USER, WEEK, request)).isInstanceOf(SlotChangedException.class);
        verify(items, never()).saveAndFlush(any());
    }

    @Test
    void sourceNoLongerInThatSlotIsRefused() throws Exception {
        MealPlanItem pho = existing(DayOfWeek.MONDAY, Meal.LUNCH, 1, 2, ItemSource.MANUAL);
        MoveRequest request = new MoveRequest(new MoveRequest.From(DayOfWeek.SUNDAY, Meal.LUNCH, pho.getId(), 0),
                new MoveRequest.To(DayOfWeek.FRIDAY, Meal.DINNER, null));

        assertThatThrownBy(() -> service.move(USER, WEEK, request)).isInstanceOf(SlotChangedException.class);
        verify(items, never()).saveAndFlush(any());
    }

    @Test
    void targetThatGotADishMeanwhileIsRefused() throws Exception {
        MealPlanItem pho = existing(DayOfWeek.MONDAY, Meal.LUNCH, 1, 2, ItemSource.MANUAL);
        existing(DayOfWeek.TUESDAY, Meal.DINNER, 2, 5, ItemSource.MANUAL);

        assertThatThrownBy(() -> service.move(USER, WEEK, move(pho, DayOfWeek.TUESDAY, Meal.DINNER, null)))
                .isInstanceOf(SlotChangedException.class);
        verify(items, never()).saveAndFlush(any());
    }

    @Test
    void targetThatChangedMeanwhileIsRefused() throws Exception {
        MealPlanItem pho = existing(DayOfWeek.MONDAY, Meal.LUNCH, 1, 2, ItemSource.MANUAL);
        MealPlanItem ca = existing(DayOfWeek.TUESDAY, Meal.DINNER, 2, 5, ItemSource.MANUAL);
        ExpectedSlotState stale = new ExpectedSlotState(ca.getId(), ca.getVersion() + 1);

        assertThatThrownBy(() -> service.move(USER, WEEK, move(pho, DayOfWeek.TUESDAY, Meal.DINNER, stale)))
                .isInstanceOf(SlotChangedException.class);
        assertThatThrownBy(() -> service.move(USER, WEEK, move(pho, DayOfWeek.SUNDAY, Meal.DINNER, saw(ca))))
                .isInstanceOf(SlotChangedException.class);
        verify(items, never()).saveAndFlush(any());
    }

    @Test
    void weekWithoutPlanIsRefused() {
        when(plans.findByUserIdAndWeekStart(USER, WEEK)).thenReturn(Optional.empty());
        MoveRequest request = new MoveRequest(new MoveRequest.From(DayOfWeek.MONDAY, Meal.LUNCH, 1L, 0),
                new MoveRequest.To(DayOfWeek.FRIDAY, Meal.DINNER, null));

        assertThatThrownBy(() -> service.move(USER, WEEK, request)).isInstanceOf(SlotChangedException.class);
    }

    @Test
    void moveOntoItselfChangesNothing() throws Exception {
        MealPlanItem pho = existing(DayOfWeek.MONDAY, Meal.LUNCH, 1, 2, ItemSource.MANUAL);

        MoveResult result = service.move(USER, WEEK, move(pho, DayOfWeek.MONDAY, Meal.LUNCH, saw(pho)));

        assertThat(result.kind()).isEqualTo(MoveResult.Kind.MOVED);
        assertThat(result.items()).singleElement().satisfies(p -> assertThat(p.item().version()).isZero());
        verify(items, never()).saveAndFlush(any());
    }
}
