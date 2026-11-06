package com.wikicook.aiplanner.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.ai.testing.FakeEmbeddingClient;
import com.wikicook.aiplanner.domain.Candidate;
import com.wikicook.aiplanner.dto.GenerationConstraints;
import com.wikicook.aiplanner.service.CandidateService;
import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.aiplanner.support.RecipeSeed;
import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.support.AbstractPostgresIT;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** Step 2 of the pipeline (research R5): nearest recipes per meal among the eligible ones. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CandidateQueryIT extends AbstractPostgresIT {

    @Autowired
    CandidateQuery candidateQuery;

    @Autowired
    EligibleRecipeQuery eligibleQuery;

    @Autowired
    RecipeEmbeddingRepository embeddings;

    @Autowired
    JdbcTemplate jdbc;

    private final FakeEmbeddingClient embeddingClient = new FakeEmbeddingClient(768);
    private CandidateService service;
    private RecipeSeed.Seeded seed;
    /** Breakfast recipes from nearest to farthest from the query vector. */
    private List<Long> breakfastByDistance;

    @BeforeEach
    void seed() {
        RecipeFixtures.clear(jdbc);
        seed = RecipeSeed.seed(new RecipeFixtures(jdbc));
        service = new CandidateService(candidateQuery, embeddingClient);
        // Angle grows as the id falls, so "nearest" is the reverse of id order.
        List<Long> breakfast = new ArrayList<>(seed.breakfast());
        breakfast.addAll(seed.allDay());
        breakfast.sort(Comparator.reverseOrder());
        for (int i = 0; i < breakfast.size(); i++) {
            embeddings.upsert(breakfast.get(i), angle(i * 0.05), FakeEmbeddingClient.MODEL, "h".repeat(64));
        }
        breakfastByDistance = breakfast;
    }

    private static float[] angle(double radians) {
        float[] v = new float[768];
        v[0] = (float) Math.cos(radians);
        v[1] = (float) Math.sin(radians);
        return v;
    }

    private static GenerationConstraints constraints(Set<Long> preferredCuisines) {
        return new GenerationConstraints(Set.of(), Set.of(), DietType.NONE, null, null, preferredCuisines);
    }

    private Map<Meal, List<Candidate>> breakfast(int slots, GenerationConstraints constraints) {
        Map<Meal, List<Long>> eligible = eligibleQuery.eligibleByMeal(EnumSet.of(Meal.BREAKFAST),
                constraints.restrictions(), constraints.maxCookTimeMinutes());
        embeddingClient.vector(service.queryText(Meal.BREAKFAST, constraints), 1, 0);
        return service.candidates(eligible, Map.of(Meal.BREAKFAST, slots), constraints);
    }

    @Test
    void takesTheNearestKPerMeal() { // US1-2, research R5: k = max(3 × slots, 20)
        List<Candidate> sevenSlots = breakfast(7, constraints(Set.of())).get(Meal.BREAKFAST);
        List<Candidate> oneSlot = breakfast(1, constraints(Set.of())).get(Meal.BREAKFAST);

        assertThat(sevenSlots).extracting(Candidate::recipeId).containsExactlyElementsOf(breakfastByDistance.subList(0, 21));
        assertThat(sevenSlots).extracting(Candidate::rank).startsWith(0, 1, 2);
        assertThat(oneSlot).hasSize(20);
        Candidate first = sevenSlots.getFirst();
        assertThat(first.meals()).contains(Meal.BREAKFAST);
        assertThat(first.cuisineName()).isNotBlank();
        assertThat(first.mainIngredientNames()).hasSize(first.mainIngredientIds().size()).isNotEmpty();
    }

    @Test
    void recipesWithoutEmbeddingAreAddedAtTheEnd() { // US8-1, FR-029
        long fresh = breakfastByDistance.get(25);
        embeddings.deleteByRecipeId(fresh);

        List<Candidate> result = breakfast(7, constraints(Set.of())).get(Meal.BREAKFAST);

        assertThat(result).hasSize(21);
        assertThat(result.getLast().recipeId()).isEqualTo(fresh);
        assertThat(result.subList(0, 20)).extracting(Candidate::recipeId).doesNotContain(fresh);
    }

    @Test
    void onlyEligibleRecipesAreCandidates() { // NFR-004: retrieval never widens step 1
        var noPeanut = new GenerationConstraints(Set.of(seed.peanutAllergen()), Set.of(), DietType.NONE, null, null,
                Set.of());

        List<Candidate> result = breakfast(7, noPeanut).get(Meal.BREAKFAST);

        assertThat(result).extracting(Candidate::recipeId).doesNotContainAnyElementsOf(seed.withPeanut())
                .doesNotContain(seed.snack(), seed.archived());
    }

    @Test
    void embeddingFailureFallsBackToSqlOrder() { // US6-2: no vector step, no exception
        embeddingClient.failing(true);

        List<Candidate> result = breakfast(7, constraints(Set.of(seed.japanese()))).get(Meal.BREAKFAST);

        assertThat(result).hasSize(21);
        // Preferred cuisine first, then rating, then id.
        int lastJapanese = 0;
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i).cuisineId() == seed.japanese()) {
                lastJapanese = i;
            }
        }
        assertThat(result.subList(0, lastJapanese + 1)).allSatisfy(c -> assertThat(c.cuisineId()).isEqualTo(seed.japanese()));
        assertThat(result.subList(lastJapanese + 1, result.size()))
                .isSortedAccordingTo(Comparator.comparingDouble(this::rating).reversed());
    }

    @Test
    void queryTextHasNoPersonalData() { // NFR-003
        var constraints = new GenerationConstraints(Set.of(seed.peanutAllergen()), Set.of(seed.peanut()),
                DietType.VEGAN, 4, 30, Set.of(seed.vietnamese(), seed.japanese()));

        String text = service.queryText(Meal.BREAKFAST, constraints);

        assertThat(text).contains("sáng").contains("Việt Nam").contains("Nhật Bản").contains("30 phút")
                .doesNotContain(String.valueOf(seed.peanutAllergen())).doesNotContain("Đậu phộng");
    }

    @Test
    void oneQueryEmbeddingPerMeal() { // research R5: at most 3 embeds per generation
        GenerationConstraints constraints = constraints(Set.of());
        Map<Meal, List<Long>> eligible = eligibleQuery.eligibleByMeal(EnumSet.allOf(Meal.class),
                constraints.restrictions(), null);
        Map<Meal, Integer> slots = new EnumMap<>(Map.of(Meal.BREAKFAST, 7, Meal.LUNCH, 7, Meal.DINNER, 7));

        Map<Meal, List<Candidate>> result = service.candidates(eligible, slots, constraints);

        assertThat(embeddingClient.texts()).hasSize(3);
        assertThat(result.values().stream().mapToInt(List::size).sum()).isLessThanOrEqualTo(90);
    }

    @Test
    void kIsCappedSoTheTotalStaysWithin90() {
        Map<Meal, Integer> k = CandidateService.kPerMeal(Map.of(Meal.BREAKFAST, 20, Meal.LUNCH, 20, Meal.DINNER, 20));

        assertThat(k.values().stream().mapToInt(Integer::intValue).sum()).isLessThanOrEqualTo(90);
        assertThat(CandidateService.kPerMeal(Map.of(Meal.LUNCH, 3))).containsEntry(Meal.LUNCH, 20);
    }

    private double rating(Candidate candidate) {
        return jdbc.queryForObject("SELECT rating_avg FROM recipes WHERE id = ?", Double.class, candidate.recipeId());
    }
}
