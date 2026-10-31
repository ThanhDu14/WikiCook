package com.wikicook.shoppinglist.service;

import com.wikicook.shoppinglist.domain.ShoppingList;
import com.wikicook.shoppinglist.domain.ShoppingListItem;
import com.wikicook.shoppinglist.domain.ShoppingListItemSource;
import com.wikicook.shoppinglist.port.RecipeDetailPort.RecipeImportDto;
import com.wikicook.shoppinglist.port.RecipeDetailPort.RecipeIngredientImportDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Domain service implementing recipe ingredient scaling, unit conversion, and deduplication (US2 FR-006 to FR-009).
 */
@Service
public class IngredientAggregationService {

    private static final int MAX_LIST_CAPACITY = 200;

    private final UnitConversionService unitConversionService;

    public IngredientAggregationService(UnitConversionService unitConversionService) {
        this.unitConversionService = unitConversionService;
    }

    public void aggregateRecipe(
            ShoppingList activeList,
            RecipeImportDto recipe,
            BigDecimal targetServings,
            Set<Long> excludedIngredientIds
    ) {
        if (activeList == null || recipe == null) {
            return;
        }

        BigDecimal baseServings = recipe.baseServings() != null && recipe.baseServings().compareTo(BigDecimal.ZERO) > 0 ?
                recipe.baseServings() : BigDecimal.ONE;
        BigDecimal servings = targetServings != null && targetServings.compareTo(BigDecimal.ZERO) > 0 ?
                targetServings : baseServings;

        BigDecimal multiplier = servings.divide(baseServings, 4, RoundingMode.HALF_UP);

        for (RecipeIngredientImportDto ingredient : recipe.ingredients()) {
            if (ingredient.ingredientId() != null && excludedIngredientIds.contains(ingredient.ingredientId())) {
                continue;
            }

            BigDecimal rawQty = ingredient.rawQuantity() != null ? ingredient.rawQuantity() : BigDecimal.ONE;
            BigDecimal scaledQty = rawQty.multiply(multiplier);

            // Match existing item with same ingredientId and compatible unit
            Optional<ShoppingListItem> matchedItemOpt = activeList.getItems().stream()
                    .filter(item -> item.getIngredientId() != null
                            && item.getIngredientId().equals(ingredient.ingredientId())
                            && isUnitCompatible(item, ingredient))
                    .findFirst();

            if (matchedItemOpt.isPresent()) {
                ShoppingListItem existingItem = matchedItemOpt.get();
                UnitQuantityResult sumResult = sumQuantities(existingItem, ingredient, scaledQty);
                existingItem.setQuantity(sumResult.quantity());
                existingItem.setUnitId(sumResult.unitId());
                existingItem.setUnitName(sumResult.unitName());

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
                        targetServings != null ? targetServings.intValue() : 1
                );
                existingItem.addSource(source);
            } else {
                // Enforce capacity check before adding new distinct item
                if (activeList.getItems().size() >= MAX_LIST_CAPACITY) {
                    throw new CapacityExceededException("Shopping list capacity limit of " + MAX_LIST_CAPACITY + " items reached");
                }

                UnitQuantityResult normalized = normalizeQuantityAndUnit(scaledQty, ingredient.unitId(), ingredient.unitName());
                Long aisleId = ingredient.defaultAisleCategoryId() != null ? ingredient.defaultAisleCategoryId() : 6L;

                ShoppingListItem newItem = new ShoppingListItem(
                        activeList,
                        UUID.randomUUID(),
                        ingredient.ingredientId(),
                        ingredient.ingredientName(),
                        normalized.quantity(),
                        normalized.unitId(),
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
                        targetServings != null ? targetServings.intValue() : 1
                );
                newItem.addSource(source);

                activeList.addItem(newItem);
            }
        }
    }

    private boolean isUnitCompatible(ShoppingListItem item, RecipeIngredientImportDto ingredient) {
        Long u1 = item.getUnitId();
        Long u2 = ingredient.unitId();
        if (u1 != null && u2 != null && u1.equals(u2)) {
            return true;
        }
        String n1 = item.getUnitName() != null ? item.getUnitName() : getUnitNameForId(u1);
        String n2 = ingredient.unitName() != null ? ingredient.unitName() : getUnitNameForId(u2);
        return unitConversionService.areUnitsCompatible(n1, n2);
    }

    private record UnitQuantityResult(BigDecimal quantity, Long unitId, String unitName) {}

    private UnitQuantityResult sumQuantities(
            ShoppingListItem item,
            RecipeIngredientImportDto ingredient,
            BigDecimal scaledQty
    ) {
        String u1Name = item.getUnitName() != null ? item.getUnitName() : getUnitNameForId(item.getUnitId());
        String u2Name = ingredient.unitName() != null ? ingredient.unitName() : getUnitNameForId(ingredient.unitId());

        UnitConversionService.ConvertedQuantity converted = unitConversionService.sumAndNormalize(
                item.getQuantity(), u1Name, scaledQty, u2Name
        );

        Long fallbackId = ingredient.unitId() != null ? ingredient.unitId() : item.getUnitId();
        Long resolvedUnitId = getUnitIdForName(converted.unitName(), fallbackId);

        return new UnitQuantityResult(converted.quantity(), resolvedUnitId, converted.unitName());
    }

    private UnitQuantityResult normalizeQuantityAndUnit(BigDecimal qty, Long unitId, String rawUnit) {
        String unitName = rawUnit != null ? rawUnit : getUnitNameForId(unitId);
        UnitConversionService.ConvertedQuantity converted = unitConversionService.normalizeQuantityAndUnit(qty, unitName);
        Long resolvedUnitId = getUnitIdForName(converted.unitName(), unitId);
        return new UnitQuantityResult(converted.quantity(), resolvedUnitId, converted.unitName());
    }

    private String getUnitNameForId(Long id) {
        if (id == null) return null;
        if (id == 1L) return "g";
        if (id == 2L) return "kg";
        if (id == 3L) return "ml";
        if (id == 4L) return "l";
        if (id == 5L) return "quả";
        if (id == 6L) return "củ";
        if (id == 7L) return "cây";
        return null;
    }

    private Long getUnitIdForName(String name, Long fallbackId) {
        if (name == null) return fallbackId;
        String lower = name.trim().toLowerCase(Locale.ROOT);
        if ("g".equals(lower) || "gram".equals(lower) || "gr".equals(lower)) return 1L;
        if ("kg".equals(lower) || "kilogram".equals(lower)) return 2L;
        if ("ml".equals(lower) || "mililit".equals(lower) || "milliliter".equals(lower)) return 3L;
        if ("l".equals(lower) || "lit".equals(lower) || "lít".equals(lower) || "liter".equals(lower)) return 4L;
        if ("quả".equals(lower) || "qua".equals(lower) || "trứng".equals(lower) || "trái".equals(lower)) return 5L;
        if ("củ".equals(lower) || "cu".equals(lower)) return 6L;
        if ("cây".equals(lower) || "cay".equals(lower)) return 7L;
        return fallbackId;
    }
}
