package com.wikicook.mealplan.port;

/** Calories of one day per person, from spec 090 (FR-032). */
public record DayNutrition(int kcalPerPerson, int includedDishes, int missingDishes) {
}
