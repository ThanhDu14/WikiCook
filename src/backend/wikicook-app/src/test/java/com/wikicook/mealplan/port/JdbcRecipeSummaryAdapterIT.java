package com.wikicook.mealplan.port;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.mealplan.domain.DietType;
import com.wikicook.support.AbstractPostgresIT;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** T021: the stand-in for 060's RecipeSummaryPort reads title, cover, time, servings, status and diet type. */
@SpringBootTest
class JdbcRecipeSummaryAdapterIT extends AbstractPostgresIT {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    RecipeSummaryPort recipes;

    private RecipeFixtures f;

    @BeforeEach
    void setUp() {
        f = new RecipeFixtures(jdbc);
    }

    @Test
    void readsEverySummaryFieldInOneCall() {
        long vegan = f.newRecipe("Đậu hũ sốt cà").minutes(10, 20).ingredients(f.ingredient("Đậu hũ", "PLANT"))
                .create();
        jdbc.update("INSERT INTO recipe_media (recipe_id, role, external_url) VALUES (?, 'COVER', ?)", vegan,
                "https://img.example/dau-hu.jpg");
        long fish = f.newRecipe("Cá kho tộ").ingredients(f.ingredient("Cá basa", "SEAFOOD")).create();
        long archived = f.newRecipe("Bò lúc lắc").status("ARCHIVED").ingredients(f.ingredient("Thịt bò", "MEAT"))
                .create();
        long snack = f.newRecipe("Bánh tráng trộn").snack().ingredients(f.ingredient("Đậu hũ", "PLANT")).create();

        Map<Long, RecipeSummary> found = recipes.findByIds(List.of(vegan, fish, archived, snack, -1L));

        assertThat(found).containsOnlyKeys(vegan, fish, archived, snack);
        assertThat(found.get(vegan)).isEqualTo(new RecipeSummary(vegan, "Đậu hũ sốt cà",
                "https://img.example/dau-hu.jpg", 30, 2, RecipeStatus.PUBLISHED, false, DietType.VEGAN));
        assertThat(found.get(fish).dietType()).isEqualTo(DietType.PESCATARIAN);
        assertThat(found.get(fish).coverImageUrl()).isNull();
        assertThat(found.get(archived).status()).isEqualTo(RecipeStatus.ARCHIVED);
        assertThat(found.get(archived).dietType()).isEqualTo(DietType.NONE);
        assertThat(found.get(snack).snack()).isTrue();
        assertThat(found.get(snack).available()).isFalse();
        assertThat(found.get(vegan).available()).isTrue();
        assertThat(found.get(archived).available()).isFalse();
    }

    @Test
    void noIdsNoQuery() {
        assertThat(recipes.findByIds(List.of())).isEmpty();
    }
}
