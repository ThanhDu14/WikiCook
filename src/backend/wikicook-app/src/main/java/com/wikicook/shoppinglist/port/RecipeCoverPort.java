package com.wikicook.shoppinglist.port;

import java.util.Collection;
import java.util.Map;

/** Cover photos of recipes, shown on the dish sections of the list (design 2026-11-24). */
public interface RecipeCoverPort {

    /** Recipe id → cover URL, only for the recipes that have a cover. */
    Map<Long, String> findCovers(Collection<Long> recipeIds);
}
