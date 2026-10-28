-- Weekly meal planner (spec 030; data-model.md section 1).

CREATE TABLE meal_plans (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users ON DELETE CASCADE,
    week_start DATE        NOT NULL CHECK (EXTRACT(ISODOW FROM week_start) = 1),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, week_start)
);

CREATE TABLE meal_plan_items (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    meal_plan_id         BIGINT       NOT NULL REFERENCES meal_plans ON DELETE CASCADE,
    day_of_week          SMALLINT     NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    meal                 VARCHAR(10)  NOT NULL CHECK (meal IN ('BREAKFAST', 'LUNCH', 'DINNER')),
    recipe_id            BIGINT REFERENCES recipes ON DELETE SET NULL,
    recipe_name_snapshot VARCHAR(100) NOT NULL,
    servings             SMALLINT     NOT NULL CHECK (servings BETWEEN 1 AND 20),
    source               VARCHAR(10)  NOT NULL CHECK (source IN ('MANUAL', 'AI', 'AUTO')),
    version              INTEGER      NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (meal_plan_id, day_of_week, meal)
);
CREATE INDEX idx_meal_plan_items_recipe ON meal_plan_items (recipe_id);
