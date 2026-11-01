package com.wikicook.shoppinglist.port;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for standalone seed RecipeDetailPort adapter.
 * Backs off automatically when a real RecipeDetailPort bean (e.g. from module 060) is present in the context.
 */
@Configuration
public class SeedRecipeDetailConfiguration {

    @Bean
    @ConditionalOnMissingBean(RecipeDetailPort.class)
    public SeedRecipeDetailAdapter seedRecipeDetailAdapter() {
        return new SeedRecipeDetailAdapter();
    }
}
