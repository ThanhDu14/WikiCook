package com.wikicook.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.AuthTestSupport.SignedIn;
import java.sql.Timestamp;
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
 * NFR-008, FR-025: Administrators read the audit trail, newest first, with filters and paging; the trail has no
 * write endpoint, keeps entries of deleted accounts without any personal data, and keeps the old ones readable.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthTestSupport.class)
class AuditLogIT extends AbstractPostgresIT {

    private static final String URL = "/api/v1/admin/audit-logs";

    private static final List<String> ACTIONS_OF_010 = List.of("SIGN_IN_SUCCEEDED", "SIGN_IN_FAILED", "ACCOUNT_LOCKED",
            "SIGNED_OUT", "PASSWORD_RESET", "REFRESH_TOKEN_REUSED", "ROLE_CHANGED", "ACCOUNT_SUSPENDED",
            "ACCOUNT_BANNED", "ACCOUNT_REINSTATED", "ACCOUNT_RESET", "ACCOUNT_PURGED_UNVERIFIED");

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

    private long newTargetId() {
        return 5_000_000_000L + (System.nanoTime() % 1_000_000_000L);
    }

    private void insert(String action, Long actor, Long target, String details, Instant at) {
        jdbc.update("INSERT INTO audit_logs (actor_id, action, target_type, target_id, details, created_at) "
                + "VALUES (?, ?, 'USER', ?, ?::jsonb, ?)", actor, action, target, details, Timestamp.from(at));
    }

    private MockHttpServletResponse list(SignedIn as, String query) throws Exception {
        var request = get(URL + (query == null ? "" : "?" + query));
        if (as != null) {
            request.header("Authorization", as.bearer());
        }
        return mvc.perform(request).andReturn().getResponse();
    }

    private JsonNode page(String query) throws Exception {
        MockHttpServletResponse response = list(admin, query);
        assertThat(response.getStatus()).isEqualTo(200);
        return json.readTree(response.getContentAsString());
    }

    @Test
    @DisplayName("NFR-008: only an Administrator reads the audit trail")
    void onlyAdministratorsRead() throws Exception {
        assertThat(list(null, null).getStatus()).isEqualTo(401);
        for (Role role : new Role[] {Role.MEMBER, Role.CONTRIBUTOR, Role.MODERATOR}) {
            SignedIn caller = support.signedIn(role, AccountStatus.ACTIVE, true);
            assertThat(list(caller, null).getStatus()).as(role.name()).isEqualTo(403);
        }
        assertThat(list(admin, null).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("NFR-008: entries come newest first with the fields of the contract")
    void newestFirstWithTheContractFields() throws Exception {
        long target = newTargetId();
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        insert("ROLE_CHANGED", admin.userId(), target, "{\"from\":\"MEMBER\",\"to\":\"CONTRIBUTOR\"}", now.minusSeconds(30));
        insert("ACCOUNT_SUSPENDED", admin.userId(), target, "{\"reason\":\"Spam\"}", now.minusSeconds(10));

        JsonNode content = page("targetId=" + target).get("content");

        assertThat(content).hasSize(2);
        assertThat(content.get(0).get("action").asText()).isEqualTo("ACCOUNT_SUSPENDED");
        assertThat(content.get(1).get("action").asText()).isEqualTo("ROLE_CHANGED");
        JsonNode entry = content.get(1);
        assertThat(entry.get("actorId").asLong()).isEqualTo(admin.userId());
        assertThat(entry.get("targetType").asText()).isEqualTo("USER");
        assertThat(entry.get("targetId").asLong()).isEqualTo(target);
        assertThat(entry.get("details").get("to").asText()).isEqualTo("CONTRIBUTOR");
        assertThat(Instant.parse(entry.get("createdAt").asText())).isEqualTo(now.minusSeconds(30));
    }

    @Test
    @DisplayName("NFR-008: every action of spec 010 can be listed and filtered")
    void everyActionOf010Appears() throws Exception {
        long target = newTargetId();
        for (String action : ACTIONS_OF_010) {
            insert(action, null, target, null, Instant.now());
        }

        List<String> seen = new ArrayList<>();
        page("targetId=" + target + "&size=100").get("content").forEach(e -> seen.add(e.get("action").asText()));
        assertThat(seen).containsExactlyInAnyOrderElementsOf(ACTIONS_OF_010);
        for (String action : ACTIONS_OF_010) {
            assertThat(page("targetId=" + target + "&action=" + action).get("content")).as(action).hasSize(1);
        }
    }

    @Test
    @DisplayName("NFR-008: paging with hasNext, and from/to bound the time")
    void pagingAndTimeRange() throws Exception {
        long target = newTargetId();
        Instant base = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        for (int i = 0; i < 5; i++) {
            insert("SIGNED_OUT", null, target, null, base.plusSeconds(i * 60L));
        }

        JsonNode first = page("targetId=" + target + "&size=2&page=0");
        JsonNode last = page("targetId=" + target + "&size=2&page=2");

        assertThat(first.get("content")).hasSize(2);
        assertThat(first.get("hasNext").asBoolean()).isTrue();
        assertThat(first.get("page").asInt()).isZero();
        assertThat(first.get("size").asInt()).isEqualTo(2);
        assertThat(last.get("content")).hasSize(1);
        assertThat(last.get("hasNext").asBoolean()).isFalse();
        JsonNode range = page("targetId=" + target + "&from=" + base.plusSeconds(60) + "&to=" + base.plusSeconds(180));
        assertThat(range.get("content")).hasSize(3);
    }

    @Test
    @DisplayName("NFR-008: an entry older than 90 days is still readable")
    void oldEntriesStayReadable() throws Exception {
        long target = newTargetId();
        insert("ROLE_CHANGED", null, target, null, Instant.now().minus(200, ChronoUnit.DAYS));

        assertThat(page("targetId=" + target).get("content")).hasSize(1);
    }

    @Test
    @DisplayName("NFR-008: the page size is 1 to 100")
    void pageSizeIsBounded() throws Exception {
        assertThat(list(admin, "size=101").getStatus()).isEqualTo(400);
        assertThat(list(admin, "size=0").getStatus()).isEqualTo(400);
        assertThat(list(admin, "page=-1").getStatus()).isEqualTo(400);
        assertThat(list(admin, "size=100").getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("NFR-008: the trail is read-only, there is no endpoint that changes or deletes an entry")
    void noWriteEndpoints() throws Exception {
        long target = newTargetId();
        insert("ROLE_CHANGED", null, target, null, Instant.now());
        long id = jdbc.queryForObject("SELECT id FROM audit_logs WHERE target_id = ?", Long.class, target);

        for (var request : List.of(
                delete(URL + "/" + id), put(URL + "/" + id), patch(URL + "/" + id), post(URL), delete(URL))) {
            int status = mvc.perform(request.header("Authorization", admin.bearer())).andReturn().getResponse()
                    .getStatus();
            assertThat(status).as(request.toString()).isIn(404, 405);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE id = ?", Long.class, id)).isEqualTo(1L);
    }

    @Test
    @DisplayName("FR-025: after the account is deleted its entries remain, and nothing in them identifies the person")
    void entriesSurviveDeletionOfTheAccountAnonymized() throws Exception {
        long victim = support.createUser(Role.MEMBER, AccountStatus.ACTIVE, true);
        String email = jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, victim);
        insert("ROLE_CHANGED", admin.userId(), victim, "{\"from\":\"MEMBER\",\"to\":\"CONTRIBUTOR\"}", Instant.now());

        jdbc.update("DELETE FROM users WHERE id = ?", victim);

        JsonNode content = page("targetId=" + victim).get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).get("targetId").asLong()).isEqualTo(victim);
        assertThat(content.get(0).get("actorId").asLong()).isEqualTo(admin.userId());
        assertThat(content.toString()).doesNotContain(email).doesNotContain("displayName");
    }

    @Test
    @DisplayName("FR-025, NFR-009: sign-in entries never hold an email, a display name or dietary data")
    void signInEntriesHoldNoPersonalData() throws Exception {
        String stranger = "audit." + System.nanoTime() + "@mail.com";
        String known = AuthTestSupport.uniqueEmail();
        support.memberWithPassword(known, "abc12345");
        for (String email : List.of(stranger, known)) {
            mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                    .content("{\"email\":\"" + email + "\",\"password\":\"wrong-password1\"}"));
        }

        String all = page("action=SIGN_IN_FAILED&size=100").toString();

        assertThat(all).doesNotContain(stranger).doesNotContain(known).doesNotContain("dietType")
                .doesNotContain("householdSize").doesNotContain("Test MEMBER");
        assertThat(all).contains("emailHash");
    }
}
