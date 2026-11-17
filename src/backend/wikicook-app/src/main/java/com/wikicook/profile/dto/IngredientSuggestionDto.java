package com.wikicook.profile.dto;

/** openapi {@code NamedItem} for an ingredient: only what the picker needs, never {@code kind} or {@code status}. */
public record IngredientSuggestionDto(long id, String name) {
}
