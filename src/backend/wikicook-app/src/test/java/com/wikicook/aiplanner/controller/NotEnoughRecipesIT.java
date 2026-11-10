package com.wikicook.aiplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.ai.testing.FakeLlmClient;
import com.wikicook.ai.testing.MutableClock;
import com.wikicook.aiplanner.dto.GenerateRequest;
import com.wikicook.aiplanner.dto.GenerationConstraints;
import com.wikicook.aiplanner.repository.EligibleRecipeQuery;
import com.wikicook.aiplanner.support.AiPlannerItBeans;
import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.aiplanner.support.RecipeSeed;
import com.wikicook.common.diet.DietRestrictions;
import com.wikicook.devstub.StubCurrentUser;
import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.service.WeekCalendar;
import com.wikicook.support.AbstractPostgresIT;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** T064: a meal with fewer suitable recipes than slots is a 422 with what to relax, and costs no generation (US4). */
@SpringBootTest(properties = {"wikicook.dev-stubs.enabled=true", "wikicook.ai.mode=mock"})
@AutoConfigureMockMvc
@Import(AiPlannerItBeans.class)
class NotEnoughRecipesIT extends AbstractPostgresIT {

    private static final long USER = 3101;
    /** The seed's quickest recipes take 15 minutes in all: only a few of them per meal. */
    private static final int MAX_COOK_TIME = 15;


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

    private final LocalDate nextWeek = WeekCalendar.currentWeekStart(Clock.systemUTC()).plusWeeks(1);

    @BeforeEach
    void seed() {
        clock.set(Instant.now()); // another class of the context may have moved it
        RecipeFixtures.clear(jdbc);
        jdbc.update("DELETE FROM ai_requests WHERE user_id = ?", USER);
        RecipeSeed.seed(new RecipeFixtures(jdbc));
    }

    private ResultActions options() throws Exception {
        return mvc.perform(get("/api/v1/ai-planner/weeks/{weekStart}/options", nextWeek)
                .header(StubCurrentUser.HEADER, USER));
    }

    @Test
    void notEnoughIs422WithSuggestionsAndCostsNothing_US4_1_US4_2_US4_3() throws Exception {
        options().andExpect(jsonPath("$.remaining").value(5));
        int llmCalls = llm.requests().size();
        int breakfasts = eligibleQuery.eligibleByMeal(Set.of(Meal.BREAKFAST), DietRestrictions.none(), MAX_COOK_TIME)
                .getOrDefault(Meal.BREAKFAST, List.of()).size();
        assertThat(breakfasts).isBetween(1, 6); // some, but fewer than seven breakfasts (edge case)
        GenerateRequest week = new GenerateRequest(UUID.randomUUID(), EnumSet.allOf(DayOfWeek.class),
                EnumSet.allOf(Meal.class), new GenerationConstraints(Set.of(), Set.of(), DietType.NONE, 2,
                MAX_COOK_TIME, Set.of()));

        mvc.perform(post("/api/v1/ai-planner/weeks/{weekStart}/generations", nextWeek)
                        .header(StubCurrentUser.HEADER, USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(week)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NOT_ENOUGH_RECIPES"))
                .andExpect(jsonPath("$.eligibleCount").value(breakfasts))
                .andExpect(jsonPath("$.requestedSlots").value(7))
                .andExpect(jsonPath("$.suggestions", contains("INCREASE_MAX_COOK_TIME", "SELECT_FEWER_SLOTS")));

        options().andExpect(status().isOk()).andExpect(jsonPath("$.remaining").value(5));
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT outcome, counted, called_llm FROM ai_requests WHERE user_id = ?", USER);
        assertThat(row).containsEntry("outcome", "NOT_ENOUGH").containsEntry("counted", false)
                .containsEntry("called_llm", false);
        assertThat(llm.requests()).hasSize(llmCalls);
    }
}
