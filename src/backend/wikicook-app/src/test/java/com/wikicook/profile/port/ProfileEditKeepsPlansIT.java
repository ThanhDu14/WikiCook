package com.wikicook.profile.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.mealplan.support.MealPlanFixtures;
import com.wikicook.profile.support.ProfileFixtures;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.HeaderCurrentUser;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

/**
 * T107 (US5-5, FR-024): editing the profile never rewrites what was already made from it. Dishes already on the plan
 * keep their servings and the shopping list keeps its lines; the next thing the person does reads the new profile.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({HeaderCurrentUser.class, AuthTestSupport.class})
class ProfileEditKeepsPlansIT extends AbstractPostgresIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AuthTestSupport support;

    ProfileFixtures fixtures;
    RecipeFixtures recipes;
    long user;
    LocalDate week;

    @BeforeEach
    void setUp() {
        RecipeFixtures.clear(jdbc);
        fixtures = new ProfileFixtures(jdbc);
        recipes = new RecipeFixtures(jdbc);
        user = support.member();
        week = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(2);
    }

    private void saveProfile(Set<Long> allergens, Integer household, int version) throws Exception {
        mvc.perform(put("/api/v1/me/dietary-profile").header(HeaderCurrentUser.HEADER, user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(ProfileFixtures.body(allergens, Set.of(), "NONE", Set.of(), household, null, version)))
                .andExpect(status().isOk());
    }

    private List<Map<String, Object>> planItems() {
        return jdbc.queryForList("""
                SELECT i.id, i.recipe_id, i.servings, i.day_of_week, i.meal, i.position, i.recipe_name_snapshot
                  FROM meal_plan_items i JOIN meal_plans p ON p.id = i.meal_plan_id WHERE p.user_id = ? ORDER BY i.id
                """, user);
    }

    private List<Map<String, Object>> shoppingLines() {
        return jdbc.queryForList("""
                SELECT i.id, i.ingredient_id, i.quantity, i.is_checked, i.version
                  FROM shopping_list_items i JOIN shopping_lists l ON l.id = i.list_id WHERE l.user_id = ? ORDER BY i.id
                """, user);
    }

    @Test
    @DisplayName("US5-5: changing the household size and adding an allergen leaves the plan and the shopping list untouched")
    void editDoesNotRewritePlansOrLists() throws Exception {
        long peanut = fixtures.allergen("PEANUT");
        long peanutButter = fixtures.ingredient("Bơ đậu phộng PE", "PLANT");
        fixtures.tagIngredientWithAllergen(peanutButter, peanut);
        long dish = recipes.newRecipe("Bánh mì bơ đậu phộng PE").ingredients(peanutButter).create();
        saveProfile(Set.of(), 2, 0);
        MealPlanFixtures plans = new MealPlanFixtures(jdbc);
        long plan = plans.plan(user, week);
        plans.item(plan, DayOfWeek.TUESDAY, "LUNCH", dish, "Bánh mì bơ đậu phộng PE", 3);
        long list = jdbc.queryForObject("INSERT INTO shopping_lists (user_id, status) VALUES (?, 'ACTIVE') RETURNING id",
                Long.class, user);
        long aisle = jdbc.queryForObject("SELECT id FROM aisle_categories LIMIT 1", Long.class);
        jdbc.update("""
                INSERT INTO shopping_list_items (list_id, client_uuid, ingredient_id, quantity, aisle_category_id)
                VALUES (?, ?, ?, 200, ?)
                """, list, UUID.randomUUID(), peanutButter, aisle);
        List<Map<String, Object>> planBefore = planItems();
        List<Map<String, Object>> shoppingBefore = shoppingLines();

        saveProfile(Set.of(peanut), 8, 1); // now allergic to the dish already on the plan, and a household of 8

        assertThat(planItems()).isEqualTo(planBefore);
        assertThat(shoppingLines()).isEqualTo(shoppingBefore);
        assertThat(planItems().getFirst().get("servings")).isEqualTo(3);
    }

    @Test
    @DisplayName("US5-5: the next action reads the new profile - a new dish gets the new household size")
    void nextAddUsesTheNewHouseholdSize() throws Exception {
        long rice = fixtures.ingredient("Gạo PE");
        long first = recipes.newRecipe("Cơm trắng PE").ingredients(rice).create();
        long second = recipes.newRecipe("Cơm chiên PE").ingredients(rice).create();
        saveProfile(Set.of(), 2, 0);
        String weekUrl = "/api/v1/meal-plans/" + week;
        mvc.perform(post(weekUrl + "/meals/TUESDAY/LUNCH/items").header(HeaderCurrentUser.HEADER, user)
                .contentType(MediaType.APPLICATION_JSON).content("{\"recipeId\":%d}".formatted(first)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.item.servings").value(2));

        saveProfile(Set.of(), 8, 1);

        mvc.perform(post(weekUrl + "/meals/TUESDAY/DINNER/items").header(HeaderCurrentUser.HEADER, user)
                .contentType(MediaType.APPLICATION_JSON).content("{\"recipeId\":%d}".formatted(second)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.item.servings").value(8));
        assertThat(planItems()).extracting(item -> item.get("servings")).containsExactly(2, 8); // the first one stays 2
    }

    @Test
    @DisplayName("US5-5: the next picker call flags a planned dish for the allergen added meanwhile")
    void nextPickerSeesTheNewAllergen() throws Exception {
        long peanut = fixtures.allergen("PEANUT");
        long peanutButter = fixtures.ingredient("Bơ đậu phộng PE", "PLANT");
        fixtures.tagIngredientWithAllergen(peanutButter, peanut);
        recipes.newRecipe("Bánh mì bơ đậu phộng PE").ingredients(peanutButter).create();
        mvc.perform(get("/api/v1/meal-plans/picker/recipes").param("meal", "LUNCH").header(HeaderCurrentUser.HEADER, user))
                .andExpect(jsonPath("$.content[0].conflicts").isEmpty());

        saveProfile(Set.of(peanut), null, 0); // the profile was empty: the first real save is version 0

        mvc.perform(get("/api/v1/meal-plans/picker/recipes").param("meal", "LUNCH").header(HeaderCurrentUser.HEADER, user))
                .andExpect(jsonPath("$.content[0].conflicts[0].type").value("ALLERGEN"));
    }
}
