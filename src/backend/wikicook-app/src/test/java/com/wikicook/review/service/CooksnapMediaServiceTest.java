package com.wikicook.review.service;

import com.wikicook.review.domain.Cooksnap;
import com.wikicook.review.domain.Review;
import com.wikicook.review.domain.ReviewStatus;
import com.wikicook.review.exception.ReviewNotFoundException;
import com.wikicook.review.repository.CooksnapRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CooksnapMediaServiceTest {

    @Mock
    private CooksnapRepository cooksnapRepository;

    @Mock
    private StorageService storageService;

    private CooksnapMediaService mediaService;

    @BeforeEach
    void setUp() {
        mediaService = new CooksnapMediaService(cooksnapRepository, storageService);
    }

    @Test
    @DisplayName("Get media succeeds when both Cooksnap and parent Review are VISIBLE")
    void testGetMediaSuccess() {
        String storageKey = "cs_105_123.webp";
        Review review = Review.builder().id(10L).status(ReviewStatus.VISIBLE).build();
        Cooksnap cooksnap = Cooksnap.builder()
                .id(1L)
                .storageKey(storageKey)
                .contentType("image/webp")
                .status(ReviewStatus.VISIBLE)
                .review(review)
                .build();

        Resource mockResource = new ByteArrayResource(new byte[]{1, 2, 3});
        when(cooksnapRepository.findByStorageKey(storageKey)).thenReturn(Optional.of(cooksnap));
        when(storageService.loadAsResource(storageKey, false)).thenReturn(mockResource);

        CooksnapMediaService.MediaResult result = mediaService.getMedia(storageKey, false);

        assertThat(result).isNotNull();
        assertThat(result.contentType()).isEqualTo("image/webp");
        assertThat(result.contentLength()).isEqualTo(3L);
        assertThat(result.resource()).isEqualTo(mockResource);
    }

    @Test
    @DisplayName("Get media throws 404 when Cooksnap is not found in database")
    void testGetMediaNotFound() {
        String storageKey = "cs_missing.webp";
        when(cooksnapRepository.findByStorageKey(storageKey)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaService.getMedia(storageKey, false))
                .isInstanceOf(ReviewNotFoundException.class)
                .hasMessageContaining("Không tìm thấy ảnh Cooksnap");
    }

    @Test
    @DisplayName("Security Gate: Throws 404 when Cooksnap is HIDDEN")
    void testGetMediaCooksnapHiddenThrows404() {
        String storageKey = "cs_hidden.webp";
        Review review = Review.builder().id(10L).status(ReviewStatus.VISIBLE).build();
        Cooksnap cooksnap = Cooksnap.builder()
                .id(1L)
                .storageKey(storageKey)
                .status(ReviewStatus.HIDDEN)
                .review(review)
                .build();

        when(cooksnapRepository.findByStorageKey(storageKey)).thenReturn(Optional.of(cooksnap));

        assertThatThrownBy(() -> mediaService.getMedia(storageKey, false))
                .isInstanceOf(ReviewNotFoundException.class)
                .hasMessageContaining("Không tìm thấy ảnh Cooksnap");
    }

    @Test
    @DisplayName("Security Gate: Throws 404 when parent Review is HIDDEN even if Cooksnap is VISIBLE")
    void testGetMediaParentReviewHiddenThrows404() {
        String storageKey = "cs_quarantine.webp";
        Review review = Review.builder().id(10L).status(ReviewStatus.HIDDEN).build();
        Cooksnap cooksnap = Cooksnap.builder()
                .id(1L)
                .storageKey(storageKey)
                .status(ReviewStatus.VISIBLE)
                .review(review)
                .build();

        when(cooksnapRepository.findByStorageKey(storageKey)).thenReturn(Optional.of(cooksnap));

        assertThatThrownBy(() -> mediaService.getMedia(storageKey, false))
                .isInstanceOf(ReviewNotFoundException.class)
                .hasMessageContaining("Không tìm thấy ảnh Cooksnap");
    }

    @Test
    @DisplayName("Throws 404 when storage resource does not exist on disk")
    void testGetMediaResourceMissingOnDiskThrows404() {
        String storageKey = "cs_file_lost.webp";
        Review review = Review.builder().id(10L).status(ReviewStatus.VISIBLE).build();
        Cooksnap cooksnap = Cooksnap.builder()
                .id(1L)
                .storageKey(storageKey)
                .contentType("image/webp")
                .status(ReviewStatus.VISIBLE)
                .review(review)
                .build();

        when(cooksnapRepository.findByStorageKey(storageKey)).thenReturn(Optional.of(cooksnap));
        when(storageService.loadAsResource(storageKey, false)).thenReturn(null);

        assertThatThrownBy(() -> mediaService.getMedia(storageKey, false))
                .isInstanceOf(ReviewNotFoundException.class)
                .hasMessageContaining("Không tìm thấy file ảnh trên hệ thống lưu trữ");
    }
}

