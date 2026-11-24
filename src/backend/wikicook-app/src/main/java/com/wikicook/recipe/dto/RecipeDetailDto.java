package com.wikicook.recipe.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Body of {@code GET /api/v1/recipes/{id}} (060 contracts/openapi.yaml {@code RecipeDetail}). */
public record RecipeDetailDto(
        long id,
        String status,
        String title,
        String description,
        String coverImageUrl,
        LookupOption cuisine,
        LookupOption category,
        List<LookupOption> methods,
        List<LookupOption> tags,
        List<LookupOption> equipment,
        String difficulty,
        Integer prepTimeMinutes,
        Integer cookTimeMinutes,
        Integer totalTimeMinutes,
        Integer servings,
        /** null when nobody has reviewed the recipe yet. */
        BigDecimal ratingAvg,
        int reviewCount,
        /** Ungrouped rows first ({@code name} null), then the groups in order of first appearance. */
        List<IngredientGroup> ingredientGroups,
        List<Step> steps,
        Video video,
        Author author,
        /** Attribution of an imported recipe, which must be shown (scraper README). */
        Source source,
        Instant publishedAt,
        boolean isOwner
) {

    public record LookupOption(long id, String slug, String name) {
    }

    public record IngredientGroup(String name, List<Ingredient> items) {
    }

    public record Ingredient(long ingredientId, String name, BigDecimal quantity, Unit unit, String amountText,
                             String note, boolean optional) {
    }

    public record Unit(String code, String name) {
    }

    public record Step(int position, String title, String content, Integer timerSeconds, List<String> imageUrls) {
    }

    public record Video(String url, String platform) {
    }

    public record Author(Long id, String displayName) {
    }

    public record Source(String name, String url) {
    }
}
