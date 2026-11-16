-- Spec 010 changes to the V101 tables plus blocked emails and the signed-in password counter
-- (data-model.md, research R2, R11, R16, R17).

-- Blocked Email (FR-026): HMAC-SHA256 of the normalized email, never the email itself.
CREATE TABLE blocked_emails (
    email_hash CHAR(64)    PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Wrong passwords while already signed in, kept apart from login_attempts (FR-022b, R17).
CREATE TABLE password_check_failures (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_password_check_failures_user ON password_check_failures (user_id, created_at DESC);

-- Role-change notice shown at the next request (FR-024a): the role before the change, NULL = nothing to show.
ALTER TABLE users
    ADD COLUMN pending_role_notice_from VARCHAR(15)
        CHECK (pending_role_notice_from IN ('MEMBER', 'CONTRIBUTOR', 'MODERATOR', 'ADMINISTRATOR'));
-- UnverifiedAccountPurgeJob (FR-005, 7 days)
CREATE INDEX idx_users_unverified_created ON users (created_at) WHERE email_verified_at IS NULL;

-- Refresh-token rotation and re-authentication (R2, FR-022a).
ALTER TABLE user_sessions
    ADD COLUMN previous_token_hash  CHAR(64),
    ADD COLUMN rotated_at           TIMESTAMPTZ,
    ADD COLUMN reauthenticated_at   TIMESTAMPTZ;
CREATE INDEX idx_user_sessions_previous_token ON user_sessions (previous_token_hash)
    WHERE previous_token_hash IS NOT NULL;
CREATE INDEX idx_user_sessions_expires ON user_sessions (expires_at);

-- login_attempts is only the lockout counter and keeps the hash of the email, even for unknown emails (R4, NFR-003).
-- No code wrote to it before spec 010, so the rows can go.
DELETE FROM login_attempts;
ALTER TABLE login_attempts
    DROP COLUMN email_normalized,
    ADD COLUMN email_hash CHAR(64) NOT NULL;
CREATE INDEX idx_login_attempts_email ON login_attempts (email_hash, created_at DESC);
CREATE INDEX idx_login_attempts_ip ON login_attempts (ip_address, created_at DESC);

-- Closure link for a banned user (R16).
ALTER TABLE one_time_tokens DROP CONSTRAINT one_time_tokens_purpose_check;
ALTER TABLE one_time_tokens
    ADD CONSTRAINT one_time_tokens_purpose_check
        CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET', 'ACCOUNT_CLOSURE'));
