package com.wikicook.profile.service;

import org.springframework.http.HttpStatus;

/**
 * The {@code code} of a ProblemDetail that spec 011 answers (research R17), with its HTTP status. The codes that come
 * from spec 010 ({@code UNAUTHENTICATED}, {@code ACCOUNT_SUSPENDED}, {@code ACCOUNT_BANNED}, {@code REAUTH_REQUIRED},
 * {@code ACCESS_DENIED}) and {@code PROFILE_UNAVAILABLE} (answered by {@code GlobalExceptionHandler}, FR-026a) are not
 * here.
 */
public enum ProfileErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "The request is not valid"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "The profile does not exist"),
    PROFILE_CHANGED(HttpStatus.CONFLICT, "The profile was changed elsewhere; reload it"),
    PASSWORD_NOT_SET(HttpStatus.CONFLICT, "The account has no password"),
    PASSWORD_ALREADY_SET(HttpStatus.CONFLICT, "The account already has a password"),
    LAST_ADMINISTRATOR(HttpStatus.CONFLICT, "The last active Administrator can not be removed"),
    ACCOUNT_STATE_CHANGED(HttpStatus.CONFLICT, "The account changed state; sign in again"),
    TOKEN_INVALID(HttpStatus.GONE, "The link is invalid, expired or already used"),
    AVATAR_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "The avatar is larger than 5 MB"),
    AVATAR_TYPE_NOT_ALLOWED(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The avatar must be a JPEG, PNG or WebP image"),
    AVATAR_NOT_AN_IMAGE(HttpStatus.UNPROCESSABLE_ENTITY, "The avatar is not a readable image"),
    AVATAR_DIMENSIONS_TOO_LARGE(HttpStatus.UNPROCESSABLE_ENTITY, "The avatar is larger than 6000 x 6000 pixels"),
    INVALID_CURRENT_PASSWORD(HttpStatus.UNPROCESSABLE_ENTITY, "The current password is not correct"),
    PASSWORD_SAME_AS_CURRENT(HttpStatus.UNPROCESSABLE_ENTITY, "The new password must differ from the current one"),
    CONFIRMATION_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY, "The confirmation text does not match"),
    PROFILE_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many changes; try again later"),
    PASSWORD_CHECK_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many wrong passwords; try again later");

    private final HttpStatus status;
    private final String detail;

    ProfileErrorCode(HttpStatus status, String detail) {
        this.status = status;
        this.detail = detail;
    }

    public HttpStatus status() {
        return status;
    }

    /** Fixed text for the ProblemDetail; never built from an exception or from user input (constitution IV). */
    public String detail() {
        return detail;
    }
}
