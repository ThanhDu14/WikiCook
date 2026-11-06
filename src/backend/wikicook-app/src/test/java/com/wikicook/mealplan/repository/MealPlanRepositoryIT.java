package com.wikicook.mealplan.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.MealPlan;
import com.wikicook.mealplan.domain.MealPlanItem;
import com.wikicook.mealplan.support.MealPlanFixtures;
import com.wikicook.support.AbstractPostgresIT;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/** T009: lazy plan creation is safe under concurrency and reads and locks stay inside the owner's plans. */
@SpringBootTest
class MealPlanRepositoryIT extends AbstractPostgresIT {

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 19);

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MealPlanRepository plans;

    @Autowired
    MealPlanItemRepository items;

    @Autowired
    TransactionTemplate tx;

    private MealPlanFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new MealPlanFixtures(jdbc);
    }

    @Test
    void findOrCreateFromTwoThreadsCreatesOnePlan() throws Exception {
        long user = fixtures.user();
        CountDownLatch start = new CountDownLatch(1);
        CompletableFuture<MealPlan> first = CompletableFuture.supplyAsync(() -> awaitThen(start, user));
        CompletableFuture<MealPlan> second = CompletableFuture.supplyAsync(() -> awaitThen(start, user));

        start.countDown();

        assertThat(first.get().getId()).isEqualTo(second.get().getId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM meal_plans WHERE user_id = ?", Long.class, user))
                .isEqualTo(1);
    }

    private MealPlan awaitThen(CountDownLatch start, long user) {
        try {
            start.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return plans.findOrCreate(user, MONDAY);
    }

    @Test
    void findOrCreateReturnsTheExistingPlan() {
        long user = fixtures.user();
        long existing = fixtures.plan(user, MONDAY);

        assertThat(plans.findOrCreate(user, MONDAY).getId()).isEqualTo(existing);
    }

    @Test
    void findByUserIdAndWeekStartNeverReturnsAnotherUsersPlan() {
        long owner = fixtures.user();
        long other = fixtures.user();
        fixtures.plan(owner, MONDAY);

        assertThat(plans.findByUserIdAndWeekStart(owner, MONDAY)).isPresent();
        assertThat(plans.findByUserIdAndWeekStart(other, MONDAY)).isEmpty();
    }

    @Test
    void findMealReturnsItsDishesByPosition() {
        long user = fixtures.user();
        long plan = fixtures.plan(user, MONDAY);
        long second = fixtures.item(plan, DayOfWeek.MONDAY, "LUNCH", 1, null, "Cá kho", 3, "MANUAL");
        long first = fixtures.item(plan, DayOfWeek.MONDAY, "LUNCH", 0, null, "Canh chua", 2, "MANUAL");
        fixtures.item(plan, DayOfWeek.TUESDAY, "DINNER", 0, null, "Bún chả", 3, "MANUAL");

        assertThat(items.findByMealPlanId(plan)).hasSize(3);
        assertThat(items.findMeal(plan, DayOfWeek.MONDAY, Meal.LUNCH)).extracting(MealPlanItem::getId)
                .containsExactly(first, second);
        assertThat(items.findMeal(plan, DayOfWeek.MONDAY, Meal.DINNER)).isEmpty();
        assertThat(items.findByIdAndMealPlanId(second, plan)).get()
                .satisfies(item -> {
                    assertThat(item.getRecipeNameSnapshot()).isEqualTo("Cá kho");
                    assertThat(item.getServings()).isEqualTo((short) 3);
                    assertThat(item.getPosition()).isEqualTo(1);
                });
        assertThat(items.findByIdAndMealPlanId(second, fixtures.plan(user, MONDAY.plusWeeks(1)))).isEmpty();
    }

    @Test
    void lockByUserIdAndWeekStartFindsOnlyTheOwnersPlan() {
        long owner = fixtures.user();
        long plan = fixtures.plan(owner, MONDAY);

        long stranger = fixtures.user();
        Long locked = tx.execute(status -> plans.lockByUserIdAndWeekStart(owner, MONDAY).orElseThrow().getId());
        Boolean strangerHasNone = tx.execute(status -> plans.lockByUserIdAndWeekStart(stranger, MONDAY).isEmpty());
        Long created = tx.execute(status -> plans.findOrCreateLocked(stranger, MONDAY).getUserId());

        assertThat(locked).isEqualTo(plan);
        assertThat(strangerHasNone).isTrue();
        assertThat(created).isEqualTo(stranger);
    }

    @Test
    void deleteByMealPlanIdAndDayClearsOnlyThatDay() {
        long plan = fixtures.plan(fixtures.user(), MONDAY);
        fixtures.item(plan, DayOfWeek.MONDAY, "LUNCH", null, "A", 2);
        fixtures.item(plan, DayOfWeek.MONDAY, "DINNER", null, "B", 2);
        fixtures.item(plan, DayOfWeek.TUESDAY, "DINNER", null, "C", 2);

        Integer deleted = tx.execute(status -> items.deleteByMealPlanIdAndDay(plan, DayOfWeek.MONDAY));

        assertThat(deleted).isEqualTo(2);
        assertThat(items.findByMealPlanId(plan)).extracting(MealPlanItem::getRecipeNameSnapshot).containsExactly("C");
    }
}
