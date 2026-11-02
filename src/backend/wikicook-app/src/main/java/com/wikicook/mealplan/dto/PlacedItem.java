package com.wikicook.mealplan.dto;

import com.wikicook.mealplan.domain.Meal;
import java.time.DayOfWeek;

/** An item and the slot it is in after a move. */
public record PlacedItem(DayOfWeek day, Meal meal, PlanItemDto item) {
}
