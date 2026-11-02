package com.wikicook.mealplan.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

/** T049: "Thêm vào thực đơn" from a recipe asks before replacing a dish (US2-2, US2-3, quickstart Q6–Q7). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(HeaderCurrentUser.class)
class MealPlanControllerUS2IT extends AbstractPostgresIT {

    private static final String SLOT = "/api/v1/meal-plans/2026-10-19/slots/WEDNESDAY/LUNCH";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    private long user;
    private long comGa;
    private long canhChua;

    @BeforeEach
    void setUp() {
        user = new MealPlanFixtures(jdbc).user();
        RecipeFixtures f = new RecipeFixtures(jdbc);
        comGa = f.publishedRecipe("Cơm gà Hội An", "Gạo");
        canhChua = f.publishedRecipe("Canh chua cá lóc", "Cá lóc");
    }

    private MockHttpServletRequestBuilder putSlot(String body) {
        return put(SLOT).header(HeaderCurrentUser.HEADER, user).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void emptySlotIsFilled() throws Exception { // US2-2
        mvc.perform(putSlot("{\"recipeId\":%d,\"expected\":null}".formatted(canhChua)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipeName").value("Canh chua cá lóc"));
    }

    @Test
    void occupiedSlotNamesTheDishThenReplacesWhenConfirmed() throws Exception { // US2-3
        JsonNode first = json.readTree(mvc.perform(putSlot("{\"recipeId\":%d}".formatted(comGa)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        String body = mvc.perform(putSlot("{\"recipeId\":%d,\"expected\":null}".formatted(canhChua)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_OCCUPIED"))
                .andExpect(jsonPath("$.currentItem.recipeName").value("Cơm gà Hội An"))
                .andReturn().getResponse().getContentAsString();
        JsonNode current = json.readTree(body).get("currentItem");
        org.assertj.core.api.Assertions.assertThat(current.get("id").asLong()).isEqualTo(first.get("id").asLong());

        mvc.perform(putSlot("{\"recipeId\":%d,\"expected\":{\"itemId\":%d,\"version\":%d}}"
                        .formatted(canhChua, current.get("id").asLong(), current.get("version").asInt())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipeName").value("Canh chua cá lóc"));
        mvc.perform(get("/api/v1/meal-plans/2026-10-19").header(HeaderCurrentUser.HEADER, user))
                .andExpect(jsonPath("$.days[2].slots[1].item.recipeId").value(canhChua));
    }
}
