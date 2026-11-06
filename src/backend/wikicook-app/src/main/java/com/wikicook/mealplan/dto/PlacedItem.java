package com.wikicook.mealplan.dto;

import com.wikicook.mealplan.domain.Meal;
import java.time.DayOfWeek;

/** openapi {@code PlacedItem}: a dish and the meal it is in. */
public record PlacedItem(DayOfWeek day, Meal meal, PlanItemDto item) {
}
