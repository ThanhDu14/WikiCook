package com.wikicook.common.diet;

import com.wikicook.mealplan.domain.DietType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One definition of "this recipe breaks the person's hard rules" for the AI planner (031, excludes), the manual
 * picker (030 T041, warns) and search (020): allergens through {@code ingredient_allergens}, excluded ingredients,
 * and the {@code ingredients.kind} values a diet forbids. Never reads {@code recipe_nutrition.diet_labels} (FR-009a).
 * Values go in as named parameters; nothing is concatenated into the SQL.
 */
public final class DietRuleSql {

    static final String ALLERGEN_IDS = "dietAllergenIds";
    static final String EXCLUDED_IDS = "dietExcludedIngredientIds";
    static final String FORBIDDEN_KINDS = "dietForbiddenKinds";

    private DietRuleSql() {
    }

    /** A SQL condition with the named parameters it uses. */
    public record SqlClause(String sql, Map<String, Object> params) {
    }

    /** {@code ingredients.kind} values a diet does not allow; fish sauce and shrimp paste are SEAFOOD. */
    public static Set<String> forbiddenKinds(DietType dietType) {
        return switch (dietType) {
            case NONE -> Set.of();
            case VEGETARIAN -> Set.of("MEAT", "SEAFOOD");
            case VEGAN -> Set.of("MEAT", "SEAFOOD", "EGG", "DAIRY", "HONEY");
            case PESCATARIAN -> Set.of("MEAT");
        };
    }

    /**
     * Condition that holds when the recipe whose id is {@code recipeIdColumn} (e.g. {@code r.id}) breaks none of
     * the rules; {@code TRUE} when there are no rules.
     */
    public static SqlClause notViolating(String recipeIdColumn, DietRestrictions restrictions) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();
        if (!restrictions.allergenIds().isEmpty()) {
            conditions.add("""
                    NOT EXISTS (SELECT 1 FROM recipe_ingredients dri
                                  JOIN ingredient_allergens dia ON dia.ingredient_id = dri.ingredient_id
                                 WHERE dri.recipe_id = %s AND dia.allergen_id IN (:%s))"""
                    .formatted(recipeIdColumn, ALLERGEN_IDS));
            params.put(ALLERGEN_IDS, restrictions.allergenIds());
        }
        if (!restrictions.excludedIngredientIds().isEmpty()) {
            conditions.add("""
                    NOT EXISTS (SELECT 1 FROM recipe_ingredients dri
                                 WHERE dri.recipe_id = %s AND dri.ingredient_id IN (:%s))"""
                    .formatted(recipeIdColumn, EXCLUDED_IDS));
            params.put(EXCLUDED_IDS, restrictions.excludedIngredientIds());
        }
        Set<String> kinds = forbiddenKinds(restrictions.dietType());
        if (!kinds.isEmpty()) {
            conditions.add("""
                    NOT EXISTS (SELECT 1 FROM recipe_ingredients dri
                                  JOIN ingredients di ON di.id = dri.ingredient_id
                                 WHERE dri.recipe_id = %s AND di.kind IN (:%s))"""
                    .formatted(recipeIdColumn, FORBIDDEN_KINDS));
            params.put(FORBIDDEN_KINDS, kinds);
        }
        return new SqlClause(conditions.isEmpty() ? "TRUE" : String.join("\n   AND ", conditions), params);
    }
}
