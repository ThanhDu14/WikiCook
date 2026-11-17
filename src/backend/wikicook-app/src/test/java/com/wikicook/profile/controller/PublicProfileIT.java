package com.wikicook.profile.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.HeaderCurrentUser;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T056 (US2-6, FR-005): the public page of a person, for guests too. The JSON has the five public fields and nothing
 * else, the recipes are the PUBLISHED ones of the author, a page of 12.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({HeaderCurrentUser.class, AuthTestSupport.class})
class PublicProfileIT extends AbstractPostgresIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AuthTestSupport support;

    @Autowired
    ObjectMapper json;

    RecipeFixtures recipes;
    long author;

    @BeforeEach
    void setUp() {
        RecipeFixtures.clear(jdbc);
        recipes = new RecipeFixtures(jdbc);
        author = support.member();
        jdbc.update("UPDATE users SET display_name = 'Bếp Lan', bio = 'Món Huế mỗi ngày', avatar_storage_key = 'avatars/abc.jpg' WHERE id = ?",
                author);
    }

    private long recipe(String title, String status, int daysAgo) {
        long id = recipes.newRecipe(title).status(status).create();
        jdbc.update("UPDATE recipes SET author_id = ?, published_at = CASE WHEN status IN ('PUBLISHED','ARCHIVED') THEN now() - make_interval(days => ?) END WHERE id = ?",
                author, daysAgo, id);
        return id;
    }

    private static List<String> fieldsOf(JsonNode node) {
        List<String> fields = new ArrayList<>();
        node.fieldNames().forEachRemaining(fields::add);
        java.util.Collections.sort(fields);
        return fields;
    }

    @Test
    @DisplayName("US2-6: a guest sees the name, avatar, bio and the number of published recipes - and no other field")
    void publicFieldsOnly() throws Exception {
        recipe("Bún bò Huế", "PUBLISHED", 1);
        recipe("Bánh bèo", "PUBLISHED", 2);
        recipe("Nháp chưa xong", "DRAFT", 0);
        recipe("Chờ duyệt", "PENDING", 0);
        recipe("Đã lưu trữ", "ARCHIVED", 5);

        String body = mvc.perform(get("/api/v1/profiles/" + author)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(author))
                .andExpect(jsonPath("$.displayName").value("Bếp Lan"))
                .andExpect(jsonPath("$.bio").value("Món Huế mỗi ngày"))
                .andExpect(jsonPath("$.avatarUrl").value("/api/v1/media/avatars/abc.jpg"))
                .andExpect(jsonPath("$.publishedRecipeCount").value(2))
                .andReturn().getResponse().getContentAsString();

        List<String> fields = fieldsOf(json.readTree(body));
        assertThat(fields).containsExactly("avatarUrl", "bio", "displayName", "id", "publishedRecipeCount");
        String email = jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, author);
        assertThat(body).doesNotContain(email).doesNotContainIgnoringCase("email").doesNotContain("householdSize")
                .doesNotContain("dietType").doesNotContain("allergen");
    }

    @Test
    @DisplayName("US2-6: a person without a picture or a bio has null for both")
    void emptyPublicFields() throws Exception {
        long plain = support.member();

        mvc.perform(get("/api/v1/profiles/" + plain)).andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(nullValue()))
                .andExpect(jsonPath("$.bio").value(nullValue()))
                .andExpect(jsonPath("$.publishedRecipeCount").value(0));
    }

    @Test
    @DisplayName("FR-005: there is no join date on the public page")
    void noJoinDate() throws Exception {
        String body = mvc.perform(get("/api/v1/profiles/" + author)).andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContainIgnoringCase("createdAt").doesNotContainIgnoringCase("joined");
    }

    @Test
    @DisplayName("US2-6: the recipes are paged by 12, newest published first, and only PUBLISHED ones")
    void recipesArePaged() throws Exception {
        for (int i = 0; i < 30; i++) {
            recipe("Món số " + String.format("%02d", i), "PUBLISHED", 100 - i); // 29 is the newest
        }
        recipe("Bản nháp", "DRAFT", 0);
        recipe("Chờ duyệt", "PENDING", 0);
        recipe("Đã lưu trữ", "ARCHIVED", 0);

        mvc.perform(get("/api/v1/profiles/" + author + "/recipes")).andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(12))
                .andExpect(jsonPath("$.totalItems").value(30))
                .andExpect(jsonPath("$.items", hasSize(12)))
                .andExpect(jsonPath("$.items[0].title").value("Món số 29"))
                .andExpect(jsonPath("$.items[11].title").value("Món số 18"));
        mvc.perform(get("/api/v1/profiles/" + author + "/recipes").param("page", "2")).andExpect(jsonPath("$.items", hasSize(6)))
                .andExpect(jsonPath("$.items[5].title").value("Món số 00"));
        mvc.perform(get("/api/v1/profiles/" + author + "/recipes").param("page", "3")).andExpect(jsonPath("$.items", hasSize(0)));
        mvc.perform(get("/api/v1/profiles/" + author + "/recipes").param("size", "24")).andExpect(jsonPath("$.items", hasSize(24)));
    }

    @Test
    @DisplayName("US2-6: a recipe card has an id, a title, a cover, the time and the rating, nothing else")
    void recipeCardFields() throws Exception {
        long id = recipe("Bún bò Huế", "PUBLISHED", 1);
        jdbc.update("UPDATE recipes SET rating_avg = 4.5, review_count = 8 WHERE id = ?", id);
        jdbc.update("INSERT INTO recipe_media (recipe_id, role, external_url) VALUES (?, 'COVER', 'https://img.test/bun-bo.jpg')", id);

        String body = mvc.perform(get("/api/v1/profiles/" + author + "/recipes")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(id))
                .andExpect(jsonPath("$.items[0].title").value("Bún bò Huế"))
                .andExpect(jsonPath("$.items[0].coverImageUrl").value("https://img.test/bun-bo.jpg"))
                .andExpect(jsonPath("$.items[0].totalTimeMinutes").value(40))
                .andExpect(jsonPath("$.items[0].ratingAvg").value(4.5))
                .andExpect(jsonPath("$.items[0].reviewCount").value(8))
                .andReturn().getResponse().getContentAsString();

        JsonNode card = json.readTree(body).get("items").get(0);
        assertThat(fieldsOf(card))
                .containsExactly("coverImageUrl", "id", "ratingAvg", "reviewCount", "title", "totalTimeMinutes");
    }

    @Test
    @DisplayName("Edge: a person with no published recipe has an empty page, not an error")
    void noRecipes() throws Exception {
        mvc.perform(get("/api/v1/profiles/" + author + "/recipes")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    @DisplayName("OQ-011-02: an id that does not exist, or is not a number, is 404 USER_NOT_FOUND")
    void unknownPersonIs404() throws Exception {
        mvc.perform(get("/api/v1/profiles/987654321")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        mvc.perform(get("/api/v1/profiles/987654321/recipes")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        mvc.perform(get("/api/v1/profiles/abc")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("openapi: page below 0 and size outside 1-24 are 400")
    void pagingIsValidated() throws Exception {
        for (String[] bad : new String[][] {{"page", "-1"}, {"size", "0"}, {"size", "25"}, {"page", "x"}}) {
            mvc.perform(get("/api/v1/profiles/" + author + "/recipes").param(bad[0], bad[1])).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    @DisplayName("FR-005: signed-in people see the same page as guests")
    void sameForSignedInPeople() throws Exception {
        long other = support.member();
        String guest = mvc.perform(get("/api/v1/profiles/" + author)).andReturn().getResponse().getContentAsString();
        String member = mvc.perform(get("/api/v1/profiles/" + author).header(HeaderCurrentUser.HEADER, other)).andReturn()
                .getResponse().getContentAsString();

        assertThat(json.readTree(member)).isEqualTo(json.readTree(guest));
        mvc.perform(get("/api/v1/profiles/" + author + "/recipes").header(HeaderCurrentUser.HEADER, other))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }
}
