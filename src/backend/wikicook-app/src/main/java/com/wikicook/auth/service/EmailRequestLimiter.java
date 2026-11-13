package com.wikicook.auth.service;

import com.wikicook.auth.domain.TokenPurpose;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * At most 3 verification and 3 reset emails per address per 15 minutes (FR-017, research R5). Counted per
 * <em>address</em>, from {@code email_send_requests}, which has no user id: an address with no account is counted like a
 * registered one, so being refused tells nothing about whether the account exists. The two purposes are counted apart,
 * and the email sent at registration counts. A request is stored only when it is allowed, so a refused one does not
 * lengthen the wait. A {@code pg_advisory_xact_lock} on the address and purpose serializes parallel requests, so eight
 * at once still let only three through.
 */
@Service
public class EmailRequestLimiter {

    public static final int MAX_REQUESTS = 3;
    public static final Duration WINDOW = Duration.ofMinutes(15);

    private static final Set<TokenPurpose> COUNTED = Set.of(TokenPurpose.EMAIL_VERIFICATION,
            TokenPurpose.PASSWORD_RESET);

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public EmailRequestLimiter(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /**
     * Takes a slot if there is one. Used where the request must not be refused, for example registration, which
     * creates the account anyway and just sends no email (FR-017).
     */
    @Transactional
    public Slot tryAcquire(String emailHash, TokenPurpose purpose) {
        if (!COUNTED.contains(purpose)) {
            throw new IllegalArgumentException("not an email purpose: " + purpose);
        }
        Instant now = clock.instant();
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", rs -> null, emailHash + ":" + purpose);
        List<Timestamp> recent = jdbc.queryForList("""
                SELECT created_at FROM email_send_requests
                WHERE email_hash = ? AND purpose = ? AND created_at > ? ORDER BY created_at ASC
                """, Timestamp.class, emailHash, purpose.name(), Timestamp.from(now.minus(WINDOW)));
        if (recent.size() >= MAX_REQUESTS) {
            // the window has room again when the oldest request still counted leaves it
            Instant frees = recent.get(recent.size() - MAX_REQUESTS).toInstant().plus(WINDOW);
            long seconds = Math.max(1, (Duration.between(now, frees).toMillis() + 999) / 1000);
            return new Slot(false, seconds);
        }
        jdbc.update("INSERT INTO email_send_requests (email_hash, purpose, created_at) VALUES (?, ?, ?)", emailHash,
                purpose.name(), Timestamp.from(now));
        return new Slot(true, 0);
    }

    /**
     * Takes a slot or refuses.
     *
     * @throws AuthException 429 {@code EMAIL_RATE_LIMITED} with {@code retryAfterSeconds}
     */
    @Transactional
    public void requireSlot(String emailHash, TokenPurpose purpose) {
        Slot slot = tryAcquire(emailHash, purpose);
        if (!slot.allowed()) {
            throw AuthException.emailRateLimited(slot.retryAfterSeconds());
        }
    }

    public record Slot(boolean allowed, long retryAfterSeconds) {
    }
}
