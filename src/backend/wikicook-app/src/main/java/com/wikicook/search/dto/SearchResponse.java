package com.wikicook.search.dto;

import com.wikicook.search.domain.SearchSort;
import java.util.List;

/** One page of results (020 contracts/openapi.yaml {@code SearchResponse}). */
public record SearchResponse(
        List<RecipeCardDto> items,
        int page,
        int pageSize,
        long totalItems,
        int totalPages,
        SearchSort sort,
        String keyword,
        boolean keywordTruncated,
        AppliedFiltersDto appliedFilters,
        DietaryNoticeDto dietary) {
}
