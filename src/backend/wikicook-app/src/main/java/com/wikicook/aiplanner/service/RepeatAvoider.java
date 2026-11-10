package com.wikicook.aiplanner.service;

import com.wikicook.mealplan.domain.Meal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Leaves out recipes the user just saw or ate (the draft "Tạo lại" replaces, this week and last week), so a new
 * generation is not the same week again. Soft: a meal keeps them when fewer than {@value #CHOICE_PER_SLOT} recipes
 * per slot would be left, and the "not enough recipes" check always runs on the full list.
 */
final class RepeatAvoider {

    static final int CHOICE_PER_SLOT = 2;

    private RepeatAvoider() {
    }

    static Map<Meal, List<Long>> apply(Map<Meal, List<Long>> eligibleByMeal, Map<Meal, Integer> slotsPerMeal,
                                       Set<Long> avoid) {
        if (avoid.isEmpty()) {
            return eligibleByMeal;
        }
        Map<Meal, List<Long>> result = new EnumMap<>(Meal.class);
        eligibleByMeal.forEach((meal, ids) -> {
            List<Long> fresh = ids.stream().filter(id -> !avoid.contains(id)).toList();
            int needed = CHOICE_PER_SLOT * slotsPerMeal.getOrDefault(meal, 0);
            result.put(meal, fresh.size() >= needed ? fresh : ids);
        });
        return result;
    }
}
