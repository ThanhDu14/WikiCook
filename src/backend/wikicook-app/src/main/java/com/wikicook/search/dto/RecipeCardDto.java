package com.wikicook.search.dto;

import com.wikicook.search.domain.Difficulty;
import java.math.BigDecimal;
import java.util.List;

/**
 * One result card (020 FR-013; contract schema {@code RecipeCard}).
 *
 * @param coverImageUrl null: the UI shows its placeholder
 * @param ratingAvg     null when {@code reviewCount = 0} ("Chưa có đánh giá")
 * @param conflicts     non-empty only for recipes shown through {@code showHidden}
 */
public record RecipeCardDto(
        long id,
        String title,
        String coverImageUrl,
        int totalTimeMinutes,
        Difficulty difficulty,
        BigDecimal ratingAvg,
        int reviewCount,
        String cuisine,
        List<ConflictDto> conflicts) {

    public RecipeCardDto {
        conflicts = List.copyOf(conflicts);
    }

    public RecipeCardDto withConflicts(List<ConflictDto> value) {
        return new RecipeCardDto(id, title, coverImageUrl, totalTimeMinutes, difficulty, ratingAvg, reviewCount,
                cuisine, value);
    }
}
