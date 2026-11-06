package com.wikicook.mealplan.domain;

/**
 * {@code meal_plan_items.meal} (spec 030 data-model); declaration order is the display order (030 FR-003).
 * Created early by spec 031, which plans meals the same way.
 */
public enum Meal {
    BREAKFAST,
    LUNCH,
    DINNER;

    /** A meal holds 0 to 4 dishes (030 FR-004, Clarifications 2026-11-02). */
    public static final int MAX_DISHES = 4;
}
