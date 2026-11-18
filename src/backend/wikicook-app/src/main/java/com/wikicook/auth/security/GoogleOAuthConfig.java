package com.wikicook.auth.security;

import com.wikicook.auth.config.AuthProperties;
import com.wikicook.auth.service.GoogleSignInService;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;

/**
 * The Google beans, created <b>only</b> when both {@code WIKICOOK_GOOGLE_CLIENT_ID} and
 * {@code WIKICOOK_GOOGLE_CLIENT_SECRET} are set (research R7, NFR-005). Spring Boot refuses an empty client id at start-up,
 * which is why there is no {@code spring.security.oauth2.client.registration} in {@code application.yml}; the
 * registration is built here instead. Without them the filter chain has no {@code oauth2Login} and
 * {@code GoogleStartController} sends the user back to {@code /login?error=google_unavailable}.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnExpression("!'${wikicook.auth.google.client-id:}'.isBlank() && !'${wikicook.auth.google.client-secret:}'.isBlank()")
public class GoogleOAuthConfig {

    @Bean
    ClientRegistrationRepository googleClientRegistrationRepository(AuthProperties properties) {
        return new InMemoryClientRegistrationRepository(googleRegistration(properties));
    }

    @Bean
    JwtDecoderFactory<ClientRegistration> googleIdTokenDecoderFactory() {
        return GoogleOAuthConfigurer.idTokenDecoders();
    }

    @Bean
    CookieOAuth2AuthorizationRequestRepository googleAuthorizationRequestRepository(AuthProperties properties, Clock clock) {
        return new CookieOAuth2AuthorizationRequestRepository(properties.jwtSecret(), properties.cookieSecure(), clock);
    }

    @Bean
    GoogleLoginHandlers googleLoginHandlers(GoogleSignInService service, RefreshCookie cookie, AuthProperties properties) {
        return new GoogleLoginHandlers(service, cookie, properties);
    }

    @Bean
    GoogleOAuthConfigurer googleOAuthConfigurer(ClientRegistrationRepository registrations,
            CookieOAuth2AuthorizationRequestRepository requests, GoogleLoginHandlers handlers) {
        return new GoogleOAuthConfigurer(registrations, requests, handlers);
    }

    /**
     * Google's endpoints and scopes ({@code openid profile email}) from Spring, our client and the redirect URI from
     * {@code WIKICOOK_PUBLIC_BASE_URL} (never from a request header: behind nginx the port is lost). Register
     * {@code {public-base-url}/api/v1/auth/oauth2/callback/google} at Google.
     */
    static ClientRegistration googleRegistration(AuthProperties properties) {
        String base = properties.publicBaseUrl();
        base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(properties.google().clientId())
                .clientSecret(properties.google().clientSecret())
                .redirectUri(base + "/api/v1/auth/oauth2/callback/google")
                .build();
    }
}
