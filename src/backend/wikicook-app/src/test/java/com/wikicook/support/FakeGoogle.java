package com.wikicook.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * Stands in for Google in the integration tests (spec 010 T141-T143, research R14): one WireMock server for the whole
 * JVM that serves the token endpoint and the key set, and signs ID tokens with a key made for the tests. Nothing here
 * talks to the real Google and no credential is real. The application is configured with the properties of
 * {@link GoogleIntegrationTest} so its Google beans exist, and {@link Registration} replaces the registration with one whose
 * endpoints point at this server (the "authorization endpoint" is only a URL the test reads: no browser follows it).
 */
public final class FakeGoogle {

    public static final String CLIENT_ID = "test-client-id.apps.googleusercontent.com";
    public static final String CLIENT_SECRET = "test-client-secret";
    public static final WireMockServer SERVER = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    private static final RSAKey KEY = key("test-key");
    private static final RSAKey OTHER_KEY = key("other-key");

    static {
        SERVER.start();
        Runtime.getRuntime().addShutdownHook(new Thread(SERVER::stop));
    }

    private FakeGoogle() {
    }

    private static RSAKey key(String id) {
        try {
            return new RSAKeyGenerator(2048).keyID(id).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The base URL of the fake; also its {@code iss}. */
    public static String issuer() {
        return SERVER.baseUrl();
    }

    /** Forgets every stub and request of the previous test and serves the key set again. */
    public static void reset() {
        SERVER.resetAll();
        SERVER.stubFor(get(urlPathEqualTo("/certs")).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json").withBody(new JWKSet(KEY.toPublicJWK()).toString())));
    }

    /** The token endpoint answers with this ID token. */
    public static void tokenEndpointReturns(String idToken) {
        SERVER.stubFor(post(urlPathEqualTo("/token")).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"access_token\":\"fake-access-token\",\"token_type\":\"Bearer\",\"expires_in\":3600,"
                        + "\"scope\":\"openid profile email\",\"id_token\":\"" + idToken + "\"}")));
    }

    /** A Google person: what the ID token says about them, and the ways a test can spoil it. */
    public record Person(String subject, String email, String name, Boolean emailVerified, String audience, Instant expires,
            boolean forgedSignature) {

        public Person(String subject, String email, String name) {
            this(subject, email, name, true, CLIENT_ID, Instant.now().plusSeconds(3600), false);
        }

        public Person unverified() {
            return new Person(subject, email, name, false, audience, expires, forgedSignature);
        }

        /** No {@code email_verified} claim at all. */
        public Person withoutVerifiedClaim() {
            return new Person(subject, email, name, null, audience, expires, forgedSignature);
        }

        public Person audience(String other) {
            return new Person(subject, email, name, emailVerified, other, expires, forgedSignature);
        }

        public Person expired() {
            return new Person(subject, email, name, emailVerified, audience, Instant.now().minusSeconds(3600), forgedSignature);
        }

        /** Signed with a key that is not in the key set of the fake. */
        public Person forged() {
            return new Person(subject, email, name, emailVerified, audience, expires, true);
        }

        /** The ID token as Google would issue it for the request that carried this {@code nonce}. */
        public String idToken(String nonce) {
            return token(this, nonce);
        }
    }

    private static String token(Person person, String nonce) {
        JWTClaimsSet.Builder set = new JWTClaimsSet.Builder()
                .issuer(issuer())
                .audience(person.audience())
                .subject(person.subject())
                .issueTime(new Date())
                .expirationTime(Date.from(person.expires()))
                .claim("nonce", nonce);
        if (person.email() != null) {
            set.claim("email", person.email());
        }
        if (person.emailVerified() != null) {
            set.claim("email_verified", person.emailVerified());
        }
        if (person.name() != null) {
            set.claim("name", person.name());
        }
        RSAKey signer = person.forgedSignature() ? OTHER_KEY : KEY;
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signer.getKeyID()).build(), set.build());
            jwt.sign(new RSASSASigner(signer));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Replaces the Google registration of the application with one that points at this fake. */
    @TestConfiguration(proxyBeanMethods = false)
    public static class Registration {

        @Bean
        @Primary
        ClientRegistrationRepository fakeGoogleRegistrations() {
            String base = issuer();
            ClientRegistration google = ClientRegistration.withRegistrationId("google")
                    .clientId(CLIENT_ID)
                    .clientSecret(CLIENT_SECRET)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("http://localhost:3000/api/v1/auth/oauth2/callback/google")
                    .scope("openid", "profile", "email")
                    .authorizationUri(base + "/auth")
                    .tokenUri(base + "/token")
                    .jwkSetUri(base + "/certs")
                    .issuerUri(base)
                    .userNameAttributeName("sub")
                    .build();
            return new InMemoryClientRegistrationRepository(google);
        }
    }
}
