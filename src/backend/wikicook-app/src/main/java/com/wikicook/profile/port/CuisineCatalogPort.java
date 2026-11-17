package com.wikicook.profile.port;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The cuisine catalogue as the profile needs it (contracts/integration.md 1.2). Owned by the admin module of 100; until
 * its bean exists {@code ProfileDefaultsConfig} provides {@link JdbcCuisineCatalog}.
 */
public interface CuisineCatalogPort {

    /** Cuisines with {@code is_active}, in the order of {@code position}, then name (FR-013). */
    List<CuisineName> listActive();

    /** The ids of {@code cuisineIds} that are active now. */
    Set<Long> activeAmong(Collection<Long> cuisineIds);

    Map<Long, String> namesOf(Collection<Long> cuisineIds);

    record CuisineName(long id, String name) {
    }
}
