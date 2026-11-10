package com.wikicook.aiplanner.service;

import static com.wikicook.aiplanner.service.GenerationFixture.MON_BREAKFAST;
import static com.wikicook.aiplanner.service.GenerationFixture.MON_LUNCH;
import static com.wikicook.aiplanner.service.GenerationFixture.REQUESTED;
import static com.wikicook.aiplanner.service.GenerationFixture.ROW;
import static com.wikicook.aiplanner.service.GenerationFixture.USER;
import static com.wikicook.aiplanner.service.GenerationFixture.WEEK;
import static com.wikicook.aiplanner.service.GenerationFixture.constraints;
import static com.wikicook.aiplanner.service.GenerationFixture.request;
import static com.wikicook.aiplanner.service.GenerationFixture.validReply;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.wikicook.ai.quota.AiFeature;
import com.wikicook.ai.quota.AiOutcome;
import com.wikicook.ai.quota.AiTrigger;
import com.wikicook.ai.quota.UserAllowance;
import com.wikicook.ai.testing.FakeLlmClient;
import com.wikicook.aiplanner.controller.GenerationInProgressException;
import com.wikicook.aiplanner.controller.NoEmptySlotException;
import com.wikicook.aiplanner.controller.NotEnoughRecipesException;
import com.wikicook.aiplanner.controller.PastWeekException;
import com.wikicook.aiplanner.controller.RateLimitedException;
import com.wikicook.aiplanner.domain.RelaxSuggestion;
import com.wikicook.aiplanner.dto.DraftItemDto;
import com.wikicook.aiplanner.dto.DraftMode;
import com.wikicook.aiplanner.dto.DraftResponse;
import com.wikicook.aiplanner.dto.GenerateRequest;
import com.wikicook.aiplanner.dto.GenerationConstraints;
import com.wikicook.common.diet.DietRestrictions;
import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.domain.ItemSource;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.SlotKey;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** T031: the AI path of a generation (research R4–R7, data-model 4). */
class GenerationServiceTest {

    private final GenerationFixture f = new GenerationFixture();
    private final FakeLlmClient llm = new FakeLlmClient();
    private final GenerationService service = f.service(llm);

    @Test
    void stepsRunInOrder_filterRetrieveLlmCheck() {
        llm.thenReply(validReply());

        service.generate(USER, WEEK, request());

        InOrder order = inOrder(f.quota, f.eligible, f.candidateService);
        order.verify(f.quota).reserve(eq(AiFeature.MEAL_PLANNER), eq(AiTrigger.USER), eq(USER), eq(null), eq(WEEK),
                eq(5), any(GenerationConstraints.class));
        order.verify(f.eligible).eligibleByMeal(eq(EnumSet.allOf(Meal.class)), eq(constraints(4).restrictions()),
                eq(null));
        order.verify(f.candidateService).candidates(anyMap(), eq(Map.of(Meal.BREAKFAST, 2, Meal.LUNCH, 1,
                Meal.DINNER, 2)), eq(constraints(4)));
        order.verify(f.eligible).stillEligible(anyCollection(), eq(constraints(4).restrictions()));
        order.verify(f.quota).finish(ROW, AiOutcome.AI_DRAFT, true, 0, null);
        assertThat(llm.requests()).hasSize(1);
    }

    @Test
    void fillsOnlyTheSelectedEmptySlots_US1_5_FR_007() {
        llm.thenReply(validReply());

        DraftResponse draft = service.generate(USER, WEEK, request());

        assertThat(draft.items()).extracting(item -> new SlotKey(item.day(), item.meal()))
                .containsExactlyElementsOf(REQUESTED)
                .doesNotContain(MON_LUNCH);
        assertThat(draft.chooseSlots()).isEmpty();
        // The prompt never asks for the slot the user planned.
        assertThat(llm.requests().getFirst().userPrompt()).doesNotContain("MONDAY | LUNCH");
    }

    @Test
    void draftItemsAreAiWithTheRequestedServings_US1_5() {
        llm.thenReply(validReply());
        GenerateRequest request = request();

        DraftResponse draft = service.generate(USER, WEEK, request);

        assertThat(draft.mode()).isEqualTo(DraftMode.AI);
        assertThat(draft.notice()).isNull();
        assertThat(draft.requestId()).isEqualTo(request.requestId());
        assertThat(draft.weekStart()).isEqualTo(WEEK);
        assertThat(draft.removedPicks()).isZero();
        assertThat(draft.items().getFirst()).isEqualTo(new DraftItemDto(DayOfWeek.MONDAY, Meal.BREAKFAST, 1,
                "Món 1", null, 20, 4, ItemSource.AI, DietType.NONE));
        assertThat(draft.items()).allSatisfy(item -> {
            assertThat(item.source()).isEqualTo(ItemSource.AI);
            assertThat(item.servings()).isEqualTo(4);
        });
    }

    @Test
    void servingsAsInTheRecipeStayNull() {
        llm.thenReply(validReply());

        DraftResponse draft = service.generate(USER, WEEK, request(constraints(null)));

        assertThat(draft.items()).allSatisfy(item -> assertThat(item.servings()).isNull());
    }

    @Test
    void draftHasNoDuplicateAndMealsMatch_US1_6() {
        llm.thenReply(validReply());

        DraftResponse draft = service.generate(USER, WEEK, request());

        assertThat(new HashSet<>(draft.items().stream().map(DraftItemDto::recipeId).toList())).hasSize(5);
        assertThat(draft.items()).filteredOn(item -> item.meal() == Meal.BREAKFAST)
                .allSatisfy(item -> assertThat(item.recipeId()).isBetween(1L, 5L));
    }

    @Test
    void constraintsOfTheDialogAreNotWrittenToTheProfile_US1_3_FR_004() {
        llm.thenReply(validReply());

        service.generate(USER, WEEK, request());

        verifyNoInteractions(f.profiles);
    }

    @Test
    void remainingAfterTheGenerationIsReturned() {
        llm.thenReply(validReply());
        when(f.quota.userAllowance(USER)).thenReturn(new UserAllowance(5, null), new UserAllowance(4, null));

        assertThat(service.generate(USER, WEEK, request()).remaining()).isEqualTo(4);
    }

    @Test
    void finishedGenerationIsKeptForCancelAndConfirm_T072() {
        llm.thenReply(validReply());
        GenerateRequest request = request();

        service.generate(USER, WEEK, request);

        assertThat(f.inFlight.finished(USER, request.requestId())).hasValueSatisfying(finished -> {
            assertThat(finished.requestRowId()).isEqualTo(ROW);
            assertThat(finished.outcome()).isEqualTo(AiOutcome.AI_DRAFT);
        });
    }

    @Test
    void notEnoughIsKeptWithItsOutcome_T072() {
        when(f.eligible.eligibleByMeal(anySet(), any(), any())).thenReturn(Map.of(
                Meal.BREAKFAST, List.of(1L), Meal.LUNCH, List.of(11L), Meal.DINNER, List.of(21L, 22L)));
        GenerateRequest request = request();

        assertThatThrownBy(() -> service.generate(USER, WEEK, request)).isInstanceOf(NotEnoughRecipesException.class);
        assertThat(f.inFlight.finished(USER, request.requestId()))
                .hasValueSatisfying(finished -> assertThat(finished.outcome()).isEqualTo(AiOutcome.NOT_ENOUGH));
    }

    @Test
    void pastWeekIsRejectedBeforeAnything() {
        assertThatThrownBy(() -> service.generate(USER, WEEK.minusWeeks(2), request()))
                .isInstanceOf(PastWeekException.class);
        verifyNoInteractions(f.quota, f.eligible);
    }

    @Test
    void noEmptySelectedSlotIsRejected() {
        GenerateRequest mondayLunch = new GenerateRequest(UUID.randomUUID(), Set.of(DayOfWeek.MONDAY),
                Set.of(Meal.LUNCH), constraints(2));

        assertThatThrownBy(() -> service.generate(USER, WEEK, mondayLunch)).isInstanceOf(NoEmptySlotException.class);
        verify(f.quota, never()).reserve(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void noHourlyGenerationLeftIsRejected_US5_2() {
        Instant next = Instant.parse("2026-10-28T02:30:00Z");
        when(f.quota.userAllowance(USER)).thenReturn(new UserAllowance(0, next));

        assertThatThrownBy(() -> service.generate(USER, WEEK, request()))
                .isInstanceOfSatisfying(RateLimitedException.class,
                        e -> assertThat(e.nextAvailableAt()).isEqualTo(next));
        verify(f.quota, never()).reserve(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void secondGenerationWhileOneRunsIsRejected() {
        try (InFlightGenerations.Running running = f.inFlight.begin(USER, UUID.randomUUID())) {
            assertThatThrownBy(() -> service.generate(USER, WEEK, request()))
                    .isInstanceOf(GenerationInProgressException.class);
        }
        llm.thenReply(validReply());
        assertThat(service.generate(USER, WEEK, request()).mode()).isEqualTo(DraftMode.AI);
        assertThat(f.inFlight.isRunning(USER)).isFalse();
    }

    @Test
    void fewerEligibleRecipesThanSlotsOfAMealStopsWithoutLlm_US4_1() {
        when(f.eligible.eligibleByMeal(anySet(), any(), any())).thenReturn(Map.of(
                Meal.BREAKFAST, List.of(1L), Meal.LUNCH, List.of(11L), Meal.DINNER, List.of(21L, 22L)));
        GenerationConstraints strict = new GenerationConstraints(Set.of(), Set.of(), DietType.VEGAN, 2, 15,
                Set.of(3L));

        assertThatThrownBy(() -> service.generate(USER, WEEK, request(strict)))
                .isInstanceOfSatisfying(NotEnoughRecipesException.class, e -> {
                    assertThat(e.eligibleCount()).isEqualTo(1);
                    assertThat(e.requestedSlots()).isEqualTo(2);
                    assertThat(e.suggestions()).containsExactly(RelaxSuggestion.INCREASE_MAX_COOK_TIME,
                            RelaxSuggestion.REMOVE_PREFERRED_CUISINES, RelaxSuggestion.SELECT_FEWER_SLOTS);
                });
        assertThat(llm.requests()).isEmpty();
        verify(f.quota).finish(ROW, AiOutcome.NOT_ENOUGH, false, null, null);
        assertThat(f.inFlight.isRunning(USER)).isFalse();
    }

    @Test
    void notEnoughSuggestsOnlyWhatCanBeRelaxed_US4_2() {
        when(f.eligible.eligibleByMeal(anySet(), any(), any())).thenReturn(Map.of(
                Meal.BREAKFAST, List.of(), Meal.LUNCH, List.of(11L), Meal.DINNER, List.of(21L, 22L)));

        assertThatThrownBy(() -> service.generate(USER, WEEK, request()))
                .isInstanceOfSatisfying(NotEnoughRecipesException.class, e ->
                        assertThat(e.suggestions()).containsExactly(RelaxSuggestion.SELECT_FEWER_SLOTS));
    }

    @Test
    void restrictionsComeFromTheRequest() {
        llm.thenReply(validReply());

        service.generate(USER, WEEK, request());

        verify(f.eligible).eligibleByMeal(anySet(), eq(new DietRestrictions(Set.of(9L), Set.of(), DietType.NONE)),
                eq(null));
    }

    @Test
    void firstSlotIsMondayBreakfast() {
        llm.thenReply(validReply());

        assertThat(service.generate(USER, WEEK, request()).items().getFirst().day())
                .isEqualTo(MON_BREAKFAST.day());
    }

    private static List<Long> range(long from, long to) {
        return java.util.stream.LongStream.rangeClosed(from, to).boxed().toList();
    }

    @SuppressWarnings("unchecked")
    private Map<Meal, List<Long>> eligiblePassedToRetrieval() {
        org.mockito.ArgumentCaptor<Map<Meal, List<Long>>> eligible = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(f.candidateService).candidates(eligible.capture(), anyMap(), any());
        return eligible.getValue();
    }

    @Test
    void avoidsRecipesOfTheLastDraftThisWeekAndLastWeek_T110() {
        when(f.eligible.eligibleByMeal(anySet(), any(), any())).thenReturn(Map.of(
                Meal.BREAKFAST, range(1, 10), Meal.LUNCH, range(11, 20), Meal.DINNER, range(21, 30)));
        when(f.facade.plannedRecipeIds(USER, WEEK)).thenReturn(Set.of(11L));
        when(f.facade.plannedRecipeIds(USER, WEEK.minusWeeks(1))).thenReturn(Set.of(1L, 2L));
        llm.thenReply(validReply());
        GenerateRequest regenerate = new GenerateRequest(UUID.randomUUID(), EnumSet.of(DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY), EnumSet.allOf(Meal.class), constraints(4), Set.of(21L, 22L));

        service.generate(USER, WEEK, regenerate);

        Map<Meal, List<Long>> eligible = eligiblePassedToRetrieval();
        assertThat(eligible.get(Meal.BREAKFAST)).doesNotContain(1L, 2L).hasSize(8);
        assertThat(eligible.get(Meal.LUNCH)).doesNotContain(11L);
        assertThat(eligible.get(Meal.DINNER)).doesNotContain(21L, 22L);
    }

    @Test
    void avoidingNeverMakesNotEnough_T110() {
        // two breakfasts for two breakfast slots, both eaten last week: still planned, not a 422
        when(f.eligible.eligibleByMeal(anySet(), any(), any())).thenReturn(Map.of(
                Meal.BREAKFAST, List.of(1L, 2L), Meal.LUNCH, range(11, 20), Meal.DINNER, range(21, 30)));
        when(f.facade.plannedRecipeIds(USER, WEEK.minusWeeks(1))).thenReturn(Set.of(1L, 2L));
        llm.thenReply(validReply());

        assertThat(service.generate(USER, WEEK, request()).mode()).isEqualTo(DraftMode.AI);
        assertThat(eligiblePassedToRetrieval().get(Meal.BREAKFAST)).containsExactly(1L, 2L);
    }
}
