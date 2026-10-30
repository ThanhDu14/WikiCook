package com.wikicook.mealplan.controller;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** 030 T011: only a Monday is a valid {@code weekStart}. */
class MondayDateValidatorTest {

    record Holder(@MondayDate LocalDate weekStart) {
    }

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void mondayIsValid() {
        assertThat(validator.validate(new Holder(LocalDate.of(2026, 10, 19)))).isEmpty();
    }

    @Test
    void tuesdayIsNotValid() {
        assertThat(validator.validate(new Holder(LocalDate.of(2026, 10, 20))))
                .singleElement()
                .satisfies(v -> assertThat(v.getConstraintDescriptor().getAnnotation()).isInstanceOf(MondayDate.class));
    }

    @Test
    void nullIsNotValid() {
        assertThat(validator.validate(new Holder(null))).hasSize(1);
    }
}
