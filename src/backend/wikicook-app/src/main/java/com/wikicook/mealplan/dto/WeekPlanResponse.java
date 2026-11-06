package com.wikicook.mealplan.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * openapi {@code WeekPlan}.
 *
 * @param today              set only when the week contains today in Vietnam
 * @param empty              no dish at all (FR-022)
 * @param availableItemCount dishes that can go to the shopping list (FR-025)
 */
public record WeekPlanResponse(LocalDate weekStart, LocalDate weekEnd, LocalDate today, boolean empty,
                               int availableItemCount, List<DayPlanDto> days) {
}
