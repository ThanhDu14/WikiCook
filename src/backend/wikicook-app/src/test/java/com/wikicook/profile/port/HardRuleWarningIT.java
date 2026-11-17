package com.wikicook.profile.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.profile.support.ProfileFixtures;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.HeaderCurrentUser;
import java.util.ArrayList;
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
import org.springframework.test.web.servlet.MockMvc;

/**
 * T104 (US5-2): with a profile saved through the 011 API, the picker of 030 flags the recipes that break the hard rules
 * and names what breaks them: {@code { type: ALLERGEN, label }}, {@code { type: EXCLUDED_INGREDIENT, label }}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({HeaderCurrentUser.class, AuthTestSupport.class})
class HardRuleWarningIT extends AbstractPostgresIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AuthTestSupport support;

    @Autowired
    ObjectMapper json;

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

    private void saveProfile(Set<Long> allergens, Set<Long> excluded) throws Exception {
        mvc.perform(put("/api/v1/me/dietary-profile").header(HeaderCurrentUser.HEADER, user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(ProfileFixtures.body(allergens, excluded, "NONE", Set.of(), null, null, 0))).andExpect(status().isOk());
    }

    private JsonNode picker() throws Exception {
        String body = mvc.perform(get("/api/v1/meal-plans/picker/recipes").param("meal", "LUNCH").param("size", "50")
                        .header(HeaderCurrentUser.HEADER, user)).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        return json.readTree(body).get("content");
    }

    private static JsonNode byTitle(JsonNode content, String title) {
        for (JsonNode result : content) {
            if (title.equals(result.get("title").asText())) {
                return result;
            }
        }
        throw new AssertionError("no recipe '" + title + "' in the picker");
    }

    private static List<String> conflicts(JsonNode result) {
        List<String> list = new ArrayList<>();
        result.get("conflicts").forEach(c -> list.add(c.get("type").asText() + ":" + c.get("label").asText()));
        return list;
    }

    @Test
    @DisplayName("US5-2: the picker names the allergen and the excluded ingredient from the real profile")
    void conflictsNameTheAllergenAndTheExcludedIngredient() throws Exception {
        long peanut = fixtures.allergen("PEANUT");
        long peanutButter = fixtures.ingredient("Bơ đậu phộng HR", "PLANT");
        fixtures.tagIngredientWithAllergen(peanutButter, peanut);
        long ngoRi = fixtures.ingredient("Ngò rí HR");
        long rice = fixtures.ingredient("Gạo HR");
        recipes.newRecipe("Bánh mì bơ đậu phộng HR").ingredients(peanutButter, rice).create();
        recipes.newRecipe("Canh ngò rí HR").ingredients(ngoRi, rice).create();
        recipes.newRecipe("Cơm trắng HR").ingredients(rice).create();
        saveProfile(Set.of(peanut), Set.of(ngoRi));

        JsonNode content = picker();

        assertThat(conflicts(byTitle(content, "Bánh mì bơ đậu phộng HR"))).containsExactly("ALLERGEN:Đậu phộng");
        assertThat(conflicts(byTitle(content, "Canh ngò rí HR"))).containsExactly("EXCLUDED_INGREDIENT:Ngò rí HR");
        assertThat(conflicts(byTitle(content, "Cơm trắng HR"))).isEmpty();
    }

    @Test
    @DisplayName("US5-2: recipes that break a hard rule are listed after the ones that do not")
    void conflictingRecipesComeLast() throws Exception {
        long milk = fixtures.allergen("MILK");
        long butter = fixtures.ingredient("Bơ sữa HR", "DAIRY");
        fixtures.tagIngredientWithAllergen(butter, milk);
        long rice = fixtures.ingredient("Gạo HR");
        recipes.newRecipe("A - Bơ sữa HR").ingredients(butter).create(); // would sort first by title
        recipes.newRecipe("B - Cơm HR").ingredients(rice).create();
        saveProfile(Set.of(milk), Set.of());

        JsonNode content = picker();

        assertThat(content.get(0).get("title").asText()).isEqualTo("B - Cơm HR");
        assertThat(content.get(1).get("title").asText()).isEqualTo("A - Bơ sữa HR");
    }

    @Test
    @DisplayName("US5-2: without a saved profile nothing is flagged")
    void emptyProfileFlagsNothing() throws Exception {
        long peanut = fixtures.allergen("PEANUT");
        long peanutButter = fixtures.ingredient("Bơ đậu phộng HR", "PLANT");
        fixtures.tagIngredientWithAllergen(peanutButter, peanut);
        recipes.newRecipe("Bánh mì bơ đậu phộng HR").ingredients(peanutButter).create();

        JsonNode content = picker();

        assertThat(conflicts(byTitle(content, "Bánh mì bơ đậu phộng HR"))).isEmpty();
    }
}
