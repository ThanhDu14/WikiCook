package com.wikicook.profile.port;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The ingredient catalogue as the profile needs it (contracts/integration.md 1.2). Owned by the ingredient module of
 * 060; until its bean exists {@code ProfileDefaultsConfig} provides {@link JdbcIngredientCatalog}.
 */
public interface IngredientCatalogPort {

    /** APPROVED names that contain {@code query}, without diacritics and case (FR-011, research R6); best match first. */
    List<IngredientName> suggest(String query, int limit);

    /** The ids of {@code ingredientIds} that are APPROVED now. */
    Set<Long> approvedAmong(Collection<Long> ingredientIds);

    Map<Long, String> namesOf(Collection<Long> ingredientIds);

    record IngredientName(long id, String name) {
    }
}
