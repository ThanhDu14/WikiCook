package com.wikicook.aiplanner.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.aiplanner.domain.Candidate;
import com.wikicook.mealplan.domain.Meal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** T111: a candidate list close to the query but not crowded with one kind of dish. */
class CandidateDiversifierTest {

    private static Candidate c(long id, Long category, Long firstIngredient) {
        List<Long> ingredients = firstIngredient == null ? List.of() : List.of(firstIngredient, 500L + id);
        return new Candidate(id, "Món " + id, Set.of(Meal.LUNCH), 20, 1L, "Việt Nam", category, ingredients,
                List.of(), 0);
    }

    private static List<Long> ids(List<Candidate> candidates) {
        return candidates.stream().map(Candidate::recipeId).toList();
    }

    @Test
    void keepsTheOrderWhenNothingRepeats() {
        List<Candidate> ranked = List.of(c(1, 1L, 101L), c(2, 2L, 102L), c(3, 3L, 103L), c(4, 4L, 104L));

        assertThat(ids(CandidateDiversifier.pick(ranked, 3))).containsExactly(1L, 2L, 3L);
    }

    @Test
    void pushesBackRecipesOfACategoryAlreadyChosen() {
        List<Candidate> ranked = List.of(c(1, 1L, 101L), c(2, 1L, 102L), c(3, 1L, 103L), c(4, 2L, 104L),
                c(5, 3L, 105L));

        assertThat(ids(CandidateDiversifier.pick(ranked, 3))).containsExactly(1L, 4L, 2L);
    }

    @Test
    void pushesBackRecipesWithTheSameFirstIngredient() {
        List<Candidate> ranked = List.of(c(1, 1L, 100L), c(2, 2L, 100L), c(3, 3L, 100L), c(4, 4L, 200L));

        assertThat(ids(CandidateDiversifier.pick(ranked, 2))).containsExactly(1L, 4L);
    }

    @Test
    void missingCategoryOrIngredientsAreNotARepeat() {
        List<Candidate> ranked = List.of(c(1, null, null), c(2, null, null), c(3, 3L, 103L));

        assertThat(ids(CandidateDiversifier.pick(ranked, 2))).containsExactly(1L, 2L);
    }

    @Test
    void returnsEveryCandidateWhenThereAreNoMoreThanK() {
        List<Candidate> ranked = List.of(c(1, 1L, 100L), c(2, 1L, 100L));

        assertThat(ids(CandidateDiversifier.pick(ranked, 5))).containsExactlyInAnyOrder(1L, 2L);
        assertThat(CandidateDiversifier.pick(List.of(), 5)).isEmpty();
    }

    @Test
    void sameInputSameOutput() {
        List<Candidate> ranked = List.of(c(1, 1L, 100L), c(2, 1L, 100L), c(3, 2L, 200L), c(4, 2L, 100L),
                c(5, 3L, 300L), c(6, 1L, 300L));

        assertThat(CandidateDiversifier.pick(ranked, 4)).isEqualTo(CandidateDiversifier.pick(ranked, 4));
    }
}
