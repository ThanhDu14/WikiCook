package com.wikicook.aiplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.ai.testing.FakeLlmClient;
import com.wikicook.ai.testing.MutableClock;
import com.wikicook.aiplanner.dto.GenerateRequest;
import com.wikicook.aiplanner.dto.GenerationConstraints;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
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

/**
 * T058 (quickstart A5): the LLM answers with a recipe that does not exist, an archived one and one with the user's
 * allergen. None of them reaches the draft; their meals become "Chọn món" and the row counts them (SC-003, NFR-004,
 * FR-032).
 */
@SpringBootTest(properties = {"wikicook.dev-stubs.enabled=true", "wikicook.ai.mode=mock"})
@AutoConfigureMockMvc
@Import(AiPlannerItBeans.class)
class DraftSafetyIT extends AbstractPostgresIT {

    private static final long USER = 3001;
    private static final long MISSING_RECIPE = 987_654_321L;


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

    private RecipeSeed.Seeded seed;
    private final LocalDate nextWeek = WeekCalendar.currentWeekStart(Clock.systemUTC()).plusWeeks(1);

    @BeforeEach
    void seed() {
        clock.set(Instant.now()); // another class of the context may have moved it
        RecipeFixtures.clear(jdbc);
        jdbc.update("DELETE FROM ai_requests WHERE user_id = ?", USER);
        seed = RecipeSeed.seed(new RecipeFixtures(jdbc));
    }

    @Test
    void badPicksBecomeChooseSlots_US3_1_US3_2() throws Exception {
        long withPeanut = seed.withPeanut().getFirst();
        List<Long> bad = List.of(MISSING_RECIPE, seed.archived(), withPeanut);
        String reply = """
                {"slots":[
                  {"day":"MONDAY","meal":"BREAKFAST","recipeId":%d},
                  {"day":"MONDAY","meal":"LUNCH","recipeId":%d},
                  {"day":"MONDAY","meal":"DINNER","recipeId":%d}
                ]}""".formatted(bad.get(0), bad.get(1), bad.get(2));
        llm.thenReply(reply);
        int llmCalls = llm.requests().size();
        GenerateRequest request = new GenerateRequest(UUID.randomUUID(), Set.of(DayOfWeek.MONDAY),
                EnumSet.allOf(Meal.class), new GenerationConstraints(Set.of(seed.peanutAllergen()), Set.of(),
                DietType.NONE, 2, null, Set.of()));

        String body = mvc.perform(post("/api/v1/ai-planner/weeks/{weekStart}/generations", nextWeek)
                        .header(StubCurrentUser.HEADER, USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("AI"))
                .andExpect(jsonPath("$.chooseSlots", hasSize(3)))
                .andExpect(jsonPath("$.removedPicks").value(3))
                .andReturn().getResponse().getContentAsString();

        JsonNode draft = json.readTree(body);
        List<Long> shown = new ArrayList<>();
        draft.path("items").forEach(item -> shown.add(item.path("recipeId").asLong()));
        assertThat(shown).doesNotContainAnyElementsOf(bad);
        List<String> chooseSlots = new ArrayList<>();
        draft.path("chooseSlots").forEach(slot -> chooseSlots.add(slot.path("day").asText() + " "
                + slot.path("meal").asText()));
        assertThat(chooseSlots).containsExactly("MONDAY BREAKFAST", "MONDAY LUNCH", "MONDAY DINNER");
        assertThat(jdbc.queryForObject("SELECT removed_picks FROM ai_requests WHERE user_id = ?", Integer.class,
                USER)).isEqualTo(3);
        assertThat(llm.requests()).hasSize(llmCalls + 1); // dropped picks do not ask for a retry
    }
}
