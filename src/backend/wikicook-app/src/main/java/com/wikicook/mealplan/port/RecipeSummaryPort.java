package com.wikicook.mealplan.port;

import java.util.Collection;
import java.util.Map;

/** Implemented by the recipe module (spec 060); {@link JdbcRecipeSummaryAdapter} stands in until then. */
public interface RecipeSummaryPort {

    /** Summaries of the recipes that exist, keyed by id; unknown ids are left out. */
    Map<Long, RecipeSummary> findByIds(Collection<Long> recipeIds);
}
