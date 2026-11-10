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
 * outside and never breaks a hard rule. Greedy over slots in day → meal order, deterministic for the same input and
 * seed. Scores: preferred cuisine, ingredients shared with the week (shopping), minus a dish of the same category or
 * the same main (first) ingredient as one of the day before.
 */
@Component
public class BackupPlanner {

    static final int PREFERRED_CUISINE = 3;
    static final int MAX_SHARED_INGREDIENTS = 3;
    static final int SAME_CATEGORY_AS_DAY_BEFORE = -2;
    static final int SAME_MAIN_INGREDIENT_AS_DAY_BEFORE = -2;
    /** With a seed, equal scores within this many ranks are broken by the seed instead of the rank. */
    static final int RANK_BUCKET = 5;

    /**
     * @param picks       chosen recipe per slot, in day → meal order
     * @param chooseSlots slots with no candidate left (US6-5), in day → meal order
     */
    public record Plan(Map<SlotKey, Long> picks, List<SlotKey> chooseSlots) {
    }

    public Plan plan(List<SlotKey> slots, Map<Meal, List<Candidate>> candidates, Set<Long> preferredCuisineIds) {
        return plan(slots, candidates, preferredCuisineIds, null);
    }

    /** @param seed one per generation, so "Tạo lại" does not give the same draft; {@code null}: ties by rank */
    public Plan plan(List<SlotKey> slots, Map<Meal, List<Candidate>> candidates, Set<Long> preferredCuisineIds,
                     Long seed) {
        Map<SlotKey, Long> picks = new TreeMap<>();
        List<SlotKey> chooseSlots = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        Set<Long> chosenIngredients = new HashSet<>();
        Map<DayOfWeek, Set<Long>> categoriesByDay = new EnumMap<>(DayOfWeek.class);
        Map<DayOfWeek, Set<Long>> mainIngredientsByDay = new EnumMap<>(DayOfWeek.class);
        for (SlotKey slot : new TreeSet<>(slots)) {
            boolean monday = slot.day() == DayOfWeek.MONDAY;
            Set<Long> dayBefore = monday ? Set.of() : categoriesByDay.getOrDefault(slot.day().minus(1), Set.of());
            Set<Long> mainsDayBefore = monday ? Set.of()
                    : mainIngredientsByDay.getOrDefault(slot.day().minus(1), Set.of());
            Candidate best = null;
            int bestScore = Integer.MIN_VALUE;
            for (Candidate candidate : candidates.getOrDefault(slot.meal(), List.of())) {
                if (used.contains(candidate.recipeId())) {
                    continue;
                }
                int score = score(candidate, preferredCuisineIds, chosenIngredients, dayBefore, mainsDayBefore);
                if (best == null || score > bestScore || score == bestScore && before(candidate, best, seed)) {
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
            if (!best.mainIngredientIds().isEmpty()) {
                mainIngredientsByDay.computeIfAbsent(slot.day(), day -> new HashSet<>())
                        .add(best.mainIngredientIds().getFirst());
            }
        }
        return new Plan(picks, chooseSlots);
    }

    private static int score(Candidate candidate, Set<Long> preferredCuisineIds, Set<Long> chosenIngredients,
                             Set<Long> categoriesDayBefore, Set<Long> mainIngredientsDayBefore) {
        int score = 0;
        if (candidate.cuisineId() != null && preferredCuisineIds.contains(candidate.cuisineId())) {
            score += PREFERRED_CUISINE;
        }
        int shared = (int) candidate.mainIngredientIds().stream().filter(chosenIngredients::contains).count();
        score += Math.min(shared, MAX_SHARED_INGREDIENTS);
        if (candidate.categoryId() != null && categoriesDayBefore.contains(candidate.categoryId())) {
            score += SAME_CATEGORY_AS_DAY_BEFORE;
        }
        if (!candidate.mainIngredientIds().isEmpty()
                && mainIngredientsDayBefore.contains(candidate.mainIngredientIds().getFirst())) {
            score += SAME_MAIN_INGREDIENT_AS_DAY_BEFORE;
        }
        return score;
    }

    /** Tie-break: retrieval rank, then id; with a seed, rank bucket, then a seeded order of ids. */
    private static boolean before(Candidate a, Candidate b, Long seed) {
        if (seed != null) {
            int bucketA = a.rank() / RANK_BUCKET;
            int bucketB = b.rank() / RANK_BUCKET;
            if (bucketA != bucketB) {
                return bucketA < bucketB;
            }
            long mixA = mix(seed ^ a.recipeId());
            long mixB = mix(seed ^ b.recipeId());
            if (mixA != mixB) {
                return mixA < mixB;
            }
        }
        return a.rank() != b.rank() ? a.rank() < b.rank() : a.recipeId() < b.recipeId();
    }

    /** SplitMix64 finaliser: spreads close ids and seeds far apart. */
    private static long mix(long value) {
        long z = value + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
