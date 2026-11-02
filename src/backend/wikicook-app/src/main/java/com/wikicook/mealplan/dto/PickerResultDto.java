package com.wikicook.mealplan.dto;

import java.util.List;

/** openapi {@code PickerResult}; {@code conflicts} is empty when the recipe fits the profile. */
public record PickerResultDto(long recipeId, String title, String coverImageUrl, Integer totalTimeMinutes,
                              String cuisineName, boolean mealSuited, List<ConflictDto> conflicts) {
}
