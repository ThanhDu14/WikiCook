package com.wikicook.auth.repository;

import com.wikicook.auth.domain.SanctionKind;
import com.wikicook.auth.dto.SessionSnapshot;
import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;

/** Picked up by Spring Data through the {@code Impl} suffix of {@link UserSessionRepository}. */
class UserSessionRepositoryImpl implements UserSessionRepositoryCustom {

    private static final String SNAPSHOT_SQL = """
            SELECT s.id AS session_id, s.user_id, u.role, u.status, u.email_verified_at,
                   s.revoked_at, s.expires_at, s.last_activity_at, u.pending_role_notice_from,
                   x.kind AS sanction_kind, x.reason AS sanction_reason, x.ends_at AS sanction_ends_at
            FROM user_sessions s
            JOIN users u ON u.id = s.user_id
            LEFT JOIN user_suspensions x ON x.user_id = u.id AND x.lifted_at IS NULL
            WHERE s.id = ?
            """;

    private static final String TOUCH_SQL = """
            UPDATE user_sessions
            SET last_activity_at = ?,
                expires_at = CASE WHEN remember_me THEN expires_at
                                  ELSE LEAST(?, created_at + interval '30 days') END
            WHERE id = ? AND last_activity_at < ?
            """;

    private final JdbcTemplate jdbc;

    UserSessionRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<SessionSnapshot> findSnapshot(long sessionId) {
        return jdbc.query(SNAPSHOT_SQL, UserSessionRepositoryImpl::snapshot, sessionId).stream().findFirst();
    }

    @Override
    public boolean touchActivity(long sessionId, Instant now, Instant staleBefore, Instant slidingExpiry) {
        return jdbc.update(TOUCH_SQL, Timestamp.from(now), Timestamp.from(slidingExpiry), sessionId,
                Timestamp.from(staleBefore)) > 0;
    }

    private static SessionSnapshot snapshot(ResultSet rs, int row) throws SQLException {
        String pendingNotice = rs.getString("pending_role_notice_from");
        String sanctionKind = rs.getString("sanction_kind");
        return new SessionSnapshot(rs.getLong("session_id"), rs.getLong("user_id"),
                Role.valueOf(rs.getString("role")), AccountStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("email_verified_at") != null, instant(rs, "revoked_at"), instant(rs, "expires_at"),
                instant(rs, "last_activity_at"), pendingNotice == null ? null : Role.valueOf(pendingNotice),
                sanctionKind == null ? null : SanctionKind.valueOf(sanctionKind), rs.getString("sanction_reason"),
                instant(rs, "sanction_ends_at"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
