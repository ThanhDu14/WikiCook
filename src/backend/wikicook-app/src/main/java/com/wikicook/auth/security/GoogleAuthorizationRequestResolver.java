package com.wikicook.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * Builds the redirect to Google for {@code GET /api/v1/auth/oauth2/google} (research R7): Authorization Code with PKCE
 * ({@code code_challenge_method=S256}), a {@code state} and a {@code nonce}, plus the page to return to, which is
 * accepted only when it is a path inside WikiCook (FR-011).
 *
 * <p>{@code intent=reauth} (re-authentication of a Google-only account, FR-022a) is not built: it needs proof from the
 * real Google that it returns {@code auth_time} (research R7, open point 40, task T149b). The resolver then answers
 * "not mine" and {@code GoogleStartController} sends the user back, as a cancelled re-authentication. It must never fall
 * through to a plain sign-in, which would create a session and prove nothing.
 */
class GoogleAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    GoogleAuthorizationRequestResolver(ClientRegistrationRepository registrations, String baseUri) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(registrations, baseUri);
        this.delegate.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return isReauth(request) ? null : withRedirect(delegate.resolve(request), request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return isReauth(request) ? null : withRedirect(delegate.resolve(request, clientRegistrationId), request);
    }

    static boolean isReauth(HttpServletRequest request) {
        return "reauth".equalsIgnoreCase(request.getParameter("intent"));
    }

    private static OAuth2AuthorizationRequest withRedirect(OAuth2AuthorizationRequest resolved, HttpServletRequest request) {
        if (resolved == null) {
            return null;
        }
        String back = SafeRedirects.sanitize(request.getParameter("redirect"));
        if (back == null) {
            return resolved;
        }
        return OAuth2AuthorizationRequest.from(resolved)
                .attributes(attributes -> attributes.put(CookieOAuth2AuthorizationRequestRepository.REDIRECT, back))
                .build();
    }
}
