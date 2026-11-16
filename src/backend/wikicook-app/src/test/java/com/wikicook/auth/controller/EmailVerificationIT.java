package com.wikicook.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.MutableClock;
import com.wikicook.support.RecordingMailSender;
import com.wikicook.support.RecordingMailSender.Recorder;
import jakarta.mail.internet.MimeMessage;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
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
 * Email verification over HTTP (FR-005, FR-017; research R6, R8): the 24 hour single-use link, "already verified",
 * expired and unknown links, resending (which replaces the older link and never extends the 7 days), the limit, and an
 * SMTP failure that the user fixes by asking again.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({AuthTestSupport.class, RecordingMailSender.class, MutableClock.class})
class EmailVerificationIT extends AbstractPostgresIT {

    private static final String PASSWORD = "abc12345";

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
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                .contentType("application/json").content(json.writeValueAsString(body))).andReturn().getResponse();
    }

    private String registerAndGetToken(String email) throws Exception {
        MockHttpServletResponse response = post("/api/v1/auth/register", Map.of("displayName", "An", "email", email,
                "password", PASSWORD, "confirmPassword", PASSWORD));
        assertThat(response.getStatus()).isEqualTo(201);
        return nextToken(email, 1);
    }

    /** The token of the {@code n}-th mail to this address, waiting for the asynchronous send. */
    private String nextToken(String email, int n) {
        await().atMost(Duration.ofSeconds(10)).until(() -> smtp.messagesTo(email).size() >= n);
        MimeMessage message = smtp.messagesTo(email).get(n - 1);
        Matcher matcher = RegistrationIT.LINK.matcher(Recorder.parts(message).get(0));
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private MockHttpServletResponse confirm(String token) throws Exception {
        return post("/api/v1/auth/verification/confirm", Map.of("token", token));
    }

    private MockHttpServletResponse resendByEmail(String email) throws Exception {
        return post("/api/v1/auth/verification/resend", Map.of("email", email));
    }

    private JsonNode body(MockHttpServletResponse response) throws Exception {
        return json.readTree(response.getContentAsString());
    }

    private boolean verified(String email) {
        return jdbc.queryForObject("SELECT email_verified_at IS NOT NULL FROM users WHERE email = ?", Boolean.class,
                email);
    }

    @Test
    @DisplayName("US2-4, FR-005: the link verifies the account within 24 hours and does not sign the user in")
    void linkVerifiesTheAccountWithoutSigningIn() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        String token = registerAndGetToken(email);
        assertThat(verified(email)).isFalse();

        MockHttpServletResponse response = confirm(token);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(body(response).get("status").asText()).isEqualTo("VERIFIED");
        assertThat(body(response).has("accessToken")).isFalse();
        assertThat(response.getHeader("Set-Cookie")).as("verifying does not sign in").isNull();
        assertThat(verified(email)).isTrue();
        long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_sessions WHERE user_id = ?", Long.class, id))
                .isZero();
        MockHttpServletResponse login = post("/api/v1/auth/login", Map.of("email", email, "password", PASSWORD));
        assertThat(body(login).get("account").get("emailVerified").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("US2-4: the link still works one second before 24 hours")
    void linkWorksUntilItExpires() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        String token = registerAndGetToken(email);
        clock.advance(Duration.ofHours(24).minusSeconds(1));

        assertThat(confirm(token).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("AF 1.12: a link that was already used for a verified account is 200 ALREADY_VERIFIED")
    void usedLinkOfAVerifiedAccountIsAlreadyVerified() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        String token = registerAndGetToken(email);
        confirm(token);

        MockHttpServletResponse again = confirm(token);

        assertThat(again.getStatus()).isEqualTo(200);
        assertThat(body(again).get("status").asText()).isEqualTo("ALREADY_VERIFIED");
    }

    @Test
    @DisplayName("US2-6: an expired link is 410 TOKEN_EXPIRED, and a new link can be asked for with the old token")
    void expiredLinkCanBeResentWithItsToken() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        String token = registerAndGetToken(email);
        clock.advance(Duration.ofHours(24).plusSeconds(1));

        MockHttpServletResponse expired = confirm(token);
        assertThat(expired.getStatus()).isEqualTo(410);
        assertThat(body(expired).get("code").asText()).isEqualTo("TOKEN_EXPIRED");
        assertThat(verified(email)).isFalse();

        MockHttpServletResponse resend = post("/api/v1/auth/verification/resend", Map.of("token", token));
        assertThat(resend.getStatus()).isEqualTo(202);
        String fresh = nextToken(email, 2);
        assertThat(fresh).isNotEqualTo(token);
        assertThat(confirm(fresh).getStatus()).isEqualTo(200);
        assertThat(verified(email)).isTrue();
    }

    @Test
    @DisplayName("TOKEN_INVALID: an unknown token is 410, a blank one is 400")
    void unknownAndBlankTokens() throws Exception {
        MockHttpServletResponse unknown = confirm("never-issued");
        assertThat(unknown.getStatus()).isEqualTo(410);
        assertThat(body(unknown).get("code").asText()).isEqualTo("TOKEN_INVALID");

        assertThat(confirm("").getStatus()).isEqualTo(400);
        assertThat(confirm("x".repeat(129)).getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("FR-005: resending replaces the older link, which then is TOKEN_INVALID")
    void resendInvalidatesTheOlderLink() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        String first = registerAndGetToken(email);

        assertThat(resendByEmail(email).getStatus()).isEqualTo(202);
        String second = nextToken(email, 2);

        MockHttpServletResponse old = confirm(first);
        assertThat(old.getStatus()).isEqualTo(410);
        assertThat(body(old).get("code").asText()).isEqualTo("TOKEN_INVALID");
        assertThat(confirm(second).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("FR-005: resending does not extend the 7 day mark, created_at is untouched")
    void resendDoesNotChangeTheCreationTime() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        registerAndGetToken(email);
        Timestamp before = jdbc.queryForObject("SELECT created_at FROM users WHERE email = ?", Timestamp.class, email);

        resendByEmail(email);
        nextToken(email, 2);

        assertThat(jdbc.queryForObject("SELECT created_at FROM users WHERE email = ?", Timestamp.class, email))
                .isEqualTo(before);
    }

    @Test
    @DisplayName("FR-017: resend is 202 for an unknown address and for a verified account, and sends nothing")
    void resendIsNeutralForUnknownAndVerifiedAddresses() throws Exception {
        String verified = AuthTestSupport.uniqueEmail();
        support.createUser(verified, com.wikicook.common.security.Role.MEMBER,
                com.wikicook.common.security.AccountStatus.ACTIVE, true, null);

        MockHttpServletResponse unknown = resendByEmail(AuthTestSupport.uniqueEmail());
        MockHttpServletResponse alreadyVerified = resendByEmail(verified);

        assertThat(unknown.getStatus()).isEqualTo(202);
        assertThat(alreadyVerified.getStatus()).isEqualTo(202);
        assertThat(unknown.getContentAsString()).isEqualTo(alreadyVerified.getContentAsString());
        Thread.sleep(500);
        assertThat(smtp.messages()).isEmpty();
    }

    @Test
    @DisplayName("FR-017: the 4th request in 15 minutes is 429 EMAIL_RATE_LIMITED with retryAfterSeconds, registered or not")
    void fourthResendIsRateLimitedIdenticallyForRegisteredAndUnknownAddresses() throws Exception {
        String registered = AuthTestSupport.uniqueEmail();
        registerAndGetToken(registered); // counts as the first
        String unknown = AuthTestSupport.uniqueEmail();

        for (int i = 0; i < 2; i++) {
            assertThat(resendByEmail(registered).getStatus()).isEqualTo(202);
        }
        for (int i = 0; i < 3; i++) {
            assertThat(resendByEmail(unknown).getStatus()).isEqualTo(202);
        }
        MockHttpServletResponse limitedRegistered = resendByEmail(registered);
        MockHttpServletResponse limitedUnknown = resendByEmail(unknown);

        assertThat(limitedRegistered.getStatus()).isEqualTo(429).isEqualTo(limitedUnknown.getStatus());
        assertThat(body(limitedRegistered).get("code").asText()).isEqualTo("EMAIL_RATE_LIMITED");
        assertThat(body(limitedUnknown).get("code").asText()).isEqualTo("EMAIL_RATE_LIMITED");
        assertThat(body(limitedRegistered).get("retryAfterSeconds").asLong()).isBetween(1L, 900L);
        assertThat(limitedRegistered.getHeader("Retry-After")).isNotNull();
        assertThat(limitedRegistered.getContentAsString()).doesNotContain(registered);
    }

    @Test
    @DisplayName("Resend: exactly one of email and token is required")
    void resendNeedsExactlyOneIdentifier() throws Exception {
        assertThat(post("/api/v1/auth/verification/resend", Map.of()).getStatus()).isEqualTo(400);
        Map<String, Object> both = new LinkedHashMap<>();
        both.put("email", AuthTestSupport.uniqueEmail());
        both.put("token", "abc");
        assertThat(post("/api/v1/auth/verification/resend", both).getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Resend by an unknown token is 202 and sends nothing")
    void resendByUnknownTokenIsNeutral() throws Exception {
        assertThat(post("/api/v1/auth/verification/resend", Map.of("token", "never-issued")).getStatus())
                .isEqualTo(202);
        Thread.sleep(300);
        assertThat(smtp.messages()).isEmpty();
    }

    @Test
    @DisplayName("Edge: Email delivery failure - the request still succeeds, and asking again delivers the email")
    void smtpFailureDoesNotFailTheRequestAndResendWorks() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        smtp.failNext(3); // all three attempts of the first mail fail

        MockHttpServletResponse response = post("/api/v1/auth/register", Map.of("displayName", "An", "email", email,
                "password", PASSWORD, "confirmPassword", PASSWORD));

        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(body(response).get("verificationEmailSent").asBoolean()).isTrue();
        await().atMost(Duration.ofSeconds(15)).until(() -> smtp.attempts() >= 3);
        assertThat(smtp.messagesTo(email)).isEmpty();

        assertThat(resendByEmail(email).getStatus()).isEqualTo(202);
        String token = nextToken(email, 1);
        assertThat(confirm(token).getStatus()).isEqualTo(200);
    }
}
