package com.wikicook.profile.dto;

import java.util.List;

/** openapi {@code PublicRecipePage}: one page of {@code size} (12 by default); {@code items} is empty when there is none. */
public record PublicRecipePageDto(List<PublicRecipeCardDto> items, int page, int size, long totalItems) {
}
