package com.wikicook.mealplan.service.exception;

import org.springframework.http.HttpStatus;

/** A meal planner error with its HTTP status and stable {@code code} (research R11). */
public abstract class MealPlanException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected MealPlanException(HttpStatus status, String code, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
