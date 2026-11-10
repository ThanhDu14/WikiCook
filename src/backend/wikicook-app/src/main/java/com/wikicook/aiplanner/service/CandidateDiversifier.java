package com.wikicook.aiplanner.service;

import com.wikicook.aiplanner.domain.Candidate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Picks {@code k} candidates from a longer list ranked by closeness, so the LLM is not offered twenty variations of
 * one dish: greedy, each step takes the lowest {@code position + penalty}, where a candidate pays
 * {@value #REPEAT_PENALTY} for each chosen one of the same category and for each with the same first (main)
 * ingredient. A list with no repeats keeps its order. Deterministic.
 */
final class CandidateDiversifier {

    static final int REPEAT_PENALTY = 3;

    private CandidateDiversifier() {
    }

    static List<Candidate> pick(List<Candidate> ranked, int k) {
        List<Candidate> left = new ArrayList<>(ranked);
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            positions.add(i);
        }
        Map<Long, Integer> categories = new HashMap<>();
        Map<Long, Integer> firstIngredients = new HashMap<>();
        List<Candidate> chosen = new ArrayList<>(Math.min(k, ranked.size()));
        while (chosen.size() < k && !left.isEmpty()) {
            int best = 0;
            int bestCost = Integer.MAX_VALUE;
            for (int i = 0; i < left.size(); i++) {
                Candidate candidate = left.get(i);
                int cost = positions.get(i)
                        + REPEAT_PENALTY * count(categories, candidate.categoryId())
                        + REPEAT_PENALTY * count(firstIngredients, firstIngredient(candidate));
                if (cost < bestCost) { // ties: the closer one, as the list is in rank order
                    best = i;
                    bestCost = cost;
                }
            }
            Candidate taken = left.remove(best);
            positions.remove(best);
            chosen.add(taken);
            add(categories, taken.categoryId());
            add(firstIngredients, firstIngredient(taken));
        }
        return chosen;
    }

    private static Long firstIngredient(Candidate candidate) {
        return candidate.mainIngredientIds().isEmpty() ? null : candidate.mainIngredientIds().getFirst();
    }

    private static int count(Map<Long, Integer> counts, Long key) {
        return key == null ? 0 : counts.getOrDefault(key, 0);
    }

    private static void add(Map<Long, Integer> counts, Long key) {
        if (key != null) {
            counts.merge(key, 1, Integer::sum);
        }
    }
}
