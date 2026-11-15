package com.wikicook.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.auth.security.PublicRoutes;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.MatrixProbeController;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * FR-021, research R10: walks every mapping the application has and checks it without a token. Every {@code /api/v1}
 * route that is not declared in {@link PublicRoutes} must answer 401, so a route a new module adds is checked by this
 * test without anyone having to remember it; the declared public routes must not answer 401.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({AuthTestSupport.class, MatrixProbeController.class})
class RouteProtectionIT extends AbstractPostgresIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    private record Route(HttpMethod method, String url) {
        @Override
        public String toString() {
            return method + " " + url;
        }
    }

    private List<Route> routes() {
        Set<Route> routes = new LinkedHashSet<>();
        for (RequestMappingInfo info : mappings.getHandlerMethods().keySet()) {
            if (info.getPathPatternsCondition() == null) {
                continue;
            }
            for (String pattern : info.getPathPatternsCondition().getPatternValues()) {
                if (!pattern.startsWith("/api/v1/")) {
                    continue;
                }
                String url = pattern.replaceAll("\\{[^}]+}", "1");
                Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
                if (methods.isEmpty()) {
                    routes.add(new Route(HttpMethod.GET, url));
                }
                methods.forEach(method -> routes.add(new Route(HttpMethod.valueOf(method.name()), url)));
            }
        }
        return new ArrayList<>(routes);
    }

    private static boolean isPublic(Route route) {
        return PublicRoutes.matcher().matches(new MockHttpServletRequest(route.method().name(), route.url()));
    }

    @Test
    @DisplayName("FR-021: the walk finds the routes of every module, so the check below is not vacuous")
    void theWalkSeesTheRoutesOfEveryModule() {
        List<String> urls = routes().stream().map(Route::url).toList();

        assertThat(urls).anyMatch(u -> u.startsWith("/api/v1/auth/"))
                .anyMatch(u -> u.startsWith("/api/v1/meal-plans/"))
                .anyMatch(u -> u.startsWith("/api/v1/shopping-lists"))
                .anyMatch(u -> u.startsWith("/api/v1/ai-planner/"))
                .anyMatch(u -> u.startsWith("/api/v1/recipes/search"))
                .anyMatch(u -> u.startsWith("/api/v1/admin/"))
                .anyMatch(u -> u.startsWith("/api/v1/probe/"));
        assertThat(routes().size()).isGreaterThan(30);
    }

    @Test
    @DisplayName("FR-021: every /api/v1 route outside PublicRoutes is 401 without a token")
    void everyNonPublicRouteIs401WithoutAToken() throws Exception {
        List<String> failures = new ArrayList<>();
        for (Route route : routes()) {
            if (isPublic(route)) {
                continue;
            }
            int status = mvc.perform(MockMvcRequestBuilders.request(route.method(), route.url())).andReturn()
                    .getResponse().getStatus();
            if (status != 401) {
                failures.add(route + " answered " + status);
            }
        }

        assertThat(failures).as("routes that are open by accident").isEmpty();
    }

    @Test
    @DisplayName("FR-021, 020 AS 3.4: the declared public routes, recipe search included, are not 401 without a token")
    void publicRoutesAreNotUnauthorized() throws Exception {
        List<String> failures = new ArrayList<>();
        int checked = 0;
        for (Route route : routes()) {
            if (!isPublic(route)) {
                continue;
            }
            checked++;
            int status = mvc.perform(MockMvcRequestBuilders.request(route.method(), route.url())).andReturn()
                    .getResponse().getStatus();
            if (status == 401) {
                failures.add(route.toString());
            }
        }

        assertThat(checked).isGreaterThanOrEqualTo(8);
        assertThat(failures).isEmpty();
    }

    @Test
    @DisplayName("R10: the three routes of 020 and the auth entry points are exactly the public ones of the application")
    void thePublicSetIsWhatIsDeclared() {
        List<String> publicRoutes = routes().stream().filter(RouteProtectionIT::isPublic).map(Route::toString)
                .toList();

        assertThat(publicRoutes).contains("GET /api/v1/recipes/search", "GET /api/v1/recipes/search/options",
                "GET /api/v1/recipes/featured", "POST /api/v1/auth/login", "POST /api/v1/auth/register",
                "POST /api/v1/auth/refresh", "POST /api/v1/auth/logout", "POST /api/v1/auth/verification/confirm",
                "POST /api/v1/auth/verification/resend");
        assertThat(publicRoutes).doesNotContain("GET /api/v1/auth/me", "POST /api/v1/auth/reauthenticate",
                "POST /api/v1/auth/role-notice/ack");
        assertThat(publicRoutes).noneMatch(r -> r.contains("/admin/") || r.contains("/probe/"));
    }
}
