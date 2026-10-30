package com.wikicook.aiplanner.support;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Inserts just enough lookup, ingredient and recipe rows for embedding tests. */
public final class RecipeFixtures {

    private final JdbcTemplate jdbc;
    private final long cuisineId;
    private final long categoryId;
    private final long unitId;

    public RecipeFixtures(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        cuisineId = jdbc.queryForObject("INSERT INTO cuisines (name, slug) VALUES ('Việt Nam', ?) RETURNING id",
                Long.class, "vn-" + suffix);
        categoryId = jdbc.queryForObject("INSERT INTO categories (name, slug) VALUES ('Món chính', ?) RETURNING id",
                Long.class, "main-" + suffix);
        unitId = jdbc.queryForObject("""
                INSERT INTO units (code, name_vi, unit_type) VALUES (?, 'gam', 'MASS') RETURNING id
                """, Long.class, "g-" + suffix);
    }

    /** Removes recipes (and with them ingredients rows and embeddings) left by earlier tests. */
    public static void clear(JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM recipe_embeddings");
        jdbc.update("DELETE FROM recipes");
    }

    public long publishedRecipe(String title, String... ingredients) {
        return recipe(title, "PUBLISHED", ingredients);
    }

    /** Lunch and dinner, 10 + 30 minutes, ingredients in the given order. */
    public long recipe(String title, String status, String... ingredients) {
        long id = jdbc.queryForObject("""
                INSERT INTO recipes (title, cuisine_id, category_id, difficulty, servings, prep_time_minutes,
                                     cook_time_minutes, status, published_at)
                VALUES (?, ?, ?, 'EASY', 2, 10, 30, ?, CASE WHEN ? IN ('PUBLISHED', 'ARCHIVED') THEN now() END)
                RETURNING id
                """, Long.class, title, cuisineId, categoryId, status, status);
        List<String> names = List.of(ingredients);
        for (int i = 0; i < names.size(); i++) {
            jdbc.update("""
                    INSERT INTO recipe_ingredients (recipe_id, ingredient_id, unit_id, quantity, position)
                    VALUES (?, ?, ?, 100, ?)
                    """, id, ingredient(names.get(i)), unitId, i + 1);
        }
        return id;
    }

    public void archive(long recipeId) {
        jdbc.update("UPDATE recipes SET status = 'ARCHIVED', archived_at = now() WHERE id = ?", recipeId);
    }

    public void renameFirstIngredient(long recipeId, String name) {
        jdbc.update("UPDATE recipe_ingredients SET ingredient_id = ? WHERE recipe_id = ? AND position = 1",
                ingredient(name), recipeId);
    }

    private long ingredient(String name) {
        List<Long> existing = jdbc.queryForList(
                "SELECT id FROM ingredients WHERE name_normalized = f_unaccent(lower(trim(?))) AND status = 'APPROVED'",
                Long.class, name);
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }
        return jdbc.queryForObject("""
                INSERT INTO ingredients (name, name_normalized, kind) VALUES (?, f_unaccent(lower(trim(?))), 'OTHER')
                RETURNING id
                """, Long.class, name, name);
    }
}
