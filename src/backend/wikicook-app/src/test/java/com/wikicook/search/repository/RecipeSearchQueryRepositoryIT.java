package com.wikicook.search.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.common.diet.DietRestrictions;
import com.wikicook.mealplan.domain.DietType;
import com.wikicook.search.domain.Difficulty;
import com.wikicook.search.domain.Keyword;
import com.wikicook.search.domain.SearchCriteria;
import com.wikicook.search.domain.SearchSort;
import com.wikicook.search.domain.TaxonomyRef;
import com.wikicook.search.domain.TimeRange;
import com.wikicook.search.dto.ConflictDto;
import com.wikicook.search.dto.RecipeCardDto;
import com.wikicook.search.support.SearchFixtures;
import com.wikicook.support.AbstractPostgresIT;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** The search query of spec 020: keyword, filters, dietary rule, sort (US1–US4). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RecipeSearchQueryRepositoryIT extends AbstractPostgresIT {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    RecipeSearchQueryRepository repository;

    SearchFixtures fx;

    @BeforeEach
    void setUp() {
        SearchFixtures.clear(jdbc);
        fx = new SearchFixtures(jdbc);
    }

    private static SearchCriteria criteria(String q, SearchSort sort) {
        return new SearchCriteria(Keyword.of(q), null, List.of(), List.of(), Set.of(), List.of(), null, sort, 1, false);
    }

    private static SearchCriteria keyword(String q) {
        return criteria(q, SearchSort.RELEVANCE);
    }

    private static SearchCriteria browse(SearchSort sort) {
        return criteria(null, sort);
    }

    private List<Long> ids(SearchCriteria c) {
        return ids(c, null);
    }

    private List<Long> ids(SearchCriteria c, DietRestrictions rule) {
        return repository.page(c, rule, 0, 50).stream().map(RecipeCardDto::id).toList();
    }

    // ---------------------------------------------------------------- US1 keyword

    @Test
    void neverReturnsUnpublished() {
        long published = fx.recipe("Phở bò").create();
        for (String status : List.of("DRAFT", "PENDING", "REJECTED", "REVISION_REQUESTED", "ARCHIVED")) {
            fx.recipe("Phở bò " + status).status(status).create();
        }

        assertThat(ids(keyword("pho"))).containsExactly(published);
        assertThat(repository.count(keyword("pho"), null)).isEqualTo(1);
        assertThat(repository.count(browse(SearchSort.NEWEST), null)).isEqualTo(1);
    }

    @Test
    void ignoresDiacriticsAndCase() {
        long pho = fx.recipe("Phở bò Hà Nội").create();
        fx.recipe("Bún chả").create();
        long tofu = fx.recipe("Đậu hũ non").create();

        assertThat(ids(keyword("pho bo"))).containsExactly(pho);
        assertThat(ids(keyword("PHỞ BÒ"))).containsExactly(pho);
        assertThat(ids(keyword("dau hu"))).containsExactly(tofu);
    }

    @Test
    void titleRanksAboveIngredient() {
        long chicken = fx.ingredient("Gà", "MEAT");
        long inIngredients = fx.recipe("Cháo hành").ingredients(chicken).publishedAt(OffsetDateTime.now()).create();
        long inTitle = fx.recipe("Gà luộc").publishedAt(OffsetDateTime.now().minusDays(5)).create();

        assertThat(ids(keyword("ga"))).containsExactly(inTitle, inIngredients);
    }

    @Test
    void matchesDescriptionIngredientsAndTags() {
        long tag = fx.tag("TAG", "Món nhậu", "mon-nhau");
        long viaDescription = fx.recipe("Món A").description("Ngon với bia").create();
        long viaIngredient = fx.recipe("Món B").ingredients(fx.ingredient("Bia", "OTHER")).create();
        long viaTag = fx.recipe("Món C").tags(tag).create();

        assertThat(ids(keyword("bia"))).containsExactlyInAnyOrder(viaDescription, viaIngredient);
        assertThat(ids(keyword("nhau"))).containsExactly(viaTag);
    }

    @Test
    void allWordsRankAboveSomeWords() {
        long some = fx.recipe("Canh cải").publishedAt(OffsetDateTime.now()).create();
        long all = fx.recipe("Canh chua cá lóc").publishedAt(OffsetDateTime.now().minusDays(3)).create();

        assertThat(ids(keyword("canh chua cá"))).containsExactly(all, some);
    }

    @Test
    void blankKeywordBrowsesAllNewestFirst() {
        long old = fx.recipe("Món cũ hơn").publishedAt(OffsetDateTime.now().minusDays(2)).create();
        long fresh = fx.recipe("Món mới hơn").publishedAt(OffsetDateTime.now()).create();

        assertThat(ids(browse(SearchSort.NEWEST))).containsExactly(fresh, old);
    }

    @Test
    void tieBreakPublishedAtThenIdDescending() {
        OffsetDateTime same = OffsetDateTime.now().minusHours(1);
        long first = fx.recipe("Món thứ một").publishedAt(same).create();
        long second = fx.recipe("Món thứ hai").publishedAt(same).create();

        assertThat(ids(browse(SearchSort.NEWEST))).containsExactly(second, first);
    }

    @Test
    void offsetAndLimitPage() {
        for (int i = 0; i < 5; i++) {
            fx.recipe("Món " + i).publishedAt(OffsetDateTime.now().minusMinutes(i)).create();
        }

        assertThat(repository.page(browse(SearchSort.NEWEST), null, 2, 2)).hasSize(2);
        assertThat(repository.page(browse(SearchSort.NEWEST), null, 4, 2)).hasSize(1);
    }

    @Test
    void cardFields() {
        long id = fx.recipe("Gà kho gừng").difficulty("MEDIUM").minutes(10, 15).rating(4.7, 88)
                .coverUrl("https://cdn.example.com/1.jpg").create();
        long noReviews = fx.recipe("Trứng chiên").coverKey("5b0c3c1e-7a1f-4a59-9a43-0c1f2b7d9e10.jpg").create();
        long noCover = fx.recipe("Cơm trắng").create();

        Map<Long, RecipeCardDto> cards = repository.page(browse(SearchSort.NEWEST), null, 0, 10).stream()
                .collect(java.util.stream.Collectors.toMap(RecipeCardDto::id, c -> c));

        RecipeCardDto card = cards.get(id);
        assertThat(card.title()).isEqualTo("Gà kho gừng");
        assertThat(card.totalTimeMinutes()).isEqualTo(25);
        assertThat(card.difficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(card.ratingAvg()).isEqualByComparingTo(new BigDecimal("4.70"));
        assertThat(card.reviewCount()).isEqualTo(88);
        assertThat(card.cuisine()).isEqualTo("Việt Nam");
        assertThat(card.coverImageUrl()).isEqualTo("https://cdn.example.com/1.jpg");
        assertThat(card.conflicts()).isEmpty();
        assertThat(cards.get(noReviews).ratingAvg()).isNull();
        assertThat(cards.get(noReviews).coverImageUrl())
                .isEqualTo("/api/v1/media/5b0c3c1e-7a1f-4a59-9a43-0c1f2b7d9e10.jpg");
        assertThat(cards.get(noCover).coverImageUrl()).isNull();
    }

    // ---------------------------------------------------------------- US2 filters

    @Test
    void filtersAndBetweenGroupsOrWithin() {
        long vnEasy = fx.recipe("Món VN dễ").cuisine(fx.vietnam).difficulty("EASY").create();
        long jpEasy = fx.recipe("Món JP dễ").cuisine(fx.japan).difficulty("EASY").create();
        fx.recipe("Món VN khó").cuisine(fx.vietnam).difficulty("HARD").create();
        long otherCuisine = fx.cuisine("Thái Lan", "thai-lan");
        fx.recipe("Món Thái dễ").cuisine(otherCuisine).difficulty("EASY").create();

        SearchCriteria c = new SearchCriteria(Keyword.of(null), null,
                List.of(new TaxonomyRef(fx.vietnam, "vn", "Việt Nam"), new TaxonomyRef(fx.japan, "jp", "Nhật Bản")),
                List.of(), Set.of(Difficulty.EASY), List.of(), null, SearchSort.NEWEST, 1, false);

        assertThat(ids(c)).containsExactlyInAnyOrder(vnEasy, jpEasy);
    }

    @Test
    void categoryFilter() {
        long soup = fx.recipe("Canh bí đỏ").category(fx.soup).create();
        fx.recipe("Cơm chiên").category(fx.mainDish).create();

        SearchCriteria c = new SearchCriteria(Keyword.of(null), null, List.of(),
                List.of(new TaxonomyRef(fx.soup, "s", "Canh")), Set.of(), List.of(), null, SearchSort.NEWEST, 1, false);

        assertThat(ids(c)).containsExactly(soup);
    }

    @Test
    void timeRanges() {
        long fifteen = fx.recipe("Món 15 phút").minutes(5, 10).create();
        long thirty = fx.recipe("Món 30 phút").minutes(10, 20).create();
        long sixty = fx.recipe("Món 60 phút").minutes(20, 40).create();
        long ninety = fx.recipe("Món 90 phút").minutes(30, 60).create();

        assertThat(ids(withTime(TimeRange.LE15))).containsExactly(fifteen);
        assertThat(ids(withTime(TimeRange.LE30))).containsExactlyInAnyOrder(fifteen, thirty);
        assertThat(ids(withTime(TimeRange.LE60))).containsExactlyInAnyOrder(fifteen, thirty, sixty);
        assertThat(ids(withTime(TimeRange.GT60))).containsExactly(ninety);
    }

    private static SearchCriteria withTime(TimeRange time) {
        return new SearchCriteria(Keyword.of(null), time, List.of(), List.of(), Set.of(), List.of(), null,
                SearchSort.NEWEST, 1, false);
    }

    @Test
    void methodFilterUsesMethodTags() {
        long boil = fx.tag("METHOD", "Luộc", "luoc");
        long fry = fx.tag("METHOD", "Chiên", "chien");
        long boiled = fx.recipe("Rau luộc").tags(boil).create();
        long fried = fx.recipe("Cá chiên").tags(fry).create();
        fx.recipe("Gỏi cuốn").create();

        SearchCriteria c = new SearchCriteria(Keyword.of(null), null, List.of(), List.of(), Set.of(),
                List.of(new TaxonomyRef(boil, "l", "Luộc"), new TaxonomyRef(fry, "c", "Chiên")), null,
                SearchSort.NEWEST, 1, false);

        assertThat(ids(c)).containsExactlyInAnyOrder(boiled, fried);
    }

    @Test
    void dietVegetarianExcludesMeatAndSeafoodKinds() {
        long veg = fx.recipe("Rau xào").ingredients(fx.ingredient("Cải ngọt", "PLANT")).create();
        fx.recipe("Bò xào").ingredients(fx.ingredient("Thịt bò", "MEAT")).create();
        fx.recipe("Rau chấm mắm").ingredients(fx.ingredient("Nước mắm", "SEAFOOD")).create();

        SearchCriteria c = new SearchCriteria(Keyword.of(null), null, List.of(), List.of(), Set.of(), List.of(),
                DietType.VEGETARIAN, SearchSort.NEWEST, 1, false);

        assertThat(ids(c)).containsExactly(veg);
    }

    // ---------------------------------------------------------------- US3 dietary rule

    @Test
    void hidesAllergenAndExcludedIngredient() {
        long peanutAllergen = fx.allergen("PEANUT", "Đậu phộng");
        long peanut = fx.ingredient("Đậu phộng rang", "PLANT");
        fx.linkAllergen(peanut, peanutAllergen);
        long coriander = fx.ingredient("Ngò rí", "PLANT");
        long safe = fx.recipe("Gà luộc").create();
        long withPeanut = fx.recipe("Gỏi gà").ingredients(peanut).create();
        long withCoriander = fx.recipe("Gà xé").ingredients(coriander).create();
        DietRestrictions rule = new DietRestrictions(Set.of(peanutAllergen), Set.of(coriander), DietType.NONE);

        assertThat(ids(keyword("ga"), rule)).containsExactly(safe);
        assertThat(repository.count(keyword("ga"), rule)).isEqualTo(1);
        assertThat(repository.count(keyword("ga"), null)).isEqualTo(3);

        Map<Long, List<ConflictDto>> conflicts = repository.conflicts(List.of(safe, withPeanut, withCoriander),
                Set.of(peanutAllergen), Set.of(coriander));
        assertThat(conflicts.get(withPeanut))
                .containsExactly(new ConflictDto(ConflictDto.Type.ALLERGEN, "Đậu phộng"));
        assertThat(conflicts.get(withCoriander))
                .containsExactly(new ConflictDto(ConflictDto.Type.EXCLUDED_INGREDIENT, "Ngò rí"));
        assertThat(conflicts).doesNotContainKey(safe);
    }

    @Test
    void conflictsAreDistinctAndSorted() {
        long peanutAllergen = fx.allergen("PEANUT", "Đậu phộng");
        long shellfish = fx.allergen("SHELLFISH", "Giáp xác");
        long peanut = fx.ingredient("Đậu phộng", "PLANT");
        long peanutOil = fx.ingredient("Dầu đậu phộng", "OTHER");
        long shrimp = fx.ingredient("Tôm", "SEAFOOD");
        fx.linkAllergen(peanut, peanutAllergen);
        fx.linkAllergen(peanutOil, peanutAllergen);
        fx.linkAllergen(shrimp, shellfish);
        long id = fx.recipe("Gỏi tôm").ingredients(shrimp, peanut, peanutOil).create();

        assertThat(repository.conflicts(List.of(id), Set.of(peanutAllergen, shellfish), Set.of()).get(id))
                .extracting(ConflictDto::name).containsExactly("Đậu phộng", "Giáp xác");
    }

    // ---------------------------------------------------------------- US4 sort

    @Test
    void quickestSort() {
        long slow = fx.recipe("Món nấu chậm").minutes(30, 60).create();
        long fast = fx.recipe("Nhanh").minutes(5, 5).create();

        assertThat(ids(browse(SearchSort.QUICKEST))).containsExactly(fast, slow);
    }

    @Test
    void topRatedNeedsThreeReviews() {
        long well = fx.recipe("Nhiều đánh giá").rating(4.5, 40).create();
        long single = fx.recipe("Một đánh giá").rating(5.0, 1).create();
        long none = fx.recipe("Chưa đánh giá").create();
        long better = fx.recipe("Tốt hơn").rating(4.8, 3).create();

        assertThat(ids(browse(SearchSort.TOP_RATED))).containsExactly(better, well, single, none);
    }

    @Test
    void featuredUsesPositionThenNewest() {
        long newest = fx.recipe("Mới nhất").publishedAt(OffsetDateTime.now()).create();
        long older = fx.recipe("Món cũ hơn").publishedAt(OffsetDateTime.now().minusDays(3)).create();
        long pinned = fx.recipe("Biên tập chọn").publishedAt(OffsetDateTime.now().minusDays(9)).create();
        jdbc.update("UPDATE recipes SET featured_position = 1 WHERE id = ?", pinned);
        fx.recipe("Nháp").status("DRAFT").create();

        assertThat(repository.featured(10)).extracting(RecipeCardDto::id).containsExactly(pinned, newest, older);
        assertThat(repository.featured(1)).extracting(RecipeCardDto::id).containsExactly(pinned);
    }
}
