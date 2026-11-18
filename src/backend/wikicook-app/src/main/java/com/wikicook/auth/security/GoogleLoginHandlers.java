package com.wikicook.auth.security;

import com.wikicook.auth.config.AuthProperties;
import com.wikicook.auth.service.AuthException;
import com.wikicook.auth.service.GoogleSignInService;
import com.wikicook.auth.service.GoogleSignInService.GoogleIdentity;
import com.wikicook.auth.service.GoogleSignInService.Outcome;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Ends the Google redirect flow by sending the browser back to the SPA (spec 010 US5, research R7). Google has been
 * proven by now: Spring Security exchanged the code (with the PKCE verifier) and checked the ID token (signature,
 * issuer, audience, expiry, nonce). Nothing here trusts the browser: {@link GoogleSignInService} decides, and the
 * places the browser is sent to are fixed, except the page to return to, which is checked again.
 *
 * <ul>
 * <li>signed in: the {@code wc_rt} cookie and {@code /auth/callback?redirect=…}, where the SPA trades the cookie for
 *     an access token;
 * <li>refused: {@code /login?error=google_no_verified_email}, {@code registration_refused}, {@code google_cancelled}
 *     (the user said no at Google) or {@code google_unavailable} (anything else that went wrong: nothing was created);
 * <li>suspended or banned: {@code /account-status#code=…&reason=…&endsAt=…}; a fragment, so the reason does not reach
 *     a proxy log or a {@code Referer}.
 * </ul>
 */
public class GoogleLoginHandlers implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(GoogleLoginHandlers.class);
    private static final String CANCELLED = "access_denied";

    private final GoogleSignInService service;
    private final RefreshCookie cookie;
    private final String baseUrl;

    public GoogleLoginHandlers(GoogleSignInService service, RefreshCookie cookie, AuthProperties properties) {
        this.service = service;
        this.cookie = cookie;
        String base = properties.publicBaseUrl();
        this.baseUrl = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) {
        try {
            if (!(authentication.getPrincipal() instanceof OidcUser user)) {
                redirect(response, loginError("google_unavailable"));
                return;
            }
            String back = SafeRedirects.sanitize((String) request.getAttribute(
                    CookieOAuth2AuthorizationRequestRepository.REDIRECT_ATTRIBUTE));
            GoogleIdentity identity = new GoogleIdentity(user.getSubject(), user.getEmail(), emailVerified(user),
                    user.getFullName());
            Outcome outcome = service.signIn(identity, request.getHeader(HttpHeaders.USER_AGENT));
            switch (outcome.kind()) {
                case SIGNED_IN -> {
                    response.addHeader(HttpHeaders.SET_COOKIE, cookie.issue(outcome.session().refreshToken(), false,
                            outcome.session().session().getExpiresAt()));
                    redirect(response, "/auth/callback" + (back == null ? "" : "?redirect=" + encode(back)));
                }
                case NO_VERIFIED_EMAIL -> redirect(response, loginError("google_no_verified_email"));
                case REGISTRATION_REFUSED -> redirect(response, loginError("registration_refused"));
                case RESTRICTED -> redirect(response, accountStatus(outcome.restriction()));
                case IDENTITY_CONFLICT -> redirect(response, loginError("google_unavailable"));
            }
        } catch (RuntimeException e) {
            // all or nothing: the takeover is rolled back, no session exists. Only the type is logged, never a value
            log.warn("Google sign-in not completed: {}", rootCause(e));
            log.debug("Google sign-in failure", e);
            redirect(response, loginError("google_unavailable"));
        }
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) {
        String code = exception instanceof OAuth2AuthenticationException oauth ? oauth.getError().getErrorCode() : "";
        if (!CANCELLED.equals(code)) {
            log.warn("Google sign-in refused: {}", code.isEmpty() ? exception.getClass().getSimpleName() : code);
        }
        redirect(response, loginError(CANCELLED.equals(code) ? "google_cancelled" : "google_unavailable"));
    }

    /** Google sends a boolean; the claim may arrive as the text "true" too. Anything else is not verified (FR-018). */
    private static boolean emailVerified(OidcUser user) {
        Object claim = user.getClaims().get("email_verified");
        return claim instanceof Boolean verified ? verified : claim instanceof String text && "true".equalsIgnoreCase(text);
    }

    private static String accountStatus(AuthException restriction) {
        StringBuilder fragment = new StringBuilder("code=").append(restriction.code().name());
        if (restriction.properties().get("reason") instanceof String reason) {
            fragment.append("&reason=").append(encode(reason));
        }
        if (restriction.properties().get("endsAt") instanceof Instant endsAt) {
            fragment.append("&endsAt=").append(encode(endsAt.toString()));
        }
        return "/account-status#" + fragment;
    }

    private static String loginError(String code) {
        return "/login?error=" + code;
    }

    private void redirect(HttpServletResponse response, String path) {
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader(HttpHeaders.LOCATION, baseUrl + path);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String rootCause(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getClass().getName();
    }
}
