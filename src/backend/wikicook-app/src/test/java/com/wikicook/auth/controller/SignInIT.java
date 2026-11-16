package com.wikicook.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

/**
 * User story 1 over HTTP with the real database: the happy path and its cookie, the identical failure for a wrong
 * password and an unknown email, the input edge cases, suspended and banned accounts, and the audit trail.
 * {@code cookie-secure=true} is the production default, so the {@code Secure} attribute is checked here.
 */
@SpringBootTest(properties = "wikicook.auth.cookie-secure=true")
@AutoConfigureMockMvc
@Import(AuthTestSupport.class)
class SignInIT extends AbstractPostgresIT {

    private static final String PASSWORD = "abc12345";

    @Autowired
    MockMvc mvc;

    @Autowired
    AuthTestSupport support;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    @BeforeEach
    void setUp() {
        support.resetThrottle();
    }

    private MockHttpServletResponse login(String email, String password, boolean rememberMe) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("rememberMe", rememberMe);
        return mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content(json.writeValueAsString(body))).andReturn().getResponse();
    }

    private MockHttpServletResponse login(String email, String password) throws Exception {
        return login(email, password, false);
    }

    private JsonNode body(MockHttpServletResponse response) throws Exception {
        return json.readTree(response.getContentAsString());
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    @Test
    @DisplayName("US1-1: correct credentials give an access token, the account and the refresh cookie")
    void correctCredentialsSignIn() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        long id = support.memberWithPassword(email, PASSWORD);

        MockHttpServletResponse response = login(email, PASSWORD);

        assertThat(response.getStatus()).isEqualTo(200);
        JsonNode body = body(response);
        assertThat(body.get("accessToken").asText()).isNotBlank();
        assertThat(body.get("expiresIn").asInt()).isEqualTo(900);
        assertThat(body.get("account").get("id").asLong()).isEqualTo(id);
        assertThat(body.get("account").get("email").asText()).isEqualTo(email);
        assertThat(body.get("account").get("role").asText()).isEqualTo("MEMBER");
        assertThat(body.get("account").get("emailVerified").asBoolean()).isTrue();
        assertThat(body.get("account").has("passwordHash")).isFalse();
        String cookie = response.getHeader("Set-Cookie");
        assertThat(cookie).startsWith("wc_rt=").contains("HttpOnly").contains("Secure").contains("SameSite=Strict")
                .contains("Path=/api/v1/auth").doesNotContain("Max-Age");
        // the token really opens the API
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + body.get("accessToken").asText()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("US1-2, US1-3, FR-008: wrong password and unknown email give the same status, code and body")
    void wrongPasswordAndUnknownEmailLookIdentical() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        support.memberWithPassword(email, PASSWORD);

        MockHttpServletResponse wrongPassword = login(email, "wrong-password1");
        MockHttpServletResponse unknown = login(AuthTestSupport.uniqueEmail(), "wrong-password1");

        assertThat(wrongPassword.getStatus()).isEqualTo(401).isEqualTo(unknown.getStatus());
        assertThat(body(wrongPassword).get("code").asText()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(wrongPassword.getContentAsString()).isEqualTo(unknown.getContentAsString());
        assertThat(wrongPassword.getHeader("Set-Cookie")).isNull();
        assertThat(wrongPassword.getContentAsString()).doesNotContain(email);
    }

    @Test
    @DisplayName("US1-7, Edge: Email with leading/trailing spaces or mixed case - matches the registered email")
    void emailIsTrimmedAndCaseInsensitive() throws Exception {
        String email = "an." + System.nanoTime() + "@mail.com";
        support.memberWithPassword(email, PASSWORD);

        assertThat(login(" " + email.toUpperCase() + " ", PASSWORD).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Edge: Password with leading/trailing spaces - the password is compared exactly as typed")
    void passwordIsNotTrimmed() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        support.memberWithPassword(email, PASSWORD);
        String spaced = AuthTestSupport.uniqueEmail();
        support.memberWithPassword(spaced, " " + PASSWORD + " ");

        assertThat(login(email, " " + PASSWORD + " ").getStatus()).isEqualTo(401);
        assertThat(login(spaced, PASSWORD).getStatus()).isEqualTo(401);
        assertThat(login(spaced, " " + PASSWORD + " ").getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Edge: Very long input - an email over 254 or a password over 128 code points is 400 and is not an attempt")
    void tooLongInputIsA400AndNotCounted() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        long id = support.memberWithPassword(email, PASSWORD);

        MockHttpServletResponse longEmail = login("a".repeat(250) + "@mail.com", PASSWORD);
        MockHttpServletResponse longPassword = login(email, "a1".repeat(65)); // 130 code points
        MockHttpServletResponse emoji129 = login(email, "😀".repeat(129));

        assertThat(longEmail.getStatus()).isEqualTo(400);
        assertThat(body(longEmail).get("code").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(body(longEmail).get("errors").get(0).get("field").asText()).isEqualTo("email");
        assertThat(body(longEmail).get("errors").get(0).get("code").asText()).isEqualTo("TOO_LONG");
        assertThat(longPassword.getStatus()).isEqualTo(400);
        assertThat(body(longPassword).get("errors").get(0).get("field").asText()).isEqualTo("password");
        assertThat(body(longPassword).get("errors").get(0).get("code").asText()).isEqualTo("TOO_LONG");
        assertThat(emoji129.getStatus()).isEqualTo(400);
        assertThat(count("SELECT count(*) FROM login_attempts WHERE user_id = ?", id)).isZero();
    }

    @Test
    @DisplayName("Edge: Very long input - a 128 emoji password (512 bytes) registered earlier still signs in (R3)")
    void passwordOf128EmojiSignsIn() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        String password = "😀".repeat(128);
        support.memberWithPassword(email, password);

        assertThat(login(email, password).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Edge: Required fields - a missing email or password is 400 VALIDATION_ERROR")
    void missingFieldsAre400() throws Exception {
        MockHttpServletResponse response = mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content("{\"email\":\"\"}")).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(400);
        List<String> fields = body(response).get("errors").findValuesAsText("field");
        assertThat(fields).contains("email", "password");
    }

    @Test
    @DisplayName("US2-5: an account with an unverified email can sign in (it just cannot publish)")
    void unverifiedAccountSignsIn() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        support.userWithPassword(email, PASSWORD, Role.MEMBER, AccountStatus.ACTIVE, false);

        MockHttpServletResponse response = login(email, PASSWORD);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(body(response).get("account").get("emailVerified").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("Edge: Suspended account - the right password is 403 ACCOUNT_SUSPENDED with reason and endsAt")
    void suspendedAccountWithRightPassword() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        long id = support.userWithPassword(email, PASSWORD, Role.MEMBER, AccountStatus.SUSPENDED, true);
        Instant endsAt = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        support.sanction(id, "Spam in reviews", endsAt);

        MockHttpServletResponse response = login(email, PASSWORD);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body(response).get("code").asText()).isEqualTo("ACCOUNT_SUSPENDED");
        assertThat(body(response).get("reason").asText()).isEqualTo("Spam in reviews");
        assertThat(body(response).get("endsAt").asText()).isEqualTo(endsAt.toString());
        assertThat(response.getHeader("Set-Cookie")).isNull();
        assertThat(count("SELECT count(*) FROM user_sessions WHERE user_id = ?", id)).isZero();
    }

    @Test
    @DisplayName("Edge: Banned account - the right password is 403 ACCOUNT_BANNED with a reason and no endsAt")
    void bannedAccountWithRightPassword() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        long id = support.userWithPassword(email, PASSWORD, Role.MEMBER, AccountStatus.BANNED, true);
        support.sanction(id, "Fraud", null);

        MockHttpServletResponse response = login(email, PASSWORD);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body(response).get("code").asText()).isEqualTo("ACCOUNT_BANNED");
        assertThat(body(response).get("reason").asText()).isEqualTo("Fraud");
        assertThat(body(response).has("endsAt")).isFalse();
    }

    @Test
    @DisplayName("Edge: Suspended / banned account - the wrong password is 401, the status is not revealed (FR-013)")
    void restrictedAccountWithWrongPasswordIsAGenericFailure() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        long id = support.userWithPassword(email, PASSWORD, Role.MEMBER, AccountStatus.BANNED, true);
        support.sanction(id, "Fraud", null);

        MockHttpServletResponse response = login(email, "wrong-password1");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body(response).get("code").asText()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(response.getContentAsString()).doesNotContain("Fraud").doesNotContain("BANNED");
    }

    @Test
    @DisplayName("FR-025: SIGN_IN_SUCCEEDED and SIGN_IN_FAILED are audited, last_sign_in_at is written")
    void signInIsAuditedAndLastSignInIsWritten() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        long id = support.memberWithPassword(email, PASSWORD);

        login(email, "wrong-password1");
        login(email, PASSWORD);

        assertThat(count("SELECT count(*) FROM audit_logs WHERE action = 'SIGN_IN_FAILED' AND target_id = ?", id))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT details->>'reason' FROM audit_logs WHERE action = 'SIGN_IN_FAILED' "
                + "AND target_id = ?", String.class, id)).isEqualTo("BAD_CREDENTIALS");
        assertThat(count("SELECT count(*) FROM audit_logs WHERE action = 'SIGN_IN_SUCCEEDED' AND target_id = ?", id))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT actor_id FROM audit_logs WHERE action = 'SIGN_IN_SUCCEEDED' "
                + "AND target_id = ?", Long.class, id)).isEqualTo(id);
        assertThat(jdbc.queryForObject("SELECT last_sign_in_at FROM users WHERE id = ?", Timestamp.class, id))
                .isNotNull();
    }

    @Test
    @DisplayName("FR-025, NFR-003: an unknown email is audited without target and with its hash, never readable")
    void unknownEmailIsAuditedByHash() throws Exception {
        String stranger = "stranger." + System.nanoTime() + "@mail.com";

        login(stranger, "wrong-password1");

        List<Map<String, Object>> rows = jdbc.queryForList("SELECT target_id, details::text AS details FROM audit_logs "
                + "WHERE action = 'SIGN_IN_FAILED' AND details->>'emailHash' IS NOT NULL ORDER BY id DESC LIMIT 1");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("target_id")).isNull();
        assertThat(rows.get(0).get("details").toString()).contains("BAD_CREDENTIALS").doesNotContain(stranger);
        assertThat(count("SELECT count(*) FROM audit_logs WHERE details::text LIKE ?", "%" + stranger + "%")).isZero();
    }

    @Test
    @DisplayName("FR-012: the 5th wrong password writes locked_until, an ACCOUNT_LOCKED audit and an outbox row")
    void lockWritesLockedUntilAuditAndOutboxForARegisteredAccount() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        long id = support.memberWithPassword(email, PASSWORD);

        for (int i = 0; i < 5; i++) {
            login(email, "wrong-password1");
        }

        assertThat(jdbc.queryForObject("SELECT locked_until FROM users WHERE id = ?", Timestamp.class, id))
                .isNotNull();
        assertThat(count("SELECT count(*) FROM audit_logs WHERE action = 'ACCOUNT_LOCKED' AND target_id = ?", id))
                .isEqualTo(1);
        assertThat(count("SELECT count(*) FROM email_outbox WHERE user_id = ? AND template = 'ACCOUNT_LOCKED'", id))
                .isEqualTo(1);
        // a sixth try while locked adds neither a second audit nor a second email
        login(email, "wrong-password1");
        assertThat(count("SELECT count(*) FROM email_outbox WHERE user_id = ? AND template = 'ACCOUNT_LOCKED'", id))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("NFR-003: locking an unknown email sends no email and writes no outbox row")
    void lockOfAnUnknownEmailSendsNothing() throws Exception {
        String stranger = AuthTestSupport.uniqueEmail();
        long before = count("SELECT count(*) FROM email_outbox");

        for (int i = 0; i < 5; i++) {
            login(stranger, "wrong-password1");
        }

        assertThat(count("SELECT count(*) FROM email_outbox")).isEqualTo(before);
    }
}
