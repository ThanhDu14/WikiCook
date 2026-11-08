package com.wikicook.search.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.search.support.SearchFixtures;
import com.wikicook.support.AbstractPostgresIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** V204: {@code f_recipe_search_vector} builds the weighted, accent-free document (020 research R1, R2). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RecipeSearchVectorMigrationIT extends AbstractPostgresIT {

    @Autowired
    JdbcTemplate jdbc;

    SearchFixtures fx;

    @BeforeEach
    void setUp() {
        SearchFixtures.clear(jdbc);
        fx = new SearchFixtures(jdbc);
    }

    private boolean matches(long recipeId, String weights, String term) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT ts_filter(search_vector, ?::"char"[]) @@ to_tsquery('simple', ?)
                FROM recipes WHERE id = ?
                """, Boolean.class, "{" + weights + "}", term, recipeId));
    }

    @Test
    void titleIsWeightAAccentFreeAndLowerCase() {
        long id = fx.recipe("Phở Bò Hà Nội").create();

        assertThat(matches(id, "a", "pho")).isTrue();
        assertThat(matches(id, "a", "bo")).isTrue();
        assertThat(matches(id, "b,c", "pho")).isFalse();
    }

    @Test
    void ingredientsAndTagOrMethodNamesAreWeightBButNotEquipment() {
        long peanut = fx.ingredient("Đậu phộng", "PLANT");
        long tag = fx.tag("TAG", "Món nhanh", "mon-nhanh");
        long method = fx.tag("METHOD", "Luộc", "luoc");
        long equipment = fx.tag("EQUIPMENT", "Nồi áp suất", "noi-ap-suat");
        long id = fx.recipe("Gỏi gà").ingredients(peanut).tags(tag, method, equipment).create();

        assertThat(matches(id, "b", "dau & phong")).isTrue();
        assertThat(matches(id, "b", "nhanh")).isTrue();
        assertThat(matches(id, "b", "luoc")).isTrue();
        assertThat(matches(id, "a,b,c", "suat")).isFalse();
    }

    @Test
    void descriptionIsWeightC() {
        long id = fx.recipe("Canh chua").description("Món miền Tây với cá lóc").create();

        assertThat(matches(id, "c", "loc")).isTrue();
        assertThat(matches(id, "a,b", "loc")).isFalse();
    }

    @Test
    void dStrokeIsFoldedToD() {
        long id = fx.recipe("Đậu hũ sốt cà").create();

        assertThat(matches(id, "a", "dau")).isTrue();
    }

    @Test
    void recipeWithoutDescriptionOrChildrenStillGetsAVector() {
        long id = fx.recipe("Trứng chiên").create();

        assertThat(jdbc.queryForObject("SELECT search_vector IS NOT NULL FROM recipes WHERE id = ?", Boolean.class, id))
                .isTrue();
    }
}
