package com.wikicook.profile.dto;

/** openapi {@code PublicRecipeCard}: one published recipe of the person (FR-005). */
public record PublicRecipeCardDto(long id, String title, String coverImageUrl, Integer totalTimeMinutes, double ratingAvg,
        int reviewCount) {
}
