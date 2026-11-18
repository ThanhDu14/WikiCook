package com.wikicook.auth.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * Keeps the Google authorization request (the {@code state}, the {@code nonce}, the PKCE verifier and the page to
 * return to) in the cookie {@code wc_oauth} instead of an {@code HttpSession}, which the application does not have
 * (research R1, R7). The value is {@code base64url(json) + "." + base64url(HMAC-SHA256)}: a changed byte, a cookie
 * made by someone else or one older than five minutes is simply "no request", so the callback is refused. The cookie
 * is {@code HttpOnly}, {@code SameSite=Lax} because Google comes back by a top-level navigation, and sent only to the
 * Google routes. It is removed the first time the callback reads it.
 *
 * <p>The page to return to is also put in the request attribute {@link #REDIRECT_ATTRIBUTE} when the request is
 * removed, because the success handler of Spring Security does not receive the authorization request.
 */
public class CookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String COOKIE_NAME = "wc_oauth";
    public static final String PATH = "/api/v1/auth/oauth2";
    /** Where the page to return to is stored in the authorization request attributes. */
    public static final String REDIRECT = "wikicook.redirect";
    /** Request attribute read by the login handlers: the return page of the request that was just removed. */
    public static final String REDIRECT_ATTRIBUTE = "wikicook.oauth.redirect";
    public static final Duration TTL = Duration.ofMinutes(5);

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String HMAC = "HmacSHA256";

    private final byte[] key;
    private final boolean secure;
    private final Clock clock;

    /** @param secret any server secret; the signing key is derived from it with a label, never used as it is */
    public CookieOAuth2AuthorizationRequestRepository(String secret, boolean secure, Clock clock) {
        this.key = hmac(secret.getBytes(StandardCharsets.UTF_8), "wikicook-oauth-cookie".getBytes(StandardCharsets.UTF_8));
        this.secure = secure;
        this.clock = clock;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Stored stored = read(request);
        return stored == null ? null : stored.toRequest();
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request,
            HttpServletResponse response) {
        if (authorizationRequest == null) {
            response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO));
            return;
        }
        Stored stored = Stored.of(authorizationRequest, clock.instant().plus(TTL).getEpochSecond());
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(write(stored));
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(payload + "." + sign(payload), TTL));
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        Stored stored = read(request);
        if (stored != null) {
            response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO));
            String redirect = stored.attributes().get(REDIRECT);
            if (redirect != null) {
                request.setAttribute(REDIRECT_ATTRIBUTE, redirect);
            }
        }
        return stored == null ? null : stored.toRequest();
    }

    private Stored read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return parse(cookie.getValue());
            }
        }
        return null;
    }

    private Stored parse(String value) {
        int dot = value == null ? -1 : value.indexOf('.');
        if (dot <= 0 || dot == value.length() - 1) {
            return null;
        }
        String payload = value.substring(0, dot);
        byte[] expected = sign(payload).getBytes(StandardCharsets.US_ASCII);
        byte[] given = value.substring(dot + 1).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, given)) {
            return null;
        }
        try {
            Stored stored = JSON.readValue(Base64.getUrlDecoder().decode(payload), Stored.class);
            return stored.expiresAt() > clock.instant().getEpochSecond() ? stored : null;
        } catch (IllegalArgumentException | java.io.IOException e) {
            return null;
        }
    }

    private String cookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(PATH)
                .maxAge(maxAge)
                .build()
                .toString();
    }

    private String sign(String payload) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(key, payload.getBytes(StandardCharsets.US_ASCII)));
    }

    private static byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(key, HMAC));
            return mac.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }

    private static byte[] write(Stored stored) {
        try {
            return JSON.writeValueAsBytes(stored);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot write the authorization request", e);
        }
    }

    /** The parts of the authorization request that are needed again at the callback; every value is a string. */
    record Stored(String authorizationUri, String clientId, String redirectUri, java.util.Set<String> scopes,
            String state, String authorizationRequestUri, Map<String, String> additionalParameters,
            Map<String, String> attributes, long expiresAt) {

        static Stored of(OAuth2AuthorizationRequest request, long expiresAt) {
            return new Stored(request.getAuthorizationUri(), request.getClientId(), request.getRedirectUri(),
                    new LinkedHashSet<>(request.getScopes()), request.getState(), request.getAuthorizationRequestUri(),
                    strings(request.getAdditionalParameters()), strings(request.getAttributes()), expiresAt);
        }

        OAuth2AuthorizationRequest toRequest() {
            return OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri(authorizationUri)
                    .clientId(clientId)
                    .redirectUri(redirectUri)
                    .scopes(scopes)
                    .state(state)
                    .authorizationRequestUri(authorizationRequestUri)
                    .additionalParameters(new LinkedHashMap<>(additionalParameters))
                    .attributes(attrs -> attrs.putAll(attributes))
                    .build();
        }

        private static Map<String, String> strings(Map<String, Object> source) {
            Map<String, String> result = new LinkedHashMap<>();
            source.forEach((name, value) -> {
                if (value != null) {
                    result.put(name, value.toString());
                }
            });
            return result;
        }
    }
}
