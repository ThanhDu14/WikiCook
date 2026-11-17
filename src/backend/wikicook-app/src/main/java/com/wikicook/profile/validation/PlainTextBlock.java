package com.wikicook.profile.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Visible text that may span several lines (the bio, FR-003): line breaks are kept, every other control character, the
 * Unicode line and paragraph separators and the invisible formatting characters are refused. A zero-width joiner stays
 * (emoji). {@code null} and blank are valid; the DTO turns a blank bio into {@code null}. The violation message is the
 * code {@code BIO_INVALID}.
 */
@Documented
@Constraint(validatedBy = PlainTextBlockValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface PlainTextBlock {

    String message() default "BIO_INVALID";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
