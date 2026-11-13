package com.wikicook.auth.dto;

import com.wikicook.auth.service.EmailNormalizer;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

/** Ask for a new link by {@code email} (screen AU03) or by the {@code token} of an expired link (AU04): exactly one. */
public record ResendVerificationRequest(@Size(max = 254) String email, @Size(max = 128) String token) {

    public ResendVerificationRequest {
        email = email == null || email.isBlank() ? null : EmailNormalizer.normalize(email);
        token = token == null || token.isBlank() ? null : token;
    }

    @AssertTrue(message = "REQUIRED")
    public boolean isExactlyOneIdentifier() {
        return (email == null) != (token == null);
    }
}
