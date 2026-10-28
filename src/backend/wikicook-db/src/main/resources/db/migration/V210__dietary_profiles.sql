-- Dietary profile lists (spec 011; full-schema.md 4.1). Numbered after V201 because it needs ingredients.

CREATE TABLE user_allergens (
    user_id     BIGINT      NOT NULL REFERENCES users ON DELETE CASCADE,
    allergen_id BIGINT      NOT NULL REFERENCES allergens ON DELETE RESTRICT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, allergen_id)
);

CREATE TABLE user_excluded_ingredients (
    user_id       BIGINT      NOT NULL REFERENCES users ON DELETE CASCADE,
    ingredient_id BIGINT      NOT NULL REFERENCES ingredients ON DELETE RESTRICT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, ingredient_id)
);

-- Used by the AI planner (031 FR-002).
CREATE TABLE user_preferred_cuisines (
    user_id    BIGINT      NOT NULL REFERENCES users ON DELETE CASCADE,
    cuisine_id BIGINT      NOT NULL REFERENCES cuisines ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, cuisine_id)
);
