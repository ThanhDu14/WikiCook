package com.wikicook.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "review_helpful_votes", uniqueConstraints = {
    @UniqueConstraint(name = "uq_review_helpful_votes", columnNames = {"review_id", "user_id"})
})
public class ReviewHelpfulVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    // NULL once the voter deletes their account (ON DELETE SET NULL)
    @Column(name = "user_id")
    private Long userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ReviewHelpfulVote() {
    }

    public ReviewHelpfulVote(Long id, Review review, Long userId, Instant createdAt) {
        this.id = id;
        this.review = review;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    public static ReviewHelpfulVoteBuilder builder() {
        return new ReviewHelpfulVoteBuilder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static class ReviewHelpfulVoteBuilder {
        private Long id;
        private Review review;
        private Long userId;
        private Instant createdAt;

        ReviewHelpfulVoteBuilder() {
        }

        public ReviewHelpfulVoteBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ReviewHelpfulVoteBuilder review(Review review) {
            this.review = review;
            return this;
        }

        public ReviewHelpfulVoteBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public ReviewHelpfulVoteBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ReviewHelpfulVote build() {
            return new ReviewHelpfulVote(id, review, userId, createdAt);
        }
    }
}
