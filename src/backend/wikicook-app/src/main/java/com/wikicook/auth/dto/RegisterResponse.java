package com.wikicook.auth.dto;

import java.time.Instant;

/**
 * Registration result. When the email limit of FR-017 was reached the account exists but no link was made:
 * {@code verificationEmailSent} is false, {@code verificationExpiresAt} is null and {@code retryAfterSeconds} says
 * how long until the user can ask for one.
 */
public record RegisterResponse(String email, boolean verificationEmailSent, Instant verificationExpiresAt,
        Long retryAfterSeconds) {
}
