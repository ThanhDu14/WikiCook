package com.wikicook.auth.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.FakeGoogle;
import com.wikicook.support.FakeGoogle.Person;
import com.wikicook.support.GoogleFlow;
import com.wikicook.support.GoogleFlow.Started;
import com.wikicook.support.GoogleIntegrationTest;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Spec 010 NFR-005 (T143), Google configured but failing: Google answers with an error, or does not answer. The user
 * is sent back to {@code /login?error=google_unavailable} within the five seconds of research R7, nothing is created,
 * and e-mail sign-in keeps working. (Google not configured at all is {@code GoogleNotConfiguredIT}.)
 */
@GoogleIntegrationTest
class GoogleUnavailableIT extends AbstractPostgresIT {

    private static final String UNAVAILABLE = "http://localhost:3000/login?error=google_unavailable";
    private static final int SLOW_MILLIS = 8_000;

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AuthTestSupport support;

    @BeforeEach
    void setUp() {
        FakeGoogle.reset();
        support.resetThrottle();
    }

    private long users(String email) {
        return jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Long.class, email);
    }

    private void assertEmailSignInWorks() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        support.memberWithPassword(email, "Correct-horse-1");
        MockHttpServletResponse login = mvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login").contentType("application/json")
                .content("{\"email\":\"%s\",\"password\":\"Correct-horse-1\",\"rememberMe\":false}".formatted(email)))
                .andReturn().getResponse();
        assertThat(login.getStatus()).as("POST /auth/login while Google is down").isEqualTo(200);
    }

    @Test
    @DisplayName("NFR-005: the token endpoint answers 500: back to /login with google_unavailable, nothing created")
    void tokenEndpointFails() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        Started started = GoogleFlow.start(mvc, null);
        FakeGoogle.SERVER.stubFor(post(urlPathEqualTo("/token")).willReturn(aResponse().withStatus(500).withBody("boom")));

        MockHttpServletResponse response = GoogleFlow.callback(mvc, started, started.state());

        assertThat(response.getHeader("Location")).isEqualTo(UNAVAILABLE);
        assertThat(response.getHeaders("Set-Cookie")).noneMatch(h -> h.startsWith("wc_rt="));
        assertThat(users(email)).isZero();
        assertEmailSignInWorks();
    }

    @Test
    @DisplayName("an OAuth error from the token endpoint (invalid_grant, a used or expired code) is the same answer")
    void tokenEndpointRefusesTheCode() throws Exception {
        Started started = GoogleFlow.start(mvc, null);
        FakeGoogle.SERVER.stubFor(post(urlPathEqualTo("/token")).willReturn(aResponse().withStatus(400)
                .withHeader("Content-Type", "application/json").withBody("{\"error\":\"invalid_grant\"}")));

        MockHttpServletResponse response = GoogleFlow.callback(mvc, started, started.state());

        assertThat(response.getHeader("Location")).isEqualTo(UNAVAILABLE);
        assertEmailSignInWorks();
    }

    @Test
    @DisplayName("NFR-005, R7: a token endpoint that does not answer is given up after 5 seconds, not waited for")
    void tokenEndpointTimesOut() throws Exception {
        Started started = GoogleFlow.start(mvc, null);
        FakeGoogle.SERVER.stubFor(post(urlPathEqualTo("/token")).willReturn(aResponse().withStatus(200).withFixedDelay(SLOW_MILLIS)));

        long begin = System.nanoTime();
        MockHttpServletResponse response = GoogleFlow.callback(mvc, started, started.state());
        Duration waited = Duration.ofNanos(System.nanoTime() - begin);

        assertThat(response.getHeader("Location")).isEqualTo(UNAVAILABLE);
        assertThat(waited).as("gave up at the 5 s timeout").isBetween(Duration.ofSeconds(4), Duration.ofMillis(SLOW_MILLIS - 1_000));
        assertEmailSignInWorks();
    }

    @Test
    @DisplayName("NFR-005, R7: a key set that does not answer is given up after 5 seconds too; no account is created")
    void keySetTimesOut() throws Exception {
        Started started = GoogleFlow.start(mvc, null);
        String email = AuthTestSupport.uniqueEmail();
        // an unknown key id makes the decoder fetch the key set again, which here never answers in time
        FakeGoogle.tokenEndpointReturns(new Person("google-" + UUID.randomUUID(), email, "P").forged().idToken(started.nonce()));
        FakeGoogle.SERVER.stubFor(get(urlPathEqualTo("/certs")).willReturn(aResponse().withStatus(200).withFixedDelay(SLOW_MILLIS)));

        long begin = System.nanoTime();
        MockHttpServletResponse response = GoogleFlow.callback(mvc, started, started.state());
        Duration waited = Duration.ofNanos(System.nanoTime() - begin);

        assertThat(response.getHeader("Location")).isEqualTo(UNAVAILABLE);
        assertThat(waited).isLessThan(Duration.ofMillis(SLOW_MILLIS - 1_000));
        assertThat(users(email)).isZero();
        assertEmailSignInWorks();
    }

    @Test
    @DisplayName("the key set answers 500: refused, nothing created")
    void keySetFails() throws Exception {
        Started started = GoogleFlow.start(mvc, null);
        String email = AuthTestSupport.uniqueEmail();
        FakeGoogle.tokenEndpointReturns(new Person("google-" + UUID.randomUUID(), email, "P").forged().idToken(started.nonce()));
        FakeGoogle.SERVER.stubFor(get(urlPathEqualTo("/certs")).willReturn(aResponse().withStatus(500)));

        MockHttpServletResponse response = GoogleFlow.callback(mvc, started, started.state());

        assertThat(response.getHeader("Location")).isEqualTo(UNAVAILABLE);
        assertThat(users(email)).isZero();
    }
}
