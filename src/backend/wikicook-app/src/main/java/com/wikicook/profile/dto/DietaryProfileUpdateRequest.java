package com.wikicook.profile.dto;

import com.wikicook.mealplan.domain.DietType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * {@code PUT /me/dietary-profile}: the whole form, replaced in one go (FR-017). Every list and {@code dietType} must be
 * present, so a client that forgets a field can not clear an allergy list without saying so. The message of a
 * constraint is the {@code errors[].code} the client reads (FR-016); a missing value is {@code REQUIRED}.
 *
 * @param householdSize      1-20, {@code null} = not set
 * @param maxCookTimeMinutes 10-240, {@code null} = no limit
 * @param version            {@code dietaryVersion} of the form when it was loaded (Edge: Two tabs)
 */
public record DietaryProfileUpdateRequest(
        @NotNull Set<@NotNull Long> allergenIds,
        @NotNull @Size(max = 50, message = "EXCLUDED_LIMIT_EXCEEDED") Set<@NotNull Long> excludedIngredientIds,
        @NotNull DietType dietType,
        @NotNull Set<@NotNull Long> preferredCuisineIds,
        @Min(value = 1, message = "OUT_OF_RANGE") @Max(value = 20, message = "OUT_OF_RANGE") Integer householdSize,
        @Min(value = 10, message = "OUT_OF_RANGE") @Max(value = 240, message = "OUT_OF_RANGE") Integer maxCookTimeMinutes,
        @NotNull Integer version) {
}
