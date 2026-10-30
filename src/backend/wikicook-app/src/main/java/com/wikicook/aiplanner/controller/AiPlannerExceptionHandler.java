package com.wikicook.aiplanner.controller;

import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * ProblemDetail + {@code code} for the AI planner controllers (research R16), same shape as 030. Runs before
 * {@code GlobalExceptionHandler}, which keeps handling everything else. Details are fixed texts: nothing from
 * the provider or the exception cause reaches the client.
 */
@RestControllerAdvice(basePackages = "com.wikicook.aiplanner")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AiPlannerExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AiPlannerExceptionHandler.class);

    private final Clock clock;

    public AiPlannerExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(AiPlannerException.class)
    ResponseEntity<ProblemDetail> handle(AiPlannerException ex) {
        if (ex.getCause() != null) {
            log.warn("{}: {}", ex.code(), ex.getCause().getClass().getSimpleName());
        }
        ProblemDetail problem = problem(ex.status(), ex.code(), ex.getMessage());
        ResponseEntity.BodyBuilder response = ResponseEntity.status(ex.status());
        switch (ex) {
            case NotEnoughRecipesException notEnough -> {
                problem.setProperty("eligibleCount", notEnough.eligibleCount());
                problem.setProperty("requestedSlots", notEnough.requestedSlots());
                problem.setProperty("suggestions", notEnough.suggestions());
            }
            case RateLimitedException limited -> {
                problem.setProperty("nextAvailableAt", limited.nextAvailableAt());
                long seconds = Math.max(1, Duration.between(clock.instant(), limited.nextAvailableAt()).toSeconds());
                response.header(HttpHeaders.RETRY_AFTER, String.valueOf(seconds));
            }
            default -> {
            }
        }
        return response.body(problem);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class,
            HttpMessageNotReadableException.class})
    ProblemDetail handleInvalidInput(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The request is not valid");
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code);
        return problem;
    }
}
