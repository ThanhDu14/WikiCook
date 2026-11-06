package com.wikicook.mealplan.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/** openapi {@code DayPlan}: three slots in BREAKFAST, LUNCH, DINNER order (FR-003); nutrition may be null. */
public record DayPlanDto(LocalDate date, DayOfWeek day, List<SlotDto> slots, DayNutritionDto nutrition) {
}
