package com.wikicook.common.media;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MediaUrlsTest {

    @AfterEach
    void reset() {
        MediaUrls.configure(null);
    }

    @Test
    @DisplayName("without a CDN the URLs are the media routes of the backend")
    void backendRoutes() {
        assertThat(MediaUrls.of("avatars/a.jpg")).isEqualTo("/api/v1/media/avatars/a.jpg");
        assertThat(MediaUrls.of(null)).isNull();
        assertThat(MediaUrls.cooksnap("cs_1.webp", false)).isEqualTo("/api/v1/media/cooksnaps/cs_1.webp");
        assertThat(MediaUrls.cooksnap("cs_1.webp", true)).isEqualTo("/api/v1/media/cooksnaps/cs_1.webp?thumbnail=true");
    }

    @Test
    @DisplayName("with a CDN the URLs point at the S3 keys behind CloudFront")
    void cloudFront() {
        MediaUrls.configure("https://d123.cloudfront.net");

        assertThat(MediaUrls.of("avatars/a.jpg")).isEqualTo("https://d123.cloudfront.net/avatars/a.jpg");
        assertThat(MediaUrls.cooksnap("cs_1.webp", false)).isEqualTo("https://d123.cloudfront.net/cooksnaps/cs_1.webp");
        assertThat(MediaUrls.cooksnap("cs_1.webp", true))
                .isEqualTo("https://d123.cloudfront.net/cooksnaps/thumb_cs_1.webp");
    }

    @Test
    @DisplayName("a trailing slash and a blank base are handled")
    void baseNormalised() {
        MediaUrls.configure("https://cdn.example.com/");
        assertThat(MediaUrls.of("avatars/a.jpg")).isEqualTo("https://cdn.example.com/avatars/a.jpg");

        MediaUrls.configure("  ");
        assertThat(MediaUrls.of("avatars/a.jpg")).isEqualTo("/api/v1/media/avatars/a.jpg");
    }
}
