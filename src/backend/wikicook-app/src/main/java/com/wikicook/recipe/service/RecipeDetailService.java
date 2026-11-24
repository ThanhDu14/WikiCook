package com.wikicook.recipe.service;

import com.wikicook.common.media.MediaUrls;
import com.wikicook.recipe.dto.RecipeDetailDto;
import com.wikicook.recipe.dto.RecipeDetailDto.Author;
import com.wikicook.recipe.dto.RecipeDetailDto.Ingredient;
import com.wikicook.recipe.dto.RecipeDetailDto.IngredientGroup;
import com.wikicook.recipe.dto.RecipeDetailDto.LookupOption;
import com.wikicook.recipe.dto.RecipeDetailDto.Source;
import com.wikicook.recipe.dto.RecipeDetailDto.Step;
import com.wikicook.recipe.dto.RecipeDetailDto.Unit;
import com.wikicook.recipe.dto.RecipeDetailDto.Video;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the detail page of one recipe (060 T035). A PUBLISHED recipe is shown to anyone, a guest too; any other status
 * only to its author, everyone else gets the same 404 as for a missing id.
 */
@Service
@Transactional(readOnly = true)
public class RecipeDetailService {

    private final NamedParameterJdbcTemplate jdbc;

    public RecipeDetailService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public RecipeDetailDto get(long recipeId, Optional<Long> viewerId) {
        Map<String, Object> params = Map.of("id", recipeId);
        List<RecipeDetailDto> found = jdbc.query("""
                SELECT r.*, u.display_name AS author_name, u.is_system AS author_is_system,
                       c.slug AS cuisine_slug, c.name AS cuisine_name, cat.slug AS category_slug, cat.name AS category_name
                  FROM recipes r
                  LEFT JOIN users u ON u.id = r.author_id
                  LEFT JOIN cuisines c ON c.id = r.cuisine_id
                  LEFT JOIN categories cat ON cat.id = r.category_id
                 WHERE r.id = :id
                """, params, (rs, n) -> header(rs, viewerId));
        RecipeDetailDto header = found.stream().findFirst().orElseThrow(() -> new RecipeNotFoundException(recipeId));
        if (!"PUBLISHED".equals(header.status()) && !header.isOwner()) {
            throw new RecipeNotFoundException(recipeId);
        }

        Map<String, List<LookupOption>> tagsByKind = tags(params);
        return new RecipeDetailDto(header.id(), header.status(), header.title(), header.description(), cover(params),
                header.cuisine(), header.category(), tagsByKind.getOrDefault("METHOD", List.of()),
                tagsByKind.getOrDefault("TAG", List.of()), tagsByKind.getOrDefault("EQUIPMENT", List.of()),
                header.difficulty(), header.prepTimeMinutes(), header.cookTimeMinutes(), header.totalTimeMinutes(),
                header.servings(), header.ratingAvg(), header.reviewCount(), ingredientGroups(params), steps(params),
                header.video(), header.author(), header.source(), header.publishedAt(), header.isOwner());
    }

    private static RecipeDetailDto header(ResultSet rs, Optional<Long> viewerId) throws SQLException {
        Long authorId = rs.getObject("author_id", Long.class);
        boolean owner = authorId != null && viewerId.filter(authorId::equals).isPresent();
        int reviewCount = rs.getInt("review_count");
        String videoUrl = rs.getString("video_url");
        String sourceUrl = rs.getString("source_url");
        Timestamp publishedAt = rs.getTimestamp("published_at");
        boolean systemAuthor = rs.getBoolean("author_is_system");
        String authorName = rs.getString("author_name");
        String sourceName = rs.getString("source_name");
        return new RecipeDetailDto(
                rs.getLong("id"), rs.getString("status"), rs.getString("title"), rs.getString("description"), null,
                lookup(rs, "cuisine_id", "cuisine_slug", "cuisine_name"),
                lookup(rs, "category_id", "category_slug", "category_name"),
                List.of(), List.of(), List.of(),
                rs.getString("difficulty"),
                rs.getObject("prep_time_minutes", Integer.class), rs.getObject("cook_time_minutes", Integer.class),
                rs.getObject("total_time_minutes", Integer.class), rs.getObject("servings", Integer.class),
                reviewCount > 0 ? rs.getBigDecimal("rating_avg") : null, reviewCount,
                List.of(), List.of(),
                videoUrl == null || videoUrl.isBlank() ? null : new Video(videoUrl, rs.getString("video_platform")),
                // an imported recipe belongs to the system account: show its source instead of that name
                new Author(systemAuthor ? null : authorId,
                        systemAuthor || authorName == null ? (sourceName != null ? sourceName : "WikiCook") : authorName),
                sourceUrl == null || sourceUrl.isBlank() ? null
                        : new Source(sourceName != null ? sourceName : "angingon.com", sourceUrl),
                publishedAt != null ? publishedAt.toInstant() : null,
                owner);
    }

    private static LookupOption lookup(ResultSet rs, String id, String slug, String name) throws SQLException {
        Long value = rs.getObject(id, Long.class);
        return value == null ? null : new LookupOption(value, rs.getString(slug), rs.getString(name));
    }

    private String cover(Map<String, Object> params) {
        List<String> covers = jdbc.query("""
                SELECT storage_key, external_url FROM recipe_media
                 WHERE recipe_id = :id AND role = 'COVER' ORDER BY position, id LIMIT 1
                """, params, (rs, n) -> mediaUrl(rs));
        return covers.isEmpty() ? null : covers.get(0);
    }

    /** An uploaded file (storage key) goes through the media store's public URL; a scraped one is used as is. */
    private static String mediaUrl(ResultSet rs) throws SQLException {
        String key = rs.getString("storage_key");
        return key != null && !key.isBlank() ? MediaUrls.of(key) : rs.getString("external_url");
    }

    private Map<String, List<LookupOption>> tags(Map<String, Object> params) {
        Map<String, List<LookupOption>> byKind = new LinkedHashMap<>();
        jdbc.query("""
                SELECT t.id, t.kind, t.slug, t.name FROM recipe_tags rt JOIN tags t ON t.id = rt.tag_id
                 WHERE rt.recipe_id = :id ORDER BY t.position, t.name
                """, params, rs -> {
            byKind.computeIfAbsent(rs.getString("kind"), k -> new ArrayList<>())
                    .add(new LookupOption(rs.getLong("id"), rs.getString("slug"), rs.getString("name")));
        });
        return byKind;
    }

    private List<IngredientGroup> ingredientGroups(Map<String, Object> params) {
        Map<String, List<Ingredient>> groups = new LinkedHashMap<>();
        groups.put(null, new ArrayList<>()); // ungrouped rows come first
        jdbc.query("""
                SELECT ri.group_name, ri.ingredient_id, i.name, ri.quantity, u.code AS unit_code,
                       u.name_vi AS unit_name, ri.amount_text, ri.note, ri.is_optional
                  FROM recipe_ingredients ri
                  JOIN ingredients i ON i.id = ri.ingredient_id
                  JOIN units u ON u.id = ri.unit_id
                 WHERE ri.recipe_id = :id
                 ORDER BY ri.position, ri.id
                """, params, rs -> {
            String group = rs.getString("group_name");
            BigDecimal quantity = rs.getBigDecimal("quantity");
            groups.computeIfAbsent(group == null || group.isBlank() ? null : group, k -> new ArrayList<>())
                    .add(new Ingredient(rs.getLong("ingredient_id"), rs.getString("name"),
                            quantity != null ? quantity.stripTrailingZeros() : null,
                            new Unit(rs.getString("unit_code"), rs.getString("unit_name")),
                            rs.getString("amount_text"), rs.getString("note"), rs.getBoolean("is_optional")));
        });
        List<IngredientGroup> result = new ArrayList<>();
        groups.forEach((name, items) -> {
            if (!items.isEmpty()) {
                result.add(new IngredientGroup(name, items));
            }
        });
        return result;
    }

    private List<Step> steps(Map<String, Object> params) {
        Map<Long, List<String>> images = new LinkedHashMap<>();
        jdbc.query("""
                SELECT step_id, storage_key, external_url FROM recipe_media
                 WHERE recipe_id = :id AND role = 'STEP' AND step_id IS NOT NULL ORDER BY position, id
                """, params, rs -> {
            images.computeIfAbsent(rs.getLong("step_id"), k -> new ArrayList<>()).add(mediaUrl(rs));
        });
        return jdbc.query("""
                SELECT id, position, title, content, timer_seconds FROM recipe_steps
                 WHERE recipe_id = :id ORDER BY position, id
                """, params, (rs, n) -> new Step(rs.getInt("position"), rs.getString("title"), rs.getString("content"),
                rs.getObject("timer_seconds", Integer.class), images.getOrDefault(rs.getLong("id"), List.of())));
    }
}
