package com.wikicook.auth.controller;

import com.wikicook.auth.service.AuthException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * ProblemDetail + {@code code} for the controllers of {@code com.wikicook.auth} (research R15). Limited to this package
 * on purpose: the advices of 030, 031 and 040 sit at the same precedence and answer validation errors of their own
 * controllers, so a global advice here would race with them. Anything not handled falls through to
 * {@code GlobalExceptionHandler} (500 without detail), so no stack trace or class name leaves the server.
 *
 * <p>Field errors become {@code errors: [{field, code}]} (NFR-007). The code is the message of the constraint when it
 * already is a code ({@code PASSWORD_TOO_SHORT}, {@code PASSWORD_MISMATCH}, set by 010's own validators), otherwise
 * it is derived from the Bean Validation annotation: {@code REQUIRED}, {@code TOO_LONG}, {@code INVALID_EMAIL},
 * {@code INVALID_FORMAT}.
 */
@RestControllerAdvice(basePackages = "com.wikicook.auth")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthExceptionHandler {

    private static final Pattern CODE_LIKE = Pattern.compile("[A-Z][A-Z0-9_]+");

    @ExceptionHandler(AuthException.class)
    ResponseEntity<ProblemDetail> handle(AuthException exception) {
        ResponseEntity.BodyBuilder response = ResponseEntity.status(exception.code().status());
        if (exception.retryAfterSeconds() != null) {
            response.header("Retry-After", exception.retryAfterSeconds().toString());
        }
        return response.body(exception.problem());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleInvalidBody(MethodArgumentNotValidException exception) {
        List<FieldCode> errors = new ArrayList<>();
        exception.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError field) {
                errors.add(new FieldCode(field.getField(), codeOf(field.getCode(), field.getDefaultMessage())));
            } else {
                // a class-level constraint such as "passwords match": its message is the code, its field the name
                errors.add(new FieldCode(error.getObjectName(), codeOf(error.getCode(), error.getDefaultMessage())));
            }
        });
        return validation(errors);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class, HandlerMethodValidationException.class})
    ProblemDetail handleInvalidInput(Exception exception) {
        return validation(List.of());
    }

    private static ProblemDetail validation(List<FieldCode> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "The request is not valid");
        problem.setProperty("code", "VALIDATION_ERROR");
        if (!errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        return problem;
    }

    private static String codeOf(String constraint, String message) {
        if (message != null && CODE_LIKE.matcher(message).matches()) {
            return message;
        }
        if (constraint == null) {
            return "INVALID_FORMAT";
        }
        return switch (constraint) {
            case "NotBlank", "NotNull", "NotEmpty" -> "REQUIRED";
            case "Size", "Max", "Length" -> "TOO_LONG";
            case "Email" -> "INVALID_EMAIL";
            default -> "INVALID_FORMAT";
        };
    }

    public record FieldCode(String field, String code) {
    }
}
