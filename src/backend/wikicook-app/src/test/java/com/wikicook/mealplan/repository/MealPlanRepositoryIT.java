package com.wikicook.mealplan.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.MealPlan;
import com.wikicook.mealplan.domain.MealPlanItem;
import com.wikicook.mealplan.support.MealPlanFixtures;
import com.wikicook.support.AbstractPostgresIT;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/** T009: lazy plan creation is safe under concurrency, reads stay inside the owner's plans, locks go by id. */
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
    void findsSlotsAndItemsOfAPlan() {
        long user = fixtures.user();
        long plan = fixtures.plan(user, MONDAY);
        long lunch = fixtures.item(plan, DayOfWeek.MONDAY, "LUNCH", null, "Phở bò", 2);
        fixtures.item(plan, DayOfWeek.TUESDAY, "DINNER", null, "Cá kho", 3);

        assertThat(items.findByMealPlanId(plan)).hasSize(2);
        assertThat(items.findSlot(plan, DayOfWeek.MONDAY, Meal.LUNCH)).get()
                .satisfies(item -> {
                    assertThat(item.getId()).isEqualTo(lunch);
                    assertThat(item.getRecipeNameSnapshot()).isEqualTo("Phở bò");
                    assertThat(item.getServings()).isEqualTo((short) 2);
                    assertThat(item.getDay()).isEqualTo(DayOfWeek.MONDAY);
                });
        assertThat(items.findSlot(plan, DayOfWeek.MONDAY, Meal.DINNER)).isEmpty();
    }

    @Test
    void lockItemsByIdsOrderedReturnsItemsInIdOrder() {
        long plan = fixtures.plan(fixtures.user(), MONDAY);
        long a = fixtures.item(plan, DayOfWeek.MONDAY, "LUNCH", null, "A", 2);
        long b = fixtures.item(plan, DayOfWeek.FRIDAY, "DINNER", null, "B", 2);
        long c = fixtures.item(plan, DayOfWeek.WEDNESDAY, "BREAKFAST", null, "C", 2);

        List<Long> ids = tx.execute(status -> items.lockItemsByIdsOrdered(List.of(c, a, b)).stream()
                .map(MealPlanItem::getId).toList());

        assertThat(ids).containsExactly(a, b, c);
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
