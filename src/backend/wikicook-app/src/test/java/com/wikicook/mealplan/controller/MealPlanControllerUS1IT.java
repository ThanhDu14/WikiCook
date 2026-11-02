package com.wikicook.mealplan.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.mealplan.support.HeaderCurrentUser;
import com.wikicook.mealplan.support.MealPlanFixtures;
import com.wikicook.support.AbstractPostgresIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** T032: the US1 endpoints end to end (quickstart Q1–Q5). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(HeaderCurrentUser.class)
class MealPlanControllerUS1IT extends AbstractPostgresIT {

    private static final String WEEK = "/api/v1/meal-plans/2026-10-19";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    private long user;
    private long pho;

    @BeforeEach
    void setUp() {
        user = new MealPlanFixtures(jdbc).user();
        pho = new RecipeFixtures(jdbc).publishedRecipe("Phở bò Hà Nội", "Bánh phở");
    }

    private MockHttpServletRequestBuilder as(long userId, MockHttpServletRequestBuilder request) {
        return request.header(HeaderCurrentUser.HEADER, userId).contentType(MediaType.APPLICATION_JSON);
    }

    private JsonNode add(long userId, String slot, long recipeId) throws Exception {
        String body = mvc.perform(as(userId, put(WEEK + "/slots/" + slot)).content("{\"recipeId\":%d}".formatted(recipeId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    @Test
    void emptyWeekHasSevenDaysOfThreeMeals() throws Exception { // US1-1
        mvc.perform(as(user, get(WEEK)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value("2026-10-19"))
                .andExpect(jsonPath("$.weekEnd").value("2026-10-25"))
                .andExpect(jsonPath("$.empty").value(true))
                .andExpect(jsonPath("$.days", hasSize(7)))
                .andExpect(jsonPath("$.days[0].day").value("MONDAY"))
                .andExpect(jsonPath("$.days[0].date").value("2026-10-19"))
                .andExpect(jsonPath("$.days[6].day").value("SUNDAY"))
                .andExpect(jsonPath("$.days[3].slots", hasSize(3)))
                .andExpect(jsonPath("$.days[3].slots[0].meal").value("BREAKFAST"))
                .andExpect(jsonPath("$.days[3].slots[1].meal").value("LUNCH"))
                .andExpect(jsonPath("$.days[3].slots[2].meal").value("DINNER"))
                .andExpect(jsonPath("$.days[3].slots[2].item").value(nullValue()));
    }

    @Test
    void weekStartMustBeAMonday() throws Exception {
        mvc.perform(as(user, get("/api/v1/meal-plans/2026-10-20")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WEEK_START"));
    }

    @Test
    void withoutSignInEveryCallIs401() throws Exception {
        mvc.perform(get(WEEK))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(put(WEEK + "/slots/MONDAY/LUNCH").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipeId\":%d}".formatted(pho)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addReturnsThePlanItemAndTheWeekShowsIt() throws Exception { // US1-2, US1-5
        JsonNode item = add(user, "TUESDAY/BREAKFAST", pho);

        org.assertj.core.api.Assertions.assertThat(item.get("recipeName").asText()).isEqualTo("Phở bò Hà Nội");
        org.assertj.core.api.Assertions.assertThat(item.get("servings").asInt()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(item.get("source").asText()).isEqualTo("MANUAL");
        org.assertj.core.api.Assertions.assertThat(item.get("available").asBoolean()).isTrue();
        mvc.perform(as(user, get(WEEK)))
                .andExpect(jsonPath("$.empty").value(false))
                .andExpect(jsonPath("$.availableItemCount").value(1))
                .andExpect(jsonPath("$.days[1].slots[0].item.recipeId").value(pho))
                .andExpect(jsonPath("$.days[1].slots[0].item.totalTimeMinutes").value(40));
    }

    @Test
    void servingsOutsideOneToTwentyAreRefusedAndKept() throws Exception { // edge case servings
        JsonNode item = add(user, "MONDAY/LUNCH", pho);
        String expected = "{\"itemId\":%d,\"version\":%d}".formatted(item.get("id").asLong(), item.get("version").asInt());

        for (String servings : new String[] {"0", "21", "2.5", "null"}) {
            mvc.perform(as(user, patch(WEEK + "/slots/MONDAY/LUNCH/servings"))
                            .content("{\"servings\":%s,\"expected\":%s}".formatted(servings, expected)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        mvc.perform(as(user, put(WEEK + "/slots/MONDAY/DINNER")).content("{\"recipeId\":%d,\"servings\":21}".formatted(pho)))
                .andExpect(status().isBadRequest());

        mvc.perform(as(user, patch(WEEK + "/slots/MONDAY/LUNCH/servings"))
                        .content("{\"servings\":6,\"expected\":%s}".formatted(expected)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servings").value(6))
                .andExpect(jsonPath("$.version").value(1)); // US1-7
        mvc.perform(as(user, get(WEEK))).andExpect(jsonPath("$.days[0].slots[1].item.servings").value(6));
    }

    @Test
    void unpublishedRecipeIs422() throws Exception { // FR-010
        long draft = new RecipeFixtures(jdbc).recipe("Bản nháp chưa duyệt", "ARCHIVED", "Gạo");

        mvc.perform(as(user, put(WEEK + "/slots/MONDAY/LUNCH")).content("{\"recipeId\":%d}".formatted(draft)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_AVAILABLE"));
    }

    @Test
    void anotherUserSeesOnlyTheirOwnEmptyWeek() throws Exception { // FR-006, NFR-004
        add(user, "MONDAY/LUNCH", pho);
        long other = new MealPlanFixtures(jdbc).user();

        mvc.perform(as(other, get(WEEK))).andExpect(jsonPath("$.empty").value(true));
    }

    @Test
    void twoDevicesOfOneUserSeeTheSameWeek() throws Exception { // US1-9, SC-004
        add(user, "SUNDAY/DINNER", pho);

        // a second request with the same identity, as from another device: the change is already saved
        mvc.perform(as(user, get(WEEK))).andExpect(jsonPath("$.days[6].slots[2].item.recipeId").value(pho));
    }
}
