package com.wikicook.shoppinglist.service;

import com.wikicook.shoppinglist.domain.ShoppingList;
import com.wikicook.shoppinglist.domain.ShoppingListItem;
import com.wikicook.shoppinglist.domain.ShoppingListItemSource;
import com.wikicook.shoppinglist.domain.Unit;
import com.wikicook.shoppinglist.port.RecipeDetailPort.RecipeImportDto;
import com.wikicook.shoppinglist.port.RecipeDetailPort.RecipeIngredientImportDto;
import com.wikicook.shoppinglist.repository.UnitRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Domain service implementing recipe ingredient scaling, unit conversion, and deduplication (US2 FR-006 to FR-009).
 */
@Service
public class IngredientAggregationService {

    private static final int MAX_LIST_CAPACITY = 200;

    private final UnitConversionService unitConversionService;
    private final UnitRepository unitRepository;

    public IngredientAggregationService(UnitConversionService unitConversionService, UnitRepository unitRepository) {
        this.unitConversionService = unitConversionService;
        this.unitRepository = unitRepository;
    }

    public void aggregateRecipe(
            ShoppingList activeList,
            RecipeImportDto recipe,
            BigDecimal targetServings,
            Set<Long> excludedIngredientIds
    ) {
        if (targetServings == null || targetServings.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Target servings must be positive");
        }

        if (activeList == null || recipe == null) {
            return;
        }

        if (recipe.ingredients() == null) {
            return;
        }

        BigDecimal baseServings = recipe.baseServings() != null && recipe.baseServings().compareTo(BigDecimal.ZERO) > 0 ?
                recipe.baseServings() : BigDecimal.ONE;
        BigDecimal servings = targetServings;

        BigDecimal multiplier = servings.divide(baseServings, 4, RoundingMode.HALF_UP);
        Set<Long> excluded = excludedIngredientIds != null ? excludedIngredientIds : Set.of();

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
                        servings.intValue()
                );
                existingItem.addSource(source);
            } else {
                // Enforce capacity check before adding new distinct item
                if (activeList.getItems().size() >= MAX_LIST_CAPACITY) {
                    throw new CapacityExceededException("Shopping list capacity limit of " + MAX_LIST_CAPACITY + " items reached");
                }

                UnitQuantityResult normalized = normalizeQuantityAndUnit(scaledQty, ingredient.unitId(), ingredient.unitName());
                Long aisleId = resolveAisleCategoryId(ingredient.defaultAisleCode(), ingredient.defaultAisleCategoryId());

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
                        servings.intValue()
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
        return unitRepository.findById(id)
                .map(u -> u.getNameVi() != null ? u.getNameVi() : u.getCode())
                .orElse(null);
    }

    private Long getUnitIdForName(String name, Long fallbackId) {
        if (name == null || name.isBlank()) return fallbackId;
        String clean = name.trim();
        return unitRepository.findByCode(clean)
                .or(() -> unitRepository.findByNameVi(clean))
                .map(Unit::getId)
                .orElse(fallbackId);
    }

    // Precedence: defaultAisleCode -> defaultAisleCategoryId -> 6L (GENERAL fallback).
    // Note: AISLE_CODE_TO_ID provides reverse lookup mapped to V402 seed lookups until
    // a full AisleCategoryRepository is introduced by catalog module 060.
    private static final Map<String, Long> AISLE_CODE_TO_ID = Map.of(
            "PRODUCE", 1L,
            "MEAT_SEAFOOD", 2L,
            "DAIRY_EGGS", 3L,
            "PANTRY_SPICES", 4L,
            "FROZEN", 5L,
            "GENERAL", 6L
    );

    public Long resolveAisleCategoryId(String aisleCode, Long fallbackId) {
        if (aisleCode != null && !aisleCode.isBlank()) {
            Long mapped = AISLE_CODE_TO_ID.get(aisleCode.trim().toUpperCase(Locale.ROOT));
            if (mapped != null) {
                return mapped;
            }
        }
        if (fallbackId != null) {
            return fallbackId;
        }
        return 6L;
    }
}
