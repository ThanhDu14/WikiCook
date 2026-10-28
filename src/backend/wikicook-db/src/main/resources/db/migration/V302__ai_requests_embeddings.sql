-- AI usage log and recipe embeddings (spec 031; data-model.md section 1, research R18).

CREATE TABLE recipe_embeddings (
    recipe_id    BIGINT      PRIMARY KEY REFERENCES recipes ON DELETE CASCADE,
    embedding    VECTOR(768) NOT NULL,
    model        VARCHAR(50) NOT NULL,
    content_hash CHAR(64)    NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_recipe_embeddings_hnsw ON recipe_embeddings USING hnsw (embedding vector_cosine_ops);

-- Every AI call of 031 and 090; quotas are COUNT queries on this table.
CREATE TABLE ai_requests (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    feature         VARCHAR(15)  NOT NULL CHECK (feature IN ('MEAL_PLANNER', 'NUTRITION')),
    user_id         BIGINT REFERENCES users ON DELETE CASCADE,
    recipe_id       BIGINT REFERENCES recipes ON DELETE CASCADE,
    actor_id        BIGINT REFERENCES users ON DELETE SET NULL,
    trigger         VARCHAR(15)  NOT NULL
        CHECK (trigger IN ('USER', 'APPROVAL', 'REVISION', 'IMPORT', 'ADMIN_RECALC')),
    outcome         VARCHAR(15)  NOT NULL
        CHECK (outcome IN ('AI_DRAFT', 'AUTO_DRAFT', 'NOT_ENOUGH', 'SUCCESS', 'FAILED', 'REJECTED',
                           'TIMEOUT', 'CANCELLED', 'QUOTA_CAP')),
    called_llm      BOOLEAN      NOT NULL,
    counted         BOOLEAN      NOT NULL,
    week_start      DATE,
    requested_slots SMALLINT,
    removed_picks   SMALLINT,
    constraints     JSONB,
    reason          VARCHAR(100),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_ai_requests_counted CHECK (counted = (feature = 'MEAL_PLANNER' AND outcome = 'AI_DRAFT')),
    CONSTRAINT chk_ai_requests_meal_planner
        CHECK (feature <> 'MEAL_PLANNER' OR (user_id IS NOT NULL AND week_start IS NOT NULL)),
    CONSTRAINT chk_ai_requests_nutrition CHECK (feature <> 'NUTRITION' OR recipe_id IS NOT NULL)
);
-- 5 generations per user in a rolling 60 minutes.
CREATE INDEX idx_ai_requests_user_counted ON ai_requests (user_id, created_at DESC) WHERE counted;
-- Daily cap per feature.
CREATE INDEX idx_ai_requests_feature_llm ON ai_requests (feature, created_at) WHERE called_llm;
-- Nutrition history for admins (090 FR-023).
CREATE INDEX idx_ai_requests_recipe ON ai_requests (recipe_id, created_at DESC);
