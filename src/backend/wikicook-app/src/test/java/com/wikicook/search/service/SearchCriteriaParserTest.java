package com.wikicook.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.wikicook.mealplan.domain.DietType;
import com.wikicook.search.domain.Difficulty;
import com.wikicook.search.domain.SearchCriteria;
import com.wikicook.search.domain.SearchSort;
import com.wikicook.search.domain.TaxonomyRef;
import com.wikicook.search.domain.TimeRange;
import com.wikicook.search.repository.SearchOptionsRepository;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Invalid URL values are ignored, never rejected (020 FR-024, FR-017, research R4, R7). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchCriteriaParserTest {

    @Mock
    SearchOptionsRepository options;

    SearchCriteriaParser parser;

    static final TaxonomyRef VN = new TaxonomyRef(1, "viet-nam", "Việt Nam");
    static final TaxonomyRef JP = new TaxonomyRef(2, "nhat-ban", "Nhật Bản");
    static final TaxonomyRef SOUP = new TaxonomyRef(5, "canh-sup", "Canh / Súp");
    static final TaxonomyRef BOIL = new TaxonomyRef(9, "luoc", "Luộc");

    @BeforeEach
    void setUp() {
        when(options.activeCuisines()).thenReturn(List.of(VN, JP));
        when(options.activeCategories()).thenReturn(List.of(SOUP));
        when(options.activeMethods()).thenReturn(List.of(BOIL));
        parser = new SearchCriteriaParser(options);
    }

    private static RawSearchParams raw() {
        return new RawSearchParams(null, null, List.of(), List.of(), List.of(), List.of(), null, null, null, null);
    }

    @Test
    void emptyRequestBrowsesAllNewestFirstOnPageOne() {
        SearchCriteria c = parser.parse(raw());

        assertThat(c.keyword().isBlank()).isTrue();
        assertThat(c.sort()).isEqualTo(SearchSort.NEWEST);
        assertThat(c.page()).isEqualTo(1);
        assertThat(c.showHidden()).isFalse();
        assertThat(c.time()).isNull();
        assertThat(c.diet()).isNull();
    }

    @Test
    void keywordDefaultsToRelevance() {
        SearchCriteria c = parser.parse(raw().withQ("pho bo"));

        assertThat(c.sort()).isEqualTo(SearchSort.RELEVANCE);
    }

    @Test
    void relevanceWithoutKeywordFallsBackToNewest() {
        assertThat(parser.parse(raw().withSort("RELEVANCE")).sort()).isEqualTo(SearchSort.NEWEST);
    }

    @Test
    void enumsAreCaseInsensitiveAndUnknownValuesDropped() {
        SearchCriteria c = parser.parse(new RawSearchParams("ga", "le30", List.of(), List.of(),
                List.of("easy", "SUPER_HARD", "Hard"), List.of(), "vegan", "top_rated", "2", "true"));

        assertThat(c.time()).isEqualTo(TimeRange.LE30);
        assertThat(c.difficulties()).containsExactlyInAnyOrder(Difficulty.EASY, Difficulty.HARD);
        assertThat(c.diet()).isEqualTo(DietType.VEGAN);
        assertThat(c.sort()).isEqualTo(SearchSort.TOP_RATED);
        assertThat(c.page()).isEqualTo(2);
        assertThat(c.showHidden()).isTrue();
    }

    @Test
    void unknownEnumValuesBecomeDefaults() {
        SearchCriteria c = parser.parse(new RawSearchParams("ga", "LE45", List.of(), List.of(), List.of(), List.of(),
                "CARNIVORE", "RANDOM", "abc", "maybe"));

        assertThat(c.time()).isNull();
        assertThat(c.diet()).isNull();
        assertThat(c.sort()).isEqualTo(SearchSort.RELEVANCE);
        assertThat(c.page()).isEqualTo(1);
        assertThat(c.showHidden()).isFalse();
    }

    @Test
    void dietNoneIsNotAFilter() {
        assertThat(parser.parse(raw().withDiet("NONE")).diet()).isNull();
    }

    @Test
    void pageBelowOneBecomesOne() {
        assertThat(parser.parse(raw().withPage("0")).page()).isEqualTo(1);
        assertThat(parser.parse(raw().withPage("-3")).page()).isEqualTo(1);
    }

    @Test
    void slugsResolveToActiveRowsUnknownDroppedDuplicatesRemoved() {
        SearchCriteria c = parser.parse(new RawSearchParams(null, null,
                List.of("nhat-ban", "khong-ton-tai", "viet-nam", "nhat-ban"), List.of("canh-sup"),
                List.of(), List.of("luoc", "chien"), null, null, null, null));

        assertThat(c.cuisines()).containsExactly(JP, VN);
        assertThat(c.categories()).containsExactly(SOUP);
        assertThat(c.methods()).containsExactly(BOIL);
    }

    @Test
    void moreThanTwentyValuesPerGroupAreCut() {
        List<String> many = IntStream.range(0, 30).mapToObj(i -> i % 2 == 0 ? "viet-nam" : "x" + i).toList();
        List<String> slugs = new java.util.ArrayList<>(many);
        slugs.add("nhat-ban"); // position 31: beyond the first 20 values

        assertThat(parser.parse(raw().withCuisines(slugs)).cuisines()).containsExactly(VN);
    }

    @Test
    void nullListsAreTreatedAsEmpty() {
        SearchCriteria c = parser.parse(new RawSearchParams(null, null, null, null, null, null, null, null, null, null));

        assertThat(c.cuisines()).isEmpty();
        assertThat(c.difficulties()).isEmpty();
    }
}
