package com.wikicook.shoppinglist.port;

import com.wikicook.mealplan.api.HandoffItem;
import com.wikicook.mealplan.api.MealPlanShoppingFacade;
import com.wikicook.mealplan.api.ShoppingHandoff;
import com.wikicook.shoppinglist.port.RecipeDetailPort.RecipeIngredientImportDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * {@link MealPlanHandoffPort} over the planner's {@link MealPlanShoppingFacade} (030 decides which dishes are
 * available) and the recipe tables for the ingredients. Optional ingredients are left out. An ingredient without an
 * aisle gets one from its {@code kind} (meat and seafood, eggs and dairy), the rest falls back to GENERAL later.
 */
@Component
public class JdbcMealPlanHandoffAdapter implements MealPlanHandoffPort {

    private static final String INGREDIENTS = """
            SELECT ri.recipe_id, r.title, r.servings, ri.ingredient_id, i.name, ri.quantity, ri.unit_id,
                   u.name_vi AS unit_name, i.aisle_category_id, ac.code AS aisle_code, i.kind
              FROM recipe_ingredients ri
              JOIN recipes r ON r.id = ri.recipe_id
              JOIN ingredients i ON i.id = ri.ingredient_id
              JOIN units u ON u.id = ri.unit_id
              LEFT JOIN aisle_categories ac ON ac.id = i.aisle_category_id
             WHERE ri.recipe_id IN (:ids) AND NOT ri.is_optional
             ORDER BY ri.recipe_id, ri.position
            """;

    private final MealPlanShoppingFacade mealPlans;
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcMealPlanHandoffAdapter(MealPlanShoppingFacade mealPlans, NamedParameterJdbcTemplate jdbc) {
        this.mealPlans = mealPlans;
        this.jdbc = jdbc;
    }

    @Override
    public Optional<MealPlanHandoffDto> getMealPlanForShoppingList(Long userId, LocalDate weekStart) {
        ShoppingHandoff handoff = mealPlans.getHandoff(userId, weekStart);
        if (handoff.items().isEmpty()) {
            return Optional.empty();
        }
        Set<Long> recipeIds = handoff.items().stream().map(HandoffItem::recipeId).collect(Collectors.toSet());
        Map<Long, RecipeRows> recipes = loadRecipes(recipeIds);

        List<MealPlanItemHandoffDto> items = new ArrayList<>();
        for (HandoffItem item : handoff.items()) {
            RecipeRows recipe = recipes.get(item.recipeId());
            if (recipe == null) {
                continue; // a recipe without any ingredient row adds nothing to the list
            }
            items.add(new MealPlanItemHandoffDto(item.mealPlanItemId(), item.recipeId(), recipe.title,
                    recipe.baseServings, BigDecimal.valueOf(item.servings()), recipe.ingredients));
        }
        return Optional.of(new MealPlanHandoffDto(weekStart, items, handoff.skippedUnavailable()));
    }

    private Map<Long, RecipeRows> loadRecipes(Set<Long> recipeIds) {
        Map<Long, RecipeRows> recipes = new LinkedHashMap<>();
        jdbc.query(INGREDIENTS, Map.of("ids", recipeIds), rs -> {
            long recipeId = rs.getLong("recipe_id");
            RecipeRows recipe = recipes.computeIfAbsent(recipeId, id -> {
                try {
                    int servings = rs.getInt("servings");
                    return new RecipeRows(rs.getString("title"),
                            BigDecimal.valueOf(rs.wasNull() || servings <= 0 ? 1 : servings));
                } catch (java.sql.SQLException e) {
                    throw new IllegalStateException(e);
                }
            });
            String aisleCode = rs.getString("aisle_code");
            Long aisleId = rs.getObject("aisle_category_id", Long.class);
            recipe.ingredients.add(new RecipeIngredientImportDto(
                    rs.getLong("ingredient_id"),
                    rs.getString("name"),
                    rs.getBigDecimal("quantity"),
                    rs.getLong("unit_id"),
                    rs.getString("unit_name"),
                    aisleId,
                    aisleCode != null ? aisleCode : aisleFromKind(rs.getString("kind"))));
        });
        return recipes;
    }

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

    private static final class RecipeRows {
        final String title;
        final BigDecimal baseServings;
        final List<RecipeIngredientImportDto> ingredients = new ArrayList<>();

        RecipeRows(String title, BigDecimal baseServings) {
            this.title = title;
            this.baseServings = baseServings;
        }
    }
}
