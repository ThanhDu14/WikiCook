package com.wikicook.mealplan.controller;

import com.wikicook.mealplan.service.WeekCalendar;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

/** Checks {@link MondayDate}. */
public class MondayDateValidator implements ConstraintValidator<MondayDate, LocalDate> {

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        return WeekCalendar.isMonday(value);
    }
}
