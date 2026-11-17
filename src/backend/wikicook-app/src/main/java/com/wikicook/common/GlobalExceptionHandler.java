package com.wikicook.common;

import com.wikicook.common.diet.DietaryProfileUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Every error leaves the API as an RFC 7807 {@code ProblemDetail}. Spring MVC exceptions (validation, 404, 405…)
 * are mapped by the base class; anything else becomes a 500 without the exception message or stack trace.
 * Feature packages add their own {@code @RestControllerAdvice} for domain exceptions.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * FR-026a: the dietary profile can not be read, so 030, 031 and 020 must not choose dishes without it. The cause is
     * logged, never sent; the client only learns that it can try again (research R8, R17).
     */
    @ExceptionHandler(DietaryProfileUnavailableException.class)
    ProblemDetail handleProfileUnavailable(DietaryProfileUnavailableException ex) {
        log.error("Dietary profile unavailable", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "The dietary profile can not be read right now");
        problem.setProperty("code", "PROFILE_UNAVAILABLE");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
    }
}
