package com.wikicook.profile.port;

import com.wikicook.profile.repository.DietaryLinkRepository.Snapshot;
import com.wikicook.profile.service.DietaryProfileReader;
import com.wikicook.shoppinglist.port.DietaryProfilePort;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The real {@link DietaryProfilePort} of 040 (research R8). A shopping line holds an ingredient, not an allergen, so
 * {@code allergenIngredientIds} are the ingredients that {@code ingredient_allergens} ties to the person's allergens;
 * {@code excludedIngredientIds} are the ones they excluded themselves. Same failure rule as
 * {@link MealPlanDietaryAdapter}.
 */
@Component
public class ShoppingDietaryAdapter implements DietaryProfilePort {

    private final DietaryProfileReader reader;

    public ShoppingDietaryAdapter(DietaryProfileReader reader) {
        this.reader = reader;
    }

    @Override
    public UserDietaryRestrictions getRestrictions(Long userId) {
        Snapshot profile = reader.read(userId).orElse(null);
        if (profile == null) {
            return new UserDietaryRestrictions(Set.of(), Set.of());
        }
        return new UserDietaryRestrictions(reader.ingredientIdsWithAllergens(profile.allergenIds()),
                profile.excludedIngredientIds());
    }
}
