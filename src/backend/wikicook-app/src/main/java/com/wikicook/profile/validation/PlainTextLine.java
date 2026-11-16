package com.wikicook.profile.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * One line of visible text: no control character, no line or paragraph separator, no invisible formatting character
 * (zero-width space, bidi override, byte order mark), and not only spaces. A zero-width joiner stays, because emoji
 * such as the woman cook are built with it. {@code null} is valid; presence is {@code @NotBlank}'s job. The violation
 * message is the code {@code DISPLAY_NAME_INVALID} that {@code ProfileExceptionHandler} passes on as
 * {@code errors[].code}.
 */
@Documented
@Constraint(validatedBy = PlainTextLineValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface PlainTextLine {

    String message() default "DISPLAY_NAME_INVALID";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
