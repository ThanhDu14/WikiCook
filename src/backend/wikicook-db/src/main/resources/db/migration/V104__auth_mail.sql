-- Email rate limit counter and the outbox for notification emails (spec 010 FR-012, FR-017, FR-024a; research R5, R8).

-- Counts sends per email hash, also for emails with no account, so a 429 does not reveal whether one exists (FR-017).
CREATE TABLE email_send_requests (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email_hash CHAR(64)    NOT NULL,
    purpose    VARCHAR(20) NOT NULL CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_email_send_requests_lookup ON email_send_requests (email_hash, purpose, created_at DESC);

-- Emails without a link (lock, suspension, ban...). The address is read from users at send time.
CREATE TABLE email_outbox (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT      NOT NULL REFERENCES users ON DELETE CASCADE,
    template        VARCHAR(30) NOT NULL CHECK (template IN
        ('ACCOUNT_LOCKED', 'ACCOUNT_SUSPENDED', 'ACCOUNT_BANNED', 'SUSPENSION_ENDED', 'BAN_LIFTED')),
    -- Reason and dates; never a token.
    payload         JSONB       NOT NULL,
    attempts        SMALLINT    NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    sent_at         TIMESTAMPTZ,
    failed_at       TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_email_outbox_due ON email_outbox (next_attempt_at)
    WHERE sent_at IS NULL AND failed_at IS NULL;
