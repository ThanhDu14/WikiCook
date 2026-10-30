-- Shopping lists and ingredient aggregation (spec 040; full-schema.md 4.8).

CREATE TABLE shopping_lists (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(100) NOT NULL DEFAULT 'Danh sách mua sắm',
    status      VARCHAR(10) NOT NULL CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    archived_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Exactly one active shopping list per user (040 FR-001)
CREATE UNIQUE INDEX uq_shopping_lists_active_user ON shopping_lists (user_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_shopping_lists_user_status ON shopping_lists (user_id, status);

CREATE TABLE shopping_list_items (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    list_id            BIGINT NOT NULL REFERENCES shopping_lists(id) ON DELETE CASCADE,
    client_uuid        UUID NOT NULL UNIQUE,
    ingredient_id      BIGINT REFERENCES ingredients(id) ON DELETE RESTRICT,
    custom_name        VARCHAR(100),
    quantity           NUMERIC(10,3),
    unit_id            BIGINT REFERENCES units(id) ON DELETE RESTRICT,
    aisle_category_id  BIGINT NOT NULL REFERENCES aisle_categories(id) ON DELETE RESTRICT,
    is_checked         BOOLEAN NOT NULL DEFAULT false,
    is_cleared         BOOLEAN NOT NULL DEFAULT false,
    is_manually_edited BOOLEAN NOT NULL DEFAULT false,
    cleared_at         TIMESTAMPTZ,
    version            INTEGER NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_shopping_list_items_name_or_ing CHECK (ingredient_id IS NOT NULL OR (custom_name IS NOT NULL AND length(trim(custom_name)) > 0))
);

CREATE INDEX idx_shopping_list_items_list_id ON shopping_list_items (list_id);
CREATE INDEX idx_shopping_list_items_ingredient ON shopping_list_items (list_id, ingredient_id) WHERE is_cleared = false;

CREATE TABLE shopping_list_item_sources (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    item_id               BIGINT NOT NULL REFERENCES shopping_list_items(id) ON DELETE CASCADE,
    recipe_id             BIGINT REFERENCES recipes(id) ON DELETE SET NULL,
    meal_plan_item_id     BIGINT REFERENCES meal_plan_items(id) ON DELETE SET NULL,
    recipe_title_snapshot VARCHAR(100) NOT NULL,
    contributed_quantity  NUMERIC(10,3),
    unit_id               BIGINT REFERENCES units(id) ON DELETE RESTRICT,
    scaled_servings       INTEGER NOT NULL DEFAULT 1,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_shopping_list_item_sources_item ON shopping_list_item_sources (item_id);

CREATE TABLE user_aisle_preferences (
    user_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ingredient_id     BIGINT NOT NULL REFERENCES ingredients(id) ON DELETE CASCADE,
    aisle_category_id BIGINT NOT NULL REFERENCES aisle_categories(id) ON DELETE RESTRICT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, ingredient_id)
);
