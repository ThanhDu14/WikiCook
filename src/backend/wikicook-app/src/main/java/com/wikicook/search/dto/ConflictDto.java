package com.wikicook.search.dto;

/** Why a recipe breaks the viewer's profile (020 FR-020): an allergen ({@code name_vi}) or an excluded ingredient. */
public record ConflictDto(Type type, String name) {

    public enum Type {
        ALLERGEN,
        EXCLUDED_INGREDIENT
    }
}
