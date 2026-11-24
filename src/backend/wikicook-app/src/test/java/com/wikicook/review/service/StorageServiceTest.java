package com.wikicook.review.service;

import com.wikicook.common.media.LocalDiskMediaStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Cooksnap files over the shared media store; the local disk store stands in for S3, which shares its contract. */
class StorageServiceTest {

    @TempDir
    Path tempDir;

    private MediaStorageCooksnapService storageService;

    @BeforeEach
    void setUp() {
        storageService = new MediaStorageCooksnapService(new LocalDiskMediaStorage(tempDir));
    }

    @Test
    @DisplayName("Store and retrieve full image and thumbnail successfully, under cooksnaps/")
    void testStoreAndRetrieve() throws IOException {
        String key = "cs_105_test.webp";
        byte[] fullBytes = new byte[]{1, 2, 3, 4, 5};
        byte[] thumbBytes = new byte[]{6, 7, 8};

        storageService.store(key, fullBytes, thumbBytes);

        Resource fullResource = storageService.loadAsResource(key, false);
        assertThat(fullResource).isNotNull();
        assertThat(fullResource.exists()).isTrue();
        assertThat(fullResource.getContentAsByteArray()).isEqualTo(fullBytes);

        Resource thumbResource = storageService.loadAsResource(key, true);
        assertThat(thumbResource).isNotNull();
        assertThat(thumbResource.getContentAsByteArray()).isEqualTo(thumbBytes);

        assertThat(tempDir.resolve("cooksnaps/cs_105_test.webp")).exists();
        assertThat(tempDir.resolve("cooksnaps/thumb_cs_105_test.webp")).exists();
    }

    @Test
    @DisplayName("Without a thumbnail the full image is stored as the thumbnail, so the CDN thumbnail URL works")
    void testThumbnailFallbackToFullImage() throws IOException {
        String key = "cs_no_thumb.webp";
        byte[] fullBytes = new byte[]{9, 10, 11};

        storageService.store(key, fullBytes, null);

        Resource thumbResource = storageService.loadAsResource(key, true);
        assertThat(thumbResource).isNotNull();
        assertThat(thumbResource.getContentAsByteArray()).isEqualTo(fullBytes);
        assertThat(tempDir.resolve("cooksnaps/thumb_cs_no_thumb.webp")).exists();
    }

    @Test
    @DisplayName("Delete removes both full image and thumbnail")
    void testDeleteRemovesFiles() {
        String key = "cs_to_delete.webp";
        storageService.store(key, new byte[]{1, 2}, new byte[]{3, 4});

        assertThat(storageService.loadAsResource(key, false)).isNotNull();

        storageService.delete(key);

        assertThat(storageService.loadAsResource(key, false)).isNull();
        assertThat(storageService.loadAsResource(key, true)).isNull();
    }

    @Test
    @DisplayName("Register post-commit deletion outside active transaction immediately deletes files")
    void testPostCommitDeletionOutsideTransaction() {
        String key = "cs_post_commit.webp";
        storageService.store(key, new byte[]{1}, new byte[]{2});

        storageService.registerPostCommitDeletion(key);

        assertThat(storageService.loadAsResource(key, false)).isNull();
    }

    @Test
    @DisplayName("Hide moves the files under hidden/ (off the CDN paths); restore brings them back")
    void testHideAndRestore() throws IOException {
        String key = "cs_hidden.webp";
        storageService.store(key, new byte[]{1, 2}, new byte[]{3});

        storageService.registerPostCommitHide(key);

        assertThat(storageService.loadAsResource(key, false)).isNull();
        assertThat(storageService.loadAsResource(key, true)).isNull();
        assertThat(tempDir.resolve("hidden/cooksnaps/cs_hidden.webp")).exists();
        assertThat(tempDir.resolve("hidden/cooksnaps/thumb_cs_hidden.webp")).exists();

        storageService.registerPostCommitRestore(key);

        assertThat(storageService.loadAsResource(key, false).getContentAsByteArray()).containsExactly(1, 2);
        assertThat(storageService.loadAsResource(key, true).getContentAsByteArray()).containsExactly(3);
        assertThat(tempDir.resolve("hidden/cooksnaps/cs_hidden.webp")).doesNotExist();
    }

    @Test
    @DisplayName("Hiding twice, or deleting a hidden Cooksnap, is not an error and leaves nothing behind")
    void testHideTwiceThenDelete() {
        String key = "cs_twice.webp";
        storageService.store(key, new byte[]{1}, new byte[]{2});

        storageService.registerPostCommitHide(key);
        storageService.registerPostCommitHide(key);
        storageService.delete(key);

        assertThat(tempDir.resolve("hidden/cooksnaps/cs_twice.webp")).doesNotExist();
        assertThat(tempDir.resolve("cooksnaps/cs_twice.webp")).doesNotExist();
    }
}
