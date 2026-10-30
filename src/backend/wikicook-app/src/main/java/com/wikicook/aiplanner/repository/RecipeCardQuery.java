package com.wikicook.aiplanner.repository;

import com.wikicook.aiplanner.domain.RecipeCard;
import com.wikicook.common.diet.DietRuleSql;
import com.wikicook.mealplan.domain.DietType;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Title, cover and time of the recipes in a draft. Stands in for 030's {@code RecipeSummaryPort} (060) until it
 * exists. The diet type is the strictest one the ingredient kinds allow, the same rule as {@code DietRuleSql}.
 */
@Repository
public class RecipeCardQuery {

    private final NamedParameterJdbcTemplate jdbc;

    public RecipeCardQuery(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<Long, RecipeCard> cards(Collection<Long> recipeIds) {
        Map<Long, RecipeCard> cards = new LinkedHashMap<>();
        if (recipeIds.isEmpty()) {
            return cards;
        }
        jdbc.query("""
                SELECT r.id, r.title, r.total_time_minutes,
                       (SELECT m.external_url FROM recipe_media m
                         WHERE m.recipe_id = r.id AND m.role = 'COVER') AS cover_url,
                       ARRAY(SELECT DISTINCT i.kind FROM recipe_ingredients ri JOIN ingredients i ON i.id = ri.ingredient_id
                              WHERE ri.recipe_id = r.id) AS kinds
                  FROM recipes r
                 WHERE r.id IN (:ids)
                 ORDER BY r.id
                """, new MapSqlParameterSource("ids", recipeIds), rs -> {
            String[] kinds = (String[]) rs.getArray("kinds").getArray();
            cards.put(rs.getLong("id"), new RecipeCard(rs.getLong("id"), rs.getString("title"),
                    rs.getString("cover_url"), rs.getObject("total_time_minutes", Integer.class),
                    dietType(Set.of(kinds))));
        });
        return cards;
    }

    static DietType dietType(Set<String> kinds) {
        for (DietType diet : new DietType[] {DietType.VEGAN, DietType.VEGETARIAN, DietType.PESCATARIAN}) {
            if (kinds.stream().noneMatch(DietRuleSql.forbiddenKinds(diet)::contains)) {
                return diet;
            }
        }
        return DietType.NONE;
    }
}
