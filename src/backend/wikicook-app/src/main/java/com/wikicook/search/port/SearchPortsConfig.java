package com.wikicook.search.port;

import java.util.Optional;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Viewer adapters until spec 010 provides one (a real {@link SearchViewerPort} bean wins over both). Registered as an
 * auto-configuration, like {@code DefaultPortsConfig}, so {@code @ConditionalOnMissingBean} sees application beans.
 */
@AutoConfiguration(afterName = "com.wikicook.devstub.DevStubConfig")
public class SearchPortsConfig {

    /** Dev stubs on: viewer from the {@code X-Dev-User-Id} header. */
    @Bean
    @ConditionalOnMissingBean(SearchViewerPort.class)
    @ConditionalOnProperty(name = "wikicook.dev-stubs.enabled", havingValue = "true")
    SearchViewerPort devHeaderSearchViewer() {
        return new DevHeaderSearchViewer();
    }

    /** No login yet and no dev stubs: everybody searches as a guest. */
    @Bean
    @ConditionalOnMissingBean(SearchViewerPort.class)
    SearchViewerPort guestSearchViewer() {
        return Optional::empty;
    }
}
