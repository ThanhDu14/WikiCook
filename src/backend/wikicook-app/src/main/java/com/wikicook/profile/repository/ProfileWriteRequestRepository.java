package com.wikicook.profile.repository;

import com.wikicook.profile.domain.ProfileWriteKind;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * SQL of the write counter (NFR-008, research R13). No entity: rows are inserted, counted and deleted in one short
 * transaction under an advisory lock, never loaded as objects.
 */
@Repository
public class ProfileWriteRequestRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ProfileWriteRequestRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Held until the transaction ends: two requests of one person count one after the other. */
    public void lock(long userId) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended('profile-write:' || CAST(:userId AS text), 0))",
                Map.of("userId", userId), rs -> {
                });
    }

    /** Rows older than an hour no longer count, so they can go. */
    public void deleteExpired(long userId) {
        jdbc.update("DELETE FROM profile_write_requests WHERE user_id = :userId AND created_at < now() - interval '1 hour'",
                Map.of("userId", userId));
    }

    public int count(long userId, ProfileWriteKind kind) {
        return jdbc.queryForObject("SELECT count(*) FROM profile_write_requests WHERE user_id = :userId AND kind = :kind",
                Map.of("userId", userId, "kind", kind.name()), Integer.class);
    }

    /** Seconds until the oldest row of this kind leaves the hour, at least 1. */
    public int secondsUntilOldestExpires(long userId, ProfileWriteKind kind) {
        Integer seconds = jdbc.queryForObject("""
                SELECT GREATEST(1, CEIL(EXTRACT(EPOCH FROM (min(created_at) + interval '1 hour' - now()))))::int
                  FROM profile_write_requests WHERE user_id = :userId AND kind = :kind
                """, Map.of("userId", userId, "kind", kind.name()), Integer.class);
        return seconds == null ? 1 : seconds;
    }

    public void insert(long userId, ProfileWriteKind kind) {
        jdbc.update("INSERT INTO profile_write_requests (user_id, kind) VALUES (:userId, :kind)",
                Map.of("userId", userId, "kind", kind.name()));
    }
}
