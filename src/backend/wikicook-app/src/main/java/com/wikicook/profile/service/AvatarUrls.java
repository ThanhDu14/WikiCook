package com.wikicook.profile.service;

/** The public URL of a stored avatar: the media route of this module followed by the storage key. */
public final class AvatarUrls {

    private static final String PREFIX = "/api/v1/media/";

    private AvatarUrls() {
    }

    /** {@code null} for no picture, so the client shows the default avatar. */
    public static String of(String storageKey) {
        return storageKey == null ? null : PREFIX + storageKey;
    }
}
