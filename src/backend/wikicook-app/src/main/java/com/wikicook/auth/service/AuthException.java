package com.wikicook.auth.service;

import com.wikicook.auth.dto.SessionSnapshot;
import com.wikicook.common.security.AccountStatus;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ProblemDetail;

/**
 * A domain error of spec 010: a status, a {@link AuthErrorCode code} and optional extra fields of the contract
 * ({@code retryAt}, {@code reason}, {@code endsAt}, {@code closureToken}, {@code retryAfterSeconds}). Both
 * {@code AuthExceptionHandler} (controllers) and {@code ProblemResponseWriter} (security filters) turn it into the same
 * ProblemDetail through {@link #problem()}. The message is shown to the client, so it never carries internal detail.
 */
public class AuthException extends RuntimeException {

    private final AuthErrorCode code;
    private final Map<String, Object> properties = new LinkedHashMap<>();

    public AuthException(AuthErrorCode code, String detail) {
        super(detail);
        this.code = code;
    }

    public AuthException(AuthErrorCode code) {
        this(code, code.name());
    }

    /** Adds an extra field of the contract; a null value is left out, as the field is optional. */
    public AuthException with(String name, Object value) {
        if (value != null) {
            properties.put(name, value);
        }
        return this;
    }

    public AuthErrorCode code() {
        return code;
    }

    public Map<String, Object> properties() {
        return Map.copyOf(properties);
    }

    public ProblemDetail problem() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), getMessage());
        problem.setProperty("code", code.name());
        properties.forEach(problem::setProperty);
        return problem;
    }

    /** Fills {@code Retry-After} for the two 429 codes (research R5). */
    public Long retryAfterSeconds() {
        return properties.get("retryAfterSeconds") instanceof Number seconds ? seconds.longValue() : null;
    }

    public static AuthException unauthenticated() {
        return new AuthException(AuthErrorCode.UNAUTHENTICATED, "Sign in first");
    }

    public static AuthException invalidCredentials() {
        return new AuthException(AuthErrorCode.INVALID_CREDENTIALS, "Email or password is incorrect");
    }

    public static AuthException invalidClient() {
        return new AuthException(AuthErrorCode.INVALID_CLIENT, "Missing or wrong X-WikiCook-Client header");
    }

    public static AuthException accessDenied() {
        return new AuthException(AuthErrorCode.ACCESS_DENIED, "You are not allowed to do this");
    }

    public static AuthException emailNotVerified() {
        return new AuthException(AuthErrorCode.EMAIL_NOT_VERIFIED, "Verify your email first");
    }

    public static AuthException accountLocked(Instant retryAt) {
        return new AuthException(AuthErrorCode.ACCOUNT_LOCKED, "Too many failed sign-ins, try again later")
                .with("retryAt", retryAt);
    }

    public static AuthException accountSuspended(String reason, Instant endsAt) {
        return new AuthException(AuthErrorCode.ACCOUNT_SUSPENDED, "This account is suspended")
                .with("reason", reason).with("endsAt", endsAt);
    }

    /** {@code closureToken} is set only after the password was right (research R16); null leaves it out. */
    public static AuthException accountBanned(String reason, String closureToken) {
        AuthException exception = new AuthException(AuthErrorCode.ACCOUNT_BANNED, "This account is banned")
                .with("reason", reason);
        return closureToken == null ? exception : exception.with("closureToken", closureToken);
    }

    public static AuthException userNotFound() {
        return new AuthException(AuthErrorCode.USER_NOT_FOUND, "No such account");
    }

    public static AuthException selfActionForbidden() {
        return new AuthException(AuthErrorCode.SELF_ACTION_FORBIDDEN, "You cannot do this to your own account");
    }

    public static AuthException reauthRequired() {
        return new AuthException(AuthErrorCode.REAUTH_REQUIRED, "Confirm your password again to continue");
    }

    public static AuthException suspensionLimitExceeded() {
        return new AuthException(AuthErrorCode.SUSPENSION_LIMIT_EXCEEDED, "A suspension lasts at most 30 days");
    }

    public static AuthException accountNotActive() {
        return new AuthException(AuthErrorCode.ACCOUNT_NOT_ACTIVE, "The account is not active");
    }

    public static AuthException accountNotSanctioned() {
        return new AuthException(AuthErrorCode.ACCOUNT_NOT_SANCTIONED, "The account has no suspension or ban to lift");
    }

    public static AuthException lastAdministrator() {
        return new AuthException(AuthErrorCode.LAST_ADMINISTRATOR, "There must be at least one active Administrator");
    }

    public static AuthException validation(String detail) {
        return new AuthException(AuthErrorCode.VALIDATION_ERROR, detail);
    }

    /** 400 {@code VALIDATION_ERROR} with {@code errors[{field, code}]} like a Bean Validation failure (FR-004, R6). */
    public static AuthException passwordEqualsEmail() {
        return validation("The request is not valid").with("errors",
                List.of(Map.of("field", "newPassword", "code", "PASSWORD_EQUALS_EMAIL")));
    }

    public static AuthException emailTaken() {
        return new AuthException(AuthErrorCode.EMAIL_TAKEN, "An account with this email already exists");
    }

    /** Neutral on purpose: it must not say that the email is on the Blocked Email list (FR-026). */
    public static AuthException registrationRefused() {
        return new AuthException(AuthErrorCode.REGISTRATION_REFUSED, "This email cannot be used to register");
    }

    public static AuthException tokenExpired() {
        return new AuthException(AuthErrorCode.TOKEN_EXPIRED, "This link has expired");
    }

    public static AuthException tokenInvalid() {
        return new AuthException(AuthErrorCode.TOKEN_INVALID, "This link is not valid");
    }

    public static AuthException emailRateLimited(long retryAfterSeconds) {
        return new AuthException(AuthErrorCode.EMAIL_RATE_LIMITED, "Too many emails requested, try again later")
                .with("retryAfterSeconds", retryAfterSeconds);
    }

    /** The 403 of a suspended or banned account, with the reason and, for a suspension, its end (FR-013, FR-024). */
    public static AuthException restricted(SessionSnapshot snapshot) {
        return snapshot.status() == AccountStatus.BANNED
                ? accountBanned(snapshot.sanctionReason(), null)
                : accountSuspended(snapshot.sanctionReason(), snapshot.sanctionEndsAt());
    }

    public static AuthException tooManyRequests(long retryAfterSeconds) {
        return new AuthException(AuthErrorCode.TOO_MANY_REQUESTS, "Too many requests, try again later")
                .with("retryAfterSeconds", retryAfterSeconds);
    }
}
