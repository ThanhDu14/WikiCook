package com.wikicook.profile.config;

import com.wikicook.common.media.LocalDiskMediaStorage;
import com.wikicook.common.media.MediaStorage;
import com.wikicook.profile.port.CuisineCatalogPort;
import com.wikicook.profile.port.IngredientCatalogPort;
import com.wikicook.profile.port.JdbcCuisineCatalog;
import com.wikicook.profile.port.JdbcIngredientCatalog;
import com.wikicook.profile.port.JdbcPublicRecipes;
import com.wikicook.profile.port.ProfileAuditPort;
import com.wikicook.profile.port.PublicRecipesPort;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * What spec 011 uses while the modules it depends on do not provide their side yet (contracts/integration.md 1.2,
 * the same way {@code DefaultPortsConfig} serves 030). An auto-configuration listed in
 * {@code META-INF/spring/…AutoConfiguration.imports}, so it is processed after every application bean and
 * {@code @ConditionalOnMissingBean} sees the real ones: a real bean replaces the default without any change here.
 */
@AutoConfiguration
@EnableConfigurationProperties(ProfileProperties.class)
public class ProfileDefaultsConfig {

    /** ⛓ 010: records nothing until the adapter over {@code AuditWriter} exists (T116). */
    @Bean
    @ConditionalOnMissingBean(ProfileAuditPort.class)
    ProfileAuditPort noAudit() {
        return (userId, action, details) -> {
        };
    }

    /** ⛓ 060: reads the ingredient tables directly until its catalogue bean exists (T040). */
    @Bean
    @ConditionalOnMissingBean(IngredientCatalogPort.class)
    IngredientCatalogPort jdbcIngredientCatalog(NamedParameterJdbcTemplate jdbc) {
        return new JdbcIngredientCatalog(jdbc);
    }

    /** ⛓ 100: reads the cuisine table directly until its catalogue bean exists (T040). */
    @Bean
    @ConditionalOnMissingBean(CuisineCatalogPort.class)
    CuisineCatalogPort jdbcCuisineCatalog(NamedParameterJdbcTemplate jdbc) {
        return new JdbcCuisineCatalog(jdbc);
    }

    /** ⛓ 060: reads the recipe tables directly until its public recipe bean exists (T064). */
    @Bean
    @ConditionalOnMissingBean(PublicRecipesPort.class)
    PublicRecipesPort jdbcPublicRecipes(NamedParameterJdbcTemplate jdbc) {
        return new JdbcPublicRecipes(jdbc);
    }

    /** Avatars on local disk (research R7); 060 and 070 reuse the bean for their photos. */
    @Bean
    @ConditionalOnMissingBean(MediaStorage.class)
    MediaStorage localDiskMediaStorage(ProfileProperties properties) {
        return new LocalDiskMediaStorage(Path.of(properties.mediaDir()));
    }
}
