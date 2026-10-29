package com.wikicook.shoppinglist.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ShoppingListItemDto(
    UUID clientUuid,
    Long ingredientId,
    String displayName,
    BigDecimal displayQuantity,
    String displayUnit,
    BigDecimal normalizedQuantity,
    String normalizedUnit,
    Long aisleCategoryId,
    boolean isChecked,
    boolean isCleared,
    boolean isManuallyEdited,
    int sourceRecipeCount,
    List<ItemProvenanceDto> sources
) {}
