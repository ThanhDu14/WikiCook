package com.wikicook.shoppinglist.dto;

import java.util.List;

public record AisleGroupDto(
    Long aisleId,
    String aisleCode,
    String aisleName,
    int displayOrder,
    List<ShoppingListItemDto> items
) {}
