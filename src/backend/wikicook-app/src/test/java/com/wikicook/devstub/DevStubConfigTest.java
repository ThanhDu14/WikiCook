package com.wikicook.devstub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.wikicook.common.security.CurrentUser;
import com.wikicook.mealplan.api.MealPlanWriteFacade;
import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.port.DietaryProfilePort;
import com.wikicook.mealplan.port.PlannerProfile;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/** The dev stubs stand in for 010/011 only when switched on, and step aside for the real beans. */
class DevStubConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class, DevStubConfig.class))
            .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class));

    @Test
    void offByDefault() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(CurrentUser.class);
            assertThat(context).doesNotHaveBean(DietaryProfilePort.class);
            assertThat(context).doesNotHaveBean(MealPlanWriteFacade.class);
        });
    }

    @Test
    void offWhenFalse() {
        runner.withPropertyValues("wikicook.dev-stubs.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MealPlanWriteFacade.class));
    }

    @Test
    void onProvidesEveryStub() {
        runner.withPropertyValues("wikicook.dev-stubs.enabled=true").run(context -> {
            assertThat(context).getBean(CurrentUser.class).isInstanceOf(StubCurrentUser.class);
            assertThat(context).getBean(DietaryProfilePort.class).isInstanceOf(StubDietaryProfile.class);
            assertThat(context).doesNotHaveBean(MealPlanWriteFacade.class); // 030 provides the real one
        });
    }

    @Test
    void realBeansReplaceTheStubs() {
        runner.withPropertyValues("wikicook.dev-stubs.enabled=true")
                .withUserConfiguration(RealBeans.class)
                .run(context -> {
                    assertThat(context).getBean(CurrentUser.class).isSameAs(RealBeans.USER);
                    assertThat(context).getBean(DietaryProfilePort.class).isSameAs(RealBeans.PROFILE);
                    assertThat(context).doesNotHaveBean(StubCurrentUser.class);
                });
    }

    @Test
    void defaultProfileIsEmpty() {
        runner.withPropertyValues("wikicook.dev-stubs.enabled=true").run(context -> {
            PlannerProfile profile = context.getBean(DietaryProfilePort.class).findByUserId(1);
            assertThat(profile).isEqualTo(PlannerProfile.empty());
            assertThat(context.getBean(DevStubProperties.class).userId()).isEqualTo(1);
        });
    }

    @Test
    void profileComesFromProperties() {
        runner.withPropertyValues(
                "wikicook.dev-stubs.enabled=true",
                "wikicook.dev-stubs.user-id=7",
                "wikicook.dev-stubs.profile.household-size=4",
                "wikicook.dev-stubs.profile.diet-type=PESCATARIAN",
                "wikicook.dev-stubs.profile.max-cook-time-minutes=45",
                "wikicook.dev-stubs.profile.allergen-ids=1,2",
                "wikicook.dev-stubs.profile.excluded-ingredient-ids=10",
                "wikicook.dev-stubs.profile.preferred-cuisine-ids=3").run(context -> {
                    assertThat(context.getBean(DietaryProfilePort.class).findByUserId(99)).isEqualTo(
                            new PlannerProfile(4, DietType.PESCATARIAN, 45, Set.of(1L, 2L), Set.of(10L), Set.of(3L)));
                    assertThat(context.getBean(DevStubProperties.class).userId()).isEqualTo(7);
                });
    }

    @Test
    void nonPositiveConfiguredUserIdStopsStartup() {
        runner.withPropertyValues("wikicook.dev-stubs.enabled=true", "wikicook.dev-stubs.user-id=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    static class RealBeans {

        static final CurrentUser USER = () -> 42L;
        static final DietaryProfilePort PROFILE = userId -> PlannerProfile.empty();

        @Bean
        CurrentUser realCurrentUser() {
            return USER;
        }

        @Bean
        DietaryProfilePort realProfile() {
            return PROFILE;
        }
    }
}
