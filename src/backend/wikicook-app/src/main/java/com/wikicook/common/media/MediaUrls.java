package com.wikicook.common.media;

/**
 * The URLs clients use for stored files. Without a CDN they are the media routes of the backend
 * ({@code /api/v1/media/<key>}); with {@code wikicook.media.public-base-url} set they point straight at CloudFront
 * ({@code <base>/<key>}), which serves the S3 objects of the same keys. Static so the DTO factories of 010, 011 and 070
 * build them alike; the base is set once at startup by {@link MediaStorageConfig}.
 */
public final class MediaUrls {

    private static final String API_PREFIX = "/api/v1/media/";

    private static volatile String cdnBase;

    private MediaUrls() {
    }

    /** Sets the CDN base; blank or {@code null} goes back to the backend routes. */
    static void configure(String publicBaseUrl) {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            cdnBase = null;
        } else {
            String base = publicBaseUrl.strip();
            cdnBase = base.endsWith("/") ? base : base + "/";
        }
    }

    /** The URL of an avatar from its storage key ({@code avatars/<uuid>.jpg}); {@code null} for no picture. */
    public static String of(String storageKey) {
        if (storageKey == null) {
            return null;
        }
        String base = cdnBase;
        return (base != null ? base : API_PREFIX) + storageKey;
    }

    /** The URL of a Cooksnap photo or its thumbnail from the storage key kept in {@code cooksnaps.storage_key}. */
    public static String cooksnap(String storageKey, boolean thumbnail) {
        String base = cdnBase;
        if (base != null) {
            return base + CooksnapKeys.publicKey(storageKey, thumbnail);
        }
        return API_PREFIX + "cooksnaps/" + storageKey + (thumbnail ? "?thumbnail=true" : "");
    }
}
