package com.wikicook.search.service;

import com.wikicook.common.diet.DietRestrictions;
import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.port.DietaryProfilePort;
import com.wikicook.mealplan.port.PlannerProfile;
import com.wikicook.search.domain.Difficulty;
import com.wikicook.search.domain.SearchCriteria;
import com.wikicook.search.domain.SearchSort;
import com.wikicook.search.domain.TimeRange;
import com.wikicook.search.dto.AppliedFiltersDto;
import com.wikicook.search.dto.ConflictDto;
import com.wikicook.search.dto.DietaryNoticeDto;
import com.wikicook.search.dto.FeaturedResponse;
import com.wikicook.search.dto.RecipeCardDto;
import com.wikicook.search.dto.SearchOptionsDto;
import com.wikicook.search.dto.SearchResponse;
import com.wikicook.search.dto.TaxonomyOptionDto;
import com.wikicook.search.repository.RecipeSearchQueryRepository;
import com.wikicook.search.repository.SearchOptionsRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs one search (020): counts, clamps the page, applies the viewer's dietary profile unless the per-search
 * override is on, and attaches conflict warnings to recipes shown through the override (research R5, R7).
 */
@Service
public class RecipeSearchService {

    static final int MAX_FEATURED = 20;

    private final RecipeSearchQueryRepository repository;
    private final SearchOptionsRepository options;
    private final DietaryProfilePort profiles;

    public RecipeSearchService(RecipeSearchQueryRepository repository, SearchOptionsRepository options,
            DietaryProfilePort profiles) {
        this.repository = repository;
        this.options = options;
        this.profiles = profiles;
    }

    /** {@code viewer} empty = guest: no profile, nothing hidden (AS 3.4). */
    @Transactional(readOnly = true)
    public SearchResponse search(SearchCriteria criteria, Optional<Long> viewer) {
        DietRestrictions rule = viewer.map(profiles::findByUserId).map(RecipeSearchService::hardRules).orElse(null);

        long all = repository.count(criteria, null);
        long conflicting = rule == null ? 0 : all - repository.count(criteria, rule);
        boolean applied = rule != null && !criteria.showHidden();
        boolean showingHidden = rule != null && criteria.showHidden();
        long totalItems = applied ? all - conflicting : all;

        int totalPages = (int) ((totalItems + SearchCriteria.PAGE_SIZE - 1) / SearchCriteria.PAGE_SIZE);
        int page = totalPages == 0 ? 1 : Math.min(criteria.page(), totalPages);
        List<RecipeCardDto> items = repository.page(criteria, applied ? rule : null,
                (page - 1) * SearchCriteria.PAGE_SIZE, SearchCriteria.PAGE_SIZE);
        if (showingHidden && !items.isEmpty()) {
            items = withConflicts(items, rule);
        }

        return new SearchResponse(
                items,
                page,
                SearchCriteria.PAGE_SIZE,
                totalItems,
                totalPages,
                criteria.sort(),
                criteria.keyword().text(),
                criteria.keyword().truncated(),
                applied(criteria),
                new DietaryNoticeDto(viewer.isPresent(), applied, conflicting, showingHidden));
    }

    /** Filter panel options (FR-009): active admin lists plus the fixed codes. */
    @Transactional(readOnly = true)
    public SearchOptionsDto options() {
        return new SearchOptionsDto(
                options.activeCuisines().stream().map(TaxonomyOptionDto::of).toList(),
                options.activeCategories().stream().map(TaxonomyOptionDto::of).toList(),
                options.activeMethods().stream().map(TaxonomyOptionDto::of).toList(),
                List.of(TimeRange.values()),
                List.of(Difficulty.values()),
                List.of(DietType.VEGETARIAN, DietType.VEGAN, DietType.PESCATARIAN),
                List.of(SearchSort.values()));
    }

    /** Home page cards, same for every viewer; {@code limit} clamped to 1..{@value #MAX_FEATURED}. */
    @Transactional(readOnly = true)
    public FeaturedResponse featured(int limit) {
        return new FeaturedResponse(repository.featured(Math.clamp(limit, 1, MAX_FEATURED)));
    }

    /**
     * Allergens and excluded ingredients only; diet type is never applied from the profile (FR-022). Null when the
     * profile has neither, so nothing is hidden and no notice is shown.
     */
    private static DietRestrictions hardRules(PlannerProfile profile) {
        if (profile.allergenIds().isEmpty() && profile.excludedIngredientIds().isEmpty()) {
            return null;
        }
        return new DietRestrictions(profile.allergenIds(), profile.excludedIngredientIds(), DietType.NONE);
    }

    private List<RecipeCardDto> withConflicts(List<RecipeCardDto> items, DietRestrictions rule) {
        Map<Long, List<ConflictDto>> conflicts = repository.conflicts(
                items.stream().map(RecipeCardDto::id).toList(), rule.allergenIds(), rule.excludedIngredientIds());
        return items.stream()
                .map(card -> card.withConflicts(conflicts.getOrDefault(card.id(), List.of())))
                .toList();
    }

    private static AppliedFiltersDto applied(SearchCriteria c) {
        return new AppliedFiltersDto(
                c.time(),
                c.cuisines().stream().map(TaxonomyOptionDto::of).toList(),
                c.categories().stream().map(TaxonomyOptionDto::of).toList(),
                c.difficulties().stream().sorted().toList(),
                c.methods().stream().map(TaxonomyOptionDto::of).toList(),
                c.diet());
    }
}
