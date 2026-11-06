package com.wikicook.mealplan.port;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

/** Implemented by the nutrition module (spec 090); the default knows nothing (contracts/integration.md 1). */
public interface DayNutritionPort {

    /** Key: day of the week → recipe ids of that day's dishes. Days without data are left out. */
    Map<DayOfWeek, DayNutrition> totals(Map<DayOfWeek, List<Long>> recipeIdsByDay);
}
