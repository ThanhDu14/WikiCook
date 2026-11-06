package com.wikicook.mealplan.port;

import com.wikicook.common.diet.DietRuleSql;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Stand-in for 060's {@link RecipeSummaryPort}: reads {@code recipes}, the cover in {@code recipe_media} and the
 * ingredient kinds in one query. Steps aside once the recipe module provides the port.
 */
public class JdbcRecipeSummaryAdapter implements RecipeSummaryPort {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcRecipeSummaryAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<Long, RecipeSummary> findByIds(Collection<Long> recipeIds) {
        Map<Long, RecipeSummary> found = new LinkedHashMap<>();
        if (recipeIds.isEmpty()) {
            return found;
        }
        jdbc.query("""
                SELECT r.id, r.title, r.total_time_minutes, r.servings, r.status, r.is_snack,
                       (SELECT COALESCE(m.storage_key, m.external_url) FROM recipe_media m
                         WHERE m.recipe_id = r.id AND m.role = 'COVER') AS cover_url,
                       ARRAY(SELECT DISTINCT i.kind FROM recipe_ingredients ri
                               JOIN ingredients i ON i.id = ri.ingredient_id
                              WHERE ri.recipe_id = r.id) AS kinds
                  FROM recipes r
                 WHERE r.id IN (:ids)
                """, new MapSqlParameterSource("ids", Set.copyOf(recipeIds)), rs -> {
            String[] kinds = (String[]) rs.getArray("kinds").getArray();
            long id = rs.getLong("id");
            found.put(id, new RecipeSummary(id, rs.getString("title"), rs.getString("cover_url"),
                    rs.getObject("total_time_minutes", Integer.class), rs.getObject("servings", Integer.class),
                    RecipeStatus.valueOf(rs.getString("status")), rs.getBoolean("is_snack"),
                    DietRuleSql.dietTypeOf(Set.of(kinds))));
        });
        return found;
    }
}
