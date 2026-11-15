package com.wikicook.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.auth.api.AccountAdminFacade;
import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.AuthTestSupport.SignedIn;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * User story 3 on the admin API (FR-022 - FR-024a, FR-025; research R9, R10): role changes, suspensions, bans and
 * reinstatements take effect on the next request, are audited and mailed through the outbox, never remove the last
 * active Administrator, and cope with a suspension that ended but was not yet closed by the job.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthTestSupport.class)
class AccountAdminIT extends AbstractPostgresIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    AuthTestSupport support;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    @Autowired
    AccountAdminFacade facade;

    @BeforeEach
    void setUp() {
        support.resetThrottle();
    }

    private SignedIn admin() {
        return support.signedIn(Role.ADMINISTRATOR, AccountStatus.ACTIVE, true);
    }

    private SignedIn moderator() {
        return support.signedIn(Role.MODERATOR, AccountStatus.ACTIVE, true);
    }

    private long member() {
        return support.createUser(Role.MEMBER, AccountStatus.ACTIVE, true);
    }

    private MockHttpServletResponse call(MockHttpServletRequestBuilder request, SignedIn as, Object body)
            throws Exception {
        if (as != null) {
            request.header("Authorization", as.bearer());
        }
        if (body != null) {
            request.contentType("application/json").content(json.writeValueAsString(body));
        }
        return mvc.perform(request).andReturn().getResponse();
    }

    private MockHttpServletResponse changeRole(SignedIn as, long target, String role) throws Exception {
        return call(patch("/api/v1/admin/users/" + target + "/role"), as, Map.of("role", role));
    }

    private MockHttpServletResponse suspend(SignedIn as, long target, String reason, Instant endsAt)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reason", reason);
        body.put("endsAt", endsAt.toString());
        return call(post("/api/v1/admin/users/" + target + "/suspension"), as, body);
    }

    private MockHttpServletResponse ban(SignedIn as, long target, String reason) throws Exception {
        return call(post("/api/v1/admin/users/" + target + "/ban"), as, Map.of("reason", reason));
    }

    private MockHttpServletResponse reinstate(SignedIn as, long target) throws Exception {
        return call(post("/api/v1/admin/users/" + target + "/reinstatement"), as, Map.of("reason", "appeal accepted"));
    }

    private JsonNode body(MockHttpServletResponse response) throws Exception {
        return json.readTree(response.getContentAsString());
    }

    private String code(MockHttpServletResponse response) throws Exception {
        return body(response).get("code").asText();
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private long outbox(long userId, String template) {
        return count("SELECT count(*) FROM email_outbox WHERE user_id = ? AND template = ?", userId, template);
    }

    private long audit(String action, long targetId) {
        return count("SELECT count(*) FROM audit_logs WHERE action = ? AND target_id = ?", action, targetId);
    }

    private static Instant in(Duration duration) {
        return Instant.now().plus(duration).truncatedTo(ChronoUnit.SECONDS);
    }

    private MockHttpServletResponse me(SignedIn as) throws Exception {
        return mvc.perform(get("/api/v1/auth/me").header("Authorization", as.bearer())).andReturn().getResponse();
    }

    /** A suspended account whose suspension ended a minute ago and which the job has not closed yet. */
    private SignedIn expiredButNotClosedSuspension() {
        SignedIn target = support.signedIn(Role.MEMBER, AccountStatus.SUSPENDED, true);
        support.sanction(target.userId(), "Spam", Instant.now().minusSeconds(60));
        return target;
    }

    // ---- roles (US3-3, US3-4, US3-5) ----

    @Test
    @DisplayName("US3-4, FR-024: a role change applies to the next request of that user and is audited, without an email")
    void roleChangeAppliesOnTheNextRequest() throws Exception {
        SignedIn admin = admin();
        SignedIn target = support.signedInMember();
        assertThat(body(me(target)).get("role").asText()).isEqualTo("MEMBER");

        MockHttpServletResponse response = changeRole(admin, target.userId(), "CONTRIBUTOR");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(body(response).get("role").asText()).isEqualTo("CONTRIBUTOR");
        assertThat(body(response).get("email").asText()).as("an Administrator sees the email").isNotBlank();
        assertThat(body(me(target)).get("role").asText()).as("same JWT, next request").isEqualTo("CONTRIBUTOR");
        assertThat(jdbc.queryForObject("SELECT details->>'from' || '>' || (details->>'to') FROM audit_logs "
                + "WHERE action = 'ROLE_CHANGED' AND target_id = ?", String.class, target.userId()))
                .isEqualTo("MEMBER>CONTRIBUTOR");
        assertThat(jdbc.queryForObject("SELECT actor_id FROM audit_logs WHERE action = 'ROLE_CHANGED' AND "
                + "target_id = ?", Long.class, target.userId())).isEqualTo(admin.userId());
        assertThat(count("SELECT count(*) FROM email_outbox WHERE user_id = ?", target.userId()))
                .as("a role change sends no email (FR-024a)").isZero();
        assertThat(jdbc.queryForObject("SELECT pending_role_notice_from FROM users WHERE id = ?", String.class,
                target.userId())).isEqualTo("MEMBER");
    }

    @Test
    @DisplayName("US3-3: a Moderator cannot change a role")
    void moderatorCannotChangeARole() throws Exception {
        long target = member();

        MockHttpServletResponse response = changeRole(moderator(), target, "CONTRIBUTOR");

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(code(response)).isEqualTo("ACCESS_DENIED");
        assertThat(jdbc.queryForObject("SELECT role FROM users WHERE id = ?", String.class, target))
                .isEqualTo("MEMBER");
    }

    @Test
    @DisplayName("US3-5: a user whose role was lowered is refused the privileged action on the same JWT")
    void loweredRoleIsRefusedOnTheNextPrivilegedRequest() throws Exception {
        SignedIn admin = admin();
        SignedIn mod = moderator();
        long victim = member();
        assertThat(suspend(mod, victim, "first", in(Duration.ofDays(1))).getStatus()).isEqualTo(200);
        jdbc.update("UPDATE users SET status = 'ACTIVE' WHERE id = ?", victim);
        jdbc.update("UPDATE user_suspensions SET lifted_at = now(), lift_reason = 'LIFTED' WHERE user_id = ?", victim);

        assertThat(changeRole(admin, mod.userId(), "MEMBER").getStatus()).isEqualTo(200);

        MockHttpServletResponse refused = suspend(mod, victim, "again", in(Duration.ofDays(1)));
        assertThat(refused.getStatus()).isEqualTo(403);
        assertThat(code(refused)).isEqualTo("ACCESS_DENIED");
    }

    @Test
    @DisplayName("FR-024a: several role changes before the notice is seen keep the original role, going back clears it")
    void roleNoticeKeepsTheOriginalRoleAndClearsWhenBack() throws Exception {
        SignedIn admin = admin();
        long target = member();

        changeRole(admin, target, "CONTRIBUTOR");
        changeRole(admin, target, "MODERATOR");
        assertThat(jdbc.queryForObject("SELECT pending_role_notice_from FROM users WHERE id = ?", String.class,
                target)).isEqualTo("MEMBER");

        changeRole(admin, target, "MEMBER");
        assertThat(jdbc.queryForObject("SELECT pending_role_notice_from FROM users WHERE id = ?", String.class,
                target)).isNull();
    }

    @Test
    @DisplayName("FR-022a: changing to the role the user already has changes nothing and is not audited")
    void sameRoleIsANoOp() throws Exception {
        long target = member();

        MockHttpServletResponse response = changeRole(admin(), target, "MEMBER");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(audit("ROLE_CHANGED", target)).isZero();
    }

    @Test
    @DisplayName("FR-022a: an Administrator cannot change their own role, nor a system account's, and an unknown id is 404")
    void selfSystemAndUnknownTargets() throws Exception {
        SignedIn admin = admin();

        MockHttpServletResponse self = changeRole(admin, admin.userId(), "MEMBER");
        assertThat(self.getStatus()).isEqualTo(403);
        assertThat(code(self)).isEqualTo("SELF_ACTION_FORBIDDEN");

        MockHttpServletResponse system = changeRole(admin, support.systemAccount(), "MODERATOR");
        assertThat(system.getStatus()).isEqualTo(403);
        assertThat(code(system)).isEqualTo("ACCESS_DENIED");

        MockHttpServletResponse unknown = changeRole(admin, 987654321L, "MODERATOR");
        assertThat(unknown.getStatus()).isEqualTo(404);
        assertThat(code(unknown)).isEqualTo("USER_NOT_FOUND");
    }

    @Test
    @DisplayName("FR-022a: acting on another Administrator needs a re-authentication from the last 5 minutes")
    void actionOnAnotherAdministratorNeedsReauthentication() throws Exception {
        SignedIn admin = admin();
        long other = support.createUser(Role.ADMINISTRATOR, AccountStatus.ACTIVE, true);

        MockHttpServletResponse without = changeRole(admin, other, "MODERATOR");
        assertThat(without.getStatus()).isEqualTo(403);
        assertThat(code(without)).isEqualTo("REAUTH_REQUIRED");

        support.reauthenticated(admin.sessionId(), Instant.now().minus(Duration.ofMinutes(6)));
        assertThat(code(changeRole(admin, other, "MODERATOR"))).isEqualTo("REAUTH_REQUIRED");

        support.reauthenticated(admin.sessionId(), Instant.now().minus(Duration.ofMinutes(4)));
        assertThat(changeRole(admin, other, "MODERATOR").getStatus()).isEqualTo(200);
        assertThat(audit("ROLE_CHANGED", other)).isEqualTo(1);
    }

    @Test
    @DisplayName("FR-022a: promoting to Administrator needs the re-authentication too")
    void promotionToAdministratorNeedsReauthentication() throws Exception {
        SignedIn admin = admin();
        long target = member();

        assertThat(code(changeRole(admin, target, "ADMINISTRATOR"))).isEqualTo("REAUTH_REQUIRED");
        support.reauthenticated(admin.sessionId(), Instant.now());
        assertThat(changeRole(admin, target, "ADMINISTRATOR").getStatus()).isEqualTo(200);
    }

    // ---- suspensions (US3-6) ----

    @Test
    @DisplayName("US3-6, FR-024, FR-024a: a Moderator suspends a Member for 30 days, sessions end at once, mail and audit follow")
    void moderatorSuspendsAMember() throws Exception {
        SignedIn mod = moderator();
        SignedIn target = support.signedInMember();
        long secondSession = support.session(target.userId());
        Instant endsAt = in(Duration.ofDays(30));

        MockHttpServletResponse response = suspend(mod, target.userId(), "Spam in reviews", endsAt);

        assertThat(response.getStatus()).isEqualTo(200);
        JsonNode body = body(response);
        assertThat(body.get("status").asText()).isEqualTo("SUSPENDED");
        assertThat(body.get("activeSanction").get("kind").asText()).isEqualTo("SUSPENSION");
        assertThat(body.get("activeSanction").get("reason").asText()).isEqualTo("Spam in reviews");
        assertThat(body.has("email")).as("a Moderator never sees the email (NFR-009)").isFalse();
        MockHttpServletResponse next = me(target);
        assertThat(next.getStatus()).as("next request").isEqualTo(403);
        assertThat(code(next)).isEqualTo("ACCOUNT_SUSPENDED");
        assertThat(count("SELECT count(*) FROM user_sessions WHERE user_id = ? AND revoked_at IS NULL",
                target.userId())).as("every session revoked in the same transaction").isZero();
        assertThat(secondSession).isPositive();
        assertThat(outbox(target.userId(), "ACCOUNT_SUSPENDED")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT payload->>'reason' FROM email_outbox WHERE user_id = ? AND "
                + "template = 'ACCOUNT_SUSPENDED'", String.class, target.userId())).isEqualTo("Spam in reviews");
        assertThat(jdbc.queryForObject("SELECT payload->>'endsAt' FROM email_outbox WHERE user_id = ? AND "
                + "template = 'ACCOUNT_SUSPENDED'", String.class, target.userId())).isEqualTo(endsAt.toString());
        assertThat(audit("ACCOUNT_SUSPENDED", target.userId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT actor_id FROM audit_logs WHERE action = 'ACCOUNT_SUSPENDED' AND "
                + "target_id = ?", Long.class, target.userId())).isEqualTo(mod.userId());
    }

    @Test
    @DisplayName("US3-6: 31 days, banning, lifting and a Moderator or Administrator target are refused for a Moderator")
    void moderatorLimits() throws Exception {
        SignedIn mod = moderator();
        long victim = member();

        MockHttpServletResponse tooLong = suspend(mod, victim, "x", in(Duration.ofDays(31)));
        assertThat(tooLong.getStatus()).isEqualTo(403);
        assertThat(code(tooLong)).isEqualTo("SUSPENSION_LIMIT_EXCEEDED");
        assertThat(code(ban(mod, victim, "x"))).isEqualTo("ACCESS_DENIED");
        assertThat(code(reinstate(mod, victim))).isEqualTo("ACCESS_DENIED");
        long otherModerator = support.createUser(Role.MODERATOR, AccountStatus.ACTIVE, true);
        assertThat(code(suspend(mod, otherModerator, "x", in(Duration.ofDays(1))))).isEqualTo("ACCESS_DENIED");
        long administrator = support.createUser(Role.ADMINISTRATOR, AccountStatus.ACTIVE, true);
        assertThat(code(suspend(mod, administrator, "x", in(Duration.ofDays(1))))).isEqualTo("ACCESS_DENIED");
        assertThat(count("SELECT count(*) FROM user_suspensions WHERE user_id IN (?, ?, ?)", victim, otherModerator,
                administrator)).isZero();
    }

    @Test
    @DisplayName("FR-022: an Administrator may suspend for 30 days but not for 31")
    void administratorSuspensionLimitIsThirtyDaysToo() throws Exception {
        SignedIn admin = admin();
        long a = member();
        long b = member();

        assertThat(suspend(admin, a, "x", in(Duration.ofDays(30))).getStatus()).isEqualTo(200);
        MockHttpServletResponse tooLong = suspend(admin, b, "x", in(Duration.ofDays(31)));
        assertThat(tooLong.getStatus()).isEqualTo(403);
        assertThat(code(tooLong)).isEqualTo("SUSPENSION_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("FR-013: reason is required (1-500 characters) and the end date must be in the future")
    void suspensionInputIsValidated() throws Exception {
        SignedIn mod = moderator();
        long victim = member();

        assertThat(suspend(mod, victim, "", in(Duration.ofDays(1))).getStatus()).isEqualTo(400);
        assertThat(suspend(mod, victim, "x".repeat(501), in(Duration.ofDays(1))).getStatus()).isEqualTo(400);
        assertThat(suspend(mod, victim, "ok", Instant.now().minusSeconds(60)).getStatus()).isEqualTo(400);
        assertThat(suspend(mod, victim, "x".repeat(500), in(Duration.ofDays(1))).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("FR-022: suspending an account that is not ACTIVE, or banning one that is BANNED, is 409 ACCOUNT_NOT_ACTIVE")
    void sanctionsNeedAnActiveAccount() throws Exception {
        SignedIn admin = admin();
        long banned = support.createUser(Role.MEMBER, AccountStatus.BANNED, true);
        support.sanction(banned, "Fraud", null);
        long suspended = support.createUser(Role.MEMBER, AccountStatus.SUSPENDED, true);
        support.sanction(suspended, "Spam", Instant.now().plus(Duration.ofDays(5)));

        MockHttpServletResponse suspendBanned = suspend(admin, banned, "x", in(Duration.ofDays(1)));
        assertThat(suspendBanned.getStatus()).isEqualTo(409);
        assertThat(code(suspendBanned)).isEqualTo("ACCOUNT_NOT_ACTIVE");
        assertThat(code(suspend(admin, suspended, "x", in(Duration.ofDays(1))))).isEqualTo("ACCOUNT_NOT_ACTIVE");
        MockHttpServletResponse banBanned = ban(admin, banned, "x");
        assertThat(banBanned.getStatus()).isEqualTo(409);
        assertThat(code(banBanned)).isEqualTo("ACCOUNT_NOT_ACTIVE");
    }

    // ---- bans, reinstatement ----

    @Test
    @DisplayName("FR-022, FR-024a: banning a suspended account closes the old sanction as REPLACED_BY_BAN and mails the ban")
    void banReplacesARunningSuspension() throws Exception {
        SignedIn admin = admin();
        SignedIn target = support.signedIn(Role.MEMBER, AccountStatus.SUSPENDED, true);
        long old = support.sanction(target.userId(), "Spam", Instant.now().plus(Duration.ofDays(5)));

        MockHttpServletResponse response = ban(admin, target.userId(), "Fraud");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(body(response).get("status").asText()).isEqualTo("BANNED");
        Map<String, Object> closed = jdbc.queryForMap("SELECT lift_reason, lifted_by FROM user_suspensions "
                + "WHERE id = ?", old);
        assertThat(closed.get("lift_reason")).isEqualTo("REPLACED_BY_BAN");
        assertThat(closed.get("lifted_by")).isEqualTo(admin.userId());
        assertThat(jdbc.queryForObject("SELECT kind FROM user_suspensions WHERE user_id = ? AND lifted_at IS NULL",
                String.class, target.userId())).isEqualTo("BAN");
        assertThat(outbox(target.userId(), "ACCOUNT_BANNED")).isEqualTo(1);
        assertThat(audit("ACCOUNT_BANNED", target.userId())).isEqualTo(1);
        MockHttpServletResponse next = me(target);
        assertThat(next.getStatus()).isEqualTo(403);
        assertThat(code(next)).isEqualTo("ACCOUNT_BANNED");
    }

    @Test
    @DisplayName("Q45, FR-022, FR-024a: an Administrator reinstates a ban and a suspension early: mail, audit, LIFTED")
    void administratorReinstates() throws Exception {
        SignedIn admin = admin();
        long banned = support.createUser(Role.MEMBER, AccountStatus.BANNED, true);
        long bannedSanction = support.sanction(banned, "Fraud", null);
        long suspended = support.createUser(Role.MEMBER, AccountStatus.SUSPENDED, true);
        long suspensionRow = support.sanction(suspended, "Spam", Instant.now().plus(Duration.ofDays(5)));

        assertThat(reinstate(admin, banned).getStatus()).isEqualTo(200);
        assertThat(reinstate(admin, suspended).getStatus()).isEqualTo(200);

        assertThat(jdbc.queryForObject("SELECT status FROM users WHERE id = ?", String.class, banned))
                .isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT status FROM users WHERE id = ?", String.class, suspended))
                .isEqualTo("ACTIVE");
        assertThat(outbox(banned, "BAN_LIFTED")).isEqualTo(1);
        assertThat(outbox(suspended, "SUSPENSION_ENDED")).isEqualTo(1);
        assertThat(audit("ACCOUNT_REINSTATED", banned)).isEqualTo(1);
        assertThat(audit("ACCOUNT_REINSTATED", suspended)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT lift_reason FROM user_suspensions WHERE id = ?", String.class,
                bannedSanction)).isEqualTo("LIFTED");
        assertThat(jdbc.queryForObject("SELECT lifted_by FROM user_suspensions WHERE id = ?", Long.class,
                suspensionRow)).isEqualTo(admin.userId());
    }

    @Test
    @DisplayName("FR-022: reinstating an ACTIVE account is 409 ACCOUNT_NOT_SANCTIONED and sends nothing")
    void reinstatingAnActiveAccountIsRefused() throws Exception {
        long active = member();

        MockHttpServletResponse response = reinstate(admin(), active);

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(code(response)).isEqualTo("ACCOUNT_NOT_SANCTIONED");
        assertThat(count("SELECT count(*) FROM email_outbox WHERE user_id = ?", active)).isZero();
        assertThat(audit("ACCOUNT_REINSTATED", active)).isZero();
    }

    @Test
    @DisplayName("US3-6 / R10: the lift reason stays LIFTED after the lifting Administrator's account is deleted")
    void liftReasonSurvivesDeletionOfTheLifter() throws Exception {
        SignedIn admin = admin();
        long banned = support.createUser(Role.MEMBER, AccountStatus.BANNED, true);
        long sanction = support.sanction(banned, "Fraud", null);
        reinstate(admin, banned);

        jdbc.update("DELETE FROM users WHERE id = ?", admin.userId());

        Map<String, Object> row = jdbc.queryForMap("SELECT lift_reason, lifted_by FROM user_suspensions WHERE id = ?",
                sanction);
        assertThat(row.get("lift_reason")).isEqualTo("LIFTED");
        assertThat(row.get("lifted_by")).isNull();
    }

    // ---- a suspension that ended but was not closed by the job (research R9) ----

    @Test
    @DisplayName("R9: suspending again after ends_at succeeds; the old suspension is EXPIRED and SUSPENSION_ENDED is mailed once")
    void suspendAgainAfterTheEndDate() throws Exception {
        SignedIn mod = moderator();
        SignedIn target = expiredButNotClosedSuspension();

        MockHttpServletResponse response = suspend(mod, target.userId(), "Again", in(Duration.ofDays(2)));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(jdbc.queryForList("SELECT lift_reason FROM user_suspensions WHERE user_id = ? AND lifted_at IS "
                + "NOT NULL", String.class, target.userId())).containsExactly("EXPIRED");
        assertThat(outbox(target.userId(), "SUSPENSION_ENDED")).isEqualTo(1);
        assertThat(outbox(target.userId(), "ACCOUNT_SUSPENDED")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM users WHERE id = ?", String.class, target.userId()))
                .isEqualTo("SUSPENDED");
    }

    @Test
    @DisplayName("R9: banning after ends_at records EXPIRED for the old suspension, not REPLACED_BY_BAN")
    void banAfterTheEndDateRecordsExpired() throws Exception {
        SignedIn admin = admin();
        SignedIn target = expiredButNotClosedSuspension();

        assertThat(ban(admin, target.userId(), "Fraud").getStatus()).isEqualTo(200);

        assertThat(jdbc.queryForList("SELECT lift_reason FROM user_suspensions WHERE user_id = ? AND kind = "
                + "'SUSPENSION'", String.class, target.userId())).containsExactly("EXPIRED");
        assertThat(outbox(target.userId(), "SUSPENSION_ENDED")).isEqualTo(1);
        assertThat(outbox(target.userId(), "ACCOUNT_BANNED")).isEqualTo(1);
    }

    @Test
    @DisplayName("R9: reinstating after ends_at is 409 ACCOUNT_NOT_SANCTIONED, without ACCOUNT_REINSTATED, and the end is kept")
    void reinstateAfterTheEndDateKeepsTheEndOfTheSuspension() throws Exception {
        SignedIn admin = admin();
        SignedIn target = expiredButNotClosedSuspension();

        MockHttpServletResponse response = reinstate(admin, target.userId());

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(code(response)).isEqualTo("ACCOUNT_NOT_SANCTIONED");
        assertThat(audit("ACCOUNT_REINSTATED", target.userId())).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM users WHERE id = ?", String.class, target.userId()))
                .as("the 409 does not roll the ending back").isEqualTo("ACTIVE");
        assertThat(outbox(target.userId(), "SUSPENSION_ENDED")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT lift_reason FROM user_suspensions WHERE user_id = ?", String.class,
                target.userId())).isEqualTo("EXPIRED");
        Timestamp liftedAt = jdbc.queryForObject("SELECT lifted_at FROM user_suspensions WHERE user_id = ?",
                Timestamp.class, target.userId());
        Timestamp endsAt = jdbc.queryForObject("SELECT ends_at FROM user_suspensions WHERE user_id = ?",
                Timestamp.class, target.userId());
        assertThat(liftedAt).isEqualTo(endsAt);
    }

    @Test
    @DisplayName("R9: a role change after ends_at also closes the ended suspension first")
    void roleChangeClosesAnEndedSuspension() throws Exception {
        SignedIn admin = admin();
        SignedIn target = expiredButNotClosedSuspension();

        assertThat(changeRole(admin, target.userId(), "CONTRIBUTOR").getStatus()).isEqualTo(200);

        assertThat(jdbc.queryForObject("SELECT status FROM users WHERE id = ?", String.class, target.userId()))
                .isEqualTo("ACTIVE");
        assertThat(outbox(target.userId(), "SUSPENSION_ENDED")).isEqualTo(1);
    }

    // ---- the last active Administrator (FR-023) ----

    @Test
    @DisplayName("FR-023: the last active Administrator cannot be demoted, suspended or banned, even by a suspended Administrator")
    void lastActiveAdministratorIsProtected() throws Exception {
        jdbc.update("UPDATE users SET status = 'SUSPENDED' WHERE role = 'ADMINISTRATOR'");
        long last = support.createUser(Role.ADMINISTRATOR, AccountStatus.ACTIVE, true);
        SignedIn suspendedAdmin = support.signedIn(Role.ADMINISTRATOR, AccountStatus.SUSPENDED, true);
        support.reauthenticated(suspendedAdmin.sessionId(), Instant.now());
        SecurityContextHolder.getContext().setAuthentication(new com.wikicook.auth.security.AccountAuthentication(
                new com.wikicook.auth.security.AuthenticatedAccount(suspendedAdmin.userId(), Role.ADMINISTRATOR, true,
                        suspendedAdmin.sessionId(), null), AuthorityUtils.createAuthorityList("ROLE_ADMINISTRATOR")));
        try {
            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> facade.changeRole(suspendedAdmin.userId(), last, Role.MODERATOR))
                    .hasFieldOrPropertyWithValue("code", com.wikicook.auth.service.AuthErrorCode.LAST_ADMINISTRATOR);
            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> facade.suspend(suspendedAdmin.userId(), last, "x", in(Duration.ofDays(1))))
                    .hasFieldOrPropertyWithValue("code", com.wikicook.auth.service.AuthErrorCode.LAST_ADMINISTRATOR);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> facade.ban(suspendedAdmin.userId(), last, "x"))
                    .hasFieldOrPropertyWithValue("code", com.wikicook.auth.service.AuthErrorCode.LAST_ADMINISTRATOR);
        } finally {
            SecurityContextHolder.clearContext();
        }
        assertThat(jdbc.queryForObject("SELECT role || status FROM users WHERE id = ?", String.class, last))
                .isEqualTo("ADMINISTRATORACTIVE");
    }

    @Test
    @DisplayName("FR-023: two Administrators demoting each other at the same time still leave one")
    void concurrentDemotionsLeaveOneAdministrator() throws Exception {
        jdbc.update("UPDATE users SET status = 'SUSPENDED' WHERE role = 'ADMINISTRATOR'");
        SignedIn a = admin();
        SignedIn b = admin();
        support.reauthenticated(a.sessionId(), Instant.now());
        support.reauthenticated(b.sessionId(), Instant.now());

        List<Integer> statuses = new ArrayList<>();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Integer> first = pool.submit(() -> changeRole(a, b.userId(), "MODERATOR").getStatus());
            Future<Integer> second = pool.submit(() -> changeRole(b, a.userId(), "MODERATOR").getStatus());
            statuses.add(first.get(30, TimeUnit.SECONDS));
            statuses.add(second.get(30, TimeUnit.SECONDS));
        }

        assertThat(statuses).allMatch(s -> s == 200 || s == 403 || s == 409);
        assertThat(count("SELECT count(*) FROM users WHERE role = 'ADMINISTRATOR' AND status = 'ACTIVE'"))
                .isGreaterThanOrEqualTo(1);
        assertThat(statuses.stream().filter(s -> s == 200)).hasSize(1);
    }

    @Test
    @DisplayName("FR-024: the facade used by spec 100 is the same operation as the REST call")
    void facadeAndRestAreTheSameOperation() throws Exception {
        SignedIn admin = admin();
        long target = member();
        SecurityContextHolder.getContext().setAuthentication(new com.wikicook.auth.security.AccountAuthentication(
                new com.wikicook.auth.security.AuthenticatedAccount(admin.userId(), Role.ADMINISTRATOR, true,
                        admin.sessionId(), null), AuthorityUtils.createAuthorityList("ROLE_ADMINISTRATOR")));
        try {
            assertThat(facade.changeRole(admin.userId(), target, Role.CONTRIBUTOR).role()).isEqualTo(Role.CONTRIBUTOR);
        } finally {
            SecurityContextHolder.clearContext();
        }
        assertThat(audit("ROLE_CHANGED", target)).isEqualTo(1);
    }

    @Test
    @DisplayName("NFR-009 / FR-022: viewing an account is for Administrators only, with email and the running sanction")
    void viewAccountIsForAdministrators() throws Exception {
        SignedIn admin = admin();
        long banned = support.createUser(Role.MEMBER, AccountStatus.BANNED, true);
        support.sanction(banned, "Fraud", null);

        MockHttpServletResponse ok = call(get("/api/v1/admin/users/" + banned), admin, null);
        assertThat(ok.getStatus()).isEqualTo(200);
        assertThat(body(ok).get("email").asText()).isNotBlank();
        assertThat(body(ok).get("activeSanction").get("kind").asText()).isEqualTo("BAN");
        assertThat(call(get("/api/v1/admin/users/" + banned), moderator(), null).getStatus()).isEqualTo(403);
        assertThat(call(get("/api/v1/admin/users/" + banned), null, null).getStatus()).isEqualTo(401);
        assertThat(call(get("/api/v1/admin/users/987654321"), admin, null).getStatus()).isEqualTo(404);
    }
}
