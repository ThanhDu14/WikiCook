package com.wikicook.mealplan.controller;

import com.wikicook.mealplan.service.exception.MealPlanException;
import com.wikicook.mealplan.service.exception.SlotOccupiedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * ProblemDetail + {@code code} for the meal planner controllers (research R11), the same shape as 031. Runs
 * before {@code GlobalExceptionHandler}, which keeps handling everything else (500 without details).
 */
@RestControllerAdvice(basePackages = "com.wikicook.mealplan")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MealPlanExceptionHandler {

    private static final String WEEK_START = "weekStart";

    @ExceptionHandler(MealPlanException.class)
    ProblemDetail handle(MealPlanException ex) {
        ProblemDetail problem = problem(ex.status(), ex.code(), ex.getMessage());
        if (ex instanceof SlotOccupiedException occupied) {
            problem.setProperty("currentItem", occupied.currentItem());
        }
        return problem;
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ProblemDetail handleInvalidInput(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The request is not valid");
    }

    /** A bad {@code weekStart} has its own code (openapi {@code INVALID_WEEK_START}). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail handleInvalidParameter(HandlerMethodValidationException ex) {
        boolean weekStart = ex.getParameterValidationResults().stream()
                .anyMatch(result -> WEEK_START.equals(result.getMethodParameter().getParameterName()));
        return weekStart ? invalidWeekStart() : handleInvalidInput(ex);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return WEEK_START.equals(ex.getName()) ? invalidWeekStart() : handleInvalidInput(ex);
    }

    private static ProblemDetail invalidWeekStart() {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_WEEK_START", "weekStart must be a Monday (YYYY-MM-DD)");
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code);
        return problem;
    }
}
