-- 030 FR-004 (Clarifications 2026-11-02): a meal holds 0–4 dishes, ordered by position.
-- V301 allowed one dish per (plan, day, meal); existing rows become the first dish of their meal.

ALTER TABLE meal_plan_items
    DROP CONSTRAINT meal_plan_items_meal_plan_id_day_of_week_meal_key;

ALTER TABLE meal_plan_items
    ADD COLUMN position SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT meal_plan_items_position_check CHECK (position BETWEEN 0 AND 3);

-- Deferred so moves and removals can renumber dishes inside one transaction (research R5).
ALTER TABLE meal_plan_items
    ADD CONSTRAINT meal_plan_items_meal_position_key
        UNIQUE (meal_plan_id, day_of_week, meal, position) DEFERRABLE INITIALLY DEFERRED;
