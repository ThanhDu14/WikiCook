package com.wikicook.aiplanner.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.mealplan.domain.DietType;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Diet type shown on a draft dish: the strictest diet its ingredient kinds allow (FR-009, DietRuleSql). */
class RecipeCardQueryTest {

    @Test
    void strictestDietTheIngredientsAllow() {
        assertThat(RecipeCardQuery.dietType(Set.of("PLANT"))).isEqualTo(DietType.VEGAN);
        assertThat(RecipeCardQuery.dietType(Set.of())).isEqualTo(DietType.VEGAN);
        assertThat(RecipeCardQuery.dietType(Set.of("PLANT", "EGG"))).isEqualTo(DietType.VEGETARIAN);
        assertThat(RecipeCardQuery.dietType(Set.of("SEAFOOD"))).isEqualTo(DietType.PESCATARIAN);
        assertThat(RecipeCardQuery.dietType(Set.of("MEAT", "PLANT"))).isEqualTo(DietType.NONE);
    }
}
