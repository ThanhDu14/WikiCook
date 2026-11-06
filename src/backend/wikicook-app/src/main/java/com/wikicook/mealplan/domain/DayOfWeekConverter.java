package com.wikicook.mealplan.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.DayOfWeek;

/** {@code meal_plan_items.day_of_week}: ISO numbering, 1 = Monday … 7 = Sunday (data-model 1). */
@Converter
public class DayOfWeekConverter implements AttributeConverter<DayOfWeek, Short> {

    @Override
    public Short convertToDatabaseColumn(DayOfWeek day) {
        return day == null ? null : (short) day.getValue();
    }

    @Override
    public DayOfWeek convertToEntityAttribute(Short value) {
        return value == null ? null : DayOfWeek.of(value);
    }
}
