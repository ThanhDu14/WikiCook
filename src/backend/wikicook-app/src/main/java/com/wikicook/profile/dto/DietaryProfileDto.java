package com.wikicook.profile.dto;

import com.wikicook.mealplan.domain.DietType;
import java.util.List;

/**
 * openapi {@code DietaryProfile}, level 3 of research R3: only the owner reads it. {@code empty} is derived (no column):
 * no allergen, exclusion or cuisine, {@code dietType} NONE, no household size and no time limit (FR-008).
 *
 * @param version {@code users.dietary_version}, sent back on save
 */
public record DietaryProfileDto(
        List<AllergenDto> allergens,
        List<IngredientSuggestionDto> excludedIngredients,
        DietType dietType,
        List<CuisineDto> preferredCuisines,
        Integer householdSize,
        Integer maxCookTimeMinutes,
        boolean empty,
        int version,
        List<DietaryNoticeDto> notices) {
}
