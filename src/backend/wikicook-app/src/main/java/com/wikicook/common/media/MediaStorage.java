package com.wikicook.common.media;

import java.util.Optional;
import org.springframework.core.io.Resource;

/**
 * Where uploaded files live (research R7). Shared by the avatars of 011 and the recipe and cooksnap photos of 060 and
 * 070, so a later move to object storage touches one class. A key is a relative path such as
 * {@code avatars/<uuid>.jpg}; a key that is not a plain relative path is an {@link IllegalArgumentException}.
 */
public interface MediaStorage {

    /** Writes {@code content} under {@code key}; readers never see a half-written file. */
    void store(String key, byte[] content);

    /** The file under {@code key}, or empty when there is none. */
    Optional<Resource> load(String key);

    /** Removes the file; nothing happens when it is already gone. */
    void delete(String key);
}
