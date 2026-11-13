package com.wikicook.auth.security;

import com.wikicook.auth.config.AuthProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * The refresh-token cookie {@code wc_rt} (research R2): {@code HttpOnly} so scripts cannot read it, {@code Secure}
 * unless {@code wikicook.auth.cookie-secure=false} (plain http on localhost), {@code SameSite=Strict}, and sent only to
 * {@code /api/v1/auth}. With "Remember me" it has a {@code Max-Age} (the time left of the session); without it it is a
 * session cookie that the browser drops when it closes.
 */
@Component
public class RefreshCookie {

    public static final String NAME = "wc_rt";
    public static final String PATH = "/api/v1/auth";

    private final AuthProperties properties;
    private final Clock clock;

    public RefreshCookie(AuthProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /** The cookie carrying {@code token}; {@code expiresAt} only matters when {@code rememberMe} is true. */
    public String issue(String token, boolean rememberMe, Instant expiresAt) {
        ResponseCookie.ResponseCookieBuilder cookie = base(token);
        if (rememberMe) {
            cookie.maxAge(Duration.between(clock.instant(), expiresAt).toSeconds());
        }
        return cookie.build().toString();
    }

    /** Tells the browser to forget the cookie. */
    public String clear() {
        return base("").maxAge(Duration.ZERO).build().toString();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .path(PATH);
    }
}
