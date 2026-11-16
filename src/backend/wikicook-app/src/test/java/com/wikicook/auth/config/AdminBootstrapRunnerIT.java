package com.wikicook.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.TestAuthProperties;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spec Assumptions: the first Administrator comes from {@code WIKICOOK_BOOTSTRAP_ADMIN_EMAIL} and
 * {@code WIKICOOK_BOOTSTRAP_ADMIN_PASSWORD}, created only when no active Administrator exists; blank variables do
 * nothing; an address already used by someone else is never silently promoted.
 */
@SpringBootTest(properties = {"wikicook.auth.bootstrap-admin.email=  Boot.Admin@Mail.COM ",
        "wikicook.auth.bootstrap-admin.password=Admin2026pass"})
@Import(AuthTestSupport.class)
class AdminBootstrapRunnerIT extends AbstractPostgresIT {

    private static final String EMAIL = "boot.admin@mail.com";

    @Autowired
    AdminBootstrapRunner runner;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AuthTestSupport support;

    @Autowired
    PasswordEncoder encoder;

    @BeforeEach
    void noAdministratorsAndNoBootstrapAccount() {
        jdbc.update("DELETE FROM users WHERE role = 'ADMINISTRATOR' OR email = ?", EMAIL);
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    @Test
    @DisplayName("Assumptions: with no Administrator the account is created, verified, normalized and able to sign in")
    void createsTheFirstAdministrator() {
        runner.bootstrap();

        Map<String, Object> admin = jdbc.queryForMap("SELECT role, status, email_verified_at, password_hash, "
                + "display_name FROM users WHERE email = ?", EMAIL);
        assertThat(admin.get("role")).isEqualTo("ADMINISTRATOR");
        assertThat(admin.get("status")).isEqualTo("ACTIVE");
        assertThat((Timestamp) admin.get("email_verified_at")).isNotNull();
        assertThat(admin.get("display_name")).isEqualTo("Administrator");
        assertThat(encoder.matches("Admin2026pass", (String) admin.get("password_hash"))).isTrue();
        assertThat((String) admin.get("password_hash")).doesNotContain("Admin2026pass");
    }

    @Test
    @DisplayName("Assumptions: running again creates nothing more")
    void isIdempotent() {
        runner.bootstrap();
        runner.bootstrap();

        assertThat(count("SELECT count(*) FROM users WHERE email = ?", EMAIL)).isEqualTo(1);
    }

    @Test
    @DisplayName("Assumptions: an active Administrator already exists, so nothing is created")
    void doesNothingWhenAnActiveAdministratorExists() {
        support.createUser(Role.ADMINISTRATOR, AccountStatus.ACTIVE, true);

        runner.bootstrap();

        assertThat(count("SELECT count(*) FROM users WHERE email = ?", EMAIL)).isZero();
    }

    @Test
    @DisplayName("Assumptions: a suspended or banned Administrator is not an active one, so the account is created")
    void inactiveAdministratorsDoNotCount() {
        support.createUser(Role.ADMINISTRATOR, AccountStatus.SUSPENDED, true);
        support.createUser(Role.ADMINISTRATOR, AccountStatus.BANNED, true);

        runner.bootstrap();

        assertThat(count("SELECT count(*) FROM users WHERE email = ? AND role = 'ADMINISTRATOR'", EMAIL)).isEqualTo(1);
    }

    @Test
    @DisplayName("Safety: an address that already belongs to another account is not promoted")
    void doesNotPromoteAnExistingAccount() {
        support.createUser(EMAIL, Role.MEMBER, AccountStatus.ACTIVE, true, null);

        runner.bootstrap();

        assertThat(jdbc.queryForObject("SELECT role FROM users WHERE email = ?", String.class, EMAIL))
                .isEqualTo("MEMBER");
    }

    @Test
    @DisplayName("Assumptions: empty variables do nothing")
    void blankVariablesDoNothing() {
        AdminBootstrapRunner blank = new AdminBootstrapRunner(jdbc, encoder,
                new com.wikicook.auth.security.BcryptBulkhead(2), TestAuthProperties.create(), Clock.systemUTC());

        blank.bootstrap();

        assertThat(count("SELECT count(*) FROM users WHERE role = 'ADMINISTRATOR' AND status = 'ACTIVE'")).isZero();
    }

    @Test
    @DisplayName("FR-004: a password that breaks the rules creates no account")
    void aWeakBootstrapPasswordIsRefused() {
        AuthProperties weak = TestAuthProperties.withBootstrap(EMAIL, "short");
        AdminBootstrapRunner runnerWithWeakPassword = new AdminBootstrapRunner(jdbc, encoder,
                new com.wikicook.auth.security.BcryptBulkhead(2), weak, Clock.systemUTC());

        runnerWithWeakPassword.bootstrap();

        assertThat(count("SELECT count(*) FROM users WHERE email = ?", EMAIL)).isZero();
    }
}
