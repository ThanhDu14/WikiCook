package com.wikicook.auth.dto;

import com.wikicook.auth.service.EmailNormalizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code POST /auth/password/forgot} (FR-014): the email is trimmed and lower-cased as the record is built (FR-002). */
public record ForgotPasswordRequest(@NotBlank @Size(max = 254) @Email String email) {

    public ForgotPasswordRequest {
        email = EmailNormalizer.normalize(email);
    }
}
