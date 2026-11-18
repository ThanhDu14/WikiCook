package com.wikicook.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** FR-011: where Google sign-in may send the browser afterwards; the same cases as {@code safeRedirect.test.ts}. */
class SafeRedirectsTest {

    @ParameterizedTest
    @ValueSource(strings = {"/", "/meal-planner", "/recipes/42?tab=reviews", "/search?q=ph%E1%BB%9F#top", "/profile/edit"})
    @DisplayName("a path inside WikiCook is kept as it is")
    void accepted(String path) {
        assertThat(SafeRedirects.sanitize(path)).isEqualTo(path);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "https://evil.example/phish", "http://localhost:3000/", "//evil.example", "///evil.example", "/\\evil.example",
        "\\\\evil.example", "javascript:alert(1)", "mailto:x@y.z", "relative/path", "evil.example", "/login", "/register",
        "/login?redirect=/x", "/path\nHeader: x", "/path\r\nSet-Cookie: a=b", "/with\u0000nul", "/tab\there"})
    @DisplayName("anything else is dropped: another site, a protocol-relative URL, a scheme, control characters, a sign-in page")
    void refused(String value) {
        assertThat(SafeRedirects.sanitize(value)).isNull();
    }

    @Test
    @DisplayName("a path longer than 2048 characters is dropped")
    void tooLong() {
        assertThat(SafeRedirects.sanitize("/" + "a".repeat(2047))).isNotNull();
        assertThat(SafeRedirects.sanitize("/" + "a".repeat(2048))).isNull();
    }

    @Test
    @DisplayName("a space or a malformed escape, which is not a valid URI, is dropped")
    void notAUri() {
        assertThat(SafeRedirects.sanitize("/has space")).isNull();
        assertThat(SafeRedirects.sanitize("/bad%zzescape")).isNull();
    }
}
