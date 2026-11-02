package com.wikicook.mealplan.repository;

import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.MealPlanItem;
import jakarta.persistence.LockModeType;
import java.time.DayOfWeek;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Items of a plan; callers pass a plan they already loaded for the current user. */
public interface MealPlanItemRepository extends JpaRepository<MealPlanItem, Long> {

    List<MealPlanItem> findByMealPlanId(Long mealPlanId);

    @Query("SELECT i FROM MealPlanItem i WHERE i.mealPlan.id = :planId AND i.day = :day AND i.meal = :meal")
    Optional<MealPlanItem> findSlot(@Param("planId") Long planId, @Param("day") DayOfWeek day,
                                    @Param("meal") Meal meal);

    /** Id of the item in a slot, without loading it, so it can be locked first (research R5). */
    @Query("SELECT i.id FROM MealPlanItem i WHERE i.mealPlan.id = :planId AND i.day = :day AND i.meal = :meal")
    Optional<Long> findSlotItemId(@Param("planId") Long planId, @Param("day") DayOfWeek day,
                                  @Param("meal") Meal meal);

    /** {@code SELECT … FOR UPDATE} in id order, so two moves on the same items cannot deadlock (research R5). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM MealPlanItem i WHERE i.id IN :ids ORDER BY i.id")
    List<MealPlanItem> lockItemsByIdsOrdered(@Param("ids") Collection<Long> ids);

    @Modifying
    @Query("DELETE FROM MealPlanItem i WHERE i.mealPlan.id = :planId AND i.day = :day")
    int deleteByMealPlanIdAndDay(@Param("planId") Long planId, @Param("day") DayOfWeek day);
}
