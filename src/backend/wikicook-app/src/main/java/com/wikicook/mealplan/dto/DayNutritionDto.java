package com.wikicook.mealplan.dto;

/** openapi {@code DayNutrition}, from spec 090 (FR-032). */
public record DayNutritionDto(int kcalPerPerson, int includedDishes, int missingDishes) {
}
