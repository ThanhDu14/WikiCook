package com.wikicook.aiplanner.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.mealplan.domain.Meal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

/** T110: recipes just suggested or eaten leave the candidates while each meal keeps enough choice. */
class RepeatAvoiderTest {

    private static List<Long> ids(long from, long to) {
        return LongStream.rangeClosed(from, to).boxed().toList();
    }

    @Test
    void dropsAvoidedRecipesWhenTwiceTheSlotsAreLeft() {
        Map<Meal, List<Long>> result = RepeatAvoider.apply(Map.of(Meal.LUNCH, ids(1, 10)), Map.of(Meal.LUNCH, 3),
                Set.of(1L, 2L, 99L));

        assertThat(result.get(Meal.LUNCH)).containsExactlyElementsOf(ids(3, 10));
    }

    @Test
    void keepsEverythingWhenTooLittleChoiceWouldBeLeft() {
        Map<Meal, List<Long>> result = RepeatAvoider.apply(Map.of(Meal.LUNCH, ids(1, 7)), Map.of(Meal.LUNCH, 3),
                Set.of(1L, 2L));

        assertThat(result.get(Meal.LUNCH)).containsExactlyElementsOf(ids(1, 7)); // 5 left < 2 × 3
    }

    @Test
    void decidesForEachMealOnItsOwn() {
        Map<Meal, List<Long>> result = RepeatAvoider.apply(
                Map.of(Meal.BREAKFAST, ids(1, 4), Meal.DINNER, ids(11, 20)),
                Map.of(Meal.BREAKFAST, 2, Meal.DINNER, 2), Set.of(1L, 11L));

        assertThat(result.get(Meal.BREAKFAST)).containsExactlyElementsOf(ids(1, 4));
        assertThat(result.get(Meal.DINNER)).containsExactlyElementsOf(ids(12, 20));
    }

    @Test
    void nothingToAvoidChangesNothing() {
        Map<Meal, List<Long>> eligible = Map.of(Meal.LUNCH, ids(1, 3));

        assertThat(RepeatAvoider.apply(eligible, Map.of(Meal.LUNCH, 1), Set.of())).isEqualTo(eligible);
    }
}
