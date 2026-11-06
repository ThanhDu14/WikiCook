package com.wikicook.mealplan.dto;

import com.wikicook.mealplan.domain.Meal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;

/**
 * openapi {@code MoveItemRequest}: where a dish goes (FR-014, FR-015).
 *
 * @param position {@code null} = the end of the target meal
 * @param version  the version of the dish the client saw (research R4)
 */
public record MoveItemRequest(
        @NotNull DayOfWeek day,
        @NotNull Meal meal,
        @Min(0) @Max(3) Integer position,
        @NotNull Integer version) {
}
