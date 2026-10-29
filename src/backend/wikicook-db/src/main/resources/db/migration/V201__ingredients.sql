-- Shared ingredient catalogue (spec 060, 011, 040; full-schema.md 4.4).

CREATE TABLE ingredients (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name              VARCHAR(100) NOT NULL,
    name_normalized   VARCHAR(100) NOT NULL,
    -- Source for vegetarian/vegan rules (031 FR-009a, 090 FR-011). Fish sauce and shrimp paste are SEAFOOD, lard is MEAT.
    kind              VARCHAR(10)  NOT NULL
        CHECK (kind IN ('MEAT', 'SEAFOOD', 'EGG', 'DAIRY', 'HONEY', 'PLANT', 'OTHER')),
    status            VARCHAR(10)  NOT NULL DEFAULT 'APPROVED'
        CHECK (status IN ('APPROVED', 'PROPOSED', 'REJECTED')),
    proposed_by       BIGINT REFERENCES users ON DELETE SET NULL,
    aisle_category_id BIGINT REFERENCES aisle_categories ON DELETE SET NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_ingredients_name_normalized CHECK (name_normalized = f_unaccent(lower(trim(name))))
);
CREATE UNIQUE INDEX uq_ingredients_approved_name ON ingredients (name_normalized) WHERE status = 'APPROVED';
CREATE INDEX idx_ingredients_name_trgm ON ingredients USING gin (name_normalized gin_trgm_ops);

-- Single source of truth for allergen filtering (020, 031) and warnings (090).
CREATE TABLE ingredient_allergens (
    ingredient_id BIGINT      NOT NULL REFERENCES ingredients ON DELETE CASCADE,
    allergen_id   BIGINT      NOT NULL REFERENCES allergens ON DELETE RESTRICT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (ingredient_id, allergen_id)
);
CREATE INDEX idx_ingredient_allergens_allergen ON ingredient_allergens (allergen_id);
