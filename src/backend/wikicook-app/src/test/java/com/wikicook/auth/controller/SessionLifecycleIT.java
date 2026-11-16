package com.wikicook.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.MutableClock;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
 * Sessions end to end (FR-009, FR-010, research R2): sign out, "Remember me" and the 24 hour idle rule with a movable
 * clock, parallel devices, refresh-token rotation with its 10 second grace, and reuse detection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({AuthTestSupport.class, MutableClock.class})
class SessionLifecycleIT extends AbstractPostgresIT {

    private static final String PASSWORD = "abc12345";
    private static final Pattern COOKIE_VALUE = Pattern.compile("wc_rt=([^;]*)");

    @Autowired
    MockMvc mvc;

    @Autowired
    AuthTestSupport support;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MutableClock clock;

    @Autowired
    ObjectMapper json;

    private String email;
    private long userId;

    @BeforeEach
    void setUp() {
        clock.reset();
        support.resetThrottle();
        email = AuthTestSupport.uniqueEmail();
        userId = support.memberWithPassword(email, PASSWORD);
    }

    private MockHttpServletResponse login(boolean rememberMe) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD,
                        "rememberMe", rememberMe)))).andReturn().getResponse();
    }

    private static String cookieValue(MockHttpServletResponse response) {
        String header = response.getHeader("Set-Cookie");
        assertThat(header).as("Set-Cookie").isNotNull();
        Matcher matcher = COOKIE_VALUE.matcher(header);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private MockHttpServletResponse refresh(String cookie) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").header("X-WikiCook-Client", "web")
                .cookie(new Cookie("wc_rt", cookie))).andReturn().getResponse();
    }

    private MockHttpServletResponse logout(String cookie) throws Exception {
        return mvc.perform(post("/api/v1/auth/logout").header("X-WikiCook-Client", "web")
                .cookie(new Cookie("wc_rt", cookie))).andReturn().getResponse();
    }

    private int me(String accessToken) throws Exception {
        return mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
                .andReturn().getResponse().getStatus();
    }

    private JsonNode body(MockHttpServletResponse response) throws Exception {
        return json.readTree(response.getContentAsString());
    }

    @Test
    @DisplayName("US1-4, FR-010: signing out ends the session at once, the still-valid JWT is 401, and it is audited")
    void signOutEndsTheSessionImmediately() throws Exception {
        MockHttpServletResponse login = login(false);
        String accessToken = body(login).get("accessToken").asText();
        assertThat(me(accessToken)).isEqualTo(200);

        MockHttpServletResponse out = logout(cookieValue(login));

        assertThat(out.getStatus()).isEqualTo(204);
        assertThat(out.getHeader("Set-Cookie")).startsWith("wc_rt=;").contains("Max-Age=0").contains("HttpOnly")
                .contains("Path=/api/v1/auth");
        assertThat(me(accessToken)).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE action = 'SIGNED_OUT' AND actor_id = ?",
                Long.class, userId)).isEqualTo(1);
        assertThat(refresh(cookieValue(login)).getStatus()).as("the cookie is dead too").isEqualTo(401);
    }

    @Test
    @DisplayName("US1-4: signing out is idempotent and works without a valid access token")
    void signOutIsIdempotent() throws Exception {
        String cookie = cookieValue(login(false));

        assertThat(logout(cookie).getStatus()).isEqualTo(204);
        assertThat(logout(cookie).getStatus()).isEqualTo(204);
        assertThat(mvc.perform(post("/api/v1/auth/logout").header("X-WikiCook-Client", "web"))
                .andReturn().getResponse().getStatus()).isEqualTo(204);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE action = 'SIGNED_OUT' AND actor_id = ?",
                Long.class, userId)).as("only the first sign-out is news").isEqualTo(1);
    }

    @Test
    @DisplayName("US1-5, FR-009: with Remember me the cookie has Max-Age and the session is refreshable for 30 days")
    void rememberMeLastsThirtyDays() throws Exception {
        MockHttpServletResponse login = login(true);
        String header = login.getHeader("Set-Cookie");
        Matcher maxAge = Pattern.compile("Max-Age=(\\d+)").matcher(header);
        assertThat(maxAge.find()).as(header).isTrue();
        assertThat(Long.parseLong(maxAge.group(1))).isBetween(Duration.ofDays(30).minusMinutes(1).toSeconds(),
                Duration.ofDays(30).toSeconds());

        clock.advance(Duration.ofDays(29));
        MockHttpServletResponse refreshed = refresh(cookieValue(login));
        assertThat(refreshed.getStatus()).isEqualTo(200);
        assertThat(me(body(refreshed).get("accessToken").asText())).isEqualTo(200);

        clock.advance(Duration.ofDays(2));
        assertThat(refresh(cookieValue(refreshed)).getStatus()).as("past 30 days from sign-in").isEqualTo(401);
    }

    @Test
    @DisplayName("US1-6, FR-009: without Remember me the cookie is a session cookie and 24 hours idle ends the session")
    void withoutRememberMeTwentyFourHoursIdleEndsTheSession() throws Exception {
        MockHttpServletResponse login = login(false);
        assertThat(login.getHeader("Set-Cookie")).doesNotContain("Max-Age");

        clock.advance(Duration.ofHours(23));
        MockHttpServletResponse refreshed = refresh(cookieValue(login));
        assertThat(refreshed.getStatus()).as("still within 24 hours").isEqualTo(200);
        assertThat(refreshed.getHeader("Set-Cookie")).doesNotContain("Max-Age");

        clock.advance(Duration.ofHours(23)); // the refresh counted as activity
        MockHttpServletResponse again = refresh(cookieValue(refreshed));
        assertThat(again.getStatus()).isEqualTo(200);

        clock.advance(Duration.ofHours(25));
        assertThat(refresh(cookieValue(again)).getStatus()).as("25 hours without activity").isEqualTo(401);
    }

    @Test
    @DisplayName("US1-6: the access token of an idle session stops working after 24 hours of inactivity")
    void anIdleSessionsAccessTokenStopsWorking() throws Exception {
        MockHttpServletResponse login = login(false);
        String accessToken = body(login).get("accessToken").asText();

        clock.advance(Duration.ofHours(25));

        assertThat(me(accessToken)).isEqualTo(401);
    }

    @Test
    @DisplayName("Edge: Simultaneous sessions - signing out one device leaves the other signed in")
    void signingOutOneDeviceKeepsTheOther() throws Exception {
        MockHttpServletResponse phone = login(true);
        MockHttpServletResponse laptop = login(false);

        logout(cookieValue(phone));

        assertThat(me(body(phone).get("accessToken").asText())).isEqualTo(401);
        assertThat(me(body(laptop).get("accessToken").asText())).isEqualTo(200);
        assertThat(refresh(cookieValue(laptop)).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("R2: refresh rotates the cookie and returns a fresh access token with the account")
    void refreshRotatesTheCookie() throws Exception {
        MockHttpServletResponse login = login(true);
        String first = cookieValue(login);

        MockHttpServletResponse refreshed = refresh(first);

        assertThat(refreshed.getStatus()).isEqualTo(200);
        assertThat(cookieValue(refreshed)).isNotEqualTo(first).isNotEmpty();
        assertThat(body(refreshed).get("expiresIn").asInt()).isEqualTo(900);
        assertThat(body(refreshed).get("account").get("id").asLong()).isEqualTo(userId);
        assertThat(refreshed.getHeader("Set-Cookie")).contains("Max-Age").contains("HttpOnly");
    }

    @Test
    @DisplayName("R2: the old cookie sent again within 10 seconds (two tabs) is accepted and the cookie is not changed")
    void replayWithinTenSecondsIsAcceptedWithoutANewCookie() throws Exception {
        String first = cookieValue(login(true));
        MockHttpServletResponse rotated = refresh(first);
        String second = cookieValue(rotated);

        clock.advance(Duration.ofSeconds(9));
        MockHttpServletResponse replay = refresh(first);

        assertThat(replay.getStatus()).isEqualTo(200);
        assertThat(replay.getHeader("Set-Cookie")).as("no new cookie for the loser of the race").isNull();
        assertThat(me(body(replay).get("accessToken").asText())).isEqualTo(200);
        assertThat(refresh(second).getStatus()).as("the rotated cookie still works").isEqualTo(200);
    }

    @Test
    @DisplayName("R2: the old cookie after 10 seconds is theft, the session is revoked and REFRESH_TOKEN_REUSED is audited")
    void replayAfterTenSecondsRevokesTheSession() throws Exception {
        MockHttpServletResponse login = login(true);
        String first = cookieValue(login);
        String second = cookieValue(refresh(first));

        clock.advance(Duration.ofSeconds(11));
        MockHttpServletResponse stolen = refresh(first);

        assertThat(stolen.getStatus()).isEqualTo(401);
        assertThat(body(stolen).get("code").asText()).isEqualTo("UNAUTHENTICATED");
        assertThat(refresh(second).getStatus()).as("the legitimate cookie died with the session").isEqualTo(401);
        assertThat(me(body(login).get("accessToken").asText())).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE action = 'REFRESH_TOKEN_REUSED' "
                + "AND actor_id = ?", Long.class, userId)).isEqualTo(1);
    }

    @Test
    @DisplayName("R2: two parallel refreshes with the same cookie both succeed and do not sign the user out")
    void parallelRefreshesDoNotSignTheUserOut() throws Exception {
        String cookie = cookieValue(login(true));
        List<MockHttpServletResponse> responses = new ArrayList<>();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<MockHttpServletResponse>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> refresh(cookie)));
            }
            for (Future<MockHttpServletResponse> future : futures) {
                responses.add(future.get(30, TimeUnit.SECONDS));
            }
        }

        assertThat(responses).allSatisfy(r -> assertThat(r.getStatus()).isEqualTo(200));
        assertThat(responses.stream().filter(r -> r.getHeader("Set-Cookie") != null)).hasSize(1);
        String rotated = responses.stream().map(r -> r.getHeader("Set-Cookie")).filter(h -> h != null).findFirst()
                .map(h -> {
                    Matcher matcher = COOKIE_VALUE.matcher(h);
                    matcher.find();
                    return matcher.group(1);
                }).orElseThrow();
        assertThat(refresh(rotated).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("US1-4: refresh without the cookie is 401 UNAUTHENTICATED")
    void refreshWithoutTheCookieIs401() throws Exception {
        MockHttpServletResponse response = mvc.perform(post("/api/v1/auth/refresh")
                .header("X-WikiCook-Client", "web")).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body(response).get("code").asText()).isEqualTo("UNAUTHENTICATED");
        assertThat(refresh("not-a-real-token").getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("FR-013, FR-024: refreshing for a suspended account is 403 ACCOUNT_SUSPENDED with the reason")
    void refreshOfASuspendedAccountIs403() throws Exception {
        String cookie = cookieValue(login(true));
        jdbc.update("UPDATE users SET status = 'SUSPENDED' WHERE id = ?", userId);
        Instant endsAt = clock.instant().plus(Duration.ofDays(1)).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        support.sanction(userId, "Spam", endsAt);

        MockHttpServletResponse response = refresh(cookie);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body(response).get("code").asText()).isEqualTo("ACCOUNT_SUSPENDED");
        assertThat(body(response).get("reason").asText()).isEqualTo("Spam");
    }

    @Test
    @DisplayName("FR-024a: /auth/me returns the account, with the role notice while one is pending")
    void meReturnsTheAccountAndThePendingRoleNotice() throws Exception {
        String accessToken = body(login(false)).get("accessToken").asText();

        JsonNode plain = json.readTree(mvc.perform(get("/api/v1/auth/me").header("Authorization",
                "Bearer " + accessToken)).andReturn().getResponse().getContentAsString());
        assertThat(plain.get("id").asLong()).isEqualTo(userId);
        assertThat(plain.get("email").asText()).isEqualTo(email);
        assertThat(plain.get("role").asText()).isEqualTo("MEMBER");
        assertThat(plain.has("roleNotice") && !plain.get("roleNotice").isNull()).isFalse();

        jdbc.update("UPDATE users SET role = 'CONTRIBUTOR', pending_role_notice_from = 'MEMBER' WHERE id = ?", userId);
        JsonNode withNotice = json.readTree(mvc.perform(get("/api/v1/auth/me").header("Authorization",
                "Bearer " + accessToken)).andReturn().getResponse().getContentAsString());
        assertThat(withNotice.get("role").asText()).isEqualTo("CONTRIBUTOR");
        assertThat(withNotice.get("roleNotice").get("previousRole").asText()).isEqualTo("MEMBER");
        assertThat(withNotice.get("roleNotice").get("newRole").asText()).isEqualTo("CONTRIBUTOR");
    }

    @Test
    @DisplayName("NFR-009: /auth/me without a token is 401")
    void meWithoutATokenIs401() throws Exception {
        assertThat(mvc.perform(get("/api/v1/auth/me")).andReturn().getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Role is not copied into the token: a role change shows in /auth/me with the same JWT")
    void roleChangeShowsWithTheSameToken() throws Exception {
        String accessToken = body(login(false)).get("accessToken").asText();
        jdbc.update("UPDATE users SET role = 'MODERATOR' WHERE id = ?", userId);

        JsonNode me = json.readTree(mvc.perform(get("/api/v1/auth/me").header("Authorization",
                "Bearer " + accessToken)).andReturn().getResponse().getContentAsString());

        assertThat(me.get("role").asText()).isEqualTo("MODERATOR");
    }
}
