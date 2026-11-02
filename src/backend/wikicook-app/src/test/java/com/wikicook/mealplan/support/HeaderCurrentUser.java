package com.wikicook.mealplan.support;

import com.wikicook.common.security.CurrentUser;
import com.wikicook.common.security.UnauthenticatedException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Stands in for 010 in controller tests: the {@value #HEADER} header is the token, no header is a 401. Two
 * requests with the same header are "two devices of one user".
 */
@TestConfiguration(proxyBeanMethods = false)
public class HeaderCurrentUser {

    public static final String HEADER = "X-Test-User";

    @Bean
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
}
