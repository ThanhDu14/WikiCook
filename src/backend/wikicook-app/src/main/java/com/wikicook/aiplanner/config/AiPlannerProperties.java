package com.wikicook.aiplanner.config;

import com.wikicook.mealplan.domain.Meal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;

/** {@code wikicook.ai-planner.*}. */
@Validated
@ConfigurationProperties("wikicook.ai-planner")
public record AiPlannerProperties(@Valid @NotNull @DefaultValue MealCutoffs mealCutoffs) {

    /** Vietnam time after which today's meal can no longer be planned (research R12, edge case "Chủ nhật tối"). */
    public record MealCutoffs(
            @NotNull @DateTimeFormat(pattern = "HH:mm") @DefaultValue("10:00") LocalTime breakfast,
            @NotNull @DateTimeFormat(pattern = "HH:mm") @DefaultValue("14:00") LocalTime lunch,
            @NotNull @DateTimeFormat(pattern = "HH:mm") @DefaultValue("20:00") LocalTime dinner) {

        public LocalTime of(Meal meal) {
            return switch (meal) {
                case BREAKFAST -> breakfast;
                case LUNCH -> lunch;
                case DINNER -> dinner;
            };
        }
    }
}
