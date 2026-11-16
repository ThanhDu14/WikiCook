package com.wikicook.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Tells the app that a role-change notice is waiting (FR-024a, research R11). While {@code
 * users.pending_role_notice_from} is set, every response to a request with a valid bearer carries
 * {@value #HEADER}{@code : 1}, including the 403 a demoted user gets; the client then reloads {@code GET /auth/me}
 * once. The value comes from the principal that the per-request session check already loaded, so it costs no query.
 *
 * <p>Two answers never carry it: {@code GET /auth/me}, whose body already holds the notice, and
 * {@code POST /auth/role-notice/ack}, which reads the notice before it clears it; otherwise the client would be told
 * again about a notice it is showing. Runs right after the bearer authentication, and sets the header before the
 * request continues, so it is on error responses written later by the security filters or the controllers too. Not a
 * Spring bean: a Filter bean would be registered a second time by Spring Boot.
 */
public class AccountStateHeaderFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-WikiCook-Role-Notice";

    private static final String ME = "/api/v1/auth/me";
    private static final String ACK = "/api/v1/auth/role-notice/ack";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AccountAuthentication account
                && account.getPrincipal().pendingRoleNoticeFrom() != null && !excluded(request)) {
            response.setHeader(HEADER, "1");
        }
        chain.doFilter(request, response);
    }

    private static boolean excluded(HttpServletRequest request) {
        String path = request.getRequestURI();
        return ("GET".equals(request.getMethod()) && ME.equals(path))
                || ("POST".equals(request.getMethod()) && ACK.equals(path));
    }
}
