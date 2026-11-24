package com.wikicook.common.media;

import java.util.regex.Pattern;

/**
 * The shape every storage key must have, on disk or on S3: letters, digits, {@code . _ -} and {@code /} between
 * segments that do not start with a dot (research R7). Such a key never leaves the root of a directory and is a safe
 * S3 object key and CloudFront path.
 */
final class MediaKeys {

    private static final Pattern KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*(/[A-Za-z0-9][A-Za-z0-9._-]*)*");

    private MediaKeys() {
    }

    static String requireValid(String key) {
        if (key == null || !KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Not a plain relative key");
        }
        return key;
    }

    /** The MIME type of a key from its extension; the files are made by our own image pipelines. */
    static String contentType(String key) {
        String lower = key.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }
}
