package com.wikicook.profile.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * {@code wikicook.profile.*}.
 *
 * @param mediaDir          directory of {@code LocalDiskMediaStorage}; the Docker volume {@code media} in compose
 * @param writeLimitPerHour profile writes per hour per kind (NFR-008)
 */
@Validated
@ConfigurationProperties("wikicook.profile")
public record ProfileProperties(
        @NotBlank @DefaultValue("./.media") String mediaDir,
        @Positive @DefaultValue("20") int writeLimitPerHour) {
}
