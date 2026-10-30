package com.wikicook.common.diet;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.mealplan.domain.DietType;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DietRuleSqlTest {

    @Test
    void noRulesIsTrue() {
        DietRuleSql.SqlClause clause = DietRuleSql.notViolating("r.id", DietRestrictions.none());

        assertThat(clause.sql()).isEqualTo("TRUE");
        assertThat(clause.params()).isEmpty();
    }

    @Test
    void valuesGoInParametersNotInTheSql() {
        var restrictions = new DietRestrictions(Set.of(4242L), Set.of(9191L), DietType.VEGAN);

        DietRuleSql.SqlClause clause = DietRuleSql.notViolating("x.recipe_id", restrictions);

        assertThat(clause.sql()).doesNotContain("4242").doesNotContain("9191").doesNotContain("DAIRY")
                .contains("x.recipe_id").contains(":dietAllergenIds").contains(":dietExcludedIngredientIds")
                .contains(":dietForbiddenKinds");
        assertThat(clause.params()).containsEntry("dietAllergenIds", Set.of(4242L))
                .containsEntry("dietExcludedIngredientIds", Set.of(9191L))
                .containsEntry("dietForbiddenKinds", Set.of("MEAT", "SEAFOOD", "EGG", "DAIRY", "HONEY"));
    }

    @Test
    void forbiddenKindsPerDiet() {
        assertThat(DietRuleSql.forbiddenKinds(DietType.NONE)).isEmpty();
        assertThat(DietRuleSql.forbiddenKinds(DietType.VEGETARIAN)).containsExactlyInAnyOrder("MEAT", "SEAFOOD");
        assertThat(DietRuleSql.forbiddenKinds(DietType.PESCATARIAN)).containsExactly("MEAT");
    }
}
