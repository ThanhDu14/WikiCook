package com.wikicook.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

/**
 * Ignores the {@code Authorization} header on the public routes of {@link PublicRoutes#bearerIgnoredMatcher()}
 * (research R10); recipe search stays optionally authenticated so a signed-in user is recognized. Without it a bearer that is expired or
 * wrong would make {@code /auth/refresh} and {@code /auth/login} answer 401, and a client could never get a new token
 * after its access token ran out.
 */
public class PublicRouteBearerTokenResolver implements BearerTokenResolver {

    private final BearerTokenResolver delegate = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        return PublicRoutes.bearerIgnoredMatcher().matches(request) ? null : delegate.resolve(request);
    }
}
