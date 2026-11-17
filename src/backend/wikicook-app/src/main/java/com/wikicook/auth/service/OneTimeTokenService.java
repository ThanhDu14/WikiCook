package com.wikicook.auth.service;

import com.wikicook.auth.domain.TokenPurpose;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single-use links for email verification, password reset and account closure (FR-005, FR-015; research R6). A token is
 * 32 bytes from {@link SecureRandom}, Base64URL, sent by email; the database keeps only its SHA-256. It is consumed with
 * one conditional {@code UPDATE … RETURNING}, so two requests with the same link cannot both win, and the change runs in
 * the caller's transaction, together with whatever the token allows (verifying the account, changing the password).
 * Issuing a token marks the older unused ones of the same user and purpose as used, so only the newest link works.
 */
@Service
public class OneTimeTokenService {

    private static final int TOKEN_BYTES = 32;

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public OneTimeTokenService(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** A new token; earlier unused tokens of this user and purpose stop working. */
    @Transactional
    public Issued issue(long userId, TokenPurpose purpose, Duration ttl) {
        Instant now = clock.instant();
        jdbc.update("UPDATE one_time_tokens SET used_at = ? WHERE user_id = ? AND purpose = ? AND used_at IS NULL",
                Timestamp.from(now), userId, purpose.name());
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = now.plus(ttl);
        jdbc.update("""
                INSERT INTO one_time_tokens (user_id, purpose, token_hash, expires_at, created_at)
                VALUES (?, ?, ?, ?, ?)
                """, userId, purpose.name(), SessionService.hash(token), Timestamp.from(expiresAt),
                Timestamp.from(now));
        return new Issued(token, expiresAt);
    }

    /**
     * Uses the token if it is valid. The user is known for every outcome except {@link Status#UNKNOWN}, which lets the
     * caller answer "already verified" for a link used before.
     */
    @Transactional
    public Consumption consume(String token, TokenPurpose purpose) {
        Instant now = clock.instant();
        String hash = SessionService.hash(token);
        List<Long> consumed = jdbc.queryForList("""
                UPDATE one_time_tokens SET used_at = ?
                WHERE token_hash = ? AND purpose = ? AND used_at IS NULL AND expires_at > ?
                RETURNING user_id
                """, Long.class, Timestamp.from(now), hash, purpose.name(), Timestamp.from(now));
        if (!consumed.isEmpty()) {
            return new Consumption(Status.CONSUMED, consumed.get(0));
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT user_id, used_at FROM one_time_tokens WHERE token_hash = ? AND purpose = ?", hash,
                purpose.name());
        if (rows.isEmpty()) {
            return new Consumption(Status.UNKNOWN, null);
        }
        Long userId = ((Number) rows.get(0).get("user_id")).longValue();
        return new Consumption(rows.get(0).get("used_at") != null ? Status.USED : Status.EXPIRED, userId);
    }

    /**
     * Looks at a token without using it (the reset form checks its link before it shows, AU06). The state is the same
     * one {@link #consume} would find: {@link Status#VALID} for a link that can still be used, otherwise the reason
     * it cannot.
     */
    @Transactional(readOnly = true)
    public Inspection inspect(String token, TokenPurpose purpose) {
        Instant now = clock.instant();
        return jdbc.query("SELECT user_id, used_at, expires_at FROM one_time_tokens WHERE token_hash = ? AND purpose = ?",
                (rs, row) -> {
                    Instant expiresAt = rs.getTimestamp("expires_at").toInstant();
                    Status status = rs.getTimestamp("used_at") != null ? Status.USED
                            : expiresAt.isAfter(now) ? Status.VALID : Status.EXPIRED;
                    return new Inspection(status, rs.getLong("user_id"), expiresAt);
                }, SessionService.hash(token), purpose.name()).stream().findFirst()
                .orElse(new Inspection(Status.UNKNOWN, null, null));
    }

    /** The user a token was issued to, whatever its state (to resend an expired link). */
    @Transactional(readOnly = true)
    public Optional<Long> userOf(String token, TokenPurpose purpose) {
        return jdbc.queryForList("SELECT user_id FROM one_time_tokens WHERE token_hash = ? AND purpose = ?",
                Long.class, SessionService.hash(token), purpose.name()).stream().findFirst();
    }

    public enum Status {
        CONSUMED,
        /** Only from {@code inspect}: unused and not expired, so {@code consume} would use it. */
        VALID,
        /** Past its expiry and never used. */
        EXPIRED,
        /** Used before, or replaced by a newer token. */
        USED,
        UNKNOWN
    }

    public record Consumption(Status status, Long userId) {
    }

    /** {@code userId} and {@code expiresAt} are null for {@link Status#UNKNOWN}. */
    public record Inspection(Status status, Long userId, Instant expiresAt) {
    }

    public record Issued(String token, Instant expiresAt) {
    }
}
