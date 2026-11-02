package com.wikicook.shoppinglist.dto;

import java.util.List;

public record ShoppingListResponse(
    Long id,
    String title,
    String status,
    int totalItems,
    int checkedItems,
    int clearedItems,
    List<AisleGroupDto> aisleGroups
) {}
