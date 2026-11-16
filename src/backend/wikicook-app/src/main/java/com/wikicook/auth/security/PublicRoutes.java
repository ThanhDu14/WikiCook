package com.wikicook.auth.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * The routes that need no sign-in (research R10). Everything else under {@code /api/**} needs a session, and
 * anything outside {@code /api/**} that is not listed here is refused. A module that adds a public route (for example
 * 060 reading a published recipe) adds it here, so nothing becomes public by accident.
 *
 * <p>Public routes come in two kinds:
 * <ul>
 * <li><b>Bearer ignored</b> ({@link #bearerIgnoredMatcher()}): sign-in, refresh, health, API docs. An expired or wrong
 *     {@code Authorization} header there must not turn the request into a 401, or a client with an expired access
 *     token could never refresh it.
 * <li><b>Optionally signed in</b>: recipe search of 020. A guest is served without a token; a valid token identifies
 *     the user (so {@code AuthSearchViewer} can apply their dietary profile, 020 R6). Not listed in the first group on
 *     purpose; an invalid token is 401 and the client refreshes once and retries, as for any other route (R12).
 * </ul>
 */
public final class PublicRoutes {

    private static final PathPatternRequestMatcher.Builder PATHS = PathPatternRequestMatcher.withDefaults();

    private static final RequestMatcher BEARER_IGNORED = new OrRequestMatcher(
            // sign-in, register, refresh, verification, password reset, Google: all of /auth except the three that
            // act on the signed-in account
            new AndRequestMatcher(
                    PATHS.matcher("/api/v1/auth/**"),
                    new NegatedRequestMatcher(new OrRequestMatcher(
                            PATHS.matcher("/api/v1/auth/me"),
                            PATHS.matcher("/api/v1/auth/role-notice/**"),
                            PATHS.matcher("/api/v1/auth/reauthenticate")))),
            PATHS.matcher("/actuator/health"),
            PATHS.matcher("/actuator/health/**"),
            PATHS.matcher("/v3/api-docs"),
            PATHS.matcher("/v3/api-docs/**"),
            PATHS.matcher("/swagger-ui.html"),
            PATHS.matcher("/swagger-ui/**"));

    private static final RequestMatcher OPTIONALLY_SIGNED_IN = new OrRequestMatcher(
            // 020: a guest searches recipes (020 acceptance 3.4); a signed-in user gets their dietary profile applied
            PATHS.matcher(HttpMethod.GET, "/api/v1/recipes/search"),
            PATHS.matcher(HttpMethod.GET, "/api/v1/recipes/search/options"),
            PATHS.matcher(HttpMethod.GET, "/api/v1/recipes/featured"));

    private static final RequestMatcher ALL = new OrRequestMatcher(BEARER_IGNORED, OPTIONALLY_SIGNED_IN);

    private PublicRoutes() {
    }

    /** Every route that may be called without signing in. */
    public static RequestMatcher matcher() {
        return ALL;
    }

    /** The public routes where an {@code Authorization} header is not even looked at. */
    public static RequestMatcher bearerIgnoredMatcher() {
        return BEARER_IGNORED;
    }
}
