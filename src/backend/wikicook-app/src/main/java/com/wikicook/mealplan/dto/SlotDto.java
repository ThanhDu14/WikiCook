package com.wikicook.mealplan.dto;

import com.wikicook.mealplan.domain.Meal;

/** openapi {@code Slot}; {@code item = null} is an empty slot. */
public record SlotDto(Meal meal, PlanItemDto item) {
}
