package com.wikicook.auth.config;

import com.wikicook.auth.security.PrehashedBCryptPasswordEncoder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Registers {@link AuthProperties} and the password encoder (research R3: BCrypt, cost from configuration). */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthProperties.class)
public class AuthConfig {

    @Bean
    PasswordEncoder passwordEncoder(AuthProperties properties) {
        return new PrehashedBCryptPasswordEncoder(properties.bcryptStrength());
    }
}
