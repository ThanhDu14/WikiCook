package com.wikicook.auth.repository;

import com.wikicook.auth.dto.SessionSnapshot;
import java.time.Instant;
import java.util.Optional;

/** The per-request queries of research R1, written with {@code JdbcTemplate}: one statement each, no entity loading. */
public interface UserSessionRepositoryCustom {

    /** {@code user_sessions} JOIN {@code users} by primary key, LEFT JOIN the running sanction; empty when no row. */
    Optional<SessionSnapshot> findSnapshot(long sessionId);

    /**
     * Records activity at most once per window: only when {@code last_activity_at < staleBefore}. A session without
     * "remember me" also slides {@code expires_at} to {@code slidingExpiry}, capped 30 days after creation (R2).
     *
     * @return true when the row was updated
     */
    boolean touchActivity(long sessionId, Instant now, Instant staleBefore, Instant slidingExpiry);
}
