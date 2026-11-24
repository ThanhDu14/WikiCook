package com.wikicook.review.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "wikicook.review.storage")
public class ReviewStorageProperties {

    /**
     * Maximum file size in bytes for a single Cooksnap image (default: 5 MB).
     */
    private long maxSizeBytes = 5 * 1024 * 1024L;

    /**
     * Allowed MIME types for Cooksnap uploads.
     */
    private List<String> allowedMimeTypes = List.of("image/jpeg", "image/png", "image/webp");

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public List<String> getAllowedMimeTypes() {
        return allowedMimeTypes;
    }

    public void setAllowedMimeTypes(List<String> allowedMimeTypes) {
        this.allowedMimeTypes = allowedMimeTypes;
    }
}

