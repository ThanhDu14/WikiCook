package com.wikicook.review.service;

import org.springframework.core.io.Resource;

import java.io.IOException;

public interface StorageService {

    /**
     * Store the full resolution image and thumbnail under the given storageKey (local disk or S3).
     */
    void store(String storageKey, byte[] fullImageBytes, byte[] thumbnailBytes) throws IOException;

    /**
     * Load image or thumbnail as a Spring Resource.
     */
    Resource loadAsResource(String storageKey, boolean thumbnail);

    /**
     * Delete both the full image and its thumbnail immediately.
     */
    void delete(String storageKey);

    /**
     * Schedule deletion of written files if the current transaction rolls back.
     */
    void registerRollbackDeletion(String storageKey);

    /**
     * Schedule deletion of removed files after the current transaction successfully commits.
     */
    void registerPostCommitDeletion(String storageKey);

    /**
     * After the current transaction commits, take the files of a hidden Cooksnap off the public paths (served by the
     * CDN) while keeping them for a later restore.
     */
    void registerPostCommitHide(String storageKey);

    /** After the current transaction commits, put the files of a restored Cooksnap back on the public paths. */
    void registerPostCommitRestore(String storageKey);
}

