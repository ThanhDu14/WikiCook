package com.wikicook.mealplan.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

/** T077: {@code GET …/shopping-handoff} end to end (US5, quickstart Q16). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(HeaderCurrentUser.class)
class ShoppingHandoffControllerIT extends AbstractPostgresIT {

    private static final LocalDate WEEK = LocalDate.of(2026, 10, 19);
    private static final String HANDOFF = "/api/v1/meal-plans/2026-10-19/shopping-handoff";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    private MealPlanFixtures plans;
    private long user;
    private long pho;
    private long boLucLac;

    @BeforeEach
    void setUp() {
        plans = new MealPlanFixtures(jdbc);
        user = plans.user();
        RecipeFixtures r = new RecipeFixtures(jdbc);
        pho = r.publishedRecipe("Phở bò Hà Nội", "Bánh phở");
        boLucLac = r.publishedRecipe("Bò lúc lắc", "Thịt bò");
    }

    @Test
    void handsOverTheAvailableDishesWithTheirServings() throws Exception { // US5-1, US5-3
        long plan = plans.plan(user, WEEK);
        long phoItem = plans.item(plan, DayOfWeek.MONDAY, "BREAKFAST", pho, "Phở bò Hà Nội", 3);
        plans.item(plan, DayOfWeek.TUESDAY, "DINNER", boLucLac, "Bò lúc lắc", 2);
        new RecipeFixtures(jdbc).archive(boLucLac);

        mvc.perform(get(HANDOFF).header(HeaderCurrentUser.HEADER, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value("2026-10-19"))
                .andExpect(jsonPath("$.skippedUnavailable").value(1))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].mealPlanItemId").value(phoItem))
                .andExpect(jsonPath("$.items[0].recipeId").value(pho))
                .andExpect(jsonPath("$.items[0].servings").value(3))
                .andExpect(jsonPath("$.items[0].day").value("MONDAY"))
                .andExpect(jsonPath("$.items[0].meal").value("BREAKFAST"));
    }

    @Test
    void aWeekWithoutPlanHandsOverNothing() throws Exception { // US5-2
        mvc.perform(get(HANDOFF).header(HeaderCurrentUser.HEADER, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.skippedUnavailable").value(0));
    }

    @Test
    void anotherUserSeesOnlyTheirOwnWeek() throws Exception { // FR-006, NFR-004
        plans.item(plans.plan(user, WEEK), DayOfWeek.MONDAY, "LUNCH", pho, "Phở bò Hà Nội", 2);
        long other = plans.user();

        mvc.perform(get(HANDOFF).header(HeaderCurrentUser.HEADER, other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void withoutSignInIs401() throws Exception {
        mvc.perform(get(HANDOFF))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void weekStartMustBeAMonday() throws Exception {
        mvc.perform(get("/api/v1/meal-plans/2026-10-21/shopping-handoff").header(HeaderCurrentUser.HEADER, user))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WEEK_START"));
    }
}
