package com.wikicook.review.service;

import org.springframework.core.io.Resource;

import java.io.IOException;

public interface StorageService {

    /**
     * Store the full resolution image and thumbnail on disk under the given storageKey.
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
}

