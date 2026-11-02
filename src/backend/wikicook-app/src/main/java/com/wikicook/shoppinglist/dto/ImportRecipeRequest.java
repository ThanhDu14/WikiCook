package com.wikicook.shoppinglist.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ImportRecipeRequest(
    @NotNull Long recipeId,
    @NotNull @DecimalMin("0.5") BigDecimal targetServings,
    List<Long> excludedIngredientIds
) {}
