package com.wikicook.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.MutableClock;
import com.wikicook.support.RecordingMailSender;
import com.wikicook.support.RecordingMailSender.Recorder;
import jakarta.mail.internet.MimeMessage;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Password reset over HTTP (US4, FR-014 to FR-017, FR-004, FR-012, NFR-003; research R6, R8): the 30 minute single-use
 * link, the neutral answer, the three-per-15-minutes limit, the single transaction of the change, and an SMTP failure
 * that the user fixes by asking again.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({AuthTestSupport.class, RecordingMailSender.class, MutableClock.class})
class PasswordResetIT extends AbstractPostgresIT {

    private static final String PASSWORD = "abc12345";
    private static final String NEW_PASSWORD = "newpass987";
    private static final Pattern LINK = Pattern.compile("reset-password#token=([A-Za-z0-9_-]+)");
    private static final String FORGOT = "/api/v1/auth/password/forgot";
    private static final String VALIDATE = "/api/v1/auth/password/reset/validate";
    private static final String RESET = "/api/v1/auth/password/reset";

    @Autowired
    MockMvc mvc;

    @Autowired
    AuthTestSupport support;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    @Autowired
    MutableClock clock;

    private final Recorder smtp = RecordingMailSender.recorder();

    @BeforeEach
    void setUp() {
        clock.reset();
        smtp.clear();
        support.resetThrottle();
    }

    private MockHttpServletResponse post(String path, Map<String, Object> body) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(path).contentType("application/json")
                .content(json.writeValueAsString(body))).andReturn().getResponse();
    }

    private JsonNode body(MockHttpServletResponse response) throws Exception {
        return json.readTree(response.getContentAsString());
    }

    private MockHttpServletResponse forgot(String email) throws Exception {
        return post(FORGOT, Map.of("email", email));
    }

    private MockHttpServletResponse validate(String token) throws Exception {
        return post(VALIDATE, Map.of("token", token));
    }

    private MockHttpServletResponse reset(String token, String password) throws Exception {
        return post(RESET, Map.of("token", token, "newPassword", password, "confirmPassword", password));
    }

    private MockHttpServletResponse login(String email, String password) throws Exception {
        return post("/api/v1/auth/login", Map.of("email", email, "password", password));
    }

    /** The token of the {@code n}-th mail to this address, waiting for the background send. */
    private String linkTokenOf(String email, int n) {
        await().atMost(Duration.ofSeconds(10)).until(() -> smtp.messagesTo(email).size() >= n);
        MimeMessage message = smtp.messagesTo(email).get(n - 1);
        Matcher matcher = LINK.matcher(Recorder.parts(message).get(0));
        assertThat(matcher.find()).as("the reset link in the mail").isTrue();
        return matcher.group(1);
    }

    /** Asks for a link for a fresh active account with a password and returns the account's email and the token. */
    private String[] accountWithLink() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        support.memberWithPassword(email, PASSWORD);
        assertThat(forgot(email).getStatus()).isEqualTo(202);
        return new String[] {email, linkTokenOf(email, 1)};
    }

    private long idOf(String email) {
        return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private void assertNothingSentTo(String email, long userId) {
        await().during(Duration.ofMillis(600)).atMost(Duration.ofSeconds(5)).until(
                () -> smtp.messagesTo(email).isEmpty()
                        && count("SELECT count(*) FROM one_time_tokens WHERE user_id = ?", userId) == 0);
    }

    // ---- forgot ----------------------------------------------------------------------------------------------

    @Test
    @DisplayName("US4-1, FR-014: a registered address gets one mail with a reset link; 202 with an empty body")
    void registeredAddressGetsALink() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        support.memberWithPassword(email, PASSWORD);

        MockHttpServletResponse response = forgot("  " + email.toUpperCase() + " ");

        assertThat(response.getStatus()).isEqualTo(202);
        assertThat(response.getContentAsString()).isEmpty();
        String token = linkTokenOf(email, 1);
        assertThat(token).hasSizeGreaterThan(30);
        MimeMessage mail = smtp.messagesTo(email).get(0);
        assertThat(mail.getSubject()).containsIgnoringCase("mật khẩu");
        assertThat(Recorder.parts(mail)).hasSize(2).allSatisfy(part -> assertThat(part).contains("30 phút")
                .contains("http://localhost:3000/reset-password#token=" + token));
    }

    @Test
    @DisplayName("US4-1, NFR-003: an unknown address gets the same 202 and body, and no mail")
    void unknownAddressGetsTheSameAnswerAndNoMail() throws Exception {
        String registered = AuthTestSupport.uniqueEmail();
        support.memberWithPassword(registered, PASSWORD);
        String unknown = AuthTestSupport.uniqueEmail();

        MockHttpServletResponse known = forgot(registered);
        MockHttpServletResponse stranger = forgot(unknown);

        assertThat(stranger.getStatus()).isEqualTo(known.getStatus()).isEqualTo(202);
        assertThat(stranger.getContentAsString()).isEqualTo(known.getContentAsString());
        linkTokenOf(registered, 1);
        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(5)).until(
                () -> smtp.messagesTo(unknown).isEmpty());
    }

    @Test
    @DisplayName("FR-014, NFR-003: Google-only, suspended and banned accounts get 202, no token and no mail")
    void accountsThatMustNotGetALinkAreAnsweredTheSame() throws Exception {
        String googleOnly = AuthTestSupport.uniqueEmail();
        long googleId = support.userWithPassword(googleOnly, null, Role.MEMBER, AccountStatus.ACTIVE, true);
        String suspended = AuthTestSupport.uniqueEmail();
        long suspendedId = support.userWithPassword(suspended, PASSWORD, Role.MEMBER, AccountStatus.SUSPENDED, true);
        String banned = AuthTestSupport.uniqueEmail();
        long bannedId = support.userWithPassword(banned, PASSWORD, Role.MEMBER, AccountStatus.BANNED, true);

        for (String email : new String[] {googleOnly, suspended, banned}) {
            MockHttpServletResponse response = forgot(email);
            assertThat(response.getStatus()).isEqualTo(202);
            assertThat(response.getContentAsString()).isEmpty();
        }

        assertNothingSentTo(googleOnly, googleId);
        assertNothingSentTo(suspended, suspendedId);
        assertNothingSentTo(banned, bannedId);
    }

    @Test
    @DisplayName("FR-014: a malformed or blank email is 400 VALIDATION_ERROR, which reveals nothing about registration")
    void malformedEmailIsRefused() throws Exception {
        MockHttpServletResponse malformed = forgot("not-an-email");
        assertThat(malformed.getStatus()).isEqualTo(400);
        assertThat(body(malformed).get("code").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(body(malformed).get("errors").get(0).get("field").asText()).isEqualTo("email");

        assertThat(forgot("").getStatus()).isEqualTo(400);
        assertThat(post(FORGOT, Map.of()).getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("FR-017: the 4th request in 15 minutes is 429 EMAIL_RATE_LIMITED with the wait, registered or not")
    void fourthRequestIsRateLimitedIdenticallyForRegisteredAndUnknownAddresses() throws Exception {
        String registered = AuthTestSupport.uniqueEmail();
        support.memberWithPassword(registered, PASSWORD);
        String unknown = AuthTestSupport.uniqueEmail();

        for (int i = 0; i < 3; i++) {
            assertThat(forgot(registered).getStatus()).isEqualTo(202);
            assertThat(forgot(unknown).getStatus()).isEqualTo(202);
        }
        MockHttpServletResponse limitedRegistered = forgot(registered);
        MockHttpServletResponse limitedUnknown = forgot(unknown);

        assertThat(limitedRegistered.getStatus()).isEqualTo(429).isEqualTo(limitedUnknown.getStatus());
        assertThat(body(limitedRegistered).get("code").asText()).isEqualTo("EMAIL_RATE_LIMITED");
        assertThat(body(limitedUnknown).get("code").asText()).isEqualTo("EMAIL_RATE_LIMITED");
        assertThat(body(limitedRegistered).get("retryAfterSeconds").asLong()).isBetween(1L, 900L);
        assertThat(body(limitedRegistered).get("retryAfterSeconds").asLong())
                .isEqualTo(body(limitedUnknown).get("retryAfterSeconds").asLong());
        assertThat(limitedRegistered.getHeader("Retry-After")).isNotNull();
        assertThat(limitedRegistered.getContentAsString()).doesNotContain(registered);
        linkTokenOf(registered, 3);
        assertThat(smtp.messagesTo(registered)).as("the refused request sent nothing").hasSize(3);

        clock.advance(Duration.ofMinutes(15).plusSeconds(1));
        assertThat(forgot(registered).getStatus()).isEqualTo(202);
    }

    @Test
    @DisplayName("FR-017: the reset limit is counted apart from the verification one")
    void resetLimitIsNotUsedUpByTheVerificationEmail() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        assertThat(post("/api/v1/auth/register", Map.of("displayName", "An", "email", email, "password", PASSWORD,
                "confirmPassword", PASSWORD)).getStatus()).isEqualTo(201);

        for (int i = 0; i < 3; i++) {
            assertThat(forgot(email).getStatus()).isEqualTo(202);
        }
        assertThat(forgot(email).getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("US4-4, FR-015: the newest link works and the one before it is TOKEN_INVALID")
    void newestLinkReplacesTheOlderOne() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        support.memberWithPassword(email, PASSWORD);
        forgot(email);
        String first = linkTokenOf(email, 1);
        forgot(email);
        String second = linkTokenOf(email, 2);

        MockHttpServletResponse old = validate(first);
        assertThat(old.getStatus()).isEqualTo(410);
        assertThat(body(old).get("code").asText()).isEqualTo("TOKEN_INVALID");
        assertThat(reset(first, NEW_PASSWORD).getStatus()).isEqualTo(410);
        assertThat(reset(second, NEW_PASSWORD).getStatus()).isEqualTo(204);
    }

    @Test
    @DisplayName("Edge: Email delivery failure - the request is still 202, and asking again delivers a working link")
    void smtpFailureDoesNotFailTheRequestAndAskingAgainWorks() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        support.memberWithPassword(email, PASSWORD);
        smtp.failNext(3); // all three attempts of the first mail fail

        assertThat(forgot(email).getStatus()).isEqualTo(202);
        await().atMost(Duration.ofSeconds(15)).until(() -> smtp.attempts() >= 3);
        assertThat(smtp.messagesTo(email)).isEmpty();

        assertThat(forgot(email).getStatus()).isEqualTo(202);
        String token = linkTokenOf(email, 1);
        assertThat(reset(token, NEW_PASSWORD).getStatus()).isEqualTo(204);
    }

    // ---- validate --------------------------------------------------------------------------------------------

    @Test
    @DisplayName("AU06: validate shows the account email and the expiry 30 minutes after the request, and uses nothing up")
    void validateShowsTheEmailAndDoesNotConsumeTheLink() throws Exception {
        Instant asked = clock.instant();
        String[] account = accountWithLink();

        MockHttpServletResponse first = validate(account[1]);
        MockHttpServletResponse second = validate(account[1]);

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(200);
        assertThat(body(first).get("email").asText()).isEqualTo(account[0]);
        assertThat(Instant.parse(body(first).get("expiresAt").asText())).isEqualTo(asked.plus(Duration.ofMinutes(30)));
        assertThat(count("SELECT count(*) FROM one_time_tokens WHERE user_id = ? AND used_at IS NULL",
                idOf(account[0]))).isEqualTo(1);
        assertThat(reset(account[1], NEW_PASSWORD).getStatus()).isEqualTo(204);
    }

    @Test
    @DisplayName("TOKEN_INVALID: an unknown token is 410, a blank or overlong one is 400")
    void unknownBlankAndOverlongTokens() throws Exception {
        MockHttpServletResponse unknown = validate("never-issued");
        assertThat(unknown.getStatus()).isEqualTo(410);
        assertThat(body(unknown).get("code").asText()).isEqualTo("TOKEN_INVALID");

        assertThat(validate("").getStatus()).isEqualTo(400);
        assertThat(validate("x".repeat(129)).getStatus()).isEqualTo(400);
        assertThat(reset("never-issued", NEW_PASSWORD).getStatus()).isEqualTo(410);
    }

    @Test
    @DisplayName("US4-3, FR-015: the link works at 29:59 and is 410 TOKEN_EXPIRED after 30 minutes")
    void linkLivesThirtyMinutes() throws Exception {
        String[] fresh = accountWithLink();
        clock.advance(Duration.ofMinutes(30).minusSeconds(1));
        assertThat(validate(fresh[1]).getStatus()).isEqualTo(200);

        clock.advance(Duration.ofSeconds(2));
        MockHttpServletResponse validate = validate(fresh[1]);
        MockHttpServletResponse reset = reset(fresh[1], NEW_PASSWORD);

        assertThat(validate.getStatus()).isEqualTo(410);
        assertThat(body(validate).get("code").asText()).isEqualTo("TOKEN_EXPIRED");
        assertThat(reset.getStatus()).isEqualTo(410);
        assertThat(body(reset).get("code").asText()).isEqualTo("TOKEN_EXPIRED");
        assertThat(login(fresh[0], PASSWORD).getStatus()).as("the old password still works").isEqualTo(200);
    }

    // ---- reset -----------------------------------------------------------------------------------------------

    @Test
    @DisplayName("US4-2, FR-015, FR-016: reset changes the password, uses the link, ends every session, writes the audit")
    void resetDoesEverythingTogether() throws Exception {
        String[] account = accountWithLink();
        long id = idOf(account[0]);
        long sessionA = support.session(id);
        long sessionB = support.session(id);
        assertThat(count("SELECT count(*) FROM user_sessions WHERE user_id = ? AND revoked_at IS NULL", id))
                .isEqualTo(2);

        MockHttpServletResponse response = reset(account[1], NEW_PASSWORD);

        assertThat(response.getStatus()).isEqualTo(204);
        assertThat(response.getContentAsString()).isEmpty();
        assertThat(response.getHeader("Set-Cookie")).as("resetting does not sign in").isNull();
        assertThat(count("SELECT count(*) FROM user_sessions WHERE user_id = ? AND revoked_at IS NULL", id)).isZero();
        assertThat(count("SELECT count(*) FROM user_sessions WHERE id IN (?, ?) AND revoked_at IS NOT NULL", sessionA,
                sessionB)).isEqualTo(2);
        assertThat(count("SELECT count(*) FROM one_time_tokens WHERE user_id = ? AND used_at IS NULL", id)).isZero();
        assertThat(count("SELECT count(*) FROM audit_logs WHERE action = 'PASSWORD_RESET' AND target_id = ?", id))
                .isEqualTo(1);
        assertThat(login(account[0], PASSWORD).getStatus()).as("the old password is gone").isEqualTo(401);
        assertThat(login(account[0], NEW_PASSWORD).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("US4-3: a link that was already used is 410 TOKEN_INVALID and changes nothing")
    void usedLinkIsRefused() throws Exception {
        String[] account = accountWithLink();
        assertThat(reset(account[1], NEW_PASSWORD).getStatus()).isEqualTo(204);

        MockHttpServletResponse again = reset(account[1], "another-pass1");

        assertThat(again.getStatus()).isEqualTo(410);
        assertThat(body(again).get("code").asText()).isEqualTo("TOKEN_INVALID");
        assertThat(login(account[0], NEW_PASSWORD).getStatus()).isEqualTo(200);
        assertThat(count("SELECT count(*) FROM audit_logs WHERE action = 'PASSWORD_RESET' AND target_id = ?",
                idOf(account[0]))).isEqualTo(1);
    }

    @Test
    @DisplayName("AF 3.5, FR-004: a weak password is 400 VALIDATION_ERROR with errors[{field, code}] and the link survives")
    void weakPasswordIsRefusedAndTheLinkIsNotUsedUp() throws Exception {
        String[] account = accountWithLink();

        MockHttpServletResponse response = reset(account[1], "abc");

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(body(response).get("code").asText()).isEqualTo("VALIDATION_ERROR");
        JsonNode errors = body(response).get("errors");
        assertThat(errors).isNotEmpty().allSatisfy(error -> assertThat(error.get("field").asText())
                .isEqualTo("newPassword"));
        assertThat(errors.findValuesAsText("code")).contains("PASSWORD_TOO_SHORT", "PASSWORD_NO_DIGIT");
        assertThat(count("SELECT count(*) FROM one_time_tokens WHERE user_id = ? AND used_at IS NULL",
                idOf(account[0]))).isEqualTo(1);
        assertThat(reset(account[1], NEW_PASSWORD).getStatus()).as("the same link still works").isEqualTo(204);
    }

    @Test
    @DisplayName("FR-004: a confirmation that differs is PASSWORD_MISMATCH on confirmPassword and the link survives")
    void mismatchedConfirmationIsRefused() throws Exception {
        String[] account = accountWithLink();

        MockHttpServletResponse response = post(RESET,
                Map.of("token", account[1], "newPassword", NEW_PASSWORD, "confirmPassword", "different-pass1"));

        assertThat(response.getStatus()).isEqualTo(400);
        JsonNode error = body(response).get("errors").get(0);
        assertThat(error.get("field").asText()).isEqualTo("confirmPassword");
        assertThat(error.get("code").asText()).isEqualTo("PASSWORD_MISMATCH");
        assertThat(reset(account[1], NEW_PASSWORD).getStatus()).isEqualTo(204);
    }

    @Test
    @DisplayName("FR-004: a password equal to the email is 400 PASSWORD_EQUALS_EMAIL on newPassword and the link survives")
    void passwordEqualToTheEmailIsRefused() throws Exception {
        String[] account = accountWithLink();

        MockHttpServletResponse response = reset(account[1], account[0].toUpperCase());

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(body(response).get("code").asText()).isEqualTo("VALIDATION_ERROR");
        JsonNode error = body(response).get("errors").get(0);
        assertThat(error.get("field").asText()).isEqualTo("newPassword");
        assertThat(error.get("code").asText()).isEqualTo("PASSWORD_EQUALS_EMAIL");
        assertThat(count("SELECT count(*) FROM one_time_tokens WHERE user_id = ? AND used_at IS NULL",
                idOf(account[0]))).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM audit_logs WHERE action = 'PASSWORD_RESET' AND target_id = ?",
                idOf(account[0]))).isZero();
        assertThat(reset(account[1], NEW_PASSWORD).getStatus()).isEqualTo(204);
    }

    @Test
    @DisplayName("R6: resetting does not mark an unverified email as verified")
    void resetDoesNotVerifyTheEmail() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        long id = support.userWithPassword(email, PASSWORD, Role.MEMBER, AccountStatus.ACTIVE, false);
        forgot(email);
        String token = linkTokenOf(email, 1);

        assertThat(reset(token, NEW_PASSWORD).getStatus()).isEqualTo(204);

        assertThat(jdbc.queryForObject("SELECT email_verified_at IS NULL FROM users WHERE id = ?", Boolean.class, id))
                .isTrue();
    }

    @Test
    @DisplayName("FR-012: resetting does not lift the lock, and a locked account can still ask for and use a link")
    void resetDoesNotUnlockTheAccount() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        long id = support.memberWithPassword(email, PASSWORD);
        for (int i = 0; i < 5; i++) {
            login(email, "wrong-password1");
        }
        assertThat(login(email, PASSWORD).getStatus()).as("locked").isEqualTo(423);
        Timestamp lockedUntil = jdbc.queryForObject("SELECT locked_until FROM users WHERE id = ?", Timestamp.class, id);
        assertThat(lockedUntil).isNotNull();

        forgot(email);
        String token = linkTokenOf(email, 1);
        assertThat(reset(token, NEW_PASSWORD).getStatus()).isEqualTo(204);

        assertThat(jdbc.queryForObject("SELECT locked_until FROM users WHERE id = ?", Timestamp.class, id))
                .isEqualTo(lockedUntil);
        MockHttpServletResponse stillLocked = login(email, NEW_PASSWORD);
        assertThat(stillLocked.getStatus()).isEqualTo(423);
        assertThat(body(stillLocked).get("code").asText()).isEqualTo("ACCOUNT_LOCKED");
        clock.advance(Duration.ofMinutes(16));
        assertThat(login(email, NEW_PASSWORD).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("FR-014: a link issued before the account was banned no longer works")
    void linkOfAnAccountBannedLaterIsRefused() throws Exception {
        String[] account = accountWithLink();
        jdbc.update("UPDATE users SET status = 'BANNED' WHERE email = ?", account[0]);

        assertThat(validate(account[1]).getStatus()).isEqualTo(410);
        MockHttpServletResponse response = reset(account[1], NEW_PASSWORD);

        assertThat(response.getStatus()).isEqualTo(410);
        assertThat(body(response).get("code").asText()).isEqualTo("TOKEN_INVALID");
    }

    @Test
    @DisplayName("NFR-001: the password and the token never appear in a response body")
    void secretsAreNotEchoed() throws Exception {
        String[] account = accountWithLink();

        MockHttpServletResponse weak = reset(account[1], "abc");
        MockHttpServletResponse validate = validate(account[1]);

        assertThat(weak.getContentAsString()).doesNotContain(account[1]).doesNotContain("\"abc\"");
        assertThat(validate.getContentAsString()).doesNotContain(account[1]);
    }
}
