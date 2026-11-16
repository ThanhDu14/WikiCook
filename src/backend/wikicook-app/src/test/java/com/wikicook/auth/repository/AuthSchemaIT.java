package com.wikicook.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * V102-V104 apply and their constraints hold (data-model.md, research R11). Pure SQL on the migrated schema, so it
 * fails with "relation does not exist" until the migrations exist.
 */
@SpringBootTest
@Import(AuthTestSupport.class)
class AuthSchemaIT extends AbstractPostgresIT {

    private static final Timestamp NOW = Timestamp.from(Instant.now());
    private static final Timestamp LATER = Timestamp.from(Instant.now().plus(1, ChronoUnit.DAYS));

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AuthTestSupport support;

    @BeforeEach
    void setUp() {
        support.resetThrottle();
    }

    private long insertSuspension(long userId, String kind, Timestamp endsAt) {
        return jdbc.queryForObject("""
                INSERT INTO user_suspensions (user_id, kind, reason, starts_at, ends_at)
                VALUES (?, ?, 'because', ?, ?) RETURNING id
                """, Long.class, userId, kind, NOW, endsAt);
    }

    @Test
    @DisplayName("Edge: Suspension vs ban - a ban has no end date, a suspension must have one (V102 CHECK)")
    void banMustNotHaveEndsAtAndSuspensionMust() {
        long user = support.member();
        assertThatThrownBy(() -> insertSuspension(user, "BAN", LATER)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertSuspension(user, "SUSPENSION", null)).isInstanceOf(DataAccessException.class);
        assertThat(insertSuspension(user, "SUSPENSION", LATER)).isPositive();
    }

    @Test
    @DisplayName("Edge: Suspension vs ban - at most one running sanction per account (V102 UNIQUE WHERE lifted_at IS NULL)")
    void onlyOneRunningSanctionPerUser() {
        long user = support.member();
        insertSuspension(user, "SUSPENSION", LATER);
        assertThatThrownBy(() -> insertSuspension(user, "BAN", null)).isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("FR-025: lifted_at and lift_reason are set together (V102 CHECK)")
    void liftedAtAndLiftReasonGoTogether() {
        long user = support.member();
        long id = insertSuspension(user, "SUSPENSION", LATER);
        assertThatThrownBy(() -> jdbc.update("UPDATE user_suspensions SET lifted_at = now() WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE user_suspensions SET lift_reason = 'LIFTED' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE user_suspensions SET lifted_at = now(), lift_reason = 'OTHER' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThat(jdbc.update("UPDATE user_suspensions SET lifted_at = now(), lift_reason = 'LIFTED' WHERE id = ?",
                id)).isEqualTo(1);
    }

    @Test
    @DisplayName("FR-015, R16: one_time_tokens.purpose allows ACCOUNT_CLOSURE and nothing unknown (V103)")
    void oneTimeTokenPurposeCheck() {
        long user = support.member();
        String insert = "INSERT INTO one_time_tokens (user_id, purpose, token_hash, expires_at) VALUES (?, ?, ?, ?)";
        assertThat(jdbc.update(insert, user, "ACCOUNT_CLOSURE", "a".repeat(64), LATER)).isEqualTo(1);
        assertThatThrownBy(() -> jdbc.update(insert, user, "OTHER", "b".repeat(64), LATER))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("FR-017: email_send_requests.purpose is EMAIL_VERIFICATION or PASSWORD_RESET only (V104)")
    void emailSendRequestPurposeCheck() {
        String insert = "INSERT INTO email_send_requests (email_hash, purpose) VALUES (?, ?)";
        assertThat(jdbc.update(insert, "c".repeat(64), "PASSWORD_RESET")).isEqualTo(1);
        assertThatThrownBy(() -> jdbc.update(insert, "c".repeat(64), "ACCOUNT_CLOSURE"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("FR-024a: email_outbox.template is restricted (V104)")
    void emailOutboxTemplateCheck() {
        long user = support.member();
        String insert = "INSERT INTO email_outbox (user_id, template, payload, next_attempt_at) "
                + "VALUES (?, ?, '{}'::jsonb, now())";
        assertThat(jdbc.update(insert, user, "BAN_LIFTED")).isEqualTo(1);
        assertThatThrownBy(() -> jdbc.update(insert, user, "NEWSLETTER")).isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("Edge: Account deleted after 7 days - deleting users cascades owned rows and leaves audit_logs alone")
    void deletingUserCascadesAndKeepsAudit() {
        long user = support.createUser(Role.MEMBER, AccountStatus.ACTIVE, false);
        long actor = support.member();
        support.session(user);
        jdbc.update("INSERT INTO login_attempts (user_id, email_hash, success) VALUES (?, ?, false)", user,
                "d".repeat(64));
        jdbc.update("INSERT INTO email_outbox (user_id, template, payload, next_attempt_at) "
                + "VALUES (?, 'ACCOUNT_LOCKED', '{}'::jsonb, now())", user);
        long auditId = jdbc.queryForObject("""
                INSERT INTO audit_logs (actor_id, action, target_type, target_id)
                VALUES (?, 'ROLE_CHANGED', 'USER', ?) RETURNING id
                """, Long.class, actor, user);

        jdbc.update("DELETE FROM users WHERE id = ?", user);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_sessions WHERE user_id = ?", Long.class, user))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM login_attempts WHERE user_id = ?", Long.class, user))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM email_outbox WHERE user_id = ?", Long.class, user))
                .isZero();
        // audit_logs has no FK: actor_id and target_id keep their values and no UPDATE ran
        assertThat(jdbc.queryForMap("SELECT actor_id, target_id FROM audit_logs WHERE id = ?", auditId))
                .containsEntry("actor_id", actor).containsEntry("target_id", user);
    }

    @Test
    @DisplayName("NFR-004 of 100: audit_logs is append-only, UPDATE and DELETE are rejected by trigger")
    void auditLogsIsAppendOnly() {
        long id = jdbc.queryForObject(
                "INSERT INTO audit_logs (action) VALUES ('SIGNED_OUT') RETURNING id", Long.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE audit_logs SET action = 'X' WHERE id = ?", id))
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM audit_logs WHERE id = ?", id))
                .hasMessageContaining("append-only");
    }

    @Test
    @DisplayName("V103: users.pending_role_notice_from only takes a role; blocked_emails is keyed by hash")
    void usersAndBlockedEmailsColumns() {
        long user = support.member();
        assertThat(jdbc.update("UPDATE users SET pending_role_notice_from = 'CONTRIBUTOR' WHERE id = ?", user))
                .isEqualTo(1);
        assertThatThrownBy(() -> jdbc.update("UPDATE users SET pending_role_notice_from = 'ROOT' WHERE id = ?", user))
                .isInstanceOf(DataAccessException.class);
        String hash = "e".repeat(64);
        jdbc.update("INSERT INTO blocked_emails (email_hash) VALUES (?) ON CONFLICT DO NOTHING", hash);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO blocked_emails (email_hash) VALUES (?)", hash))
                .isInstanceOf(DataAccessException.class);
    }
}
