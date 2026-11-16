package com.wikicook.auth.job;

import com.wikicook.auth.api.AuditWriter;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Closes suspensions whose end date has passed (FR-024a, research R9): {@code lifted_at = ends_at}, {@code lift_reason =
 * EXPIRED}, the account goes back to ACTIVE, one {@code SUSPENSION_ENDED} email is queued and the end is audited.
 *
 * <p>{@link #finishExpired(long)} is the part every admin operation needs: the job runs only every few minutes, so
 * {@code AccountAdminService} calls it before it reads an account's status. It runs in its <em>own</em> transaction
 * ({@code REQUIRES_NEW}) that commits at once: if it shared the caller's transaction, a refused operation (for example
 * lifting a suspension that already ended, a 409) would roll the ending back and lose the email. It is idempotent
 * ({@code UPDATE … WHERE lifted_at IS NULL}), so concurrent callers queue the email only once.
 *
 * <p>The scheduled sweep over all accounts is added with the rest of the jobs of phase 8.
 */
@Component
public class SuspensionExpiryJob {

    /** Audit action of a suspension that ended by itself; {@code ACCOUNT_REINSTATED} is reserved for an early lift. */
    public static final String AUDIT_ACTION = "SUSPENSION_EXPIRED";

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final AuditWriter audit;

    public SuspensionExpiryJob(JdbcTemplate jdbc, Clock clock, AuditWriter audit) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.audit = audit;
    }

    /** @return true when a suspension of this user was closed now */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean finishExpired(long userId) {
        Instant now = clock.instant();
        List<Timestamp> ended = jdbc.queryForList("""
                UPDATE user_suspensions SET lifted_at = ends_at, lift_reason = 'EXPIRED'
                WHERE user_id = ? AND kind = 'SUSPENSION' AND lifted_at IS NULL AND ends_at <= ?
                RETURNING ends_at
                """, Timestamp.class, userId, Timestamp.from(now));
        if (ended.isEmpty()) {
            return false;
        }
        jdbc.update("UPDATE users SET status = 'ACTIVE', updated_at = ? WHERE id = ? AND status = 'SUSPENDED'",
                Timestamp.from(now), userId);
        jdbc.update("""
                INSERT INTO email_outbox (user_id, template, payload, next_attempt_at, created_at)
                VALUES (?, 'SUSPENSION_ENDED', jsonb_build_object('endedAt', ?::text), ?, ?)
                """, userId, ended.get(0).toInstant().toString(), Timestamp.from(now), Timestamp.from(now));
        audit.write(AUDIT_ACTION, null, "USER", userId, Map.of("endedAt", ended.get(0).toInstant().toString()));
        return true;
    }
}
