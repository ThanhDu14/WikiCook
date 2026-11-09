package com.wikicook.search.repository;

import com.wikicook.common.diet.DietRestrictions;
import com.wikicook.common.diet.DietRuleSql;
import com.wikicook.mealplan.domain.DietType;
import com.wikicook.search.domain.Difficulty;
import com.wikicook.search.domain.SearchCriteria;
import com.wikicook.search.domain.TaxonomyRef;
import com.wikicook.search.dto.ConflictDto;
import com.wikicook.search.dto.RecipeCardDto;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The search query of spec 020 (research R2–R5, R7, R8). SQL is assembled only from the fixed fragments below; every
 * user value is a bound parameter, and the keyword reaches SQL as letter/digit tokens joined with {@code |}.
 * Only PUBLISHED recipes are ever read (FR-002, NFR-005).
 */
@Repository
public class RecipeSearchQueryRepository {

    private static final String TS_QUERY = "to_tsquery('simple', f_unaccent(:tsq))";

    private static final String CARD_COLUMNS = """
            r.id, r.title, r.total_time_minutes, r.difficulty, r.rating_avg, r.review_count, cu.name AS cuisine,
            (SELECT coalesce(m.external_url, '/api/v1/media/' || m.storage_key)
               FROM recipe_media m WHERE m.recipe_id = r.id AND m.role = 'COVER') AS cover_url""";

    private static final String NEWEST = "r.published_at DESC, r.id DESC";

    private final NamedParameterJdbcTemplate jdbc;

    public RecipeSearchQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Matching recipes; {@code profileRule} null = no dietary hiding. */
    public long count(SearchCriteria criteria, DietRestrictions profileRule) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = where(criteria, profileRule, params);
        Long count = jdbc.queryForObject("SELECT count(*) FROM recipes r WHERE " + where, params, Long.class);
        return count == null ? 0 : count;
    }

    /** One slice of the results in the criteria's sort order; cards have no conflicts yet. */
    public List<RecipeCardDto> page(SearchCriteria criteria, DietRestrictions profileRule, int offset, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("offset", offset)
                .addValue("limit", limit);
        String where = where(criteria, profileRule, params);
        String sql = "SELECT " + CARD_COLUMNS + """

                FROM recipes r
                LEFT JOIN cuisines cu ON cu.id = r.cuisine_id
                WHERE """ + " " + where + "\nORDER BY " + orderBy(criteria) + "\nOFFSET :offset LIMIT :limit";
        return jdbc.query(sql, params, RecipeSearchQueryRepository::card);
    }

    /**
     * Allergens (catalogue links only, FR-018) and excluded ingredients of the given recipes; recipes without a
     * conflict are absent. Names are distinct per recipe and sorted.
     */
    public Map<Long, List<ConflictDto>> conflicts(Collection<Long> recipeIds, Set<Long> allergenIds,
            Set<Long> excludedIngredientIds) {
        Map<Long, List<ConflictDto>> result = new LinkedHashMap<>();
        if (recipeIds.isEmpty()) {
            return result;
        }
        List<String> parts = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource("ids", recipeIds);
        if (!allergenIds.isEmpty()) {
            parts.add("""
                    SELECT DISTINCT ri.recipe_id, 'ALLERGEN' AS type, a.name_vi AS name
                      FROM recipe_ingredients ri
                      JOIN ingredient_allergens ia ON ia.ingredient_id = ri.ingredient_id
                      JOIN allergens a ON a.id = ia.allergen_id
                     WHERE ri.recipe_id IN (:ids) AND ia.allergen_id IN (:allergenIds)""");
            params.addValue("allergenIds", allergenIds);
        }
        if (!excludedIngredientIds.isEmpty()) {
            parts.add("""
                    SELECT DISTINCT ri.recipe_id, 'EXCLUDED_INGREDIENT' AS type, i.name AS name
                      FROM recipe_ingredients ri
                      JOIN ingredients i ON i.id = ri.ingredient_id
                     WHERE ri.recipe_id IN (:ids) AND ri.ingredient_id IN (:excludedIds)""");
            params.addValue("excludedIds", excludedIngredientIds);
        }
        if (parts.isEmpty()) {
            return result;
        }
        String sql = "SELECT * FROM (" + String.join("\nUNION\n", parts) + ") c ORDER BY recipe_id, type, name";
        jdbc.query(sql, params, rs -> {
            result.computeIfAbsent(rs.getLong("recipe_id"), id -> new ArrayList<>())
                    .add(new ConflictDto(ConflictDto.Type.valueOf(rs.getString("type")), rs.getString("name")));
        });
        return result;
    }

    /** Home page: editor-pinned recipes by {@code featured_position}, then the newest (contract getFeaturedRecipes). */
    public List<RecipeCardDto> featured(int limit) {
        String sql = "SELECT " + CARD_COLUMNS + """

                FROM recipes r
                LEFT JOIN cuisines cu ON cu.id = r.cuisine_id
                WHERE r.status = 'PUBLISHED'
                ORDER BY r.featured_position ASC NULLS LAST, r.published_at DESC, r.id DESC
                LIMIT :limit""";
        return jdbc.query(sql, new MapSqlParameterSource("limit", limit), RecipeSearchQueryRepository::card);
    }

    private static String where(SearchCriteria c, DietRestrictions profileRule, MapSqlParameterSource params) {
        List<String> conditions = new ArrayList<>();
        conditions.add("r.status = 'PUBLISHED'");
        if (!c.keyword().isBlank()) {
            conditions.add("r.search_vector @@ " + TS_QUERY);
            params.addValue("tsq", c.keyword().toTsQuery());
        }
        if (c.time() != null) {
            if (c.time().maxMinutes() == null) {
                conditions.add("r.total_time_minutes > 60");
            } else {
                conditions.add("r.total_time_minutes <= :maxMinutes");
                params.addValue("maxMinutes", c.time().maxMinutes());
            }
        }
        if (!c.cuisines().isEmpty()) {
            conditions.add("r.cuisine_id IN (:cuisineIds)");
            params.addValue("cuisineIds", ids(c.cuisines()));
        }
        if (!c.categories().isEmpty()) {
            conditions.add("r.category_id IN (:categoryIds)");
            params.addValue("categoryIds", ids(c.categories()));
        }
        if (!c.difficulties().isEmpty()) {
            conditions.add("r.difficulty IN (:difficulties)");
            params.addValue("difficulties", c.difficulties().stream().map(Difficulty::name).toList());
        }
        if (!c.methods().isEmpty()) {
            conditions.add("EXISTS (SELECT 1 FROM recipe_tags rt WHERE rt.recipe_id = r.id AND rt.tag_id IN (:methodIds))");
            params.addValue("methodIds", ids(c.methods()));
        }
        if (c.diet() != null) {
            // Diet is a normal filter, never taken from the profile (FR-022); it only uses ingredients.kind.
            add(conditions, params, DietRuleSql.notViolating("r.id", new DietRestrictions(Set.of(), Set.of(), c.diet())));
        }
        if (profileRule != null) {
            // Allergens and excluded ingredients only; the shared rule of 031 (FR-021).
            add(conditions, params, DietRuleSql.notViolating("r.id", new DietRestrictions(profileRule.allergenIds(),
                    profileRule.excludedIngredientIds(), DietType.NONE)));
        }
        return String.join("\n  AND ", conditions);
    }

    private static void add(List<String> conditions, MapSqlParameterSource params, DietRuleSql.SqlClause clause) {
        conditions.add("(" + clause.sql() + ")");
        params.addValues(clause.params());
    }

    private static String orderBy(SearchCriteria c) {
        return switch (c.sort()) {
            case RELEVANCE -> c.keyword().isBlank() ? NEWEST : "ts_rank(r.search_vector, " + TS_QUERY + ") DESC, " + NEWEST;
            case NEWEST -> NEWEST;
            case TOP_RATED -> "(r.review_count >= 3) DESC, r.rating_avg DESC, r.review_count DESC, " + NEWEST;
            case QUICKEST -> "r.total_time_minutes ASC, " + NEWEST;
        };
    }

    private static List<Long> ids(List<TaxonomyRef> refs) {
        return refs.stream().map(TaxonomyRef::id).toList();
    }

    private static RecipeCardDto card(ResultSet rs, int row) throws SQLException {
        int reviewCount = rs.getInt("review_count");
        BigDecimal rating = rs.getBigDecimal("rating_avg");
        return new RecipeCardDto(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("cover_url"),
                rs.getInt("total_time_minutes"),
                Difficulty.valueOf(rs.getString("difficulty")),
                reviewCount == 0 ? null : rating,
                reviewCount,
                rs.getString("cuisine"),
                List.of());
    }
}
