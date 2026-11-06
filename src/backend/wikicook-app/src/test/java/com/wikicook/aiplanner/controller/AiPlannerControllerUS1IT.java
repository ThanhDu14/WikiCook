package com.wikicook.aiplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.ai.testing.FakeEmbeddingClient;
import com.wikicook.ai.testing.FakeLlmClient;
import com.wikicook.aiplanner.domain.Candidate;
import com.wikicook.aiplanner.dto.GenerateRequest;
import com.wikicook.aiplanner.dto.GenerationConstraints;
import com.wikicook.aiplanner.repository.EligibleRecipeQuery;
import com.wikicook.aiplanner.service.CandidateService;
import com.wikicook.aiplanner.service.InFlightGenerations;
import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.aiplanner.support.RecipeSeed;
import com.wikicook.devstub.DevUserProvisioner;
import com.wikicook.devstub.StubCurrentUser;
import com.wikicook.mealplan.api.DraftItem;
import com.wikicook.mealplan.api.MealPlanWriteFacade;
import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.domain.ItemSource;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.service.WeekCalendar;
import com.wikicook.support.AbstractPostgresIT;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** T032: the US1 endpoints end to end, with the dev stubs standing in for 010/011/030. */
@SpringBootTest(properties = {"wikicook.dev-stubs.enabled=true", "wikicook.ai.mode=mock"})
@AutoConfigureMockMvc
class AiPlannerControllerUS1IT extends AbstractPostgresIT {

    private static final AtomicLong NEXT_USER = new AtomicLong(1000);

    @TestConfiguration
    static class FakeClients {

        @Bean
        @Primary
        FakeLlmClient fakeLlmClient() {
            return new FakeLlmClient();
        }

        @Bean
        @Primary
        FakeEmbeddingClient fakeEmbeddingClient() {
            return new FakeEmbeddingClient(768);
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    FakeLlmClient llm;

    @Autowired
    EligibleRecipeQuery eligibleQuery;

    @Autowired
    CandidateService candidateService;

    @Autowired
    MealPlanWriteFacade mealPlans;

    @Autowired
    DevUserProvisioner provisioner;

    @Autowired
    InFlightGenerations inFlight;

    private RecipeSeed.Seeded seed;
    private long user;
    private final LocalDate nextWeek = WeekCalendar.currentWeekStart(Clock.systemUTC()).plusWeeks(1);

    @BeforeEach
    void seed() {
        RecipeFixtures.clear(jdbc);
        seed = RecipeSeed.seed(new RecipeFixtures(jdbc));
        user = NEXT_USER.incrementAndGet();
    }

    private static GenerationConstraints constraints(Integer servings, Integer maxCookTime) {
        return new GenerationConstraints(Set.of(), Set.of(), DietType.NONE, servings, maxCookTime, Set.of());
    }

    /** Monday, all three meals, four servings, allergic to peanuts. */
    private GenerateRequest monday() {
        return new GenerateRequest(UUID.randomUUID(), Set.of(DayOfWeek.MONDAY), EnumSet.allOf(Meal.class),
                new GenerationConstraints(Set.of(seed.peanutAllergen()), Set.of(), DietType.NONE, 4, null, Set.of()));
    }

    /** The reply the LLM is scripted with: the best candidate of each meal, as the pipeline will rank them. */
    private String replyFor(GenerateRequest request) {
        Map<Meal, Integer> perMeal = new EnumMap<>(Meal.class);
        request.meals().forEach(meal -> perMeal.put(meal, request.days().size()));
        Map<Meal, List<Candidate>> candidates = candidateService.candidates(
                eligibleQuery.eligibleByMeal(request.meals(), request.constraints().restrictions(), null), perMeal,
                request.constraints());
        List<String> slots = new ArrayList<>();
        int i = 0;
        for (Meal meal : Meal.values()) {
            if (request.meals().contains(meal)) {
                long id = candidates.get(meal).get(i++).recipeId(); // a different rank per meal: no duplicate
                slots.add("{\"day\":\"MONDAY\",\"meal\":\"%s\",\"recipeId\":%d}".formatted(meal, id));
            }
        }
        return "{\"slots\":[" + String.join(",", slots) + "]}";
    }

    private ResultActions options(LocalDate week) throws Exception {
        return mvc.perform(get("/api/v1/ai-planner/weeks/{weekStart}/options", week)
                .header(StubCurrentUser.HEADER, user));
    }

    private ResultActions generate(LocalDate week, Object body) throws Exception {
        return mvc.perform(post("/api/v1/ai-planner/weeks/{weekStart}/generations", week)
                .header(StubCurrentUser.HEADER, user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    @Test
    void optionsOfNextWeek_US1_1_US1_2() throws Exception {
        options(nextWeek)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value(nextWeek.toString()))
                .andExpect(jsonPath("$.selectableDays", hasSize(7)))
                .andExpect(jsonPath("$.emptySlots", hasSize(21)))
                .andExpect(jsonPath("$.emptySlots[0].day").value("MONDAY"))
                .andExpect(jsonPath("$.emptySlots[0].meal").value("BREAKFAST"))
                .andExpect(jsonPath("$.defaults.dietType").value("NONE"))
                .andExpect(jsonPath("$.defaults.allergenIds", hasSize(0)))
                .andExpect(jsonPath("$.defaults.servings").value(nullValue()))
                .andExpect(jsonPath("$.profileIncomplete").value(true))
                .andExpect(jsonPath("$.remaining").value(5))
                .andExpect(jsonPath("$.nextAvailableAt").value(nullValue()))
                .andExpect(jsonPath("$.aiAvailableToday").value(true));
    }

    @Test
    void optionsOfAPastWeekOfferNothing() throws Exception {
        options(nextWeek.minusWeeks(2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectableDays", hasSize(0)))
                .andExpect(jsonPath("$.emptySlots", hasSize(0)));
    }

    @Test
    void generateGivesAnAiDraft_US1_5_US1_6() throws Exception {
        GenerateRequest request = monday();
        llm.thenReply(replyFor(request));

        generate(nextWeek, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(request.requestId().toString()))
                .andExpect(jsonPath("$.mode").value("AI"))
                .andExpect(jsonPath("$.notice").value(nullValue()))
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[0].day").value("MONDAY"))
                .andExpect(jsonPath("$.items[0].meal").value("BREAKFAST"))
                .andExpect(jsonPath("$.items[0].source").value("AI"))
                .andExpect(jsonPath("$.items[0].servings").value(4))
                .andExpect(jsonPath("$.items[0].recipeName").isString())
                .andExpect(jsonPath("$.chooseSlots", hasSize(0)))
                .andExpect(jsonPath("$.removedPicks").value(0))
                .andExpect(jsonPath("$.remaining").value(4));

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT outcome, counted, called_llm, requested_slots, constraints::text AS constraints "
                        + "FROM ai_requests WHERE user_id = ?", user);
        assertThat(row).containsEntry("outcome", "AI_DRAFT").containsEntry("counted", true)
                .containsEntry("called_llm", true);
        assertThat(((Number) row.get("requested_slots")).intValue()).isEqualTo(3);
        // NFR-003: no email or name of the user in the stored constraints, nor in the prompt.
        String email = jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, user);
        assertThat((String) row.get("constraints")).doesNotContain(email).doesNotContain("Dev user");
        assertThat(llm.requests().getLast().userPrompt()).doesNotContain(email).doesNotContain(String.valueOf(user));
    }

    @Test
    void llmFailureGivesAnAutoDraft_US6_1() throws Exception {
        llm.thenFail();

        generate(nextWeek, monday())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("AUTO"))
                .andExpect(jsonPath("$.notice").value("AUTO_NO_AI"))
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[0].source").value("AUTO"))
                .andExpect(jsonPath("$.remaining").value(5));
        assertThat(jdbc.queryForObject("SELECT outcome FROM ai_requests WHERE user_id = ?", String.class, user))
                .isEqualTo("AUTO_DRAFT");
    }

    @Test
    void pastWeekIs422() throws Exception {
        generate(nextWeek.minusWeeks(2), monday())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PAST_WEEK"));
    }

    @Test
    void fullWeekIs422NoEmptySlot() throws Exception {
        List<DraftItem> monday = new ArrayList<>();
        for (Meal meal : Meal.values()) {
            monday.add(new DraftItem(DayOfWeek.MONDAY, meal, seed.allDay().get(meal.ordinal()), 2, ItemSource.MANUAL));
        }
        provisioner.ensure(user); // the real 030 tables reference users; the stub kept weeks in memory
        mealPlans.saveDraft(user, nextWeek, monday);

        generate(nextWeek, monday())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NO_EMPTY_SLOT"));
    }

    @Test
    void weekStartNotAMondayIs400() throws Exception {
        options(nextWeek.plusDays(1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WEEK_START"));
        generate(nextWeek.plusDays(1), monday())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WEEK_START"));
        mvc.perform(get("/api/v1/ai-planner/weeks/not-a-date/options").header(StubCurrentUser.HEADER, user))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WEEK_START"));
    }

    @Test
    void invalidBodiesAre400() throws Exception {
        UUID id = UUID.randomUUID();
        Set<DayOfWeek> monday = Set.of(DayOfWeek.MONDAY);
        Set<Meal> lunch = Set.of(Meal.LUNCH);
        List<Object> bodies = List.of(
                new GenerateRequest(id, Set.of(), lunch, constraints(2, null)),
                new GenerateRequest(id, monday, Set.of(), constraints(2, null)),
                new GenerateRequest(id, monday, lunch, constraints(0, null)),
                new GenerateRequest(id, monday, lunch, constraints(21, null)),
                new GenerateRequest(id, monday, lunch, constraints(2, 5)),
                new GenerateRequest(null, monday, lunch, constraints(2, null)),
                Map.of("requestId", id.toString(), "days", List.of("FUNDAY"), "meals", List.of("LUNCH")));
        for (Object body : bodies) {
            generate(nextWeek, body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ai_requests WHERE user_id = ?", Integer.class, user))
                .isZero();
    }

    @Test
    void secondGenerationWhileOneRunsIs409() throws Exception {
        GenerateRequest first = monday();
        llm.thenSleep(Duration.ofSeconds(2), replyFor(first));
        CompletableFuture<ResultActions> running = CompletableFuture.supplyAsync(() -> {
            try {
                return generate(nextWeek, first);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        long waitUntil = System.currentTimeMillis() + 5_000;
        while (!inFlight.isRunning(user) && System.currentTimeMillis() < waitUntil) {
            Thread.onSpinWait();
        }

        generate(nextWeek, monday())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GENERATION_IN_PROGRESS"));
        running.get().andExpect(status().isOk()).andExpect(jsonPath("$.mode").value("AI"));
    }

    @Test
    void invalidDevUserHeaderIs400() throws Exception {
        mvc.perform(get("/api/v1/ai-planner/weeks/{weekStart}/options", nextWeek)
                        .header(StubCurrentUser.HEADER, "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
