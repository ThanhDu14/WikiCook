package com.wikicook.shoppinglist.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Body of {@code POST /api/v1/shopping-lists/active/import/meal-plan} (US2 FR-004). The week is its Monday, as in the
 * planner's own routes; {@code mode} decides what happens to an active list that already has items (Q3).
 */
public record ImportMealPlanRequest(
    @NotNull LocalDate weekStart,
    @NotNull Mode mode,
    List<@Valid ServingOverride> servingOverrides,
    List<Long> excludedIngredientIds
) {

    public enum Mode {
        /** Add to the active list, summing what is already there. */
        MERGE,
        /** Archive the active list (when it has items) and fill a new one. */
        ARCHIVE_AND_NEW
    }

    public record ServingOverride(@NotNull Long mealPlanItemId, @NotNull @DecimalMin("0.5") BigDecimal targetServings) {
    }
}
