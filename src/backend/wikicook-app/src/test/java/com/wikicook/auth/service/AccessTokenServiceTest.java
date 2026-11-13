package com.wikicook.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wikicook.support.TestAuthProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

/** R1: HS256 access token, 15 minutes, claims sub, sid, iss, iat, exp and no role claim. */
class AccessTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-13T03:00:00Z");

    private static AccessTokenService at(Instant now) {
        return new AccessTokenService(TestAuthProperties.create(), Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void carriesSubSidIssIatExpAndNoRoleClaim() {
        AccessTokenService service = at(NOW);

        AccessTokenService.IssuedToken issued = service.issue(42L, 7L);
        Jwt jwt = service.decode(issued.value());

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.<Long>getClaim("sid")).isEqualTo(7L);
        // "wikicook" is not a URL, so read the claim as text rather than through getIssuer()
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("wikicook");
        assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(jwt.getClaims()).doesNotContainKeys("role", "roles", "scope", "scp", "authorities");
        assertThat(issued.expiresInSeconds()).isEqualTo(900);
    }

    @Test
    void rejectsATokenSignedWithAnotherSecret() {
        AccessTokenService other = new AccessTokenService(
                TestAuthProperties.create("another-secret-0123456789abcdef0123456789", "key"),
                Clock.fixed(NOW, ZoneOffset.UTC));
        String forged = other.issue(42L, 7L).value();

        assertThatThrownBy(() -> at(NOW).decode(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsATokenAfterItExpires() {
        String token = at(NOW).issue(42L, 7L).value();

        assertThat(at(NOW.plusSeconds(14 * 60 + 59)).decode(token).getSubject()).isEqualTo("42");
        assertThatThrownBy(() -> at(NOW.plusSeconds(15 * 60 + 1)).decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsGarbage() {
        assertThatThrownBy(() -> at(NOW).decode("not-a-jwt")).isInstanceOf(JwtException.class);
    }
}
