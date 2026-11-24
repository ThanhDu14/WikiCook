package com.wikicook.recipe.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;

import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.AuthTestSupport.SignedIn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** 060 T035: GET /api/v1/recipes/{id}. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthTestSupport.class)
class RecipeDetailControllerIT extends AbstractPostgresIT {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired AuthTestSupport auth;

    private RecipeFixtures recipes;

    @BeforeEach
    void setUp() {
        auth.resetThrottle();
        recipes = new RecipeFixtures(jdbc);
    }

    private long ingredient(String name) {
        return recipes.ingredient(name + " " + System.nanoTime(), "PLANT");
    }

    @Test
    @DisplayName("publishedForGuest: no token → 200, not the owner, imported source and cover shown")
    void publishedForGuest() throws Exception {
        long id = recipes.newRecipe("Gà kho gừng").ingredients(ingredient("Thịt gà")).create();
        jdbc.update("UPDATE recipes SET source_name = 'angingon.com', source_url = 'https://angingon.com/ga-kho', "
                + "description = 'Món kho đậm đà' WHERE id = ?", id);
        jdbc.update("INSERT INTO recipe_media (recipe_id, role, external_url) VALUES (?, 'COVER', 'https://img/ga.jpg')", id);

        mvc.perform(get("/api/v1/recipes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Gà kho gừng"))
                .andExpect(jsonPath("$.description").value("Món kho đậm đà"))
                .andExpect(jsonPath("$.coverImageUrl").value("https://img/ga.jpg"))
                .andExpect(jsonPath("$.isOwner").value(false))
                .andExpect(jsonPath("$.source.name").value("angingon.com"))
                .andExpect(jsonPath("$.source.url").value("https://angingon.com/ga-kho"))
                .andExpect(jsonPath("$.servings").value(2))
                .andExpect(jsonPath("$.difficulty").value("EASY"));
    }

    @Test
    @DisplayName("draftHiddenFromOthers: a draft answers 404, same body as a missing id")
    void draftHiddenFromOthers() throws Exception {
        long author = auth.member();
        long id = recipes.newRecipe("Nháp").status("DRAFT").create();
        jdbc.update("UPDATE recipes SET author_id = ? WHERE id = ?", author, id);
        SignedIn other = auth.signedInMember();

        mvc.perform(get("/api/v1/recipes/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(get("/api/v1/recipes/{id}", id).header(HttpHeaders.AUTHORIZATION, other.bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(get("/api/v1/recipes/{id}", 999_999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("draftVisibleToAuthor: the author reads their draft and is the owner")
    void draftVisibleToAuthor() throws Exception {
        SignedIn author = auth.signedInMember();
        long id = recipes.newRecipe("Nháp của tôi").status("DRAFT").create();
        jdbc.update("UPDATE recipes SET author_id = ? WHERE id = ?", author.userId(), id);

        mvc.perform(get("/api/v1/recipes/{id}", id).header(HttpHeaders.AUTHORIZATION, author.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.isOwner").value(true))
                .andExpect(jsonPath("$.author.id").value(author.userId()));
    }

    @Test
    @DisplayName("groupsIngredients: ungrouped first as name = null, then groups in first-appearance order; steps with images")
    void groupsIngredients() throws Exception {
        long id = recipes.newRecipe("Bún bò").create();
        long unit = jdbc.queryForObject("SELECT id FROM units ORDER BY id LIMIT 1", Long.class);
        long broth = ingredient("Xương");
        long noodle = ingredient("Bún");
        long salt = ingredient("Muối");
        long herb = ingredient("Rau");
        jdbc.update("""
                INSERT INTO recipe_ingredients (recipe_id, ingredient_id, unit_id, quantity, group_name, position) VALUES
                  (?, ?, ?, 500, 'Nước dùng', 1), (?, ?, ?, 1, NULL, 2), (?, ?, ?, NULL, 'Ăn kèm', 3), (?, ?, ?, 2, 'Nước dùng', 4)
                """, id, broth, unit, id, salt, unit, id, herb, unit, id, noodle, unit);
        long step = jdbc.queryForObject("""
                INSERT INTO recipe_steps (recipe_id, position, content, timer_seconds) VALUES (?, 1, 'Ninh xương', 3600)
                RETURNING id
                """, Long.class, id);
        jdbc.update("INSERT INTO recipe_media (recipe_id, step_id, role, position, external_url) VALUES (?, ?, 'STEP', 1, 'https://img/b1.jpg')",
                id, step);

        mvc.perform(get("/api/v1/recipes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingredientGroups[0].name").value(nullValue()))
                .andExpect(jsonPath("$.ingredientGroups[0].items[0].ingredientId").value(salt))
                .andExpect(jsonPath("$.ingredientGroups[1].name").value("Nước dùng"))
                .andExpect(jsonPath("$.ingredientGroups[1].items[*].ingredientId").value(contains((int) broth, (int) noodle)))
                .andExpect(jsonPath("$.ingredientGroups[2].name").value("Ăn kèm"))
                .andExpect(jsonPath("$.ingredientGroups[2].items[0].quantity").value(nullValue()))
                .andExpect(jsonPath("$.steps[0].content").value("Ninh xương"))
                .andExpect(jsonPath("$.steps[0].timerSeconds").value(3600))
                .andExpect(jsonPath("$.steps[0].imageUrls[0]").value("https://img/b1.jpg"));
    }

    @Test
    @DisplayName("ratingNullWithoutReviews: ratingAvg is null while reviewCount is 0")
    void ratingNullWithoutReviews() throws Exception {
        long id = recipes.newRecipe("Chưa ai chấm").rating(4.5).create();

        mvc.perform(get("/api/v1/recipes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewCount").value(0))
                .andExpect(jsonPath("$.ratingAvg").value(nullValue()));
    }
}
