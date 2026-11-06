package com.wikicook.mealplan.dto;

import java.util.List;

/** openapi {@code MoveResult}: one meal after a reorder, source and target meals after a move. */
public record MoveResult(List<MealItems> meals) {
}
