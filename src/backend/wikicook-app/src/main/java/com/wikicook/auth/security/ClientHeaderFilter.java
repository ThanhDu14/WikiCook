package com.wikicook.auth.security;

import com.wikicook.auth.service.AuthException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * The two endpoints that read the refresh cookie, {@code /auth/refresh} and {@code /auth/logout}, need the custom
 * header {@code X-WikiCook-Client: web}; without it the answer is 403 {@code INVALID_CLIENT} (research R2). With
 * {@code SameSite=Strict} and no CORS this is the whole CSRF defence, because a cross-site form cannot set a custom
 * header. Not a Spring bean on purpose: a Filter bean would be registered a second time by Spring Boot.
 */
public class ClientHeaderFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-WikiCook-Client";
    public static final String VALUE = "web";

    private static final Set<String> COOKIE_ENDPOINTS = Set.of("/api/v1/auth/refresh", "/api/v1/auth/logout");

    private final ProblemResponseWriter problems;

    public ClientHeaderFilter(ProblemResponseWriter problems) {
        this.problems = problems;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !COOKIE_ENDPOINTS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (VALUE.equals(request.getHeader(HEADER))) {
            chain.doFilter(request, response);
        } else {
            problems.write(response, AuthException.invalidClient());
        }
    }
}
