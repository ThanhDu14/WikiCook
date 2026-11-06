package com.wikicook.aiplanner.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wikicook.aiplanner.domain.RelaxSuggestion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Research R16: every AI planner error is a ProblemDetail with a stable {@code code}. */
@WebMvcTest(controllers = AiPlannerExceptionHandlerTest.FakeController.class)
@Import({AiPlannerExceptionHandlerTest.FixedClock.class, AiPlannerExceptionHandlerTest.FakeController.class})
class AiPlannerExceptionHandlerTest {

    static final Instant NOW = Instant.parse("2026-11-02T02:00:00Z");

    @Autowired
    MockMvc mvc;

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    record Body(@Min(1) @Max(20) int servings) {
    }

    @RestController
    static class FakeController {

        @GetMapping("/test/ai/{error}")
        void fail(@PathVariable String error) {
            throw switch (error) {
                case "past" -> new PastWeekException();
                case "no-slot" -> new NoEmptySlotException();
                case "not-enough" -> new NotEnoughRecipesException(3, 21,
                        List.of(RelaxSuggestion.INCREASE_MAX_COOK_TIME, RelaxSuggestion.SELECT_FEWER_SLOTS));
                case "rate" -> new RateLimitedException(NOW.plusSeconds(1_501));
                case "busy" -> new GenerationInProgressException();
                case "backup" -> new BackupUnavailableException(
                        new IllegalStateException("Gemini said: API key AIzaSECRET invalid"));
                case "suggestions" -> new SuggestionsUnavailableException();
                default -> new IllegalStateException("provider detail AIzaSECRET");
            };
        }

        @PostMapping("/test/ai/body")
        void body(@Valid @RequestBody Body body) {
        }
    }

    @Test
    void validationErrorIs400() throws Exception { // FR-005
        mvc.perform(post("/test/ai/body").contentType(MediaType.APPLICATION_JSON).content("{\"servings\":99}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(post("/test/ai/body").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void weekAndSlotErrorsAre422() throws Exception { // US1-6, edge cases "tuần đã qua", "không còn ô trống"
        mvc.perform(get("/test/ai/past")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PAST_WEEK"));
        mvc.perform(get("/test/ai/no-slot")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NO_EMPTY_SLOT"));
    }

    @Test
    void notEnoughRecipesCarriesCountsAndSuggestions() throws Exception { // US4-1, FR-024
        mvc.perform(get("/test/ai/not-enough"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NOT_ENOUGH_RECIPES"))
                .andExpect(jsonPath("$.eligibleCount").value(3))
                .andExpect(jsonPath("$.requestedSlots").value(21))
                .andExpect(jsonPath("$.suggestions[0]").value("INCREASE_MAX_COOK_TIME"))
                .andExpect(jsonPath("$.suggestions[1]").value("SELECT_FEWER_SLOTS"));
    }

    @Test
    void rateLimitedHasRetryAfterAndNextAvailableAt() throws Exception { // US5-2, FR-025
        mvc.perform(get("/test/ai/rate"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.nextAvailableAt").value("2026-11-02T02:25:01Z"))
                .andExpect(header().string("Retry-After", "1501"));
    }

    @Test
    void conflictAndUnavailable() throws Exception { // US5-4, US6-6, US7-5
        mvc.perform(get("/test/ai/busy")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GENERATION_IN_PROGRESS"));
        mvc.perform(get("/test/ai/suggestions")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SUGGESTIONS_UNAVAILABLE"));
    }

    @Test
    void providerMessagesNeverLeak() throws Exception { // NFR-002
        mvc.perform(get("/test/ai/backup"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("BACKUP_UNAVAILABLE"))
                .andExpect(content().string(not(containsString("AIza"))))
                .andExpect(content().string(not(containsString("Gemini"))));
        mvc.perform(get("/test/ai/other"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("AIza"))));
    }
}
