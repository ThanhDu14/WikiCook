package com.wikicook.mealplan.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/** T114 (merges T032, T049, T059): the item endpoints of openapi 0.2.0 end to end (quickstart Q1–Q9). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(HeaderCurrentUser.class)
class MealPlanControllerIT extends AbstractPostgresIT {

    private static final String WEEK = "/api/v1/meal-plans/2026-10-19";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    private long user;
    private long pho;
    private long canh;
    private long ca;

    @BeforeEach
    void setUp() {
        user = new MealPlanFixtures(jdbc).user();
        RecipeFixtures r = new RecipeFixtures(jdbc);
        pho = r.publishedRecipe("Phở bò Hà Nội", "Bánh phở");
        canh = r.publishedRecipe("Canh chua cá lóc", "Cá lóc");
        ca = r.publishedRecipe("Cá kho tộ", "Cá basa");
    }

    private MockHttpServletRequestBuilder as(long userId, MockHttpServletRequestBuilder request) {
        return request.header(HeaderCurrentUser.HEADER, userId).contentType(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request) {
        return as(user, request);
    }

    /** POST …/meals/{meal}/items; returns the PlanItem. */
    private JsonNode add(String meal, long recipeId) throws Exception {
        String body = mvc.perform(as(post(WEEK + "/meals/" + meal + "/items"))
                        .content("{\"recipeId\":%d}".formatted(recipeId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("item");
    }

    private static String item(JsonNode item) {
        return WEEK + "/items/" + item.get("id").asLong();
    }

    @Test
    void emptyWeekHasSevenDaysOfThreeEmptyMeals() throws Exception { // US1-1
        mvc.perform(as(get(WEEK)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value("2026-10-19"))
                .andExpect(jsonPath("$.weekEnd").value("2026-10-25"))
                .andExpect(jsonPath("$.empty").value(true))
                .andExpect(jsonPath("$.days", hasSize(7)))
                .andExpect(jsonPath("$.days[0].date").value("2026-10-19"))
                .andExpect(jsonPath("$.days[6].day").value("SUNDAY"))
                .andExpect(jsonPath("$.days[3].slots", hasSize(3)))
                .andExpect(jsonPath("$.days[3].slots[0].meal").value("BREAKFAST"))
                .andExpect(jsonPath("$.days[3].slots[2].meal").value("DINNER"))
                .andExpect(jsonPath("$.days[3].slots[2].items", hasSize(0)));
    }

    @Test
    void weekStartMustBeAMonday() throws Exception {
        mvc.perform(as(get("/api/v1/meal-plans/2026-10-20")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WEEK_START"));
    }

    @Test
    void withoutSignInEveryCallIs401() throws Exception {
        mvc.perform(get(WEEK))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(post(WEEK + "/meals/MONDAY/LUNCH/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipeId\":%d}".formatted(pho)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addReturnsThePlacedItemAndTheWeekShowsIt() throws Exception { // US1-2, US1-5, US2-2
        mvc.perform(as(post(WEEK + "/meals/TUESDAY/BREAKFAST/items")).content("{\"recipeId\":%d}".formatted(pho)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.day").value("TUESDAY"))
                .andExpect(jsonPath("$.meal").value("BREAKFAST"))
                .andExpect(jsonPath("$.item.recipeName").value("Phở bò Hà Nội"))
                .andExpect(jsonPath("$.item.position").value(0))
                .andExpect(jsonPath("$.item.servings").value(2))
                .andExpect(jsonPath("$.item.source").value("MANUAL"))
                .andExpect(jsonPath("$.item.available").value(true));
        mvc.perform(as(get(WEEK)))
                .andExpect(jsonPath("$.empty").value(false))
                .andExpect(jsonPath("$.availableItemCount").value(1))
                .andExpect(jsonPath("$.days[1].slots[0].items[0].recipeId").value(pho))
                .andExpect(jsonPath("$.days[1].slots[0].items[0].totalTimeMinutes").value(40));
    }

    @Test
    void aMealTakesFourDishesInOrderThenIsFull() throws Exception { // US1-11, US2-3, FR-004
        add("MONDAY/DINNER", canh);
        add("MONDAY/DINNER", ca);
        add("MONDAY/DINNER", pho);
        add("MONDAY/DINNER", canh); // the same recipe twice is fine

        mvc.perform(as(post(WEEK + "/meals/MONDAY/DINNER/items")).content("{\"recipeId\":%d}".formatted(ca)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MEAL_FULL"));
        mvc.perform(as(get(WEEK)))
                .andExpect(jsonPath("$.days[0].slots[2].items", hasSize(4)))
                .andExpect(jsonPath("$.days[0].slots[2].items[0].recipeName").value("Canh chua cá lóc"))
                .andExpect(jsonPath("$.days[0].slots[2].items[1].recipeName").value("Cá kho tộ"))
                .andExpect(jsonPath("$.days[0].slots[2].items[3].position").value(3))
                .andExpect(jsonPath("$.availableItemCount").value(4));
    }

    @Test
    void servingsOutsideOneToTwentyAreRefusedAndKept() throws Exception { // edge case servings, US1-7
        JsonNode dish = add("MONDAY/LUNCH", pho);

        for (String servings : new String[] {"0", "21", "2.5", "null"}) {
            mvc.perform(as(patch(item(dish) + "/servings")).content("{\"servings\":%s,\"version\":0}"
                            .formatted(servings)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        mvc.perform(as(post(WEEK + "/meals/MONDAY/DINNER/items"))
                        .content("{\"recipeId\":%d,\"servings\":21}".formatted(pho)))
                .andExpect(status().isBadRequest());

        mvc.perform(as(patch(item(dish) + "/servings")).content("{\"servings\":6,\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.item.servings").value(6))
                .andExpect(jsonPath("$.item.version").value(1));
        mvc.perform(as(get(WEEK))).andExpect(jsonPath("$.days[0].slots[1].items[0].servings").value(6));
    }

    @Test
    void unpublishedRecipeIs422() throws Exception { // FR-010
        long draft = new RecipeFixtures(jdbc).recipe("Bản nháp chưa duyệt", "ARCHIVED", "Gạo");

        mvc.perform(as(post(WEEK + "/meals/MONDAY/LUNCH/items")).content("{\"recipeId\":%d}".formatted(draft)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_AVAILABLE"));
    }

    @Test
    void replaceKeepsServingsAndPosition() throws Exception { // US3-5
        add("MONDAY/DINNER", canh);
        JsonNode second = add("MONDAY/DINNER", ca);
        mvc.perform(as(patch(item(second) + "/servings")).content("{\"servings\":5,\"version\":0}"))
                .andExpect(status().isOk());

        mvc.perform(as(put(item(second))).content("{\"recipeId\":%d,\"version\":1}".formatted(pho)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.item.id").value(second.get("id").asLong()))
                .andExpect(jsonPath("$.item.recipeName").value("Phở bò Hà Nội"))
                .andExpect(jsonPath("$.item.servings").value(5))
                .andExpect(jsonPath("$.item.position").value(1));
        mvc.perform(as(put(item(second))).content("{\"recipeId\":%d,\"version\":1}".formatted(canh)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_CHANGED"));
    }

    @Test
    void removeMovesTheLaterDishesUp() throws Exception { // US3-6
        JsonNode first = add("MONDAY/DINNER", canh);
        add("MONDAY/DINNER", ca);

        mvc.perform(as(delete(item(first))).param("version", "3"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_CHANGED"));
        mvc.perform(as(delete(item(first))).param("version", "0")).andExpect(status().isNoContent());

        mvc.perform(as(get(WEEK)))
                .andExpect(jsonPath("$.days[0].slots[2].items", hasSize(1)))
                .andExpect(jsonPath("$.days[0].slots[2].items[0].recipeName").value("Cá kho tộ"))
                .andExpect(jsonPath("$.days[0].slots[2].items[0].position").value(0));
    }

    @Test
    void moveToAnotherMealReturnsBothMeals() throws Exception { // US3-1, US3-2
        JsonNode first = add("MONDAY/LUNCH", canh);
        add("MONDAY/LUNCH", ca);
        add("FRIDAY/DINNER", pho);

        mvc.perform(as(post(item(first) + "/move")).content("""
                        {"day":"FRIDAY","meal":"DINNER","position":0,"version":0}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meals", hasSize(2)))
                .andExpect(jsonPath("$.meals[0].day").value("MONDAY"))
                .andExpect(jsonPath("$.meals[0].meal").value("LUNCH"))
                .andExpect(jsonPath("$.meals[0].items[0].recipeName").value("Cá kho tộ"))
                .andExpect(jsonPath("$.meals[0].items[0].position").value(0))
                .andExpect(jsonPath("$.meals[1].day").value("FRIDAY"))
                .andExpect(jsonPath("$.meals[1].items[0].recipeName").value("Canh chua cá lóc"))
                .andExpect(jsonPath("$.meals[1].items[1].recipeName").value("Phở bò Hà Nội"));
        mvc.perform(as(get(WEEK)))
                .andExpect(jsonPath("$.days[0].slots[1].items", hasSize(1)))
                .andExpect(jsonPath("$.days[4].slots[2].items", hasSize(2)));
    }

    @Test
    void reorderInsideAMealReturnsThatMeal() throws Exception { // US3-4 "Lên trên"
        add("MONDAY/DINNER", canh);
        JsonNode second = add("MONDAY/DINNER", ca);

        mvc.perform(as(post(item(second) + "/move")).content("""
                        {"day":"MONDAY","meal":"DINNER","position":0,"version":0}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meals", hasSize(1)))
                .andExpect(jsonPath("$.meals[0].items[0].recipeName").value("Cá kho tộ"))
                .andExpect(jsonPath("$.meals[0].items[1].recipeName").value("Canh chua cá lóc"));
    }

    @Test
    void moveIntoAFullMealIs409AndNothingMoves() throws Exception { // US3-3
        JsonNode lunch = add("MONDAY/LUNCH", canh);
        for (int i = 0; i < 4; i++) {
            add("TUESDAY/DINNER", ca);
        }

        mvc.perform(as(post(item(lunch) + "/move")).content("""
                        {"day":"TUESDAY","meal":"DINNER","version":0}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEAL_FULL"));
        mvc.perform(as(get(WEEK))).andExpect(jsonPath("$.days[0].slots[1].items", hasSize(1)));
    }

    @Test
    void moveWithAStaleVersionOrABadPositionIsRefused() throws Exception {
        JsonNode dish = add("MONDAY/LUNCH", canh);

        mvc.perform(as(post(item(dish) + "/move")).content("""
                        {"day":"FRIDAY","meal":"DINNER","version":2}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_CHANGED"));
        mvc.perform(as(post(item(dish) + "/move")).content("""
                        {"day":"FRIDAY","meal":"DINNER","position":4,"version":0}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void clearADayAlsoWhenItIsAlreadyEmpty() throws Exception { // US3-7
        add("MONDAY/LUNCH", canh);
        add("MONDAY/LUNCH", ca);

        mvc.perform(as(delete(WEEK + "/days/MONDAY"))).andExpect(status().isNoContent());
        mvc.perform(as(get(WEEK))).andExpect(jsonPath("$.empty").value(true));
        mvc.perform(as(delete(WEEK + "/days/MONDAY"))).andExpect(status().isNoContent());
        mvc.perform(as(delete("/api/v1/meal-plans/2026-11-02/days/SUNDAY"))).andExpect(status().isNoContent());
    }

    @Test
    void anEditFromAnotherTabOnARemovedDishIs409() throws Exception { // edge case: two tabs
        JsonNode dish = add("MONDAY/LUNCH", pho);
        mvc.perform(as(delete(item(dish))).param("version", "0")).andExpect(status().isNoContent());

        mvc.perform(as(patch(item(dish) + "/servings")).content("{\"servings\":3,\"version\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_CHANGED"));
    }

    @Test
    void anotherUsersOrAnotherWeeksItemIsNotReachable() throws Exception { // FR-006, NFR-004
        JsonNode dish = add("MONDAY/LUNCH", pho);
        long stranger = new MealPlanFixtures(jdbc).user();

        mvc.perform(as(stranger, get(WEEK))).andExpect(jsonPath("$.empty").value(true));
        mvc.perform(as(stranger, post(item(dish) + "/move")).content("""
                        {"day":"FRIDAY","meal":"BREAKFAST","version":0}"""))
                .andExpect(status().isConflict());
        mvc.perform(as(stranger, delete(item(dish))).param("version", "0")).andExpect(status().isConflict());
        mvc.perform(as(delete("/api/v1/meal-plans/2026-11-02/items/" + dish.get("id").asLong()))
                        .param("version", "0"))
                .andExpect(status().isConflict());
        mvc.perform(as(get(WEEK))).andExpect(jsonPath("$.days[0].slots[1].items[0].recipeName")
                .value("Phở bò Hà Nội"));
    }

    @Test
    void twoDevicesOfOneUserSeeTheSameWeek() throws Exception { // US1-9, SC-004
        add("SUNDAY/DINNER", pho);

        mvc.perform(as(get(WEEK))).andExpect(jsonPath("$.days[6].slots[2].items[0].recipeId").value(pho));
    }
}
