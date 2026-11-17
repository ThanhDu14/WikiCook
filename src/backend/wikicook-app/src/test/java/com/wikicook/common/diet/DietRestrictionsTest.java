package com.wikicook.common.diet;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.common.diet.DietRuleSql.SqlClause;
import com.wikicook.mealplan.domain.DietType;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * T102a (FR-020, FR-021, FR-022): the hard rules of a person are the allergens and the excluded ingredients, nothing
 * else. {@code hardRules} is the one factory 020, 030 and 031 use when the person has not confirmed a diet type.
 */
class DietRestrictionsTest {

    @Test
    @DisplayName("FR-020: hardRules keeps the allergen ids and the excluded ingredient ids")
    void keepsBothLists() {
        DietRestrictions rules = DietRestrictions.hardRules(Set.of(1L, 2L), Set.of(10L));

        assertThat(rules.allergenIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(rules.excludedIngredientIds()).containsExactly(10L);
    }

    @Test
    @DisplayName("FR-022: hardRules never carries a diet type, so a Vegetarian profile alone removes nothing")
    void neverCarriesADietType() {
        assertThat(DietRestrictions.hardRules(Set.of(1L), Set.of(2L)).dietType()).isEqualTo(DietType.NONE);
        assertThat(DietRestrictions.hardRules(Set.of(), Set.of()).dietType()).isEqualTo(DietType.NONE);
    }

    @Test
    @DisplayName("FR-021: with two empty lists the condition is TRUE and has no parameters")
    void emptyRulesAreTrue() {
        SqlClause clause = DietRuleSql.notViolating("r.id", DietRestrictions.hardRules(Set.of(), Set.of()));

        assertThat(clause.sql()).isEqualTo("TRUE");
        assertThat(clause.params()).isEmpty();
    }

    @Test
    @DisplayName("FR-021: with data the condition is about allergens and ingredients, never about ingredients.kind")
    void noKindCondition() {
        SqlClause clause = DietRuleSql.notViolating("r.id", DietRestrictions.hardRules(Set.of(1L), Set.of(2L)));

        assertThat(clause.sql()).contains("ingredient_allergens").contains("recipe_ingredients");
        assertThat(clause.sql()).doesNotContain("kind").doesNotContain("diet_labels");
        assertThat(clause.params()).containsOnlyKeys("dietAllergenIds", "dietExcludedIngredientIds");
    }

    @Test
    @DisplayName("FR-022: the diet type is a rule only when the caller hands it over (031 after the person confirmed)")
    void dietTypeStillWorksWhenTheCallerAsksForIt() {
        SqlClause clause = DietRuleSql.notViolating("r.id", new DietRestrictions(Set.of(), Set.of(), DietType.VEGAN));

        assertThat(clause.sql()).contains("kind");
    }
}
