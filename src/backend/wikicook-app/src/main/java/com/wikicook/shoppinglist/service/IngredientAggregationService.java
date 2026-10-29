package com.wikicook.shoppinglist.service;

import com.wikicook.shoppinglist.domain.ShoppingList;
import com.wikicook.shoppinglist.domain.ShoppingListItem;
import com.wikicook.shoppinglist.domain.ShoppingListItemSource;
import com.wikicook.shoppinglist.port.RecipeDetailPort.RecipeImportDto;
import com.wikicook.shoppinglist.port.RecipeDetailPort.RecipeIngredientImportDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Core engine for ingredient aggregation, scaling, deduplication, and provenance tracking (T043, US2).
 */
@Service
public class IngredientAggregationService {

    public static final int MAX_LIST_CAPACITY = 200;

    private final UnitConversionService unitConversionService;

    public IngredientAggregationService(UnitConversionService unitConversionService) {
        this.unitConversionService = unitConversionService;
    }

    public void aggregateRecipe(ShoppingList activeList, RecipeImportDto recipe, BigDecimal targetServings, Set<Long> excludedIngredientIds) {
        if (targetServings == null || targetServings.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Target servings must be positive");
        }

        BigDecimal baseServings = recipe.baseServings();
        if (baseServings == null || baseServings.compareTo(BigDecimal.ZERO) <= 0) {
            baseServings = BigDecimal.ONE;
        }

        BigDecimal multiplier = targetServings.divide(baseServings, 4, RoundingMode.HALF_UP);

        Set<Long> excluded = excludedIngredientIds != null ? excludedIngredientIds : Set.of();

        if (recipe.ingredients() == null) {
            return;
        }

        for (RecipeIngredientImportDto ingredient : recipe.ingredients()) {
            if (ingredient.ingredientId() != null && excluded.contains(ingredient.ingredientId())) {
                continue;
            }

            BigDecimal rawQty = ingredient.rawQuantity() != null ? ingredient.rawQuantity() : BigDecimal.ONE;
            BigDecimal scaledQty = rawQty.multiply(multiplier);

            // Match existing item with same ingredientId and compatible unit
            Optional<ShoppingListItem> matchedItemOpt = activeList.getItems().stream()
                    .filter(item -> item.getIngredientId() != null
                            && item.getIngredientId().equals(ingredient.ingredientId())
                            && unitConversionService.areUnitsCompatible(item.getUnitName(), ingredient.unitName()))
                    .findFirst();

            if (matchedItemOpt.isPresent()) {
                ShoppingListItem existingItem = matchedItemOpt.get();
                var sum = unitConversionService.sumAndNormalize(
                        existingItem.getQuantity(), existingItem.getUnitName(),
                        scaledQty, ingredient.unitName()
                );
                existingItem.setQuantity(sum.quantity());
                existingItem.setUnitName(sum.unitName());

                // FR-016: uncheck purchased item if additional quantity is merged
                existingItem.setChecked(false);
                if (existingItem.isCleared()) {
                    existingItem.setCleared(false);
                    existingItem.setClearedAt(null);
                }

                // Add provenance snapshot
                ShoppingListItemSource source = new ShoppingListItemSource(
                        existingItem,
                        recipe.recipeId(),
                        null,
                        recipe.title(),
                        scaledQty,
                        ingredient.unitId(),
                        ingredient.unitName(),
                        targetServings.intValue()
                );
                existingItem.addSource(source);
            } else {
                // Enforce capacity check before adding new distinct item
                if (activeList.getItems().size() >= MAX_LIST_CAPACITY) {
                    throw new CapacityExceededException("Shopping list capacity limit of " + MAX_LIST_CAPACITY + " items reached");
                }

                var normalized = unitConversionService.normalizeQuantityAndUnit(scaledQty, ingredient.unitName());
                Long aisleId = ingredient.defaultAisleCategoryId() != null ? ingredient.defaultAisleCategoryId() : 6L;

                ShoppingListItem newItem = new ShoppingListItem(
                        activeList,
                        UUID.randomUUID(),
                        ingredient.ingredientId(),
                        ingredient.ingredientName(),
                        normalized.quantity(),
                        ingredient.unitId(),
                        normalized.unitName(),
                        aisleId
                );

                ShoppingListItemSource source = new ShoppingListItemSource(
                        newItem,
                        recipe.recipeId(),
                        null,
                        recipe.title(),
                        scaledQty,
                        ingredient.unitId(),
                        ingredient.unitName(),
                        targetServings.intValue()
                );
                newItem.addSource(source);

                activeList.addItem(newItem);
            }
        }
    }
}
