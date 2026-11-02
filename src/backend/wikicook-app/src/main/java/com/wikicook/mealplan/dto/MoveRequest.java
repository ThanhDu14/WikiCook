package com.wikicook.mealplan.dto;

import com.wikicook.mealplan.domain.Meal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;

/** {@code POST …/moves}: the dish the client saw in {@code from}, and what it saw in {@code to} (FR-014, FR-015). */
public record MoveRequest(@NotNull @Valid From from, @NotNull @Valid To to) {

    public record From(@NotNull DayOfWeek day, @NotNull Meal meal, @NotNull Long itemId, @NotNull Integer version) {
    }

    /** {@code expected} is {@code null} when the target slot looked empty. */
    public record To(@NotNull DayOfWeek day, @NotNull Meal meal, @Valid ExpectedSlotState expected) {
    }
}
