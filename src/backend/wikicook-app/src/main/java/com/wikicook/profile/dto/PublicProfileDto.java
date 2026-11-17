package com.wikicook.profile.dto;

/**
 * openapi {@code PublicProfile}, level 1 of research R3: the only fields a guest can ever receive about a person
 * (FR-005). A record with exactly these five components, so no private field can be added by mistake in a mapper.
 */
public record PublicProfileDto(long id, String displayName, String avatarUrl, String bio, long publishedRecipeCount) {
}
