package com.wikicook.mealplan.port;

import java.util.Set;

/** Implemented by the collections module (spec 080); the default has no collections (contracts/integration.md 1). */
public interface CollectionRecipePort {

    /** Every recipe id the user saved in any collection. */
    Set<Long> savedRecipeIds(long userId);
}
