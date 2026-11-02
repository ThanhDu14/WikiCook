package com.wikicook.mealplan.service;

import com.wikicook.mealplan.domain.ItemSource;
import com.wikicook.mealplan.domain.MealPlan;
import com.wikicook.mealplan.domain.MealPlanItem;
import com.wikicook.mealplan.domain.SlotKey;
import com.wikicook.mealplan.dto.ExpectedSlotState;
import com.wikicook.mealplan.dto.MoveRequest;
import com.wikicook.mealplan.dto.MoveResult;
import com.wikicook.mealplan.dto.PlacedItem;
import com.wikicook.mealplan.dto.PlanItemDto;
import com.wikicook.mealplan.repository.MealPlanItemRepository;
import com.wikicook.mealplan.repository.MealPlanRepository;
import com.wikicook.mealplan.service.exception.SlotChangedException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moves a dish to another slot of the same week, swapping when that slot has one (US3-1, US3-2). The items are
 * locked in id order before anything is checked, then both slots are checked against what the client saw; a swap
 * exchanges the dishes of the two rows so ids stay where they are (research R5). One transaction: both slots
 * change or neither does (NFR-005).
 */
@Service
public class SlotMoveService {

    private final MealPlanRepository plans;
    private final MealPlanItemRepository items;
    private final MealPlanService dtos;
    private final Clock clock;

    public SlotMoveService(MealPlanRepository plans, MealPlanItemRepository items, MealPlanService dtos, Clock clock) {
        this.plans = plans;
        this.items = items;
        this.dtos = dtos;
        this.clock = clock;
    }

    @Transactional
    public MoveResult move(long userId, LocalDate weekStart, MoveRequest request) {
        MoveRequest.From from = request.from();
        MoveRequest.To to = request.to();
        MealPlan plan = plans.findByUserIdAndWeekStart(userId, weekStart).orElseThrow(SlotChangedException::new);
        SlotKey fromSlot = new SlotKey(from.day(), from.meal());
        SlotKey toSlot = new SlotKey(to.day(), to.meal());
        Optional<Long> targetId = fromSlot.equals(toSlot) ? Optional.empty()
                : items.findSlotItemId(plan.getId(), to.day(), to.meal());

        TreeSet<Long> ids = new TreeSet<>(List.of(from.itemId()));
        targetId.ifPresent(ids::add);
        Map<Long, MealPlanItem> locked = items.lockItemsByIdsOrdered(ids).stream()
                .collect(Collectors.toMap(MealPlanItem::getId, item -> item));

        MealPlanItem source = locked.get(from.itemId());
        if (source == null || !Objects.equals(source.getMealPlan().getId(), plan.getId())
                || !source.slot().equals(fromSlot) || source.getVersion() != from.version()) {
            throw new SlotChangedException();
        }
        if (fromSlot.equals(toSlot)) {
            return new MoveResult(MoveResult.Kind.MOVED, List.of(placed(source)));
        }
        MealPlanItem target = targetId.map(locked::get).orElse(null);
        if (target == null) {
            if (to.expected() != null) {
                throw new SlotChangedException();
            }
            return moveToEmpty(plan, source, toSlot);
        }
        requireSeen(target, toSlot, to.expected());
        return swap(plan, source, target);
    }

    private static void requireSeen(MealPlanItem target, SlotKey slot, ExpectedSlotState expected) {
        if (!target.slot().equals(slot)) {
            throw new SlotChangedException();
        }
        MealPlanService.requireExpected(target, expected);
    }

    private MoveResult moveToEmpty(MealPlan plan, MealPlanItem source, SlotKey to) {
        Instant now = touch(plan);
        source.moveTo(to.day(), to.meal());
        source.touch(now);
        MealPlanItem saved = items.saveAndFlush(source);
        return new MoveResult(MoveResult.Kind.MOVED, List.of(placed(saved)));
    }

    /** Exchanges recipe, name, servings and source of the two rows; each keeps its id and slot. */
    private MoveResult swap(MealPlan plan, MealPlanItem source, MealPlanItem target) {
        Instant now = touch(plan);
        Long recipeId = source.getRecipeId();
        String name = source.getRecipeNameSnapshot();
        int servings = source.getServings();
        ItemSource origin = source.getSource();

        source.setDish(target.getRecipeId(), target.getRecipeNameSnapshot(), target.getServings(),
                target.getSource());
        source.touch(now);
        MealPlanItem first = items.saveAndFlush(source);
        target.setDish(recipeId, name, servings, origin);
        target.touch(now);
        MealPlanItem second = items.saveAndFlush(target);
        Map<MealPlanItem, PlanItemDto> placed = dtos.toDtos(List.of(first, second));
        return new MoveResult(MoveResult.Kind.SWAPPED, List.of(
                new PlacedItem(first.getDay(), first.getMeal(), placed.get(first)),
                new PlacedItem(second.getDay(), second.getMeal(), placed.get(second))));
    }

    private Instant touch(MealPlan plan) {
        Instant now = clock.instant();
        plan.touch(now);
        return now;
    }

    private PlacedItem placed(MealPlanItem item) {
        return new PlacedItem(item.getDay(), item.getMeal(), dtos.toDto(item));
    }
}
