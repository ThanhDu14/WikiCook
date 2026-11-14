package com.wikicook.review.service;

import com.wikicook.review.domain.Cooksnap;
import com.wikicook.review.domain.ReviewStatus;
import com.wikicook.review.exception.ReviewNotFoundException;
import com.wikicook.review.repository.CooksnapRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CooksnapMediaService {

    private final CooksnapRepository cooksnapRepository;
    private final StorageService storageService;

    public CooksnapMediaService(CooksnapRepository cooksnapRepository, StorageService storageService) {
        this.cooksnapRepository = cooksnapRepository;
        this.storageService = storageService;
    }

    public record MediaResult(
            Resource resource,
            String contentType,
            long contentLength
    ) {}

    @Transactional(readOnly = true)
    public MediaResult getMedia(String storageKey, boolean thumbnail) {
        Cooksnap cooksnap = cooksnapRepository.findByStorageKey(storageKey)
                .orElseThrow(() -> new ReviewNotFoundException("Không tìm thấy ảnh Cooksnap"));

        // Critical Security Gate: Both Cooksnap and its Review must be VISIBLE
        if (cooksnap.getStatus() != ReviewStatus.VISIBLE ||
                cooksnap.getReview() == null ||
                cooksnap.getReview().getStatus() != ReviewStatus.VISIBLE) {
            throw new ReviewNotFoundException("Không tìm thấy ảnh Cooksnap");
        }

        Resource resource = storageService.loadAsResource(storageKey, thumbnail);
        if (resource == null || !resource.exists()) {
            throw new ReviewNotFoundException("Không tìm thấy file ảnh trên hệ thống lưu trữ");
        }

        try {
            return new MediaResult(resource, cooksnap.getContentType(), resource.contentLength());
        } catch (Exception e) {
            return new MediaResult(resource, cooksnap.getContentType(), -1L);
        }
    }
}

