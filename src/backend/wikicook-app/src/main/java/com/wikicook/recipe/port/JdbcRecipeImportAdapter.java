package com.wikicook.recipe.port;

import com.wikicook.shoppinglist.port.RecipeDetailPort;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 060's real {@link RecipeDetailPort} for 040: the ingredients of a recipe from the recipe tables, so "Thêm vào danh
 * sách đi chợ" on the detail page works for every recipe. The 040 seed adapter backs off once this bean exists
 * ({@code @Primary} also wins when both happen to be registered). Optional ingredients are left out, as in the
 * meal-plan import.
 */
@Component
@Primary
public class JdbcRecipeImportAdapter implements RecipeDetailPort {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcRecipeImportAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<RecipeImportDto> getRecipeForImport(Long recipeId) {
        Map<String, Object> params = Map.of("id", recipeId);
        List<RecipeImportDto> recipe = jdbc.query("SELECT title, servings FROM recipes WHERE id = :id", params,
                (rs, n) -> {
                    int servings = rs.getInt("servings");
                    return new RecipeImportDto(recipeId, rs.getString("title"),
                            BigDecimal.valueOf(rs.wasNull() || servings <= 0 ? 1 : servings), new ArrayList<>());
                });
        if (recipe.isEmpty()) {
            return Optional.empty();
        }
        List<RecipeIngredientImportDto> ingredients = recipe.get(0).ingredients();
        jdbc.query("""
                SELECT ri.ingredient_id, i.name, ri.quantity, ri.unit_id, u.name_vi AS unit_name,
                       i.aisle_category_id, ac.code AS aisle_code, i.kind
                  FROM recipe_ingredients ri
                  JOIN ingredients i ON i.id = ri.ingredient_id
                  JOIN units u ON u.id = ri.unit_id
                  LEFT JOIN aisle_categories ac ON ac.id = i.aisle_category_id
                 WHERE ri.recipe_id = :id AND NOT ri.is_optional
                 ORDER BY ri.position, ri.id
                """, params, rs -> {
            String aisleCode = rs.getString("aisle_code");
            ingredients.add(new RecipeIngredientImportDto(
                    rs.getLong("ingredient_id"), rs.getString("name"), rs.getBigDecimal("quantity"),
                    rs.getLong("unit_id"), rs.getString("unit_name"), rs.getObject("aisle_category_id", Long.class),
                    aisleCode != null ? aisleCode : aisleFromKind(rs.getString("kind"))));
        });
        return Optional.of(recipe.get(0));
    }

    /** An ingredient without an aisle gets one from its kind; the rest falls back to GENERAL in 040. */
    static String aisleFromKind(String kind) {
        if (kind == null) {
            return null;
        }
        return switch (kind) {
            case "MEAT", "SEAFOOD" -> "MEAT_SEAFOOD";
            case "EGG", "DAIRY" -> "DAIRY_EGGS";
            default -> null;
        };
    }
}
