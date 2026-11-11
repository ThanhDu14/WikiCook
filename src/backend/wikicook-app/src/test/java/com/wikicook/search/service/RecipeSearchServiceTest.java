package com.wikicook.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wikicook.common.diet.DietRestrictions;
import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.port.DietaryProfilePort;
import com.wikicook.mealplan.port.PlannerProfile;
import com.wikicook.search.domain.Difficulty;
import com.wikicook.search.domain.Keyword;
import com.wikicook.search.domain.SearchCriteria;
import com.wikicook.search.domain.SearchSort;
import com.wikicook.search.domain.TaxonomyRef;
import com.wikicook.search.dto.ConflictDto;
import com.wikicook.search.dto.RecipeCardDto;
import com.wikicook.search.dto.SearchResponse;
import com.wikicook.search.repository.RecipeSearchQueryRepository;
import com.wikicook.search.repository.SearchOptionsRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Page clamping, echo and the dietary notice of spec 020 (FR-016, FR-019, FR-020, FR-022, AS 3.4). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecipeSearchServiceTest {

    @Mock
    RecipeSearchQueryRepository repository;

    @Mock
    DietaryProfilePort profiles;

    @Mock
    SearchOptionsRepository options;

    RecipeSearchService service;

    static final RecipeCardDto CARD = new RecipeCardDto(7, "Gỏi gà", null, 25, Difficulty.EASY, null, 0, "Việt Nam",
            List.of());

    @BeforeEach
    void setUp() {
        service = new RecipeSearchService(repository, options, profiles);
        when(repository.page(any(), any(), anyInt(), anyInt())).thenReturn(List.of(CARD));
        when(repository.page(any(), isNull(), anyInt(), anyInt())).thenReturn(List.of(CARD));
    }

    private static SearchCriteria criteria(String q, int page, boolean showHidden) {
        return new SearchCriteria(Keyword.of(q), null, List.of(new TaxonomyRef(1, "viet-nam", "Việt Nam")), List.of(),
                Set.of(Difficulty.EASY), List.of(), DietType.VEGETARIAN,
                q == null ? SearchSort.NEWEST : SearchSort.RELEVANCE, page, showHidden);
    }

    private static PlannerProfile profile(Set<Long> allergens, Set<Long> excluded) {
        return new PlannerProfile(null, DietType.VEGAN, null, allergens, excluded, Set.of());
    }

    @Test
    void clampsPageBeyondLast() {
        when(repository.count(any(), isNull())).thenReturn(45L);

        SearchResponse r = service.search(criteria("ga", 9, false), Optional.empty());

        assertThat(r.page()).isEqualTo(3);
        assertThat(r.totalPages()).isEqualTo(3);
        assertThat(r.totalItems()).isEqualTo(45);
        assertThat(r.pageSize()).isEqualTo(20);
        verify(repository).page(any(), isNull(), eq(40), eq(20));
    }

    @Test
    void zeroResultsIsPageOneOfZero() {
        when(repository.count(any(), isNull())).thenReturn(0L);

        SearchResponse r = service.search(criteria("xyz", 4, false), Optional.empty());

        assertThat(r.page()).isEqualTo(1);
        assertThat(r.totalPages()).isZero();
    }

    @Test
    void echoesKeywordSortAndAppliedFilters() {
        when(repository.count(any(), isNull())).thenReturn(1L);

        SearchResponse r = service.search(criteria("  gà  ", 1, false), Optional.empty());

        assertThat(r.keyword()).isEqualTo("gà");
        assertThat(r.keywordTruncated()).isFalse();
        assertThat(r.sort()).isEqualTo(SearchSort.RELEVANCE);
        assertThat(r.appliedFilters().cuisines()).extracting("slug").containsExactly("viet-nam");
        assertThat(r.appliedFilters().difficulties()).containsExactly(Difficulty.EASY);
        assertThat(r.appliedFilters().diet()).isEqualTo(DietType.VEGETARIAN);
        assertThat(r.appliedFilters().categories()).isEmpty();
    }

    @Test
    void guestHasNoProfile() {
        when(repository.count(any(), isNull())).thenReturn(5L);

        SearchResponse r = service.search(criteria("ga", 1, true), Optional.empty());

        assertThat(r.dietary().profileAvailable()).isFalse();
        assertThat(r.dietary().applied()).isFalse();
        assertThat(r.dietary().hiddenCount()).isZero();
        assertThat(r.dietary().showingHidden()).isFalse();
        verify(profiles, never()).findByUserId(anyLong());
    }

    private static long anyLong() {
        return org.mockito.ArgumentMatchers.anyLong();
    }

    @Test
    void emptyProfileIsNotApplied() {
        when(profiles.findByUserId(3)).thenReturn(profile(Set.of(), Set.of()));
        when(repository.count(any(), isNull())).thenReturn(5L);

        SearchResponse r = service.search(criteria("ga", 1, false), Optional.of(3L));

        assertThat(r.dietary().profileAvailable()).isTrue();
        assertThat(r.dietary().applied()).isFalse();
        assertThat(r.dietary().hiddenCount()).isZero();
        assertThat(r.totalItems()).isEqualTo(5);
    }

    @Test
    void reportsHiddenCountAndCountsOnlyVisibleRecipes() {
        when(profiles.findByUserId(3)).thenReturn(profile(Set.of(11L), Set.of(22L)));
        when(repository.count(any(), isNull())).thenReturn(48L);
        when(repository.count(any(), any(DietRestrictions.class))).thenReturn(42L);

        SearchResponse r = service.search(criteria("ga", 1, false), Optional.of(3L));

        assertThat(r.dietary().applied()).isTrue();
        assertThat(r.dietary().hiddenCount()).isEqualTo(6);
        assertThat(r.dietary().showingHidden()).isFalse();
        assertThat(r.totalItems()).isEqualTo(42);
        assertThat(r.items()).allSatisfy(card -> assertThat(card.conflicts()).isEmpty());
    }

    @Test
    void profileRuleNeverCarriesTheDietTypeOfTheProfile() {
        when(profiles.findByUserId(3)).thenReturn(profile(Set.of(11L), Set.of()));
        when(repository.count(any(), isNull())).thenReturn(10L);
        when(repository.count(any(), any(DietRestrictions.class))).thenReturn(10L);

        service.search(criteria("ga", 1, false), Optional.of(3L));

        verify(repository).count(any(), eq(new DietRestrictions(Set.of(11L), Set.of(), DietType.NONE)));
    }

    @Test
    void showHiddenSkipsRuleAndAddsConflicts() {
        when(profiles.findByUserId(3)).thenReturn(profile(Set.of(11L), Set.of(22L)));
        when(repository.count(any(), isNull())).thenReturn(48L);
        when(repository.count(any(), any(DietRestrictions.class))).thenReturn(44L);
        when(repository.conflicts(List.of(7L), Set.of(11L), Set.of(22L)))
                .thenReturn(Map.of(7L, List.of(new ConflictDto(ConflictDto.Type.ALLERGEN, "Đậu phộng"))));

        SearchResponse r = service.search(criteria("ga", 1, true), Optional.of(3L));

        assertThat(r.dietary().applied()).isFalse();
        assertThat(r.dietary().showingHidden()).isTrue();
        assertThat(r.dietary().hiddenCount()).isEqualTo(4);
        assertThat(r.totalItems()).isEqualTo(48);
        assertThat(r.items().getFirst().conflicts())
                .containsExactly(new ConflictDto(ConflictDto.Type.ALLERGEN, "Đậu phộng"));
        verify(repository).page(any(), isNull(), eq(0), eq(20));
    }

    @Test
    void allHiddenGivesEmptyPageWithHiddenCount() {
        when(profiles.findByUserId(3)).thenReturn(profile(Set.of(11L), Set.of()));
        when(repository.count(any(), isNull())).thenReturn(4L);
        when(repository.count(any(), any(DietRestrictions.class))).thenReturn(0L);
        when(repository.page(any(), any(DietRestrictions.class), anyInt(), anyInt())).thenReturn(List.of());

        SearchResponse r = service.search(criteria("goi ga", 1, false), Optional.of(3L));

        assertThat(r.items()).isEmpty();
        assertThat(r.totalItems()).isZero();
        assertThat(r.dietary().hiddenCount()).isEqualTo(4);
    }
}
