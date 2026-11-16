package com.wikicook.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.AuthTestSupport.SignedIn;
import com.wikicook.support.MatrixProbeController;
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
 * FR-024a, research R11: a role change sends no email, shows a notice once, and the app learns of it on the next
 * request of the affected user: every response to a bearer request carries {@code X-WikiCook-Role-Notice: 1} until the
 * notice is acknowledged, except the answers of {@code GET /auth/me} and {@code POST /auth/role-notice/ack}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({AuthTestSupport.class, MatrixProbeController.class})
class RoleNoticeIT extends AbstractPostgresIT {

    private static final String HEADER = "X-WikiCook-Role-Notice";

    @Autowired
    MockMvc mvc;

    @Autowired
    AuthTestSupport support;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    private SignedIn admin;

    @BeforeEach
    void setUp() {
        support.resetThrottle();
        admin = support.signedIn(Role.ADMINISTRATOR, AccountStatus.ACTIVE, true);
    }

    private void changeRole(long target, String role) throws Exception {
        MockHttpServletResponse response = mvc.perform(patch("/api/v1/admin/users/" + target + "/role")
                .header("Authorization", admin.bearer()).contentType("application/json")
                .content("{\"role\":\"" + role + "\"}")).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private MockHttpServletResponse me(SignedIn as) throws Exception {
        return mvc.perform(get("/api/v1/auth/me").header("Authorization", as.bearer())).andReturn().getResponse();
    }

    private MockHttpServletResponse whoami(SignedIn as) throws Exception {
        return mvc.perform(get("/api/v1/probe/whoami").header("Authorization", as.bearer())).andReturn()
                .getResponse();
    }

    private MockHttpServletResponse ack(SignedIn as) throws Exception {
        return mvc.perform(post("/api/v1/auth/role-notice/ack").header("Authorization", as.bearer())).andReturn()
                .getResponse();
    }

    private JsonNode notice(SignedIn as) throws Exception {
        return json.readTree(me(as).getContentAsString()).get("roleNotice");
    }

    @Test
    @DisplayName("FR-024a: /auth/me names the previous and the new role after a change, and a change sends no email")
    void meCarriesTheNotice() throws Exception {
        SignedIn target = support.signedInMember();
        assertThat(notice(target)).isNull();

        changeRole(target.userId(), "CONTRIBUTOR");

        JsonNode notice = notice(target);
        assertThat(notice.get("previousRole").asText()).isEqualTo("MEMBER");
        assertThat(notice.get("newRole").asText()).isEqualTo("CONTRIBUTOR");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM email_outbox WHERE user_id = ?", Long.class,
                target.userId())).isZero();
    }

    @Test
    @DisplayName("FR-024a: acknowledging clears the notice, and acknowledging again is not an error")
    void ackClearsTheNotice() throws Exception {
        SignedIn target = support.signedInMember();
        changeRole(target.userId(), "CONTRIBUTOR");

        assertThat(ack(target).getStatus()).isEqualTo(204);
        assertThat(notice(target)).isNull();
        assertThat(ack(target).getStatus()).isEqualTo(204);
        assertThat(jdbc.queryForObject("SELECT pending_role_notice_from FROM users WHERE id = ?", String.class,
                target.userId())).isNull();
    }

    @Test
    @DisplayName("FR-024a: several changes before it is seen keep the original role; going back to it clears the notice")
    void severalChangesKeepTheOriginalRole() throws Exception {
        SignedIn target = support.signedInMember();

        changeRole(target.userId(), "CONTRIBUTOR");
        changeRole(target.userId(), "MODERATOR");
        JsonNode notice = notice(target);
        assertThat(notice.get("previousRole").asText()).isEqualTo("MEMBER");
        assertThat(notice.get("newRole").asText()).isEqualTo("MODERATOR");

        changeRole(target.userId(), "MEMBER");
        assertThat(notice(target)).isNull();
    }

    @Test
    @DisplayName("FR-024a: every bearer response carries the header until acknowledged, but /auth/me and the ack do not")
    void headerIsOnEveryResponseExceptMeAndAck() throws Exception {
        SignedIn target = support.signedInMember();
        assertThat(whoami(target).getHeader(HEADER)).as("no notice, no header").isNull();

        changeRole(target.userId(), "CONTRIBUTOR");

        assertThat(whoami(target).getHeader(HEADER)).isEqualTo("1");
        assertThat(whoami(target).getHeader(HEADER)).as("until it is acknowledged, not only once").isEqualTo("1");
        assertThat(me(target).getHeader(HEADER)).as("the notice is already in the body").isNull();
        assertThat(mvc.perform(get("/api/v1/shopping-lists/active").header("Authorization", target.bearer()))
                .andReturn().getResponse().getHeader(HEADER)).isEqualTo("1");

        MockHttpServletResponse acknowledged = ack(target);
        assertThat(acknowledged.getHeader(HEADER)).as("the ack is read before it clears").isNull();
        assertThat(whoami(target).getHeader(HEADER)).isNull();
    }

    @Test
    @DisplayName("US3-5: the 403 a demoted Moderator gets also carries the header, so the app learns of the demotion")
    void theForbiddenAnswerAfterADemotionCarriesTheHeader() throws Exception {
        SignedIn moderator = support.signedIn(Role.MODERATOR, AccountStatus.ACTIVE, true);
        long victim = support.createUser(Role.MEMBER, AccountStatus.ACTIVE, true);
        changeRole(moderator.userId(), "MEMBER");

        MockHttpServletResponse refused = mvc.perform(post("/api/v1/admin/users/" + victim + "/suspension")
                .header("Authorization", moderator.bearer()).contentType("application/json")
                .content("{\"reason\":\"x\",\"endsAt\":\"2099-01-01T00:00:00Z\"}")).andReturn().getResponse();

        assertThat(refused.getStatus()).isEqualTo(403);
        assertThat(refused.getHeader(HEADER)).isEqualTo("1");
        JsonNode notice = notice(moderator);
        assertThat(notice.get("previousRole").asText()).isEqualTo("MODERATOR");
        assertThat(notice.get("newRole").asText()).isEqualTo("MEMBER");
    }

    @Test
    @DisplayName("FR-024a: a request without a bearer never carries the header")
    void noBearerNoHeader() throws Exception {
        assertThat(mvc.perform(get("/api/v1/recipes/search")).andReturn().getResponse().getHeader(HEADER)).isNull();
        assertThat(mvc.perform(get("/api/v1/probe/whoami")).andReturn().getResponse().getHeader(HEADER)).isNull();
    }

    @Test
    @DisplayName("FR-024a: the ack needs a signed-in user")
    void ackNeedsASession() throws Exception {
        assertThat(mvc.perform(post("/api/v1/auth/role-notice/ack")).andReturn().getResponse().getStatus())
                .isEqualTo(401);
    }
}
