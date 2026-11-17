package com.wikicook.profile.port;

import com.wikicook.mealplan.port.DietaryProfilePort;
import com.wikicook.mealplan.port.PlannerProfile;
import com.wikicook.profile.service.DietaryProfileReader;
import org.springframework.stereotype.Component;

/**
 * The real {@link DietaryProfilePort} of 030, 031 and 020 (research R8), replacing the dev stub and the empty default.
 * {@code householdSize} stays {@code null} when the person left it empty; the reader applies the default (FR-023). A
 * person without a row is an empty profile (FR-026); a database failure is {@code DietaryProfileUnavailableException},
 * not an empty profile (FR-026a).
 */
@Component
public class MealPlanDietaryAdapter implements DietaryProfilePort {

    private final DietaryProfileReader reader;

    public MealPlanDietaryAdapter(DietaryProfileReader reader) {
        this.reader = reader;
    }

    @Override
    public PlannerProfile findByUserId(long userId) {
        return reader.read(userId)
                .map(profile -> new PlannerProfile(profile.householdSize(), profile.dietType(),
                        profile.maxCookTimeMinutes(), profile.allergenIds(), profile.excludedIngredientIds(),
                        profile.preferredCuisineIds()))
                .orElseGet(PlannerProfile::empty);
    }
}
