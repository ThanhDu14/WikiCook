package com.wikicook.mealplan.dto;

import com.wikicook.mealplan.domain.ItemSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Add or replace the dish of a slot (FR-007, FR-008, FR-016).
 *
 * @param servings {@code null}: household size or recipe servings when adding (FR-012), the old value when replacing
 * @param source   {@code null} = MANUAL; AI when picked from 031's suggestions (031 FR-038)
 * @param expected {@code null} = the slot must be empty
 */
public record PutSlotRequest(
        @NotNull Long recipeId,
        @Min(1) @Max(20) Integer servings,
        ItemSource source,
        @Valid ExpectedSlotState expected) {
}
