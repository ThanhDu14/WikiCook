package com.wikicook.support;

import com.wikicook.common.security.CurrentUser;
import com.wikicook.common.security.UnauthenticatedException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Stands in for 010 in the controller tests of 030, 031 and 040 (research R13): the {@value #HEADER} header is the
 * token, no header is a 401. Two requests with the same header are "two devices of one user".
 *
 * <p>It replaces the real {@code CurrentUser} ({@code @Primary}) and opens every route ({@code SecurityFilterChain} at
 * the highest precedence, so it wins over the production chain), so these tests keep their small diff and need no
 * session rows. It exists only in tests that {@code @Import} it; the real security has its own tests
 * ({@code SessionCheckIT}, {@code SecuredRoutesIT}).
 */
@TestConfiguration(proxyBeanMethods = false)
public class HeaderCurrentUser {

    public static final String HEADER = "X-Test-User";

    @Bean
    @Primary
    CurrentUser headerCurrentUser() {
        return () -> {
            String header = RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servlet
                    ? servlet.getRequest().getHeader(HEADER) : null;
            if (header == null) {
                throw new UnauthenticatedException();
            }
            return Long.parseLong(header);
        };
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain permitAllForTests(HttpSecurity http) throws Exception {
        // not "any request": Spring Security rejects two any-request chains, and the production chain must stay behind
        http.securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
        return http.build();
    }
}
