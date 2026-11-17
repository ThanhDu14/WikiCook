package com.wikicook.profile.support;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Rows the profile ITs need on top of {@code AuthTestSupport}: the seeded allergens, approved or proposed ingredients,
 * cuisines, and a read of what a user's dietary profile looks like in the tables (not through the API, so a test can
 * tell what was really stored).
 */
public final class ProfileFixtures {

    private final JdbcTemplate jdbc;

    public ProfileFixtures(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** {@code allergens.id} of a code seeded by V211 ({@code PEANUT}, {@code MILK} …). */
    public long allergen(String code) {
        return jdbc.queryForObject("SELECT id FROM allergens WHERE code = ?", Long.class, code);
    }

    /** An APPROVED ingredient of kind OTHER; an existing approved one with the same name is reused. */
    public long ingredient(String name) {
        return ingredient(name, "OTHER");
    }

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

    /** An ingredient a Member proposed and nobody approved yet: not offered, not accepted for a new exclusion. */
    public long proposedIngredient(String name) {
        return jdbc.queryForObject("""
                INSERT INTO ingredients (name, name_normalized, kind, status)
                VALUES (?, f_unaccent(lower(trim(?))), 'OTHER', 'PROPOSED') RETURNING id
                """, Long.class, name, name);
    }

    public long cuisine(String name, boolean active, int position) {
        return jdbc.queryForObject("""
                INSERT INTO cuisines (name, slug, is_active, position) VALUES (?, ?, ?, ?) RETURNING id
                """, Long.class, name, "c-" + UUID.randomUUID(), active, position);
    }

    public long cuisine(String name) {
        return cuisine(name, true, 0);
    }

    public void tagIngredientWithAllergen(long ingredientId, long allergenId) {
        jdbc.update("INSERT INTO ingredient_allergens (ingredient_id, allergen_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                ingredientId, allergenId);
    }

    /** The stored profile of a user, read straight from the tables. */
    public Stored stored(long userId) {
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT diet_type, household_size, max_cook_time_minutes, dietary_version FROM users WHERE id = ?
                """, userId);
        return new Stored(
                (String) row.get("diet_type"),
                row.get("household_size") == null ? null : ((Number) row.get("household_size")).intValue(),
                row.get("max_cook_time_minutes") == null ? null : ((Number) row.get("max_cook_time_minutes")).intValue(),
                ((Number) row.get("dietary_version")).intValue(),
                Set.copyOf(jdbc.queryForList("SELECT allergen_id FROM user_allergens WHERE user_id = ?", Long.class, userId)),
                Set.copyOf(jdbc.queryForList("SELECT ingredient_id FROM user_excluded_ingredients WHERE user_id = ?",
                        Long.class, userId)),
                Set.copyOf(jdbc.queryForList("SELECT cuisine_id FROM user_preferred_cuisines WHERE user_id = ?",
                        Long.class, userId)));
    }

    public record Stored(String dietType, Integer householdSize, Integer maxCookTimeMinutes, int dietaryVersion,
            Set<Long> allergenIds, Set<Long> excludedIngredientIds, Set<Long> preferredCuisineIds) {
    }

    /** The JSON body of {@code PUT /me/dietary-profile}; {@code null} numbers are sent as JSON null. */
    public static String body(Set<Long> allergens, Set<Long> excluded, String dietType, Set<Long> cuisines,
            Object householdSize, Object maxCookTimeMinutes, Object version) {
        return """
                {"allergenIds": %s, "excludedIngredientIds": %s, "dietType": "%s", "preferredCuisineIds": %s,
                 "householdSize": %s, "maxCookTimeMinutes": %s, "version": %s}
                """.formatted(allergens, excluded, dietType, cuisines, householdSize, maxCookTimeMinutes, version);
    }
}
