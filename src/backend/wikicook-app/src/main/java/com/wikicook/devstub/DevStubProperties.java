package com.wikicook.devstub;

import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.port.PlannerProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * {@code wikicook.dev-stubs.*}.
 *
 * @param userId  the user when a request has no {@code X-Dev-User-Id} header
 * @param profile what {@link StubDietaryProfile} returns for every user
 */
@Validated
@ConfigurationProperties("wikicook.dev-stubs")
public record DevStubProperties(
        @DefaultValue("false") boolean enabled,
        @Positive @DefaultValue("1") long userId,
        @Valid @NotNull @DefaultValue Profile profile) {

    public record Profile(
            Integer householdSize,
            @NotNull @DefaultValue("NONE") DietType dietType,
            Integer maxCookTimeMinutes,
            @DefaultValue Set<Long> allergenIds,
            @DefaultValue Set<Long> excludedIngredientIds,
            @DefaultValue Set<Long> preferredCuisineIds) {

        PlannerProfile toPlannerProfile() {
            return new PlannerProfile(householdSize, dietType, maxCookTimeMinutes, allergenIds, excludedIngredientIds,
                    preferredCuisineIds);
        }
    }
}
