package com.wikicook.mealplan.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.mealplan.support.HeaderCurrentUser;
import com.wikicook.mealplan.support.MealPlanFixtures;
import com.wikicook.support.AbstractPostgresIT;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * T072: weeks other than the current one (US4). "Now" is Sunday 2026-10-18 in Vietnam, so the current week starts
 * 2026-10-12, 2026-09-28 is in the past and 2026-10-19 is next week.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({HeaderCurrentUser.class, MealPlanControllerUS4IT.FixedClock.class})
class MealPlanControllerUS4IT extends AbstractPostgresIT {

    private static final String PAST = "/api/v1/meal-plans/2026-09-28";
    private static final String CURRENT = "/api/v1/meal-plans/2026-10-12";
    private static final String NEXT = "/api/v1/meal-plans/2026-10-19";

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClock {

        /** 2026-10-18 10:00 in Vietnam. */
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-10-18T03:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    private long user;
    private long pho;
    private long canh;

    @BeforeEach
    void setUp() {
        user = new MealPlanFixtures(jdbc).user();
        RecipeFixtures r = new RecipeFixtures(jdbc);
        pho = r.publishedRecipe("Phở bò Hà Nội", "Bánh phở");
        canh = r.publishedRecipe("Canh chua cá lóc", "Cá lóc");
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request) {
        return request.header(HeaderCurrentUser.HEADER, user).contentType(MediaType.APPLICATION_JSON);
    }

    private JsonNode add(String week, String meal, long recipeId) throws Exception {
        String body = mvc.perform(as(post(week + "/meals/" + meal + "/items"))
                        .content("{\"recipeId\":%d}".formatted(recipeId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("item");
    }

    @Test
    void aPastWeekCanStillBeReadAndEdited() throws Exception { // US4-5, FR-023
        mvc.perform(as(get(PAST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value("2026-09-28"))
                .andExpect(jsonPath("$.today").doesNotExist());

        JsonNode item = add(PAST, "WEDNESDAY/DINNER", pho);
        mvc.perform(as(put(PAST + "/items/" + item.get("id").asLong()))
                        .content("{\"recipeId\":%d,\"version\":%d}".formatted(canh, item.get("version").asInt())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.item.recipeName").value("Canh chua cá lóc"));

        mvc.perform(as(get(PAST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empty").value(false))
                .andExpect(jsonPath("$.days[2].slots[2].items", hasSize(1)))
                .andExpect(jsonPath("$.days[2].slots[2].items[0].recipeId").value(canh));
    }

    @Test
    void aDishPlannedForNextWeekIsStillThereAfterVisitingThisWeek() throws Exception { // US4-1, US4-4
        add(NEXT, "MONDAY/LUNCH", pho);

        mvc.perform(as(get(CURRENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today").value("2026-10-18"))
                .andExpect(jsonPath("$.empty").value(true));
        mvc.perform(as(get(NEXT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today").doesNotExist())
                .andExpect(jsonPath("$.empty").value(false))
                .andExpect(jsonPath("$.days[0].slots[1].items[0].recipeId").value(pho));
    }
}
