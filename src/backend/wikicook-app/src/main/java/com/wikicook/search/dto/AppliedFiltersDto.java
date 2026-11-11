package com.wikicook.search.dto;

import com.wikicook.mealplan.domain.DietType;
import com.wikicook.search.domain.Difficulty;
import com.wikicook.search.domain.TimeRange;
import java.util.List;

/** Filters actually used after invalid values were dropped (020 FR-011, FR-024). */
public record AppliedFiltersDto(
        TimeRange time,
        List<TaxonomyOptionDto> cuisines,
        List<TaxonomyOptionDto> categories,
        List<Difficulty> difficulties,
        List<TaxonomyOptionDto> methods,
        DietType diet) {
}
