package com.wikicook.auth.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * {@code wikicook.auth.*} (spec 010 T007). Secrets come only from the environment; startup fails when
 * {@code jwt-secret} is shorter than 32 bytes or {@code email-hash-key} is empty (constitution IV).
 */
@Validated
@ConfigurationProperties("wikicook.auth")
public record AuthProperties(
        String jwtSecret,
        @NotBlank(message = "wikicook.auth.email-hash-key (WIKICOOK_EMAIL_HASH_KEY) must be set") String emailHashKey,
        @DefaultValue("http://localhost:3000") String publicBaseUrl,
        @DefaultValue("true") boolean cookieSecure,
        @DefaultValue("12") int bcryptStrength,
        @DefaultValue("15m") Duration accessTokenTtl,
        @DefaultValue("no-reply@wikicook.local") String mailFrom,
        @DefaultValue("") String appealContact,
        @Valid @DefaultValue SignIn signIn,
        @Valid @DefaultValue Jobs jobs,
        @Valid @DefaultValue Google google,
        @Valid @DefaultValue BootstrapAdmin bootstrapAdmin) {

    private static final int MIN_JWT_SECRET_BYTES = 32;

    public record SignIn(@DefaultValue("100") int ipLimit) {
    }

    public record Jobs(@DefaultValue("true") boolean enabled) {
    }

    public record Google(@DefaultValue("") String clientId, @DefaultValue("") String clientSecret) {

        public boolean configured() {
            return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
        }
    }

    /** The first Administrator, created at startup when both are set and no active Administrator exists. */
    public record BootstrapAdmin(@DefaultValue("") String email, @DefaultValue("") String password) {

        public boolean configured() {
            return email != null && !email.isBlank() && password != null && !password.isEmpty();
        }

        /** The password must never reach a log line (NFR-001). */
        @Override
        public String toString() {
            return "BootstrapAdmin[email=" + email + ", password=****]";
        }
    }

    @AssertTrue(message = "wikicook.auth.jwt-secret (WIKICOOK_JWT_SECRET) must be at least 32 bytes")
    public boolean isJwtSecretLongEnough() {
        return jwtSecret != null && jwtSecret.getBytes(StandardCharsets.UTF_8).length >= MIN_JWT_SECRET_BYTES;
    }

    /**
     * Where users send an appeal: {@code appeal-contact}, or {@code mail-from} when it is empty or only blanks,
     * because Compose passes an unset variable as an empty string (research R8).
     */
    public String appealContact() {
        return appealContact == null || appealContact.isBlank() ? mailFrom : appealContact;
    }
}
