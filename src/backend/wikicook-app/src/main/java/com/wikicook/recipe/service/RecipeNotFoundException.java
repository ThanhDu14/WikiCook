package com.wikicook.recipe.service;

/** No such recipe, or one the viewer may not see: both answer the same 404 (060 NFR-004). */
public class RecipeNotFoundException extends RuntimeException {
    public RecipeNotFoundException(long id) {
        super("Recipe " + id + " not found");
    }
}
