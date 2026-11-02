package com.wikicook.shoppinglist.port;

import java.util.Set;

/**
 * Port to query user allergen and dietary restrictions (specs/040 integration.md 2.3).
 * Note: Not used in US1 + US2 slice.
 */
public interface DietaryProfilePort {

    UserDietaryRestrictions getRestrictions(Long userId);

    record UserDietaryRestrictions(
        Set<Long> allergenIngredientIds,
        Set<Long> excludedIngredientIds
    ) {}
}
