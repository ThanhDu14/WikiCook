package com.wikicook.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.AuthTestSupport.SignedIn;
import com.wikicook.support.MatrixProbeController;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * FR-021, SC-004, US3-1, US3-2, US3-3: the nine rows of the permission matrix against the five actors (Guest, Member,
 * Contributor, Moderator, Administrator) = 45 cases, plus an unverified Member for FR-006. Every case calls the API
 * directly with a real JWT over HTTP, never through the UI. The modules that do not exist yet (070 reviews, 060 recipes,
 * 100 moderation) are stood in for by {@code MatrixProbeController}, which lives outside the {@code auth} package so
 * that a missing permission travels the same way as in another module (research R15).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({AuthTestSupport.class, MatrixProbeController.class})
class PermissionMatrixIT extends AbstractPostgresIT {

    private static final int OK = 200;

    enum Actor { GUEST, MEMBER, CONTRIBUTOR, MODERATOR, ADMINISTRATOR, UNVERIFIED_MEMBER }

    /** One matrix row: the request, and for each actor the expected status and, when refused, the code. */
    record Row(String name, HttpMethod method, Function<Long, String> path, String body, Object[][] expected) {

        @Override
        public String toString() {
            return name;
        }
    }

    private static Object[] ok() {
        return new Object[] {OK, null};
    }

    private static Object[] unauthenticated() {
        return new Object[] {401, "UNAUTHENTICATED"};
    }

    private static Object[] denied() {
        return new Object[] {403, "ACCESS_DENIED"};
    }

    private static Object[] unverified() {
        return new Object[] {403, "EMAIL_NOT_VERIFIED"};
    }

    private static final String FUTURE = Instant.now().plus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS)
            .toString();

    /** Order of each {@code expected}: GUEST, MEMBER, CONTRIBUTOR, MODERATOR, ADMINISTRATOR, UNVERIFIED_MEMBER. */
    static final Row[] ROWS = {
            new Row("1 browse and read published recipes", HttpMethod.GET, t -> "/api/v1/recipes/search", null,
                    new Object[][] {ok(), ok(), ok(), ok(), ok(), ok()}),
            new Row("2 bookmark, meal plan, shopping list", HttpMethod.GET, t -> "/api/v1/shopping-lists/active", null,
                    new Object[][] {unauthenticated(), ok(), ok(), ok(), ok(), ok()}),
            new Row("3 review, rate, post cooksnaps (verified email)", HttpMethod.GET, t -> "/api/v1/probe/review",
                    null, new Object[][] {unauthenticated(), ok(), ok(), ok(), ok(), unverified()}),
            new Row("4 submit recipes (verified email)", HttpMethod.GET, t -> "/api/v1/probe/submit", null,
                    new Object[][] {unauthenticated(), ok(), ok(), ok(), ok(), unverified()}),
            new Row("5 verified Contributor badge", HttpMethod.GET, t -> "/api/v1/probe/badge", null,
                    new Object[][] {unauthenticated(), ok(), ok(), ok(), ok(), ok()}),
            new Row("6 moderate recipes, reviews, reports", HttpMethod.GET, t -> "/api/v1/probe/moderate", null,
                    new Object[][] {unauthenticated(), denied(), denied(), ok(), ok(), denied()}),
            new Row("7 manage categories and taxonomies", HttpMethod.GET, t -> "/api/v1/probe/taxonomies", null,
                    new Object[][] {unauthenticated(), denied(), denied(), denied(), ok(), denied()}),
            new Row("8 suspend users", HttpMethod.POST, t -> "/api/v1/admin/users/" + t + "/suspension",
                    "{\"reason\":\"Spam\",\"endsAt\":\"" + FUTURE + "\"}",
                    new Object[][] {unauthenticated(), denied(), denied(), ok(), ok(), denied()}),
            new Row("9 change roles, ban users, lift suspensions or bans", HttpMethod.PATCH,
                    t -> "/api/v1/admin/users/" + t + "/role", "{\"role\":\"CONTRIBUTOR\"}",
                    new Object[][] {unauthenticated(), denied(), denied(), denied(), ok(), denied()}),
    };

    @Autowired
    TestRestTemplate http;

    @Autowired
    AuthTestSupport support;

    @Autowired
    ObjectMapper json;

    @BeforeEach
    void setUp() {
        support.resetThrottle();
    }

    static Stream<Arguments> cases() {
        return Stream.of(ROWS).flatMap(row -> Stream.of(Actor.values()).map(actor -> Arguments.of(row, actor)));
    }

    private SignedIn signedIn(Actor actor) {
        return switch (actor) {
            case GUEST -> null;
            case MEMBER -> support.signedIn(Role.MEMBER, AccountStatus.ACTIVE, true);
            case CONTRIBUTOR -> support.signedIn(Role.CONTRIBUTOR, AccountStatus.ACTIVE, true);
            case MODERATOR -> support.signedIn(Role.MODERATOR, AccountStatus.ACTIVE, true);
            case ADMINISTRATOR -> support.signedIn(Role.ADMINISTRATOR, AccountStatus.ACTIVE, true);
            case UNVERIFIED_MEMBER -> support.signedIn(Role.MEMBER, AccountStatus.ACTIVE, false);
        };
    }

    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("cases")
    void everyRowAndActorGetsTheExpectedAnswer(Row row, Actor actor) throws Exception {
        SignedIn caller = signedIn(actor);
        long target = support.member(); // the account the admin rows act on
        HttpHeaders headers = new HttpHeaders();
        if (caller != null) {
            headers.set("Authorization", caller.bearer());
        }
        if (row.body() != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        ResponseEntity<String> response = http.exchange(row.path().apply(target), row.method(),
                new HttpEntity<>(row.body(), headers), String.class);

        Object[] expected = row.expected()[actor.ordinal()];
        assertThat(response.getStatusCode().value()).as("%s as %s: %s", row, actor, response.getBody())
                .isEqualTo(expected[0]);
        if (expected[1] != null) {
            JsonNode body = json.readTree(response.getBody());
            assertThat(body.get("code").asText()).as("%s as %s", row, actor).isEqualTo(expected[1]);
        }
    }

    @ParameterizedTest(name = "badge for {0}")
    @MethodSource("badgeHolders")
    void theContributorBadgeIsDataNotARight(Role role, boolean hasBadge) throws Exception {
        SignedIn caller = support.signedIn(role, AccountStatus.ACTIVE, true);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", caller.bearer());

        ResponseEntity<String> response = http.exchange("/api/v1/probe/badge", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(json.readTree(response.getBody()).get("verifiedContributor").asBoolean()).isEqualTo(hasBadge);
    }

    static Stream<Arguments> badgeHolders() {
        return Stream.of(Arguments.of(Role.MEMBER, false), Arguments.of(Role.CONTRIBUTOR, true),
                Arguments.of(Role.MODERATOR, false), Arguments.of(Role.ADMINISTRATOR, false));
    }
}
