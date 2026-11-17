package com.wikicook.profile.controller;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.wikicook.profile.service.ProfileErrorCode;
import com.wikicook.profile.service.ProfileException;
import com.wikicook.profile.service.ProfileException.FieldCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
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
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * ProblemDetail + {@code code} for the controllers of {@code com.wikicook.profile} (research R17), the same shape as
 * {@code MealPlanExceptionHandler}. Limited to this package on purpose: the advices of 030, 031 and 040 sit at the same
 * precedence and answer the validation errors of their own controllers. What it does not handle (including
 * {@code DietaryProfileUnavailableException}, which also leaves the planners) falls to {@code GlobalExceptionHandler},
 * so no stack trace or class name leaves the server.
 *
 * <p>Field errors become {@code errors: [{field, code}]} (FR-016). The code is the message of the constraint when it
 * already is a code ({@code OUT_OF_RANGE}, {@code EXCLUDED_LIMIT_EXCEEDED}), otherwise it comes from the annotation.
 */
@RestControllerAdvice(basePackages = "com.wikicook.profile")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProfileExceptionHandler {

    private static final Pattern CODE_LIKE = Pattern.compile("[A-Z][A-Z0-9_]+");
    private static final Set<Class<?>> WHOLE_NUMBERS = Set.of(Integer.class, int.class, Short.class, short.class,
            Long.class, long.class);

    @ExceptionHandler(ProfileException.class)
    ResponseEntity<ProblemDetail> handle(ProfileException exception) {
        return respond(exception.code(), exception.errors(), exception.retryAfterSeconds());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handle(MethodArgumentNotValidException exception) {
        List<FieldCode> errors = new ArrayList<>();
        exception.getBindingResult().getAllErrors().forEach(error -> {
            String field = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
            errors.add(new FieldCode(field, codeOf(error.getCode(), error.getDefaultMessage())));
        });
        return respond(ProfileErrorCode.VALIDATION_ERROR, errors, null);
    }

    /**
     * A body Jackson can not read. {@code householdSize: 2.5} is a {@code MismatchedInputException} with the field in
     * its path: the error stays next to its field (Edge: Invalid data) and the parser's own message is dropped.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handle(HttpMessageNotReadableException exception) {
        List<FieldCode> errors = List.of();
        if (exception.getCause() instanceof MismatchedInputException mismatch) {
            String field = mismatch.getPath().stream().map(JsonMappingException.Reference::getFieldName)
                    .filter(name -> name != null).findFirst().orElse(null);
            if (field != null) {
                boolean wholeNumber = mismatch.getTargetType() != null && WHOLE_NUMBERS.contains(mismatch.getTargetType());
                errors = List.of(new FieldCode(field, wholeNumber ? "NOT_A_WHOLE_NUMBER" : "INVALID_FORMAT"));
            }
        }
        return respond(ProfileErrorCode.VALIDATION_ERROR, errors, null);
    }

    /**
     * A multipart request that could not be read. Over the limit of {@code spring.servlet.multipart.max-file-size} or
     * {@code max-request-size} it is 413 {@code AVATAR_TOO_LARGE}: raised after the handler is chosen because
     * {@code resolve-lazily} is on (R7), as a {@link MaxUploadSizeExceededException} or, when a {@code @RequestPart} is
     * read first, as a plain {@link MultipartException} whose cause is the size error of the servlet container.
     */
    @ExceptionHandler(MultipartException.class)
    ResponseEntity<ProblemDetail> handle(MultipartException exception) {
        return respond(isTooLarge(exception) ? ProfileErrorCode.AVATAR_TOO_LARGE : ProfileErrorCode.VALIDATION_ERROR,
                List.of(), null);
    }

    private static boolean isTooLarge(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof MaxUploadSizeExceededException || cause.getClass().getSimpleName().contains("SizeLimitExceeded")) {
                return true;
            }
        }
        return false;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ProblemDetail> handle(HandlerMethodValidationException exception) {
        List<FieldCode> errors = new ArrayList<>();
        exception.getParameterValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                errors.add(new FieldCode(name, codeOf(null, error.getDefaultMessage())));
            }
        });
        return respond(ProfileErrorCode.VALIDATION_ERROR, errors, null);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ProblemDetail> handleBadRequestShape(Exception exception) {
        return respond(ProfileErrorCode.VALIDATION_ERROR, List.of(), null);
    }

    /** The profile row was changed between the read and the save of the same form (Edge: Two tabs). */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> handleLostRace(OptimisticLockingFailureException exception) {
        return respond(ProfileErrorCode.PROFILE_CHANGED, List.of(), null);
    }

    /** A foreign key of the dietary lists broke because the catalogue changed just now (research R5). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleIntegrity(DataIntegrityViolationException exception) {
        return respond(ProfileErrorCode.VALIDATION_ERROR, List.of(), null);
    }

    private static ResponseEntity<ProblemDetail> respond(ProfileErrorCode code, List<FieldCode> errors,
            Integer retryAfterSeconds) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), code.detail());
        problem.setProperty("code", code.name());
        if (!errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        ResponseEntity.BodyBuilder response = ResponseEntity.status(code.status());
        if (retryAfterSeconds != null) {
            problem.setProperty("retryAfterSeconds", retryAfterSeconds);
            response.header("Retry-After", retryAfterSeconds.toString());
        }
        return response.body(problem);
    }

    private static String codeOf(String constraint, String message) {
        if (message != null && CODE_LIKE.matcher(message).matches()) {
            return message;
        }
        if (constraint == null) {
            return "INVALID";
        }
        return switch (constraint) {
            case "NotNull", "NotBlank", "NotEmpty" -> "REQUIRED";
            case "Min", "Max", "Range" -> "OUT_OF_RANGE";
            case "Size" -> "TOO_LONG";
            default -> "INVALID";
        };
    }
}
