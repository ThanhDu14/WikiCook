package com.wikicook.shoppinglist.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Port to retrieve weekly meal plan items for bulk shopping list generation (specs/040 integration.md 2.2).
 */
public interface MealPlanHandoffPort {

    Optional<MealPlanHandoffDto> getMealPlanForShoppingList(Long userId, Long mealPlanId);

    record MealPlanHandoffDto(
        Long mealPlanId,
        LocalDate weekStart,
        List<MealPlanItemHandoffDto> items
    ) {}

    record MealPlanItemHandoffDto(
        Long mealPlanItemId,
        Long recipeId,
        String recipeTitle,
        BigDecimal recipeBaseServings,
        BigDecimal effectiveServings,
        List<RecipeDetailPort.RecipeIngredientImportDto> rawIngredients
    ) {}
}
