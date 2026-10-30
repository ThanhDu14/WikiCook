package com.wikicook.devstub;

import com.wikicook.aiplanner.repository.EligibleRecipeQuery;
import com.wikicook.common.diet.DietRestrictions;
import com.wikicook.mealplan.api.BatchSaveResult;
import com.wikicook.mealplan.api.DraftItem;
import com.wikicook.mealplan.api.MealPlanWriteFacade;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.SlotKey;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Weeks kept in memory until 030 stores them in {@code meal_plan_items}; lost on restart. */
public class InMemoryMealPlanFacade implements MealPlanWriteFacade {

    private record WeekKey(long userId, LocalDate weekStart) {
    }

    private final EligibleRecipeQuery eligibleQuery;
    private final Map<WeekKey, Map<SlotKey, DraftItem>> weeks = new HashMap<>();

    public InMemoryMealPlanFacade(EligibleRecipeQuery eligibleQuery) {
        this.eligibleQuery = eligibleQuery;
    }

    @Override
    public synchronized Set<SlotKey> emptySlots(long userId, LocalDate weekStart) {
        Map<SlotKey, DraftItem> week = weeks.getOrDefault(new WeekKey(userId, weekStart), Map.of());
        Set<SlotKey> empty = new TreeSet<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            for (Meal meal : Meal.values()) {
                SlotKey slot = new SlotKey(day, meal);
                if (!week.containsKey(slot)) {
                    empty.add(slot);
                }
            }
        }
        return empty;
    }

    @Override
    public synchronized Set<Long> plannedRecipeIds(long userId, LocalDate weekStart) {
        return weeks.getOrDefault(new WeekKey(userId, weekStart), Map.of()).values().stream()
                .map(DraftItem::recipeId).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public synchronized BatchSaveResult saveDraft(long userId, LocalDate weekStart, List<DraftItem> items) {
        if (items.isEmpty()) {
            return new BatchSaveResult(0, 0, 0);
        }
        Set<Long> available = eligibleQuery.stillEligible(
                items.stream().map(DraftItem::recipeId).collect(Collectors.toSet()), DietRestrictions.none());
        Map<SlotKey, DraftItem> week = weeks.computeIfAbsent(new WeekKey(userId, weekStart), key -> new TreeMap<>());
        int saved = 0;
        int occupied = 0;
        int unavailable = 0;
        for (DraftItem item : items) {
            SlotKey slot = new SlotKey(item.day(), item.meal());
            if (week.containsKey(slot)) {
                occupied++;
            } else if (!available.contains(item.recipeId())) {
                unavailable++;
            } else {
                week.put(slot, item);
                saved++;
            }
        }
        return new BatchSaveResult(saved, occupied, unavailable);
    }
}
