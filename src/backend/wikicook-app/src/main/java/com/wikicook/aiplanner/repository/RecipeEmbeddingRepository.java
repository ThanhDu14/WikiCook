package com.wikicook.aiplanner.repository;

import com.pgvector.PGvector;
import com.wikicook.aiplanner.domain.RecipeEmbedding;
import com.wikicook.aiplanner.embedding.RecipeEmbeddingSource;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * {@code recipe_embeddings} over JDBC: Hibernate has no built-in mapping for the {@code vector} column, while
 * {@link PGvector} binds it as a plain JDBC parameter.
 */
@Repository
public class RecipeEmbeddingRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public RecipeEmbeddingRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Published recipes with what the embedding text needs and their stored hash and model ({@code null} when
     * there is no row yet); the caller compares hashes to find stale rows.
     */
    public List<PublishedRecipe> findPublishedAfter(long afterId, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("afterId", afterId).addValue("limit", limit);
        return jdbc.query("""
                SELECT r.id, r.title, c.name AS category, cu.name AS cuisine,
                       r.for_breakfast, r.for_lunch, r.for_dinner, r.total_time_minutes,
                       ARRAY(SELECT i.name
                               FROM recipe_ingredients ri
                               JOIN ingredients i ON i.id = ri.ingredient_id
                              WHERE ri.recipe_id = r.id
                              ORDER BY ri.position) AS ingredients,
                       e.content_hash, e.model
                  FROM recipes r
                  LEFT JOIN categories c ON c.id = r.category_id
                  LEFT JOIN cuisines cu ON cu.id = r.cuisine_id
                  LEFT JOIN recipe_embeddings e ON e.recipe_id = r.id
                 WHERE r.status = 'PUBLISHED' AND r.id > :afterId
                 ORDER BY r.id
                 LIMIT :limit
                """, params, (rs, rowNum) -> new PublishedRecipe(source(rs), rs.getString("content_hash"),
                rs.getString("model")));
    }

    public void upsert(long recipeId, float[] embedding, String model, String contentHash) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("recipeId", recipeId)
                .addValue("embedding", new PGvector(embedding))
                .addValue("model", model)
                .addValue("contentHash", contentHash);
        jdbc.update("""
                INSERT INTO recipe_embeddings (recipe_id, embedding, model, content_hash)
                VALUES (:recipeId, :embedding, :model, :contentHash)
                ON CONFLICT (recipe_id) DO UPDATE
                   SET embedding = EXCLUDED.embedding,
                       model = EXCLUDED.model,
                       content_hash = EXCLUDED.content_hash,
                       updated_at = now()
                """, params);
    }

    public Optional<RecipeEmbedding> findByRecipeId(long recipeId) {
        return jdbc.query("SELECT * FROM recipe_embeddings WHERE recipe_id = :recipeId",
                new MapSqlParameterSource("recipeId", recipeId), (rs, rowNum) -> new RecipeEmbedding(
                        rs.getLong("recipe_id"),
                        new PGvector(rs.getString("embedding")).toArray(),
                        rs.getString("model"),
                        rs.getString("content_hash"),
                        rs.getObject("updated_at", OffsetDateTime.class).toInstant()))
                .stream().findFirst();
    }

    public int deleteByRecipeId(long recipeId) {
        return jdbc.update("DELETE FROM recipe_embeddings WHERE recipe_id = :recipeId",
                new MapSqlParameterSource("recipeId", recipeId));
    }

    /** Rows of recipes that were archived or sent back to draft (FR-030). */
    public int deleteNotPublished() {
        return jdbc.update("""
                DELETE FROM recipe_embeddings e
                 WHERE NOT EXISTS (SELECT 1 FROM recipes r WHERE r.id = e.recipe_id AND r.status = 'PUBLISHED')
                """, new MapSqlParameterSource());
    }

    /** Published recipes not embedded yet; retrieval adds them after the vector matches (research R5). */
    public List<Long> findPublishedWithoutEmbedding(int limit) {
        return jdbc.queryForList("""
                SELECT r.id FROM recipes r
                 WHERE r.status = 'PUBLISHED'
                   AND NOT EXISTS (SELECT 1 FROM recipe_embeddings e WHERE e.recipe_id = r.id)
                 ORDER BY r.id
                 LIMIT :limit
                """, new MapSqlParameterSource("limit", limit), Long.class);
    }

    /**
     * @param contentHash stored hash, or {@code null} when the recipe has no embedding yet
     * @param model       stored model, or {@code null} when the recipe has no embedding yet
     */
    public record PublishedRecipe(RecipeEmbeddingSource source, String contentHash, String model) {
    }

    private static RecipeEmbeddingSource source(ResultSet rs) throws SQLException {
        Array ingredients = rs.getArray("ingredients");
        return new RecipeEmbeddingSource(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("category"),
                rs.getString("cuisine"),
                rs.getBoolean("for_breakfast"),
                rs.getBoolean("for_lunch"),
                rs.getBoolean("for_dinner"),
                rs.getObject("total_time_minutes", Integer.class),
                Arrays.asList((String[]) ingredients.getArray()));
    }
}
