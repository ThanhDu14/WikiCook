package com.wikicook.search.domain;

import com.wikicook.mealplan.domain.DietType;
import java.util.List;
import java.util.Set;

/**
 * One validated search (020 data-model §3). Built by {@code SearchCriteriaParser}: unknown values are already
 * dropped, {@code sort} is the effective order and {@code page} is at least 1.
 *
 * @param time null = any time
 * @param diet null = no diet filter; never taken from the profile (FR-022)
 */
public record SearchCriteria(
        Keyword keyword,
        TimeRange time,
        List<TaxonomyRef> cuisines,
        List<TaxonomyRef> categories,
        Set<Difficulty> difficulties,
        List<TaxonomyRef> methods,
        DietType diet,
        SearchSort sort,
        int page,
        boolean showHidden) {

    public static final int PAGE_SIZE = 20;

    public SearchCriteria {
        cuisines = List.copyOf(cuisines);
        categories = List.copyOf(categories);
        difficulties = Set.copyOf(difficulties);
        methods = List.copyOf(methods);
    }

    public SearchCriteria withPage(int newPage) {
        return new SearchCriteria(keyword, time, cuisines, categories, difficulties, methods, diet, sort, newPage,
                showHidden);
    }
}
