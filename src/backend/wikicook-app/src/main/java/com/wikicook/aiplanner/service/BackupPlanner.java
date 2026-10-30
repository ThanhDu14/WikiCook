package com.wikicook.aiplanner.service;

import com.wikicook.aiplanner.domain.Candidate;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.SlotKey;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/**
 * Step 5 of generation (research R8): a draft without AI from the candidates of steps 1–2, so it calls nothing
 * outside and never breaks a hard rule. Greedy over slots in day → meal order, deterministic for the same input.
 */
@Component
public class BackupPlanner {

    static final int PREFERRED_CUISINE = 3;
    static final int MAX_SHARED_INGREDIENTS = 3;
    static final int SAME_CATEGORY_AS_DAY_BEFORE = -2;

    /**
     * @param picks       chosen recipe per slot, in day → meal order
     * @param chooseSlots slots with no candidate left (US6-5), in day → meal order
     */
    public record Plan(Map<SlotKey, Long> picks, List<SlotKey> chooseSlots) {
    }

    public Plan plan(List<SlotKey> slots, Map<Meal, List<Candidate>> candidates, Set<Long> preferredCuisineIds) {
        Map<SlotKey, Long> picks = new TreeMap<>();
        List<SlotKey> chooseSlots = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        Set<Long> chosenIngredients = new HashSet<>();
        Map<DayOfWeek, Set<Long>> categoriesByDay = new EnumMap<>(DayOfWeek.class);
        for (SlotKey slot : new TreeSet<>(slots)) {
            Set<Long> dayBefore = slot.day() == DayOfWeek.MONDAY ? Set.of()
                    : categoriesByDay.getOrDefault(slot.day().minus(1), Set.of());
            Candidate best = null;
            int bestScore = Integer.MIN_VALUE;
            for (Candidate candidate : candidates.getOrDefault(slot.meal(), List.of())) {
                if (used.contains(candidate.recipeId())) {
                    continue;
                }
                int score = score(candidate, preferredCuisineIds, chosenIngredients, dayBefore);
                if (best == null || score > bestScore || score == bestScore && before(candidate, best)) {
                    best = candidate;
                    bestScore = score;
                }
            }
            if (best == null) {
                chooseSlots.add(slot);
                continue;
            }
            picks.put(slot, best.recipeId());
            used.add(best.recipeId());
            chosenIngredients.addAll(best.mainIngredientIds());
            if (best.categoryId() != null) {
                categoriesByDay.computeIfAbsent(slot.day(), day -> new HashSet<>()).add(best.categoryId());
            }
        }
        return new Plan(picks, chooseSlots);
    }

    private static int score(Candidate candidate, Set<Long> preferredCuisineIds, Set<Long> chosenIngredients,
                             Set<Long> categoriesDayBefore) {
        int score = 0;
        if (candidate.cuisineId() != null && preferredCuisineIds.contains(candidate.cuisineId())) {
            score += PREFERRED_CUISINE;
        }
        int shared = (int) candidate.mainIngredientIds().stream().filter(chosenIngredients::contains).count();
        score += Math.min(shared, MAX_SHARED_INGREDIENTS);
        if (candidate.categoryId() != null && categoriesDayBefore.contains(candidate.categoryId())) {
            score += SAME_CATEGORY_AS_DAY_BEFORE;
        }
        return score;
    }

    /** Tie-break: retrieval rank, then id. */
    private static boolean before(Candidate a, Candidate b) {
        return a.rank() != b.rank() ? a.rank() < b.rank() : a.recipeId() < b.recipeId();
    }
}
