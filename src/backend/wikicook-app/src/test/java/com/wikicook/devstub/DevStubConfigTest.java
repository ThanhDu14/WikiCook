package com.wikicook.devstub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.wikicook.common.security.CurrentUser;
import com.wikicook.mealplan.api.MealPlanWriteFacade;
import com.wikicook.mealplan.port.DietaryProfilePort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The dev stubs stand in for 010 only when switched on, and step aside for the real beans. The dietary profile is no
 * longer stubbed: spec 011 provides {@code MealPlanDietaryAdapter} (T047).
 */
class DevStubConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class, DevStubConfig.class))
            .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class));

    @Test
    void offByDefault() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(CurrentUser.class);
            assertThat(context).doesNotHaveBean(MealPlanWriteFacade.class);
        });
    }

    @Test
    void offWhenFalse() {
        runner.withPropertyValues("wikicook.dev-stubs.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MealPlanWriteFacade.class));
    }

    @Test
    void onProvidesTheCurrentUserOnly() {
        runner.withPropertyValues("wikicook.dev-stubs.enabled=true").run(context -> {
            assertThat(context).getBean(CurrentUser.class).isInstanceOf(StubCurrentUser.class);
            assertThat(context).doesNotHaveBean(DietaryProfilePort.class); // 011 provides the real one
            assertThat(context).doesNotHaveBean(MealPlanWriteFacade.class); // 030 provides the real one
        });
    }

    @Test
    void realBeansReplaceTheStubs() {
        runner.withPropertyValues("wikicook.dev-stubs.enabled=true")
                .withUserConfiguration(RealBeans.class)
                .run(context -> {
                    assertThat(context).getBean(CurrentUser.class).isSameAs(RealBeans.USER);
                    assertThat(context).doesNotHaveBean(StubCurrentUser.class);
                });
    }

    @Test
    void userIdDefaultsToOneAndComesFromProperties() {
        runner.withPropertyValues("wikicook.dev-stubs.enabled=true").run(context ->
                assertThat(context.getBean(DevStubProperties.class).userId()).isEqualTo(1));
        runner.withPropertyValues("wikicook.dev-stubs.enabled=true", "wikicook.dev-stubs.user-id=7").run(context ->
                assertThat(context.getBean(DevStubProperties.class).userId()).isEqualTo(7));
    }

    @Test
    void nonPositiveConfiguredUserIdStopsStartup() {
        runner.withPropertyValues("wikicook.dev-stubs.enabled=true", "wikicook.dev-stubs.user-id=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    static class RealBeans {

        static final CurrentUser USER = () -> 42L;

        @Bean
        CurrentUser realCurrentUser() {
            return USER;
        }
    }
}
