package com.wikicook.support;

import com.wikicook.auth.service.AccessTokenService;
import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Test data for the 010 integration tests: accounts by role, status and verification (also with a fixed
 * {@code id}), session rows, and the throttle reset every IT calls in {@code @BeforeEach} because the Postgres
 * container is shared (research R5). Import it with {@code @Import(AuthTestSupport.class)}.
 *
 * <p>There is deliberately no helper that clears {@code audit_logs}: the table has a trigger that rejects UPDATE and
 * DELETE (research R11), so audit assertions filter by the {@code target_id} or {@code action} of data the test
 * created itself.
 */
public class AuthTestSupport {

    private static final AtomicLong SEQ = new AtomicLong(1);

    private final JdbcTemplate jdbc;
    private final AccessTokenService tokens;

    @Autowired
    public AuthTestSupport(JdbcTemplate jdbc, AccessTokenService tokens) {
        this.jdbc = jdbc;
        this.tokens = tokens;
    }

    /** A verified, active Member with a unique email. */
    public long member() {
        return createUser(Role.MEMBER, AccountStatus.ACTIVE, true);
    }

    public long createUser(Role role, AccountStatus status, boolean emailVerified) {
        return insertUser(null, uniqueEmail(), role, status, emailVerified, null);
    }

    public long createUser(String email, Role role, AccountStatus status, boolean emailVerified, String passwordHash) {
        return insertUser(null, email, role, status, emailVerified, passwordHash);
    }

    /** Creates the account with this exact id (for tests that send {@code X-Test-User}); a no-op when it exists. */
    public long createUserWithId(long id, Role role, AccountStatus status, boolean emailVerified) {
        Long existing = jdbc.query("SELECT id FROM users WHERE id = ?", rs -> rs.next() ? rs.getLong(1) : null, id);
        if (existing != null) {
            return existing;
        }
        return insertUser(id, "user-" + id + "@test.wikicook.local", role, status, emailVerified, null);
    }

    public long createUserWithId(long id) {
        return createUserWithId(id, Role.MEMBER, AccountStatus.ACTIVE, true);
    }

    private long insertUser(Long id, String email, Role role, AccountStatus status, boolean emailVerified,
            String passwordHash) {
        Instant verifiedAt = emailVerified ? Instant.now() : null;
        if (id == null) {
            return jdbc.queryForObject("""
                    INSERT INTO users (email, display_name, role, status, email_verified_at, password_hash)
                    VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                    """, Long.class, email, "Test " + role, role.name(), status.name(), ts(verifiedAt),
                    passwordHash);
        }
        jdbc.update("""
                INSERT INTO users (id, email, display_name, role, status, email_verified_at, password_hash)
                OVERRIDING SYSTEM VALUE VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING
                """, id, email, "Test " + role, role.name(), status.name(), ts(verifiedAt), passwordHash);
        // keep the identity sequence ahead of explicit ids so later inserts do not collide
        jdbc.queryForObject("SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT max(id) FROM users))",
                Long.class);
        return id;
    }

    /** An open session that expires in 30 days; returns {@code user_sessions.id}. */
    public long session(long userId) {
        Instant now = Instant.now();
        return session(userId, now, now.plusSeconds(30L * 24 * 3600), null);
    }

    public long session(long userId, Instant lastActivityAt, Instant expiresAt, Instant revokedAt) {
        return jdbc.queryForObject("""
                INSERT INTO user_sessions (user_id, token_hash, remember_me, last_activity_at, expires_at, revoked_at)
                VALUES (?, ?, true, ?, ?, ?) RETURNING id
                """, Long.class, userId, randomHash(), ts(lastActivityAt), ts(expiresAt), ts(revokedAt));
    }

    /** A real access token for this session, signed like production (research R1). */
    public String jwt(long userId, long sessionId) {
        return tokens.issue(userId, sessionId).value();
    }

    /** An account plus an open session plus a JWT: what a signed-in caller of the matrix tests looks like. */
    public SignedIn signedIn(Role role, AccountStatus status, boolean emailVerified) {
        long userId = createUser(role, status, emailVerified);
        long sessionId = session(userId);
        return new SignedIn(userId, sessionId, jwt(userId, sessionId));
    }

    public SignedIn signedInMember() {
        return signedIn(Role.MEMBER, AccountStatus.ACTIVE, true);
    }

    /** {@code Authorization} header value. */
    public record SignedIn(long userId, long sessionId, String jwt) {

        public String bearer() {
            return "Bearer " + jwt;
        }
    }

    /** Empties the three counters FR-012 and FR-017 read; call it in {@code @BeforeEach} of every 010 IT. */
    public void resetThrottle() {
        jdbc.update("DELETE FROM login_attempts");
        jdbc.update("DELETE FROM email_send_requests");
        jdbc.update("DELETE FROM password_check_failures");
    }

    public static String uniqueEmail() {
        return "user-" + SEQ.getAndIncrement() + "-" + System.nanoTime() + "@test.wikicook.local";
    }

    private static String randomHash() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static Timestamp ts(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
