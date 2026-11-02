package com.wikicook.mealplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.dto.AddItemRequest;
import com.wikicook.mealplan.dto.MoveItemRequest;
import com.wikicook.mealplan.repository.MealPlanItemRepository;
import com.wikicook.mealplan.service.exception.MealFullException;
import com.wikicook.mealplan.service.exception.SlotChangedException;
import com.wikicook.mealplan.support.MealPlanFixtures;
import com.wikicook.support.AbstractPostgresIT;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** T113 (replaces T058): a move changes everything or nothing; a meal never gets a fifth dish (NFR-005, SC-005). */
@SpringBootTest
class ItemConcurrencyIT extends AbstractPostgresIT {

    private static final LocalDate WEEK = LocalDate.of(2026, 10, 19);

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MealPlanService service;

    @Autowired
    ItemMoveService moves;

    @MockitoSpyBean
    MealPlanItemRepository items;

    private MealPlanFixtures f;
    private long user;

    @BeforeEach
    void setUp() {
        f = new MealPlanFixtures(jdbc);
        user = f.user();
    }

    private List<Map<String, Object>> rows() {
        return jdbc.queryForList("""
                SELECT i.id, i.day_of_week, i.meal, i.position, i.recipe_name_snapshot, i.servings, i.version
                FROM meal_plan_items i JOIN meal_plans p ON p.id = i.meal_plan_id
                WHERE p.user_id = ? ORDER BY i.id
                """, user);
    }

    @Test
    void failureHalfwayThroughAMoveLeavesBothMealsAsTheyWere() {
        long plan = f.plan(user, WEEK);
        long canh = f.item(plan, DayOfWeek.MONDAY, "LUNCH", 0, null, "Canh chua", 2, "MANUAL");
        f.item(plan, DayOfWeek.MONDAY, "LUNCH", 1, null, "Cá kho", 2, "MANUAL");
        long bun = f.item(plan, DayOfWeek.TUESDAY, "DINNER", 0, null, "Bún chả", 2, "MANUAL");
        List<Map<String, Object>> before = rows();
        doThrow(new IllegalStateException("disk full")).when(items)
                .saveAndFlush(argThat(item -> item != null && item.getId() == bun));

        assertThatThrownBy(() -> moves.move(user, WEEK, canh,
                new MoveItemRequest(DayOfWeek.TUESDAY, Meal.DINNER, 0, 0)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(rows()).isEqualTo(before);
    }

    @Test
    void fiveAddsAtOnceGiveFourDishesAndOneMealFull() throws Exception { // FR-004, the plan does not exist yet
        long recipe = new RecipeFixtures(jdbc).publishedRecipe("Canh chua cá lóc", "Cá lóc");

        List<Object> outcomes = race(5, () -> service.addItem(user, WEEK, DayOfWeek.MONDAY, Meal.DINNER,
                new AddItemRequest(recipe, null, null)));

        assertThat(outcomes).filteredOn(MealFullException.class::isInstance).hasSize(1);
        assertThat(outcomes).filteredOn(o -> !(o instanceof Exception)).hasSize(4);
        assertThat(rows()).extracting(r -> ((Number) r.get("position")).intValue())
                .containsExactlyInAnyOrder(0, 1, 2, 3);
    }

    @Test
    void twoMovesOfTheSameDishAtOnceOneWinsAndNoDishIsLostOrDoubled() throws Exception {
        long plan = f.plan(user, WEEK);
        long canh = f.item(plan, DayOfWeek.MONDAY, "LUNCH", 0, null, "Canh chua", 2, "MANUAL");
        f.item(plan, DayOfWeek.TUESDAY, "DINNER", 0, null, "Bún chả", 2, "MANUAL");

        List<Object> outcomes = race(2, () -> moves.move(user, WEEK, canh,
                new MoveItemRequest(DayOfWeek.TUESDAY, Meal.DINNER, null, 0)));

        assertThat(outcomes).filteredOn(SlotChangedException.class::isInstance).hasSize(1);
        assertThat(rows()).extracting(r -> r.get("recipe_name_snapshot"))
                .containsExactlyInAnyOrder("Canh chua", "Bún chả");
        assertThat(jdbc.queryForList("""
                SELECT recipe_name_snapshot FROM meal_plan_items
                WHERE meal_plan_id = ? AND day_of_week = 2 AND meal = 'DINNER' ORDER BY position
                """, String.class, plan)).containsExactly("Bún chả", "Canh chua");
    }

    /** Runs {@code task} on {@code n} threads at the same moment; each result or exception is one outcome. */
    private static List<Object> race(int n, Callable<?> task) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(n);
        List<Future<Object>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < n; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        return task.call();
                    } catch (RuntimeException e) {
                        return e;
                    }
                }));
            }
            start.countDown();
            List<Object> outcomes = new ArrayList<>();
            for (Future<Object> future : futures) {
                outcomes.add(future.get());
            }
            return outcomes;
        } finally {
            pool.shutdown();
        }
    }
}
