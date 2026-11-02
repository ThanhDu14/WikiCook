package com.wikicook.mealplan.service;

import com.wikicook.common.diet.DietRestrictions;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.dto.PickerPageDto;
import com.wikicook.mealplan.port.CollectionRecipePort;
import com.wikicook.mealplan.port.DietaryProfilePort;
import com.wikicook.mealplan.port.PlannerProfile;
import com.wikicook.mealplan.repository.RecipePickerQueryRepository;
import org.springframework.stereotype.Service;

/** The picker of a slot (FR-007, T042): ranks by the caller's profile, optionally within their collections. */
@Service
public class PickerService {

    public enum Scope {
        ALL,
        COLLECTIONS
    }

    private final RecipePickerQueryRepository query;
    private final DietaryProfilePort profiles;
    private final CollectionRecipePort collections;

    public PickerService(RecipePickerQueryRepository query, DietaryProfilePort profiles,
                         CollectionRecipePort collections) {
        this.query = query;
        this.profiles = profiles;
        this.collections = collections;
    }

    public PickerPageDto search(long userId, Meal meal, String q, Scope scope, int page, int size) {
        PlannerProfile profile = profiles.findByUserId(userId);
        var restrictions = new DietRestrictions(profile.allergenIds(), profile.excludedIngredientIds(),
                profile.dietType());
        var onlyIds = scope == Scope.COLLECTIONS ? collections.savedRecipeIds(userId) : null;
        return query.search(new RecipePickerQueryRepository.Criteria(meal, q, restrictions,
                profile.preferredCuisineIds(), profile.maxCookTimeMinutes(), onlyIds, page, size));
    }
}
