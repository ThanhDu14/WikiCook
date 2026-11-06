package com.wikicook.mealplan.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wikicook.mealplan.support.MealPlanFixtures;
import com.wikicook.support.AbstractPostgresIT;
import java.time.DayOfWeek;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/** T110: V304 lets a meal hold up to 4 ordered dishes (FR-004, data-model 1). */
@SpringBootTest
class MealPlanMultiDishSchemaIT extends AbstractPostgresIT {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate tx;

    private MealPlanFixtures plans;
    private long plan;

    @BeforeEach
    void setUp() {
        plans = new MealPlanFixtures(jdbc);
        plan = plans.plan(plans.user(), LocalDate.of(2026, 10, 19));
    }

    private long dish(int position, String name) {
        return plans.item(plan, DayOfWeek.MONDAY, "LUNCH", position, null, name, 2, "MANUAL");
    }

    @Test
    void aMealHoldsFourDishesInOrder() {
        dish(0, "Canh chua");
        dish(1, "Cá kho");
        dish(2, "Rau muống xào");
        dish(3, "Trứng chiên");

        assertThat(jdbc.queryForList("""
                SELECT recipe_name_snapshot FROM meal_plan_items
                WHERE meal_plan_id = ? AND day_of_week = 1 AND meal = 'LUNCH' ORDER BY position
                """, String.class, plan)).containsExactly("Canh chua", "Cá kho", "Rau muống xào", "Trứng chiên");
    }

    @Test
    void positionIsZeroToThree() {
        assertThatThrownBy(() -> dish(4, "Món thứ năm")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> dish(-1, "Âm")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void twoDishesCannotShareAPositionOnceCommitted() {
        dish(0, "Canh chua");

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> dish(0, "Cá kho")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void dishesCanSwapPositionsInsideOneTransaction() { // deferred UNIQUE, research R5
        long canh = dish(0, "Canh chua");
        long ca = dish(1, "Cá kho");

        tx.executeWithoutResult(status -> {
            jdbc.update("UPDATE meal_plan_items SET position = 1 WHERE id = ?", canh);
            jdbc.update("UPDATE meal_plan_items SET position = 0 WHERE id = ?", ca);
        });

        assertThat(jdbc.queryForObject("SELECT position FROM meal_plan_items WHERE id = ?", Integer.class, ca))
                .isZero();
    }

    @Test
    void rowsWrittenWithoutAPositionAreTheFirstDish() { // V301 data keeps working after V304
        long old = jdbc.queryForObject("""
                INSERT INTO meal_plan_items (meal_plan_id, day_of_week, meal, recipe_name_snapshot, servings, source)
                VALUES (?, 1, 'LUNCH', 'Phở bò', 2, 'MANUAL') RETURNING id
                """, Long.class, plan);

        assertThat(jdbc.queryForObject("SELECT position FROM meal_plan_items WHERE id = ?", Integer.class, old))
                .isZero();
    }

    @Test
    void otherMealsAndDaysAreSeparate() {
        dish(0, "Canh chua");
        plans.item(plan, DayOfWeek.MONDAY, "DINNER", 0, null, "Cá kho", 2, "MANUAL");
        plans.item(plan, DayOfWeek.TUESDAY, "LUNCH", 0, null, "Bún chả", 2, "MANUAL");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM meal_plan_items WHERE meal_plan_id = ?", Long.class,
                plan)).isEqualTo(3);
    }
}
