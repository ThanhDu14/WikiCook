package com.wikicook.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The bodies of the password reset routes (contract schemas {@code ForgotPasswordRequest}, {@code ResetPasswordRequest}):
 * field rules of FR-004 reported on the field the client sent ({@code newPassword}), and no password in {@code toString}
 * (NFR-001).
 */
class PasswordResetRequestsTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static <T> Set<String> problems(Set<ConstraintViolation<T>> violations) {
        Set<String> out = new TreeSet<>();
        violations.forEach(v -> out.add(v.getPropertyPath() + ":" + v.getMessage()));
        return out;
    }

    @Test
    @DisplayName("FR-014: the email is trimmed and lower-cased, and must look like an email")
    void forgotNormalizesAndChecksTheEmail() {
        assertThat(new ForgotPasswordRequest("  An@Mail.COM ").email()).isEqualTo("an@mail.com");
        assertThat(problems(validator.validate(new ForgotPasswordRequest("an@mail.com")))).isEmpty();
        assertThat(problems(validator.validate(new ForgotPasswordRequest("not-an-email")))).hasSize(1)
                .allMatch(p -> p.startsWith("email:")); // the text of @Email depends on the JVM locale
        assertThat(problems(validator.validate(new ForgotPasswordRequest(" ")))).anyMatch(p -> p.startsWith("email:"));
        assertThat(problems(validator.validate(new ForgotPasswordRequest("a".repeat(250) + "@m.com")))).anyMatch(
                p -> p.startsWith("email:"));
    }

    @Test
    @DisplayName("FR-004: a good password and a matching confirmation pass")
    void goodFormPasses() {
        assertThat(problems(validator.validate(new ResetPasswordRequest("tok", "newpass123", "newpass123")))).isEmpty();
    }

    @Test
    @DisplayName("FR-004: each broken rule is reported on newPassword with its code")
    void passwordRulesAreReportedOnNewPassword() {
        Set<String> problems = problems(validator.validate(new ResetPasswordRequest("tok", "abc", "abc")));

        assertThat(problems).contains("newPassword:PASSWORD_TOO_SHORT", "newPassword:PASSWORD_NO_DIGIT")
                .allMatch(p -> p.startsWith("newPassword:"));
    }

    @Test
    @DisplayName("FR-004: a confirmation that differs is PASSWORD_MISMATCH on confirmPassword")
    void mismatchIsReportedOnConfirmPassword() {
        Set<String> problems = problems(validator.validate(new ResetPasswordRequest("tok", "newpass123", "newpass124")));

        assertThat(problems).containsExactly("confirmPassword:PASSWORD_MISMATCH");
    }

    @Test
    @DisplayName("NFR-007: a missing token or password is REQUIRED-like (NotBlank/NotNull), a long token is refused")
    void tokenAndPasswordsArePresent() {
        assertThat(problems(validator.validate(new ResetPasswordRequest(" ", "newpass123", "newpass123"))))
                .anyMatch(p -> p.startsWith("token:"));
        assertThat(problems(validator.validate(new ResetPasswordRequest("x".repeat(129), "newpass123", "newpass123"))))
                .anyMatch(p -> p.startsWith("token:"));
        assertThat(problems(validator.validate(new ResetPasswordRequest("tok", null, "newpass123"))))
                .anyMatch(p -> p.startsWith("newPassword:"));
        assertThat(problems(validator.validate(new ResetPasswordRequest("tok", "newpass123", null))))
                .anyMatch(p -> p.startsWith("confirmPassword:"));
    }

    @Test
    @DisplayName("NFR-001: toString never shows a password or the token")
    void toStringHidesSecrets() {
        String text = new ResetPasswordRequest("secret-token-value", "newpass123", "newpass123").toString();

        assertThat(text).doesNotContain("newpass123").doesNotContain("secret-token-value");
    }
}
