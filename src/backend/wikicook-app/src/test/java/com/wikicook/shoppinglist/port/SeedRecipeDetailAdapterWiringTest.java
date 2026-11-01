package com.wikicook.shoppinglist.port;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SeedRecipeDetailAdapterWiringTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Configuration
    static class RealAdapterConfiguration {
        @Bean
        public RecipeDetailPort realRecipeDetailPort() {
            return recipeId -> Optional.empty();
        }
    }

    @Test
    @DisplayName("Wiring (a): when only SeedRecipeDetailAdapter is registered, it becomes the single RecipeDetailPort bean")
    void whenOnlySeedAdapterPresent_registersSeedBean() {
        contextRunner
                .withUserConfiguration(SeedRecipeDetailAdapter.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(RecipeDetailPort.class);
                    assertThat(context).hasSingleBean(SeedRecipeDetailAdapter.class);
                    assertThat(context.getBean(RecipeDetailPort.class)).isInstanceOf(SeedRecipeDetailAdapter.class);
                });
    }

    @Test
    @DisplayName("Wiring (b): when a custom RecipeDetailPort is present, SeedRecipeDetailAdapter is backed off")
    void whenRealAdapterPresent_backsOffSeedAdapter() {
        contextRunner
                .withUserConfiguration(RealAdapterConfiguration.class, SeedRecipeDetailAdapter.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(RecipeDetailPort.class);
                    assertThat(context).doesNotHaveBean(SeedRecipeDetailAdapter.class);
                    assertThat(context).hasBean("realRecipeDetailPort");
                });
    }
}
