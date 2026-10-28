-- Recipes and their parts (spec 060, 020; full-schema.md 4.5, draft-recipes-ingredients.md 4.1–4.9).

CREATE TABLE recipes (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    author_id          BIGINT REFERENCES users ON DELETE SET NULL,
    title              VARCHAR(100),
    description        VARCHAR(500),
    cuisine_id         BIGINT REFERENCES cuisines ON DELETE RESTRICT,
    category_id        BIGINT REFERENCES categories ON DELETE RESTRICT,
    difficulty         VARCHAR(10) CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    prep_time_minutes  SMALLINT CHECK (prep_time_minutes BETWEEN 0 AND 1440),
    cook_time_minutes  SMALLINT CHECK (cook_time_minutes BETWEEN 0 AND 1440),
    total_time_minutes SMALLINT GENERATED ALWAYS AS (COALESCE(prep_time_minutes, 0) + COALESCE(cook_time_minutes, 0)) STORED,
    servings           SMALLINT CHECK (servings BETWEEN 1 AND 50),
    -- Meal suitability (030, 031). No information means lunch + dinner.
    for_breakfast      BOOLEAN     NOT NULL DEFAULT false,
    for_lunch          BOOLEAN     NOT NULL DEFAULT true,
    for_dinner         BOOLEAN     NOT NULL DEFAULT true,
    is_snack           BOOLEAN     NOT NULL DEFAULT false,
    video_url          VARCHAR(500),
    video_platform     VARCHAR(10) CHECK (video_platform IN ('YOUTUBE', 'TIKTOK')),
    status             VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'PENDING', 'PUBLISHED', 'REJECTED', 'REVISION_REQUESTED', 'ARCHIVED')),
    last_wizard_step   SMALLINT    NOT NULL DEFAULT 1 CHECK (last_wizard_step BETWEEN 1 AND 6),
    rating_avg         NUMERIC(3, 2) NOT NULL DEFAULT 0,
    review_count       INTEGER     NOT NULL DEFAULT 0,
    featured_position  SMALLINT,
    -- Written by the recipe service from title, tags, ingredient names and description (draft 4.2).
    search_vector      TSVECTOR,
    source_name        VARCHAR(50),
    source_url         VARCHAR(500),
    imported_at        TIMESTAMPTZ,
    version            INTEGER     NOT NULL DEFAULT 0,
    submitted_at       TIMESTAMPTZ,
    published_at       TIMESTAMPTZ,
    archived_at        TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_recipes_video CHECK ((video_url IS NULL) = (video_platform IS NULL)),
    CONSTRAINT chk_recipes_snack CHECK (NOT is_snack OR NOT (for_breakfast OR for_lunch OR for_dinner)),
    -- prep_time is optional: 129 of the first 1,000 scraped recipes had none.
    CONSTRAINT chk_recipes_complete CHECK (
        status = 'DRAFT' OR (
            title IS NOT NULL AND char_length(title) >= 5
            AND cuisine_id IS NOT NULL AND category_id IS NOT NULL
            AND difficulty IS NOT NULL AND servings IS NOT NULL
            AND cook_time_minutes IS NOT NULL AND total_time_minutes > 0
        )
    ),
    CONSTRAINT chk_recipes_published_at CHECK (status NOT IN ('PUBLISHED', 'ARCHIVED') OR published_at IS NOT NULL)
);
CREATE UNIQUE INDEX uq_recipes_source_url ON recipes (source_url) WHERE source_url IS NOT NULL;
CREATE UNIQUE INDEX uq_recipes_featured ON recipes (featured_position) WHERE featured_position IS NOT NULL;
CREATE INDEX idx_recipes_published ON recipes (published_at DESC, id DESC) WHERE status = 'PUBLISHED';
CREATE INDEX idx_recipes_search ON recipes USING gin (search_vector) WHERE status = 'PUBLISHED';
CREATE INDEX idx_recipes_author_status ON recipes (author_id, status, updated_at DESC);
CREATE INDEX idx_recipes_filter ON recipes (cuisine_id, category_id, difficulty, total_time_minutes)
    WHERE status = 'PUBLISHED';
CREATE INDEX idx_recipes_rating ON recipes (rating_avg DESC, review_count DESC) WHERE status = 'PUBLISHED';
CREATE INDEX idx_recipes_pending ON recipes (submitted_at) WHERE status = 'PENDING';
-- Meal picker (030) and the AI pre-filter (031).
CREATE INDEX idx_recipes_meals ON recipes (for_breakfast, for_lunch, for_dinner)
    WHERE status = 'PUBLISHED' AND NOT is_snack;

CREATE TABLE recipe_ingredients (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipe_id     BIGINT      NOT NULL REFERENCES recipes ON DELETE CASCADE,
    group_name    VARCHAR(60),
    ingredient_id BIGINT      NOT NULL REFERENCES ingredients ON DELETE RESTRICT,
    quantity      NUMERIC(10, 3) CHECK (quantity > 0),
    unit_id       BIGINT      NOT NULL REFERENCES units ON DELETE RESTRICT,
    amount_text   VARCHAR(50),
    note          VARCHAR(100),
    is_optional   BOOLEAN     NOT NULL DEFAULT false,
    position      SMALLINT    NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (recipe_id, position)
);
-- Allergen / excluded-ingredient rules use NOT EXISTS on this index.
CREATE INDEX idx_recipe_ingredients_ingredient ON recipe_ingredients (ingredient_id, recipe_id);

CREATE TABLE recipe_steps (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipe_id     BIGINT      NOT NULL REFERENCES recipes ON DELETE CASCADE,
    position      SMALLINT    NOT NULL,
    title         VARCHAR(100),
    content       VARCHAR(3000),
    timer_seconds INTEGER CHECK (timer_seconds BETWEEN 10 AND 86400),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (recipe_id, position)
);

CREATE TABLE recipe_media (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipe_id    BIGINT      NOT NULL REFERENCES recipes ON DELETE CASCADE,
    step_id      BIGINT REFERENCES recipe_steps ON DELETE CASCADE,
    role         VARCHAR(10) NOT NULL CHECK (role IN ('COVER', 'STEP')),
    position     SMALLINT    NOT NULL DEFAULT 0,
    storage_key  VARCHAR(255),
    external_url VARCHAR(500),
    content_type VARCHAR(50),
    size_bytes   INTEGER CHECK (size_bytes > 0),
    uploaded_by  BIGINT REFERENCES users ON DELETE SET NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_recipe_media_source CHECK (num_nonnulls(storage_key, external_url) = 1),
    CONSTRAINT chk_recipe_media_upload CHECK (storage_key IS NULL OR (content_type IS NOT NULL AND size_bytes IS NOT NULL)),
    CONSTRAINT chk_recipe_media_step CHECK ((role = 'STEP') = (step_id IS NOT NULL))
);
CREATE UNIQUE INDEX uq_recipe_media_cover ON recipe_media (recipe_id) WHERE role = 'COVER';
CREATE UNIQUE INDEX uq_recipe_media_step_position ON recipe_media (step_id, position) WHERE role = 'STEP';

CREATE TABLE recipe_tags (
    recipe_id  BIGINT      NOT NULL REFERENCES recipes ON DELETE CASCADE,
    tag_id     BIGINT      NOT NULL REFERENCES tags ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (recipe_id, tag_id)
);
CREATE INDEX idx_recipe_tags_tag ON recipe_tags (tag_id);
