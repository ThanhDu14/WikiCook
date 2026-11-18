package com.wikicook.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.support.MutableClock;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * Spec 010 T139: the authorization request of Google in the cookie {@code wc_oauth} (research R7): saved and read
 * back with {@code state}, {@code nonce}, PKCE verifier and return page; signed (a change is refused), short-lived and
 * removed on first use; {@code HttpOnly}, {@code SameSite=Lax}, only for the Google routes.
 */
class CookieOAuth2AuthorizationRequestRepositoryTest {

    private final MutableClock clock = new MutableClock();
    private CookieOAuth2AuthorizationRequestRepository repository;

    @BeforeEach
    void setUp() {
        clock.reset();
        repository = new CookieOAuth2AuthorizationRequestRepository("a-server-secret-of-at-least-32-bytes!!", true, clock);
    }

    private static OAuth2AuthorizationRequest request() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("client-1")
                .redirectUri("https://wikicook.example/api/v1/auth/oauth2/callback/google")
                .scopes(java.util.Set.of("openid", "email"))
                .state("state-123")
                .additionalParameters(java.util.Map.of("nonce", "hashed-nonce", "code_challenge", "challenge", "code_challenge_method", "S256"))
                .attributes(attributes -> {
                    attributes.put("registration_id", "google");
                    attributes.put("nonce", "raw-nonce");
                    attributes.put("code_verifier", "verifier-xyz");
                    attributes.put(CookieOAuth2AuthorizationRequestRepository.REDIRECT, "/meal-planner?week=2");
                })
                .authorizationRequestUri("https://accounts.google.com/o/oauth2/v2/auth?state=state-123")
                .build();
    }

    private Cookie saved() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        repository.saveAuthorizationRequest(request(), new MockHttpServletRequest(), response);
        String header = response.getHeader("Set-Cookie");
        assertThat(header).isNotNull();
        String value = header.substring("wc_oauth=".length(), header.indexOf(';'));
        return new Cookie("wc_oauth", value);
    }

    private static MockHttpServletRequest with(Cookie cookie) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (cookie != null) {
            request.setCookies(cookie);
        }
        return request;
    }

    @Test
    @DisplayName("what was saved comes back: state, nonce, PKCE verifier, redirect URI and the return page")
    void roundTrip() {
        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(with(saved()));

        assertThat(loaded).isNotNull();
        assertThat(loaded.getState()).isEqualTo("state-123");
        assertThat(loaded.getClientId()).isEqualTo("client-1");
        assertThat(loaded.getRedirectUri()).isEqualTo("https://wikicook.example/api/v1/auth/oauth2/callback/google");
        assertThat(loaded.getScopes()).containsExactlyInAnyOrder("openid", "email");
        assertThat(loaded.<String>getAttribute("nonce")).isEqualTo("raw-nonce");
        assertThat(loaded.<String>getAttribute("code_verifier")).isEqualTo("verifier-xyz");
        assertThat(loaded.<String>getAttribute("registration_id")).isEqualTo("google");
        assertThat(loaded.<String>getAttribute(CookieOAuth2AuthorizationRequestRepository.REDIRECT)).isEqualTo("/meal-planner?week=2");
        assertThat(loaded.getAdditionalParameters()).containsEntry("code_challenge_method", "S256").containsEntry("nonce", "hashed-nonce");
        assertThat(loaded.getAuthorizationRequestUri()).isEqualTo("https://accounts.google.com/o/oauth2/v2/auth?state=state-123");
    }

    @Test
    @DisplayName("the cookie is HttpOnly, SameSite=Lax, Secure, five minutes, and only sent to the Google routes")
    void cookieAttributes() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        repository.saveAuthorizationRequest(request(), new MockHttpServletRequest(), response);

        assertThat(response.getHeader("Set-Cookie")).startsWith("wc_oauth=").contains("HttpOnly").contains("Secure")
                .contains("SameSite=Lax").contains("Max-Age=300").contains("Path=/api/v1/auth/oauth2");
    }

    @Test
    @DisplayName("without Secure only when the configuration says so (plain http on localhost)")
    void notSecureOnRequest() {
        repository = new CookieOAuth2AuthorizationRequestRepository("a-server-secret-of-at-least-32-bytes!!", false, clock);
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(request(), new MockHttpServletRequest(), response);

        assertThat(response.getHeader("Set-Cookie")).doesNotContain("Secure");
    }

    @Test
    @DisplayName("a changed payload fails the HMAC: no request")
    void changedPayload() {
        Cookie cookie = saved();
        String value = cookie.getValue();
        int dot = value.indexOf('.');
        char first = value.charAt(0);
        String changed = (first == 'A' ? 'B' : 'A') + value.substring(1, dot) + value.substring(dot);

        assertThat(repository.loadAuthorizationRequest(with(new Cookie("wc_oauth", changed)))).isNull();
    }

    @Test
    @DisplayName("a changed signature, a missing one and garbage are refused")
    void changedSignatureAndGarbage() {
        String value = saved().getValue();
        String changedSignature = value.substring(0, value.length() - 2) + (value.endsWith("AA") ? "BB" : "AA");

        assertThat(repository.loadAuthorizationRequest(with(new Cookie("wc_oauth", changedSignature)))).isNull();
        assertThat(repository.loadAuthorizationRequest(with(new Cookie("wc_oauth", value.substring(0, value.indexOf('.')))))).isNull();
        assertThat(repository.loadAuthorizationRequest(with(new Cookie("wc_oauth", "garbage")))).isNull();
        assertThat(repository.loadAuthorizationRequest(with(new Cookie("wc_oauth", ".")))).isNull();
        assertThat(repository.loadAuthorizationRequest(with(null))).isNull();
    }

    @Test
    @DisplayName("a cookie signed with another secret is refused")
    void anotherSecret() {
        Cookie cookie = saved();
        CookieOAuth2AuthorizationRequestRepository other = new CookieOAuth2AuthorizationRequestRepository(
                "another-secret-of-at-least-32-bytes!!!!!", true, clock);

        assertThat(other.loadAuthorizationRequest(with(cookie))).isNull();
    }

    @Test
    @DisplayName("a request older than five minutes is gone, even if the browser kept the cookie")
    void expires() {
        Cookie cookie = saved();

        clock.advance(Duration.ofMinutes(4));
        assertThat(repository.loadAuthorizationRequest(with(cookie))).isNotNull();
        clock.advance(Duration.ofMinutes(2));
        assertThat(repository.loadAuthorizationRequest(with(cookie))).isNull();
    }

    @Test
    @DisplayName("removing returns the request, clears the cookie and leaves the return page for the login handlers")
    void removeOnFirstUse() {
        MockHttpServletRequest request = with(saved());
        MockHttpServletResponse response = new MockHttpServletResponse();

        OAuth2AuthorizationRequest removed = repository.removeAuthorizationRequest(request, response);

        assertThat(removed).isNotNull();
        assertThat(removed.getState()).isEqualTo("state-123");
        assertThat(response.getHeader("Set-Cookie")).startsWith("wc_oauth=;").contains("Max-Age=0");
        assertThat(request.getAttribute(CookieOAuth2AuthorizationRequestRepository.REDIRECT_ATTRIBUTE)).isEqualTo("/meal-planner?week=2");
    }

    @Test
    @DisplayName("removing a request that is not there changes nothing and sets no cookie")
    void removeNothing() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(repository.removeAuthorizationRequest(with(null), response)).isNull();
        assertThat(response.getHeaders("Set-Cookie")).isEmpty();
    }

    @Test
    @DisplayName("saving null clears the cookie")
    void saveNull() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(null, new MockHttpServletRequest(), response);

        assertThat(response.getHeader("Set-Cookie")).startsWith("wc_oauth=;").contains("Max-Age=0");
    }
}
