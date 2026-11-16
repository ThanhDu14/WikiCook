package com.wikicook.auth.dto;

import com.wikicook.auth.service.EmailNormalizer;
import com.wikicook.auth.validation.PasswordPair;
import com.wikicook.auth.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Registration form (FR-001). The display name and the email are trimmed, and the email lower-cased, as the record is
 * built (FR-002), so validation and the service both see the normalized values; the password and its confirmation are
 * kept exactly as typed.
 */
@ValidPassword
public record RegisterRequest(
        @NotBlank @Size(max = 50) String displayName,
        @NotBlank @Size(max = 254) @Email String email,
        @NotNull @ValidPassword String password,
        @NotNull String confirmPassword) implements PasswordPair {

    public RegisterRequest {
        displayName = displayName == null ? null : displayName.strip();
        email = EmailNormalizer.normalize(email);
    }

    /** Passwords must never reach a log line (NFR-001). */
    @Override
    public String toString() {
        return "RegisterRequest[displayName=" + displayName + ", email=" + email + ", password=****]";
    }
}
