package com.wikicook.auth.domain;

/** {@code one_time_tokens.purpose}; the first two also count in {@code email_send_requests} (FR-017). */
public enum TokenPurpose {
    EMAIL_VERIFICATION,
    PASSWORD_RESET,
    /** Closure link for a banned user (research R16). */
    ACCOUNT_CLOSURE
}
