package com.wikicook.auth.dto;

import com.wikicook.auth.validation.MaxCodePoints;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * Email and password sign-in (FR-007). The email is trimmed and lower-cased by the service; the password is kept exactly
 * as typed, up to 128 code points (R3). Format is not checked: a malformed email is just a failed sign-in, the same as
 * an unknown one (FR-008).
 */
public record LoginRequest(
        @NotBlank @Size(max = 254) String email,
        @NotEmpty @MaxCodePoints(128) String password,
        boolean rememberMe) {

    /** The password must never reach a log line (NFR-001). */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=****, rememberMe=" + rememberMe + "]";
    }
}
