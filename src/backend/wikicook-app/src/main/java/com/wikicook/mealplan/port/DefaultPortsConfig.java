package com.wikicook.mealplan.port;

import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * What 030 uses while 060, 080 and 090 do not provide their ports yet (011 does: {@code MealPlanDietaryAdapter}) (research R7). An auto-configuration
 * that runs after the dev stubs, so the real beans and {@code com.wikicook.devstub} both win over these.
 */
@AutoConfiguration(afterName = "com.wikicook.devstub.DevStubConfig")
public class DefaultPortsConfig {

    /** ⛓ 080: nobody has collections yet. */
    @Bean
    @ConditionalOnMissingBean(CollectionRecipePort.class)
    CollectionRecipePort noCollections() {
        return userId -> Set.of();
    }

    /** ⛓ 090: no nutrition data, the UI shows "Chưa có số liệu dinh dưỡng". */
    @Bean
    @ConditionalOnMissingBean(DayNutritionPort.class)
    DayNutritionPort noNutrition() {
        return recipeIdsByDay -> Map.of();
    }

    /** ⛓ 060: read the recipe tables directly. */
    @Bean
    @ConditionalOnMissingBean(RecipeSummaryPort.class)
    RecipeSummaryPort jdbcRecipeSummaries(NamedParameterJdbcTemplate jdbc) {
        return new JdbcRecipeSummaryAdapter(jdbc);
    }
}
