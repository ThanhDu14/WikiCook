package com.wikicook.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.AuthTestSupport.SignedIn;
import java.time.LocalDate;
import java.util.UUID;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Research R13, nothing mocked: the real security filter chain and real JWTs on the routes of 030, 031 and 040. The
 * three identity headers of the old stubs have no effect, and two users cannot read each other's data. This is the
 * proof that the impersonation hole of {@code X-User-Id} is closed, which the other ITs of those modules (that use
 * {@code HeaderCurrentUser}) deliberately do not cover.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthTestSupport.class)
class SecuredRoutesIT extends AbstractPostgresIT {

    private static final String[] OLD_IDENTITY_HEADERS = {"X-User-Id", "X-Dev-User-Id", "X-Test-User"};

    @Autowired
    MockMvc mvc;

    @Autowired
    AuthTestSupport support;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    private final String monday = LocalDate.now().plusWeeks(1).with(java.time.DayOfWeek.MONDAY).toString();

    @BeforeEach
    void setUp() {
        support.resetThrottle();
    }

    private String[] routes() {
        return new String[] {"/api/v1/meal-plans/" + monday, "/api/v1/shopping-lists/active",
                "/api/v1/ai-planner/weeks/" + monday + "/options"};
    }

    private MockHttpServletResponse as(MockHttpServletRequestBuilder request, SignedIn who, Long claimsToBe)
            throws Exception {
        if (who != null) {
            request.header("Authorization", who.bearer());
        }
        if (claimsToBe != null) {
            for (String header : OLD_IDENTITY_HEADERS) {
                request.header(header, claimsToBe);
            }
        }
        return mvc.perform(request).andReturn().getResponse();
    }

    @Test
    @DisplayName("R13: without a JWT every route is 401 UNAUTHENTICATED, whichever identity header is sent")
    void identityHeadersAloneOpenNothing() throws Exception {
        SignedIn victim = support.signedInMember();

        for (String route : routes()) {
            assertThat(as(get(route), null, null).getStatus()).as(route).isEqualTo(401);
            for (String header : OLD_IDENTITY_HEADERS) {
                MockHttpServletResponse response = mvc.perform(get(route).header(header, victim.userId()))
                        .andReturn().getResponse();
                assertThat(response.getStatus()).as("%s with %s", route, header).isEqualTo(401);
                assertThat(json.readTree(response.getContentAsString()).get("code").asText())
                        .isEqualTo("UNAUTHENTICATED");
            }
        }
    }

    @Test
    @DisplayName("R13: with a real JWT the routes answer, and a header naming another user changes nothing")
    void aRealJwtOpensTheRoutesForItsOwner() throws Exception {
        SignedIn alice = support.signedInMember();
        SignedIn bob = support.signedInMember();

        for (String route : routes()) {
            assertThat(as(get(route), alice, bob.userId()).getStatus()).as(route).isEqualTo(200);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM meal_plans WHERE user_id = ?", Long.class,
                bob.userId())).as("the header did not make Alice act as Bob").isZero();
    }

    @Test
    @DisplayName("R13: two users cannot read each other's shopping list, even naming each other in a header")
    void usersCannotReadEachOthersData() throws Exception {
        SignedIn alice = support.signedInMember();
        SignedIn bob = support.signedInMember();
        String secret = "Alice-secret-" + System.nanoTime();
        MockHttpServletResponse created = mvc.perform(post("/api/v1/shopping-lists/active/items")
                .header("Authorization", alice.bearer()).contentType("application/json")
                .content("{\"clientUuid\":\"" + UUID.randomUUID() + "\",\"customName\":\"" + secret + "\"}"))
                .andReturn().getResponse();
        assertThat(created.getStatus()).isEqualTo(201);

        MockHttpServletResponse alices = as(get("/api/v1/shopping-lists/active"), alice, null);
        MockHttpServletResponse bobs = as(get("/api/v1/shopping-lists/active"), bob, null);
        MockHttpServletResponse bobClaimingToBeAlice = as(get("/api/v1/shopping-lists/active"), bob, alice.userId());

        assertThat(alices.getContentAsString()).contains(secret);
        assertThat(bobs.getContentAsString()).doesNotContain(secret);
        assertThat(bobClaimingToBeAlice.getContentAsString()).doesNotContain(secret);
        JsonNode bobsList = json.readTree(bobs.getContentAsString());
        JsonNode alicesList = json.readTree(alices.getContentAsString());
        assertThat(bobsList.get("id").asLong()).isNotEqualTo(alicesList.get("id").asLong());
    }

    @Test
    @DisplayName("R13: a user's list cannot be fetched by id by someone else")
    void aListCannotBeFetchedByAnotherUserById() throws Exception {
        SignedIn alice = support.signedInMember();
        SignedIn bob = support.signedInMember();
        long aliceListId = json.readTree(as(get("/api/v1/shopping-lists/active"), alice, null).getContentAsString())
                .get("id").asLong();

        MockHttpServletResponse response = as(get("/api/v1/shopping-lists/" + aliceListId), bob, null);

        assertThat(response.getStatus()).isIn(403, 404);
        assertThat(response.getContentAsString()).doesNotContain("Alice");
    }
}
