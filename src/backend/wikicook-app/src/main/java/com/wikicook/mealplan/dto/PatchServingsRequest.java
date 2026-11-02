package com.wikicook.mealplan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** New servings of the dish in a slot (FR-011). */
public record PatchServingsRequest(
        @NotNull @Min(1) @Max(20) Integer servings,
        @NotNull @Valid ExpectedSlotState expected) {
}
