package com.wikicook.common.media;

import java.util.Optional;
import org.springframework.core.io.Resource;

/**
 * Where uploaded files live (research R7). Shared by the avatars of 011 and the recipe and cooksnap photos of 060 and
 * 070. {@link LocalDiskMediaStorage} keeps them on a directory, {@link S3MediaStorage} in an S3 bucket
 * ({@code wikicook.media.storage}). A key is a relative path such as
 * {@code avatars/<uuid>.jpg}; a key that is not a plain relative path is an {@link IllegalArgumentException}.
 */
public interface MediaStorage {

    /** Writes {@code content} under {@code key}; readers never see a half-written file. */
    void store(String key, byte[] content);

    /** The file under {@code key}, or empty when there is none. */
    Optional<Resource> load(String key);

    /** Removes the file; nothing happens when it is already gone. */
    void delete(String key);

    /**
     * Moves the file from {@code from} to {@code to} (used to take a hidden Cooksnap off the public paths); nothing
     * happens when {@code from} is already gone.
     */
    default void move(String from, String to) {
        Optional<Resource> source = load(from);
        if (source.isEmpty()) {
            return;
        }
        try {
            store(to, source.get().getContentAsByteArray());
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException("Can not move " + from, e);
        }
        delete(from);
    }
}
