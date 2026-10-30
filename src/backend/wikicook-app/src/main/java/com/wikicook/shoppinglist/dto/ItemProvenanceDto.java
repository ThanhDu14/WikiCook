package com.wikicook.shoppinglist.dto;

import java.math.BigDecimal;

public record ItemProvenanceDto(
    Long sourceId,
    Long recipeId,
    Long mealPlanItemId,
    String recipeTitleSnapshot,
    BigDecimal contributedQuantity,
    String unitName
) {}
