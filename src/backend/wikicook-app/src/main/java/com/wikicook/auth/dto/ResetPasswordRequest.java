package com.wikicook.auth.dto;

import com.wikicook.auth.validation.PasswordPair;
import com.wikicook.auth.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /auth/password/reset} (FR-015, FR-016). The form carries no email, so {@link ValidPassword} checks the
 * rules of FR-004 that need none and that the confirmation matches; the password is compared with the account email in
 * {@code PasswordResetService}, after the account is read from the token (research R6). Errors are reported on
 * {@code newPassword}, the name the client sends. The password is kept exactly as typed.
 */
@ValidPassword
public record ResetPasswordRequest(
        @NotBlank @Size(max = 128) String token,
        @NotNull @ValidPassword String newPassword,
        @NotNull String confirmPassword) implements PasswordPair {

    @Override
    public String password() {
        return newPassword;
    }

    @Override
    public String email() {
        return null;
    }

    @Override
    public String passwordField() {
        return "newPassword";
    }

    /** Neither the passwords nor the one-time token may reach a log line (NFR-001). */
    @Override
    public String toString() {
        return "ResetPasswordRequest[token=****, newPassword=****, confirmPassword=****]";
    }
}
