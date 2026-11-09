package com.wikicook.search.support;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Lookup, ingredient and recipe rows for search tests (spec 020); every slug is unique per instance. */
public final class SearchFixtures {

    private final JdbcTemplate jdbc;
    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final long unitId;
    public final long vietnam;
    public final long japan;
    public final long mainDish;
    public final long soup;

    public SearchFixtures(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        unitId = jdbc.queryForObject("INSERT INTO units (code, name_vi, unit_type) VALUES (?, 'gam', 'MASS') RETURNING id",
                Long.class, "g-" + suffix);
        vietnam = cuisine("Việt Nam", "viet-nam");
        japan = cuisine("Nhật Bản", "nhat-ban");
        mainDish = category("Món chính", "mon-chinh");
        soup = category("Canh / Súp", "canh-sup");
    }

    /** Removes recipes (cascading to their rows) left by earlier tests in the shared database. */
    public static void clear(JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM recipe_embeddings");
        jdbc.update("DELETE FROM recipes");
    }

    /** The slug given to a lookup row by this fixture instance. */
    public String slug(String base) {
        return base + "-" + suffix;
    }

    public long cuisine(String name, String slugBase) {
        return jdbc.queryForObject("INSERT INTO cuisines (name, slug) VALUES (?, ?) RETURNING id", Long.class, name,
                slug(slugBase));
    }

    public long category(String name, String slugBase) {
        return jdbc.queryForObject("INSERT INTO categories (name, slug) VALUES (?, ?) RETURNING id", Long.class, name,
                slug(slugBase));
    }

    /** {@code kind} = TAG, METHOD or EQUIPMENT. */
    public long tag(String kind, String name, String slugBase) {
        return jdbc.queryForObject("INSERT INTO tags (kind, name, slug) VALUES (?, ?, ?) RETURNING id", Long.class,
                kind, name, slug(slugBase));
    }

    public long allergen(String code, String nameVi) {
        return jdbc.queryForObject("INSERT INTO allergens (code, name_vi) VALUES (?, ?) RETURNING id", Long.class,
                code + "-" + suffix, nameVi);
    }

    /** An approved ingredient, reused when the normalized name already exists. */
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

    public void linkAllergen(long ingredientId, long allergenId) {
        jdbc.update("INSERT INTO ingredient_allergens (ingredient_id, allergen_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                ingredientId, allergenId);
    }

    public Builder recipe(String title) {
        return new Builder(title);
    }

    /** Defaults: PUBLISHED now, Việt Nam, Món chính, EASY, 10 + 20 minutes, no reviews, no cover. */
    public final class Builder {

        private final String title;
        private String description;
        private String status = "PUBLISHED";
        private OffsetDateTime publishedAt = OffsetDateTime.now();
        private long cuisine = vietnam;
        private long category = mainDish;
        private String difficulty = "EASY";
        private int prep = 10;
        private int cook = 20;
        private double ratingAvg;
        private int reviewCount;
        private String coverUrl;
        private String coverKey;
        private final List<Long> ingredientIds = new ArrayList<>();
        private final List<Long> tagIds = new ArrayList<>();

        private Builder(String title) {
            this.title = title;
        }

        public Builder description(String value) {
            description = value;
            return this;
        }

        public Builder status(String value) {
            status = value;
            return this;
        }

        public Builder publishedAt(OffsetDateTime value) {
            publishedAt = value;
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

        public Builder difficulty(String value) {
            difficulty = value;
            return this;
        }

        public Builder minutes(int prepMinutes, int cookMinutes) {
            prep = prepMinutes;
            cook = cookMinutes;
            return this;
        }

        public Builder rating(double avg, int count) {
            ratingAvg = avg;
            reviewCount = count;
            return this;
        }

        public Builder coverUrl(String url) {
            coverUrl = url;
            return this;
        }

        public Builder coverKey(String key) {
            coverKey = key;
            return this;
        }

        public Builder ingredients(long... ids) {
            for (long id : ids) {
                ingredientIds.add(id);
            }
            return this;
        }

        public Builder tags(long... ids) {
            for (long id : ids) {
                tagIds.add(id);
            }
            return this;
        }

        /** Inserts the recipe and its rows, then fills {@code search_vector} as the publishing code must. */
        public long create() {
            boolean published = status.equals("PUBLISHED") || status.equals("ARCHIVED");
            long id = jdbc.queryForObject("""
                    INSERT INTO recipes (title, description, cuisine_id, category_id, difficulty, servings,
                                         prep_time_minutes, cook_time_minutes, status, published_at,
                                         rating_avg, review_count)
                    VALUES (?, ?, ?, ?, ?, 2, ?, ?, ?, ?, ?, ?)
                    RETURNING id
                    """, Long.class, title, description, cuisine, category, difficulty, prep, cook, status,
                    published ? publishedAt : null, ratingAvg, reviewCount);
            for (int i = 0; i < ingredientIds.size(); i++) {
                jdbc.update("""
                        INSERT INTO recipe_ingredients (recipe_id, ingredient_id, unit_id, quantity, position)
                        VALUES (?, ?, ?, 100, ?)
                        """, id, ingredientIds.get(i), unitId, i + 1);
            }
            for (long tagId : tagIds) {
                jdbc.update("INSERT INTO recipe_tags (recipe_id, tag_id) VALUES (?, ?)", id, tagId);
            }
            if (coverUrl != null) {
                jdbc.update("INSERT INTO recipe_media (recipe_id, role, external_url) VALUES (?, 'COVER', ?)", id,
                        coverUrl);
            } else if (coverKey != null) {
                jdbc.update("""
                        INSERT INTO recipe_media (recipe_id, role, storage_key, content_type, size_bytes)
                        VALUES (?, 'COVER', ?, 'image/jpeg', 1000)
                        """, id, coverKey);
            }
            jdbc.update("UPDATE recipes SET search_vector = f_recipe_search_vector(id) WHERE id = ?", id);
            return id;
        }
    }
}
