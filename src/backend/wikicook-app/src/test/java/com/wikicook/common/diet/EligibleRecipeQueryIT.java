package com.wikicook.common.diet;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.aiplanner.repository.EligibleRecipeQuery;
import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.support.AbstractPostgresIT;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** Step 1 of the pipeline (research R4): hard rules in SQL, so no unsafe recipe ever reaches the LLM (NFR-004). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class EligibleRecipeQueryIT extends AbstractPostgresIT {

    private static final Set<Meal> ALL_MEALS = EnumSet.allOf(Meal.class);

    @Autowired
    EligibleRecipeQuery query;

    @Autowired
    JdbcTemplate jdbc;

    private RecipeFixtures f;
    private long rice;
    private long tofu;

    @BeforeEach
    void seed() {
        RecipeFixtures.clear(jdbc);
        f = new RecipeFixtures(jdbc);
        rice = f.ingredient("Gạo", "PLANT");
        tofu = f.ingredient("Đậu hũ", "PLANT");
    }

    private Map<Meal, List<Long>> eligible(DietRestrictions restrictions, Integer maxCookTime) {
        return query.eligibleByMeal(ALL_MEALS, restrictions, maxCookTime);
    }

    private List<Long> lunch(DietRestrictions restrictions) {
        return eligible(restrictions, null).get(Meal.LUNCH);
    }

    @Test
    void allergenThroughIngredientAllergensExcludesRecipe() { // FR-009, US3-2
        long peanutAllergen = f.allergen("PEANUT");
        long peanut = f.ingredient("Đậu phộng", "PLANT");
        f.tagAllergen(peanut, peanutAllergen);
        long withPeanut = f.newRecipe("Xôi đậu phộng").ingredients(rice, peanut).create();
        long safe = f.newRecipe("Cơm đậu hũ").ingredients(rice, tofu).create();

        assertThat(lunch(new DietRestrictions(Set.of(peanutAllergen), Set.of(), DietType.NONE)))
                .containsExactly(safe);
        assertThat(lunch(DietRestrictions.none())).containsExactly(withPeanut, safe);
    }

    @Test
    void excludedIngredientExcludesRecipe() { // FR-009
        long coriander = f.ingredient("Rau mùi", "PLANT");
        f.newRecipe("Canh rau mùi").ingredients(coriander).create();
        long safe = f.newRecipe("Cơm đậu hũ").ingredients(rice, tofu).create();

        assertThat(lunch(new DietRestrictions(Set.of(), Set.of(coriander), DietType.NONE))).containsExactly(safe);
    }

    @Test
    void dietTypesFollowIngredientKind() { // FR-009a
        long pork = f.newRecipe("Thịt kho").ingredients(f.ingredient("Thịt heo", "MEAT")).create();
        long fishSauce = f.newRecipe("Rau muống luộc chấm nước mắm")
                .ingredients(f.ingredient("Rau muống", "PLANT"), f.ingredient("Nước mắm", "SEAFOOD")).create();
        long omelette = f.newRecipe("Trứng chiên").ingredients(f.ingredient("Trứng gà", "EGG")).create();
        long milk = f.newRecipe("Sữa chua").ingredients(f.ingredient("Sữa tươi", "DAIRY")).create();
        long honey = f.newRecipe("Chanh mật ong").ingredients(f.ingredient("Mật ong", "HONEY")).create();
        long plant = f.newRecipe("Cơm đậu hũ").ingredients(rice, tofu).create();

        assertThat(lunch(new DietRestrictions(Set.of(), Set.of(), DietType.VEGETARIAN)))
                .containsExactly(omelette, milk, honey, plant); // fish sauce counts as seafood
        assertThat(lunch(new DietRestrictions(Set.of(), Set.of(), DietType.VEGAN))).containsExactly(plant);
        assertThat(lunch(new DietRestrictions(Set.of(), Set.of(), DietType.PESCATARIAN)))
                .containsExactly(fishSauce, omelette, milk, honey, plant);
        assertThat(lunch(DietRestrictions.none())).contains(pork);
    }

    @Test
    void nutritionDietLabelsAreNotTrusted() { // FR-009a: label says VEGAN, ingredients say DAIRY
        long labelled = f.newRecipe("Bánh flan").ingredients(f.ingredient("Sữa đặc", "DAIRY")).create();
        f.dietLabels(labelled, "VEGAN", "VEGETARIAN");

        assertThat(lunch(new DietRestrictions(Set.of(), Set.of(), DietType.VEGAN))).doesNotContain(labelled);
    }

    @Test
    void onlyPublishedNonSnackRecipesWithinCookTime() { // FR-008a, US1-1
        long quick = f.newRecipe("Cơm chiên nhanh").minutes(5, 15).ingredients(rice).create();
        f.newRecipe("Bò kho lâu").minutes(30, 120).ingredients(rice).create();
        f.newRecipe("Bản nháp").status("DRAFT").create();
        f.newRecipe("Món đã lưu trữ").status("ARCHIVED").create();
        f.newRecipe("Bánh tráng trộn").snack().create();

        Map<Meal, List<Long>> result = eligible(DietRestrictions.none(), 30);

        assertThat(result.get(Meal.LUNCH)).containsExactly(quick);
        assertThat(result.get(Meal.DINNER)).containsExactly(quick);
    }

    @Test
    void groupsByMealFlags() { // US1-4, US4: counts per meal
        long breakfastOnly = f.newRecipe("Bánh mì ốp la").meals(true, false, false).create();
        long lunchDinner = f.newRecipe("Cơm gà").create();
        long allDay = f.newRecipe("Phở bò").meals(true, true, true).create();

        Map<Meal, List<Long>> result = query.eligibleByMeal(EnumSet.of(Meal.BREAKFAST, Meal.DINNER),
                DietRestrictions.none(), null);

        assertThat(result).containsOnlyKeys(Meal.BREAKFAST, Meal.DINNER);
        assertThat(result.get(Meal.BREAKFAST)).containsExactly(breakfastOnly, allDay);
        assertThat(result.get(Meal.DINNER)).containsExactly(lunchDinner, allDay);
    }
}
