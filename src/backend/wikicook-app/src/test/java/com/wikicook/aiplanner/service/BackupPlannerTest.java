package com.wikicook.aiplanner.service;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.TUESDAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.aiplanner.domain.Candidate;
import com.wikicook.aiplanner.service.BackupPlanner.Plan;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.SlotKey;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Step 5 of the pipeline (research R8): a draft without AI, greedy and deterministic. */
class BackupPlannerTest {

    private static final long VIET = 1;
    private static final long JAPAN = 2;
    private final BackupPlanner planner = new BackupPlanner();

    private static Candidate candidate(long id, int rank, long cuisine, long category, Long... ingredients) {
        return new Candidate(id, "Món " + id, Set.of(Meal.LUNCH, Meal.DINNER), 30, cuisine, "c" + cuisine, category,
                List.of(ingredients), List.of(), rank);
    }

    private static SlotKey slot(DayOfWeek day, Meal meal) {
        return new SlotKey(day, meal);
    }

    @Test
    void sameInputSameDraftAndNoRepeatedRecipe() { // US6-1, FR-011
        List<Candidate> lunch = List.of(candidate(10, 0, VIET, 1), candidate(11, 1, VIET, 2), candidate(12, 2, VIET, 3));
        List<SlotKey> slots = List.of(slot(TUESDAY, Meal.LUNCH), slot(MONDAY, Meal.LUNCH), slot(MONDAY, Meal.DINNER));
        Map<Meal, List<Candidate>> candidates = Map.of(Meal.LUNCH, lunch, Meal.DINNER, lunch);

        Plan first = planner.plan(slots, candidates, Set.of());
        Plan second = planner.plan(slots, candidates, Set.of());

        assertThat(first).isEqualTo(second);
        assertThat(first.picks()).containsKeys(slot(MONDAY, Meal.LUNCH), slot(MONDAY, Meal.DINNER), slot(TUESDAY, Meal.LUNCH));
        assertThat(new HashSet<>(first.picks().values())).hasSize(3);
        // Equal scores: retrieval rank decides, Monday lunch first.
        assertThat(first.picks()).containsEntry(slot(MONDAY, Meal.LUNCH), 10L);
    }

    @Test
    void picksOnlyFromTheSlotsMeal() { // US6-1: never a breakfast-only recipe at lunch
        Map<Meal, List<Candidate>> candidates = Map.of(
                Meal.BREAKFAST, List.of(candidate(1, 0, VIET, 1)),
                Meal.LUNCH, List.of(candidate(2, 5, VIET, 1)));

        Plan plan = planner.plan(List.of(slot(MONDAY, Meal.LUNCH), slot(MONDAY, Meal.BREAKFAST)), candidates, Set.of());

        assertThat(plan.picks()).containsEntry(slot(MONDAY, Meal.LUNCH), 2L).containsEntry(slot(MONDAY, Meal.BREAKFAST), 1L);
    }

    @Test
    void preferredCuisineComesFirst() { // FR-008: +3
        Map<Meal, List<Candidate>> candidates = Map.of(Meal.LUNCH,
                List.of(candidate(1, 0, VIET, 1), candidate(2, 9, JAPAN, 2)));

        Plan plan = planner.plan(List.of(slot(MONDAY, Meal.LUNCH)), candidates, Set.of(JAPAN));

        assertThat(plan.picks()).containsEntry(slot(MONDAY, Meal.LUNCH), 2L);
    }

    @Test
    void sharedIngredientsEarnPoints() { // research R8: +1 per shared ingredient, at most +3
        Map<Meal, List<Candidate>> candidates = Map.of(
                Meal.LUNCH, List.of(candidate(1, 0, VIET, 1, 100L, 101L)),
                Meal.DINNER, List.of(candidate(2, 0, VIET, 2, 200L), candidate(3, 1, VIET, 3, 101L)));

        Plan plan = planner.plan(List.of(slot(MONDAY, Meal.LUNCH), slot(MONDAY, Meal.DINNER)), candidates, Set.of());

        assertThat(plan.picks()).containsEntry(slot(MONDAY, Meal.DINNER), 3L);
    }

    @Test
    void sharedIngredientBonusIsCappedAtThree() {
        Map<Meal, List<Candidate>> candidates = Map.of(
                Meal.LUNCH, List.of(candidate(1, 0, VIET, 1, 1L, 2L, 3L, 4L, 5L)),
                // 5 shared (capped to +3) vs preferred cuisine (+3): a tie, so rank decides.
                Meal.DINNER, List.of(candidate(2, 0, JAPAN, 2), candidate(3, 1, VIET, 3, 1L, 2L, 3L, 4L, 5L)));

        Plan plan = planner.plan(List.of(slot(MONDAY, Meal.LUNCH), slot(MONDAY, Meal.DINNER)), candidates,
                Set.of(JAPAN));

        assertThat(plan.picks()).containsEntry(slot(MONDAY, Meal.DINNER), 2L);
    }

    @Test
    void sameCategoryAsTheDayBeforeIsPenalised() { // research R8: −2
        Map<Meal, List<Candidate>> candidates = Map.of(Meal.LUNCH,
                List.of(candidate(1, 0, VIET, 7), candidate(2, 1, VIET, 7), candidate(3, 2, VIET, 8)));

        Plan plan = planner.plan(List.of(slot(MONDAY, Meal.LUNCH), slot(TUESDAY, Meal.LUNCH)), candidates, Set.of());

        assertThat(plan.picks()).containsEntry(slot(MONDAY, Meal.LUNCH), 1L).containsEntry(slot(TUESDAY, Meal.LUNCH), 3L);
    }

    @Test
    void slotWithoutCandidatesIsLeftToChoose() { // US6-5
        Map<Meal, List<Candidate>> candidates = Map.of(Meal.LUNCH, List.of(candidate(1, 0, VIET, 1)));

        Plan plan = planner.plan(List.of(slot(MONDAY, Meal.LUNCH), slot(TUESDAY, Meal.LUNCH), slot(MONDAY, Meal.DINNER)),
                candidates, Set.of());

        assertThat(plan.picks()).containsOnlyKeys(slot(MONDAY, Meal.LUNCH));
        assertThat(plan.chooseSlots()).containsExactly(slot(MONDAY, Meal.DINNER), slot(TUESDAY, Meal.LUNCH));
    }

    @Test
    void fullWeekWith90CandidatesTakesUnder50Ms() { // SC-002a
        List<SlotKey> slots = new ArrayList<>();
        Map<Meal, List<Candidate>> candidates = new EnumMap<>(Meal.class);
        for (Meal meal : Meal.values()) {
            List<Candidate> list = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                long id = meal.ordinal() * 100L + i;
                list.add(candidate(id, i, i % 3, i % 5, (long) i % 7, (long) i % 11, (long) i % 13, (long) i % 17,
                        (long) i % 19));
            }
            candidates.put(meal, list);
            for (DayOfWeek day : DayOfWeek.values()) {
                slots.add(slot(day, meal));
            }
        }
        planner.plan(slots, candidates, Set.of(1L)); // warm-up

        long start = System.nanoTime();
        Plan plan = planner.plan(slots, candidates, Set.of(1L));
        long millis = (System.nanoTime() - start) / 1_000_000;

        assertThat(plan.picks()).hasSize(21);
        assertThat(millis).isLessThan(50);
    }

    @Test
    void avoidsTheMainIngredientOfTheDayBefore_T112() {
        Map<Meal, List<Candidate>> candidates = Map.of(Meal.LUNCH, List.of(
                candidate(1, 0, VIET, 1, 100L), candidate(2, 1, VIET, 2, 100L), candidate(3, 2, VIET, 3, 200L)));

        Plan plan = planner.plan(List.of(slot(MONDAY, Meal.LUNCH), slot(TUESDAY, Meal.LUNCH)), candidates, Set.of());

        assertThat(plan.picks()).containsEntry(slot(MONDAY, Meal.LUNCH), 1L)
                .containsEntry(slot(TUESDAY, Meal.LUNCH), 3L); // not 2: pork again the next day
    }

    @Test
    void seedBreaksTiesAmongCloseRanksAndIsRepeatable_T112() {
        List<Candidate> lunch = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            lunch.add(candidate(10 + i, i, VIET, 1 + i));
        }
        lunch.add(candidate(99, 9, VIET, 99)); // far in the ranking: never wins a tie with the close ones
        Map<Meal, List<Candidate>> candidates = Map.of(Meal.LUNCH, lunch);
        List<SlotKey> monday = List.of(slot(MONDAY, Meal.LUNCH));

        Set<Long> picked = new HashSet<>();
        for (long seed = 1; seed <= 30; seed++) {
            Plan plan = planner.plan(monday, candidates, Set.of(), seed);
            assertThat(plan).isEqualTo(planner.plan(monday, candidates, Set.of(), seed));
            picked.add(plan.picks().get(slot(MONDAY, Meal.LUNCH)));
        }

        assertThat(picked).hasSizeGreaterThan(1).doesNotContain(99L);
    }
}
