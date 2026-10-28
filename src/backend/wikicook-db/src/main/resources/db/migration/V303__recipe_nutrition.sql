-- AI nutrition estimate per recipe (spec 090; data-model.md section 1). No row = never estimated.

CREATE TABLE recipe_nutrition (
    recipe_id            BIGINT       PRIMARY KEY REFERENCES recipes ON DELETE CASCADE,
    status               VARCHAR(12)  NOT NULL CHECK (status IN ('CALCULATING', 'AVAILABLE', 'FAILED')),
    kcal                 SMALLINT CHECK (kcal BETWEEN 0 AND 3000),
    protein_g            SMALLINT CHECK (protein_g BETWEEN 0 AND 300),
    carbs_g              SMALLINT CHECK (carbs_g BETWEEN 0 AND 300),
    fat_g                SMALLINT CHECK (fat_g BETWEEN 0 AND 300),
    -- Display only (FR-019), never used for filtering.
    diet_labels          VARCHAR(15)[] NOT NULL DEFAULT '{}'
        CHECK (diet_labels <@ ARRAY ['GLUTEN_FREE','DAIRY_FREE','NUT_FREE','LOW_SODIUM','VEGETARIAN','VEGAN']::VARCHAR[]),
    low_confidence       BOOLEAN      NOT NULL DEFAULT false,
    excluded_ingredients JSONB,
    input_fingerprint    CHAR(64)     NOT NULL,
    last_error           VARCHAR(30),
    is_hidden            BOOLEAN      NOT NULL DEFAULT false,
    hidden_reason        VARCHAR(300),
    hidden_by            BIGINT REFERENCES users ON DELETE SET NULL,
    computed_at          TIMESTAMPTZ,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_recipe_nutrition_hidden CHECK (NOT is_hidden OR hidden_reason IS NOT NULL),
    CONSTRAINT chk_recipe_nutrition_available CHECK (
        status <> 'AVAILABLE'
        OR (kcal IS NOT NULL AND protein_g IS NOT NULL AND carbs_g IS NOT NULL AND fat_g IS NOT NULL)
    )
);
-- Calorie filter of 020.
CREATE INDEX idx_recipe_nutrition_kcal ON recipe_nutrition (kcal) WHERE status = 'AVAILABLE' AND NOT is_hidden;
CREATE INDEX idx_recipe_nutrition_status ON recipe_nutrition (status);
