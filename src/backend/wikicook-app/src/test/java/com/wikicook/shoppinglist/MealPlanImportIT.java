package com.wikicook.shoppinglist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.mealplan.support.MealPlanFixtures;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.HeaderCurrentUser;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** T038 / T045: POST /shopping-lists/active/import/meal-plan end to end, from the 030 tables to the list. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(HeaderCurrentUser.class)
class MealPlanImportIT extends AbstractPostgresIT {

    private static final LocalDate WEEK = LocalDate.of(2026, 11, 23);
    private static final String URL = "/api/v1/shopping-lists/active/import/meal-plan";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;

    private long user;
    private long planId;
    private long onion;
    private long chicken;
    private long boKho;
    private long gaXao;

    @BeforeEach
    void setUp() {
        MealPlanFixtures plans = new MealPlanFixtures(jdbc);
        user = plans.user();
        RecipeFixtures recipes = new RecipeFixtures(jdbc);
        onion = recipes.ingredient("Hành tây IT", "PLANT");
        chicken = recipes.ingredient("Thịt gà IT", "MEAT");
        // fixture recipes: 2 servings, 100 of each ingredient
        boKho = recipes.newRecipe("Bò kho IT").ingredients(onion).create();
        gaXao = recipes.newRecipe("Gà xào hành IT").ingredients(onion, chicken).create();
        jdbc.update("INSERT INTO recipe_media (recipe_id, role, external_url) VALUES (?, 'COVER', ?)",
                boKho, "https://img.example/bo-kho.jpg");
        planId = plans.plan(user, WEEK);
        plans.item(planId, DayOfWeek.MONDAY, "LUNCH", boKho, "Bò kho IT", 4);   // x2
        plans.item(planId, DayOfWeek.TUESDAY, "DINNER", gaXao, "Gà xào hành IT", 2); // x1
    }

    private JsonNode importWeek(String mode, LocalDate week, int expectedStatus) throws Exception {
        String body = mvc.perform(post(URL)
                        .header(HeaderCurrentUser.HEADER, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weekStart\":\"%s\",\"mode\":\"%s\"}".formatted(week, mode)))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private static List<JsonNode> items(JsonNode list) {
        List<JsonNode> all = new ArrayList<>();
        list.path("aisleGroups").forEach(g -> g.path("items").forEach(all::add));
        return all;
    }

    private static JsonNode item(JsonNode list, long ingredientId) {
        return items(list).stream().filter(i -> i.path("ingredientId").asLong() == ingredientId).findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("the week's dishes become one list: onion summed over both dishes, chicken in the meat aisle")
    void importsTheWeek() throws Exception {
        JsonNode list = importWeek("MERGE", WEEK, 200);

        JsonNode onionRow = item(list, onion);
        assertThat(onionRow.path("displayName").asText()).isEqualTo("Hành tây IT");
        assertThat(onionRow.path("displayQuantity").decimalValue()).isEqualByComparingTo("300"); // 200 + 100
        assertThat(onionRow.path("sourceRecipeCount").asInt()).isEqualTo(2);
        assertThat(onionRow.path("sources").findValuesAsText("recipeTitleSnapshot"))
                .containsExactlyInAnyOrder("Bò kho IT", "Gà xào hành IT");

        assertThat(list.path("recipeCovers").path(String.valueOf(boKho)).asText()).isEqualTo("https://img.example/bo-kho.jpg");
        assertThat(list.path("recipeCovers").has(String.valueOf(gaXao))).isFalse();

        JsonNode chickenRow = item(list, chicken);
        assertThat(chickenRow.path("displayQuantity").decimalValue()).isEqualByComparingTo("100");
        assertThat(chickenRow.path("aisleCategoryId").asLong())
                .isEqualTo(jdbc.queryForObject("SELECT id FROM aisle_categories WHERE code = 'MEAT_SEAFOOD'", Long.class));

        Long planItemSources = jdbc.queryForObject("""
                SELECT count(*) FROM shopping_list_item_sources s JOIN meal_plan_items mi ON mi.id = s.meal_plan_item_id
                 WHERE mi.meal_plan_id = ?
                """, Long.class, planId);
        assertThat(planItemSources).isEqualTo(3L);
    }

    @Test
    @DisplayName("importing twice with MERGE sums again; ARCHIVE_AND_NEW starts over and keeps the old list in history")
    void mergeThenArchive() throws Exception {
        importWeek("MERGE", WEEK, 200);
        JsonNode merged = importWeek("MERGE", WEEK, 200);
        assertThat(item(merged, onion).path("displayQuantity").decimalValue()).isEqualByComparingTo("600");

        JsonNode fresh = importWeek("ARCHIVE_AND_NEW", WEEK, 200);
        assertThat(item(fresh, onion).path("displayQuantity").decimalValue()).isEqualByComparingTo("300");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM shopping_lists WHERE user_id = ? AND status = 'ARCHIVED'",
                Long.class, user)).isEqualTo(1L);
    }

    @Test
    @DisplayName("a week without dishes is 422 EMPTY_MEAL_PLAN")
    void emptyWeek() throws Exception {
        mvc.perform(post(URL).header(HeaderCurrentUser.HEADER, user).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weekStart\":\"2026-11-30\",\"mode\":\"MERGE\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("EMPTY_MEAL_PLAN"));
    }

    @Test
    @DisplayName("a missing mode is 400")
    void missingMode() throws Exception {
        mvc.perform(post(URL).header(HeaderCurrentUser.HEADER, user).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weekStart\":\"2026-11-23\"}"))
                .andExpect(status().isBadRequest());
    }
}
