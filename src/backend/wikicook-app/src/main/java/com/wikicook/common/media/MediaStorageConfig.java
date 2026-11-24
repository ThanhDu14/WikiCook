package com.wikicook.common.media;

import java.net.URI;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * Chooses the {@link MediaStorage}. With {@code wikicook.media.storage=s3} this application bean is the store, and the
 * local-disk default of {@code ProfileDefaultsConfig} ({@code @ConditionalOnMissingBean}) steps aside. Credentials
 * come from the default AWS chain, never from this configuration: an IAM role on EC2/ECS, or
 * {@code AWS_ACCESS_KEY_ID}/{@code AWS_SECRET_ACCESS_KEY} elsewhere.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MediaProperties.class)
public class MediaStorageConfig {

    @Bean
    InitializingBean mediaUrlsBase(MediaProperties properties) {
        return () -> MediaUrls.configure(properties.publicBaseUrl());
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "wikicook.media", name = "storage", havingValue = "s3")
    S3Client mediaS3Client(MediaProperties properties) {
        S3ClientBuilder builder = S3Client.builder();
        MediaProperties.S3 s3 = properties.s3();
        if (!s3.region().isBlank()) {
            builder.region(Region.of(s3.region()));
        }
        if (!s3.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(s3.endpoint())).forcePathStyle(true);
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "wikicook.media", name = "storage", havingValue = "s3")
    MediaStorage s3MediaStorage(S3Client mediaS3Client, MediaProperties properties) {
        return new S3MediaStorage(mediaS3Client, properties.s3().bucket());
    }
}
