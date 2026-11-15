package com.wikicook.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.support.TestAuthProperties;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** T035: the startup checks of {@code wikicook.auth} (NFR-001, constitution IV) and the appeal-contact fallback. */
class AuthPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validPropertiesPass() {
        assertThat(validator.validate(TestAuthProperties.create())).isEmpty();
    }

    @Test
    void jwtSecretShorterThan32BytesIsRejected() {
        var violations = validator.validate(TestAuthProperties.create("x".repeat(31), "key"));
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("32");
    }

    @Test
    void jwtSecretCountsBytesNotCharacters() {
        // 11 characters x 3 bytes = 33 bytes
        assertThat(validator.validate(TestAuthProperties.create("ế".repeat(11), "key"))).isEmpty();
    }

    @Test
    void blankEmailHashKeyIsRejected() {
        assertThat(validator.validate(TestAuthProperties.create(TestAuthProperties.JWT_SECRET, " "))).isNotEmpty();
    }

    @Test
    void appealContactFallsBackToMailFromWhenEmptyOrBlank() {
        AuthProperties empty = withAppealContact("");
        AuthProperties blank = withAppealContact("   ");
        AuthProperties set = withAppealContact("help@wikicook.vn");

        assertThat(empty.appealContact()).isEqualTo("no-reply@wikicook.local");
        assertThat(blank.appealContact()).isEqualTo("no-reply@wikicook.local");
        assertThat(set.appealContact()).isEqualTo("help@wikicook.vn");
    }

    private static AuthProperties withAppealContact(String contact) {
        return new AuthProperties(TestAuthProperties.JWT_SECRET, TestAuthProperties.EMAIL_HASH_KEY,
                "http://localhost:3000", false, 4, Duration.ofMinutes(15), "no-reply@wikicook.local", contact,
                new AuthProperties.SignIn(100), new AuthProperties.Jobs(false), new AuthProperties.Google("", ""),
                new AuthProperties.BootstrapAdmin("", ""));
    }
}
