-- Spec 011 (user profile and dietary profile): what V101 and V210 do not have yet
-- (specs/011-user-profile-dietary/data-model.md, research R2, R5, R13, R15).
-- Lower than V403 that already ran on shared databases, hence spring.flyway.out-of-order (like V102-V104).

-- FR-003 bio; two versions so that saving the avatar does not report a false conflict to the dietary form (R5).
ALTER TABLE users
    ADD COLUMN bio             VARCHAR(300),
    ADD COLUMN profile_version INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN dietary_version INTEGER NOT NULL DEFAULT 0;

-- FR-015: 10-240 minutes. V101 only had ">= 10"; its auto-generated name is users_max_cook_time_minutes_check.
ALTER TABLE users DROP CONSTRAINT users_max_cook_time_minutes_check;
ALTER TABLE users
    ADD CONSTRAINT chk_users_max_cook_time CHECK (max_cook_time_minutes BETWEEN 10 AND 240);

-- FR-019: shown to the owner at the next visit; the name is stored because the catalogue row may be gone (R15).
CREATE TABLE dietary_notices (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users ON DELETE CASCADE,
    kind             VARCHAR(20)  NOT NULL
        CHECK (kind IN ('INGREDIENT_MERGED', 'INGREDIENT_REMOVED', 'ALLERGEN_REMOVED')),
    subject_name     VARCHAR(100) NOT NULL,
    replacement_name VARCHAR(100),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_dietary_notices_user ON dietary_notices (user_id, created_at DESC);

-- NFR-008: 20 writes per hour per kind, counted here instead of in Redis (R13). The limiter deletes the rows of the
-- person it counts once they are an hour old, so there is no cleanup job.
CREATE TABLE profile_write_requests (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users ON DELETE CASCADE,
    kind       VARCHAR(15) NOT NULL CHECK (kind IN ('PROFILE_SAVE', 'AVATAR_UPLOAD')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_profile_write_requests_user ON profile_write_requests (user_id, kind, created_at DESC);

-- FR-009: the fixed allergen list. Nothing else seeds it (research, table at the end, row 18).
INSERT INTO allergens (code, name_vi) VALUES
    ('GLUTEN',     'Gluten'),
    ('PEANUT',     'Đậu phộng'),
    ('TREE_NUT',   'Các loại hạt cây'),
    ('MILK',       'Sữa'),
    ('EGG',        'Trứng'),
    ('FISH',       'Cá'),
    ('CRUSTACEAN', 'Giáp xác (tôm, cua)'),
    ('MOLLUSC',    'Nhuyễn thể (mực, sò, hến)'),
    ('SOY',        'Đậu nành'),
    ('SESAME',     'Mè (vừng)')
ON CONFLICT (code) DO NOTHING;
