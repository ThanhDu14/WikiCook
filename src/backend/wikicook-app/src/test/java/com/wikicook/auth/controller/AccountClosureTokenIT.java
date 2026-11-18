package com.wikicook.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.auth.api.ClosureTokens;
import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.MutableClock;
import java.time.Duration;
import java.util.OptionalLong;
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
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Spec 010 FR-013, research R16 (T157): a banned user has no session, so after the right password they get a
 * {@code closureToken} that opens only the closure page of 011: single use, 10 minutes, no session row, not a bearer.
 * {@code consume} joins the transaction of the caller (011 FR-037b): a rollback gives the token back, and outside a
 * transaction it fails instead of spending the token. (Google gives the token in the fragment: {@code GoogleSignInIT}.)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({AuthTestSupport.class, MutableClock.class})
class AccountClosureTokenIT extends AbstractPostgresIT {

    private static final String PASSWORD = "Correct-horse-1";

    @Autowired
    MockMvc mvc;

    @Autowired
    AuthTestSupport support;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ClosureTokens closure;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    MutableClock clock;

    @Autowired
    ObjectMapper json;

    @BeforeEach
    void setUp() {
        clock.reset();
        support.resetThrottle();
    }

    private record Banned(long id, String email) {
    }

    private Banned banned() {
        String email = AuthTestSupport.uniqueEmail();
        long id = support.userWithPassword(email, PASSWORD, Role.MEMBER, AccountStatus.BANNED, true);
        support.sanction(id, "Fraud", null);
        return new Banned(id, email);
    }

    private MockHttpServletResponse login(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content("{\"email\":\"%s\",\"password\":\"%s\",\"rememberMe\":false}".formatted(email, password)))
                .andReturn().getResponse();
    }

    private JsonNode body(MockHttpServletResponse response) throws Exception {
        return json.readTree(response.getContentAsString());
    }

    private String tokenOf(Banned user) throws Exception {
        MockHttpServletResponse response = login(user.email(), PASSWORD);
        assertThat(response.getStatus()).isEqualTo(403);
        return body(response).get("closureToken").asText();
    }

    private OptionalLong consumeInTransaction(String token) {
        return transaction.execute(status -> closure.consume(token));
    }

    @Test
    @DisplayName("FR-013: the right password of a banned account is 403 ACCOUNT_BANNED with a closureToken, and no session")
    void rightPasswordGivesTheToken() throws Exception {
        Banned user = banned();

        MockHttpServletResponse response = login(user.email(), PASSWORD);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body(response).get("code").asText()).isEqualTo("ACCOUNT_BANNED");
        assertThat(body(response).get("closureToken").asText()).hasSizeGreaterThan(30);
        assertThat(response.getHeaders("Set-Cookie")).as("no refresh cookie").isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_sessions WHERE user_id = ?", Long.class, user.id())).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM one_time_tokens WHERE user_id = ? AND purpose = 'ACCOUNT_CLOSURE'",
                Long.class, user.id())).isEqualTo(1);
    }

    @Test
    @DisplayName("the token is stored as a hash only, for 10 minutes")
    void storedAsHashFor10Minutes() throws Exception {
        Banned user = banned();
        String token = tokenOf(user);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM one_time_tokens WHERE token_hash = ?", Long.class, token)).isZero();
        Long seconds = jdbc.queryForObject(
                "SELECT extract(epoch FROM expires_at - created_at)::bigint FROM one_time_tokens WHERE user_id = ? AND purpose = 'ACCOUNT_CLOSURE'",
                Long.class, user.id());
        assertThat(seconds).isEqualTo(600);
    }

    @Test
    @DisplayName("a wrong password gives no token and does not say the account is banned")
    void wrongPasswordGivesNothing() throws Exception {
        Banned user = banned();

        MockHttpServletResponse response = login(user.email(), "wrong-password1");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).doesNotContain("closureToken").doesNotContain("BANNED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM one_time_tokens WHERE user_id = ?", Long.class, user.id())).isZero();
    }

    @Test
    @DisplayName("a suspended account gets no closure token (only a ban leads to closing the account)")
    void suspendedGetsNoToken() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        long id = support.userWithPassword(email, PASSWORD, Role.MEMBER, AccountStatus.SUSPENDED, true);
        support.sanction(id, "Spam", java.time.Instant.now().plus(Duration.ofDays(2)));

        MockHttpServletResponse response = login(email, PASSWORD);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body(response).has("closureToken")).isFalse();
    }

    @Test
    @DisplayName("consume returns the user once; a second use is refused")
    void singleUse() throws Exception {
        Banned user = banned();
        String token = tokenOf(user);

        assertThat(consumeInTransaction(token)).hasValue(user.id());
        assertThat(consumeInTransaction(token)).isEmpty();
    }

    @Test
    @DisplayName("an unknown, blank or null token is refused")
    void unknownToken() {
        assertThat(consumeInTransaction("not-a-token")).isEmpty();
        assertThat(consumeInTransaction("")).isEmpty();
        assertThat(consumeInTransaction(null)).isEmpty();
    }

    @Test
    @DisplayName("the token is dead after 10 minutes")
    void expires() throws Exception {
        Banned user = banned();
        String token = tokenOf(user);

        clock.advance(Duration.ofMinutes(11));

        assertThat(consumeInTransaction(token)).isEmpty();
    }

    @Test
    @DisplayName("a newer token replaces the older one of the same user")
    void newestWins() throws Exception {
        Banned user = banned();
        String first = tokenOf(user);
        String second = tokenOf(user);

        assertThat(consumeInTransaction(first)).isEmpty();
        assertThat(consumeInTransaction(second)).hasValue(user.id());
    }

    @Test
    @DisplayName("011 FR-037b: when the caller's transaction rolls back, the token can be used again")
    void rollbackGivesTheTokenBack() throws Exception {
        Banned user = banned();
        String token = tokenOf(user);

        transaction.executeWithoutResult(status -> {
            assertThat(closure.consume(token)).hasValue(user.id());
            status.setRollbackOnly();
        });

        assertThat(consumeInTransaction(token)).as("not spent by the rolled-back action").hasValue(user.id());
    }

    @Test
    @DisplayName("called outside a transaction it fails and does not spend the token")
    void outsideATransactionFails() throws Exception {
        Banned user = banned();
        String token = tokenOf(user);

        assertThatThrownBy(() -> closure.consume(token)).isInstanceOf(TransactionException.class);

        assertThat(consumeInTransaction(token)).hasValue(user.id());
    }

    @Test
    @DisplayName("the token opens no API: as a bearer it is a 401, and it is not a refresh cookie either")
    void notABearerAndNotASession() throws Exception {
        Banned user = banned();
        String token = tokenOf(user);

        assertThat(mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token)).andReturn().getResponse().getStatus())
                .isEqualTo(401);
        assertThat(mvc.perform(get("/api/v1/me/profile").header("Authorization", "Bearer " + token)).andReturn().getResponse().getStatus())
                .isEqualTo(401);
        assertThat(mvc.perform(post("/api/v1/auth/refresh").header("X-WikiCook-Client", "web")
                .cookie(new jakarta.servlet.http.Cookie("wc_rt", token))).andReturn().getResponse().getStatus()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_sessions WHERE user_id = ?", Long.class, user.id())).isZero();
    }
}
