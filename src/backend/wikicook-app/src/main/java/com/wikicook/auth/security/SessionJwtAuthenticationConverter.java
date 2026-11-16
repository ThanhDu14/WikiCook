package com.wikicook.auth.security;

import com.wikicook.auth.dto.SessionSnapshot;
import com.wikicook.auth.repository.UserSessionRepository;
import com.wikicook.auth.service.AuthException;
import com.wikicook.common.security.AccountStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Turns a signature-valid access token into an authenticated request, or refuses it (research R1). Exactly one query
 * by primary key decides, in this order:
 * <ol>
 * <li>no row, or the token's {@code sub} is not the session's owner: 401 {@code UNAUTHENTICATED};
 * <li>{@code users.status} is not ACTIVE: 403 {@code ACCOUNT_SUSPENDED} or {@code ACCOUNT_BANNED} with the reason,
 *     even if the session was revoked, because a suspension revokes sessions and the user must see the suspension;
 * <li>revoked or expired session: 401 {@code UNAUTHENTICATED};
 * <li>otherwise the authorities come from the <em>current</em> {@code users.role}, plus {@code EMAIL_VERIFIED}.
 * </ol>
 * So logout, password reset, role change and suspension apply on the next request even while the JWT is valid.
 * Activity is written at most once every five minutes (research R2). Registered as a bean in {@link SecurityConfig},
 * not scanned: a Spring {@code Converter} component would be pulled into every {@code @WebMvcTest} slice.
 */
public class SessionJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    static final Duration ACTIVITY_WINDOW = Duration.ofMinutes(5);
    static final Duration IDLE_TIMEOUT = Duration.ofHours(24);

    private final UserSessionRepository sessions;
    private final Clock clock;

    public SessionJwtAuthenticationConverter(UserSessionRepository sessions, Clock clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long userId = parseLong(jwt.getSubject());
        Number sid = jwt.getClaim("sid");
        if (userId == null || sid == null) {
            throw failure(AuthException.unauthenticated());
        }
        SessionSnapshot snapshot = sessions.findSnapshot(sid.longValue())
                .filter(found -> found.userId() == userId)
                .orElseThrow(() -> failure(AuthException.unauthenticated()));

        if (snapshot.status() != AccountStatus.ACTIVE) {
            throw failure(AuthException.restricted(snapshot));
        }
        Instant now = clock.instant();
        if (snapshot.revokedAt() != null || !snapshot.expiresAt().isAfter(now)) {
            throw failure(AuthException.unauthenticated());
        }
        // the snapshot already has the activity time, so a fresh session costs no second statement; the WHERE of the
        // UPDATE still keeps two concurrent requests from both writing
        Instant staleBefore = now.minus(ACTIVITY_WINDOW);
        if (snapshot.lastActivityAt().isBefore(staleBefore)) {
            sessions.touchActivity(snapshot.sessionId(), now, staleBefore, now.plus(IDLE_TIMEOUT));
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(snapshot.role().authority()));
        if (snapshot.emailVerified()) {
            authorities.add(new SimpleGrantedAuthority("EMAIL_VERIFIED"));
        }
        AuthenticatedAccount account = new AuthenticatedAccount(snapshot.userId(), snapshot.role(),
                snapshot.emailVerified(), snapshot.sessionId(), snapshot.pendingRoleNoticeFrom());
        return new AccountAuthentication(account, authorities, jwt);
    }

    private static Long parseLong(String value) {
        try {
            return value == null ? null : Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static AuthFailure failure(AuthException problem) {
        return new AuthFailure(problem);
    }

    /** Carries the problem through Spring Security's authentication machinery to the entry point. */
    public static class AuthFailure extends AuthenticationException {

        private final transient AuthException problem;

        public AuthFailure(AuthException problem) {
            super(problem.getMessage());
            this.problem = problem;
        }

        public AuthException problem() {
            return problem;
        }
    }
}
