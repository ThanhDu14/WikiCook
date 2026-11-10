package com.wikicook.aiplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.ai.testing.FakeLlmClient;
import com.wikicook.ai.testing.MutableClock;
import com.wikicook.aiplanner.domain.Candidate;
import com.wikicook.aiplanner.dto.GenerateRequest;
import com.wikicook.aiplanner.dto.GenerationConstraints;
import com.wikicook.aiplanner.repository.EligibleRecipeQuery;
import com.wikicook.aiplanner.service.CandidateService;
import com.wikicook.aiplanner.support.AiPlannerItBeans;
import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.aiplanner.support.RecipeSeed;
import com.wikicook.devstub.StubCurrentUser;
import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.service.WeekCalendar;
import com.wikicook.support.AbstractPostgresIT;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * T068: five counted generations per user in a rolling 60 minutes (US5, FR-025, FR-026, SC-007), with a clock the
 * test moves.
 */
@SpringBootTest(properties = {"wikicook.dev-stubs.enabled=true", "wikicook.ai.mode=mock"})
@AutoConfigureMockMvc
@Import(AiPlannerItBeans.class)
class RateLimitIT extends AbstractPostgresIT {

    private static final AtomicLong NEXT_USER = new AtomicLong(3200);


    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    FakeLlmClient llm;

    @Autowired
    MutableClock clock;

    @Autowired
    EligibleRecipeQuery eligibleQuery;

    @Autowired
    CandidateService candidateService;

    private long user;
    private Instant start;
    private LocalDate nextWeek;

    @BeforeEach
    void seed() {
        RecipeFixtures.clear(jdbc);
        RecipeSeed.seed(new RecipeFixtures(jdbc));
        user = NEXT_USER.incrementAndGet();
        start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        clock.set(start);
        nextWeek = WeekCalendar.currentWeekStart(Clock.fixed(start, ZoneOffset.UTC)).plusWeeks(1);
    }

    private static GenerateRequest monday(Integer maxCookTime) {
        return new GenerateRequest(UUID.randomUUID(), Set.of(DayOfWeek.MONDAY), EnumSet.allOf(Meal.class),
                new GenerationConstraints(Set.of(), Set.of(), DietType.NONE, 2, maxCookTime, Set.of()));
    }

    /** "Tạo lại" sends the constraints of the draft again with a new request id (US2-5). */
    private static GenerateRequest again(GenerateRequest last) {
        return new GenerateRequest(UUID.randomUUID(), last.days(), last.meals(), last.constraints());
    }

    /** A valid reply: a different-ranked candidate per meal. */
    private String replyFor(GenerateRequest request) {
        Map<Meal, Integer> perMeal = new EnumMap<>(Meal.class);
        request.meals().forEach(meal -> perMeal.put(meal, 1));
        Map<Meal, List<Candidate>> candidates = candidateService.candidates(
                eligibleQuery.eligibleByMeal(request.meals(), request.constraints().restrictions(),
                        request.constraints().maxCookTimeMinutes()), perMeal, request.constraints());
        List<String> slots = new ArrayList<>();
        int i = 0;
        for (Meal meal : Meal.values()) {
            long id = candidates.get(meal).get(i++).recipeId();
            slots.add("{\"day\":\"MONDAY\",\"meal\":\"%s\",\"recipeId\":%d}".formatted(meal, id));
        }
        return "{\"slots\":[" + String.join(",", slots) + "]}";
    }

    private ResultActions options() throws Exception {
        return mvc.perform(get("/api/v1/ai-planner/weeks/{weekStart}/options", nextWeek)
                .header(StubCurrentUser.HEADER, user));
    }

    private ResultActions generate(GenerateRequest request) throws Exception {
        return mvc.perform(post("/api/v1/ai-planner/weeks/{weekStart}/generations", nextWeek)
                .header(StubCurrentUser.HEADER, user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request)));
    }

    private int rows() {
        return jdbc.queryForObject("SELECT count(*) FROM ai_requests WHERE user_id = ?", Integer.class, user);
    }

    @Test
    void sixthCountedGenerationInAnHourIs429UntilTheOldestLeavesTheWindow_US5_1_to_US5_4() throws Exception {
        options().andExpect(status().isOk())
                .andExpect(jsonPath("$.remaining").value(5))
                .andExpect(jsonPath("$.nextAvailableAt").value(nullValue()));

        // Not counted (US5-4, FR-026): a draft made without AI and a "not enough recipes" answer.
        llm.thenFail();
        generate(monday(null)).andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("AUTO"))
                .andExpect(jsonPath("$.remaining").value(5));
        generate(monday(10)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NOT_ENOUGH_RECIPES"));
        options().andExpect(jsonPath("$.remaining").value(5));

        // Five AI drafts, one a minute: the first and four "Tạo lại" (US5-3).
        GenerateRequest last = monday(null);
        for (int i = 0; i < 5; i++) {
            clock.set(start.plus(Duration.ofMinutes(i)));
            last = i == 0 ? last : again(last);
            llm.thenReply(replyFor(last));
            generate(last).andExpect(status().isOk())
                    .andExpect(jsonPath("$.mode").value("AI"))
                    .andExpect(jsonPath("$.remaining").value(4 - i));
        }
        Instant nextAvailable = start.plus(Duration.ofMinutes(60));
        options().andExpect(jsonPath("$.remaining").value(0))
                .andExpect(jsonPath("$.nextAvailableAt").value(nextAvailable.toString()));

        // The sixth, ten minutes after the first: refused before anything is written or asked (US5-2, SC-007).
        clock.set(start.plus(Duration.ofMinutes(10)));
        int llmCalls = llm.requests().size();
        int rowsBefore = rows();
        MvcResult limited = generate(again(last))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(header().string("Retry-After", String.valueOf(Duration.ofMinutes(50).toSeconds())))
                .andReturn();
        String at = json.readTree(limited.getResponse().getContentAsString()).path("nextAvailableAt").asText();
        assertThat(Instant.parse(at)).isEqualTo(nextAvailable);
        assertThat(llm.requests()).hasSize(llmCalls);
        assertThat(rows()).isEqualTo(rowsBefore);

        // Rolling window: once the oldest counted generation is 60 minutes old, one more is allowed.
        clock.set(nextAvailable.plusSeconds(1));
        options().andExpect(jsonPath("$.remaining").value(1))
                .andExpect(jsonPath("$.nextAvailableAt").value(nullValue()));
        GenerateRequest after = again(last);
        llm.thenReply(replyFor(after));
        generate(after).andExpect(status().isOk()).andExpect(jsonPath("$.remaining").value(0));
    }
}
