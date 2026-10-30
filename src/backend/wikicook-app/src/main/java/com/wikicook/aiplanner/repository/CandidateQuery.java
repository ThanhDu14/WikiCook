package com.wikicook.aiplanner.repository;

import com.pgvector.PGvector;
import com.wikicook.aiplanner.domain.Candidate;
import com.wikicook.mealplan.domain.Meal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Step 2 of generation (research R5): ranks recipes already found eligible in step 1, by cosine distance to the
 * meal's query vector, or by SQL when there is no vector. Vectors of another model are not comparable, so they
 * count as missing.
 */
@Repository
public class CandidateQuery {

    private static final String SELECT = """
            SELECT r.id, r.title, r.for_breakfast, r.for_lunch, r.for_dinner, r.total_time_minutes,
                   r.cuisine_id, cu.name AS cuisine_name, r.category_id,
                   ARRAY(SELECT ri.ingredient_id FROM recipe_ingredients ri
                          WHERE ri.recipe_id = r.id ORDER BY ri.position LIMIT 5) AS ingredient_ids,
                   ARRAY(SELECT i.name FROM recipe_ingredients ri JOIN ingredients i ON i.id = ri.ingredient_id
                          WHERE ri.recipe_id = r.id ORDER BY ri.position LIMIT 5) AS ingredient_names
              FROM recipes r
              LEFT JOIN cuisines cu ON cu.id = r.cuisine_id
            """;
    /** Preferred cuisine first, then rating, then id (research R5). */
    private static final String SQL_ORDER = """
             ORDER BY (r.cuisine_id IN (:preferred)) DESC, r.rating_avg DESC, r.id
             LIMIT :k
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public CandidateQuery(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The {@code k} recipes among {@code ids} closest to {@code query}; only vectors of {@code model} count. */
    public List<Candidate> nearest(Collection<Long> ids, float[] query, String model, int k) {
        if (ids.isEmpty()) {
            return List.of();
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ids", ids).addValue("query", new PGvector(query)).addValue("model", model).addValue("k", k);
        return jdbc.query(SELECT + """
                  JOIN recipe_embeddings e ON e.recipe_id = r.id AND e.model = :model
                 WHERE r.id IN (:ids)
                 ORDER BY e.embedding <=> :query, r.id
                 LIMIT :k
                """, params, (rs, rowNum) -> candidate(rs));
    }

    /** Recipes among {@code ids} with no vector of {@code model} yet (FR-029), in SQL order. */
    public List<Candidate> withoutEmbedding(Collection<Long> ids, Set<Long> preferredCuisineIds, String model, int k) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.query(SELECT + """
                 WHERE r.id IN (:ids)
                   AND NOT EXISTS (SELECT 1 FROM recipe_embeddings e WHERE e.recipe_id = r.id AND e.model = :model)
                """ + SQL_ORDER, sqlParams(ids, preferredCuisineIds, k).addValue("model", model),
                (rs, rowNum) -> candidate(rs));
    }

    /** Ranking without vectors, used when the embedding provider is unavailable. */
    public List<Candidate> bySql(Collection<Long> ids, Set<Long> preferredCuisineIds, int k) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.query(SELECT + " WHERE r.id IN (:ids)\n" + SQL_ORDER, sqlParams(ids, preferredCuisineIds, k),
                (rs, rowNum) -> candidate(rs));
    }

    /** Names of the given cuisines, for the query text and the prompt. */
    public List<String> cuisineNames(Collection<Long> cuisineIds) {
        if (cuisineIds.isEmpty()) {
            return List.of();
        }
        return jdbc.queryForList("SELECT name FROM cuisines WHERE id IN (:ids) ORDER BY position, name",
                new MapSqlParameterSource("ids", cuisineIds), String.class);
    }

    private static MapSqlParameterSource sqlParams(Collection<Long> ids, Set<Long> preferredCuisineIds, int k) {
        // IN () is not valid SQL; -1 matches no cuisine.
        Set<Long> preferred = preferredCuisineIds.isEmpty() ? Set.of(-1L) : preferredCuisineIds;
        return new MapSqlParameterSource().addValue("ids", ids).addValue("preferred", preferred).addValue("k", k);
    }

    private static Candidate candidate(ResultSet rs) throws SQLException {
        Set<Meal> meals = EnumSet.noneOf(Meal.class);
        if (rs.getBoolean("for_breakfast")) {
            meals.add(Meal.BREAKFAST);
        }
        if (rs.getBoolean("for_lunch")) {
            meals.add(Meal.LUNCH);
        }
        if (rs.getBoolean("for_dinner")) {
            meals.add(Meal.DINNER);
        }
        return new Candidate(
                rs.getLong("id"),
                rs.getString("title"),
                meals,
                rs.getObject("total_time_minutes", Integer.class),
                rs.getObject("cuisine_id", Long.class),
                rs.getString("cuisine_name"),
                rs.getObject("category_id", Long.class),
                Arrays.asList((Long[]) rs.getArray("ingredient_ids").getArray()),
                Arrays.asList((String[]) rs.getArray("ingredient_names").getArray()),
                0);
    }
}
