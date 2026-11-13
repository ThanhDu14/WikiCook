package com.wikicook.support;

import com.wikicook.auth.config.AuthProperties;
import java.time.Duration;

/** {@link AuthProperties} with fixed fake values for plain unit tests (no Spring context). */
public final class TestAuthProperties {

    public static final String JWT_SECRET = "unit-test-jwt-secret-0123456789abcdef0123456789";
    public static final String EMAIL_HASH_KEY = "unit-test-email-hash-key-0123456789";

    private TestAuthProperties() {
    }

    public static AuthProperties create() {
        return create(JWT_SECRET, EMAIL_HASH_KEY);
    }

    public static AuthProperties create(String jwtSecret, String emailHashKey) {
        return new AuthProperties(jwtSecret, emailHashKey, "http://localhost:3000", false, 4,
                Duration.ofMinutes(15), "no-reply@wikicook.local", "",
                new AuthProperties.SignIn(100), new AuthProperties.Jobs(false), new AuthProperties.Google("", ""));
    }
}
