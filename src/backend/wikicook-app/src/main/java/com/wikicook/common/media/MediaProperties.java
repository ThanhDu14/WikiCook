package com.wikicook.common.media;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code wikicook.media.*}: where uploaded files (avatars, Cooksnaps) are kept and how clients reach them.
 *
 * @param storage       {@code local} (directory {@code wikicook.profile.media-dir}, the default, used by tests and CI)
 *                      or {@code s3}
 * @param publicBaseUrl base URL of the CDN in front of the bucket, e.g. {@code https://dxxxx.cloudfront.net}; when
 *                      blank, clients get the media routes of the backend ({@code /api/v1/media/...})
 * @param s3            the bucket, used when {@code storage=s3}
 */
@ConfigurationProperties("wikicook.media")
public record MediaProperties(
        @DefaultValue("local") String storage,
        @DefaultValue("") String publicBaseUrl,
        @DefaultValue S3 s3) {

    /**
     * @param bucket   the private bucket holding the objects
     * @param region   AWS region of the bucket; blank uses the default region chain ({@code AWS_REGION})
     * @param endpoint only for an S3-compatible store in development (LocalStack, MinIO); blank on AWS
     */
    public record S3(
            @DefaultValue("") String bucket,
            @DefaultValue("") String region,
            @DefaultValue("") String endpoint) {
    }
}
