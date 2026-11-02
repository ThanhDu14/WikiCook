package com.wikicook.mealplan.dto;

import com.wikicook.mealplan.domain.Meal;
import java.time.DayOfWeek;
import java.util.List;

/** openapi {@code MealItems}: the dishes of one meal by position, after a move. */
public record MealItems(DayOfWeek day, Meal meal, List<PlanItemDto> items) {
}
