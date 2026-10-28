-- Edits of published recipes and the moderation trail (spec 060 FR-024, FR-027, FR-029; draft 4.11–4.12).

CREATE TABLE recipe_revisions (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipe_id    BIGINT      NOT NULL REFERENCES recipes ON DELETE CASCADE,
    status       VARCHAR(20) NOT NULL
        CHECK (status IN ('DRAFT', 'PENDING', 'REVISION_REQUESTED', 'REJECTED', 'APPROVED')),
    content      JSONB       NOT NULL,
    version      INTEGER     NOT NULL DEFAULT 0,
    submitted_at TIMESTAMPTZ,
    decided_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- At most one open revision per recipe.
CREATE UNIQUE INDEX uq_recipe_revisions_open ON recipe_revisions (recipe_id)
    WHERE status IN ('DRAFT', 'PENDING', 'REVISION_REQUESTED');

CREATE TABLE recipe_status_history (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipe_id   BIGINT      NOT NULL REFERENCES recipes ON DELETE CASCADE,
    revision_id BIGINT REFERENCES recipe_revisions ON DELETE CASCADE,
    from_status VARCHAR(20),
    to_status   VARCHAR(20) NOT NULL,
    actor_id    BIGINT REFERENCES users ON DELETE SET NULL,
    message     VARCHAR(1000),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_recipe_status_history_message
        CHECK (to_status NOT IN ('REJECTED', 'REVISION_REQUESTED') OR message IS NOT NULL)
);
CREATE INDEX idx_recipe_status_history_recipe ON recipe_status_history (recipe_id, created_at DESC);
