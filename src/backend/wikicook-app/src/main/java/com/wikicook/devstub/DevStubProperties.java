package com.wikicook.devstub;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * {@code wikicook.dev-stubs.*}.
 *
 * @param userId the user when a request has no {@code X-Dev-User-Id} header
 */
@Validated
@ConfigurationProperties("wikicook.dev-stubs")
public record DevStubProperties(
        @DefaultValue("false") boolean enabled,
        @Positive @DefaultValue("1") long userId) {
}
