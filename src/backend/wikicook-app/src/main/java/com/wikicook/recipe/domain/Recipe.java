package com.wikicook.recipe.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id")
    private Long authorId;

    @Column(length = 100)
    private String title;

    @Column(length = 20)
    private String status = "PUBLISHED";

    /**
     * Rating average is computed exclusively via atomic SQL aggregation query.
     * Mapped read-only to prevent stale JPA entities from overwriting community ratings.
     */
    @Column(name = "rating_avg", insertable = false, updatable = false)
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    /**
     * Review count is computed exclusively via atomic SQL aggregation query.
     * Mapped read-only to prevent stale JPA entities from overwriting community ratings.
     */
    @Column(name = "review_count", insertable = false, updatable = false)
    private Integer reviewCount = 0;

    @Version
    private Integer version = 0;

    public Recipe() {
    }

    public Recipe(Long id, Long authorId, String title, String status) {
        this.id = id;
        this.authorId = authorId;
        this.title = title;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getRatingAvg() {
        return ratingAvg;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}

