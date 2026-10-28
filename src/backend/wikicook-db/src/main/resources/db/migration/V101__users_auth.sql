-- Accounts and sign-in (spec 010, 011; full-schema.md 4.1).

CREATE TABLE users (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email                 VARCHAR(254) NOT NULL UNIQUE,
    password_hash         VARCHAR(100),
    google_sub            VARCHAR(255) UNIQUE,
    display_name          VARCHAR(50)  NOT NULL,
    avatar_storage_key    VARCHAR(255),
    role                  VARCHAR(15)  NOT NULL DEFAULT 'MEMBER'
        CHECK (role IN ('MEMBER', 'CONTRIBUTOR', 'MODERATOR', 'ADMINISTRATOR')),
    status                VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'BANNED')),
    email_verified_at     TIMESTAMPTZ,
    locked_until          TIMESTAMPTZ,
    last_sign_in_at       TIMESTAMPTZ,
    diet_type             VARCHAR(15)  NOT NULL DEFAULT 'NONE'
        CHECK (diet_type IN ('NONE', 'VEGETARIAN', 'VEGAN', 'PESCATARIAN')),
    household_size        SMALLINT CHECK (household_size BETWEEN 1 AND 20),
    max_cook_time_minutes SMALLINT CHECK (max_cook_time_minutes >= 10),
    is_system             BOOLEAN      NOT NULL DEFAULT false,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_users_email_normalized CHECK (email = lower(trim(email))),
    CONSTRAINT chk_users_system_no_credentials
        CHECK (NOT is_system OR (password_hash IS NULL AND google_sub IS NULL))
);

-- Refresh-token sessions (010 FR-009, FR-016).
CREATE TABLE user_sessions (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users ON DELETE CASCADE,
    token_hash       CHAR(64)     NOT NULL UNIQUE,
    remember_me      BOOLEAN      NOT NULL,
    last_activity_at TIMESTAMPTZ  NOT NULL,
    expires_at       TIMESTAMPTZ  NOT NULL,
    revoked_at       TIMESTAMPTZ,
    user_agent       VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_user_sessions_active ON user_sessions (user_id) WHERE revoked_at IS NULL;

-- Email verification and password reset links (010 FR-005, FR-015).
CREATE TABLE one_time_tokens (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users ON DELETE CASCADE,
    purpose    VARCHAR(20) NOT NULL CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')),
    token_hash CHAR(64)    NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_one_time_tokens_user ON one_time_tokens (user_id, purpose, created_at DESC);

-- Sign-in attempts for lockout and audit (010 FR-012, FR-025). Rows older than 90 days are purged.
CREATE TABLE login_attempts (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          BIGINT REFERENCES users ON DELETE CASCADE,
    email_normalized VARCHAR(254) NOT NULL,
    success          BOOLEAN      NOT NULL,
    ip_address       INET,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_login_attempts_user ON login_attempts (user_id, created_at DESC);
