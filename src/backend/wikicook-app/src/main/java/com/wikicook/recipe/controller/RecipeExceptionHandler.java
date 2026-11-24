package com.wikicook.recipe.controller;

import com.wikicook.recipe.service.RecipeNotFoundException;
import java.net.URI;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** ProblemDetail answers of the 060 controllers; a missing and a hidden recipe look the same. */
@Order(Ordered.HIGHEST_PRECEDENCE) // before GlobalExceptionHandler, which answers 500 for unknown exceptions
@RestControllerAdvice(assignableTypes = RecipeDetailController.class)
public class RecipeExceptionHandler {

    @ExceptionHandler(RecipeNotFoundException.class)
    ProblemDetail notFound(RecipeNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Công thức này không còn tồn tại.");
        problem.setType(URI.create("https://wikicook.com/errors/not-found"));
        problem.setTitle("Not Found");
        problem.setProperty("code", "NOT_FOUND");
        return problem;
    }
}
