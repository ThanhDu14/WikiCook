package com.wikicook.mealplan.service;

import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.MealPlan;
import com.wikicook.mealplan.domain.MealPlanItem;
import com.wikicook.mealplan.domain.SlotKey;
import com.wikicook.mealplan.dto.MealItems;
import com.wikicook.mealplan.dto.MoveItemRequest;
import com.wikicook.mealplan.dto.MoveResult;
import com.wikicook.mealplan.dto.PlanItemDto;
import com.wikicook.mealplan.repository.MealPlanItemRepository;
import com.wikicook.mealplan.service.exception.MealFullException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moves a dish to another meal of the same week, or to another place in its own meal (US3, FR-014, FR-015). The
 * plan row is locked first, so nothing else writes the week meanwhile (research R5); positions are renumbered
 * inside the transaction and the deferred UNIQUE checks them at commit. One transaction: every dish ends up where
 * it should or none moves (NFR-005).
 */
@Service
public class ItemMoveService {

    private final MealPlanItemRepository items;
    private final MealPlanService week;
    private final Clock clock;

    public ItemMoveService(MealPlanItemRepository items, MealPlanService week, Clock clock) {
        this.items = items;
        this.week = week;
        this.clock = clock;
    }

    @Transactional
    public MoveResult move(long userId, LocalDate weekStart, long itemId, MoveItemRequest request) {
        MealPlan plan = week.lockedPlan(userId, weekStart);
        MealPlanItem item = week.seenItem(plan, itemId, request.version());
        SlotKey from = item.slot();
        SlotKey to = new SlotKey(request.day(), request.meal());
        boolean sameMeal = from.equals(to);

        List<MealPlanItem> source = new ArrayList<>(items.findMeal(plan.getId(), from.day(), from.meal()));
        source.remove(item);
        List<MealPlanItem> target = sameMeal ? source
                : new ArrayList<>(items.findMeal(plan.getId(), to.day(), to.meal()));
        if (!sameMeal && target.size() >= Meal.MAX_DISHES) {
            throw new MealFullException();
        }
        int at = request.position() == null ? target.size() : Math.min(request.position(), target.size());
        target.add(at, item);
        item.moveTo(to.day(), to.meal());

        Instant now = clock.instant();
        plan.touch(now);
        item.touch(now);
        List<List<MealPlanItem>> changed = sameMeal ? List.of(target) : List.of(source, target);
        for (List<MealPlanItem> meal : changed) {
            MealPlanService.renumber(meal);
            meal.forEach(items::saveAndFlush);
        }

        List<MealPlanItem> moved = new ArrayList<>(target);
        if (!sameMeal) {
            moved.addAll(source);
        }
        Map<MealPlanItem, PlanItemDto> dtos = week.toDtos(moved);
        List<MealItems> meals = new ArrayList<>();
        if (!sameMeal) {
            meals.add(new MealItems(from.day(), from.meal(), source.stream().map(dtos::get).toList()));
        }
        meals.add(new MealItems(to.day(), to.meal(), target.stream().map(dtos::get).toList()));
        return new MoveResult(List.copyOf(meals));
    }
}
