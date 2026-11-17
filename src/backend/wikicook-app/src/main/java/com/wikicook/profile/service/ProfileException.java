package com.wikicook.profile.service;

import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * A refusal the profile module reports with a {@link ProfileErrorCode}. The message is the code name, never user
 * input, so it is safe to log. Field errors ({@code errors: [{field, code}]}, FR-016) and the wait of a rate limit
 * travel as fields.
 */
public class ProfileException extends RuntimeException {

    /** One entry of {@code errors[]}: the request field and a machine code such as {@code UNKNOWN_ALLERGEN}. */
    public record FieldCode(String field, String code) {
    }

    private final ProfileErrorCode code;
    private final List<FieldCode> errors;
    private final Integer retryAfterSeconds;

    public ProfileException(ProfileErrorCode code) {
        this(code, List.of(), null);
    }

    private ProfileException(ProfileErrorCode code, List<FieldCode> errors, Integer retryAfterSeconds) {
        super(code.name());
        this.code = code;
        this.errors = List.copyOf(errors);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static ProfileException invalid(List<FieldCode> errors) {
        return new ProfileException(ProfileErrorCode.VALIDATION_ERROR, errors, null);
    }

    public static ProfileException invalid(String field, String code) {
        return invalid(List.of(new FieldCode(field, code)));
    }

    public static ProfileException rateLimited(int retryAfterSeconds) {
        return new ProfileException(ProfileErrorCode.PROFILE_RATE_LIMITED, List.of(), retryAfterSeconds);
    }

    public ProfileErrorCode code() {
        return code;
    }

    public HttpStatus status() {
        return code.status();
    }

    public List<FieldCode> errors() {
        return errors;
    }

    /** Seconds the caller should wait; only for the rate-limit codes. */
    public Integer retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
