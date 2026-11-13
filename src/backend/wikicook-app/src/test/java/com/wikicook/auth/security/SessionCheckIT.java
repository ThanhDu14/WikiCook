package com.wikicook.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.AuthTestSupport.SignedIn;
import com.wikicook.support.MatrixProbeController;
import com.wikicook.support.StatementCounting;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Research R1: every request with a bearer is checked against {@code user_sessions} JOIN {@code users} in this order:
 * no row (401), account not ACTIVE (403 with reason), revoked or expired session (401), else authorities from the
 * current role. Real JWT, real filter chain; the probe controller lives outside the auth package.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({AuthTestSupport.class, MatrixProbeController.class, StatementCounting.class})
class SessionCheckIT extends AbstractPostgresIT {

    private static final String WHOAMI = "/api/v1/probe/whoami";

    @Autowired
    MockMvc mvc;

    @Autowired
    AuthTestSupport support;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        support.resetThrottle();
    }

    private ResultActions whoami(String bearer) throws Exception {
        return mvc.perform(get(WHOAMI).header("Authorization", bearer));
    }

    @Test
    @DisplayName("FR-020: a valid session gives the caller, with authorities from users.role and EMAIL_VERIFIED")
    void validSessionGivesRoleAndVerifiedAuthority() throws Exception {
        SignedIn caller = support.signedIn(Role.CONTRIBUTOR, AccountStatus.ACTIVE, true);

        whoami(caller.bearer()).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(caller.userId()))
                .andExpect(jsonPath("$.role").value("CONTRIBUTOR"))
                .andExpect(jsonPath("$.emailVerified").value(true))
                .andExpect(jsonPath("$.sessionId").value(caller.sessionId()))
                .andExpect(jsonPath("$.authorities[0]").value("EMAIL_VERIFIED"))
                .andExpect(jsonPath("$.authorities[1]").value("ROLE_CONTRIBUTOR"));
    }

    @Test
    @DisplayName("FR-006: an unverified account has no EMAIL_VERIFIED authority")
    void unverifiedAccountHasNoVerifiedAuthority() throws Exception {
        SignedIn caller = support.signedIn(Role.MEMBER, AccountStatus.ACTIVE, false);

        whoami(caller.bearer()).andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.authorities.length()").value(1))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_MEMBER"));
    }

    @Test
    @DisplayName("US3-4, FR-024: a role change applies to the same JWT on the next request")
    void roleChangeAppliesOnTheNextRequest() throws Exception {
        SignedIn caller = support.signedIn(Role.MEMBER, AccountStatus.ACTIVE, true);
        whoami(caller.bearer()).andExpect(jsonPath("$.role").value("MEMBER"));

        jdbc.update("UPDATE users SET role = 'MODERATOR' WHERE id = ?", caller.userId());

        whoami(caller.bearer()).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("MODERATOR"))
                .andExpect(jsonPath("$.authorities[1]").value("ROLE_MODERATOR"));
    }

    @Test
    @DisplayName("Edge: Account deleted after 7 days - step 1: no session row is 401 UNAUTHENTICATED")
    void deletedAccountIs401() throws Exception {
        SignedIn caller = support.signedInMember();
        jdbc.update("DELETE FROM users WHERE id = ?", caller.userId());

        whoami(caller.bearer()).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Step 1: a JWT for a session id that does not exist is 401")
    void unknownSessionIs401() throws Exception {
        long user = support.member();

        whoami("Bearer " + support.jwt(user, 987654321L)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Step 1: sub and session owner must agree, a JWT cannot borrow another user's session")
    void tokenWithAnotherUsersSessionIs401() throws Exception {
        SignedIn owner = support.signedInMember();
        long other = support.member();

        whoami("Bearer " + support.jwt(other, owner.sessionId())).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("US3-6, FR-013: a suspended account is 403 ACCOUNT_SUSPENDED with reason and endsAt")
    void suspendedAccountIs403WithReasonAndEnd() throws Exception {
        SignedIn caller = support.signedIn(Role.MEMBER, AccountStatus.SUSPENDED, true);
        Instant endsAt = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        support.sanction(caller.userId(), "Spam in reviews", endsAt);

        whoami(caller.bearer()).andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("ACCOUNT_SUSPENDED"))
                .andExpect(jsonPath("$.reason").value("Spam in reviews"))
                .andExpect(jsonPath("$.endsAt").value(endsAt.toString()));
    }

    @Test
    @DisplayName("FR-013: a banned account is 403 ACCOUNT_BANNED with a reason and no endsAt")
    void bannedAccountIs403WithoutEnd() throws Exception {
        SignedIn caller = support.signedIn(Role.MEMBER, AccountStatus.BANNED, true);
        support.sanction(caller.userId(), "Fraud", null);

        whoami(caller.bearer()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_BANNED"))
                .andExpect(jsonPath("$.reason").value("Fraud"))
                .andExpect(jsonPath("$.endsAt").doesNotExist());
    }

    @Test
    @DisplayName("Step 2 before 3: a suspension revokes the session, the user still sees the suspension, not 'expired'")
    void sanctionShownEvenWhenTheSessionIsAlreadyRevoked() throws Exception {
        SignedIn caller = support.signedIn(Role.MEMBER, AccountStatus.SUSPENDED, true);
        support.sanction(caller.userId(), "Spam", Instant.now().plus(1, ChronoUnit.DAYS));
        jdbc.update("UPDATE user_sessions SET revoked_at = now() WHERE id = ?", caller.sessionId());

        whoami(caller.bearer()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_SUSPENDED"));
    }

    @Test
    @DisplayName("US1-4, FR-010: a revoked session is 401 even while the JWT is still valid")
    void revokedSessionIs401() throws Exception {
        SignedIn caller = support.signedInMember();
        whoami(caller.bearer()).andExpect(status().isOk());

        jdbc.update("UPDATE user_sessions SET revoked_at = now() WHERE id = ?", caller.sessionId());

        whoami(caller.bearer()).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("FR-009: a session past expires_at is 401")
    void expiredSessionIs401() throws Exception {
        long user = support.member();
        long session = support.session(user, Instant.now().minusSeconds(60), Instant.now().minusSeconds(1), null);

        whoami("Bearer " + support.jwt(user, session)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("A token that is not a JWT is 401 UNAUTHENTICATED")
    void garbageBearerIs401() throws Exception {
        whoami("Bearer not-a-jwt").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("R1: one query per request when the activity time is fresh")
    void oneQueryPerRequest() throws Exception {
        SignedIn caller = support.signedInMember();

        StatementCounting.reset();
        whoami(caller.bearer()).andExpect(status().isOk());

        assertThat(StatementCounting.statements()).hasSize(1);
        assertThat(StatementCounting.statements().get(0)).containsIgnoringCase("user_sessions")
                .containsIgnoringCase("users");
    }

    @Test
    @DisplayName("R2: last_activity_at is written only when it is older than 5 minutes")
    void lastActivityIsUpdatedAtMostEveryFiveMinutes() throws Exception {
        long user = support.member();
        Instant stale = Instant.now().minus(10, ChronoUnit.MINUTES);
        long staleSession = support.session(user, stale, Instant.now().plus(1, ChronoUnit.DAYS), null);
        Instant fresh = Instant.now().minus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS);
        long freshSession = support.session(user, fresh, Instant.now().plus(1, ChronoUnit.DAYS), null);

        StatementCounting.reset();
        whoami("Bearer " + support.jwt(user, staleSession)).andExpect(status().isOk());
        assertThat(StatementCounting.statements()).hasSize(2); // the check and the activity update
        whoami("Bearer " + support.jwt(user, freshSession)).andExpect(status().isOk());

        assertThat(lastActivity(staleSession)).isAfter(Instant.now().minus(1, ChronoUnit.MINUTES));
        assertThat(lastActivity(freshSession)).isEqualTo(fresh);
    }

    private Instant lastActivity(long sessionId) {
        Timestamp timestamp = jdbc.queryForObject("SELECT last_activity_at FROM user_sessions WHERE id = ?",
                Timestamp.class, sessionId);
        return timestamp.toInstant();
    }
}
