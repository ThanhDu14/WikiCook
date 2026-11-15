package com.wikicook.auth.api;

import java.time.Duration;

/**
 * Re-authentication of a session (FR-022a; 011 FR-029, FR-031). Proving the password again stamps
 * {@code user_sessions.reauthenticated_at}; sensitive actions then ask for a stamp that is recent enough.
 */
public interface ReauthService {

    /** @throws com.wikicook.auth.service.AuthException 403 {@code REAUTH_REQUIRED} when there is no recent stamp */
    void requireRecent(long sessionId, Duration maxAge);

    boolean isRecent(long sessionId, Duration maxAge);

    /**
     * Checks the password of {@code userId} (through the BCrypt bulkhead) and stamps the session.
     *
     * @throws com.wikicook.auth.service.AuthException 401 {@code INVALID_CREDENTIALS} for a wrong password, or an
     *     account that has none
     */
    void reauthenticate(long userId, long sessionId, String password);
}
