package com.wikicook.mealplan.service.exception;

import org.springframework.http.HttpStatus;

/** The meal already has 4 dishes (FR-004); adding or moving one more into it is refused. */
public class MealFullException extends MealPlanException {

    public MealFullException() {
        super(HttpStatus.CONFLICT, "MEAL_FULL", "This meal already has 4 dishes");
    }
}
