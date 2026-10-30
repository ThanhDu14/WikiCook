package com.wikicook.mealplan.controller;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A {@code weekStart}: not null and a Monday (030 FR-001). */
@Documented
@Constraint(validatedBy = MondayDateValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface MondayDate {

    String message() default "must be a Monday";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
