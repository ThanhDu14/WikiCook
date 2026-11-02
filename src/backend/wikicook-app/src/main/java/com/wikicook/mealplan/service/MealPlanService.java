package com.wikicook.mealplan.service;

import com.wikicook.mealplan.domain.ItemSource;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.MealPlan;
import com.wikicook.mealplan.domain.MealPlanItem;
import com.wikicook.mealplan.domain.SlotKey;
import com.wikicook.mealplan.dto.AddItemRequest;
import com.wikicook.mealplan.dto.DayNutritionDto;
import com.wikicook.mealplan.dto.DayPlanDto;
import com.wikicook.mealplan.dto.PatchServingsRequest;
import com.wikicook.mealplan.dto.PlacedItem;
import com.wikicook.mealplan.dto.PlanItemDto;
import com.wikicook.mealplan.dto.ReplaceItemRequest;
import com.wikicook.mealplan.dto.SlotDto;
import com.wikicook.mealplan.dto.WeekPlanResponse;
import com.wikicook.mealplan.port.DayNutrition;
import com.wikicook.mealplan.port.DayNutritionPort;
import com.wikicook.mealplan.port.DietaryProfilePort;
import com.wikicook.mealplan.port.RecipeStatus;
import com.wikicook.mealplan.port.RecipeSummary;
import com.wikicook.mealplan.port.RecipeSummaryPort;
import com.wikicook.mealplan.repository.MealPlanItemRepository;
import com.wikicook.mealplan.repository.MealPlanRepository;
import com.wikicook.mealplan.service.exception.MealFullException;
import com.wikicook.mealplan.service.exception.RecipeNotAvailableException;
import com.wikicook.mealplan.service.exception.SlotChangedException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads one user's week and edits it dish by dish, up to 4 dishes per meal (US1, FR-004). Every lookup goes
 * through the caller's own plan, so no request reaches another user's data (FR-006). Writes lock the plan row,
 * then check the version of the dish the client saw (research R4, R5).
 */
@Service
public class MealPlanService {

    private final MealPlanRepository plans;
    private final MealPlanItemRepository items;
    private final RecipeSummaryPort recipes;
    private final DietaryProfilePort profiles;
    private final DayNutritionPort nutrition;
    private final Clock clock;

    public MealPlanService(MealPlanRepository plans, MealPlanItemRepository items, RecipeSummaryPort recipes,
                           DietaryProfilePort profiles, DayNutritionPort nutrition, Clock clock) {
        this.plans = plans;
        this.items = items;
        this.recipes = recipes;
        this.profiles = profiles;
        this.nutrition = nutrition;
        this.clock = clock;
    }

    /** The week, 7 empty days when there is no plan yet; never creates one (research R3). */
    @Transactional(readOnly = true)
    public WeekPlanResponse getWeek(long userId, LocalDate weekStart) {
        List<MealPlanItem> weekItems = plans.findByUserIdAndWeekStart(userId, weekStart)
                .map(plan -> items.findByMealPlanId(plan.getId()))
                .orElse(List.of());
        Map<MealPlanItem, PlanItemDto> dtos = toDtos(weekItems);
        Map<SlotKey, List<PlanItemDto>> byMeal = weekItems.stream()
                .sorted(Comparator.comparingInt(MealPlanItem::getPosition))
                .collect(Collectors.groupingBy(MealPlanItem::slot,
                        Collectors.mapping(dtos::get, Collectors.toList())));

        Map<DayOfWeek, DayNutrition> totals = nutritionOf(byMeal);
        List<DayPlanDto> days = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            List<SlotDto> slots = new ArrayList<>();
            for (Meal meal : Meal.values()) {
                slots.add(new SlotDto(meal, List.copyOf(byMeal.getOrDefault(new SlotKey(day, meal), List.of()))));
            }
            DayNutrition total = totals.get(day);
            days.add(new DayPlanDto(WeekCalendar.dateOf(weekStart, day), day, List.copyOf(slots), total == null ? null
                    : new DayNutritionDto(total.kcalPerPerson(), total.includedDishes(), total.missingDishes())));
        }
        LocalDate today = WeekCalendar.today(clock);
        boolean containsToday = !today.isBefore(weekStart) && !today.isAfter(weekStart.plusDays(6));
        int available = (int) dtos.values().stream().filter(PlanItemDto::available).count();
        return new WeekPlanResponse(weekStart, weekStart.plusDays(6), containsToday ? today : null,
                weekItems.isEmpty(), available, List.copyOf(days));
    }

    /**
     * Calories per day from 090 in one call for the week (T097, FR-032): the available dishes of each day, a
     * recipe twice in a day counted twice; servings play no part. No call when the week has no available dish.
     */
    private Map<DayOfWeek, DayNutrition> nutritionOf(Map<SlotKey, List<PlanItemDto>> byMeal) {
        Map<DayOfWeek, List<Long>> recipeIdsByDay = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            List<Long> ids = new ArrayList<>();
            for (Meal meal : Meal.values()) {
                byMeal.getOrDefault(new SlotKey(day, meal), List.of()).stream()
                        .filter(PlanItemDto::available).forEach(dish -> ids.add(dish.recipeId()));
            }
            if (!ids.isEmpty()) {
                recipeIdsByDay.put(day, List.copyOf(ids));
            }
        }
        return recipeIdsByDay.isEmpty() ? Map.of() : nutrition.totals(recipeIdsByDay);
    }

    /** Adds a dish at the end of a meal (US1-2, US1-11, US2); a meal with 4 dishes is full (FR-004). */
    @Transactional
    public PlacedItem addItem(long userId, LocalDate weekStart, DayOfWeek day, Meal meal, AddItemRequest request) {
        RecipeSummary recipe = availableRecipe(request.recipeId());
        MealPlan plan = plans.findOrCreateLocked(userId, weekStart);
        int count = items.findMeal(plan.getId(), day, meal).size();
        if (count >= Meal.MAX_DISHES) {
            throw new MealFullException();
        }
        int servings = request.servings() != null ? request.servings()
                : ServingsPolicy.defaultServings(profiles.findByUserId(userId), recipe);
        ItemSource source = Objects.requireNonNullElse(request.source(), ItemSource.MANUAL);
        return save(plan, new MealPlanItem(plan, day, meal, count, recipe.id(), recipe.title(), servings, source),
                recipe);
    }

    /** Another recipe in the same place; servings stay unless sent (US3-5, FR-016). */
    @Transactional
    public PlacedItem replaceItem(long userId, LocalDate weekStart, long itemId, ReplaceItemRequest request) {
        RecipeSummary recipe = availableRecipe(request.recipeId());
        MealPlan plan = lockedPlan(userId, weekStart);
        MealPlanItem item = seenItem(plan, itemId, request.version());
        int servings = request.servings() != null ? request.servings() : item.getServings();
        ItemSource source = Objects.requireNonNullElse(request.source(), ItemSource.MANUAL);
        item.setDish(recipe.id(), recipe.title(), servings, source);
        return save(plan, item, recipe);
    }

    /** New servings for one dish (US1-7); other dishes keep theirs (FR-013). */
    @Transactional
    public PlacedItem patchServings(long userId, LocalDate weekStart, long itemId, PatchServingsRequest request) {
        MealPlan plan = lockedPlan(userId, weekStart);
        MealPlanItem item = seenItem(plan, itemId, request.version());
        item.setServings(request.servings());
        return save(plan, item, null);
    }

    /** Removes the dish the client saw (US3-6); the dishes after it in the meal move up (FR-016). */
    @Transactional
    public void removeItem(long userId, LocalDate weekStart, long itemId, int version) {
        MealPlan plan = lockedPlan(userId, weekStart);
        MealPlanItem item = seenItem(plan, itemId, version);
        List<MealPlanItem> rest = new ArrayList<>(items.findMeal(plan.getId(), item.getDay(), item.getMeal()));
        rest.remove(item);
        plan.touch(clock.instant());
        items.delete(item);
        renumber(rest);
        items.flush();
    }

    /** Empties a day (US3-7); the client asked for confirmation, and an empty day or week is fine. */
    @Transactional
    public void clearDay(long userId, LocalDate weekStart, DayOfWeek day) {
        plans.lockByUserIdAndWeekStart(userId, weekStart).ifPresent(plan -> {
            if (items.deleteByMealPlanIdAndDay(plan.getId(), day) > 0) {
                plan.touch(clock.instant());
            }
        });
    }

    /** The caller's plan of the week, locked; none means the dish the client saw is gone. */
    MealPlan lockedPlan(long userId, LocalDate weekStart) {
        return plans.lockByUserIdAndWeekStart(userId, weekStart).orElseThrow(SlotChangedException::new);
    }

    /** The dish of this plan at the version the client saw, or {@link SlotChangedException} (research R4). */
    MealPlanItem seenItem(MealPlan plan, long itemId, int version) {
        MealPlanItem item = items.findByIdAndMealPlanId(itemId, plan.getId()).orElseThrow(SlotChangedException::new);
        if (item.getVersion() != version) {
            throw new SlotChangedException();
        }
        return item;
    }

    /** Gives the dishes positions 0, 1, … in list order. */
    static void renumber(List<MealPlanItem> meal) {
        for (int i = 0; i < meal.size(); i++) {
            meal.get(i).setPosition(i);
        }
    }

    private RecipeSummary availableRecipe(long recipeId) {
        RecipeSummary recipe = recipes.findByIds(Set.of(recipeId)).get(recipeId);
        if (recipe == null || !recipe.available()) {
            throw new RecipeNotAvailableException();
        }
        return recipe;
    }

    private PlacedItem save(MealPlan plan, MealPlanItem item, RecipeSummary recipe) {
        var now = clock.instant();
        plan.touch(now);
        item.touch(now);
        MealPlanItem saved = items.saveAndFlush(item);
        return new PlacedItem(saved.getDay(), saved.getMeal(), recipe != null ? toDto(saved, recipe) : toDto(saved));
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
        return new PlanItemDto(item.getId(), item.getVersion(), item.getPosition(), item.getRecipeId(),
                available ? recipe.title() : item.getRecipeNameSnapshot(),
                available ? recipe.coverImageUrl() : null,
                available ? recipe.totalTimeMinutes() : null,
                item.getServings(), item.getSource(), available,
                available ? recipe.dietType() : null);
    }
}
