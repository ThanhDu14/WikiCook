package com.wikicook.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.auth.domain.AuditLog;
import com.wikicook.auth.domain.BlockedEmail;
import com.wikicook.auth.domain.EmailOutboxMessage;
import com.wikicook.auth.domain.EmailSendRequest;
import com.wikicook.auth.domain.LiftReason;
import com.wikicook.auth.domain.LoginAttempt;
import com.wikicook.auth.domain.MailTemplate;
import com.wikicook.auth.domain.OneTimeToken;
import com.wikicook.auth.domain.PasswordCheckFailure;
import com.wikicook.auth.domain.SanctionKind;
import com.wikicook.auth.domain.TokenPurpose;
import com.wikicook.auth.domain.UserAccount;
import com.wikicook.auth.domain.UserSession;
import com.wikicook.auth.domain.UserSuspension;
import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import jakarta.persistence.EntityManager;
import java.net.InetAddress;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Research R11: with {@code ddl-auto: validate} the special columns only map when declared (CHAR(64), INET, JSONB).
 * The context starting proves validation passes; the round trips prove each value comes back unchanged.
 */
@SpringBootTest
@Import(AuthTestSupport.class)
class EntityMappingIT extends AbstractPostgresIT {

    private static final String HASH_A = "a".repeat(64);
    // unique per run: AuthSchemaIT already stored "a" x 64 in the shared database
    private static final String TOKEN_HASH = java.util.UUID.randomUUID().toString().replace("-", "").repeat(2);
    private static final String HASH_B = "b".repeat(64);
    private static final Instant NOW = Instant.parse("2026-11-13T03:00:00Z");

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    AuthTestSupport support;

    private <T> T roundTrip(T entity, Class<T> type, java.util.function.Function<T, Object> id) {
        tx.executeWithoutResult(status -> entityManager.persist(entity));
        return tx.execute(status -> {
            entityManager.clear();
            return entityManager.find(type, id.apply(entity));
        });
    }

    @Test
    @DisplayName("R11: users maps role, status and the 010 columns; unmapped 011 columns are left alone")
    void userAccount() {
        long id = support.createUser(Role.MODERATOR, AccountStatus.ACTIVE, true);
        UserAccount user = tx.execute(status -> entityManager.find(UserAccount.class, id));

        assertThat(user.getRole()).isEqualTo(Role.MODERATOR);
        assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(user.getEmailVerifiedAt()).isNotNull();
        assertThat(user.getPendingRoleNoticeFrom()).isNull();
        assertThat(user.isSystem()).isFalse();
    }

    @Test
    @DisplayName("R11: CHAR(64) hashes of user_sessions round trip, including previous_token_hash")
    void userSessionCharColumns() {
        long userId = support.member();
        UserSession session = new UserSession(userId, HASH_A, true, NOW, NOW.plus(30, ChronoUnit.DAYS));
        session.rotate(HASH_B, NOW.plusSeconds(60));

        UserSession loaded = roundTrip(session, UserSession.class, UserSession::getId);

        assertThat(loaded.getTokenHash()).isEqualTo(HASH_B);
        assertThat(loaded.getPreviousTokenHash()).isEqualTo(HASH_A);
        assertThat(loaded.getRotatedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(loaded.getReauthenticatedAt()).isNull();
    }

    @Test
    @DisplayName("R11: INET round trips IPv4 and IPv6 in login_attempts")
    void loginAttemptInet() throws Exception {
        LoginAttempt v4 = roundTrip(new LoginAttempt(null, HASH_A, false, InetAddress.getByName("203.0.113.7"), NOW),
                LoginAttempt.class, LoginAttempt::getId);
        LoginAttempt v6 = roundTrip(new LoginAttempt(null, HASH_A, false, InetAddress.getByName("2001:db8::1"), NOW),
                LoginAttempt.class, LoginAttempt::getId);

        assertThat(v4.getIpAddress()).isEqualTo(InetAddress.getByName("203.0.113.7"));
        assertThat(v6.getIpAddress()).isEqualTo(InetAddress.getByName("2001:db8::1"));
        assertThat(v4.getEmailHash()).isEqualTo(HASH_A);
    }

    @Test
    @DisplayName("R11: JSONB details and INET of audit_logs round trip")
    void auditLogJsonbAndInet() throws Exception {
        AuditLog log = new AuditLog(7L, "ROLE_CHANGED", "USER", 9L,
                Map.of("from", "MEMBER", "to", "CONTRIBUTOR"), InetAddress.getByName("2001:db8::2"), NOW);

        AuditLog loaded = roundTrip(log, AuditLog.class, AuditLog::getId);

        assertThat(loaded.getDetails()).containsEntry("from", "MEMBER").containsEntry("to", "CONTRIBUTOR");
        assertThat(loaded.getIpAddress()).isEqualTo(InetAddress.getByName("2001:db8::2"));
        assertThat(loaded.getActorId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("R11: JSONB payload of email_outbox round trips")
    void emailOutboxJsonb() {
        long userId = support.member();
        EmailOutboxMessage message = new EmailOutboxMessage(userId, MailTemplate.ACCOUNT_BANNED,
                Map.of("reason", "spam", "count", 3), NOW, NOW);

        EmailOutboxMessage loaded = roundTrip(message, EmailOutboxMessage.class, EmailOutboxMessage::getId);

        assertThat(loaded.getPayload()).containsEntry("reason", "spam").containsEntry("count", 3);
        assertThat(loaded.getTemplate()).isEqualTo(MailTemplate.ACCOUNT_BANNED);
        assertThat(loaded.getAttempts()).isZero();
    }

    @Test
    @DisplayName("R11: the remaining tables map: tokens, sanctions, blocked emails and the two counters")
    void remainingEntities() {
        long userId = support.member();

        OneTimeToken token = roundTrip(new OneTimeToken(userId, TokenPurpose.ACCOUNT_CLOSURE, TOKEN_HASH,
                NOW.plusSeconds(600), NOW), OneTimeToken.class, OneTimeToken::getId);
        assertThat(token.getPurpose()).isEqualTo(TokenPurpose.ACCOUNT_CLOSURE);
        assertThat(token.getUsedAt()).isNull();

        UserSuspension suspension = roundTrip(new UserSuspension(userId, SanctionKind.SUSPENSION, "spam", NOW,
                NOW.plus(3, ChronoUnit.DAYS), null), UserSuspension.class, UserSuspension::getId);
        assertThat(suspension.getKind()).isEqualTo(SanctionKind.SUSPENSION);
        assertThat(suspension.getLiftReason()).isNull();
        tx.executeWithoutResult(status -> {
            UserSuspension managed = entityManager.find(UserSuspension.class, suspension.getId());
            managed.lift(NOW.plusSeconds(5), null, LiftReason.EXPIRED);
        });
        UserSuspension lifted = tx.execute(status -> entityManager.find(UserSuspension.class, suspension.getId()));
        assertThat(lifted.getLiftReason()).isEqualTo(LiftReason.EXPIRED);

        BlockedEmail blocked = roundTrip(new BlockedEmail(HASH_B, NOW), BlockedEmail.class, BlockedEmail::getEmailHash);
        assertThat(blocked.getEmailHash()).isEqualTo(HASH_B);

        EmailSendRequest request = roundTrip(new EmailSendRequest(HASH_A, TokenPurpose.PASSWORD_RESET, NOW),
                EmailSendRequest.class, EmailSendRequest::getId);
        assertThat(request.getPurpose()).isEqualTo(TokenPurpose.PASSWORD_RESET);

        PasswordCheckFailure failure = roundTrip(new PasswordCheckFailure(userId, NOW), PasswordCheckFailure.class,
                PasswordCheckFailure::getId);
        assertThat(failure.getUserId()).isEqualTo(userId);
    }
}
