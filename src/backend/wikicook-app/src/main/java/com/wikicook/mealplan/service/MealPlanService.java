package com.wikicook.mealplan.service;

import com.wikicook.mealplan.domain.ItemSource;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.MealPlan;
import com.wikicook.mealplan.domain.MealPlanItem;
import com.wikicook.mealplan.domain.SlotKey;
import com.wikicook.mealplan.dto.DayPlanDto;
import com.wikicook.mealplan.dto.ExpectedSlotState;
import com.wikicook.mealplan.dto.PatchServingsRequest;
import com.wikicook.mealplan.dto.PlanItemDto;
import com.wikicook.mealplan.dto.PutSlotRequest;
import com.wikicook.mealplan.dto.SlotDto;
import com.wikicook.mealplan.dto.WeekPlanResponse;
import com.wikicook.mealplan.port.DietaryProfilePort;
import com.wikicook.mealplan.port.RecipeStatus;
import com.wikicook.mealplan.port.RecipeSummary;
import com.wikicook.mealplan.port.RecipeSummaryPort;
import com.wikicook.mealplan.repository.MealPlanItemRepository;
import com.wikicook.mealplan.repository.MealPlanRepository;
import com.wikicook.mealplan.service.exception.RecipeNotAvailableException;
import com.wikicook.mealplan.service.exception.SlotChangedException;
import com.wikicook.mealplan.service.exception.SlotOccupiedException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads and edits one user's week (US1). Every lookup goes through the caller's own plan, so no request reaches
 * another user's data (FR-006). Writes check the slot state the client saw (research R4).
 */
@Service
public class MealPlanService {

    private final MealPlanRepository plans;
    private final MealPlanItemRepository items;
    private final RecipeSummaryPort recipes;
    private final DietaryProfilePort profiles;
    private final Clock clock;

    public MealPlanService(MealPlanRepository plans, MealPlanItemRepository items, RecipeSummaryPort recipes,
                           DietaryProfilePort profiles, Clock clock) {
        this.plans = plans;
        this.items = items;
        this.recipes = recipes;
        this.profiles = profiles;
        this.clock = clock;
    }

    /** The week, 7 empty days when there is no plan yet; never creates one (research R3). */
    @Transactional(readOnly = true)
    public WeekPlanResponse getWeek(long userId, LocalDate weekStart) {
        List<MealPlanItem> weekItems = plans.findByUserIdAndWeekStart(userId, weekStart)
                .map(plan -> items.findByMealPlanId(plan.getId()))
                .orElse(List.of());
        Map<SlotKey, PlanItemDto> bySlot = toDtos(weekItems).entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().slot(), Map.Entry::getValue));

        List<DayPlanDto> days = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            List<SlotDto> slots = new ArrayList<>();
            for (Meal meal : Meal.values()) {
                slots.add(new SlotDto(meal, bySlot.get(new SlotKey(day, meal))));
            }
            days.add(new DayPlanDto(WeekCalendar.dateOf(weekStart, day), day, List.copyOf(slots), null));
        }
        LocalDate today = WeekCalendar.today(clock);
        boolean containsToday = !today.isBefore(weekStart) && !today.isAfter(weekStart.plusDays(6));
        int available = (int) bySlot.values().stream().filter(PlanItemDto::available).count();
        return new WeekPlanResponse(weekStart, weekStart.plusDays(6), containsToday ? today : null, bySlot.isEmpty(),
                available, List.copyOf(days));
    }

    /** Adds a dish to an empty slot or replaces the one there (US1-2, US2, US3-4). */
    @Transactional
    public PlanItemDto putSlot(long userId, LocalDate weekStart, DayOfWeek day, Meal meal, PutSlotRequest request) {
        RecipeSummary recipe = recipes.findByIds(Set.of(request.recipeId())).get(request.recipeId());
        if (recipe == null || !recipe.available()) {
            throw new RecipeNotAvailableException();
        }
        MealPlan plan = plans.findOrCreate(userId, weekStart);
        ItemSource source = Objects.requireNonNullElse(request.source(), ItemSource.MANUAL);
        Optional<MealPlanItem> current = items.findSlot(plan.getId(), day, meal);
        MealPlanItem item;
        if (current.isEmpty()) {
            if (request.expected() != null) {
                throw new SlotChangedException();
            }
            int servings = request.servings() != null ? request.servings()
                    : ServingsPolicy.defaultServings(profiles.findByUserId(userId), recipe);
            item = new MealPlanItem(plan, day, meal, recipe.id(), recipe.title(), servings, source);
        } else {
            item = current.get();
            if (request.expected() == null) {
                throw new SlotOccupiedException(toDto(item));
            }
            requireExpected(item, request.expected());
            int servings = request.servings() != null ? request.servings() : item.getServings();
            item.setDish(recipe.id(), recipe.title(), servings, source);
        }
        return save(plan, item, recipe);
    }

    /** New servings for the dish of a slot (US1-7); other dishes keep theirs (FR-013). */
    @Transactional
    public PlanItemDto patchServings(long userId, LocalDate weekStart, DayOfWeek day, Meal meal,
                                     PatchServingsRequest request) {
        MealPlan plan = plans.findByUserIdAndWeekStart(userId, weekStart).orElseThrow(SlotChangedException::new);
        MealPlanItem item = items.findSlot(plan.getId(), day, meal).orElseThrow(SlotChangedException::new);
        requireExpected(item, request.expected());
        item.setServings(request.servings());
        return save(plan, item, null);
    }

    /** Removes the dish the client saw from a slot (US3-5); a dish changed or gone meanwhile is a 409. */
    @Transactional
    public void removeSlot(long userId, LocalDate weekStart, DayOfWeek day, Meal meal, ExpectedSlotState expected) {
        MealPlan plan = plans.findByUserIdAndWeekStart(userId, weekStart).orElseThrow(SlotChangedException::new);
        MealPlanItem item = items.findSlot(plan.getId(), day, meal).orElseThrow(SlotChangedException::new);
        requireExpected(item, expected);
        plan.touch(clock.instant());
        items.delete(item);
        items.flush();
    }

    /** Empties a day (US3-6); the client asked for confirmation, and an empty day or week is fine. */
    @Transactional
    public void clearDay(long userId, LocalDate weekStart, DayOfWeek day) {
        plans.findByUserIdAndWeekStart(userId, weekStart).ifPresent(plan -> {
            if (items.deleteByMealPlanIdAndDay(plan.getId(), day) > 0) {
                plan.touch(clock.instant());
            }
        });
    }

    static void requireExpected(MealPlanItem item, ExpectedSlotState expected) {
        if (expected == null || !Objects.equals(expected.itemId(), item.getId())
                || !Objects.equals(expected.version(), item.getVersion())) {
            throw new SlotChangedException();
        }
    }

    private PlanItemDto save(MealPlan plan, MealPlanItem item, RecipeSummary recipe) {
        var now = clock.instant();
        plan.touch(now);
        item.touch(now);
        MealPlanItem saved = items.saveAndFlush(item);
        return recipe != null ? toDto(saved, recipe) : toDto(saved);
    }

    /** Item → DTO, reading its recipe through the port. */
    PlanItemDto toDto(MealPlanItem item) {
        return toDtos(List.of(item)).get(item);
    }

    /** Items → DTOs with one port call for all of them (T040). */
    Map<MealPlanItem, PlanItemDto> toDtos(Collection<MealPlanItem> list) {
        Set<Long> ids = list.stream().map(MealPlanItem::getRecipeId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, RecipeSummary> summaries = ids.isEmpty() ? Map.of() : recipes.findByIds(ids);
        return list.stream().collect(Collectors.toMap(Function.identity(),
                item -> toDto(item, item.getRecipeId() == null ? null : summaries.get(item.getRecipeId()))));
    }

    /** Unavailable (deleted or not PUBLISHED): snapshot name, no picture or link (FR-020, research R8). */
    static PlanItemDto toDto(MealPlanItem item, RecipeSummary recipe) {
        boolean available = recipe != null && recipe.status() == RecipeStatus.PUBLISHED;
        return new PlanItemDto(item.getId(), item.getVersion(), item.getRecipeId(),
                available ? recipe.title() : item.getRecipeNameSnapshot(),
                available ? recipe.coverImageUrl() : null,
                available ? recipe.totalTimeMinutes() : null,
                item.getServings(), item.getSource(), available,
                available ? recipe.dietType() : null);
    }
}
