package com.wikicook.profile.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.HeaderCurrentUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T057 🕓 #11 (Edge: Suspended or banned): a suspended person can still be seen, a banned one answers exactly like an
 * id that does not exist. Follows the decision written in research.md row 11; that row is not approved yet.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({HeaderCurrentUser.class, AuthTestSupport.class})
class PublicProfileStatusIT extends AbstractPostgresIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Autowired
    AuthTestSupport support;

    @Autowired
    ObjectMapper json;

    private JsonNode problem(String url) throws Exception {
        return json.readTree(mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Edge: Suspended or banned - a SUSPENDED account's page and recipes are visible")
    void suspendedIsVisible() throws Exception {
        long suspended = support.createUser(Role.MEMBER, AccountStatus.SUSPENDED, true);

        mvc.perform(get("/api/v1/profiles/" + suspended)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(suspended));
        mvc.perform(get("/api/v1/profiles/" + suspended + "/recipes")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Edge: Suspended or banned - a BANNED account is 404 USER_NOT_FOUND, the same answer as an unknown id")
    void bannedLooksLikeUnknown() throws Exception {
        long banned = support.createUser(Role.MEMBER, AccountStatus.BANNED, true);
        long unknown = banned + 1_000_000;

        JsonNode forBanned = problem("/api/v1/profiles/" + banned);
        JsonNode forUnknown = problem("/api/v1/profiles/" + unknown);

        assertThat(forBanned.get("code").asText()).isEqualTo("USER_NOT_FOUND");
        for (String field : new String[] {"type", "title", "status", "detail", "code"}) {
            assertThat(forBanned.get(field)).as(field).isEqualTo(forUnknown.get(field));
        }
        java.util.List<String> bannedFields = new java.util.ArrayList<>();
        java.util.List<String> unknownFields = new java.util.ArrayList<>();
        forBanned.fieldNames().forEachRemaining(bannedFields::add);
        forUnknown.fieldNames().forEachRemaining(unknownFields::add);
        assertThat(bannedFields).containsExactlyInAnyOrderElementsOf(unknownFields);
        problem("/api/v1/profiles/" + banned + "/recipes");
    }

    @Test
    @DisplayName("Edge: Suspended or banned - an ACTIVE account is visible, an account that was banned and is reinstated again")
    void reinstatedIsVisibleAgain() throws Exception {
        long member = support.createUser(Role.MEMBER, AccountStatus.BANNED, true);
        problem("/api/v1/profiles/" + member);

        jdbc.update("UPDATE users SET status = 'ACTIVE' WHERE id = ?", member);

        mvc.perform(get("/api/v1/profiles/" + member)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Edge: Suspended or banned - the answer for a banned account does not depend on who asks")
    void sameForSignedInPeople() throws Exception {
        long banned = support.createUser(Role.MEMBER, AccountStatus.BANNED, true);
        long other = support.member();

        mvc.perform(get("/api/v1/profiles/" + banned).header(HeaderCurrentUser.HEADER, other)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }
}
