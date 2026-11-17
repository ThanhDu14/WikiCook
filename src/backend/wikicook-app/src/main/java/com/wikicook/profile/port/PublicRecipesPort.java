package com.wikicook.profile.port;

import java.util.List;

/**
 * The published recipes of one author for the public profile (FR-005, contracts/integration.md 1.2). Owned by the recipe
 * module of 060; until its bean exists {@code ProfileDefaultsConfig} provides {@link JdbcPublicRecipes}.
 */
public interface PublicRecipesPort {

    /** PUBLISHED recipes of {@code authorId}, newest first; {@code page} counts from 0. */
    List<PublicRecipeCard> findPublishedByAuthor(long authorId, int page, int size);

    long countPublishedByAuthor(long authorId);

    record PublicRecipeCard(long id, String title, String coverImageUrl, Integer totalTimeMinutes, double ratingAvg,
            int reviewCount) {
    }
}
