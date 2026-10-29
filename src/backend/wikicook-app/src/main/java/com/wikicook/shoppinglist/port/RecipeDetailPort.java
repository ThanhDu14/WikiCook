package com.wikicook.shoppinglist.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Port to retrieve recipe details for ingredient aggregation (specs/040 integration.md 2.1).
 */
public interface RecipeDetailPort {

    Optional<RecipeImportDto> getRecipeForImport(Long recipeId);

    record RecipeImportDto(
        Long recipeId,
        String title,
        BigDecimal baseServings,
        List<RecipeIngredientImportDto> ingredients
    ) {}

    record RecipeIngredientImportDto(
        Long ingredientId,
        String ingredientName,
        BigDecimal rawQuantity,
        Long unitId,
        String unitName,
        Long defaultAisleCategoryId,
        String defaultAisleCode
    ) {}
}
