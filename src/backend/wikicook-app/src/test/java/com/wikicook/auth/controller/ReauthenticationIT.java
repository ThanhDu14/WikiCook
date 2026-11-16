package com.wikicook.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.auth.security.BcryptBulkhead;
import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.MutableClock;
import java.sql.Timestamp;
import java.time.Duration;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * FR-022a: {@code POST /auth/reauthenticate} with the right password opens 5 minutes in which an Administrator may act
 * on another Administrator; after that {@code REAUTH_REQUIRED} again. A wrong password is a 401, and the comparison
 * runs through the BCrypt bulkhead.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({AuthTestSupport.class, MutableClock.class})
class ReauthenticationIT extends AbstractPostgresIT {

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

    @MockitoSpyBean
    BcryptBulkhead bulkhead;

    private AuthTestSupport.SignedIn admin;
    private long otherAdmin;

    @BeforeEach
    void setUp() {
        clock.reset();
        support.resetThrottle();
        long id = support.userWithPassword(AuthTestSupport.uniqueEmail(), PASSWORD, Role.ADMINISTRATOR,
                AccountStatus.ACTIVE, true);
        long session = support.session(id);
        admin = new AuthTestSupport.SignedIn(id, session, support.jwt(id, session));
        otherAdmin = support.createUser(Role.ADMINISTRATOR, AccountStatus.ACTIVE, true);
    }

    private MockHttpServletResponse reauthenticate(String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/reauthenticate").header("Authorization", admin.bearer())
                .contentType("application/json").content(json.writeValueAsString(Map.of("password", password))))
                .andReturn().getResponse();
    }

    private MockHttpServletResponse demoteTheOtherAdministrator() throws Exception {
        return mvc.perform(patch("/api/v1/admin/users/" + otherAdmin + "/role")
                .header("Authorization", admin.bearer()).contentType("application/json")
                .content("{\"role\":\"MODERATOR\"}")).andReturn().getResponse();
    }

    private String codeOf(MockHttpServletResponse response) throws Exception {
        JsonNode body = json.readTree(response.getContentAsString());
        return body.get("code").asText();
    }

    @Test
    @DisplayName("FR-022a: the right password opens 5 minutes, then REAUTH_REQUIRED again")
    void rightPasswordOpensFiveMinutes() throws Exception {
        assertThat(codeOf(demoteTheOtherAdministrator())).isEqualTo("REAUTH_REQUIRED");

        MockHttpServletResponse response = reauthenticate(PASSWORD);
        assertThat(response.getStatus()).isEqualTo(204);
        assertThat(jdbc.queryForObject("SELECT reauthenticated_at FROM user_sessions WHERE id = ?", Timestamp.class,
                admin.sessionId())).isNotNull();

        clock.advance(Duration.ofMinutes(4).plusSeconds(59));
        assertThat(demoteTheOtherAdministrator().getStatus()).as("inside the 5 minutes").isEqualTo(200);

        jdbc.update("UPDATE users SET role = 'ADMINISTRATOR' WHERE id = ?", otherAdmin);
        clock.advance(Duration.ofSeconds(2));
        MockHttpServletResponse late = demoteTheOtherAdministrator();
        assertThat(late.getStatus()).as("after 5 minutes").isEqualTo(403);
        assertThat(codeOf(late)).isEqualTo("REAUTH_REQUIRED");
    }

    @Test
    @DisplayName("FR-022a: a wrong password is 401 and opens nothing, and the comparison goes through the bulkhead")
    void wrongPasswordIs401AndGoesThroughTheBulkhead() throws Exception {
        clearInvocations(bulkhead);

        MockHttpServletResponse response = reauthenticate("wrong-password1");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(codeOf(response)).isEqualTo("INVALID_CREDENTIALS");
        verify(bulkhead, atLeastOnce()).run(org.mockito.ArgumentMatchers.any());
        assertThat(jdbc.queryForObject("SELECT reauthenticated_at FROM user_sessions WHERE id = ?", Timestamp.class,
                admin.sessionId())).isNull();
        assertThat(codeOf(demoteTheOtherAdministrator())).isEqualTo("REAUTH_REQUIRED");
    }

    @Test
    @DisplayName("FR-022a: re-authenticating one session does not open another session of the same user")
    void onlyTheReauthenticatedSessionIsOpened() throws Exception {
        reauthenticate(PASSWORD);
        long otherSession = support.session(admin.userId());

        MockHttpServletResponse response = mvc.perform(patch("/api/v1/admin/users/" + otherAdmin + "/role")
                .header("Authorization", "Bearer " + support.jwt(admin.userId(), otherSession))
                .contentType("application/json").content("{\"role\":\"MODERATOR\"}")).andReturn().getResponse();

        assertThat(codeOf(response)).isEqualTo("REAUTH_REQUIRED");
    }

    @Test
    @DisplayName("Edge: Very long input - a password over 128 code points is 400 and is not compared")
    void tooLongPasswordIs400() throws Exception {
        clearInvocations(bulkhead);

        MockHttpServletResponse response = reauthenticate("a1".repeat(65));

        assertThat(response.getStatus()).isEqualTo(400);
        org.mockito.Mockito.verifyNoInteractions(bulkhead);
    }

    @Test
    @DisplayName("An account without a password (Google only) cannot re-authenticate with one")
    void accountWithoutAPasswordIs401() throws Exception {
        long id = support.userWithPassword(AuthTestSupport.uniqueEmail(), null, Role.ADMINISTRATOR,
                AccountStatus.ACTIVE, true);
        long session = support.session(id);

        MockHttpServletResponse response = mvc.perform(post("/api/v1/auth/reauthenticate")
                .header("Authorization", "Bearer " + support.jwt(id, session)).contentType("application/json")
                .content("{\"password\":\"abc12345\"}")).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Re-authenticating needs a signed-in user")
    void needsASession() throws Exception {
        assertThat(mvc.perform(post("/api/v1/auth/reauthenticate").contentType("application/json")
                .content("{\"password\":\"abc12345\"}")).andReturn().getResponse().getStatus()).isEqualTo(401);
    }
}
