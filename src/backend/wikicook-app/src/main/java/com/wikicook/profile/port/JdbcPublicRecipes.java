package com.wikicook.profile.port;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Stand-in for 060's {@link PublicRecipesPort}: reads {@code recipes} directly with {@code idx_recipes_author_status}.
 * Only {@code PUBLISHED} recipes of the author; the cover is read the way the other modules read it.
 */
public class JdbcPublicRecipes implements PublicRecipesPort {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcPublicRecipes(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<PublicRecipeCard> findPublishedByAuthor(long authorId, int page, int size) {
        return jdbc.query("""
                SELECT r.id, r.title, r.total_time_minutes, r.rating_avg, r.review_count,
                       (SELECT COALESCE(m.storage_key, m.external_url) FROM recipe_media m
                         WHERE m.recipe_id = r.id AND m.role = 'COVER') AS cover_url
                  FROM recipes r
                 WHERE r.author_id = :authorId AND r.status = 'PUBLISHED'
                 ORDER BY r.published_at DESC NULLS LAST, r.id DESC
                 LIMIT :size OFFSET :offset
                """, Map.of("authorId", authorId, "size", size, "offset", (long) page * size),
                (rs, rowNum) -> new PublicRecipeCard(rs.getLong("id"), rs.getString("title"), rs.getString("cover_url"),
                        rs.getObject("total_time_minutes", Integer.class), rs.getDouble("rating_avg"),
                        rs.getInt("review_count")));
    }

    @Override
    public long countPublishedByAuthor(long authorId) {
        Long count = jdbc.queryForObject("SELECT count(*) FROM recipes WHERE author_id = :authorId AND status = 'PUBLISHED'",
                Map.of("authorId", authorId), Long.class);
        return count == null ? 0 : count;
    }
}
