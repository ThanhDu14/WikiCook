package com.wikicook.mealplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.dto.ExpectedSlotState;
import com.wikicook.mealplan.dto.MoveRequest;
import com.wikicook.mealplan.repository.MealPlanItemRepository;
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

/** T058: a swap changes both slots or neither, also when two run at once (NFR-005, SC-005). */
@SpringBootTest
class SlotMoveAtomicityIT extends AbstractPostgresIT {

    private static final LocalDate WEEK = LocalDate.of(2026, 10, 19);

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    SlotMoveService moves;

    @MockitoSpyBean
    MealPlanItemRepository items;

    private long user;
    private long pho;
    private long ca;

    @BeforeEach
    void setUp() {
        MealPlanFixtures f = new MealPlanFixtures(jdbc);
        user = f.user();
        long plan = f.plan(user, WEEK);
        pho = f.item(plan, DayOfWeek.MONDAY, "LUNCH", null, "Phở bò", 2);
        ca = f.item(plan, DayOfWeek.TUESDAY, "DINNER", null, "Cá kho", 5);
    }

    private MoveRequest swap() {
        return new MoveRequest(new MoveRequest.From(DayOfWeek.MONDAY, Meal.LUNCH, pho, 0),
                new MoveRequest.To(DayOfWeek.TUESDAY, Meal.DINNER, new ExpectedSlotState(ca, 0)));
    }

    private List<Map<String, Object>> rows() {
        return jdbc.queryForList("""
                SELECT i.id, i.day_of_week, i.meal, i.recipe_name_snapshot, i.servings, i.version
                FROM meal_plan_items i JOIN meal_plans p ON p.id = i.meal_plan_id
                WHERE p.user_id = ? ORDER BY i.id
                """, user);
    }

    @Test
    void failureAfterTheFirstUpdateLeavesBothSlotsAsTheyWere() {
        List<Map<String, Object>> before = rows();
        doThrow(new IllegalStateException("disk full")).when(items)
                .saveAndFlush(argThat(item -> item != null && item.getId() == ca));

        assertThatThrownBy(() -> moves.move(user, WEEK, swap())).isInstanceOf(IllegalStateException.class);

        assertThat(rows()).isEqualTo(before);
    }

    @Test
    void twoSwapsAtOnceOneWinsAndNoDishIsLostOrDoubled() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> attempt = () -> {
            start.await();
            try {
                moves.move(user, WEEK, swap());
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            results.add(pool.submit(attempt));
            results.add(pool.submit(attempt));
            start.countDown();
            assertThat(List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(true, false);
        } finally {
            pool.shutdown();
        }

        assertThat(rows()).extracting(r -> r.get("recipe_name_snapshot"))
                .containsExactlyInAnyOrder("Phở bò", "Cá kho");
        assertThat(jdbc.queryForObject("SELECT recipe_name_snapshot FROM meal_plan_items WHERE id = ?", String.class,
                pho)).isEqualTo("Cá kho");
    }
}
