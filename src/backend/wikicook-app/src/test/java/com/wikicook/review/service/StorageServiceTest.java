package com.wikicook.review.service;

import com.wikicook.review.config.ReviewStorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        ReviewStorageProperties properties = new ReviewStorageProperties();
        properties.setRootPath(tempDir.toString());
        storageService = new LocalStorageServiceImpl(properties);
    }

    @Test
    @DisplayName("Store and retrieve full image and thumbnail successfully")
    void testStoreAndRetrieve() throws IOException {
        String key = "cs_105_test.webp";
        byte[] fullBytes = new byte[]{1, 2, 3, 4, 5};
        byte[] thumbBytes = new byte[]{6, 7, 8};

        storageService.store(key, fullBytes, thumbBytes);

        // Retrieve full image
        Resource fullResource = storageService.loadAsResource(key, false);
        assertThat(fullResource).isNotNull();
        assertThat(fullResource.exists()).isTrue();
        assertThat(fullResource.getContentAsByteArray()).isEqualTo(fullBytes);

        // Retrieve thumbnail
        Resource thumbResource = storageService.loadAsResource(key, true);
        assertThat(thumbResource).isNotNull();
        assertThat(thumbResource.exists()).isTrue();
        assertThat(thumbResource.getContentAsByteArray()).isEqualTo(thumbBytes);
    }

    @Test
    @DisplayName("Retrieve thumbnail falls back to full image if thumbnail not generated")
    void testThumbnailFallbackToFullImage() throws IOException {
        String key = "cs_no_thumb.webp";
        byte[] fullBytes = new byte[]{9, 10, 11};

        storageService.store(key, fullBytes, null);

        Resource thumbResource = storageService.loadAsResource(key, true);
        assertThat(thumbResource).isNotNull();
        assertThat(thumbResource.exists()).isTrue();
        assertThat(thumbResource.getContentAsByteArray()).isEqualTo(fullBytes);
    }

    @Test
    @DisplayName("Delete removes both full image and thumbnail from disk")
    void testDeleteRemovesFiles() throws IOException {
        String key = "cs_to_delete.webp";
        storageService.store(key, new byte[]{1, 2}, new byte[]{3, 4});

        assertThat(storageService.loadAsResource(key, false)).isNotNull();

        storageService.delete(key);

        assertThat(storageService.loadAsResource(key, false)).isNull();
        assertThat(storageService.loadAsResource(key, true)).isNull();
    }

    @Test
    @DisplayName("Register post-commit deletion outside active transaction immediately deletes files")
    void testPostCommitDeletionOutsideTransaction() throws IOException {
        String key = "cs_post_commit.webp";
        storageService.store(key, new byte[]{1}, new byte[]{2});

        storageService.registerPostCommitDeletion(key);

        assertThat(storageService.loadAsResource(key, false)).isNull();
    }
}

