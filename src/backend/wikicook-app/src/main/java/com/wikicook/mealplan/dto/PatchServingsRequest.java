package com.wikicook.mealplan.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** openapi {@code PatchServingsRequest}: new servings of one dish (FR-011). */
public record PatchServingsRequest(
        @NotNull @Min(1) @Max(20) Integer servings,
        @NotNull Integer version) {
}
