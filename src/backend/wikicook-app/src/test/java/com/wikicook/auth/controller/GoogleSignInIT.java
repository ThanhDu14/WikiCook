package com.wikicook.auth.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.wikicook.auth.api.BlockedEmails;
import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.FakeGoogle;
import com.wikicook.support.FakeGoogle.Person;
import com.wikicook.support.GoogleFlow;
import com.wikicook.support.GoogleFlow.Started;
import com.wikicook.support.GoogleIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Spec 010 US5 (T141): Google sign-in end to end through the real filter chain, with {@link FakeGoogle} in place of
 * Google: the redirect carries PKCE, the token endpoint and the key set are called for real, and the ID token is
 * signed and checked. The table of research R7 is walked row by row; so are the ways the flow can be attacked (another
 * {@code state}, a changed or missing cookie, a wrong nonce, audience or signature, an open redirect).
 */
@GoogleIntegrationTest
class GoogleSignInIT extends AbstractPostgresIT {

    private static final String LOGIN = "http://localhost:3000/login?error=";
    private static final Pattern REFRESH_COOKIE = Pattern.compile("wc_rt=([^;]*)");

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AuthTestSupport support;

    @Autowired
    BlockedEmails blockedEmails;

    @Autowired
    ObjectMapper json;

    @BeforeEach
    void setUp() {
        FakeGoogle.reset();
        support.resetThrottle();
    }

    private static String newSub() {
        return "google-" + UUID.randomUUID();
    }

    /** The whole flow for one Google person; returns the answer of the callback. */
    private MockHttpServletResponse signInWithGoogle(Person person, String redirect) throws Exception {
        Started started = GoogleFlow.start(mvc, redirect);
        FakeGoogle.tokenEndpointReturns(person.idToken(started.nonce()));
        return GoogleFlow.callback(mvc, started, started.state());
    }

    private static Person person(String email, String name) {
        return new Person(newSub(), email, name);
    }

    private long countUsers(String email) {
        return jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Long.class, email);
    }

    private Map<String, Object> user(String email) {
        return jdbc.queryForMap("""
                SELECT id, role, status, display_name, google_sub, password_hash,
                       email_verified_at IS NOT NULL AS verified, last_sign_in_at IS NOT NULL AS signed_in
                FROM users WHERE email = ?""", email);
    }

    private long sessions(long userId) {
        return jdbc.queryForObject("SELECT count(*) FROM user_sessions WHERE user_id = ?", Long.class, userId);
    }

    private static String refreshToken(MockHttpServletResponse response) {
        for (String header : response.getHeaders("Set-Cookie")) {
            Matcher matcher = REFRESH_COOKIE.matcher(header);
            if (matcher.find() && !matcher.group(1).isEmpty()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private static String location(MockHttpServletResponse response) {
        return response.getHeader("Location");
    }

    private void assertNoSessionAnywhere(MockHttpServletResponse response) {
        assertThat(refreshToken(response)).as("no refresh cookie").isNull();
        assertThat(response.getCookie("JSESSIONID")).as("the application has no HttpSession").isNull();
    }

    private void assertClearsOauthCookie(MockHttpServletResponse response) {
        assertThat(response.getHeaders("Set-Cookie")).anyMatch(h -> h.startsWith("wc_oauth=;") && h.contains("Max-Age=0"));
    }

    // --- US5-1 -----------------------------------------------------------------------------------------------

    @Test
    @DisplayName("US5-1: a new Google person gets a verified Member, a session cookie and goes to /auth/callback")
    void newAccount() throws Exception {
        String email = AuthTestSupport.uniqueEmail();

        MockHttpServletResponse response = signInWithGoogle(person(email, "  Lê Thị Bếp  "), "/meal-planner?week=2");

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(location(response)).isEqualTo("http://localhost:3000/auth/callback?redirect=%2Fmeal-planner%3Fweek%3D2");
        Map<String, Object> row = user(email);
        assertThat(row).containsEntry("role", "MEMBER").containsEntry("status", "ACTIVE").containsEntry("verified", true)
                .containsEntry("display_name", "Lê Thị Bếp").containsEntry("signed_in", true);
        assertThat(row.get("google_sub")).isNotNull();
        assertThat(row.get("password_hash")).as("a Google-only account has no password").isNull();
        assertClearsOauthCookie(response);
        assertThat(response.getCookie("JSESSIONID")).isNull();

        // the cookie is the refresh cookie of research R2, and a short one: no Remember me (FR-018, OQ-010-08)
        String cookie = response.getHeaders("Set-Cookie").stream().filter(h -> h.startsWith("wc_rt=")).findFirst().orElseThrow();
        assertThat(cookie).contains("HttpOnly").contains("SameSite=Strict").contains("Path=/api/v1/auth").doesNotContain("Max-Age");
        assertThat(jdbc.queryForObject("SELECT remember_me FROM user_sessions WHERE user_id = ?", Boolean.class, row.get("id")))
                .isFalse();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM audit_logs WHERE action = 'SIGN_IN_SUCCEEDED' AND target_id = ?", Long.class, row.get("id")))
                .isEqualTo(1);

        // the SPA now trades the cookie for an access token, as after a reload
        JsonNode refreshed = json.readTree(mvc.perform(post("/api/v1/auth/refresh").header("X-WikiCook-Client", "web")
                .cookie(new Cookie("wc_rt", refreshToken(response)))).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
        assertThat(refreshed.at("/account/email").asText()).isEqualTo(email);
        assertThat(refreshed.at("/account/emailVerified").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("US5-1: signing in again with the same Google person reuses the account (google_sub)")
    void sameAccountTheSecondTime() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        Person person = person(email, "Bếp Trưởng");
        signInWithGoogle(person, null);
        long id = (Long) user(email).get("id");

        MockHttpServletResponse second = signInWithGoogle(person, null);

        assertThat(location(second)).isEqualTo("http://localhost:3000/auth/callback");
        assertThat(countUsers(email)).isEqualTo(1);
        assertThat(user(email).get("id")).isEqualTo(id);
        assertThat(sessions(id)).isEqualTo(2);
    }

    @Test
    @DisplayName("FR-018: the Google name is cut to 50 code points; without a name the part before the @ is used")
    void displayNames() throws Exception {
        String longEmail = AuthTestSupport.uniqueEmail();
        signInWithGoogle(person(longEmail, "😀".repeat(60)), null);
        assertThat(((String) user(longEmail).get("display_name")).codePointCount(0, ((String) user(longEmail).get("display_name")).length()))
                .isEqualTo(50);

        String bareEmail = AuthTestSupport.uniqueEmail();
        signInWithGoogle(person(bareEmail, null), null);
        assertThat(user(bareEmail).get("display_name")).isEqualTo(bareEmail.substring(0, bareEmail.indexOf('@')));
    }

    // --- US5-2 -----------------------------------------------------------------------------------------------

    @Test
    @DisplayName("US5-2, FR-019: an email of a verified account signs in to it, links the google_sub, keeps data and role")
    void verifiedAccountIsLinked() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        long id = support.userWithPassword(email, "Correct-horse-1", Role.CONTRIBUTOR, AccountStatus.ACTIVE, true);
        jdbc.update("UPDATE users SET bio = 'Bio cũ', display_name = 'Tên cũ' WHERE id = ?", id);
        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, id);

        MockHttpServletResponse response = signInWithGoogle(person(email, "Tên Google"), "/profile");

        assertThat(location(response)).isEqualTo("http://localhost:3000/auth/callback?redirect=%2Fprofile");
        Map<String, Object> row = user(email);
        assertThat(row).containsEntry("role", "CONTRIBUTOR").containsEntry("display_name", "Tên cũ");
        assertThat(row.get("id")).isEqualTo(id);
        assertThat(row.get("google_sub")).isNotNull();
        assertThat(row.get("password_hash")).as("the password still works").isEqualTo(hash);
        assertThat(jdbc.queryForObject("SELECT bio FROM users WHERE id = ?", String.class, id)).isEqualTo("Bio cũ");
        assertThat(refreshToken(response)).isNotNull();
    }

    @Test
    @DisplayName("an email linked to another Google person is refused and nothing is overwritten")
    void anotherGooglePersonWithTheSameEmail() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        signInWithGoogle(person(email, "Một"), null);
        String linked = (String) user(email).get("google_sub");

        MockHttpServletResponse response = signInWithGoogle(person(email, "Hai"), null);

        assertThat(location(response)).isEqualTo(LOGIN + "google_unavailable");
        assertNoSessionAnywhere(response);
        assertThat(user(email).get("google_sub")).isEqualTo(linked);
    }

    // --- US5-3 and the refused rows -----------------------------------------------------------------------

    @Test
    @DisplayName("US5-3: the user cancels at Google: back to /login with google_cancelled, nothing created")
    void cancelled() throws Exception {
        Started started = GoogleFlow.start(mvc, "/meal-planner");

        MockHttpServletResponse response = GoogleFlow.cancelled(mvc, started);

        assertThat(location(response)).isEqualTo(LOGIN + "google_cancelled");
        assertNoSessionAnywhere(response);
        assertClearsOauthCookie(response);
        FakeGoogle.SERVER.verify(0, postRequestedFor(urlPathEqualTo("/token")));
    }

    @Test
    @DisplayName("Edge: a Google account without an email address, or with an unverified one, is refused")
    void noVerifiedEmail() throws Exception {
        String unverified = AuthTestSupport.uniqueEmail();
        MockHttpServletResponse notVerified = signInWithGoogle(person(unverified, "A").unverified(), null);
        assertThat(location(notVerified)).isEqualTo(LOGIN + "google_no_verified_email");
        assertThat(countUsers(unverified)).isZero();
        assertNoSessionAnywhere(notVerified);

        MockHttpServletResponse noClaim = signInWithGoogle(person(unverified, "A").withoutVerifiedClaim(), null);
        assertThat(location(noClaim)).as("a missing email_verified is not verified").isEqualTo(LOGIN + "google_no_verified_email");
        assertThat(countUsers(unverified)).isZero();

        MockHttpServletResponse noEmail = signInWithGoogle(new Person(newSub(), null, "B"), null);
        assertThat(location(noEmail)).isEqualTo(LOGIN + "google_no_verified_email");
        assertNoSessionAnywhere(noEmail);
    }

    @Test
    @DisplayName("FR-026: an email on the Blocked Email list is refused with a neutral answer, nothing is created")
    void blockedEmail() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        blockedEmails.block(email);

        MockHttpServletResponse response = signInWithGoogle(person(email, "Bị chặn"), null);

        assertThat(location(response)).isEqualTo(LOGIN + "registration_refused");
        assertThat(countUsers(email)).isZero();
        assertNoSessionAnywhere(response);
    }

    @Test
    @DisplayName("FR-013: a suspended account is not signed in; the reason and end date go in the URL fragment")
    void suspendedAccount() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        long id = support.createUser(email, Role.MEMBER, AccountStatus.SUSPENDED, true, null);
        Instant ends = Instant.now().plus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        support.sanction(id, "Spam & quảng cáo", ends);

        MockHttpServletResponse response = signInWithGoogle(person(email, "X"), "/profile");

        URI location = URI.create(location(response));
        assertThat(location.getRawPath()).isEqualTo("/account-status");
        assertThat(location.getRawQuery()).isNull();
        Map<String, String> fragment = fragment(location);
        assertThat(fragment).containsEntry("code", "ACCOUNT_SUSPENDED").containsEntry("reason", "Spam & quảng cáo");
        assertThat(Instant.parse(fragment.get("endsAt"))).isEqualTo(ends);
        assertNoSessionAnywhere(response);
        assertThat(sessions(id)).isZero();
        assertThat(user(email).get("google_sub")).as("nothing is linked to a refused account").isNull();
    }

    @Test
    @DisplayName("FR-013: a banned account gets code and reason, no end date")
    void bannedAccount() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        long id = support.createUser(email, Role.MEMBER, AccountStatus.BANNED, true, null);
        support.sanction(id, "Vi phạm nhiều lần", null);

        MockHttpServletResponse response = signInWithGoogle(person(email, "X"), null);

        Map<String, String> fragment = fragment(URI.create(location(response)));
        assertThat(fragment).containsEntry("code", "ACCOUNT_BANNED").containsEntry("reason", "Vi phạm nhiều lần")
                .doesNotContainKey("endsAt");
        assertNoSessionAnywhere(response);
    }

    @Test
    @DisplayName("FR-024a: a suspension that has already ended is closed first, then Google signs the person in")
    void endedSuspension() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        long id = support.createUser(email, Role.MEMBER, AccountStatus.SUSPENDED, true, null);
        support.sanction(id, "Hết hạn", Instant.now().minus(1, ChronoUnit.MINUTES));

        MockHttpServletResponse response = signInWithGoogle(person(email, "X"), null);

        assertThat(location(response)).isEqualTo("http://localhost:3000/auth/callback");
        assertThat(user(email)).containsEntry("status", "ACTIVE");
        assertThat(refreshToken(response)).isNotNull();
    }

    @Test
    @DisplayName("FR-012: the lock of a wrong-password streak does not stop Google sign-in")
    void lockDoesNotStopGoogle() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        long id = support.userWithPassword(email, "Correct-horse-1", Role.MEMBER, AccountStatus.ACTIVE, true);
        jdbc.update("UPDATE users SET locked_until = now() + interval '15 minutes' WHERE id = ?", id);

        MockHttpServletResponse response = signInWithGoogle(person(email, "X"), null);

        assertThat(location(response)).isEqualTo("http://localhost:3000/auth/callback");
        assertThat(refreshToken(response)).isNotNull();
    }

    // --- the flow itself: PKCE, state, nonce, cookie, redirect ----------------------------------------------

    @Test
    @DisplayName("PKCE: the redirect to Google has code_challenge_method=S256 and the token request proves the verifier")
    void pkce() throws Exception {
        Started started = GoogleFlow.start(mvc, null);
        assertThat(started.param("response_type")).isEqualTo("code");
        assertThat(started.param("client_id")).isEqualTo(FakeGoogle.CLIENT_ID);
        assertThat(started.param("code_challenge_method")).isEqualTo("S256");
        assertThat(started.challenge()).isNotBlank();
        assertThat(started.param("scope")).isEqualTo("openid profile email");
        assertThat(started.param("redirect_uri")).isEqualTo("http://localhost:3000/api/v1/auth/oauth2/callback/google");
        assertThat(started.state()).hasSizeGreaterThan(20);
        assertThat(started.nonce()).isNotBlank();
        Cookie cookie = started.cookie();
        assertThat(cookie).isNotNull();
        String header = started.response().getHeaders("Set-Cookie").stream().filter(h -> h.startsWith("wc_oauth=")).findFirst().orElseThrow();
        assertThat(header).contains("HttpOnly").contains("SameSite=Lax").contains("Max-Age=300").contains("Path=/api/v1/auth/oauth2");

        FakeGoogle.tokenEndpointReturns(new Person(newSub(), AuthTestSupport.uniqueEmail(), "P").idToken(started.nonce()));
        GoogleFlow.callback(mvc, started, started.state());

        List<LoggedRequest> tokenCalls = FakeGoogle.SERVER.findAll(postRequestedFor(urlPathEqualTo("/token")));
        assertThat(tokenCalls).hasSize(1);
        Map<String, String> form = UriComponentsBuilder.newInstance().query(tokenCalls.get(0).getBodyAsString()).build()
                .getQueryParams().toSingleValueMap();
        assertThat(form).containsEntry("grant_type", "authorization_code").containsEntry("code", "fake-code");
        assertThat(GoogleFlow.s256(form.get("code_verifier"))).as("the verifier hashes to the challenge sent to Google")
                .isEqualTo(started.challenge());
        assertThat(tokenCalls.get(0).getHeader("Authorization")).as("client secret by Basic auth").startsWith("Basic ");
    }

    @Test
    @DisplayName("state: a callback with another state is refused before the token endpoint is called")
    void wrongState() throws Exception {
        Started started = GoogleFlow.start(mvc, null);
        String email = AuthTestSupport.uniqueEmail();
        FakeGoogle.tokenEndpointReturns(new Person(newSub(), email, "P").idToken(started.nonce()));

        MockHttpServletResponse response = GoogleFlow.callback(mvc, started, "another-state");

        assertThat(location(response)).isEqualTo(LOGIN + "google_unavailable");
        assertNoSessionAnywhere(response);
        assertThat(countUsers(email)).isZero();
        FakeGoogle.SERVER.verify(0, postRequestedFor(urlPathEqualTo("/token")));
    }

    @Test
    @DisplayName("state: a callback without the wc_oauth cookie (another browser, a replay) is refused")
    void missingCookie() throws Exception {
        Started started = GoogleFlow.start(mvc, null);
        String email = AuthTestSupport.uniqueEmail();
        FakeGoogle.tokenEndpointReturns(new Person(newSub(), email, "P").idToken(started.nonce()));

        MockHttpServletResponse response = GoogleFlow.callback(mvc, null, "code", "fake-code", started.state());

        assertThat(location(response)).isEqualTo(LOGIN + "google_unavailable");
        assertNoSessionAnywhere(response);
        assertThat(countUsers(email)).isZero();
        FakeGoogle.SERVER.verify(0, postRequestedFor(urlPathEqualTo("/token")));
    }

    @Test
    @DisplayName("a changed wc_oauth cookie (HMAC) is refused, including a swapped redirect")
    void tamperedCookie() throws Exception {
        Started started = GoogleFlow.start(mvc, "/meal-planner");
        String email = AuthTestSupport.uniqueEmail();
        FakeGoogle.tokenEndpointReturns(new Person(newSub(), email, "P").idToken(started.nonce()));
        String value = started.cookie().getValue();
        int dot = value.indexOf('.');
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                new String(java.util.Base64.getUrlDecoder().decode(value.substring(0, dot)), StandardCharsets.UTF_8)
                        .replace("/meal-planner", "/evil").getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse changed = GoogleFlow.callback(mvc, new Cookie("wc_oauth", forgedPayload + value.substring(dot)),
                "code", "fake-code", started.state());
        MockHttpServletResponse garbage = GoogleFlow.callback(mvc, new Cookie("wc_oauth", "not-a-cookie"), "code", "x", started.state());

        assertThat(location(changed)).isEqualTo(LOGIN + "google_unavailable");
        assertThat(location(garbage)).isEqualTo(LOGIN + "google_unavailable");
        assertThat(countUsers(email)).isZero();
        FakeGoogle.SERVER.verify(0, postRequestedFor(urlPathEqualTo("/token")));
    }

    @Test
    @DisplayName("nonce: an ID token made for another request (another nonce) is refused")
    void idTokenWithAnotherNonce() throws Exception {
        Started started = GoogleFlow.start(mvc, null);
        String email = AuthTestSupport.uniqueEmail();
        FakeGoogle.tokenEndpointReturns(new Person(newSub(), email, "P").idToken("a-nonce-from-another-flow"));

        MockHttpServletResponse response = GoogleFlow.callback(mvc, started, started.state());

        assertThat(location(response)).as("nonce of the ID token must match the request").isEqualTo(LOGIN + "google_unavailable");
        assertThat(countUsers(email)).isZero();
        assertNoSessionAnywhere(response);
    }

    @Test
    @DisplayName("the ID token is checked: another audience, an expired one or a signature of another key is refused")
    void badIdTokens() throws Exception {
        for (Person person : List.of(
                person(AuthTestSupport.uniqueEmail(), "A").audience("someone-elses-client"),
                person(AuthTestSupport.uniqueEmail(), "B").expired(),
                person(AuthTestSupport.uniqueEmail(), "C").forged())) {
            Started started = GoogleFlow.start(mvc, null);
            FakeGoogle.tokenEndpointReturns(person.idToken(started.nonce()));

            MockHttpServletResponse response = GoogleFlow.callback(mvc, started, started.state());

            assertThat(location(response)).isEqualTo(LOGIN + "google_unavailable");
            assertNoSessionAnywhere(response);
            assertThat(countUsers(person.email())).isZero();
        }
    }

    @Test
    @DisplayName("FR-011: a return page outside WikiCook is dropped, whoever sends it, at the start of the flow")
    void openRedirect() throws Exception {
        for (String evil : List.of("https://evil.example/phish", "//evil.example", "/\\evil.example", "javascript:alert(1)",
                "http://localhost:3000.evil.example/", "/login", "/register", "relative/path")) {
            MockHttpServletResponse response = signInWithGoogle(person(AuthTestSupport.uniqueEmail(), "R"), evil);

            assertThat(location(response)).as(evil).isEqualTo("http://localhost:3000/auth/callback");
        }
        MockHttpServletResponse fine = signInWithGoogle(person(AuthTestSupport.uniqueEmail(), "R"), "/recipes/42?tab=reviews");
        assertThat(location(fine)).isEqualTo("http://localhost:3000/auth/callback?redirect=%2Frecipes%2F42%3Ftab%3Dreviews");
    }

    @Test
    @DisplayName("a redirect sent with the callback itself is ignored: only the one signed into the cookie counts")
    void redirectOnTheCallbackIsIgnored() throws Exception {
        Started started = GoogleFlow.start(mvc, "/profile");
        FakeGoogle.tokenEndpointReturns(new Person(newSub(), AuthTestSupport.uniqueEmail(), "P").idToken(started.nonce()));

        MockHttpServletResponse response = mvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/oauth2/callback/google")
                        .param("code", "fake-code").param("state", started.state()).param("redirect", "https://evil.example")
                        .cookie(started.cookie())).andReturn().getResponse();

        assertThat(location(response)).isEqualTo("http://localhost:3000/auth/callback?redirect=%2Fprofile");
    }

    @Test
    @DisplayName("intent=reauth is not built yet: it never signs in, it answers as a cancelled re-authentication")
    void reauthIsNotASignIn() throws Exception {
        MockHttpServletResponse response = mvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/oauth2/google")
                        .param("intent", "REAUTH").param("redirect", "/profile/security")).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(location(response)).isEqualTo("http://localhost:3000/profile/security?reauth=cancelled");
        assertThat(response.getCookie("wc_oauth")).isNull();
    }

    @Test
    @DisplayName("email sign-in is unaffected by the Google beans: POST /auth/login still works")
    void passwordSignInStillWorks() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        support.memberWithPassword(email, "Correct-horse-1");

        MockHttpServletResponse response = mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content("{\"email\":\"%s\",\"password\":\"Correct-horse-1\",\"rememberMe\":false}".formatted(email)))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        FakeGoogle.SERVER.verify(0, postRequestedFor(urlPathEqualTo("/token")));
    }

    private static Map<String, String> fragment(URI location) {
        return UriComponentsBuilder.newInstance().query(location.getRawFragment()).build().getQueryParams().toSingleValueMap()
                .entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,
                        e -> org.springframework.web.util.UriUtils.decode(e.getValue().replace('+', ' '), StandardCharsets.UTF_8)));
    }
}
