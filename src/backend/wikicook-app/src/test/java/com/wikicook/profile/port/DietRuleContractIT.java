package com.wikicook.profile.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.common.diet.DietRestrictions;
import com.wikicook.common.diet.DietRuleSql;
import com.wikicook.common.diet.DietRuleSql.SqlClause;
import com.wikicook.mealplan.port.DietaryProfilePort;
import com.wikicook.mealplan.port.PlannerProfile;
import com.wikicook.profile.support.ProfileFixtures;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.HeaderCurrentUser;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T103 🕓 #18, US5-1, SC-002 and FR-021: a profile saved through the 011 API, read through {@code DietaryProfilePort},
 * and handed to {@code DietRuleSql.notViolating} through {@code DietRestrictions.hardRules}, removes exactly the recipes
 * that contain an allergen of the person or an ingredient they excluded. The data is built by {@code RecipeFixtures}
 * with real {@code ingredient_allergens} links, not scraped data: SC-002 holds on this seed only until the mapping of
 * the scraped ingredients is approved (research.md row 18, T130b).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({HeaderCurrentUser.class, AuthTestSupport.class})
class DietRuleContractIT extends AbstractPostgresIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    NamedParameterJdbcTemplate named;

    @Autowired
    AuthTestSupport support;

    @Autowired
    DietaryProfilePort port;

    ProfileFixtures fixtures;
    RecipeFixtures recipes;
    long user;

    @BeforeEach
    void setUp() {
        RecipeFixtures.clear(jdbc);
        fixtures = new ProfileFixtures(jdbc);
        recipes = new RecipeFixtures(jdbc);
        user = support.member();
    }

    private void save(Set<Long> allergens, Set<Long> excluded, String diet) throws Exception {
        mvc.perform(put("/api/v1/me/dietary-profile").header(HeaderCurrentUser.HEADER, user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(ProfileFixtures.body(allergens, excluded, diet, Set.of(), null, null, 0))).andExpect(status().isOk());
    }

    /** The recipes the rule lets through for what the port reports. */
    private Set<Long> survivors() {
        PlannerProfile profile = port.findByUserId(user);
        SqlClause clause = DietRuleSql.notViolating("r.id",
                DietRestrictions.hardRules(profile.allergenIds(), profile.excludedIngredientIds()));
        List<Long> ids = named.query("SELECT r.id FROM recipes r WHERE r.status = 'PUBLISHED' AND " + clause.sql(),
                new MapSqlParameterSource(clause.params()), (rs, n) -> rs.getLong("id"));
        return new HashSet<>(ids);
    }

    @Test
    @DisplayName("US5-1, SC-002: allergen and excluded ingredient remove exactly the recipes that contain them")
    void removesExactlyTheRecipesThatBreakTheRules() throws Exception {
        long peanut = fixtures.allergen("PEANUT");
        long milk = fixtures.allergen("MILK");
        long peanutButter = fixtures.ingredient("Bơ đậu phộng DR", "PLANT");
        long butter = fixtures.ingredient("Bơ sữa DR", "DAIRY");
        long ngoRi = fixtures.ingredient("Ngò rí DR");
        long rice = fixtures.ingredient("Gạo DR");
        fixtures.tagIngredientWithAllergen(peanutButter, peanut);
        fixtures.tagIngredientWithAllergen(butter, milk);
        long withPeanut = recipes.newRecipe("Bánh mì bơ đậu phộng DR").ingredients(peanutButter, rice).create();
        long withExcluded = recipes.newRecipe("Canh ngò rí DR").ingredients(ngoRi, rice).create();
        long withBoth = recipes.newRecipe("Cháo đậu phộng ngò rí DR").ingredients(peanutButter, ngoRi).create();
        long clean = recipes.newRecipe("Cơm trắng DR").ingredients(rice).create();
        long withMilk = recipes.newRecipe("Bánh bơ sữa DR").ingredients(butter, rice).create(); // milk is not this person's allergen
        save(Set.of(peanut), Set.of(ngoRi), "NONE");

        assertThat(survivors()).containsExactlyInAnyOrder(clean, withMilk)
                .doesNotContain(withPeanut, withExcluded, withBoth);
    }

    @Test
    @DisplayName("FR-021: an empty profile removes nothing")
    void emptyProfileRemovesNothing() {
        long rice = fixtures.ingredient("Gạo DR");
        long first = recipes.newRecipe("Cơm trắng DR").ingredients(rice).create();
        long second = recipes.newRecipe("Cơm chiên DR").ingredients(rice).create();

        assertThat(survivors()).containsExactlyInAnyOrder(first, second);
    }

    @Test
    @DisplayName("FR-022: a Vegetarian profile alone removes no recipe through the hard rules")
    void dietTypeAloneRemovesNothing() throws Exception {
        long beef = fixtures.ingredient("Thịt bò DR", "MEAT");
        long meat = recipes.newRecipe("Bò hầm DR").ingredients(beef).create();
        save(Set.of(), Set.of(), "VEGETARIAN");

        assertThat(port.findByUserId(user).dietType().name()).isEqualTo("VEGETARIAN");
        assertThat(survivors()).contains(meat);
    }

    @Test
    @DisplayName("US5-1: the rule follows a later edit of the profile")
    void followsTheNextSave() throws Exception {
        long peanut = fixtures.allergen("PEANUT");
        long peanutButter = fixtures.ingredient("Bơ đậu phộng DR", "PLANT");
        fixtures.tagIngredientWithAllergen(peanutButter, peanut);
        long dish = recipes.newRecipe("Bánh mì bơ đậu phộng DR").ingredients(peanutButter).create();
        save(Set.of(peanut), Set.of(), "NONE");
        assertThat(survivors()).doesNotContain(dish);

        mvc.perform(put("/api/v1/me/dietary-profile").header(HeaderCurrentUser.HEADER, user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(ProfileFixtures.body(Set.of(), Set.of(), "NONE", Set.of(), null, null, 1))).andExpect(status().isOk());

        assertThat(survivors()).contains(dish);
    }
}
