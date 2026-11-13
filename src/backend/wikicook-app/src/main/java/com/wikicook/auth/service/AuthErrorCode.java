package com.wikicook.auth.service;

import org.springframework.http.HttpStatus;

/** The {@code code} values of {@code Problem.code} in contracts/openapi.yaml, each with its HTTP status (R15). */
public enum AuthErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
    INVALID_CLIENT(HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED(HttpStatus.LOCKED),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN),
    ACCOUNT_BANNED(HttpStatus.FORBIDDEN),
    EMAIL_TAKEN(HttpStatus.CONFLICT),
    REGISTRATION_REFUSED(HttpStatus.UNPROCESSABLE_ENTITY),
    EMAIL_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS),
    TOKEN_EXPIRED(HttpStatus.GONE),
    TOKEN_INVALID(HttpStatus.GONE),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN),
    ACCESS_DENIED(HttpStatus.FORBIDDEN),
    REAUTH_REQUIRED(HttpStatus.FORBIDDEN),
    SELF_ACTION_FORBIDDEN(HttpStatus.FORBIDDEN),
    LAST_ADMINISTRATOR(HttpStatus.CONFLICT),
    ACCOUNT_NOT_ACTIVE(HttpStatus.CONFLICT),
    ACCOUNT_NOT_SANCTIONED(HttpStatus.CONFLICT),
    SUSPENSION_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND),
    PASSWORD_CHECK_LIMITED(HttpStatus.TOO_MANY_REQUESTS);

    private final HttpStatus status;

    AuthErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
