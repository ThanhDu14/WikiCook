package com.wikicook.auth.service;

import com.wikicook.auth.api.ReauthService;
import com.wikicook.auth.repository.UserAccountRepository;
import com.wikicook.auth.security.BcryptBulkhead;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * {@link ReauthService} on {@code user_sessions.reauthenticated_at}. The stamp belongs to one session: proving the
 * password in one browser does not open the others. The password comparison goes through {@link BcryptBulkhead} and no
 * transaction is held while it runs. The counter of wrong passwords (FR-022b) is added with {@code PasswordService}.
 */
@Service
public class ReauthServiceImpl implements ReauthService {

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final BcryptBulkhead bulkhead;

    public ReauthServiceImpl(JdbcTemplate jdbc, Clock clock, UserAccountRepository users, PasswordEncoder encoder,
            BcryptBulkhead bulkhead) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.users = users;
        this.encoder = encoder;
        this.bulkhead = bulkhead;
    }

    @Override
    public void requireRecent(long sessionId, Duration maxAge) {
        if (!isRecent(sessionId, maxAge)) {
            throw AuthException.reauthRequired();
        }
    }

    @Override
    public boolean isRecent(long sessionId, Duration maxAge) {
        Timestamp stamp = jdbc.query("SELECT reauthenticated_at FROM user_sessions WHERE id = ?",
                rs -> rs.next() ? rs.getTimestamp(1) : null, sessionId);
        if (stamp == null) {
            return false;
        }
        Instant limit = stamp.toInstant().plus(maxAge);
        return !clock.instant().isAfter(limit);
    }

    @Override
    public void reauthenticate(long userId, long sessionId, String password) {
        String stored = users.findById(userId).map(user -> user.getPasswordHash()).orElse(null);
        if (stored == null || !bulkhead.run(() -> encoder.matches(password, stored))) {
            throw AuthException.invalidCredentials();
        }
        jdbc.update("UPDATE user_sessions SET reauthenticated_at = ? WHERE id = ?", Timestamp.from(clock.instant()),
                sessionId);
    }
}
