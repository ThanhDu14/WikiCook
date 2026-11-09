package com.wikicook.search.dto;

import com.wikicook.mealplan.domain.DietType;
import com.wikicook.search.domain.Difficulty;
import com.wikicook.search.domain.SearchSort;
import com.wikicook.search.domain.TimeRange;
import java.util.List;

/** Filter options (020 FR-009); labels of the fixed codes live in the UI string store. */
public record SearchOptionsDto(
        List<TaxonomyOptionDto> cuisines,
        List<TaxonomyOptionDto> categories,
        List<TaxonomyOptionDto> methods,
        List<TimeRange> timeRanges,
        List<Difficulty> difficulties,
        List<DietType> diets,
        List<SearchSort> sorts) {
}
