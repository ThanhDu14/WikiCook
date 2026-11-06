package com.wikicook.mealplan.service.exception;

import org.springframework.http.HttpStatus;

/** {@code weekStart} is not a Monday (FR-002). */
public class InvalidWeekStartException extends MealPlanException {

    public InvalidWeekStartException() {
        super(HttpStatus.BAD_REQUEST, "INVALID_WEEK_START", "weekStart must be a Monday (YYYY-MM-DD)");
    }
}
