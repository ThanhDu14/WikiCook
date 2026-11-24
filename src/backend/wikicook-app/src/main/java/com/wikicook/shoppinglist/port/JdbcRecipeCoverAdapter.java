package com.wikicook.shoppinglist.port;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** {@link RecipeCoverPort} over {@code recipe_media}, read the way the planner (030) reads its covers. */
@Component
public class JdbcRecipeCoverAdapter implements RecipeCoverPort {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcRecipeCoverAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<Long, String> findCovers(Collection<Long> recipeIds) {
        Map<Long, String> covers = new LinkedHashMap<>();
        if (recipeIds.isEmpty()) {
            return covers;
        }
        jdbc.query("""
                SELECT m.recipe_id, COALESCE(m.storage_key, m.external_url) AS url
                  FROM recipe_media m
                 WHERE m.recipe_id IN (:ids) AND m.role = 'COVER'
                """, Map.of("ids", Set.copyOf(recipeIds)), rs -> {
            String url = rs.getString("url");
            if (url != null && !url.isBlank()) {
                covers.putIfAbsent(rs.getLong("recipe_id"), url);
            }
        });
        return covers;
    }
}
