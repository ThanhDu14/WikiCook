package com.wikicook.search.dto;

import java.util.List;

/** Home page "Nổi bật tuần này" (SR-01). */
public record FeaturedResponse(List<RecipeCardDto> items) {
}
