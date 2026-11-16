package com.wikicook.auth.service;

import com.wikicook.auth.api.AuditWriter;
import com.wikicook.auth.domain.UserSession;
import com.wikicook.auth.repository.UserSessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh-token sessions (FR-009, FR-010; research R2). The refresh token is 256 random bits; only its SHA-256 is
 * stored. Each refresh rotates it, keeping the previous hash for {@link #ROTATION_GRACE} so two tabs (or a slow
 * network) that send the old cookie are not logged out; the same old cookie later is treated as theft and revokes the
 * session.
 *
 * <p>Lifetime: with "Remember me" the session ends 30 days after sign-in and is never extended; without it, it slides
 * to 24 hours after the last activity, capped 30 days after sign-in. A successful refresh counts as activity.
 */
@Service
public class SessionService {

    public static final Duration REMEMBER_ME_TTL = Duration.ofDays(30);
    public static final Duration IDLE_TTL = Duration.ofHours(24);
    public static final Duration ROTATION_GRACE = Duration.ofSeconds(10);

    private static final int TOKEN_BYTES = 32;
    private static final int USER_AGENT_MAX = 255;

    private final UserSessionRepository sessions;
    private final AuditWriter audit;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public SessionService(UserSessionRepository sessions, AuditWriter audit, Clock clock) {
        this.sessions = sessions;
        this.audit = audit;
        this.clock = clock;
    }

    /** A new session and the refresh token to put in the cookie (shown once; only its hash is stored). */
    @Transactional
    public NewSession create(long userId, boolean rememberMe, String userAgent) {
        Instant now = clock.instant();
        String token = newToken();
        Instant expiresAt = rememberMe ? now.plus(REMEMBER_ME_TTL) : now.plus(IDLE_TTL);
        UserSession session = new UserSession(userId, hash(token), rememberMe, now, expiresAt);
        session.setUserAgent(userAgent == null ? null
                : userAgent.substring(0, Math.min(userAgent.length(), USER_AGENT_MAX)));
        return new NewSession(sessions.save(session), token);
    }

    /**
     * Exchanges a refresh token. Never throws for a bad token: a rejection that revokes a session (reuse) must commit,
     * so the caller turns {@link Status#REJECTED} into a 401 after this transaction has ended.
     */
    @Transactional
    public RefreshResult refresh(String refreshToken) {
        Instant now = clock.instant();
        String presented = hash(refreshToken);

        Optional<UserSession> current = sessions.lockByTokenHash(presented);
        if (current.isPresent()) {
            UserSession session = current.get();
            if (!usable(session, now)) {
                return RefreshResult.rejected();
            }
            String next = newToken();
            session.rotate(hash(next), now);
            session.setLastActivityAt(now);
            if (!session.isRememberMe()) {
                Instant sliding = now.plus(IDLE_TTL);
                Instant cap = session.getCreatedAt().plus(REMEMBER_ME_TTL);
                session.setExpiresAt(sliding.isBefore(cap) ? sliding : cap);
            }
            return new RefreshResult(Status.ROTATED, sessions.save(session), next);
        }

        Optional<UserSession> replayed = sessions.lockByPreviousTokenHash(presented);
        if (replayed.isPresent()) {
            UserSession session = replayed.get();
            if (!usable(session, now)) {
                return RefreshResult.rejected();
            }
            if (session.getRotatedAt() != null && !now.isAfter(session.getRotatedAt().plus(ROTATION_GRACE))) {
                return new RefreshResult(Status.GRACE_REPLAY, session, null);
            }
            session.revoke(now);
            sessions.save(session);
            audit.write("REFRESH_TOKEN_REUSED", session.getUserId(), "USER", session.getUserId(),
                    Map.of("sessionId", session.getId()));
        }
        return RefreshResult.rejected();
    }

    /**
     * Ends the session of this refresh token (FR-010). Idempotent: an unknown or already ended session changes nothing
     * and is not audited.
     *
     * @return the user whose session was ended, if there was one
     */
    @Transactional
    public Optional<Long> logout(String refreshToken) {
        Instant now = clock.instant();
        String presented = hash(refreshToken);
        Optional<UserSession> found = sessions.lockByTokenHash(presented);
        if (found.isEmpty()) {
            found = sessions.lockByPreviousTokenHash(presented);
        }
        if (found.isEmpty() || found.get().getRevokedAt() != null) {
            return Optional.empty();
        }
        UserSession session = found.get();
        session.revoke(now);
        sessions.save(session);
        audit.write("SIGNED_OUT", session.getUserId(), "USER", session.getUserId(),
                Map.of("sessionId", session.getId()));
        return Optional.of(session.getUserId());
    }

    /** Ends every open session of the user, for password reset, suspension and ban (FR-016, FR-024). */
    @Transactional
    public int revokeAll(long userId) {
        return sessions.revokeAllOf(userId, clock.instant());
    }

    private static boolean usable(UserSession session, Instant now) {
        return session.getRevokedAt() == null && session.getExpiresAt().isAfter(now);
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 as 64 lower-case hex characters, the form stored in {@code token_hash}. */
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public enum Status {
        /** The cookie was replaced: send {@code newRefreshToken} as the new cookie. */
        ROTATED,
        /** The previous cookie arrived within the grace period: answer, but leave the cookie alone. */
        GRACE_REPLAY,
        REJECTED
    }

    public record NewSession(UserSession session, String refreshToken) {
    }

    public record RefreshResult(Status status, UserSession session, String newRefreshToken) {

        static RefreshResult rejected() {
            return new RefreshResult(Status.REJECTED, null, null);
        }
    }
}
