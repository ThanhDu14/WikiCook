package com.wikicook.devstub;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Makes sure a dev user row exists, since {@code ai_requests.user_id} references {@code users}. Inserts only the
 * NOT NULL columns without default, then moves the identity sequence past it so real sign-ups do not collide.
 */
public class DevUserProvisioner {

    private final JdbcTemplate jdbc;
    private final Set<Long> known = ConcurrentHashMap.newKeySet();

    public DevUserProvisioner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void ensure(long userId) {
        if (known.contains(userId)) {
            return;
        }
        int inserted = jdbc.update("""
                INSERT INTO users (id, email, display_name) OVERRIDING SYSTEM VALUE
                VALUES (?, ?, ?)
                ON CONFLICT DO NOTHING
                """, userId, "dev-user-" + userId + "@wikicook.local", "Dev user " + userId);
        if (inserted > 0) {
            jdbc.queryForObject("SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT max(id) FROM users))",
                    Long.class);
        }
        known.add(userId);
    }
}
