package com.wikicook.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.auth.api.BlockedEmails;
import com.wikicook.auth.domain.TokenPurpose;
import com.wikicook.auth.service.EmailHasher;
import com.wikicook.auth.service.EmailRequestLimiter;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.RecordingMailSender;
import com.wikicook.support.RecordingMailSender.Recorder;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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

/**
 * User story 2, registration over HTTP (FR-001 - FR-005, FR-017, FR-026): the account, its Member role, the link
 * email, the conflict and validation answers, a double click, and the email limit that never refuses a registration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({AuthTestSupport.class, RecordingMailSender.class})
class RegistrationIT extends AbstractPostgresIT {

    private static final String PASSWORD = "abc12345";
    static final Pattern LINK = Pattern.compile("verify-email#token=([A-Za-z0-9_-]+)");

    @Autowired
    MockMvc mvc;

    @Autowired
    AuthTestSupport support;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    @Autowired
    BlockedEmails blockedEmails;

    @Autowired
    EmailRequestLimiter limiter;

    @Autowired
    EmailHasher hasher;

    private final Recorder smtp = RecordingMailSender.recorder();

    @BeforeEach
    void setUp() {
        smtp.clear();
        support.resetThrottle();
    }

    private MockHttpServletResponse register(String name, String email, String password, String confirm)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("displayName", name);
        body.put("email", email);
        body.put("password", password);
        body.put("confirmPassword", confirm);
        return mvc.perform(post("/api/v1/auth/register").contentType("application/json")
                .content(json.writeValueAsString(body))).andReturn().getResponse();
    }

    private MockHttpServletResponse register(String email) throws Exception {
        return register("An Nguyen", email, PASSWORD, PASSWORD);
    }

    private JsonNode body(MockHttpServletResponse response) throws Exception {
        return json.readTree(response.getContentAsString());
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private static String tokenOf(jakarta.mail.internet.MimeMessage message) {
        Matcher matcher = LINK.matcher(Recorder.parts(message).get(0));
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private List<String> codesOf(JsonNode body, String field) {
        List<String> codes = new ArrayList<>();
        for (JsonNode error : body.get("errors")) {
            if (field.equals(error.get("field").asText())) {
                codes.add(error.get("code").asText());
            }
        }
        return codes;
    }

    private String fieldCode(JsonNode body, String field) {
        for (JsonNode error : body.get("errors")) {
            if (field.equals(error.get("field").asText())) {
                return error.get("code").asText();
            }
        }
        return null;
    }

    @Test
    @DisplayName("US2-1, FR-001, FR-003: a Member is created unverified, with a hashed password, no session and a link email")
    void registrationCreatesAnUnverifiedMemberAndMailsTheLink() throws Exception {
        String email = AuthTestSupport.uniqueEmail();

        MockHttpServletResponse response = register(email);

        assertThat(response.getStatus()).isEqualTo(201);
        JsonNode body = body(response);
        assertThat(body.get("email").asText()).isEqualTo(email);
        assertThat(body.get("verificationEmailSent").asBoolean()).isTrue();
        assertThat(Instant.parse(body.get("verificationExpiresAt").asText()))
                .isBetween(Instant.now().plus(Duration.ofHours(23)), Instant.now().plus(Duration.ofHours(25)));
        assertThat(body.has("accessToken")).isFalse();
        assertThat(response.getHeader("Set-Cookie")).as("registering does not sign in").isNull();
        Map<String, Object> user = jdbc.queryForMap("SELECT id, role, status, email_verified_at, password_hash, "
                + "display_name FROM users WHERE email = ?", email);
        assertThat(user.get("role")).isEqualTo("MEMBER");
        assertThat(user.get("status")).isEqualTo("ACTIVE");
        assertThat(user.get("email_verified_at")).isNull();
        assertThat(user.get("display_name")).isEqualTo("An Nguyen");
        assertThat((String) user.get("password_hash")).startsWith("$2a$").doesNotContain(PASSWORD);
        assertThat(count("SELECT count(*) FROM user_sessions WHERE user_id = ?", user.get("id"))).isZero();
        assertThat(count("SELECT count(*) FROM one_time_tokens WHERE user_id = ? AND purpose = 'EMAIL_VERIFICATION'",
                user.get("id"))).isEqualTo(1);
        await().atMost(Duration.ofSeconds(10)).until(() -> smtp.messagesTo(email).size() == 1);
        assertThat(tokenOf(smtp.messagesTo(email).get(0))).isNotBlank();
    }

    @Test
    @DisplayName("US2-5: an unverified account can sign in and is told it is unverified")
    void anUnverifiedAccountCanSignIn() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        register(email);

        MockHttpServletResponse login = mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD)))).andReturn()
                .getResponse();

        assertThat(login.getStatus()).isEqualTo(200);
        assertThat(body(login).get("account").get("emailVerified").asBoolean()).isFalse();
        assertThat(body(login).get("account").get("role").asText()).isEqualTo("MEMBER");
    }

    @Test
    @DisplayName("US2-2, FR-002: an email that is already registered is 409 EMAIL_TAKEN and no second email goes out")
    void duplicateEmailIsRefused() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        register(email);
        await().atMost(Duration.ofSeconds(10)).until(() -> smtp.messagesTo(email).size() == 1);

        MockHttpServletResponse second = register("Someone Else", email, "other-pass1", "other-pass1");

        assertThat(second.getStatus()).isEqualTo(409);
        assertThat(body(second).get("code").asText()).isEqualTo("EMAIL_TAKEN");
        assertThat(count("SELECT count(*) FROM users WHERE email = ?", email)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT display_name FROM users WHERE email = ?", String.class, email))
                .isEqualTo("An Nguyen");
    }

    @Test
    @DisplayName("US2-3, FR-004: a password that breaks the rules shows which rule on the password field, no account")
    void weakPasswordIsRefusedWithTheRuleCodes() throws Exception {
        String email = AuthTestSupport.uniqueEmail();

        MockHttpServletResponse response = register("An", email, "abc", "abc");

        assertThat(response.getStatus()).isEqualTo(400);
        JsonNode body = body(response);
        assertThat(body.get("code").asText()).isEqualTo("VALIDATION_ERROR");
        List<String> codes = new ArrayList<>();
        body.get("errors").forEach(e -> codes.add(e.get("field").asText() + ":" + e.get("code").asText()));
        assertThat(codes).contains("password:PASSWORD_TOO_SHORT", "password:PASSWORD_NO_DIGIT");
        assertThat(count("SELECT count(*) FROM users WHERE email = ?", email)).isZero();
    }

    @Test
    @DisplayName("FR-004: password equal to the email and a confirmation that differs are refused")
    void passwordEqualToEmailAndMismatchAreRefused() throws Exception {
        String email = "same." + System.nanoTime() + "@mail.com";

        MockHttpServletResponse equal = register("An", email, email, email);
        MockHttpServletResponse mismatch = register("An", email, PASSWORD, PASSWORD + "x");

        assertThat(fieldCode(body(equal), "password")).isEqualTo("PASSWORD_EQUALS_EMAIL");
        assertThat(fieldCode(body(mismatch), "confirmPassword")).isEqualTo("PASSWORD_MISMATCH");
        assertThat(count("SELECT count(*) FROM users WHERE email = ?", email)).isZero();
    }

    @Test
    @DisplayName("Edge: Double-click on submit - two simultaneous requests make one account and send one email")
    void doubleClickMakesOneAccountAndOneEmail() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        List<Integer> statuses = new ArrayList<>();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> register(email).getStatus()));
            }
            for (Future<Integer> future : futures) {
                statuses.add(future.get(30, TimeUnit.SECONDS));
            }
        }

        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(count("SELECT count(*) FROM users WHERE email = ?", email)).isEqualTo(1);
        await().atMost(Duration.ofSeconds(10)).until(() -> smtp.messagesTo(email).size() == 1);
        Thread.sleep(700);
        assertThat(smtp.messagesTo(email)).hasSize(1);
        assertThat(count("SELECT count(*) FROM one_time_tokens t JOIN users u ON u.id = t.user_id WHERE u.email = ?",
                email)).isEqualTo(1);
    }

    @Test
    @DisplayName("FR-017, OQ-010: above the email limit the account is still created, without a token and without an email")
    void aboveTheLimitTheAccountIsCreatedWithoutAnEmail() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        for (int i = 0; i < 3; i++) { // three requests for this address before it ever registered
            limiter.tryAcquire(hasher.hash(email), TokenPurpose.EMAIL_VERIFICATION);
        }

        MockHttpServletResponse response = register(email);

        assertThat(response.getStatus()).isEqualTo(201);
        JsonNode body = body(response);
        assertThat(body.get("verificationEmailSent").asBoolean()).isFalse();
        assertThat(body.get("verificationExpiresAt").isNull()).isTrue();
        assertThat(body.get("retryAfterSeconds").asLong()).isBetween(1L, 900L);
        long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        assertThat(count("SELECT count(*) FROM one_time_tokens WHERE user_id = ?", id)).as("no token").isZero();
        Thread.sleep(500);
        assertThat(smtp.messagesTo(email)).as("no email").isEmpty();
    }

    @Test
    @DisplayName("FR-017: the registration email itself counts, so only two more can be requested")
    void theRegistrationEmailCountsTowardsTheLimit() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        register(email);

        assertThat(limiter.tryAcquire(hasher.hash(email), TokenPurpose.EMAIL_VERIFICATION).allowed()).isTrue();
        assertThat(limiter.tryAcquire(hasher.hash(email), TokenPurpose.EMAIL_VERIFICATION).allowed()).isTrue();
        assertThat(limiter.tryAcquire(hasher.hash(email), TokenPurpose.EMAIL_VERIFICATION).allowed()).isFalse();
    }

    @Test
    @DisplayName("Edge: Very long input - email over 254, password over 128 and name over 50 are 400 with their own codes")
    void tooLongInputIsRefused() throws Exception {
        MockHttpServletResponse response = register("n".repeat(51), "a".repeat(250) + "@mail.com",
                "a1" + "b".repeat(127), "a1" + "b".repeat(127));

        assertThat(response.getStatus()).isEqualTo(400);
        JsonNode body = body(response);
        assertThat(fieldCode(body, "displayName")).isEqualTo("TOO_LONG");
        assertThat(codesOf(body, "email")).contains("TOO_LONG"); // a 250 character local part is also not a valid address
        assertThat(fieldCode(body, "password")).isEqualTo("PASSWORD_TOO_LONG");
        assertThat(count("SELECT count(*) FROM users WHERE display_name = ?", "n".repeat(51))).isZero();
    }

    @Test
    @DisplayName("FR-001: the display name is trimmed to 1-50 characters, a blank one is required")
    void displayNameIsTrimmed() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        assertThat(register("  Bé An  ", email, PASSWORD, PASSWORD).getStatus()).isEqualTo(201);
        assertThat(jdbc.queryForObject("SELECT display_name FROM users WHERE email = ?", String.class, email))
                .isEqualTo("Bé An");

        String fifty = AuthTestSupport.uniqueEmail();
        assertThat(register("  " + "n".repeat(50) + "  ", fifty, PASSWORD, PASSWORD).getStatus()).isEqualTo(201);

        MockHttpServletResponse blank = register("   ", AuthTestSupport.uniqueEmail(), PASSWORD, PASSWORD);
        assertThat(blank.getStatus()).isEqualTo(400);
        assertThat(fieldCode(body(blank), "displayName")).isEqualTo("REQUIRED");
    }

    @Test
    @DisplayName("Edge: Email with leading/trailing spaces or mixed case - stored normalized, and the same email conflicts")
    void emailIsNormalized() throws Exception {
        String local = "Mixed." + System.nanoTime();

        MockHttpServletResponse first = register("An", "  " + local + "@Mail.COM ", PASSWORD, PASSWORD);
        MockHttpServletResponse second = register("An", local.toLowerCase() + "@mail.com", PASSWORD, PASSWORD);

        assertThat(first.getStatus()).isEqualTo(201);
        assertThat(body(first).get("email").asText()).isEqualTo(local.toLowerCase() + "@mail.com");
        assertThat(count("SELECT count(*) FROM users WHERE email = ?", local.toLowerCase() + "@mail.com"))
                .isEqualTo(1);
        assertThat(second.getStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("Edge: Required fields - an invalid email format is INVALID_EMAIL, missing fields are REQUIRED")
    void emailFormatAndRequiredFields() throws Exception {
        MockHttpServletResponse badEmail = register("An", "not-an-email", PASSWORD, PASSWORD);
        MockHttpServletResponse empty = mvc.perform(post("/api/v1/auth/register").contentType("application/json")
                .content("{}")).andReturn().getResponse();

        assertThat(fieldCode(body(badEmail), "email")).isEqualTo("INVALID_EMAIL");
        assertThat(empty.getStatus()).isEqualTo(400);
        assertThat(fieldCode(body(empty), "displayName")).isEqualTo("REQUIRED");
        assertThat(fieldCode(body(empty), "email")).isEqualTo("REQUIRED");
        assertThat(fieldCode(body(empty), "password")).isEqualTo("REQUIRED");
    }

    @Test
    @DisplayName("FR-026, OQ-010-05: an email on the Blocked Email list is 422 REGISTRATION_REFUSED with no reason")
    void blockedEmailIsRefusedNeutrally() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        blockedEmails.block(email);

        MockHttpServletResponse response = register(email);

        assertThat(response.getStatus()).isEqualTo(422);
        JsonNode body = body(response);
        assertThat(body.get("code").asText()).isEqualTo("REGISTRATION_REFUSED");
        assertThat(response.getContentAsString().toLowerCase()).doesNotContain("block").doesNotContain("ban")
                .doesNotContain(email);
        assertThat(count("SELECT count(*) FROM users WHERE email = ?", email)).isZero();
        Thread.sleep(300);
        assertThat(smtp.messagesTo(email)).isEmpty();
    }

    @Test
    @DisplayName("R3: a 128 code point password (254 UTF-16 units) registers and signs in")
    void longestPasswordRegistersAndSignsIn() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        String password = "😀".repeat(126) + "a1";

        assertThat(register("An", email, password, password).getStatus()).isEqualTo(201);

        MockHttpServletResponse login = mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", email, "password", password)))).andReturn()
                .getResponse();
        assertThat(login.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("FR-005: the account is stamped with its creation time, from which the 7 day deletion is counted")
    void createdAtIsRecorded() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        register(email);

        Timestamp createdAt = jdbc.queryForObject("SELECT created_at FROM users WHERE email = ?", Timestamp.class,
                email);

        assertThat(createdAt.toInstant()).isBetween(Instant.now().minusSeconds(30), Instant.now().plusSeconds(30));
    }
}
