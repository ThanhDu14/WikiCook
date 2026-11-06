package com.wikicook.mealplan.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.common.diet.DietRestrictions;
import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.dto.ConflictDto;
import com.wikicook.mealplan.dto.ConflictType;
import com.wikicook.mealplan.dto.PickerPageDto;
import com.wikicook.mealplan.dto.PickerResultDto;
import com.wikicook.support.AbstractPostgresIT;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** T031: picker search and ranking on a 30-recipe seed (FR-007, FR-007a, FR-007b). */
@SpringBootTest
class RecipePickerQueryRepositoryIT extends AbstractPostgresIT {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    RecipePickerQueryRepository picker;

    private RecipeFixtures f;
    private long vietnamese;
    private long japanese;
    private long peanutAllergen;
    private long peanut;
    private long milk;
    private long fishSauce;
    private long trungChien;
    private final List<Long> breakfast = new ArrayList<>();
    private long archived;
    private long snack;

    @BeforeEach
    void seed() {
        RecipeFixtures.clear(jdbc);
        f = new RecipeFixtures(jdbc);
        vietnamese = f.cuisine("Việt Nam");
        japanese = f.cuisine("Nhật Bản");
        peanutAllergen = f.allergen("PEANUT");
        peanut = f.ingredient("Đậu phộng", "PLANT");
        f.tagAllergen(peanut, peanutAllergen);
        milk = f.ingredient("Sữa tươi", "DAIRY");
        fishSauce = f.ingredient("Nước mắm", "SEAFOOD");
        long rice = f.ingredient("Gạo", "PLANT");
        long egg = f.ingredient("Trứng gà", "EGG");

        trungChien = f.newRecipe("Trứng chiên").meals(true, true, true).cuisine(vietnamese).minutes(5, 5)
                .ingredients(egg).create();
        breakfast.add(trungChien);
        for (int i = 0; i < 9; i++) { // breakfast only, half Japanese, half long to cook
            breakfast.add(f.newRecipe("Bữa sáng %02d".formatted(i)).meals(true, false, false)
                    .cuisine(i % 2 == 0 ? vietnamese : japanese).minutes(5, i < 5 ? 10 : 60).ingredients(rice)
                    .create());
        }
        for (int i = 0; i < 17; i++) { // lunch and dinner
            f.newRecipe("Món trưa %02d".formatted(i)).cuisine(vietnamese).ingredients(rice).create();
        }
        f.newRecipe("Bánh mì trứng ốp la").meals(true, false, false).cuisine(vietnamese).ingredients(egg).create();
        f.newRecipe("Xôi đậu phộng").meals(true, false, false).cuisine(vietnamese).ingredients(rice, peanut).create();
        f.newRecipe("Cháo sữa").meals(true, false, false).cuisine(vietnamese).ingredients(rice, milk).create();
        f.newRecipe("Cơm chấm nước mắm").meals(true, false, false).cuisine(vietnamese).ingredients(rice, fishSauce)
                .create();
        archived = f.newRecipe("Trứng hấp đã lưu trữ").status("ARCHIVED").meals(true, false, false).ingredients(egg)
                .create();
        snack = f.newRecipe("Trứng cút lộn xào me").snack().ingredients(egg).create();
    }

    private static RecipePickerQueryRepository.Criteria criteria(Meal meal, String q) {
        return new RecipePickerQueryRepository.Criteria(meal, q, DietRestrictions.none(), Set.of(), null, null, 0, 20);
    }

    private static List<Long> ids(PickerPageDto page) {
        return page.content().stream().map(PickerResultDto::recipeId).toList();
    }

    @Test
    void findsTitlesWithoutAccents() { // FR-007
        PickerPageDto page = picker.search(criteria(Meal.BREAKFAST, "trung"));

        assertThat(page.content()).extracting(PickerResultDto::title).contains("Trứng chiên", "Bánh mì trứng ốp la")
                .doesNotContain("Trứng hấp đã lưu trữ", "Trứng cút lộn xào me");
    }

    @Test
    void mealSuitedDishesComeFirst() { // US1-3, SC-007
        PickerPageDto page = picker.search(criteria(Meal.BREAKFAST, null));

        assertThat(page.content().subList(0, 5)).allSatisfy(r -> assertThat(r.mealSuited()).isTrue());
        assertThat(page.content()).filteredOn(PickerResultDto::mealSuited).hasSize(14);
    }

    @Test
    void preferredCuisineThenMaxTimeWithinTheSuitedDishes() { // FR-007a
        var ranked = picker.search(new RecipePickerQueryRepository.Criteria(Meal.BREAKFAST, null,
                DietRestrictions.none(), Set.of(japanese), 30, null, 0, 20)).content();

        List<PickerResultDto> suited = ranked.stream().filter(PickerResultDto::mealSuited).toList();
        // Japanese dishes first (02, 04 … are Vietnamese: odd indexes are Japanese), short ones before long ones
        assertThat(suited.subList(0, 4)).allSatisfy(r -> assertThat(r.cuisineName()).isEqualTo("Nhật Bản"));
        assertThat(suited.subList(0, 2)).extracting(PickerResultDto::totalTimeMinutes)
                .allSatisfy(minutes -> assertThat(minutes).isLessThanOrEqualTo(30));
        assertThat(suited.get(4).cuisineName()).isEqualTo("Việt Nam");
        assertThat(suited.get(4).totalTimeMinutes()).isLessThanOrEqualTo(30);
    }

    @Test
    void conflictsGoLastWithTheirReasons() { // US1-4, FR-007b
        var restrictions = new DietRestrictions(Set.of(peanutAllergen), Set.of(milk), DietType.VEGETARIAN);
        List<PickerResultDto> ranked = picker.search(new RecipePickerQueryRepository.Criteria(Meal.BREAKFAST, null,
                restrictions, Set.of(), null, null, 0, 50)).content();

        int firstConflict = ranked.indexOf(ranked.stream().filter(r -> !r.conflicts().isEmpty()).findFirst()
                .orElseThrow());
        assertThat(ranked.subList(firstConflict, ranked.size())).allSatisfy(r -> assertThat(r.conflicts()).isNotEmpty());
        assertThat(conflictsOf(ranked, "Xôi đậu phộng")).containsExactly(new ConflictDto(ConflictType.ALLERGEN, "PEANUT"));
        assertThat(conflictsOf(ranked, "Cháo sữa"))
                .containsExactly(new ConflictDto(ConflictType.EXCLUDED_INGREDIENT, "Sữa tươi"));
        assertThat(conflictsOf(ranked, "Cơm chấm nước mắm"))
                .containsExactly(new ConflictDto(ConflictType.DIET_TYPE, "Nước mắm"));
    }

    @Test
    void veganConflictsWithDairyAndEggs() {
        List<PickerResultDto> ranked = picker.search(new RecipePickerQueryRepository.Criteria(Meal.BREAKFAST, null,
                new DietRestrictions(Set.of(), Set.of(), DietType.VEGAN), Set.of(), null, null, 0, 50)).content();

        assertThat(conflictsOf(ranked, "Cháo sữa")).containsExactly(new ConflictDto(ConflictType.DIET_TYPE, "Sữa tươi"));
        assertThat(conflictsOf(ranked, "Trứng chiên"))
                .containsExactly(new ConflictDto(ConflictType.DIET_TYPE, "Trứng gà"));
    }

    private static List<ConflictDto> conflictsOf(List<PickerResultDto> ranked, String title) {
        return ranked.stream().filter(r -> r.title().equals(title)).findFirst().orElseThrow().conflicts();
    }

    @Test
    void onlyPublishedNonSnackRecipes() { // US6-3, spec Assumptions
        List<Long> all = ids(picker.search(new RecipePickerQueryRepository.Criteria(Meal.BREAKFAST, null,
                DietRestrictions.none(), Set.of(), null, null, 0, 50)));

        assertThat(all).hasSize(31).doesNotContain(archived, snack);
    }

    @Test
    void collectionsScopeOnlyReturnsSavedRecipes() {
        PickerPageDto page = picker.search(new RecipePickerQueryRepository.Criteria(Meal.LUNCH, null,
                DietRestrictions.none(), Set.of(), null, Set.of(trungChien, archived), 0, 20));

        assertThat(ids(page)).containsExactly(trungChien);
        assertThat(picker.search(new RecipePickerQueryRepository.Criteria(Meal.LUNCH, null, DietRestrictions.none(),
                Set.of(), null, Set.of(), 0, 20)).content()).isEmpty();
    }

    @Test
    void pagesKnowWhetherThereIsMore() {
        var first = picker.search(new RecipePickerQueryRepository.Criteria(Meal.DINNER, null, DietRestrictions.none(),
                Set.of(), null, null, 0, 20));
        var second = picker.search(new RecipePickerQueryRepository.Criteria(Meal.DINNER, null, DietRestrictions.none(),
                Set.of(), null, null, 1, 20));

        assertThat(first.content()).hasSize(20);
        assertThat(first.hasNext()).isTrue();
        assertThat(second.content()).hasSize(11);
        assertThat(second.hasNext()).isFalse();
        assertThat(ids(first)).doesNotContainAnyElementsOf(ids(second));
        assertThat(second.page()).isEqualTo(1);
        assertThat(second.size()).isEqualTo(20);
    }

    @Test
    void searchTextIsAParameterNotSql() {
        assertThat(picker.search(criteria(Meal.BREAKFAST, "'; DROP TABLE recipes; --")).content()).isEmpty();
        assertThat(picker.search(criteria(Meal.BREAKFAST, "%")).content()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM recipes", Long.class)).isEqualTo(33);
    }
}
