package com.wikicook.profile.repository;

import com.wikicook.mealplan.domain.DietType;
import java.sql.Array;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

/**
 * SQL of the dietary profile: the three columns of {@code users} and the three {@code user_*} lists (research R5). No
 * entity: the lists need {@code ON CONFLICT} and {@code ARRAY(SELECT …)}, and replacing a list with
 * {@code DELETE … NOT IN} plus {@code INSERT … ON CONFLICT DO NOTHING} keeps the {@code created_at} of the rows that
 * stay, which delete-all-then-insert would lose.
 */
@Repository
public class DietaryLinkRepository {

    /**
     * What is stored for one person: the dietary columns of {@code users}, the three lists and the version of the form.
     * One query, so the ports can read it at every picker open (research R8).
     */
    public record Snapshot(DietType dietType, Integer householdSize, Integer maxCookTimeMinutes, int dietaryVersion,
            Set<Long> allergenIds, Set<Long> excludedIngredientIds, Set<Long> preferredCuisineIds) {
    }

    public record AllergenRow(long id, String code, String nameVi) {
    }

    private static final String SNAPSHOT = """
            SELECT u.diet_type, u.household_size, u.max_cook_time_minutes, u.dietary_version,
                   ARRAY(SELECT ua.allergen_id FROM user_allergens ua WHERE ua.user_id = u.id) AS allergen_ids,
                   ARRAY(SELECT ue.ingredient_id FROM user_excluded_ingredients ue WHERE ue.user_id = u.id) AS ingredient_ids,
                   ARRAY(SELECT uc.cuisine_id FROM user_preferred_cuisines uc WHERE uc.user_id = u.id) AS cuisine_ids
              FROM users u WHERE u.id = :userId
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public DietaryLinkRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Empty when there is no such user. */
    public Optional<Snapshot> findSnapshot(long userId) {
        return jdbc.query(SNAPSHOT, Map.of("userId", userId), rs -> {
            if (!rs.next()) {
                return Optional.<Snapshot>empty();
            }
            return Optional.of(new Snapshot(
                    DietType.valueOf(rs.getString("diet_type")),
                    rs.getObject("household_size", Integer.class),
                    rs.getObject("max_cook_time_minutes", Integer.class),
                    rs.getInt("dietary_version"),
                    ids(rs.getArray("allergen_ids")),
                    ids(rs.getArray("ingredient_ids")),
                    ids(rs.getArray("cuisine_ids"))));
        });
    }

    /**
     * Writes the three scalar columns and moves the version up by one, only when the row still has
     * {@code expectedVersion}. The statement also locks the row, so two saves of one person run one after the other.
     *
     * @return 1, or 0 when the version has moved on (the caller reports {@code PROFILE_CHANGED})
     */
    public int updateScalars(long userId, int expectedVersion, DietType dietType, Integer householdSize,
            Integer maxCookTimeMinutes) {
        return jdbc.update("""
                UPDATE users SET diet_type = :dietType, household_size = :householdSize,
                                 max_cook_time_minutes = :maxCookTime, dietary_version = dietary_version + 1,
                                 updated_at = now()
                 WHERE id = :userId AND dietary_version = :version
                """, new MapSqlParameterSource()
                .addValue("dietType", dietType.name())
                .addValue("householdSize", householdSize, Types.SMALLINT)
                .addValue("maxCookTime", maxCookTimeMinutes, Types.SMALLINT)
                .addValue("userId", userId)
                .addValue("version", expectedVersion));
    }

    public void replaceAllergens(long userId, Set<Long> allergenIds) {
        replace("user_allergens", "allergen_id", userId, allergenIds);
    }

    public void replaceExcludedIngredients(long userId, Set<Long> ingredientIds) {
        replace("user_excluded_ingredients", "ingredient_id", userId, ingredientIds);
    }

    public void replaceCuisines(long userId, Set<Long> cuisineIds) {
        replace("user_preferred_cuisines", "cuisine_id", userId, cuisineIds);
    }

    /** {@code table} and {@code column} are constants of this class, never input. */
    private void replace(String table, String column, long userId, Set<Long> ids) {
        if (ids.isEmpty()) {
            jdbc.update("DELETE FROM " + table + " WHERE user_id = :userId", Map.of("userId", userId));
            return;
        }
        jdbc.update("DELETE FROM " + table + " WHERE user_id = :userId AND " + column + " NOT IN (:ids)",
                Map.of("userId", userId, "ids", ids));
        SqlParameterSource[] rows = ids.stream()
                .<SqlParameterSource>map(id -> new MapSqlParameterSource("userId", userId).addValue("id", id))
                .toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate("INSERT INTO " + table + " (user_id, " + column + ") VALUES (:userId, :id) ON CONFLICT DO NOTHING",
                rows);
    }

    /** The ids of {@code ids} that are rows of {@code allergens}. */
    public Set<Long> existingAllergenIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jdbc.queryForList("SELECT id FROM allergens WHERE id IN (:ids)", Map.of("ids", ids), Long.class));
    }

    public List<AllergenRow> listAllergens() {
        return jdbc.query("SELECT id, code, name_vi FROM allergens ORDER BY id", Map.of(), (rs, rowNum) ->
                new AllergenRow(rs.getLong("id"), rs.getString("code"), rs.getString("name_vi")));
    }

    public List<AllergenRow> findAllergens(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.query("SELECT id, code, name_vi FROM allergens WHERE id IN (:ids) ORDER BY id", Map.of("ids", ids),
                (rs, rowNum) -> new AllergenRow(rs.getLong("id"), rs.getString("code"), rs.getString("name_vi")));
    }

    /**
     * The ingredients tagged with any of these allergens in {@code ingredient_allergens}: what 040 needs, because a
     * shopping line has an ingredient, not an allergen (FR-010).
     */
    public Set<Long> ingredientIdsWithAllergens(Collection<Long> allergenIds) {
        if (allergenIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jdbc.queryForList("SELECT DISTINCT ingredient_id FROM ingredient_allergens WHERE allergen_id IN (:ids)",
                Map.of("ids", allergenIds), Long.class));
    }

    private static Set<Long> ids(Array array) throws SQLException {
        Set<Long> ids = new HashSet<>();
        if (array == null) {
            return ids;
        }
        Object[] values = (Object[]) array.getArray();
        for (Object value : values) {
            ids.add(((Number) value).longValue());
        }
        return ids;
    }
}
