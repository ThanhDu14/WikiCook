package com.wikicook.mealplan.support;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Users, plans and items inserted straight into the 030 tables for repository and controller tests. */
public final class MealPlanFixtures {

    private final JdbcTemplate jdbc;

    public MealPlanFixtures(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** A fresh member; ids come from the identity sequence so they never collide with the dev stub users. */
    public long user() {
        return jdbc.queryForObject("INSERT INTO users (email, display_name) VALUES (?, 'Người dùng thử') RETURNING id",
                Long.class, "mp-" + UUID.randomUUID() + "@wikicook.test");
    }

    /** {@code users.household_size} of a user (null = not set). */
    public void householdSize(long userId, Integer size) {
        jdbc.update("UPDATE users SET household_size = ? WHERE id = ?", size, userId);
    }

    public long plan(long userId, LocalDate weekStart) {
        return jdbc.queryForObject("INSERT INTO meal_plans (user_id, week_start) VALUES (?, ?) RETURNING id",
                Long.class, userId, weekStart);
    }

    /** A MANUAL item; {@code recipeName} is the snapshot. */
    public long item(long planId, DayOfWeek day, String meal, Long recipeId, String recipeName, int servings) {
        return item(planId, day, meal, recipeId, recipeName, servings, "MANUAL");
    }

    public long item(long planId, DayOfWeek day, String meal, Long recipeId, String recipeName, int servings,
                     String source) {
        return item(planId, day, meal, 0, recipeId, recipeName, servings, source);
    }

    /** An item at {@code position} (0–3) of its meal. */
    public long item(long planId, DayOfWeek day, String meal, int position, Long recipeId, String recipeName,
                     int servings, String source) {
        return jdbc.queryForObject("""
                INSERT INTO meal_plan_items (meal_plan_id, day_of_week, meal, position, recipe_id,
                                             recipe_name_snapshot, servings, source)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, planId, day.getValue(), meal, position, recipeId, recipeName, servings, source);
    }
}
