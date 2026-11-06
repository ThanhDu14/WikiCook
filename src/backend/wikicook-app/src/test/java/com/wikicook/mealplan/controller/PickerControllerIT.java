package com.wikicook.mealplan.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** T033: the picker endpoint validates its parameters and returns a PickerPage. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(HeaderCurrentUser.class)
class PickerControllerIT extends AbstractPostgresIT {

    private static final String PICKER = "/api/v1/meal-plans/picker/recipes";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    private long user;

    @BeforeEach
    void setUp() {
        RecipeFixtures.clear(jdbc);
        user = new MealPlanFixtures(jdbc).user();
        new RecipeFixtures(jdbc).newRecipe("Trứng chiên hành").meals(true, false, false).create();
    }

    @Test
    void returnsAPickerPage() throws Exception {
        mvc.perform(get(PICKER).param("meal", "BREAKFAST").param("q", "trung").header(HeaderCurrentUser.HEADER, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.content[0].title").value("Trứng chiên hành"))
                .andExpect(jsonPath("$.content[0].mealSuited").value(true))
                .andExpect(jsonPath("$.content[0].conflicts").isEmpty());
    }

    @Test
    void collectionsScopeIsEmptyWithoutCollections() throws Exception { // ⛓ 080 not there yet
        mvc.perform(get(PICKER).param("meal", "BREAKFAST").param("scope", "COLLECTIONS")
                        .header(HeaderCurrentUser.HEADER, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void mealIsRequiredAndSizeAtMostFifty() throws Exception {
        mvc.perform(get(PICKER).header(HeaderCurrentUser.HEADER, user))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get(PICKER).param("meal", "LUNCH").param("size", "51").header(HeaderCurrentUser.HEADER, user))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get(PICKER).param("meal", "SNACK").header(HeaderCurrentUser.HEADER, user))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void withoutSignInIs401() throws Exception {
        mvc.perform(get(PICKER).param("meal", "LUNCH"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
