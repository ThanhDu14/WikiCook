package com.wikicook.mealplan.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.mealplan.support.MealPlanFixtures;
import com.wikicook.support.AbstractPostgresIT;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** T008: the V301 constraints and cascades of data-model.md section 1. */
@SpringBootTest
class MealPlanSchemaIT extends AbstractPostgresIT {

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 19);

    @Autowired
    JdbcTemplate jdbc;

    private MealPlanFixtures plans;
    private long user;
    private long plan;

    @BeforeEach
    void setUp() {
        plans = new MealPlanFixtures(jdbc);
        user = plans.user();
        plan = plans.plan(user, MONDAY);
    }

    @Test
    void onePlanPerUserAndWeek() {
        assertThatThrownBy(() -> plans.plan(user, MONDAY)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(plans.plan(user, MONDAY.plusWeeks(1))).isPositive();
    }

    @Test
    void weekStartIsAMonday() {
        assertThatThrownBy(() -> plans.plan(user, MONDAY.plusDays(1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }


    @Test
    void servingsMealAndSourceAreChecked() {
        assertThatThrownBy(() -> plans.item(plan, DayOfWeek.MONDAY, "LUNCH", null, "Phở", 0))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> plans.item(plan, DayOfWeek.MONDAY, "LUNCH", null, "Phở", 21))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> plans.item(plan, DayOfWeek.MONDAY, "SNACK", null, "Phở", 2))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> plans.item(plan, DayOfWeek.MONDAY, "LUNCH", null, "Phở", 2, "IMPORT"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO meal_plan_items (meal_plan_id, day_of_week, meal, recipe_name_snapshot, servings, source)
                VALUES (?, 8, 'LUNCH', 'Phở', 2, 'MANUAL')
                """, plan)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingTheUserDeletesThePlans() {
        plans.item(plan, DayOfWeek.MONDAY, "LUNCH", null, "Phở bò", 2);

        jdbc.update("DELETE FROM users WHERE id = ?", user);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM meal_plans WHERE id = ?", Long.class, plan)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM meal_plan_items WHERE meal_plan_id = ?", Long.class, plan))
                .isZero();
    }

    @Test
    void deletingTheRecipeKeepsTheItemWithItsName() {
        long recipe = new RecipeFixtures(jdbc).publishedRecipe("Canh chua cá lóc", "Cá lóc");
        long item = plans.item(plan, DayOfWeek.TUESDAY, "DINNER", recipe, "Canh chua cá lóc", 4);

        jdbc.update("DELETE FROM recipes WHERE id = ?", recipe);

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT recipe_id, recipe_name_snapshot FROM meal_plan_items WHERE id = ?", item);
        assertThat(row.get("recipe_id")).isNull();
        assertThat(row.get("recipe_name_snapshot")).isEqualTo("Canh chua cá lóc");
    }
}
