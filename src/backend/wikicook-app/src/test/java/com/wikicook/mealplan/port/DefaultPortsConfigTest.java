package com.wikicook.mealplan.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.wikicook.aiplanner.repository.EligibleRecipeQuery;
import com.wikicook.devstub.DevStubConfig;
import com.wikicook.devstub.StubDietaryProfile;
import com.wikicook.mealplan.domain.DietType;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** T021: 030 runs before 011/060/080/090 exist, and steps aside for their beans and for the dev stubs. */
class DefaultPortsConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class,
                    DevStubConfig.class, DefaultPortsConfig.class))
            .withBean(EligibleRecipeQuery.class, () -> mock(EligibleRecipeQuery.class))
            .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withBean(NamedParameterJdbcTemplate.class, () -> mock(NamedParameterJdbcTemplate.class));

    @Test
    void defaultsAreEmpty() {
        runner.run(context -> {
            assertThat(context.getBean(DietaryProfilePort.class).findByUserId(1)).isEqualTo(PlannerProfile.empty());
            assertThat(context.getBean(CollectionRecipePort.class).savedRecipeIds(1)).isEmpty();
            assertThat(context.getBean(DayNutritionPort.class).totals(Map.of(DayOfWeek.MONDAY, List.of(1L))))
                    .isEmpty();
            assertThat(context).getBean(RecipeSummaryPort.class).isInstanceOf(JdbcRecipeSummaryAdapter.class);
        });
    }

    @Test
    void devStubProfileWinsOverTheEmptyDefault() {
        runner.withPropertyValues("wikicook.dev-stubs.enabled=true", "wikicook.dev-stubs.profile.household-size=4",
                        "wikicook.dev-stubs.profile.diet-type=VEGAN")
                .run(context -> {
                    assertThat(context).getBean(DietaryProfilePort.class).isInstanceOf(StubDietaryProfile.class);
                    PlannerProfile profile = context.getBean(DietaryProfilePort.class).findByUserId(1);
                    assertThat(profile.householdSize()).isEqualTo(4);
                    assertThat(profile.dietType()).isEqualTo(DietType.VEGAN);
                });
    }

    @Test
    void realBeansReplaceTheDefaults() {
        runner.withUserConfiguration(RealPorts.class).run(context -> {
            assertThat(context).getBean(DietaryProfilePort.class).isSameAs(RealPorts.PROFILE);
            assertThat(context).getBean(CollectionRecipePort.class).isSameAs(RealPorts.COLLECTIONS);
            assertThat(context).getBean(RecipeSummaryPort.class).isSameAs(RealPorts.RECIPES);
            assertThat(context).getBean(DayNutritionPort.class).isSameAs(RealPorts.NUTRITION);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class RealPorts {
        static final DietaryProfilePort PROFILE = userId -> PlannerProfile.empty();
        static final CollectionRecipePort COLLECTIONS = userId -> Set.of(1L);
        static final RecipeSummaryPort RECIPES = ids -> Map.of();
        static final DayNutritionPort NUTRITION = byDay -> Map.of();

        @Bean
        DietaryProfilePort profile() {
            return PROFILE;
        }

        @Bean
        CollectionRecipePort collections() {
            return COLLECTIONS;
        }

        @Bean
        RecipeSummaryPort recipes() {
            return RECIPES;
        }

        @Bean
        DayNutritionPort nutrition() {
            return NUTRITION;
        }
    }
}
