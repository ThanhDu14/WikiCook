package com.wikicook.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Password rules of FR-004 (research R3). On a {@code String} field: 8 to 128 code points after NFC, at least one
 * letter and one digit; each broken rule is its own violation whose message is the code ({@code PASSWORD_TOO_SHORT},
 * {@code PASSWORD_TOO_LONG}, {@code PASSWORD_NO_LETTER}, {@code PASSWORD_NO_DIGIT}). On a class implementing
 * {@link PasswordPair}: the password must differ from the email ({@code PASSWORD_EQUALS_EMAIL}, reported on
 * {@code password}) and the confirmation must equal it ({@code PASSWORD_MISMATCH}, reported on
 * {@code confirmPassword}). The password is never trimmed.
 */
@Documented
@Constraint(validatedBy = {ValidPasswordValidator.class, ValidPasswordPairValidator.class})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    String message() default "INVALID_FORMAT";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
