package com.wikicook.aiplanner.support;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Inserts just enough lookup, ingredient and recipe rows for AI planner tests. */
public final class RecipeFixtures {

    private final JdbcTemplate jdbc;
    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final long cuisineId;
    private final long categoryId;
    private final long unitId;

    public RecipeFixtures(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        cuisineId = cuisine("Việt Nam");
        categoryId = category("Món chính");
        unitId = jdbc.queryForObject("""
                INSERT INTO units (code, name_vi, unit_type) VALUES (?, 'gam', 'MASS') RETURNING id
                """, Long.class, "g-" + suffix);
    }

    /** Removes recipes (and with them ingredient rows, embeddings and nutrition) left by earlier tests. */
    public static void clear(JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM recipe_embeddings");
        jdbc.update("DELETE FROM recipes");
    }

    public long cuisine(String name) {
        return jdbc.queryForObject("INSERT INTO cuisines (name, slug) VALUES (?, ?) RETURNING id", Long.class, name,
                "c-" + UUID.randomUUID());
    }

    public long category(String name) {
        return jdbc.queryForObject("INSERT INTO categories (name, slug) VALUES (?, ?) RETURNING id", Long.class, name,
                "k-" + UUID.randomUUID());
    }

    public long allergen(String code) {
        return jdbc.queryForObject("INSERT INTO allergens (code, name_vi) VALUES (?, ?) RETURNING id", Long.class,
                code + "-" + suffix, code);
    }

    /** An approved ingredient, created on first use; {@code kind} as in {@code ingredients.kind}. */
    public long ingredient(String name, String kind) {
        List<Long> existing = jdbc.queryForList(
                "SELECT id FROM ingredients WHERE name_normalized = f_unaccent(lower(trim(?))) AND status = 'APPROVED'",
                Long.class, name);
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }
        return jdbc.queryForObject("""
                INSERT INTO ingredients (name, name_normalized, kind) VALUES (?, f_unaccent(lower(trim(?))), ?)
                RETURNING id
                """, Long.class, name, name, kind);
    }

    public void tagAllergen(long ingredientId, long allergenId) {
        jdbc.update("INSERT INTO ingredient_allergens (ingredient_id, allergen_id) VALUES (?, ?)", ingredientId,
                allergenId);
    }

    /** Display-only labels of spec 090; the planner must never trust them (FR-009a). */
    public void dietLabels(long recipeId, String... labels) {
        jdbc.update("""
                INSERT INTO recipe_nutrition (recipe_id, status, diet_labels, input_fingerprint)
                VALUES (?, 'CALCULATING', ?::varchar[], ?)
                """, recipeId, "{" + String.join(",", labels) + "}", "f".repeat(64));
    }

    public Builder newRecipe(String title) {
        return new Builder(title);
    }

    public long publishedRecipe(String title, String... ingredients) {
        return recipe(title, "PUBLISHED", ingredients);
    }

    /** Lunch and dinner, 10 + 30 minutes, ingredients of kind OTHER in the given order. */
    public long recipe(String title, String status, String... ingredients) {
        Builder builder = newRecipe(title).status(status);
        for (String name : ingredients) {
            builder.ingredients(ingredient(name, "OTHER"));
        }
        return builder.create();
    }

    public void archive(long recipeId) {
        jdbc.update("UPDATE recipes SET status = 'ARCHIVED', archived_at = now() WHERE id = ?", recipeId);
    }

    public void renameFirstIngredient(long recipeId, String name) {
        jdbc.update("UPDATE recipe_ingredients SET ingredient_id = ? WHERE recipe_id = ? AND position = 1",
                ingredient(name, "OTHER"), recipeId);
    }

    /** Defaults: PUBLISHED, lunch and dinner, 10 + 30 minutes, fixture cuisine and category, rating 0. */
    public final class Builder {

        private final String title;
        private String status = "PUBLISHED";
        private boolean breakfast;
        private boolean lunch = true;
        private boolean dinner = true;
        private boolean snack;
        private int prepMinutes = 10;
        private int cookMinutes = 30;
        private long cuisine = cuisineId;
        private long category = categoryId;
        private double rating;
        private final List<Long> ingredientIds = new ArrayList<>();

        private Builder(String title) {
            this.title = title;
        }

        public Builder status(String value) {
            status = value;
            return this;
        }

        public Builder meals(boolean forBreakfast, boolean forLunch, boolean forDinner) {
            breakfast = forBreakfast;
            lunch = forLunch;
            dinner = forDinner;
            return this;
        }

        /** Snacks belong to no meal (schema CHECK). */
        public Builder snack() {
            snack = true;
            return meals(false, false, false);
        }

        public Builder minutes(int prep, int cook) {
            prepMinutes = prep;
            cookMinutes = cook;
            return this;
        }

        public Builder cuisine(long id) {
            cuisine = id;
            return this;
        }

        public Builder category(long id) {
            category = id;
            return this;
        }

        public Builder rating(double value) {
            rating = value;
            return this;
        }

        public Builder ingredients(long... ids) {
            for (long id : ids) {
                ingredientIds.add(id);
            }
            return this;
        }

        public long create() {
            long id = jdbc.queryForObject("""
                    INSERT INTO recipes (title, cuisine_id, category_id, difficulty, servings, prep_time_minutes,
                                         cook_time_minutes, status, published_at, for_breakfast, for_lunch, for_dinner,
                                         is_snack, rating_avg)
                    VALUES (?, ?, ?, 'EASY', 2, ?, ?, ?, CASE WHEN ? IN ('PUBLISHED', 'ARCHIVED') THEN now() END,
                            ?, ?, ?, ?, ?)
                    RETURNING id
                    """, Long.class, title, cuisine, category, prepMinutes, cookMinutes, status, status, breakfast,
                    lunch, dinner, snack, rating);
            for (int i = 0; i < ingredientIds.size(); i++) {
                jdbc.update("""
                        INSERT INTO recipe_ingredients (recipe_id, ingredient_id, unit_id, quantity, position)
                        VALUES (?, ?, ?, 100, ?)
                        """, id, ingredientIds.get(i), unitId, i + 1);
            }
            return id;
        }
    }
}
