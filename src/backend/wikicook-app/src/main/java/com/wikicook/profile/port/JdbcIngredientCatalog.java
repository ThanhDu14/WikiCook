package com.wikicook.profile.port;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Stand-in for 060's {@link IngredientCatalogPort}: reads {@code ingredients} directly, the way
 * {@code JdbcRecipeSummaryAdapter} does for 030. It uses {@code idx_ingredients_name_trgm} (V201), no new index.
 */
public class JdbcIngredientCatalog implements IngredientCatalogPort {

    /**
     * Research R6. {@code f_unaccent(lower(trim(q)))} is the same expression as {@code chk_ingredients_name_normalized},
     * so "ngo", "Ngò" and "NGO" are one text. The three {@code replace} calls make {@code % _ \} literal in LIKE, and
     * {@code regexp_replace} makes every other punctuation literal in the word-start regex ({@code \m}).
     * Ranking: the name is the text, starts with it, a word starts with it, then similarity.
     */
    private static final String SUGGEST = """
            WITH q AS (
                SELECT f_unaccent(lower(trim(:q))) AS n
            ), p AS (
                SELECT n,
                       replace(replace(replace(n, '\\', '\\\\'), '%', '\\%'), '_', '\\_') AS like_text,
                       regexp_replace(n, '([^[:alnum:] ])', '\\\\\\1', 'g') AS regex_text
                  FROM q
            )
            SELECT i.id, i.name
              FROM ingredients i, p
             WHERE i.status = 'APPROVED'
               AND i.name_normalized LIKE '%' || p.like_text || '%' ESCAPE '\\'
             ORDER BY (i.name_normalized = p.n) DESC,
                      (i.name_normalized LIKE p.like_text || '%' ESCAPE '\\') DESC,
                      (i.name_normalized ~ ('\\m' || p.regex_text)) DESC,
                      similarity(i.name_normalized, p.n) DESC,
                      i.name ASC, i.id ASC
             LIMIT :limit
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcIngredientCatalog(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<IngredientName> suggest(String query, int limit) {
        return jdbc.query(SUGGEST, Map.of("q", query, "limit", limit),
                (rs, rowNum) -> new IngredientName(rs.getLong("id"), rs.getString("name")));
    }

    @Override
    public Set<Long> approvedAmong(Collection<Long> ingredientIds) {
        if (ingredientIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jdbc.queryForList("SELECT id FROM ingredients WHERE id IN (:ids) AND status = 'APPROVED'",
                Map.of("ids", ingredientIds), Long.class));
    }

    @Override
    public Map<Long, String> namesOf(Collection<Long> ingredientIds) {
        Map<Long, String> names = new HashMap<>();
        if (!ingredientIds.isEmpty()) {
            jdbc.query("SELECT id, name FROM ingredients WHERE id IN (:ids)", Map.of("ids", ingredientIds),
                    rs -> {
                        names.put(rs.getLong("id"), rs.getString("name"));
                    });
        }
        return names;
    }
}
