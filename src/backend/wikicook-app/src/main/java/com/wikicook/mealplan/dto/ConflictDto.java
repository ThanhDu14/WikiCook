package com.wikicook.mealplan.dto;

/** One reason with the allergen or ingredient name to show, e.g. ALLERGEN "Đậu phộng". */
public record ConflictDto(ConflictType type, String label) {
}
