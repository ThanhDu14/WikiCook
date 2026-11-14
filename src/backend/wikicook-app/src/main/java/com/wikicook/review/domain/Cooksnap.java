package com.wikicook.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "cooksnaps")
public class Cooksnap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(name = "user_id")
    private Long userId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "storage_key", nullable = false, length = 255, unique = true)
    private String storageKey;

    @NotBlank
    @Size(max = 50)
    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @NotNull
    @Positive
    @Max(5242880) // 5 MB limit
    @Column(name = "size_bytes", nullable = false)
    private Integer sizeBytes;

    @Size(max = 300)
    @Column(length = 300)
    private String caption;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReviewStatus status = ReviewStatus.VISIBLE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Cooksnap() {
    }

    public Cooksnap(Long id, Long recipeId, Review review, Long userId, String storageKey, String contentType, Integer sizeBytes, String caption, ReviewStatus status, Instant createdAt) {
        this.id = id;
        this.recipeId = recipeId;
        this.review = review;
        this.userId = userId;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.caption = caption;
        this.status = status != null ? status : ReviewStatus.VISIBLE;
        this.createdAt = createdAt;
    }

    public static CooksnapBuilder builder() {
        return new CooksnapBuilder();
    }

    public void hide() {
        this.status = ReviewStatus.HIDDEN;
    }

    public void restore() {
        this.status = ReviewStatus.VISIBLE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Long recipeId) {
        this.recipeId = recipeId;
    }

    public Review getReview() {
        return review;
    }

    public void setReview(Review review) {
        this.review = review;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Integer getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Integer sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static class CooksnapBuilder {
        private Long id;
        private Long recipeId;
        private Review review;
        private Long userId;
        private String storageKey;
        private String contentType;
        private Integer sizeBytes;
        private String caption;
        private ReviewStatus status = ReviewStatus.VISIBLE;
        private Instant createdAt;

        CooksnapBuilder() {
        }

        public CooksnapBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public CooksnapBuilder recipeId(Long recipeId) {
            this.recipeId = recipeId;
            return this;
        }

        public CooksnapBuilder review(Review review) {
            this.review = review;
            return this;
        }

        public CooksnapBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public CooksnapBuilder storageKey(String storageKey) {
            this.storageKey = storageKey;
            return this;
        }

        public CooksnapBuilder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public CooksnapBuilder sizeBytes(Integer sizeBytes) {
            this.sizeBytes = sizeBytes;
            return this;
        }

        public CooksnapBuilder caption(String caption) {
            this.caption = caption;
            return this;
        }

        public CooksnapBuilder status(ReviewStatus status) {
            this.status = status;
            return this;
        }

        public CooksnapBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Cooksnap build() {
            return new Cooksnap(id, recipeId, review, userId, storageKey, contentType, sizeBytes, caption, status, createdAt);
        }
    }
}
