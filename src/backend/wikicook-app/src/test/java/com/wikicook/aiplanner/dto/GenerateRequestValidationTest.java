package com.wikicook.aiplanner.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.domain.Meal;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Bean Validation of the generate body (FR-003, FR-005, edge cases on servings and cook time). */
class GenerateRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static GenerationConstraints constraints(Integer servings, Integer maxCookTime) {
        return new GenerationConstraints(Set.of(1L), Set.of(), DietType.NONE, servings, maxCookTime, Set.of());
    }

    private static GenerateRequest request(Set<DayOfWeek> days, Set<Meal> meals, GenerationConstraints constraints) {
        return new GenerateRequest(UUID.randomUUID(), days, meals, constraints);
    }

    private Set<String> invalidPaths(Object value) {
        Set<String> paths = new java.util.TreeSet<>();
        for (ConstraintViolation<Object> v : validator.validate(value)) {
            paths.add(v.getPropertyPath().toString());
        }
        return paths;
    }

    @Test
    void fullWeekWithProfileDefaultsIsValid() {
        assertThat(invalidPaths(request(EnumSet.allOf(DayOfWeek.class), EnumSet.allOf(Meal.class),
                constraints(4, 30)))).isEmpty();
        assertThat(invalidPaths(request(Set.of(DayOfWeek.MONDAY), Set.of(Meal.DINNER),
                constraints(null, null)))).isEmpty();
    }

    @Test
    void noDayOrNoMealIsRejected() {
        assertThat(invalidPaths(request(Set.of(), Set.of(Meal.LUNCH), constraints(2, null)))).containsExactly("days");
        assertThat(invalidPaths(request(Set.of(DayOfWeek.MONDAY), Set.of(), constraints(2, null))))
                .containsExactly("meals");
    }

    @Test
    void servingsOutside1To20AndCookTimeBelow10AreRejected() {
        Set<DayOfWeek> days = Set.of(DayOfWeek.MONDAY);
        Set<Meal> meals = Set.of(Meal.LUNCH);
        assertThat(invalidPaths(request(days, meals, constraints(0, null)))).containsExactly("constraints.servings");
        assertThat(invalidPaths(request(days, meals, constraints(21, null)))).containsExactly("constraints.servings");
        assertThat(invalidPaths(request(days, meals, constraints(2, 5))))
                .containsExactly("constraints.maxCookTimeMinutes");
    }

    @Test
    void requestIdAndConstraintsAreRequired() {
        assertThat(invalidPaths(new GenerateRequest(null, Set.of(DayOfWeek.MONDAY), Set.of(Meal.LUNCH), null)))
                .containsExactly("constraints", "requestId");
    }

    @Test
    void avoidRecipeIdsAreOptionalAndAtMost84_T114() throws Exception {
        GenerateRequest none = new GenerateRequest(UUID.randomUUID(), Set.of(DayOfWeek.MONDAY), Set.of(Meal.LUNCH),
                constraints(2, null), null);
        assertThat(none.avoidRecipeIds()).isEmpty();
        assertThat(request(Set.of(DayOfWeek.MONDAY), Set.of(Meal.LUNCH), constraints(2, null)).avoidRecipeIds())
                .isEmpty();

        Set<Long> tooMany = java.util.stream.LongStream.rangeClosed(1, 85).boxed()
                .collect(java.util.stream.Collectors.toSet());
        assertThat(invalidPaths(new GenerateRequest(UUID.randomUUID(), Set.of(DayOfWeek.MONDAY), Set.of(Meal.LUNCH),
                constraints(2, null), tooMany))).containsExactly("avoidRecipeIds");

        // an older client sends no avoidRecipeIds at all
        GenerateRequest parsed = new com.fasterxml.jackson.databind.ObjectMapper().readValue("""
                {"requestId":"%s","days":["MONDAY"],"meals":["LUNCH"],
                 "constraints":{"allergenIds":[],"excludedIngredientIds":[],"dietType":"NONE","servings":2,
                                "maxCookTimeMinutes":null,"preferredCuisineIds":[]}}"""
                .formatted(UUID.randomUUID()), GenerateRequest.class);
        assertThat(parsed.avoidRecipeIds()).isEmpty();
    }
}
