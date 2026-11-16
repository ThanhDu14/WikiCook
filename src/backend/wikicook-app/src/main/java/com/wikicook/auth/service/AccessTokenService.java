package com.wikicook.auth.service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.wikicook.auth.config.AuthProperties;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

/**
 * Signs and verifies the access token (research R1): HS256 with {@code WIKICOOK_JWT_SECRET}, 15 minutes, claims
 * {@code sub} (user id), {@code sid} ({@code user_sessions.id}), {@code iss}, {@code iat}, {@code exp}. There is
 * deliberately no role claim: the role is read from the database on every request, so a role change applies at once.
 */
@Component
public class AccessTokenService {

    static final String ISSUER = "wikicook";
    static final String SESSION_CLAIM = "sid";

    private final Clock clock;
    private final Duration ttl;
    private final JwtEncoder encoder;
    private final NimbusJwtDecoder decoder;

    @Autowired
    public AccessTokenService(AuthProperties properties, Clock clock) {
        this.clock = clock;
        this.ttl = properties.accessTokenTtl();
        SecretKey key = new SecretKeySpec(properties.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator(Duration.ZERO);
        timestamps.setClock(clock);
        this.decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamps,
                new JwtClaimValidator<Object>("iss", issuer -> issuer != null && ISSUER.equals(issuer.toString()))));
    }

    public IssuedToken issue(long userId, long sessionId) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(Long.toString(userId))
                .claim(SESSION_CLAIM, sessionId)
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .build();
        String value = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new IssuedToken(value, ttl.toSeconds());
    }

    /** @throws JwtException when the signature, issuer or expiry is wrong, or the text is not a JWT */
    public Jwt decode(String token) {
        return decoder.decode(token);
    }

    /** The decoder Spring Security's resource server uses, so there is one verification rule. */
    public JwtDecoder decoder() {
        return decoder;
    }

    public record IssuedToken(String value, long expiresInSeconds) {
    }
}
