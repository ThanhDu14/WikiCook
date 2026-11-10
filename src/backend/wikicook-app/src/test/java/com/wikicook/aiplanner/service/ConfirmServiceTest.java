package com.wikicook.aiplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.wikicook.ai.quota.AiOutcome;
import com.wikicook.ai.testing.MutableClock;
import com.wikicook.aiplanner.dto.ConfirmRequest;
import com.wikicook.aiplanner.dto.ConfirmResult;
import com.wikicook.mealplan.api.BatchSaveResult;
import com.wikicook.mealplan.api.DraftItem;
import com.wikicook.mealplan.api.MealPlanWriteFacade;
import com.wikicook.mealplan.domain.ItemSource;
import com.wikicook.mealplan.domain.Meal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** T047: confirming a draft only hands the dishes to 030 (FR-018, FR-019). */
class ConfirmServiceTest {

    private static final long USER = 7;
    private static final LocalDate WEEK = LocalDate.of(2026, 11, 16);

    private final MealPlanWriteFacade facade = mock(MealPlanWriteFacade.class);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-09T02:00:00Z"));
    private final InFlightGenerations inFlight = new InFlightGenerations(clock);
    // Only the 030 facade and the in-memory list: no LlmClient, no AiQuotaService, so confirming can neither call AI
    // nor write ai_requests.
    private final ConfirmService service = new ConfirmService(facade, inFlight);

    @Test
    void savesEveryDishKeepingSourceAndServings_US2_2() {
        ConfirmRequest request = new ConfirmRequest(UUID.randomUUID(), List.of(
                new ConfirmRequest.Item(DayOfWeek.MONDAY, Meal.BREAKFAST, 11, 4, ItemSource.AI),
                new ConfirmRequest.Item(DayOfWeek.MONDAY, Meal.LUNCH, 12, null, ItemSource.AUTO),
                new ConfirmRequest.Item(DayOfWeek.TUESDAY, Meal.DINNER, 13, 2, ItemSource.MANUAL)));
        when(facade.saveDraft(anyLong(), any(), anyList())).thenReturn(new BatchSaveResult(3, 0, 0));

        ConfirmResult result = service.confirm(USER, WEEK, request);

        verify(facade).saveDraft(eq(USER), eq(WEEK), eq(List.of(
                new DraftItem(DayOfWeek.MONDAY, Meal.BREAKFAST, 11, 4, ItemSource.AI),
                new DraftItem(DayOfWeek.MONDAY, Meal.LUNCH, 12, null, ItemSource.AUTO),
                new DraftItem(DayOfWeek.TUESDAY, Meal.DINNER, 13, 2, ItemSource.MANUAL))));
        verifyNoMoreInteractions(facade);
        assertThat(result).isEqualTo(new ConfirmResult(3, 0, 0));
    }

    @Test
    void reportsOccupiedAndUnavailableDishes_US2_6() {
        ConfirmRequest request = new ConfirmRequest(UUID.randomUUID(), List.of(
                new ConfirmRequest.Item(DayOfWeek.MONDAY, Meal.LUNCH, 12, null, ItemSource.AI),
                new ConfirmRequest.Item(DayOfWeek.MONDAY, Meal.DINNER, 13, null, ItemSource.AI),
                new ConfirmRequest.Item(DayOfWeek.FRIDAY, Meal.DINNER, 14, null, ItemSource.AI)));
        when(facade.saveDraft(anyLong(), any(), anyList())).thenReturn(new BatchSaveResult(1, 1, 1));

        assertThat(service.confirm(USER, WEEK, request)).isEqualTo(new ConfirmResult(1, 1, 1));
    }

    @Test
    void confirmingMarksTheGenerationSoItCannotBeRefunded_T072() {
        UUID requestId = UUID.randomUUID();
        try (InFlightGenerations.Running running = inFlight.begin(USER, requestId)) {
            running.finished(42, AiOutcome.AI_DRAFT);
        }
        when(facade.saveDraft(anyLong(), any(), anyList())).thenReturn(new BatchSaveResult(1, 0, 0));

        service.confirm(USER, WEEK, new ConfirmRequest(requestId, List.of(
                new ConfirmRequest.Item(DayOfWeek.MONDAY, Meal.LUNCH, 12, null, ItemSource.AI))));

        assertThat(inFlight.finished(USER, requestId)).hasValueSatisfying(f -> assertThat(f.confirmed()).isTrue());
        assertThat(inFlight.takeRefundable(USER, requestId)).isEmpty();
    }
}
