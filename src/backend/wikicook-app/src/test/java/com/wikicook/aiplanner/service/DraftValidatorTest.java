package com.wikicook.aiplanner.service;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.TUESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.wikicook.aiplanner.domain.Candidate;
import com.wikicook.aiplanner.domain.SlotKey;
import com.wikicook.aiplanner.repository.EligibleRecipeQuery;
import com.wikicook.aiplanner.service.DraftValidator.Retry;
import com.wikicook.aiplanner.service.DraftValidator.Valid;
import com.wikicook.aiplanner.service.DraftValidator.ValidationResult;
import com.wikicook.common.diet.DietRestrictions;
import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.domain.Meal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Step 4 of the pipeline (research R7): whatever the LLM answers, no unsafe or unknown recipe gets through. */
class DraftValidatorTest {

    private static final SlotKey MON_LUNCH = new SlotKey(MONDAY, Meal.LUNCH);
    private static final SlotKey MON_DINNER = new SlotKey(MONDAY, Meal.DINNER);
    private static final SlotKey TUE_LUNCH = new SlotKey(TUESDAY, Meal.LUNCH);
    private static final List<SlotKey> SLOTS = List.of(MON_LUNCH, MON_DINNER, TUE_LUNCH);
    private static final DietRestrictions NO_PEANUT = new DietRestrictions(Set.of(9L), Set.of(), DietType.NONE);

    private final EligibleRecipeQuery eligibleQuery = mock(EligibleRecipeQuery.class);
    private final DraftValidator validator = new DraftValidator(eligibleQuery);
    private final Set<Long> stillEligible = new HashSet<>(Set.of(1L, 2L, 3L, 4L));
    private Map<Meal, List<Candidate>> candidates;

    @BeforeEach
    void setUp() {
        when(eligibleQuery.stillEligible(anyCollection(), any())).thenAnswer(call -> {
            Collection<Long> ids = call.getArgument(0);
            return ids.stream().filter(stillEligible::contains).collect(java.util.stream.Collectors.toSet());
        });
        candidates = Map.of(
                Meal.LUNCH, List.of(candidate(1, Meal.LUNCH, Meal.DINNER), candidate(2, Meal.LUNCH, Meal.DINNER),
                        candidate(3, Meal.LUNCH, Meal.DINNER)),
                Meal.DINNER, List.of(candidate(1, Meal.LUNCH, Meal.DINNER), candidate(2, Meal.LUNCH, Meal.DINNER),
                        candidate(3, Meal.LUNCH, Meal.DINNER)),
                Meal.BREAKFAST, List.of(candidate(4, Meal.BREAKFAST)));
    }

    private static Candidate candidate(long id, Meal... meals) {
        return new Candidate(id, "Món " + id, Set.of(meals), 30, 1L, "Việt Nam", 1L, List.of(), List.of(), 0);
    }

    private static String answer(Object... dayMealId) {
        StringBuilder json = new StringBuilder("{\"slots\":[");
        for (int i = 0; i < dayMealId.length; i += 3) {
            json.append(i == 0 ? "" : ",").append("{\"day\":\"%s\",\"meal\":\"%s\",\"recipeId\":%s}"
                    .formatted(dayMealId[i], dayMealId[i + 1], dayMealId[i + 2]));
        }
        return json.append("]}").toString();
    }

    private Valid valid(String json) {
        ValidationResult result = validator.validate(json, SLOTS, candidates, NO_PEANUT);
        assertThat(result).isInstanceOf(Valid.class);
        return (Valid) result;
    }

    private Retry retry(String json) {
        ValidationResult result = validator.validate(json, SLOTS, candidates, NO_PEANUT);
        assertThat(result).isInstanceOf(Retry.class);
        return (Retry) result;
    }

    @Test
    void cleanAnswerKeepsEveryPick() { // US1-1
        Valid result = valid(answer("MONDAY", "LUNCH", 1, "MONDAY", "DINNER", 2, "TUESDAY", "LUNCH", 3));

        assertThat(result.picks()).containsExactlyInAnyOrderEntriesOf(Map.of(MON_LUNCH, 1L, MON_DINNER, 2L, TUE_LUNCH, 3L));
        assertThat(result.chooseSlots()).isEmpty();
        assertThat(result.removedPicks()).isZero();
    }

    @Test
    void idOutsideTheCandidatesIsDropped() { // US3-1: unknown id, or a candidate of another meal
        Valid result = valid(answer("MONDAY", "LUNCH", 999, "MONDAY", "DINNER", 4, "TUESDAY", "LUNCH", 3));

        assertThat(result.picks()).containsOnlyKeys(TUE_LUNCH);
        assertThat(result.chooseSlots()).containsExactly(MON_LUNCH, MON_DINNER);
        assertThat(result.removedPicks()).isEqualTo(2);
    }

    @Test
    void recipeNoLongerPublishedOrNowUnsafeIsDropped() { // US3-1, US3-2: second check in the DB
        stillEligible.remove(2L);

        Valid result = valid(answer("MONDAY", "LUNCH", 1, "MONDAY", "DINNER", 2, "TUESDAY", "LUNCH", 3));

        assertThat(result.picks()).containsOnlyKeys(MON_LUNCH, TUE_LUNCH);
        assertThat(result.chooseSlots()).containsExactly(MON_DINNER);
        assertThat(result.removedPicks()).isEqualTo(1);
        org.mockito.Mockito.verify(eligibleQuery).stillEligible(anyCollection(), eq(NO_PEANUT));
    }

    @Test
    void duplicateRecipeKeepsTheFirstByDayThenMeal() { // FR-011
        // Listed out of order on purpose: Tuesday first, yet Monday dinner keeps the recipe.
        Valid result = valid(answer("TUESDAY", "LUNCH", 1, "MONDAY", "DINNER", 1, "MONDAY", "LUNCH", 2));

        assertThat(result.picks()).containsExactlyInAnyOrderEntriesOf(Map.of(MON_LUNCH, 2L, MON_DINNER, 1L));
        assertThat(result.chooseSlots()).containsExactly(TUE_LUNCH);
        assertThat(result.removedPicks()).isEqualTo(1);
    }

    @Test
    void missingSlotAsksForRetry() { // research R7
        Retry result = retry(answer("MONDAY", "LUNCH", 1, "MONDAY", "DINNER", 2));

        assertThat(result.reason()).contains("Thiếu ô").contains("TUESDAY LUNCH");
    }

    @Test
    void extraOrUnrequestedOrRepeatedSlotAsksForRetry() {
        assertThat(retry(answer("MONDAY", "LUNCH", 1, "MONDAY", "DINNER", 2, "TUESDAY", "LUNCH", 3,
                "TUESDAY", "DINNER", 1)).reason()).contains("Thừa ô").contains("TUESDAY DINNER");
        assertThat(retry(answer("MONDAY", "LUNCH", 1, "MONDAY", "LUNCH", 2, "MONDAY", "DINNER", 2,
                "TUESDAY", "LUNCH", 3)).reason()).contains("Trùng ô").contains("MONDAY LUNCH");
    }

    @Test
    void brokenJsonAsksForRetry() {
        assertThat(retry("not json").reason()).isEqualTo(DraftValidator.INVALID_JSON);
        assertThat(retry("{\"slots\":[{\"day\":\"FUNDAY\",\"meal\":\"LUNCH\",\"recipeId\":1}]}").reason())
                .isEqualTo(DraftValidator.INVALID_JSON);
        assertThat(retry("{\"slots\":[{\"day\":\"MONDAY\",\"meal\":\"LUNCH\"}]}").reason())
                .isEqualTo(DraftValidator.INVALID_JSON);
        assertThat(retry("{}").reason()).isEqualTo(DraftValidator.INVALID_JSON);
        assertThat(retry(null).reason()).isEqualTo(DraftValidator.INVALID_JSON);
    }
}
