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
 * T105 (US5-3), the part that belongs to 030: a Vegetarian profile, 30 minutes at most and one preferred cuisine only
 * rank the picker, they never remove a recipe (FR-020, FR-022). The part for 031 (a generation without the
 * confirmation does not drop these dishes, with it does) waits for row 7 of the decision table in research.md, as the
 * task says, and is not written here.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({HeaderCurrentUser.class, AuthTestSupport.class})
class PreferencesNeverRemoveIT extends AbstractPostgresIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AuthTestSupport support;

    @Autowired
    ObjectMapper json;

    @BeforeEach
    void clean() {
        RecipeFixtures.clear(jdbc);
    }

    @Test
    @DisplayName("US5-3: the picker still returns the recipe with meat, the long one and the one of another cuisine")
    void preferencesOnlyRank() throws Exception {
        ProfileFixtures fixtures = new ProfileFixtures(jdbc);
        RecipeFixtures recipes = new RecipeFixtures(jdbc);
        long user = support.member();
        long preferred = fixtures.cuisine("Việt Nam PR");
        long other = fixtures.cuisine("Ý PR");
        long beef = fixtures.ingredient("Thịt bò PR", "MEAT");
        long tofu = fixtures.ingredient("Đậu hũ PR", "PLANT");
        recipes.newRecipe("Đậu hũ sốt cà - hợp hết PR").cuisine(preferred).minutes(5, 20).ingredients(tofu).create();
        recipes.newRecipe("Bò hầm - có thịt PR").cuisine(preferred).minutes(5, 20).ingredients(beef).create();
        recipes.newRecipe("Đậu hũ om - quá giờ PR").cuisine(preferred).minutes(30, 90).ingredients(tofu).create();
        recipes.newRecipe("Mì Ý chay - khác ẩm thực PR").cuisine(other).minutes(5, 20).ingredients(tofu).create();
        mvc.perform(put("/api/v1/me/dietary-profile").header(HeaderCurrentUser.HEADER, user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(ProfileFixtures.body(Set.of(), Set.of(), "VEGETARIAN", Set.of(preferred), null, 30, 0)))
                .andExpect(status().isOk());

        String body = mvc.perform(get("/api/v1/meal-plans/picker/recipes").param("meal", "LUNCH").param("size", "50")
                        .header(HeaderCurrentUser.HEADER, user)).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        List<String> titles = new ArrayList<>();
        for (JsonNode result : json.readTree(body).get("content")) {
            titles.add(result.get("title").asText());
        }

        assertThat(titles).containsExactlyInAnyOrder("Đậu hũ sốt cà - hợp hết PR", "Bò hầm - có thịt PR",
                "Đậu hũ om - quá giờ PR", "Mì Ý chay - khác ẩm thực PR");
        assertThat(titles.getFirst()).as("the recipe that fits every preference leads").isEqualTo("Đậu hũ sốt cà - hợp hết PR");
    }
}
