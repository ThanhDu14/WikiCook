package com.wikicook.mealplan.repository;

import com.wikicook.common.diet.DietRestrictions;
import com.wikicook.common.diet.DietRuleSql;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.dto.ConflictDto;
import com.wikicook.mealplan.dto.ConflictType;
import com.wikicook.mealplan.dto.PickerPageDto;
import com.wikicook.mealplan.dto.PickerResultDto;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Read-only search of the recipe picker (research R6, T041). Only PUBLISHED, non-snack recipes; ranking is
 * {@code conflict ASC, meal_suited DESC, cuisine_preferred DESC, within_max_time DESC, similarity DESC, title}.
 * Conflicting recipes stay in the list, last, with their reasons (FR-007b). Diet kinds come from
 * {@link DietRuleSql#forbiddenKinds}, the rule 031 uses. The SQL is assembled from constants only; every value,
 * search text included, is a named parameter.
 */
@Repository
public class RecipePickerQueryRepository {

    /**
     * @param q                  search text, {@code null} or blank = no filter
     * @param preferredCuisineIds ranking only
     * @param maxCookTimeMinutes ranking only, {@code null} = no limit
     * @param onlyRecipeIds      {@code null} = all recipes; otherwise the collection scope (an empty set finds nothing)
     */
    public record Criteria(Meal meal, String q, DietRestrictions restrictions, Set<Long> preferredCuisineIds,
                           Integer maxCookTimeMinutes, Set<Long> onlyRecipeIds, int page, int size) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public RecipePickerQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PickerPageDto search(Criteria c) {
        if (c.onlyRecipeIds() != null && c.onlyRecipeIds().isEmpty()) {
            return new PickerPageDto(List.of(), c.page(), c.size(), false);
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", c.size() + 1)
                .addValue("offset", (long) c.page() * c.size())
                .addValue("maxTime", c.maxCookTimeMinutes(), java.sql.Types.INTEGER);
        String q = c.q() == null ? "" : c.q().strip();

        String allergenLabels = "ARRAY[]::text[]";
        if (!c.restrictions().allergenIds().isEmpty()) {
            allergenLabels = """
                    ARRAY(SELECT DISTINCT a.name_vi FROM recipe_ingredients ri
                            JOIN ingredient_allergens ia ON ia.ingredient_id = ri.ingredient_id
                            JOIN allergens a ON a.id = ia.allergen_id
                           WHERE ri.recipe_id = r.id AND ia.allergen_id IN (:allergenIds) ORDER BY 1)""";
            params.addValue("allergenIds", c.restrictions().allergenIds());
        }
        String excludedLabels = "ARRAY[]::text[]";
        if (!c.restrictions().excludedIngredientIds().isEmpty()) {
            excludedLabels = """
                    ARRAY(SELECT DISTINCT i.name FROM recipe_ingredients ri JOIN ingredients i ON i.id = ri.ingredient_id
                           WHERE ri.recipe_id = r.id AND i.id IN (:excludedIds) ORDER BY 1)""";
            params.addValue("excludedIds", c.restrictions().excludedIngredientIds());
        }
        String dietLabels = "ARRAY[]::text[]";
        Set<String> forbidden = DietRuleSql.forbiddenKinds(c.restrictions().dietType());
        if (!forbidden.isEmpty()) {
            dietLabels = """
                    ARRAY(SELECT DISTINCT i.name FROM recipe_ingredients ri JOIN ingredients i ON i.id = ri.ingredient_id
                           WHERE ri.recipe_id = r.id AND i.kind IN (:forbiddenKinds) ORDER BY 1)""";
            params.addValue("forbiddenKinds", forbidden);
        }
        String preferred = "FALSE";
        if (!c.preferredCuisineIds().isEmpty()) {
            preferred = "COALESCE(r.cuisine_id IN (:preferredCuisineIds), FALSE)";
            params.addValue("preferredCuisineIds", c.preferredCuisineIds());
        }
        List<String> where = new ArrayList<>(List.of("r.status = 'PUBLISHED'", "NOT r.is_snack"));
        String similarity = "0";
        if (!q.isEmpty()) {
            params.addValue("q", q).addValue("qLike", "%" + escapeLike(q) + "%");
            where.add("""
                    (f_unaccent(lower(r.title)) % f_unaccent(lower(:q))
                     OR f_unaccent(lower(r.title)) LIKE f_unaccent(lower(:qLike)) ESCAPE '\\')""");
            similarity = "similarity(f_unaccent(lower(r.title)), f_unaccent(lower(:q)))";
        }
        if (c.onlyRecipeIds() != null) {
            where.add("r.id IN (:onlyIds)");
            params.addValue("onlyIds", c.onlyRecipeIds());
        }

        String sql = """
                SELECT * FROM (
                    SELECT r.id, r.title, r.total_time_minutes, cu.name AS cuisine_name,
                           (SELECT COALESCE(m.storage_key, m.external_url) FROM recipe_media m
                             WHERE m.recipe_id = r.id AND m.role = 'COVER') AS cover_url,
                           %s AS meal_suited,
                           %s AS cuisine_preferred,
                           (CAST(:maxTime AS integer) IS NULL OR r.total_time_minutes <= CAST(:maxTime AS integer))
                               AS within_max_time,
                           %s AS similarity,
                           %s AS allergen_labels,
                           %s AS excluded_labels,
                           %s AS diet_labels
                      FROM recipes r
                      LEFT JOIN cuisines cu ON cu.id = r.cuisine_id
                     WHERE %s
                ) ranked
                ORDER BY (cardinality(allergen_labels) + cardinality(excluded_labels) + cardinality(diet_labels) > 0),
                         meal_suited DESC, cuisine_preferred DESC, within_max_time DESC, similarity DESC, title, id
                LIMIT :limit OFFSET :offset
                """.formatted(mealColumn(c.meal()), preferred, similarity, allergenLabels, excludedLabels, dietLabels,
                String.join("\n   AND ", where));

        List<PickerResultDto> rows = jdbc.query(sql, params, (rs, n) -> new PickerResultDto(rs.getLong("id"),
                rs.getString("title"), rs.getString("cover_url"), rs.getObject("total_time_minutes", Integer.class),
                rs.getString("cuisine_name"), rs.getBoolean("meal_suited"), conflicts(rs)));
        boolean hasNext = rows.size() > c.size();
        return new PickerPageDto(hasNext ? List.copyOf(rows.subList(0, c.size())) : rows, c.page(), c.size(), hasNext);
    }

    private static String mealColumn(Meal meal) {
        return switch (meal) {
            case BREAKFAST -> "r.for_breakfast";
            case LUNCH -> "r.for_lunch";
            case DINNER -> "r.for_dinner";
        };
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static List<ConflictDto> conflicts(ResultSet rs) throws SQLException {
        List<ConflictDto> conflicts = new ArrayList<>();
        add(conflicts, ConflictType.ALLERGEN, rs.getArray("allergen_labels"));
        add(conflicts, ConflictType.EXCLUDED_INGREDIENT, rs.getArray("excluded_labels"));
        add(conflicts, ConflictType.DIET_TYPE, rs.getArray("diet_labels"));
        return List.copyOf(conflicts);
    }

    private static void add(List<ConflictDto> conflicts, ConflictType type, Array labels) throws SQLException {
        for (Object label : (Object[]) labels.getArray()) {
            conflicts.add(new ConflictDto(type, (String) label));
        }
    }
}
