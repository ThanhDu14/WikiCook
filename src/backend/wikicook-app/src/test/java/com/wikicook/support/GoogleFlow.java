package com.wikicook.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import jakarta.servlet.http.Cookie;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * Walks the redirect flow of Google sign-in the way a browser would, without a browser: {@link #start} is the click on
 * "Tiếp tục với Google" and returns what the redirect to Google carried; {@link #callback} is Google sending the user
 * back. The cookie {@code wc_oauth} travels between the two, as in a browser.
 */
public final class GoogleFlow {

    private GoogleFlow() {
    }

    /** The redirect to Google and the cookie that came with it. */
    public record Started(MockHttpServletResponse response, MultiValueMap<String, String> query, Cookie cookie) {

        public String state() {
            return param("state");
        }

        public String nonce() {
            return param("nonce");
        }

        public String challenge() {
            return param("code_challenge");
        }

        public String param(String name) {
            String value = query.getFirst(name);
            return value == null ? null : UriUtils.decode(value, StandardCharsets.UTF_8);
        }

        public String location() {
            return response.getHeader("Location");
        }
    }

    public static Started start(MockMvc mvc, String redirect) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/v1/auth/oauth2/google");
        if (redirect != null) {
            request.param("redirect", redirect);
        }
        MockHttpServletResponse response = mvc.perform(request).andReturn().getResponse();
        assertThat(response.getStatus()).as("start of the flow").isEqualTo(302);
        String location = response.getHeader("Location");
        assertThat(location).as("redirect to Google").startsWith(FakeGoogle.issuer() + "/auth");
        return new Started(response, UriComponentsBuilder.fromUriString(location).build().getQueryParams(),
                response.getCookie("wc_oauth"));
    }

    /** Google returns with a code. */
    public static MockHttpServletResponse callback(MockMvc mvc, Started started, String state) throws Exception {
        return callback(mvc, started.cookie(), "code", "fake-code", state);
    }

    /** Google returns with an error, as when the user cancels. */
    public static MockHttpServletResponse cancelled(MockMvc mvc, Started started) throws Exception {
        return callback(mvc, started.cookie(), "error", "access_denied", started.state());
    }

    public static MockHttpServletResponse callback(MockMvc mvc, Cookie oauth, String name, String value, String state)
            throws Exception {
        MockHttpServletRequestBuilder request = get("/api/v1/auth/oauth2/callback/google").param(name, value);
        if (state != null) {
            request.param("state", state);
        }
        if (oauth != null) {
            request.cookie(oauth);
        }
        return mvc.perform(request).andReturn().getResponse();
    }

    /** {@code base64url(sha256(verifier))}, the S256 challenge of RFC 7636. */
    public static String s256(String verifier) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    public static URI location(MockHttpServletResponse response) {
        return URI.create(response.getHeader("Location"));
    }
}
