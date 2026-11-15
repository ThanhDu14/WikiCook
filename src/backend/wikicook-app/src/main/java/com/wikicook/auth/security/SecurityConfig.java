package com.wikicook.auth.security;

import com.wikicook.auth.repository.UserSessionRepository;
import com.wikicook.auth.service.AccessTokenService;
import com.wikicook.auth.service.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * The security of the whole application (spec 010 FR-010, FR-021; research R1, R10). Stateless: no
 * {@code HttpSession}, CSRF off (every endpoint authenticates with the {@code Authorization} header; the two that read
 * the refresh cookie are protected by {@link ClientHeaderFilter}). Default is deny: only {@link PublicRoutes} are open,
 * the rest of {@code /api/**} needs a valid session, anything else is refused. Roles inherit downwards for
 * {@code @ModeratorOnly} and {@code @AdministratorOnly}. Only for a servlet application: contexts started without a web
 * environment (repository tests) have no {@code HttpSecurity} and need no filter chain.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SessionJwtAuthenticationConverter converter,
            JwtDecoder jwtDecoder, ProblemResponseWriter problems) throws Exception {
        AuthenticationEntryPoint entryPoint = entryPoint(problems);
        AccessDeniedHandler accessDenied = accessDeniedHandler(problems);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(PublicRoutes.matcher()).permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDenied))
                .oauth2ResourceServer(resource -> resource
                        .bearerTokenResolver(new PublicRouteBearerTokenResolver())
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDenied)
                        .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(converter)))
                .addFilterBefore(new ClientHeaderFilter(problems), BearerTokenAuthenticationFilter.class)
                .addFilterAfter(new AccountStateHeaderFilter(), BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    SessionJwtAuthenticationConverter sessionJwtAuthenticationConverter(UserSessionRepository sessions, Clock clock) {
        return new SessionJwtAuthenticationConverter(sessions, clock);
    }

    /** One decoder (signature, expiry, issuer) for the resource server and for {@link AccessTokenService}. */
    @Bean
    JwtDecoder jwtDecoder(AccessTokenService accessTokens) {
        return accessTokens.decoder();
    }

    /** ADMINISTRATOR > MODERATOR > CONTRIBUTOR > MEMBER (FR-020). Static so method security can see it early. */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_ADMINISTRATOR > ROLE_MODERATOR
                ROLE_MODERATOR > ROLE_CONTRIBUTOR
                ROLE_CONTRIBUTOR > ROLE_MEMBER
                """);
    }

    /** 401 for everyone who is not authenticated; a suspended or banned account gets its 403 with the reason. */
    private static AuthenticationEntryPoint entryPoint(ProblemResponseWriter problems) {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) ->
                problems.write(response, exception instanceof SessionJwtAuthenticationConverter.AuthFailure failure
                        ? failure.problem() : AuthException.unauthenticated());
    }

    /**
     * An authenticated caller who lacks the authority is 403 {@code ACCESS_DENIED}; an anonymous one is really a
     * missing sign-in, so 401.
     */
    private static AccessDeniedHandler accessDeniedHandler(ProblemResponseWriter problems) {
        return (HttpServletRequest request, HttpServletResponse response,
                org.springframework.security.access.AccessDeniedException exception) -> writeDenied(problems, response);
    }

    private static void writeDenied(ProblemResponseWriter problems, HttpServletResponse response) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean anonymous = authentication == null || authentication instanceof AnonymousAuthenticationToken;
        problems.write(response, anonymous ? AuthException.unauthenticated() : AuthException.accessDenied());
    }
}
