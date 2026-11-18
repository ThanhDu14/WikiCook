package com.wikicook.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.auth.config.AuthProperties;
import com.wikicook.support.TestAuthProperties;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/** Research R7: the Google registration is built in code, from the public base URL, without any call to Google. */
class GoogleOAuthConfigTest {

    private static AuthProperties properties(String baseUrl, String id, String secret) {
        AuthProperties base = TestAuthProperties.create();
        return new AuthProperties(base.jwtSecret(), base.emailHashKey(), baseUrl, base.cookieSecure(), base.bcryptStrength(),
                Duration.ofMinutes(15), base.mailFrom(), "", base.signIn(), base.jobs(), new AuthProperties.Google(id, secret),
                base.bootstrapAdmin());
    }

    @Test
    @DisplayName("authorization code, openid profile email, redirect URI under the public base URL, no issuer lookup")
    void registration() {
        ClientRegistration google = GoogleOAuthConfig.googleRegistration(properties("https://wikicook.example/", "id-1", "secret-1"));

        assertThat(google.getRegistrationId()).isEqualTo("google");
        assertThat(google.getClientId()).isEqualTo("id-1");
        assertThat(google.getClientSecret()).isEqualTo("secret-1");
        assertThat(google.getAuthorizationGrantType()).isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
        assertThat(google.getScopes()).containsExactlyInAnyOrder("openid", "profile", "email");
        assertThat(google.getRedirectUri()).isEqualTo("https://wikicook.example/api/v1/auth/oauth2/callback/google");
        assertThat(google.getProviderDetails().getAuthorizationUri()).startsWith("https://accounts.google.com/");
        assertThat(google.getProviderDetails().getJwkSetUri()).isNotBlank();
    }

    @Test
    @DisplayName("Google counts as configured only with both a client id and a secret")
    void configured() {
        assertThat(new AuthProperties.Google("id", "secret").configured()).isTrue();
        assertThat(new AuthProperties.Google("id", "").configured()).isFalse();
        assertThat(new AuthProperties.Google("", "secret").configured()).isFalse();
        assertThat(new AuthProperties.Google("  ", "  ").configured()).isFalse();
    }
}
