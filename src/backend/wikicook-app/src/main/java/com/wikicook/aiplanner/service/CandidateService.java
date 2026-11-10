package com.wikicook.aiplanner.service;

import com.wikicook.ai.AiUnavailableException;
import com.wikicook.ai.embedding.EmbeddingClient;
import com.wikicook.ai.embedding.EmbeddingTask;
import com.wikicook.aiplanner.domain.Candidate;
import com.wikicook.aiplanner.dto.GenerationConstraints;
import com.wikicook.aiplanner.repository.CandidateQuery;
import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.domain.Meal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Builds each meal's candidate list from its eligible recipes (research R5): one query embedding per meal, the
 * nearest {@value #POOL_FACTOR} × {@code k} recipes cut to a varied {@code k} ({@link CandidateDiversifier}), and a
 * share of recipes not embedded yet so new ones still get picked. When the embedding provider fails the list is
 * ranked by SQL instead, and varied the same way; generation never fails here.
 */
@Service
public class CandidateService {

    private static final Logger log = LoggerFactory.getLogger(CandidateService.class);
    static final int MIN_K = 20;
    static final int MAX_TOTAL = 90;
    /** How many more recipes than places are read, so the diversifier has something to choose from. */
    static final int POOL_FACTOR = 2;

    private final CandidateQuery query;
    private final EmbeddingClient embeddingClient;

    public CandidateService(CandidateQuery query, EmbeddingClient embeddingClient) {
        this.query = query;
        this.embeddingClient = embeddingClient;
    }

    /**
     * @param eligibleByMeal output of step 1 ({@code EligibleRecipeQuery})
     * @param slotsPerMeal   empty slots to fill per meal
     * @return candidates per meal, best first, with {@link Candidate#rank()} set
     */
    public Map<Meal, List<Candidate>> candidates(Map<Meal, List<Long>> eligibleByMeal, Map<Meal, Integer> slotsPerMeal,
                                                 GenerationConstraints constraints) {
        Map<Meal, List<Candidate>> result = new EnumMap<>(Meal.class);
        kPerMeal(slotsPerMeal).forEach((meal, k) -> {
            List<Long> ids = eligibleByMeal.getOrDefault(meal, List.of());
            result.put(meal, ids.isEmpty() ? List.of() : ranked(forMeal(meal, ids, k, constraints)));
        });
        return result;
    }

    private List<Candidate> forMeal(Meal meal, List<Long> ids, int k, GenerationConstraints constraints) {
        float[] vector;
        try {
            vector = embeddingClient.embed(queryText(meal, constraints), EmbeddingTask.RETRIEVAL_QUERY);
        } catch (AiUnavailableException e) {
            log.info("Candidate retrieval without vectors for {}: {}", meal, e.getMessage());
            return CandidateDiversifier.pick(query.bySql(ids, constraints.preferredCuisineIds(), POOL_FACTOR * k), k);
        }
        String model = embeddingClient.model();
        List<Candidate> nearest = query.nearest(ids, vector, model, POOL_FACTOR * k);
        List<Candidate> notEmbedded = query.withoutEmbedding(ids, constraints.preferredCuisineIds(), model, k);
        // New recipes get up to a quarter of the places, or every place the vectors could not fill.
        int reserved = Math.min(notEmbedded.size(), Math.max(k / 4, k - Math.min(nearest.size(), k)));
        List<Candidate> merged = new ArrayList<>(CandidateDiversifier.pick(nearest, k - reserved));
        merged.addAll(notEmbedded.subList(0, reserved));
        return merged;
    }

    private static List<Candidate> ranked(List<Candidate> candidates) {
        List<Candidate> ranked = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            ranked.add(candidates.get(i).withRank(i));
        }
        return List.copyOf(ranked);
    }

    /** Names of the preferred cuisines, shown in the prompt as a preference. */
    public List<String> cuisineNames(Set<Long> cuisineIds) {
        return query.cuisineNames(cuisineIds);
    }

    /** {@code k = max(3 × slots, 20)} per meal, trimmed from the largest so the total stays within 90. */
    public static Map<Meal, Integer> kPerMeal(Map<Meal, Integer> slotsPerMeal) {
        Map<Meal, Integer> k = new EnumMap<>(Meal.class);
        slotsPerMeal.forEach((meal, slots) -> k.put(meal, Math.max(3 * slots, MIN_K)));
        int total = k.values().stream().mapToInt(Integer::intValue).sum();
        while (total > MAX_TOTAL) {
            Meal largest = k.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
            k.merge(largest, -1, Integer::sum);
            total--;
        }
        return k;
    }

    /**
     * What a good recipe for this meal looks like, from the constraints only: never the user's name, email,
     * allergies or excluded ingredients (NFR-003).
     */
    public String queryText(Meal meal, GenerationConstraints constraints) {
        StringBuilder text = new StringBuilder("Món ăn ").append(switch (meal) {
            case BREAKFAST -> "sáng";
            case LUNCH -> "trưa";
            case DINNER -> "tối";
        });
        if (constraints.dietType() == DietType.VEGETARIAN || constraints.dietType() == DietType.VEGAN) {
            text.append(", món chay");
        }
        List<String> cuisines = query.cuisineNames(constraints.preferredCuisineIds());
        if (!cuisines.isEmpty()) {
            text.append(", ẩm thực ").append(String.join(", ", cuisines));
        }
        if (constraints.maxCookTimeMinutes() != null) {
            text.append(", nấu dưới ").append(constraints.maxCookTimeMinutes()).append(" phút");
        }
        return text.toString();
    }
}
