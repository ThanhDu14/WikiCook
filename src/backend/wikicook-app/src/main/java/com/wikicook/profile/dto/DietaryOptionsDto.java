package com.wikicook.profile.dto;

import com.wikicook.mealplan.domain.DietType;
import java.util.List;

/** openapi {@code DietaryOptions}: what the form offers; the client hard-codes none of it (FR-009, FR-012, FR-013). */
public record DietaryOptionsDto(List<AllergenDto> allergens, List<DietType> dietTypes, List<CuisineDto> cuisines) {
}
