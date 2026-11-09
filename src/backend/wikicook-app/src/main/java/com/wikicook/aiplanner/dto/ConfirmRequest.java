package com.wikicook.aiplanner.dto;

import com.wikicook.mealplan.domain.ItemSource;
import com.wikicook.mealplan.domain.Meal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

/**
 * Body of {@code POST …/confirm} (openapi {@code ConfirmRequest}, FR-018): the draft as the user left it.
 *
 * @param requestId the generation the draft came from
 */
public record ConfirmRequest(
        @NotNull UUID requestId,
        @NotEmpty @Size(max = 21) List<@NotNull @Valid Item> items) {

    /**
     * One dish of the draft.
     *
     * @param servings {@code null} → 030's default (household size, else the recipe's servings)
     */
    public record Item(
            @NotNull DayOfWeek day,
            @NotNull Meal meal,
            long recipeId,
            @Min(1) @Max(20) Integer servings,
            @NotNull ItemSource source) {
    }
}
