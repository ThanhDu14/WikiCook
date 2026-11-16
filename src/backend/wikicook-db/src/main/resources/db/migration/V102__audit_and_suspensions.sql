-- Audit log and account sanctions (spec 010 FR-013, FR-024, FR-025; data-model.md, research R11).
-- Moved here from V503 so 010 can run before the v1.0 moderation tables; spec 100 owns the business rules.

CREATE TABLE audit_logs (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- No foreign key on actor_id and target_id: ON DELETE SET NULL would run an UPDATE when a user is deleted
    -- (spec 011, 7-day purge, Google takeover) and the append-only trigger below rejects every UPDATE.
    actor_id    BIGINT,
    action      VARCHAR(50)  NOT NULL,
    target_type VARCHAR(30),
    target_id   BIGINT,
    -- Never holds an email or a display name (NFR-009).
    details     JSONB,
    ip_address  INET,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_created ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_logs_target ON audit_logs (target_type, target_id);

CREATE FUNCTION audit_logs_reject_change() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION 'audit_logs is append-only and cannot be updated or deleted';
END;
$$;

CREATE TRIGGER trg_audit_logs_append_only
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION audit_logs_reject_change();

CREATE TABLE user_suspensions (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users ON DELETE CASCADE,
    kind        VARCHAR(10)  NOT NULL CHECK (kind IN ('SUSPENSION', 'BAN')),
    reason      VARCHAR(500) NOT NULL,
    starts_at   TIMESTAMPTZ  NOT NULL,
    ends_at     TIMESTAMPTZ,
    created_by  BIGINT REFERENCES users ON DELETE SET NULL,
    lifted_at   TIMESTAMPTZ,
    -- Only who lifted it; do not infer how the sanction ended from it (the FK is nulled with the account).
    lifted_by   BIGINT REFERENCES users ON DELETE SET NULL,
    lift_reason VARCHAR(20) CHECK (lift_reason IN ('EXPIRED', 'LIFTED', 'REPLACED_BY_BAN')),
    CONSTRAINT chk_user_suspensions_ends_at CHECK ((kind = 'BAN') = (ends_at IS NULL)),
    CONSTRAINT chk_user_suspensions_lift CHECK ((lifted_at IS NULL) = (lift_reason IS NULL))
);
-- At most one running sanction per account.
CREATE UNIQUE INDEX uq_user_suspensions_running ON user_suspensions (user_id) WHERE lifted_at IS NULL;
-- SuspensionExpiryJob
CREATE INDEX idx_user_suspensions_expiry ON user_suspensions (ends_at)
    WHERE lifted_at IS NULL AND kind = 'SUSPENSION';
