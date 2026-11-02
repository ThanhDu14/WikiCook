package com.wikicook.mealplan.service.exception;

import org.springframework.http.HttpStatus;

/** The recipe is not PUBLISHED, is a snack, or does not exist (FR-010, US6-3). */
public class RecipeNotAvailableException extends MealPlanException {

    public RecipeNotAvailableException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "RECIPE_NOT_AVAILABLE", "The recipe cannot be added to a plan");
    }
}
