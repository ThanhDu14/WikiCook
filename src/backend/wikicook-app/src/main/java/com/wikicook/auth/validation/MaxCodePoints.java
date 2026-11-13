package com.wikicook.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * At most {@code value} Unicode code points after NFC normalization (research R3). {@code @Size} counts UTF-16 units,
 * so 128 emoji (256 units) would pass registration yet fail sign-in; code points count what the user sees. The
 * violation message is the code {@code TOO_LONG}, which {@code AuthExceptionHandler} passes on as {@code errors[].code}.
 */
@Documented
@Constraint(validatedBy = MaxCodePointsValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxCodePoints {

    int value();

    String message() default "TOO_LONG";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
