-- ============================================================================
-- Migration: V501__community.sql
-- Module: 070-review-cooksnap
-- Standards: PostgreSQL 16, full-schema.md §4.9, Constitution III & IV
-- ============================================================================

-- 1. Table: reviews
CREATE TABLE reviews (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipe_id        BIGINT       NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    user_id          BIGINT       NULL REFERENCES users(id) ON DELETE SET NULL,
    rating           SMALLINT     NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment          VARCHAR(2000) NULL,  -- API requires 10-2000 chars (FR-004)
    substitution_tip VARCHAR(500)  NULL,
    status           VARCHAR(10)  NOT NULL DEFAULT 'VISIBLE' CHECK (status IN ('VISIBLE', 'HIDDEN')),
    helpful_count    INTEGER      NOT NULL DEFAULT 0 CHECK (helpful_count >= 0),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Partial Unique Index: 1 review row per user per recipe (any status).
-- Owner deletion is a hard DELETE so the user can review again; a review HIDDEN by moderation keeps blocking.
CREATE UNIQUE INDEX uq_reviews_recipe_user 
    ON reviews(recipe_id, user_id) 
    WHERE user_id IS NOT NULL;

-- Performance Indexes for filtering, pagination, and multi-criteria sorting
CREATE INDEX idx_reviews_recipe_lookup 
    ON reviews(recipe_id, created_at DESC) 
    WHERE status = 'VISIBLE';

CREATE INDEX idx_reviews_recipe_rating 
    ON reviews(recipe_id, rating DESC, created_at DESC) 
    WHERE status = 'VISIBLE';

CREATE INDEX idx_reviews_recipe_helpful 
    ON reviews(recipe_id, helpful_count DESC, created_at DESC) 
    WHERE status = 'VISIBLE';

CREATE INDEX idx_reviews_user_lookup 
    ON reviews(user_id, created_at DESC) 
    WHERE user_id IS NOT NULL;

-- 2. Table: cooksnaps
CREATE TABLE cooksnaps (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipe_id    BIGINT       NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    review_id    BIGINT       NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    user_id      BIGINT       NULL REFERENCES users(id) ON DELETE SET NULL,
    storage_key  VARCHAR(255) NOT NULL CONSTRAINT uq_cooksnaps_storage_key UNIQUE,
    content_type VARCHAR(50)  NOT NULL CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),
    size_bytes   INTEGER      NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 5242880),
    caption      VARCHAR(300) NULL,
    status       VARCHAR(10)  NOT NULL DEFAULT 'VISIBLE' CHECK (status IN ('VISIBLE', 'HIDDEN')),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cooksnaps_review 
    ON cooksnaps(review_id) 
    WHERE status = 'VISIBLE';

CREATE INDEX idx_cooksnaps_recipe 
    ON cooksnaps(recipe_id, created_at DESC) 
    WHERE status = 'VISIBLE';

CREATE INDEX idx_cooksnaps_user 
    ON cooksnaps(user_id) 
    WHERE user_id IS NOT NULL;

-- 3. Table: review_helpful_votes
CREATE TABLE review_helpful_votes (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    review_id  BIGINT      NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    user_id    BIGINT      NULL REFERENCES users(id) ON DELETE SET NULL,  -- keep vote anonymously so helpful_count = COUNT(*)
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_review_helpful_votes UNIQUE (review_id, user_id)
);

CREATE INDEX idx_review_helpful_votes_user 
    ON review_helpful_votes(user_id, review_id)
    WHERE user_id IS NOT NULL;

-- 4. Table: reports (Shared moderation table, full-schema §4.10, spec 100 & 070)
CREATE TABLE IF NOT EXISTS reports (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reporter_id      BIGINT       NULL REFERENCES users(id) ON DELETE SET NULL,
    recipe_id        BIGINT       NULL REFERENCES recipes(id) ON DELETE CASCADE,
    review_id        BIGINT       NULL REFERENCES reviews(id) ON DELETE CASCADE,
    cooksnap_id      BIGINT       NULL REFERENCES cooksnaps(id) ON DELETE CASCADE,
    reported_user_id BIGINT       NULL REFERENCES users(id) ON DELETE CASCADE,
    reason           VARCHAR(15)  NOT NULL CHECK (reason IN ('SPAM', 'OFFENSIVE', 'COPYRIGHT', 'UNSAFE', 'OTHER')),
    details          VARCHAR(1000) NULL,
    status           VARCHAR(10)  NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'RESOLVED', 'DISMISSED')),
    handled_by       BIGINT       NULL REFERENCES users(id) ON DELETE SET NULL,
    handled_at       TIMESTAMPTZ  NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_reports_single_target CHECK (num_nonnulls(recipe_id, review_id, cooksnap_id, reported_user_id) = 1)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_reports_review_reporter 
    ON reports(review_id, reporter_id) 
    WHERE review_id IS NOT NULL AND reporter_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_reports_cooksnap_reporter 
    ON reports(cooksnap_id, reporter_id) 
    WHERE cooksnap_id IS NOT NULL AND reporter_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_reports_status_created 
    ON reports(status, created_at);

-- 5. No updated_at trigger on purpose: updated_at means "last edited by the author"
--    and is set by the application (Review.editContent()). Helpful-vote recounts and
--    moderation status changes must not change it.

-- 6. Comments on tables and columns for self-documenting schema
COMMENT ON TABLE reviews IS 'Community reviews and star ratings on recipes';
COMMENT ON COLUMN reviews.status IS 'VISIBLE, or HIDDEN by moderation/auto-quarantine (owner deletion is a hard delete)';
COMMENT ON COLUMN reviews.updated_at IS 'Last edit by the review author (not changed by votes or moderation)';
COMMENT ON COLUMN reviews.substitution_tip IS 'Optional tip explaining ingredient replacement';
COMMENT ON TABLE cooksnaps IS 'User-uploaded proof-of-cooking images attached to reviews';
COMMENT ON COLUMN cooksnaps.storage_key IS 'Relative file storage key (e.g. cs_12_45_uuid.webp)';
COMMENT ON TABLE review_helpful_votes IS 'Helpful votes cast by authenticated users on reviews';
COMMENT ON TABLE reports IS 'Shared user moderation reports queue (spec 100 & 070)';


