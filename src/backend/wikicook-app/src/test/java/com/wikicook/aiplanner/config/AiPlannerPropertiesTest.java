package com.wikicook.aiplanner.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.mealplan.domain.Meal;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** {@code wikicook.ai-planner.meal-cutoffs}: 10:00 / 14:00 / 20:00 Vietnam time unless configured (research R12). */
class AiPlannerPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(AiPlannerConfig.class);

    @Test
    void defaults() {
        runner.run(context -> {
            AiPlannerProperties.MealCutoffs cutoffs = context.getBean(AiPlannerProperties.class).mealCutoffs();
            assertThat(cutoffs.of(Meal.BREAKFAST)).isEqualTo(LocalTime.of(10, 0));
            assertThat(cutoffs.of(Meal.LUNCH)).isEqualTo(LocalTime.of(14, 0));
            assertThat(cutoffs.of(Meal.DINNER)).isEqualTo(LocalTime.of(20, 0));
        });
    }

    @Test
    void configured() {
        runner.withPropertyValues("wikicook.ai-planner.meal-cutoffs.lunch=13:30").run(context ->
                assertThat(context.getBean(AiPlannerProperties.class).mealCutoffs().of(Meal.LUNCH))
                        .isEqualTo(LocalTime.of(13, 30)));
    }
}
