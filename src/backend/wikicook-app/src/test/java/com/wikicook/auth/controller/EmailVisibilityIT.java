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
import com.wikicook.support.AuthTestSupport.SignedIn;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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
 * NFR-009: the email is visible only to its owner and to Administrators; the dietary profile and household size are
 * never in a response of spec 010.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthTestSupport.class)
class EmailVisibilityIT extends AbstractPostgresIT {

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

    private JsonNode read(String path, SignedIn as) throws Exception {
        MockHttpServletResponse response = mvc.perform(get(path).header("Authorization", as.bearer())).andReturn()
                .getResponse();
        return json.readTree(response.getContentAsString());
    }

    private static List<String> names(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    @Test
    @DisplayName("NFR-009: /auth/me has the caller's own email and no dietary profile or household size")
    void meHasOnlyTheCallersOwnAccountData() throws Exception {
        SignedIn caller = support.signedInMember();
        jdbc.update("UPDATE users SET diet_type = 'VEGAN', household_size = 4, max_cook_time_minutes = 30 "
                + "WHERE id = ?", caller.userId());
        String email = jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, caller.userId());

        JsonNode me = read("/api/v1/auth/me", caller);

        assertThat(me.get("email").asText()).isEqualTo(email);
        assertThat(names(me)).isSubsetOf("id", "displayName", "email", "role", "emailVerified", "roleNotice");
        assertThat(me.toString()).doesNotContain("householdSize").doesNotContain("dietType")
                .doesNotContain("VEGAN").doesNotContain("passwordHash").doesNotContain("password_hash");
    }

    @Test
    @DisplayName("NFR-009: another user's email is not reachable through any 010 endpoint of a non-Administrator")
    void nonAdministratorsCannotReadAnotherAccount() throws Exception {
        long other = support.createUser(Role.MEMBER, AccountStatus.ACTIVE, true);
        for (Role role : new Role[] {Role.MEMBER, Role.CONTRIBUTOR, Role.MODERATOR}) {
            SignedIn caller = support.signedIn(role, AccountStatus.ACTIVE, true);

            MockHttpServletResponse response = mvc.perform(get("/api/v1/admin/users/" + other)
                    .header("Authorization", caller.bearer())).andReturn().getResponse();

            assertThat(response.getStatus()).as(role.name()).isEqualTo(403);
            assertThat(response.getContentAsString()).doesNotContain("@");
        }
    }

    @Test
    @DisplayName("NFR-009: an Administrator reads the account with its email, never the password hash")
    void administratorReadsTheAccount() throws Exception {
        SignedIn admin = support.signedIn(Role.ADMINISTRATOR, AccountStatus.ACTIVE, true);
        long other = support.userWithPassword(AuthTestSupport.uniqueEmail(), "abc12345", Role.MEMBER,
                AccountStatus.ACTIVE, true);

        JsonNode view = read("/api/v1/admin/users/" + other, admin);

        assertThat(view.get("email").asText()).isNotBlank();
        assertThat(names(view)).isSubsetOf("id", "displayName", "email", "role", "status", "emailVerified",
                "activeSanction");
        assertThat(view.toString()).doesNotContain("$2a$").doesNotContain("householdSize");
    }

    @Test
    @DisplayName("NFR-009: the answer to a Moderator's suspension has no email")
    void moderatorSuspensionResponseHasNoEmail() throws Exception {
        SignedIn moderator = support.signedIn(Role.MODERATOR, AccountStatus.ACTIVE, true);
        long member = support.createUser(Role.MEMBER, AccountStatus.ACTIVE, true);
        String email = jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, member);

        MockHttpServletResponse response = mvc.perform(post("/api/v1/admin/users/" + member + "/suspension")
                .header("Authorization", moderator.bearer()).contentType("application/json")
                .content("{\"reason\":\"Spam\",\"endsAt\":\"" + Instant.now().plus(2, ChronoUnit.DAYS) + "\"}"))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).doesNotContain(email).doesNotContain("\"email\"");
    }
}
