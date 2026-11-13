package com.wikicook.auth.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * The routes that need no sign-in (research R10). Everything else under {@code /api/**} needs a session, and
 * anything outside {@code /api/**} that is not listed here is refused. A module that adds a public route (for example
 * 060 reading a published recipe) adds it here, so nothing becomes public by accident.
 */
public final class PublicRoutes {

    private static final PathPatternRequestMatcher.Builder PATHS = PathPatternRequestMatcher.withDefaults();

    private static final RequestMatcher MATCHER = new OrRequestMatcher(
            // sign-in, register, refresh, verification, password reset, Google: all of /auth except the three that
            // act on the signed-in account
            new AndRequestMatcher(
                    PATHS.matcher("/api/v1/auth/**"),
                    new NegatedRequestMatcher(new OrRequestMatcher(
                            PATHS.matcher("/api/v1/auth/me"),
                            PATHS.matcher("/api/v1/auth/role-notice/**"),
                            PATHS.matcher("/api/v1/auth/reauthenticate")))),
            // 020: a guest searches recipes (020 acceptance 3.4)
            PATHS.matcher(HttpMethod.GET, "/api/v1/recipes/search"),
            PATHS.matcher(HttpMethod.GET, "/api/v1/recipes/search/options"),
            PATHS.matcher(HttpMethod.GET, "/api/v1/recipes/featured"),
            PATHS.matcher("/actuator/health"),
            PATHS.matcher("/actuator/health/**"),
            PATHS.matcher("/v3/api-docs"),
            PATHS.matcher("/v3/api-docs/**"),
            PATHS.matcher("/swagger-ui.html"),
            PATHS.matcher("/swagger-ui/**"));

    private PublicRoutes() {
    }

    public static RequestMatcher matcher() {
        return MATCHER;
    }
}
