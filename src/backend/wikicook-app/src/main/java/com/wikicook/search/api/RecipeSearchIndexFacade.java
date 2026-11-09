package com.wikicook.search.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refreshes a recipe's search document (020 contracts/integration.md 1). Call it in the same transaction that moves the
 * recipe into PUBLISHED or replaces its published content (spec 100 approval, spec 060 revision approval); otherwise
 * the recipe is published but cannot be found by keyword.
 */
@Service
public class RecipeSearchIndexFacade {

    private final JdbcTemplate jdbc;

    public RecipeSearchIndexFacade(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Joins the caller's transaction (REQUIRED); no-op for an unknown id. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void reindex(long recipeId) {
        jdbc.update("UPDATE recipes SET search_vector = f_recipe_search_vector(id) WHERE id = ?", recipeId);
    }
}
