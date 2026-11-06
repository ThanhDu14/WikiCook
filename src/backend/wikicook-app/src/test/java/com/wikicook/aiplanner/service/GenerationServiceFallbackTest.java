package com.wikicook.aiplanner.service;

import static com.wikicook.aiplanner.service.GenerationFixture.REQUESTED;
import static com.wikicook.aiplanner.service.GenerationFixture.ROW;
import static com.wikicook.aiplanner.service.GenerationFixture.USER;
import static com.wikicook.aiplanner.service.GenerationFixture.WEEK;
import static com.wikicook.aiplanner.service.GenerationFixture.request;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wikicook.ai.AiUnavailableException;
import com.wikicook.ai.llm.LlmClient;
import com.wikicook.ai.quota.AiOutcome;
import com.wikicook.ai.quota.Reservation;
import com.wikicook.ai.testing.FakeLlmClient;
import com.wikicook.aiplanner.controller.BackupUnavailableException;
import com.wikicook.aiplanner.dto.DraftItemDto;
import com.wikicook.aiplanner.dto.DraftMode;
import com.wikicook.aiplanner.dto.DraftNotice;
import com.wikicook.aiplanner.dto.DraftResponse;
import com.wikicook.mealplan.domain.ItemSource;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.SlotKey;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

/** T075: when the AI path ends, a draft without AI (research R8, data-model 4); none of these is counted. */
class GenerationServiceFallbackTest {

    private final GenerationFixture f = new GenerationFixture();

    private static void assertAutoDraft(DraftResponse draft, DraftNotice notice) {
        assertThat(draft.mode()).isEqualTo(DraftMode.AUTO);
        assertThat(draft.notice()).isEqualTo(notice);
        assertThat(draft.items()).extracting(item -> new SlotKey(item.day(), item.meal()))
                .containsExactlyElementsOf(REQUESTED);
        assertThat(draft.items()).allSatisfy(item -> assertThat(item.source()).isEqualTo(ItemSource.AUTO));
        assertThat(new HashSet<>(draft.items().stream().map(DraftItemDto::recipeId).toList())).hasSize(5);
    }

    @Test
    void llmErrorGivesAnAutoDraft_US6_1_US6_3() {
        FakeLlmClient llm = new FakeLlmClient().thenFail();

        DraftResponse draft = f.service(llm).generate(USER, WEEK, request());

        assertAutoDraft(draft, DraftNotice.AUTO_NO_AI);
        verify(f.quota).finish(ROW, AiOutcome.AUTO_DRAFT, true, null, "LLM_UNAVAILABLE");
    }

    @Test
    void twoUnusableAnswersGiveAnAutoDraft_US3_5_FR_014() {
        FakeLlmClient llm = new FakeLlmClient().thenReply("[]").thenReply("{}");

        DraftResponse draft = f.service(llm).generate(USER, WEEK, request());

        assertAutoDraft(draft, DraftNotice.AUTO_NO_AI);
        assertThat(llm.requests()).hasSize(2);
        verify(f.quota).finish(eq(ROW), eq(AiOutcome.AUTO_DRAFT), eq(true), eq(null), any());
    }

    @Test
    void overTheBudgetIsATimeoutWithAnAutoDraft_US6_2_FR_023() {
        LlmClient slow = (request, timeout, token) -> {
            f.clock.set(f.clock.instant().plus(timeout));
            throw new AiUnavailableException("Gemini did not answer in time");
        };

        DraftResponse draft = f.service(slow).generate(USER, WEEK, request());

        assertAutoDraft(draft, DraftNotice.AUTO_NO_AI);
        verify(f.quota).finish(ROW, AiOutcome.TIMEOUT, true, null, "TIMEOUT");
    }

    @Test
    void dailyCapUsedUpSkipsTheLlm_FR_025a() {
        when(f.quota.reserve(any(), any(), any(), any(), any(), any(), any())).thenReturn(new Reservation(ROW, false));
        FakeLlmClient llm = new FakeLlmClient();

        DraftResponse draft = f.service(llm).generate(USER, WEEK, request());

        assertAutoDraft(draft, DraftNotice.AI_DAILY_CAP);
        assertThat(llm.requests()).isEmpty();
        verify(f.quota).finish(ROW, AiOutcome.QUOTA_CAP, false, null, null);
    }

    @Test
    void noAutoDraftIsCounted_US6_4() {
        f.service(new FakeLlmClient().thenFail()).generate(USER, WEEK, request());

        verify(f.quota, never()).finish(anyLong(), eq(AiOutcome.AI_DRAFT), anyBoolean(), any(), any());
    }

    @Test
    void filterFailureIsBackupUnavailable_US6_6() {
        when(f.eligible.eligibleByMeal(anySet(), any(), any()))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        assertThatThrownBy(() -> f.service(new FakeLlmClient()).generate(USER, WEEK, request()))
                .isInstanceOf(BackupUnavailableException.class);
        verify(f.quota).finish(ROW, AiOutcome.FAILED, false, null, "BACKUP_FAILED");
        assertThat(f.inFlight.isRunning(USER)).isFalse();
    }

    @Test
    void candidatesRunningOutLeaveChooseSlots_US6_5() {
        // Step 1 found enough breakfasts, but retrieval returned only one of them.
        when(f.eligible.eligibleByMeal(anySet(), any(), any())).thenReturn(Map.of(
                Meal.BREAKFAST, List.of(1L, 2L), Meal.LUNCH, List.of(11L), Meal.DINNER, List.of(21L, 22L)));
        f.candidates.get(Meal.BREAKFAST).subList(1, 5).clear();

        DraftResponse draft = f.service(new FakeLlmClient().thenFail()).generate(USER, WEEK, request());

        assertThat(draft.mode()).isEqualTo(DraftMode.AUTO);
        assertThat(draft.chooseSlots()).containsExactly(GenerationFixture.TUE_BREAKFAST);
    }
}
