package com.wikicook.shoppinglist.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Port to retrieve weekly meal plan items for bulk shopping list generation (specs/040 integration.md 2.2).
 * The week is addressed by its Monday, as the planner (030) does; empty when the week has no available dish.
 */
public interface MealPlanHandoffPort {

    Optional<MealPlanHandoffDto> getMealPlanForShoppingList(Long userId, LocalDate weekStart);

    /**
     * @param skippedUnavailable dishes of the week left out because their recipe is no longer available (030 FR-025)
     */
    record MealPlanHandoffDto(
        LocalDate weekStart,
        List<MealPlanItemHandoffDto> items,
        int skippedUnavailable
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
