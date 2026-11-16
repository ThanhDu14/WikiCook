package com.wikicook.auth.service;

import com.wikicook.auth.config.AuthProperties;
import com.wikicook.auth.domain.EmailOutboxMessage;
import com.wikicook.auth.domain.MailTemplate;
import com.wikicook.auth.domain.UserAccount;
import com.wikicook.auth.repository.EmailOutboxRepository;
import com.wikicook.auth.repository.UserAccountRepository;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The lockout and the per-IP limit of sign-in (FR-012, SC-005; research R4, R5). {@code login_attempts} is the only
 * source of truth, so a registered email and an unknown one take exactly the same path:
 *
 * <ol>
 * <li>{@link #begin}: a short transaction under {@code pg_advisory_xact_lock} of the email hash checks the IP limit and
 *     the lock, then <em>inserts the attempt as a failure before the password is compared</em> and commits. Parallel
 *     requests for one email queue on the lock, so together they cannot compare more than five passwords.
 * <li>the caller compares the password (BCrypt, no transaction held);
 * <li>{@link #recordFailure} or {@link #recordCorrectPassword} finish the attempt.
 * </ol>
 *
 * <p><b>The lock</b>: the five newest attempts of the email are all failures, the newest and the oldest of them are less
 * than 15 minutes apart, and 15 minutes have not passed since the newest. So it opens exactly 15 minutes after the
 * 5th failure, a rejected attempt is never stored (it cannot extend the lock), a success in between resets the count,
 * and after the lock a fresh run of five is needed. Time comes from the shared {@link Clock}.
 */
@Service
public class SignInThrottle {

    public static final int MAX_FAILURES = 5;
    public static final Duration WINDOW = Duration.ofMinutes(15);
    public static final Duration LOCK = Duration.ofMinutes(15);

    private final JdbcTemplate jdbc;
    private final UserAccountRepository users;
    private final EmailOutboxRepository outbox;
    private final AuthProperties properties;
    private final Clock clock;

    public SignInThrottle(JdbcTemplate jdbc, UserAccountRepository users, EmailOutboxRepository outbox,
            AuthProperties properties, Clock clock) {
        this.jdbc = jdbc;
        this.users = users;
        this.outbox = outbox;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Admits one sign-in attempt and stores it as a failure until the password proves otherwise.
     *
     * @throws AuthException 429 {@code TOO_MANY_REQUESTS} over the IP limit, 423 {@code ACCOUNT_LOCKED} while locked;
     *     the rejected attempt is not stored
     */
    @Transactional(noRollbackFor = AuthException.class)
    public Attempt begin(String emailHash, String email, InetAddress ip) {
        Instant now = clock.instant();
        lock(emailHash);

        if (ip != null) {
            enforceIpLimit(ip, now);
        }
        Instant lockedUntil = lockedUntil(emailHash, now);
        if (lockedUntil != null) {
            throw AuthException.accountLocked(lockedUntil);
        }

        UserAccount user = users.findByEmail(email).orElse(null);
        Long id = jdbc.queryForObject("""
                INSERT INTO login_attempts (user_id, email_hash, success, ip_address, created_at)
                VALUES (?, ?, false, ?::inet, ?) RETURNING id
                """, Long.class, user == null ? null : user.getId(), emailHash,
                ip == null ? null : ip.getHostAddress(), Timestamp.from(now));
        return new Attempt(id, user);
    }

    /**
     * The password was wrong. When this failure completes the fifth, the account gets {@code locked_until} and one
     * {@code ACCOUNT_LOCKED} email in the outbox (only if an account exists, NFR-003).
     */
    @Transactional
    public Failure recordFailure(Attempt attempt, String emailHash) {
        Instant now = clock.instant();
        lock(emailHash);
        Instant lockedUntil = lockedUntil(emailHash, now);
        if (lockedUntil == null) {
            return new Failure(null, false);
        }
        // only the attempt that is the newest one decides "newly locked", so parallel failures report it once
        Long newest = jdbc.queryForObject("SELECT max(id) FROM login_attempts WHERE email_hash = ?", Long.class,
                emailHash);
        boolean newlyLocked = attempt.id() == newest;
        UserAccount user = attempt.user();
        if (user != null && newlyLocked) {
            UserAccount managed = users.findById(user.getId()).orElse(user);
            managed.setLockedUntil(lockedUntil);
            users.save(managed);
            outbox.save(new EmailOutboxMessage(managed.getId(), MailTemplate.ACCOUNT_LOCKED,
                    Map.of("lockedUntil", lockedUntil.toString()), now, now));
        }
        return new Failure(lockedUntil, newlyLocked);
    }

    /**
     * The password was right: the attempt becomes a success (which resets the count) and the lock column is cleared.
     * {@code active} is false for a suspended or banned account: it proved its identity, but is not signed in.
     */
    @Transactional
    public void recordCorrectPassword(Attempt attempt, boolean active) {
        Instant now = clock.instant();
        jdbc.update("UPDATE login_attempts SET success = true WHERE id = ?", attempt.id());
        UserAccount user = attempt.user();
        if (user != null) {
            UserAccount managed = users.findById(user.getId()).orElse(user);
            managed.setLockedUntil(null);
            if (active) {
                managed.setLastSignInAt(now);
            }
            users.save(managed);
        }
    }

    private void lock(String emailHash) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", rs -> null, emailHash);
    }

    private void enforceIpLimit(InetAddress ip, Instant now) {
        Instant since = now.minus(WINDOW);
        List<Timestamp> attempts = jdbc.queryForList("""
                SELECT created_at FROM login_attempts
                WHERE ip_address = ?::inet AND created_at > ? ORDER BY created_at ASC
                """, Timestamp.class, ip.getHostAddress(), Timestamp.from(since));
        int limit = properties.signIn().ipLimit();
        if (attempts.size() >= limit) {
            // the window frees a slot when the oldest counted attempt leaves it
            Instant frees = attempts.get(attempts.size() - limit).toInstant().plus(WINDOW);
            long seconds = Math.max(1, (Duration.between(now, frees).toMillis() + 999) / 1000); // round up
            throw AuthException.tooManyRequests(seconds);
        }
    }

    /** The end of the lock if the email is locked at {@code now}, else null. */
    private Instant lockedUntil(String emailHash, Instant now) {
        List<Map<String, Object>> newest = jdbc.queryForList("""
                SELECT success, created_at FROM login_attempts WHERE email_hash = ? ORDER BY id DESC LIMIT ?
                """, emailHash, MAX_FAILURES);
        if (newest.size() < MAX_FAILURES || newest.stream().anyMatch(row -> (Boolean) row.get("success"))) {
            return null;
        }
        Instant last = ((Timestamp) newest.get(0).get("created_at")).toInstant();
        Instant first = ((Timestamp) newest.get(MAX_FAILURES - 1).get("created_at")).toInstant();
        // five failures spread over a full 15 minutes or more are not "within 15 minutes"; this also makes the first
        // failure after a lock opened (exactly 15 minutes after the 5th) start a new run instead of re-locking
        if (Duration.between(first, last).compareTo(WINDOW) >= 0) {
            return null;
        }
        Instant until = last.plus(LOCK);
        return now.isBefore(until) ? until : null;
    }

    /** The stored attempt; {@code user} is null when the email has no account. */
    public record Attempt(long id, UserAccount user) {
    }

    /** {@code lockedUntil} is null when the failure did not lock; {@code newlyLocked} is true for the 5th. */
    public record Failure(Instant lockedUntil, boolean newlyLocked) {
    }
}
