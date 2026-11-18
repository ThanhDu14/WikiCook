package com.wikicook.auth.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

/**
 * Where the browser may be sent after Google sign-in (FR-011): only a path inside WikiCook. Same rule as
 * {@code safeRedirect} of the frontend: another site, a protocol-relative URL, a backslash, a control character, a
 * scheme or the sign-in pages themselves are not accepted, so a crafted link cannot turn the callback into an open
 * redirect. The backend checks when the flow starts and again when it ends.
 */
public final class SafeRedirects {

    private static final int MAX_LENGTH = 2048;
    private static final Set<String> AUTH_PAGES = Set.of("/login", "/register");

    private SafeRedirects() {
    }

    /** @return {@code value} when it is a path inside WikiCook, otherwise {@code null} (the client then goes home) */
    public static String sanitize(String value) {
        if (value == null || value.isEmpty() || value.length() > MAX_LENGTH) {
            return null;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c <= 0x1f || c == 0x7f || c == '\\') {
                return null;
            }
        }
        if (!value.startsWith("/") || value.startsWith("//")) {
            return null;
        }
        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            return null;
        }
        if (uri.getScheme() != null || uri.getRawAuthority() != null || AUTH_PAGES.contains(uri.getPath())) {
            return null;
        }
        return value;
    }
}
