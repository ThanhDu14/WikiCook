package com.wikicook.aiplanner.service;

import static com.wikicook.aiplanner.service.GenerationFixture.MON_BREAKFAST;
import static com.wikicook.aiplanner.service.GenerationFixture.MON_DINNER;
import static com.wikicook.aiplanner.service.GenerationFixture.ROW;
import static com.wikicook.aiplanner.service.GenerationFixture.TUE_BREAKFAST;
import static com.wikicook.aiplanner.service.GenerationFixture.TUE_DINNER;
import static com.wikicook.aiplanner.service.GenerationFixture.TUE_LUNCH;
import static com.wikicook.aiplanner.service.GenerationFixture.USER;
import static com.wikicook.aiplanner.service.GenerationFixture.WEEK;
import static com.wikicook.aiplanner.service.GenerationFixture.reply;
import static com.wikicook.aiplanner.service.GenerationFixture.request;
import static com.wikicook.aiplanner.service.GenerationFixture.validReply;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;

import com.wikicook.ai.llm.LlmClient;
import com.wikicook.ai.llm.LlmRequest;
import com.wikicook.ai.llm.LlmResult;
import com.wikicook.ai.quota.AiOutcome;
import com.wikicook.ai.testing.FakeLlmClient;
import com.wikicook.aiplanner.dto.DraftItemDto;
import com.wikicook.aiplanner.dto.DraftMode;
import com.wikicook.aiplanner.dto.DraftResponse;
import com.wikicook.mealplan.domain.SlotKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** T057: one retry with a correction message, within the 60 s budget (research R7, R9). */
class GenerationServiceRetryTest {

    private final GenerationFixture f = new GenerationFixture();

    @Test
    void unusableFirstAnswerIsRetriedOnceWithACorrection_US3() {
        FakeLlmClient llm = new FakeLlmClient().thenReply("{\"slots\": \"oops\"}").thenReply(validReply());

        DraftResponse draft = f.service(llm).generate(USER, WEEK, request());

        assertThat(draft.mode()).isEqualTo(DraftMode.AI);
        assertThat(llm.requests()).hasSize(2);
        assertThat(llm.requests().get(1).userPrompt())
                .startsWith(llm.requests().get(0).userPrompt())
                .contains("Câu trả lời trước không dùng được: " + DraftValidator.INVALID_JSON);
        verify(f.quota).finish(ROW, AiOutcome.AI_DRAFT, true, 0, null);
    }

    @Test
    void missingSlotIsNamedInTheCorrection() {
        Map<SlotKey, Long> four = new LinkedHashMap<>();
        four.put(MON_BREAKFAST, 1L);
        four.put(MON_DINNER, 21L);
        four.put(TUE_BREAKFAST, 2L);
        four.put(TUE_LUNCH, 11L);
        FakeLlmClient llm = new FakeLlmClient().thenReply(reply(four)).thenReply(validReply());

        f.service(llm).generate(USER, WEEK, request());

        assertThat(llm.requests().get(1).userPrompt()).contains("Thiếu ô: TUESDAY DINNER");
    }

    @Test
    void noRetryWhenLessThan15SecondsAreLeft() {
        List<LlmRequest> calls = new ArrayList<>();
        LlmClient slowAndWrong = (request, timeout, token) -> {
            calls.add(request);
            assertThat(timeout).isEqualTo(Duration.ofSeconds(60));
            f.clock.set(f.clock.instant().plusSeconds(46)); // 14 s left
            return new LlmResult("not json", "fake");
        };

        DraftResponse draft = f.service(slowAndWrong).generate(USER, WEEK, request());

        assertThat(calls).hasSize(1);
        assertThat(draft.mode()).isEqualTo(DraftMode.AUTO);
        verify(f.quota).finish(eq(ROW), eq(AiOutcome.AUTO_DRAFT), eq(true), eq(null), startsWith("RETRY"));
    }

    @Test
    void retryGetsOnlyTheTimeLeft() {
        List<Duration> timeouts = new ArrayList<>();
        List<String> replies = new ArrayList<>(List.of("not json", validReply()));
        LlmClient client = (request, timeout, token) -> {
            timeouts.add(timeout);
            f.clock.set(f.clock.instant().plusSeconds(20));
            return new LlmResult(replies.removeFirst(), "fake");
        };

        assertThat(f.service(client).generate(USER, WEEK, request()).mode()).isEqualTo(DraftMode.AI);
        assertThat(timeouts).containsExactly(Duration.ofSeconds(60), Duration.ofSeconds(40));
    }

    @Test
    void badPicksBecomeChooseSlotsAndAreCounted_US3_1_US3_4() {
        Map<SlotKey, Long> picks = new LinkedHashMap<>();
        picks.put(MON_BREAKFAST, 1L);
        picks.put(MON_DINNER, 999L);   // not a candidate
        picks.put(TUE_BREAKFAST, 1L);  // duplicate of Monday breakfast
        picks.put(TUE_LUNCH, 11L);
        picks.put(TUE_DINNER, 22L);
        FakeLlmClient llm = new FakeLlmClient().thenReply(reply(picks));

        DraftResponse draft = f.service(llm).generate(USER, WEEK, request());

        assertThat(draft.mode()).isEqualTo(DraftMode.AI);
        assertThat(draft.chooseSlots()).containsExactly(MON_DINNER, TUE_BREAKFAST);
        assertThat(draft.removedPicks()).isEqualTo(2);
        assertThat(draft.items()).extracting(DraftItemDto::recipeId).containsExactly(1L, 11L, 22L);
        verify(f.quota).finish(ROW, AiOutcome.AI_DRAFT, true, 2, null);
    }
}
