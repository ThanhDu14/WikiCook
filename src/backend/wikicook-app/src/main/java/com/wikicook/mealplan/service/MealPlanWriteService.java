package com.wikicook.mealplan.service;

import com.wikicook.mealplan.api.BatchSaveResult;
import com.wikicook.mealplan.api.DraftItem;
import com.wikicook.mealplan.api.MealPlanWriteFacade;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.MealPlan;
import com.wikicook.mealplan.domain.MealPlanItem;
import com.wikicook.mealplan.domain.SlotKey;
import com.wikicook.mealplan.port.DietaryProfilePort;
import com.wikicook.mealplan.port.PlannerProfile;
import com.wikicook.mealplan.port.RecipeSummary;
import com.wikicook.mealplan.port.RecipeSummaryPort;
import com.wikicook.mealplan.repository.MealPlanItemRepository;
import com.wikicook.mealplan.repository.MealPlanRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link MealPlanWriteFacade} for the AI planner (031) on the 030 tables (T100, contracts/integration.md 2.2).
 * A "slot" here is a meal {@code (day, meal)}; 031 fills only meals with no dish and saves whole meals.
 */
@Service
public class MealPlanWriteService implements MealPlanWriteFacade {

    private final MealPlanRepository plans;
    private final MealPlanItemRepository items;
    private final RecipeSummaryPort recipes;
    private final DietaryProfilePort profiles;
    private final Clock clock;

    public MealPlanWriteService(MealPlanRepository plans, MealPlanItemRepository items, RecipeSummaryPort recipes,
                                DietaryProfilePort profiles, Clock clock) {
        this.plans = plans;
        this.items = items;
        this.recipes = recipes;
        this.profiles = profiles;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<SlotKey> emptySlots(long userId, LocalDate weekStart) {
        Set<SlotKey> taken = weekItems(userId, weekStart).stream().map(MealPlanItem::slot).collect(Collectors.toSet());
        Set<SlotKey> empty = new TreeSet<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            for (Meal meal : Meal.values()) {
                SlotKey slot = new SlotKey(day, meal);
                if (!taken.contains(slot)) {
                    empty.add(slot);
                }
            }
        }
        return empty;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> plannedRecipeIds(long userId, LocalDate weekStart) {
        return weekItems(userId, weekStart).stream().map(MealPlanItem::getRecipeId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Saves the draft in one transaction under the week's plan lock, like every 030 write (research R5). Dishes of
     * one meal keep their list order as positions; a meal that already has a dish is skipped as a whole group; a
     * recipe no longer available is skipped and the next dish takes its place; dishes past the fourth are dropped.
     */
    @Override
    @Transactional
    public BatchSaveResult saveDraft(long userId, LocalDate weekStart, List<DraftItem> draft) {
        if (draft.isEmpty()) {
            return new BatchSaveResult(0, 0, 0);
        }
        MealPlan plan = plans.findOrCreateLocked(userId, weekStart);
        Set<SlotKey> taken = items.findByMealPlanId(plan.getId()).stream().map(MealPlanItem::slot)
                .collect(Collectors.toSet());
        Map<Long, RecipeSummary> summaries = recipes.findByIds(
                draft.stream().map(DraftItem::recipeId).collect(Collectors.toSet()));
        PlannerProfile profile = profiles.findByUserId(userId);
        Map<SlotKey, List<DraftItem>> byMeal = draft.stream().collect(Collectors.groupingBy(
                item -> new SlotKey(item.day(), item.meal()), LinkedHashMap::new, Collectors.toList()));

        Instant now = clock.instant();
        List<MealPlanItem> toSave = new ArrayList<>();
        int occupied = 0;
        int unavailable = 0;
        for (Map.Entry<SlotKey, List<DraftItem>> meal : byMeal.entrySet()) {
            if (taken.contains(meal.getKey())) {
                occupied += meal.getValue().size();
                continue;
            }
            int position = 0;
            for (DraftItem dish : meal.getValue()) {
                RecipeSummary recipe = summaries.get(dish.recipeId());
                if (recipe == null || !recipe.available()) {
                    unavailable++;
                } else if (position < Meal.MAX_DISHES) {
                    int servings = dish.servings() != null ? dish.servings()
                            : ServingsPolicy.defaultServings(profile, recipe);
                    MealPlanItem item = new MealPlanItem(plan, dish.day(), dish.meal(), position++, recipe.id(),
                            recipe.title(), servings, dish.source());
                    item.touch(now);
                    toSave.add(item);
                }
            }
        }
        if (!toSave.isEmpty()) {
            plan.touch(now);
            items.saveAllAndFlush(toSave);
        }
        return new BatchSaveResult(toSave.size(), occupied, unavailable);
    }

    private List<MealPlanItem> weekItems(long userId, LocalDate weekStart) {
        return plans.findByUserIdAndWeekStart(userId, weekStart)
                .map(plan -> items.findByMealPlanId(plan.getId()))
                .orElse(List.of());
    }
}
