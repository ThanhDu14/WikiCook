package com.wikicook.search.service;

import com.wikicook.mealplan.domain.DietType;
import com.wikicook.search.domain.Difficulty;
import com.wikicook.search.domain.Keyword;
import com.wikicook.search.domain.SearchCriteria;
import com.wikicook.search.domain.SearchSort;
import com.wikicook.search.domain.TaxonomyRef;
import com.wikicook.search.domain.TimeRange;
import com.wikicook.search.repository.SearchOptionsRepository;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Turns URL values into {@link SearchCriteria}. Anything unknown or malformed is dropped or replaced by its default,
 * never rejected (020 FR-024): a shared link with a removed cuisine still opens.
 */
@Component
public class SearchCriteriaParser {

    /** Values kept per filter group; the rest is ignored. */
    static final int MAX_VALUES_PER_GROUP = 20;

    private final SearchOptionsRepository options;

    public SearchCriteriaParser(SearchOptionsRepository options) {
        this.options = options;
    }

    public SearchCriteria parse(RawSearchParams raw) {
        Keyword keyword = Keyword.of(raw.q());
        SearchSort sort = enumValue(SearchSort.class, raw.sort())
                .orElse(keyword.isBlank() ? SearchSort.NEWEST : SearchSort.RELEVANCE);
        if (sort == SearchSort.RELEVANCE && keyword.isBlank()) {
            sort = SearchSort.NEWEST;
        }
        DietType diet = enumValue(DietType.class, raw.diet()).filter(d -> d != DietType.NONE).orElse(null);
        Set<Difficulty> difficulties = EnumSet.noneOf(Difficulty.class);
        limit(raw.difficulty()).forEach(value -> enumValue(Difficulty.class, value).ifPresent(difficulties::add));

        return new SearchCriteria(
                keyword,
                enumValue(TimeRange.class, raw.time()).orElse(null),
                resolve(raw.cuisine(), options.activeCuisines()),
                resolve(raw.category(), options.activeCategories()),
                difficulties,
                resolve(raw.method(), options.activeMethods()),
                diet,
                sort,
                page(raw.page()),
                "true".equalsIgnoreCase(raw.showHidden()));
    }

    private static List<String> limit(List<String> values) {
        return values.stream().limit(MAX_VALUES_PER_GROUP).toList();
    }

    /** Active rows for the given slugs, in first-appearance order, without duplicates. */
    private static List<TaxonomyRef> resolve(List<String> slugs, List<TaxonomyRef> active) {
        if (slugs.isEmpty()) {
            return List.of();
        }
        Map<String, TaxonomyRef> bySlug = active.stream()
                .collect(Collectors.toMap(TaxonomyRef::slug, Function.identity(), (a, b) -> a));
        Set<TaxonomyRef> resolved = new LinkedHashSet<>();
        for (String slug : limit(slugs)) {
            if (slug != null) {
                TaxonomyRef ref = bySlug.get(slug.strip());
                if (ref != null) {
                    resolved.add(ref);
                }
            }
        }
        return List.copyOf(resolved);
    }

    private static int page(String value) {
        if (value == null) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(value.strip()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static <E extends Enum<E>> Optional<E> enumValue(Class<E> type, String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Enum.valueOf(type, value.strip().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
