package com.wikicook.shoppinglist.dto;

import java.util.List;
import java.util.Map;

public record ShoppingListResponse(
    Long id,
    String title,
    String status,
    int totalItems,
    int checkedItems,
    int clearedItems,
    List<AisleGroupDto> aisleGroups,
    /** Recipe id → cover photo, for the recipes the items came from that have one. */
    Map<Long, String> recipeCovers
) {}
