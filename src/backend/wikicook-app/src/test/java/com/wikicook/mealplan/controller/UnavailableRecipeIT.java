package com.wikicook.mealplan.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

/** T083: dishes whose recipe was archived or deleted after planning (US6, quickstart Q15). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(HeaderCurrentUser.class)
class UnavailableRecipeIT extends AbstractPostgresIT {

    private static final LocalDate WEEK = LocalDate.of(2026, 10, 19);
    private static final String URL = "/api/v1/meal-plans/2026-10-19";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    private MealPlanFixtures plans;
    private RecipeFixtures recipes;
    private long user;
    private long plan;
    private long boLucLac;

    @BeforeEach
    void setUp() {
        plans = new MealPlanFixtures(jdbc);
        recipes = new RecipeFixtures(jdbc);
        user = plans.user();
        plan = plans.plan(user, WEEK);
        boLucLac = recipes.publishedRecipe("Bò lúc lắc", "Thịt bò");
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request) {
        return request.header(HeaderCurrentUser.HEADER, user).contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void anArchivedRecipeKeepsItsSavedNameAndIsMarkedUnavailable() throws Exception { // US6-1, FR-020
        plans.item(plan, DayOfWeek.FRIDAY, "DINNER", boLucLac, "Bò lúc lắc (tên lúc thêm)", 2);
        recipes.archive(boLucLac);

        mvc.perform(as(get(URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empty").value(false))
                .andExpect(jsonPath("$.availableItemCount").value(0))
                .andExpect(jsonPath("$.days[4].slots[2].items[0].available").value(false))
                .andExpect(jsonPath("$.days[4].slots[2].items[0].recipeName").value("Bò lúc lắc (tên lúc thêm)"))
                .andExpect(jsonPath("$.days[4].slots[2].items[0].coverImageUrl").doesNotExist())
                .andExpect(jsonPath("$.days[4].slots[2].items[0].recipeId").value(boLucLac));
    }

    @Test
    void aDeletedRecipeLeavesTheDishWithoutRecipe() throws Exception { // US6-1, data-model ON DELETE SET NULL
        plans.item(plan, DayOfWeek.MONDAY, "LUNCH", boLucLac, "Bò lúc lắc", 2);
        jdbc.update("DELETE FROM recipes WHERE id = ?", boLucLac);

        mvc.perform(as(get(URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].slots[1].items[0].available").value(false))
                .andExpect(jsonPath("$.days[0].slots[1].items[0].recipeId").doesNotExist())
                .andExpect(jsonPath("$.days[0].slots[1].items[0].recipeName").value("Bò lúc lắc"));
    }

    @Test
    void anUnavailableDishCanBeReplacedButNotWithAnArchivedRecipe() throws Exception { // US6-3
        long item = plans.item(plan, DayOfWeek.MONDAY, "LUNCH", boLucLac, "Bò lúc lắc", 2);
        recipes.archive(boLucLac);
        long pho = recipes.publishedRecipe("Phở bò Hà Nội", "Bánh phở");
        long archived = recipes.recipe("Gà rang muối", "ARCHIVED", "Thịt gà");

        mvc.perform(as(put(URL + "/items/" + item)).content("{\"recipeId\":%d,\"version\":0}".formatted(archived)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_AVAILABLE"));
        mvc.perform(as(post(URL + "/meals/MONDAY/DINNER/items")).content("{\"recipeId\":%d}".formatted(archived)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_AVAILABLE"));
        mvc.perform(as(put(URL + "/items/" + item)).content("{\"recipeId\":%d,\"version\":0}".formatted(pho)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.item.available").value(true))
                .andExpect(jsonPath("$.item.recipeName").value("Phở bò Hà Nội"));
    }
}
