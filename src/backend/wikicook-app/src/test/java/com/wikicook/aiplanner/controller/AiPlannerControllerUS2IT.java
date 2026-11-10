package com.wikicook.aiplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.ai.testing.FakeEmbeddingClient;
import com.wikicook.ai.testing.FakeLlmClient;
import com.wikicook.aiplanner.dto.ConfirmRequest;
import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.aiplanner.support.RecipeSeed;
import com.wikicook.devstub.DevUserProvisioner;
import com.wikicook.devstub.StubCurrentUser;
import com.wikicook.mealplan.api.DraftItem;
import com.wikicook.mealplan.api.MealPlanWriteFacade;
import com.wikicook.mealplan.domain.ItemSource;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.service.WeekCalendar;
import com.wikicook.support.AbstractPostgresIT;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** T048: confirming a draft writes the week through 030 (US2-2, US2-6, FR-018, FR-019). */
@SpringBootTest(properties = {"wikicook.dev-stubs.enabled=true", "wikicook.ai.mode=mock"})
@AutoConfigureMockMvc
class AiPlannerControllerUS2IT extends AbstractPostgresIT {

    private static final AtomicLong NEXT_USER = new AtomicLong(2000);

    @TestConfiguration
    static class FakeClients {

        @Bean
        @Primary
        FakeLlmClient fakeLlmClient() {
            return new FakeLlmClient();
        }

        @Bean
        @Primary
        FakeEmbeddingClient fakeEmbeddingClient() {
            return new FakeEmbeddingClient(768);
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    FakeLlmClient llm;

    @Autowired
    MealPlanWriteFacade mealPlans;

    @Autowired
    DevUserProvisioner provisioner;

    private RecipeSeed.Seeded seed;
    private long user;
    private final LocalDate nextWeek = WeekCalendar.currentWeekStart(Clock.systemUTC()).plusWeeks(1);

    @BeforeEach
    void seed() {
        RecipeFixtures.clear(jdbc);
        seed = RecipeSeed.seed(new RecipeFixtures(jdbc));
        user = NEXT_USER.incrementAndGet();
        provisioner.ensure(user);
    }

    private ResultActions confirm(Object body) throws Exception {
        return mvc.perform(post("/api/v1/ai-planner/weeks/{weekStart}/confirm", nextWeek)
                .header(StubCurrentUser.HEADER, user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    private ResultActions week() throws Exception {
        return mvc.perform(get("/api/v1/meal-plans/{weekStart}", nextWeek).header(StubCurrentUser.HEADER, user));
    }

    private static ConfirmRequest.Item item(DayOfWeek day, Meal meal, long recipeId, ItemSource source) {
        return new ConfirmRequest.Item(day, meal, recipeId, 3, source);
    }

    @Test
    void confirmedDishesShowUpInTheWeek_US2_2() throws Exception {
        long breakfast = seed.breakfast().getFirst();
        long lunch = seed.lunchDinner().getFirst();
        long dinner = seed.lunchDinner().get(1);

        confirm(new ConfirmRequest(UUID.randomUUID(), List.of(
                item(DayOfWeek.MONDAY, Meal.BREAKFAST, breakfast, ItemSource.AI),
                item(DayOfWeek.MONDAY, Meal.LUNCH, lunch, ItemSource.AUTO),
                item(DayOfWeek.MONDAY, Meal.DINNER, dinner, ItemSource.MANUAL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(3))
                .andExpect(jsonPath("$.skippedOccupied").value(0))
                .andExpect(jsonPath("$.skippedUnavailable").value(0));

        week().andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].slots[0].items", hasSize(1)))
                .andExpect(jsonPath("$.days[0].slots[0].items[0].recipeId").value(breakfast))
                .andExpect(jsonPath("$.days[0].slots[0].items[0].source").value("AI"))
                .andExpect(jsonPath("$.days[0].slots[0].items[0].servings").value(3))
                .andExpect(jsonPath("$.days[0].slots[1].items[0].source").value("AUTO"))
                .andExpect(jsonPath("$.days[0].slots[2].items[0].source").value("MANUAL"));
        // Confirming is not a generation: nothing is counted or logged (FR-018).
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ai_requests WHERE user_id = ?", Integer.class, user))
                .isZero();
        assertThat(llm.requests()).isEmpty();
    }

    @Test
    void mealFilledOnAnotherDeviceIsSkipped_US2_6() throws Exception {
        long theirs = seed.allDay().getFirst();
        mealPlans.saveDraft(user, nextWeek, List.of(
                new DraftItem(DayOfWeek.TUESDAY, Meal.LUNCH, theirs, 2, ItemSource.MANUAL)));

        confirm(new ConfirmRequest(UUID.randomUUID(), List.of(
                item(DayOfWeek.TUESDAY, Meal.LUNCH, seed.lunchDinner().getFirst(), ItemSource.AI),
                item(DayOfWeek.TUESDAY, Meal.DINNER, seed.lunchDinner().get(1), ItemSource.AI))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(1))
                .andExpect(jsonPath("$.skippedOccupied").value(1));

        week().andExpect(jsonPath("$.days[1].slots[1].items", hasSize(1)))
                .andExpect(jsonPath("$.days[1].slots[1].items[0].recipeId").value(theirs))
                .andExpect(jsonPath("$.days[1].slots[1].items[0].source").value("MANUAL"));
    }

    @Test
    void secondTabConfirmingTheSameDraftSkipsEverything() throws Exception {
        ConfirmRequest draft = new ConfirmRequest(UUID.randomUUID(), List.of(
                item(DayOfWeek.WEDNESDAY, Meal.LUNCH, seed.lunchDinner().getFirst(), ItemSource.AI),
                item(DayOfWeek.WEDNESDAY, Meal.DINNER, seed.lunchDinner().get(1), ItemSource.AI)));

        confirm(draft).andExpect(jsonPath("$.saved").value(2));
        confirm(draft)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(0))
                .andExpect(jsonPath("$.skippedOccupied").value(2));
    }

    @Test
    void sameRecipeInAPlannedMealAndAnAiMealIsAllowed() throws Exception {
        long recipe = seed.allDay().getFirst();
        mealPlans.saveDraft(user, nextWeek, List.of(
                new DraftItem(DayOfWeek.THURSDAY, Meal.LUNCH, recipe, 2, ItemSource.MANUAL)));

        confirm(new ConfirmRequest(UUID.randomUUID(), List.of(
                item(DayOfWeek.FRIDAY, Meal.DINNER, recipe, ItemSource.AI))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(1));
    }

    @Test
    void archivedRecipeIsSkippedAsUnavailable() throws Exception {
        confirm(new ConfirmRequest(UUID.randomUUID(), List.of(
                item(DayOfWeek.SATURDAY, Meal.LUNCH, seed.archived(), ItemSource.AI),
                item(DayOfWeek.SATURDAY, Meal.DINNER, seed.lunchDinner().getFirst(), ItemSource.AI))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(1))
                .andExpect(jsonPath("$.skippedUnavailable").value(1));
    }

    @Test
    void invalidBodiesAre400() throws Exception {
        long recipe = seed.allDay().getFirst();
        List<ConfirmRequest.Item> tooMany = new ArrayList<>();
        for (int i = 0; i < 22; i++) {
            tooMany.add(item(DayOfWeek.of(i % 7 + 1), Meal.values()[i % 3], recipe, ItemSource.AI));
        }
        UUID id = UUID.randomUUID();
        List<Object> bodies = List.of(
                new ConfirmRequest(id, List.of()),
                new ConfirmRequest(id, tooMany),
                new ConfirmRequest(null, List.of(item(DayOfWeek.MONDAY, Meal.LUNCH, recipe, ItemSource.AI))),
                new ConfirmRequest(id, List.of(new ConfirmRequest.Item(DayOfWeek.MONDAY, Meal.LUNCH, recipe, 0,
                        ItemSource.AI))),
                new ConfirmRequest(id, List.of(new ConfirmRequest.Item(DayOfWeek.MONDAY, Meal.LUNCH, recipe, 21,
                        ItemSource.AI))),
                new ConfirmRequest(id, List.of(new ConfirmRequest.Item(DayOfWeek.MONDAY, Meal.LUNCH, recipe, 2,
                        null))),
                Map.of("requestId", id.toString(), "items", List.of(Map.of("day", "FUNDAY", "meal", "LUNCH",
                        "recipeId", recipe, "source", "AI"))));
        for (Object body : bodies) {
            confirm(body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        week().andExpect(jsonPath("$.empty").value(true));
    }

    @Test
    void weekStartNotAMondayIs400() throws Exception {
        mvc.perform(post("/api/v1/ai-planner/weeks/{weekStart}/confirm", nextWeek.plusDays(1))
                        .header(StubCurrentUser.HEADER, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ConfirmRequest(UUID.randomUUID(), List.of(
                                item(DayOfWeek.MONDAY, Meal.LUNCH, seed.allDay().getFirst(), ItemSource.AI))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WEEK_START"));
    }
}
