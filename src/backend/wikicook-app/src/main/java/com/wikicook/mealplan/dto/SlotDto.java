package com.wikicook.mealplan.dto;

import com.wikicook.mealplan.domain.Meal;
import java.util.List;

/** openapi {@code Slot}: the 0–4 dishes of a meal by position; empty list = empty meal. */
public record SlotDto(Meal meal, List<PlanItemDto> items) {
}
