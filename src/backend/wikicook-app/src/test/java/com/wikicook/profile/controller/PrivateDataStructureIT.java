package com.wikicook.profile.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wikicook.profile.support.ProfileFixtures;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.HeaderCurrentUser;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * T058 (US2-7, structure only; the full US2-7 with real JWTs is T112): the three levels of research R3 are held by
 * the shape of the API. Every {@code /me/**} route belongs to the caller, so none has a user id in its URL, a query or
 * a body; the only routes of this module that name another person are the public, read-only ones.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({HeaderCurrentUser.class, AuthTestSupport.class})
class PrivateDataStructureIT extends AbstractPostgresIT {

    private static final Pattern ID_LIKE = Pattern.compile("(?i)(user|owner|account|member|profile)?_?id");

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AuthTestSupport support;

    private record Route(String method, String path, HandlerMethod handler) {
    }

    private List<Route> routesOfThisModule() {
        List<Route> routes = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : mappings.getHandlerMethods().entrySet()) {
            if (!entry.getValue().getBeanType().getName().startsWith("com.wikicook.profile.controller")) {
                continue;
            }
            Set<String> paths = entry.getKey().getPathPatternsCondition().getPatternValues();
            for (String path : paths) {
                for (var method : entry.getKey().getMethodsCondition().getMethods()) {
                    routes.add(new Route(method.name(), path, entry.getValue()));
                }
            }
        }
        return routes;
    }

    @Test
    @DisplayName("US2-7: the module has routes, and every one of them is under /api/v1/me, /profiles, /media or /ingredients")
    void everyRouteIsKnown() {
        List<Route> routes = routesOfThisModule();

        assertThat(routes).isNotEmpty();
        assertThat(routes).allSatisfy(route -> assertThat(route.path()).matches("/api/v1/(me|profiles|media|ingredients)(/.*)?"));
    }

    @Test
    @DisplayName("FR-006, FR-018: no /me route has a path variable, and no route of the module has a user id in a query or a body")
    void meRoutesHaveNoUserIdAnywhere() {
        for (Route route : routesOfThisModule()) {
            if (route.path().startsWith("/api/v1/me/") || route.path().equals("/api/v1/me")) {
                assertThat(route.path()).as("%s %s", route.method(), route.path()).doesNotContain("{");
            }
            for (Parameter parameter : route.handler().getMethod().getParameters()) {
                RequestParam param = parameter.getAnnotation(RequestParam.class);
                String name = param != null && !param.name().isEmpty() ? param.name() : parameter.getName();
                if (param != null) {
                    assertThat(name).as("query parameter of %s %s", route.method(), route.path()).doesNotMatch(ID_LIKE);
                }
                assertNoIdField(parameter.getType(), route);
            }
        }
    }

    private void assertNoIdField(Class<?> type, Route route) {
        if (type.isRecord()) {
            for (var component : type.getRecordComponents()) {
                assertThat(component.getName()).as("body field of %s %s", route.method(), route.path()).doesNotMatch(ID_LIKE);
            }
        }
    }

    @Test
    @DisplayName("FR-005, FR-006: only read-only public routes name another person, and only by id of the page owner")
    void otherPeopleAreOnlyReachedThroughPublicReads() {
        for (Route route : routesOfThisModule()) {
            if (!route.path().contains("{")) {
                continue;
            }
            assertThat(route.method()).as("%s %s names a resource", route.method(), route.path()).isEqualTo("GET");
            assertThat(route.path()).as("a route with a path variable").matches("/api/v1/(profiles/\\{id(:[^}]*)?}(/recipes)?|media/avatars/\\{file(:[^}]*)?})");
            for (Parameter parameter : route.handler().getMethod().getParameters()) {
                PathVariable variable = parameter.getAnnotation(PathVariable.class);
                if (variable != null) {
                    assertThat(parameter.getType()).isIn(Long.class, long.class, String.class);
                }
            }
        }
    }

    @Test
    @DisplayName("FR-018: no write route of the module takes the target person from the request")
    void writeRoutesAreAllMe() {
        for (Route route : routesOfThisModule()) {
            if (!route.method().equals("GET")) {
                assertThat(route.path()).as("%s %s", route.method(), route.path()).startsWith("/api/v1/me/");
            }
        }
    }

    @Test
    @DisplayName("US2-7: a second user, with the same routes, sees only their own profile and their own dietary profile")
    void secondUserSeesOnlyTheirOwnData() throws Exception {
        ProfileFixtures fixtures = new ProfileFixtures(jdbc);
        long first = support.member();
        long second = support.member();
        jdbc.update("UPDATE users SET display_name = 'Tên bí mật của người thứ nhất', bio = 'Bio bí mật' WHERE id = ?", first);
        long secret = fixtures.proposedIngredient("Nguyên liệu bí mật " + System.nanoTime());
        mvc.perform(put("/api/v1/me/dietary-profile").header(HeaderCurrentUser.HEADER, first).contentType(MediaType.APPLICATION_JSON)
                .content(ProfileFixtures.body(Set.of(fixtures.allergen("PEANUT")), Set.of(), "VEGAN", Set.of(), 17, 137, 0)))
                .andExpect(status().isOk());
        jdbc.update("INSERT INTO user_excluded_ingredients (user_id, ingredient_id) VALUES (?, ?)", first, secret);

        String profile = mvc.perform(get("/api/v1/me/profile").header(HeaderCurrentUser.HEADER, second)).andReturn().getResponse()
                .getContentAsString();
        String dietary = mvc.perform(get("/api/v1/me/dietary-profile").header(HeaderCurrentUser.HEADER, second)).andReturn()
                .getResponse().getContentAsString();

        assertThat(profile).doesNotContain("Tên bí mật").doesNotContain("Bio bí mật");
        assertThat(profile).doesNotContain(jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, first));
        assertThat(dietary).contains("\"empty\":true").doesNotContain("PEANUT").doesNotContain("Nguyên liệu bí mật")
                .doesNotContain("\"householdSize\":17").doesNotContain("\"maxCookTimeMinutes\":137");
        mvc.perform(get("/api/v1/me/dietary-profile").header(HeaderCurrentUser.HEADER, first)).andExpect(status().isOk());
    }

    @SuppressWarnings("unused")
    private static Method unused;
}
