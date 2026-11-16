package com.wikicook.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.MutableClock;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
import org.springframework.test.web.servlet.MockMvc;

/**
 * FR-012, SC-005, research R4, R5: the lockout counts wrong passwords per normalized email, also for emails with no
 * account, and sign-ins per IP. The clock is fixed and moved by the test, so the 15 minute rules are exact.
 */
@SpringBootTest(properties = "wikicook.auth.sign-in.ip-limit=100")
@AutoConfigureMockMvc
@Import({AuthTestSupport.class, MutableClock.class})
class SignInThrottleIT extends AbstractPostgresIT {

    private static final String PASSWORD = "abc12345";

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

    private MockHttpServletResponse login(String email, String password) throws Exception {
        return login(email, password, "203.0.113.50");
    }

    private MockHttpServletResponse login(String email, String password, String ip) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", email, "password", password)))
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })).andReturn().getResponse();
    }

    private JsonNode body(MockHttpServletResponse response) throws Exception {
        return json.readTree(response.getContentAsString());
    }

    private void wrongPasswords(String email, int count) throws Exception {
        for (int i = 0; i < count; i++) {
            login(email, "wrong-" + i);
        }
    }

    @Test
    @DisplayName("Edge: Brute force - the 5th wrong password is already 423 ACCOUNT_LOCKED with retryAt")
    void fifthWrongPasswordLocksForFifteenMinutes() throws Exception {
        for (int i = 1; i <= 4; i++) {
            assertThat(login(email, "wrong").getStatus()).as("attempt %d", i).isEqualTo(401);
            clock.advance(Duration.ofMinutes(1));
        }
        Instant fifth = clock.instant();

        MockHttpServletResponse locked = login(email, "wrong");

        assertThat(locked.getStatus()).isEqualTo(423);
        assertThat(body(locked).get("code").asText()).isEqualTo("ACCOUNT_LOCKED");
        assertThat(Instant.parse(body(locked).get("retryAt").asText())).isEqualTo(fifth.plus(Duration.ofMinutes(15)));
    }

    @Test
    @DisplayName("Edge: Brute force - while locked even the right password is refused and the lock is not extended")
    void attemptsWhileLockedDoNotExtendTheLock() throws Exception {
        wrongPasswords(email, 5);
        Instant fifth = clock.instant();
        String retryAt = body(login(email, "wrong")).get("retryAt").asText();

        clock.advance(Duration.ofMinutes(7));
        MockHttpServletResponse again = login(email, PASSWORD);

        assertThat(again.getStatus()).isEqualTo(423);
        assertThat(body(again).get("retryAt").asText()).isEqualTo(retryAt);
        assertThat(Instant.parse(retryAt)).isEqualTo(fifth.plus(Duration.ofMinutes(15)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM login_attempts WHERE user_id = ?", Long.class, userId))
                .as("rejected attempts are not counted").isEqualTo(5L);
    }

    @Test
    @DisplayName("Edge: Brute force - the lock opens exactly 15 minutes after the 5th wrong password")
    void lockOpensFifteenMinutesAfterTheFifthFailure() throws Exception {
        wrongPasswords(email, 5);

        clock.advance(Duration.ofMinutes(15).minusSeconds(1));
        assertThat(login(email, PASSWORD).getStatus()).isEqualTo(423);

        clock.advance(Duration.ofSeconds(1));
        assertThat(login(email, PASSWORD).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Edge: Brute force - after the lock opens a wrong password starts a fresh count")
    void afterTheLockOpensTheCountStartsAgain() throws Exception {
        wrongPasswords(email, 5);
        clock.advance(Duration.ofMinutes(15));

        assertThat(login(email, "wrong").getStatus()).isEqualTo(401);
        assertThat(login(email, "wrong").getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("FR-012: failures more than 15 minutes apart do not lock")
    void failuresSpreadOverMoreThanFifteenMinutesDoNotLock() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(login(email, "wrong").getStatus()).as("attempt %d", i + 1).isEqualTo(401);
            clock.advance(Duration.ofMinutes(4)); // first to fifth: 16 minutes
        }
        assertThat(login(email, PASSWORD).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("FR-012: a correct sign-in resets the counter")
    void successfulSignInResetsTheCounter() throws Exception {
        wrongPasswords(email, 4);
        assertThat(login(email, PASSWORD).getStatus()).isEqualTo(200);

        assertThat(login(email, "wrong").getStatus()).isEqualTo(401);
        assertThat(login(email, "wrong").getStatus()).isEqualTo(401);
        assertThat(login(email, "wrong").getStatus()).isEqualTo(401);
        assertThat(login(email, "wrong").getStatus()).isEqualTo(401);
        assertThat(login(email, PASSWORD).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("SC-005: ten parallel wrong passwords compare at most five times")
    void parallelAttemptsNeverExceedFiveComparisons() throws Exception {
        List<Integer> statuses = new ArrayList<>();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                futures.add(pool.submit(() -> login(email, "wrong").getStatus()));
            }
            for (Future<Integer> future : futures) {
                statuses.add(future.get(30, TimeUnit.SECONDS));
            }
        }

        assertThat(jdbc.queryForObject("SELECT count(*) FROM login_attempts WHERE user_id = ?", Long.class, userId))
                .as("rows are inserted before the password is compared, so this is the number of comparisons")
                .isEqualTo(5L);
        assertThat(statuses).allMatch(s -> s == 401 || s == 423);
        assertThat(statuses.stream().filter(s -> s == 423).count()).isGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("OQ-010-02: an email with no account locks the same way, and users.locked_until is only written for accounts")
    void unknownEmailIsLockedIdentically() throws Exception {
        String stranger = AuthTestSupport.uniqueEmail();
        for (int i = 0; i < 4; i++) {
            assertThat(login(stranger, "wrong").getStatus()).isEqualTo(401);
        }
        Instant fifth = clock.instant();
        MockHttpServletResponse lockedStranger = login(stranger, "wrong");

        wrongPasswords(email, 4);
        MockHttpServletResponse lockedAccount = login(email, "wrong");

        assertThat(lockedStranger.getStatus()).isEqualTo(423).isEqualTo(lockedAccount.getStatus());
        assertThat(body(lockedStranger).get("code").asText()).isEqualTo(body(lockedAccount).get("code").asText());
        assertThat(body(lockedStranger).get("detail").asText()).isEqualTo(body(lockedAccount).get("detail").asText());
        assertThat(Instant.parse(body(lockedStranger).get("retryAt").asText()))
                .isEqualTo(fifth.plus(Duration.ofMinutes(15)));
        Timestamp lockedUntil = jdbc.queryForObject("SELECT locked_until FROM users WHERE id = ?", Timestamp.class,
                userId);
        assertThat(lockedUntil).isNotNull();
        assertThat(lockedUntil.toInstant()).isEqualTo(Instant.parse(body(lockedAccount).get("retryAt").asText()));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM login_attempts WHERE user_id IS NULL", Long.class))
                .isEqualTo(5L);
    }

    @Test
    @DisplayName("FR-012, R5: the 101st attempt from one IP within 15 minutes is 429 TOO_MANY_REQUESTS with Retry-After")
    void ipLimitIs100AttemptsInFifteenMinutes() throws Exception {
        for (int i = 0; i < 100; i++) {
            assertThat(login(AuthTestSupport.uniqueEmail(), "wrong", "198.51.100.7").getStatus())
                    .as("attempt %d", i + 1).isEqualTo(401);
        }

        MockHttpServletResponse limited = login(AuthTestSupport.uniqueEmail(), "wrong", "198.51.100.7");

        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(body(limited).get("code").asText()).isEqualTo("TOO_MANY_REQUESTS");
        assertThat(body(limited).get("retryAfterSeconds").asLong()).isBetween(1L, 900L);
        assertThat(limited.getHeader("Retry-After")).isEqualTo(body(limited).get("retryAfterSeconds").asText());
        // another address is not affected, and the window slides
        assertThat(login(AuthTestSupport.uniqueEmail(), "wrong", "198.51.100.8").getStatus()).isEqualTo(401);
        clock.advance(Duration.ofMinutes(15).plusSeconds(1));
        assertThat(login(AuthTestSupport.uniqueEmail(), "wrong", "198.51.100.7").getStatus()).isEqualTo(401);
    }
}
