package com.wikicook.mealplan.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.mealplan.api.BatchSaveResult;
import com.wikicook.mealplan.api.DraftItem;
import com.wikicook.mealplan.api.MealPlanWriteFacade;
import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.domain.ItemSource;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.SlotKey;
import com.wikicook.mealplan.port.DietaryProfilePort;
import com.wikicook.mealplan.port.PlannerProfile;
import com.wikicook.mealplan.support.MealPlanFixtures;
import com.wikicook.support.AbstractPostgresIT;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/** T099: the facade 031 uses to read empty meals and save a whole draft (contracts/integration.md 2.2). */
@SpringBootTest
@Import(MealPlanWriteFacadeIT.Household.class)
class MealPlanWriteFacadeIT extends AbstractPostgresIT {

    /** 011 is not there yet: a profile whose household size the test sets. */
    @TestConfiguration(proxyBeanMethods = false)
    static class Household {

        static final AtomicReference<Integer> SIZE = new AtomicReference<>();

        @Bean
        @Primary
        DietaryProfilePort householdProfile() {
            return userId -> new PlannerProfile(SIZE.get(), DietType.NONE, null, Set.of(), Set.of(), Set.of());
        }
    }

    private static final LocalDate WEEK = LocalDate.of(2026, 10, 26);

    @Autowired
    MealPlanWriteFacade facade;

    @Autowired
    JdbcTemplate jdbc;

    private MealPlanFixtures plans;
    private RecipeFixtures recipes;
    private long user;
    private long canh;
    private long ca;
    private long rau;
    private long pho;

    @BeforeEach
    void setUp() {
        Household.SIZE.set(null);
        plans = new MealPlanFixtures(jdbc);
        recipes = new RecipeFixtures(jdbc);
        user = plans.user();
        canh = recipes.publishedRecipe("Canh chua cá lóc", "Cá lóc");
        ca = recipes.publishedRecipe("Cá kho tộ", "Cá basa");
        rau = recipes.publishedRecipe("Rau muống xào tỏi", "Rau muống");
        pho = recipes.publishedRecipe("Phở bò Hà Nội", "Bánh phở");
    }

    private static DraftItem ai(DayOfWeek day, Meal meal, long recipeId) {
        return new DraftItem(day, meal, recipeId, null, ItemSource.AI);
    }

    /** {@code recipe_id → position, servings, source} of one meal, by position. */
    private List<Map<String, Object>> meal(DayOfWeek day, Meal meal) {
        return jdbc.queryForList("""
                SELECT i.recipe_id, i.position, i.servings, i.source FROM meal_plan_items i
                JOIN meal_plans p ON p.id = i.meal_plan_id
                WHERE p.user_id = ? AND p.week_start = ? AND i.day_of_week = ? AND i.meal = ?
                ORDER BY i.position
                """, user, WEEK, day.getValue(), meal.name());
    }

    @Test
    void emptySlotsAreTheMealsWithoutAnyDish() {
        assertThat(facade.emptySlots(user, WEEK)).hasSize(21); // no plan yet

        long plan = plans.plan(user, WEEK);
        plans.item(plan, DayOfWeek.MONDAY, "LUNCH", canh, "Canh chua cá lóc", 2);
        plans.item(plan, DayOfWeek.MONDAY, "LUNCH", 1, ca, "Cá kho tộ", 2, "MANUAL");

        assertThat(facade.emptySlots(user, WEEK))
                .hasSize(20)
                .doesNotContain(new SlotKey(DayOfWeek.MONDAY, Meal.LUNCH))
                .first().isEqualTo(new SlotKey(DayOfWeek.MONDAY, Meal.BREAKFAST));
    }

    @Test
    void plannedRecipeIdsAreDistinctAndOnlyTheCallersWeek() {
        assertThat(facade.plannedRecipeIds(user, WEEK)).isEmpty();

        long plan = plans.plan(user, WEEK);
        plans.item(plan, DayOfWeek.MONDAY, "LUNCH", canh, "Canh chua cá lóc", 2);
        plans.item(plan, DayOfWeek.TUESDAY, "DINNER", canh, "Canh chua cá lóc", 2);
        plans.item(plan, DayOfWeek.FRIDAY, "BREAKFAST", pho, "Phở bò Hà Nội", 2);
        plans.item(plan, DayOfWeek.FRIDAY, "LUNCH", null, "Món đã bị xóa", 2);
        long other = plans.user();
        plans.item(plans.plan(other, WEEK), DayOfWeek.MONDAY, "LUNCH", rau, "Rau muống xào tỏi", 2);
        plans.item(plans.plan(user, WEEK.plusWeeks(1)), DayOfWeek.MONDAY, "LUNCH", ca, "Cá kho tộ", 2);

        assertThat(facade.plannedRecipeIds(user, WEEK)).containsExactlyInAnyOrder(canh, pho);
    }

    @Test
    void aWholeDraftIsSavedInOrderWithItsSource() { // 031 FR-007a, FR-018
        BatchSaveResult result = facade.saveDraft(user, WEEK, List.of(
                ai(DayOfWeek.MONDAY, Meal.BREAKFAST, pho),
                ai(DayOfWeek.MONDAY, Meal.DINNER, canh),
                new DraftItem(DayOfWeek.MONDAY, Meal.DINNER, ca, 4, ItemSource.AUTO),
                ai(DayOfWeek.MONDAY, Meal.DINNER, rau)));

        assertThat(result).isEqualTo(new BatchSaveResult(4, 0, 0));
        assertThat(meal(DayOfWeek.MONDAY, Meal.DINNER))
                .extracting(row -> row.get("recipe_id"), row -> row.get("position"), row -> row.get("source"))
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(canh, 0, "AI"),
                        org.assertj.core.groups.Tuple.tuple(ca, 1, "AUTO"),
                        org.assertj.core.groups.Tuple.tuple(rau, 2, "AI"));
        assertThat(meal(DayOfWeek.MONDAY, Meal.DINNER).get(1).get("servings")).isEqualTo(4);
        assertThat(facade.emptySlots(user, WEEK)).hasSize(19);
    }

    @Test
    void servingsDefaultToTheHouseholdThenTheRecipe() { // FR-012, research R9
        facade.saveDraft(user, WEEK, List.of(ai(DayOfWeek.MONDAY, Meal.LUNCH, canh)));
        Household.SIZE.set(5);
        facade.saveDraft(user, WEEK, List.of(ai(DayOfWeek.TUESDAY, Meal.LUNCH, canh)));

        assertThat(meal(DayOfWeek.MONDAY, Meal.LUNCH).getFirst().get("servings")).isEqualTo(2);
        assertThat(meal(DayOfWeek.TUESDAY, Meal.LUNCH).getFirst().get("servings")).isEqualTo(5);
    }

    @Test
    void aMealThatAlreadyHasADishIsSkippedAsAWholeGroup() { // 031 FR-019
        plans.item(plans.plan(user, WEEK), DayOfWeek.MONDAY, "DINNER", pho, "Phở bò Hà Nội", 2);

        BatchSaveResult result = facade.saveDraft(user, WEEK, List.of(
                ai(DayOfWeek.MONDAY, Meal.DINNER, canh),
                ai(DayOfWeek.MONDAY, Meal.DINNER, ca),
                ai(DayOfWeek.MONDAY, Meal.LUNCH, rau)));

        assertThat(result).isEqualTo(new BatchSaveResult(1, 2, 0));
        assertThat(meal(DayOfWeek.MONDAY, Meal.DINNER)).extracting(row -> row.get("recipe_id")).containsExactly(pho);
        assertThat(meal(DayOfWeek.MONDAY, Meal.LUNCH)).extracting(row -> row.get("recipe_id")).containsExactly(rau);
    }

    @Test
    void recipesNoLongerPublishedAreSkippedAndTheRestCloseUp() { // 031 FR-019
        long archived = recipes.recipe("Gà rang muối", "ARCHIVED", "Thịt gà");

        BatchSaveResult result = facade.saveDraft(user, WEEK, List.of(
                ai(DayOfWeek.MONDAY, Meal.DINNER, canh),
                ai(DayOfWeek.MONDAY, Meal.DINNER, archived),
                ai(DayOfWeek.MONDAY, Meal.DINNER, rau),
                ai(DayOfWeek.MONDAY, Meal.LUNCH, 999_999_999L)));

        assertThat(result).isEqualTo(new BatchSaveResult(2, 0, 2));
        assertThat(meal(DayOfWeek.MONDAY, Meal.DINNER))
                .extracting(row -> row.get("recipe_id"), row -> row.get("position"))
                .containsExactly(org.assertj.core.groups.Tuple.tuple(canh, 0),
                        org.assertj.core.groups.Tuple.tuple(rau, 1));
    }

    @Test
    void aMealTakesAtMostFourDishes() { // FR-004
        BatchSaveResult result = facade.saveDraft(user, WEEK, List.of(
                ai(DayOfWeek.MONDAY, Meal.DINNER, canh),
                ai(DayOfWeek.MONDAY, Meal.DINNER, ca),
                ai(DayOfWeek.MONDAY, Meal.DINNER, rau),
                ai(DayOfWeek.MONDAY, Meal.DINNER, pho),
                ai(DayOfWeek.MONDAY, Meal.DINNER, canh)));

        assertThat(result.saved()).isEqualTo(4);
        assertThat(meal(DayOfWeek.MONDAY, Meal.DINNER)).hasSize(4);
    }

    @Test
    void nothingToSaveCreatesNoPlan() {
        assertThat(facade.saveDraft(user, WEEK, List.of())).isEqualTo(new BatchSaveResult(0, 0, 0));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM meal_plans WHERE user_id = ?", Integer.class, user))
                .isZero();
    }
}
