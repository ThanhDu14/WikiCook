package com.wikicook.mealplan.repository;

import com.wikicook.mealplan.domain.MealPlan;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/** Plans of a user; every lookup carries the user id, so nobody reads another user's plan (FR-006). */
public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {

    Optional<MealPlan> findByUserIdAndWeekStart(Long userId, LocalDate weekStart);

    /** Inserts the plan unless it exists; safe when two requests create the same week at once (research R3). */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO meal_plans (user_id, week_start) VALUES (:userId, :weekStart)
            ON CONFLICT (user_id, week_start) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") long userId, @Param("weekStart") LocalDate weekStart);

    @Transactional
    default MealPlan findOrCreate(long userId, LocalDate weekStart) {
        insertIfAbsent(userId, weekStart);
        return findByUserIdAndWeekStart(userId, weekStart).orElseThrow();
    }
}
