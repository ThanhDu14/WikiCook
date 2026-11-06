package com.wikicook.mealplan.service;

import com.wikicook.mealplan.api.HandoffItem;
import com.wikicook.mealplan.api.MealPlanShoppingFacade;
import com.wikicook.mealplan.api.ShoppingHandoff;
import com.wikicook.mealplan.domain.MealPlanItem;
import com.wikicook.mealplan.dto.PlanItemDto;
import com.wikicook.mealplan.repository.MealPlanItemRepository;
import com.wikicook.mealplan.repository.MealPlanRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hands the week to the Shopping List (US5, contracts/integration.md 2.1). "Available" is decided by
 * {@link MealPlanService#toDtos}, the same rule the planner shows, so the button and the list agree (FR-025).
 */
@Service
public class ShoppingHandoffService implements MealPlanShoppingFacade {

    private static final Comparator<MealPlanItem> WEEK_ORDER = Comparator
            .comparing(MealPlanItem::getDay)
            .thenComparing(MealPlanItem::getMeal)
            .thenComparingInt(MealPlanItem::getPosition);

    private final MealPlanRepository plans;
    private final MealPlanItemRepository items;
    private final MealPlanService weeks;

    public ShoppingHandoffService(MealPlanRepository plans, MealPlanItemRepository items, MealPlanService weeks) {
        this.plans = plans;
        this.items = items;
        this.weeks = weeks;
    }

    @Override
    @Transactional(readOnly = true)
    public ShoppingHandoff getHandoff(long userId, LocalDate weekStart) {
        List<MealPlanItem> weekItems = plans.findByUserIdAndWeekStart(userId, weekStart)
                .map(plan -> items.findByMealPlanId(plan.getId()))
                .orElse(List.of());
        Map<MealPlanItem, PlanItemDto> dtos = weeks.toDtos(weekItems);
        List<HandoffItem> handed = weekItems.stream()
                .filter(item -> dtos.get(item).available())
                .sorted(WEEK_ORDER)
                .map(item -> new HandoffItem(item.getId(), item.getRecipeId(), item.getServings(), item.getDay(),
                        item.getMeal()))
                .toList();
        return new ShoppingHandoff(weekStart, handed, weekItems.size() - handed.size());
    }
}
