package com.wikicook.auth.config;

import com.wikicook.auth.security.BcryptBulkhead;
import com.wikicook.auth.service.EmailNormalizer;
import com.wikicook.auth.validation.ValidPasswordValidator;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first Administrator (spec Assumptions): the platform needs one before anyone can be promoted. Reads
 * {@code WIKICOOK_BOOTSTRAP_ADMIN_EMAIL} and {@code WIKICOOK_BOOTSTRAP_ADMIN_PASSWORD}; does nothing when either is
 * empty or an <em>active</em> Administrator already exists (a suspended or banned one does not count). The account is
 * verified at once, because no email could be sent before it exists. It never promotes an existing account: if the
 * address already belongs to someone, it only logs a warning. The password must pass the rules of FR-004 and is never
 * logged.
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final BcryptBulkhead bulkhead;
    private final AuthProperties properties;
    private final Clock clock;

    public AdminBootstrapRunner(JdbcTemplate jdbc, PasswordEncoder encoder, BcryptBulkhead bulkhead,
            AuthProperties properties, Clock clock) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.bulkhead = bulkhead;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        bootstrap();
    }

    /** Public so tests (and an operator's tooling) can run it on demand. */
    @Transactional
    public void bootstrap() {
        AuthProperties.BootstrapAdmin admin = properties.bootstrapAdmin();
        if (!admin.configured()) {
            return;
        }
        Long active = jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE role = 'ADMINISTRATOR' AND status = 'ACTIVE'", Long.class);
        if (active != null && active > 0) {
            return;
        }
        String email = EmailNormalizer.normalize(admin.email());
        List<String> problems = ValidPasswordValidator.problems(admin.password());
        if (!problems.isEmpty()) {
            log.error("The bootstrap Administrator was not created: the password breaks the rules {}", problems);
            return;
        }
        Instant now = clock.instant();
        String hash = bulkhead.run(() -> encoder.encode(admin.password()));
        int created = jdbc.update("""
                INSERT INTO users (email, password_hash, display_name, role, email_verified_at, created_at, updated_at)
                VALUES (?, ?, 'Administrator', 'ADMINISTRATOR', ?, ?, ?)
                ON CONFLICT (email) DO NOTHING
                """, email, hash, Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
        if (created == 0) {
            log.warn("The bootstrap Administrator was not created: the address already belongs to an account, "
                    + "which is never promoted automatically");
        } else {
            log.info("Created the bootstrap Administrator account");
        }
    }
}
