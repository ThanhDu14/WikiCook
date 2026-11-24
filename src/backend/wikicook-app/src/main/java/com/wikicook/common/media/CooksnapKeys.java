package com.wikicook.common.media;

/**
 * Where a Cooksnap lives in {@link MediaStorage}. The database keeps the file name ({@code cs_<recipe>_<time>_<id>.webp});
 * the photo is {@code cooksnaps/<name>} and its thumbnail {@code cooksnaps/thumb_<name>}. A hidden Cooksnap is moved
 * under {@code hidden/}, which the CDN does not serve, so it can be restored by moderation.
 */
public final class CooksnapKeys {

    private CooksnapKeys() {
    }

    public static String publicKey(String storageKey, boolean thumbnail) {
        return "cooksnaps/" + (thumbnail ? "thumb_" : "") + storageKey;
    }

    public static String hiddenKey(String storageKey, boolean thumbnail) {
        return "hidden/" + publicKey(storageKey, thumbnail);
    }
}
