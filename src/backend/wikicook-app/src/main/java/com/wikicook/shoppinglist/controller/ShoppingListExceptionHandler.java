package com.wikicook.shoppinglist.controller;

import com.wikicook.shoppinglist.service.CapacityExceededException;
import com.wikicook.shoppinglist.service.ShoppingListNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Feature-specific exception handler mapping domain exceptions to RFC 7807 ProblemDetail (T016).
 */
@RestControllerAdvice(basePackages = "com.wikicook.shoppinglist")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ShoppingListExceptionHandler {

    @ExceptionHandler(CapacityExceededException.class)
    ProblemDetail handleCapacityExceeded(CapacityExceededException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create("https://wikicook.com/errors/capacity-exceeded"));
        problem.setTitle("Shopping List Capacity Exceeded");
        return problem;
    }

    @ExceptionHandler(ShoppingListNotFoundException.class)
    ProblemDetail handleNotFound(ShoppingListNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://wikicook.com/errors/not-found"));
        problem.setTitle("Resource Not Found");
        return problem;
    }
}
