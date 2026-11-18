package com.wikicook.auth.security;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.converter.ClaimTypeConverter;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

/**
 * The part of {@link SecurityConfig} that exists only when Google is configured (research R7): {@code oauth2Login}
 * with Authorization Code and PKCE, the {@code wc_oauth} cookie instead of an {@code HttpSession}, the success and
 * failure handlers, and a five second timeout for the token endpoint and the key set (NFR-005). Nothing here is
 * created without a client id, so the application starts, and e-mail sign-in works, without any Google setting.
 *
 * <p>Deliberate differences from the Spring defaults:
 * <ul>
 * <li>the authorized client is not kept anywhere (the default would create an {@code HttpSession});
 * <li>no call to the userinfo endpoint: the ID token already carries {@code email}, {@code email_verified} and
 *     {@code name}, so one request fewer can fail;
 * <li>no {@code issuer-uri}: it would make Spring call Google at start-up. The key set address is in the registration.
 * </ul>
 */
public class GoogleOAuthConfigurer {

    public static final String AUTHORIZATION_BASE = "/api/v1/auth/oauth2";
    public static final String CALLBACK_PATTERN = "/api/v1/auth/oauth2/callback/*";
    static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final ClientRegistrationRepository registrations;
    private final CookieOAuth2AuthorizationRequestRepository requests;
    private final GoogleLoginHandlers handlers;

    public GoogleOAuthConfigurer(ClientRegistrationRepository registrations,
            CookieOAuth2AuthorizationRequestRepository requests, GoogleLoginHandlers handlers) {
        this.registrations = registrations;
        this.requests = requests;
        this.handlers = handlers;
    }

    public void apply(HttpSecurity http) throws Exception {
        http.oauth2Login(login -> login
                .clientRegistrationRepository(registrations)
                .loginPage("/login") // the SPA's page; stops Spring from generating one
                .authorizationEndpoint(endpoint -> endpoint
                        .baseUri(AUTHORIZATION_BASE)
                        .authorizationRequestRepository(requests)
                        .authorizationRequestResolver(new GoogleAuthorizationRequestResolver(registrations, AUTHORIZATION_BASE)))
                .redirectionEndpoint(endpoint -> endpoint.baseUri(CALLBACK_PATTERN))
                .tokenEndpoint(endpoint -> endpoint.accessTokenResponseClient(tokenClient()))
                .userInfoEndpoint(endpoint -> endpoint.oidcUserService(idTokenOnly()))
                .authorizedClientRepository(new NoAuthorizedClients())
                .successHandler(handlers)
                .failureHandler(handlers));
    }

    /** Code for token over a request that gives up after five seconds, so a slow Google never hangs a sign-in. */
    static OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenClient() {
        RestClient restClient = RestClient.builder()
                .requestFactory(timeouts())
                .messageConverters(converters -> {
                    converters.clear();
                    converters.add(new FormHttpMessageConverter());
                    converters.add(new OAuth2AccessTokenResponseHttpMessageConverter());
                })
                .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
                .build();
        RestClientAuthorizationCodeTokenResponseClient client = new RestClientAuthorizationCodeTokenResponseClient();
        client.setRestClient(restClient);
        return client;
    }

    /**
     * Decoders for the ID token: the signature against the key set of the registration (fetched with the same timeout,
     * cached by Nimbus), then the checks of {@link OidcIdTokenValidator} (issuer, audience, expiry, authorized party).
     * The nonce is compared by the OIDC provider itself.
     */
    static JwtDecoderFactory<ClientRegistration> idTokenDecoders() {
        Map<String, JwtDecoder> decoders = new ConcurrentHashMap<>();
        RestTemplate rest = new RestTemplate(timeouts());
        return registration -> decoders.computeIfAbsent(registration.getRegistrationId(), id -> {
            NimbusJwtDecoder decoder = NimbusJwtDecoder
                    .withJwkSetUri(registration.getProviderDetails().getJwkSetUri())
                    .jwsAlgorithm(SignatureAlgorithm.RS256)
                    .restOperations(rest)
                    .build();
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(),
                    new OidcIdTokenValidator(registration)));
            decoder.setClaimSetConverter(new ClaimTypeConverter(OidcIdTokenDecoderFactory.createDefaultClaimTypeConverters()));
            return decoder;
        });
    }

    private static OAuth2UserService<OidcUserRequest, org.springframework.security.oauth2.core.oidc.user.OidcUser> idTokenOnly() {
        return request -> new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), request.getIdToken());
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT);
        factory.setReadTimeout(TIMEOUT);
        return factory;
    }

    /** Nothing is stored: the application has no session, and the access token of Google is never used again. */
    private static final class NoAuthorizedClients implements OAuth2AuthorizedClientRepository {

        @Override
        public <T extends org.springframework.security.oauth2.client.OAuth2AuthorizedClient> T loadAuthorizedClient(
                String clientRegistrationId, org.springframework.security.core.Authentication principal,
                jakarta.servlet.http.HttpServletRequest request) {
            return null;
        }

        @Override
        public void saveAuthorizedClient(org.springframework.security.oauth2.client.OAuth2AuthorizedClient authorizedClient,
                org.springframework.security.core.Authentication principal, jakarta.servlet.http.HttpServletRequest request,
                jakarta.servlet.http.HttpServletResponse response) {
            // not kept
        }

        @Override
        public void removeAuthorizedClient(String clientRegistrationId, org.springframework.security.core.Authentication principal,
                jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
            // nothing was kept
        }
    }
}
