package com.wikicook.devstub;

import com.wikicook.common.security.CurrentUser;
import com.wikicook.mealplan.port.DietaryProfilePort;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Switches the dev stubs on with {@code wikicook.dev-stubs.enabled=true} (see the package doc). An
 * auto-configuration (listed in {@code META-INF/spring/…AutoConfiguration.imports}, not component-scanned) so it
 * is processed after every application bean and {@code @ConditionalOnMissingBean} sees the real ones.
 */
@AutoConfiguration
@ConditionalOnProperty(name = "wikicook.dev-stubs.enabled", havingValue = "true")
@EnableConfigurationProperties(DevStubProperties.class)
public class DevStubConfig {

    @Bean
    DevUserProvisioner devUserProvisioner(JdbcTemplate jdbc) {
        return new DevUserProvisioner(jdbc);
    }

    /** The configured user exists before the first request, so ai_requests rows can reference it. */
    @Bean
    ApplicationRunner devUserAtStartup(DevUserProvisioner provisioner, DevStubProperties properties) {
        return args -> provisioner.ensure(properties.userId());
    }

    @Bean
    @ConditionalOnMissingBean(CurrentUser.class)
    CurrentUser stubCurrentUser(DevStubProperties properties, DevUserProvisioner provisioner) {
        return new StubCurrentUser(properties.userId(), provisioner);
    }

    @Bean
    @ConditionalOnMissingBean(DietaryProfilePort.class)
    DietaryProfilePort stubDietaryProfile(DevStubProperties properties) {
        return new StubDietaryProfile(properties.profile().toPlannerProfile());
    }
}
