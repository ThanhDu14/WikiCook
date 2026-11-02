package com.wikicook.mealplan.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.mealplan.support.HeaderCurrentUser;
import com.wikicook.mealplan.support.MealPlanFixtures;
import com.wikicook.support.AbstractPostgresIT;
import java.time.DayOfWeek;
import java.time.LocalDate;
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

/** T059: move, swap, replace, remove and clear a day through the API (US3, quickstart Q8–Q12). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(HeaderCurrentUser.class)
class MealPlanControllerUS3IT extends AbstractPostgresIT {

    private static final String WEEK = "/api/v1/meal-plans/2026-10-19";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    private long user;
    private long phoRecipe;
    private long caRecipe;
    private long pho;
    private long ca;

    @BeforeEach
    void setUp() {
        MealPlanFixtures f = new MealPlanFixtures(jdbc);
        RecipeFixtures r = new RecipeFixtures(jdbc);
        phoRecipe = r.publishedRecipe("Phở bò", "Bánh phở");
        caRecipe = r.publishedRecipe("Cá kho tộ", "Cá basa");
        user = f.user();
        long plan = f.plan(user, LocalDate.of(2026, 10, 19));
        pho = f.item(plan, DayOfWeek.MONDAY, "LUNCH", phoRecipe, "Phở bò", 2);
        ca = f.item(plan, DayOfWeek.MONDAY, "DINNER", caRecipe, "Cá kho tộ", 5);
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request) {
        return request.header(HeaderCurrentUser.HEADER, user);
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return as(request).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void moveToAnEmptySlot() throws Exception { // US3-1
        mvc.perform(json(post(WEEK + "/moves"), """
                        {"from":{"day":"MONDAY","meal":"LUNCH","itemId":%d,"version":0},
                         "to":{"day":"FRIDAY","meal":"BREAKFAST","expected":null}}""".formatted(pho)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("MOVED"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].day").value("FRIDAY"))
                .andExpect(jsonPath("$.items[0].meal").value("BREAKFAST"))
                .andExpect(jsonPath("$.items[0].item.id").value(pho));
        mvc.perform(as(get(WEEK)))
                .andExpect(jsonPath("$.days[0].slots[1].item").isEmpty())
                .andExpect(jsonPath("$.days[4].slots[0].item.recipeName").value("Phở bò"));
    }

    @Test
    void moveOntoADishSwaps() throws Exception { // US3-2
        mvc.perform(json(post(WEEK + "/moves"), """
                        {"from":{"day":"MONDAY","meal":"LUNCH","itemId":%d,"version":0},
                         "to":{"day":"MONDAY","meal":"DINNER","expected":{"itemId":%d,"version":0}}}"""
                        .formatted(pho, ca)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("SWAPPED"))
                .andExpect(jsonPath("$.items.length()").value(2));
        mvc.perform(as(get(WEEK)))
                .andExpect(jsonPath("$.days[0].slots[1].item.recipeName").value("Cá kho tộ"))
                .andExpect(jsonPath("$.days[0].slots[1].item.servings").value(5))
                .andExpect(jsonPath("$.days[0].slots[2].item.recipeName").value("Phở bò"));
    }

    @Test
    void replaceKeepsTheServings() throws Exception { // US3-4
        mvc.perform(json(put(WEEK + "/slots/MONDAY/DINNER"), """
                        {"recipeId":%d,"expected":{"itemId":%d,"version":0}}""".formatted(phoRecipe, ca)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipeName").value("Phở bò"))
                .andExpect(jsonPath("$.servings").value(5));
    }

    @Test
    void removeADish() throws Exception { // US3-5
        mvc.perform(as(delete(WEEK + "/slots/MONDAY/LUNCH").param("itemId", String.valueOf(pho))
                        .param("version", "0")))
                .andExpect(status().isNoContent());
        mvc.perform(as(get(WEEK))).andExpect(jsonPath("$.days[0].slots[1].item").isEmpty());
    }

    @Test
    void removeWithAStaleVersionIs409() throws Exception {
        mvc.perform(as(delete(WEEK + "/slots/MONDAY/LUNCH").param("itemId", String.valueOf(pho))
                        .param("version", "3")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_CHANGED"));
    }

    @Test
    void clearADayAlsoWhenItIsAlreadyEmpty() throws Exception { // US3-6
        mvc.perform(as(delete(WEEK + "/days/MONDAY"))).andExpect(status().isNoContent());
        mvc.perform(as(get(WEEK))).andExpect(jsonPath("$.empty").value(true));
        mvc.perform(as(delete(WEEK + "/days/MONDAY"))).andExpect(status().isNoContent());
        mvc.perform(as(delete("/api/v1/meal-plans/2026-11-02/days/SUNDAY"))).andExpect(status().isNoContent());
    }

    @Test
    void anEditFromAnotherTabOnARemovedDishIs409() throws Exception { // edge case: two tabs
        mvc.perform(as(delete(WEEK + "/slots/MONDAY/LUNCH").param("itemId", String.valueOf(pho))
                .param("version", "0"))).andExpect(status().isNoContent());

        mvc.perform(json(patch(WEEK + "/slots/MONDAY/LUNCH/servings"), """
                        {"servings":3,"expected":{"itemId":%d,"version":0}}""".formatted(pho)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_CHANGED"));
    }

    @Test
    void moveWithAStaleTargetIs409() throws Exception {
        mvc.perform(json(post(WEEK + "/moves"), """
                        {"from":{"day":"MONDAY","meal":"LUNCH","itemId":%d,"version":0},
                         "to":{"day":"MONDAY","meal":"DINNER","expected":null}}""".formatted(pho)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_CHANGED"));
    }

    @Test
    void anotherUsersItemCannotBeMoved() throws Exception { // FR-006
        long stranger = new MealPlanFixtures(jdbc).user();
        mvc.perform(post(WEEK + "/moves").header(HeaderCurrentUser.HEADER, stranger)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"from":{"day":"MONDAY","meal":"LUNCH","itemId":%d,"version":0},
                                 "to":{"day":"FRIDAY","meal":"BREAKFAST"}}""".formatted(pho)))
                .andExpect(status().isConflict());
        mvc.perform(as(get(WEEK))).andExpect(jsonPath("$.days[0].slots[1].item.recipeName").value("Phở bò"));
    }
}
