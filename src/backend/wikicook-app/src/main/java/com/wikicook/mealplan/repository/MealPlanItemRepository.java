package com.wikicook.mealplan.repository;

import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.MealPlanItem;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Items of a plan; callers pass a plan they already loaded for the current user. Writers lock that plan first
 * ({@link MealPlanRepository#lockByUserIdAndWeekStart}), so reading a meal here sees no concurrent change.
 */
public interface MealPlanItemRepository extends JpaRepository<MealPlanItem, Long> {

    List<MealPlanItem> findByMealPlanId(Long mealPlanId);

    /** The dishes of one meal by position (FR-004). */
    @Query("""
            SELECT i FROM MealPlanItem i
            WHERE i.mealPlan.id = :planId AND i.day = :day AND i.meal = :meal
            ORDER BY i.position
            """)
    List<MealPlanItem> findMeal(@Param("planId") Long planId, @Param("day") DayOfWeek day,
                                @Param("meal") Meal meal);

    /** An item of this plan only: an id from another week or user is not found (FR-006). */
    Optional<MealPlanItem> findByIdAndMealPlanId(Long id, Long mealPlanId);

    @Modifying
    @Query("DELETE FROM MealPlanItem i WHERE i.mealPlan.id = :planId AND i.day = :day")
    int deleteByMealPlanIdAndDay(@Param("planId") Long planId, @Param("day") DayOfWeek day);
}
