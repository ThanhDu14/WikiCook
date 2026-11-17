package com.wikicook.review.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Column(name = "user_id")
    private Long userId;

    @NotNull
    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Short rating;

    @NotBlank
    @Size(min = 10, max = 2000)
    @Column(length = 2000)
    private String comment;

    @Size(max = 500)
    @Column(name = "substitution_tip", length = 500)
    private String substitutionTip;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReviewStatus status = ReviewStatus.VISIBLE;

    // Written only by the native recount query of the helpful-vote toggle (§1.3)
    @Min(0)
    @Column(name = "helpful_count", nullable = false, insertable = false, updatable = false)
    private Integer helpfulCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Set explicitly in editContent(); no @UpdateTimestamp and no DB trigger
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cooksnap> cooksnaps = new ArrayList<>();

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewHelpfulVote> helpfulVotes = new ArrayList<>();

    public Review() {
    }

    public Review(Long id, Long recipeId, Long userId, Short rating, String comment, String substitutionTip,
                  ReviewStatus status, Integer helpfulCount, Instant createdAt, Instant updatedAt,
                  List<Cooksnap> cooksnaps, List<ReviewHelpfulVote> helpfulVotes) {
        this.id = id;
        this.recipeId = recipeId;
        this.userId = userId;
        this.rating = rating;
        this.comment = comment;
        this.substitutionTip = substitutionTip;
        this.status = status != null ? status : ReviewStatus.VISIBLE;
        this.helpfulCount = helpfulCount != null ? helpfulCount : 0;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        if (cooksnaps != null) {
            this.cooksnaps = cooksnaps;
        }
        if (helpfulVotes != null) {
            this.helpfulVotes = helpfulVotes;
        }
    }

    public static ReviewBuilder builder() {
        return new ReviewBuilder();
    }

    // Domain helper methods
    public void addCooksnap(Cooksnap cooksnap) {
        cooksnaps.add(cooksnap);
        cooksnap.setReview(this);
    }

    public void removeCooksnap(Cooksnap cooksnap) {
        cooksnaps.remove(cooksnap);
        cooksnap.setReview(null);
    }

    /** Author edit: the only place that moves updatedAt. */
    public void editContent(Short rating, String comment, String substitutionTip) {
        this.rating = rating;
        this.comment = comment;
        this.substitutionTip = substitutionTip;
        this.updatedAt = Instant.now();
    }

    /** Moderation only (manual hide / auto-quarantine). Does not touch updatedAt. */
    public void hide() {
        this.status = ReviewStatus.HIDDEN;
        cooksnaps.forEach(Cooksnap::hide);
    }

    public void restore() {
        this.status = ReviewStatus.VISIBLE;
        cooksnaps.forEach(Cooksnap::restore);
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Short getRating() {
        return rating;
    }

    public void setRating(Short rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getSubstitutionTip() {
        return substitutionTip;
    }

    public void setSubstitutionTip(String substitutionTip) {
        this.substitutionTip = substitutionTip;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public Integer getHelpfulCount() {
        return helpfulCount;
    }

    public void setHelpfulCount(Integer helpfulCount) {
        this.helpfulCount = helpfulCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Cooksnap> getCooksnaps() {
        return cooksnaps;
    }

    public void setCooksnaps(List<Cooksnap> cooksnaps) {
        this.cooksnaps = cooksnaps;
    }

    public List<ReviewHelpfulVote> getHelpfulVotes() {
        return helpfulVotes;
    }

    public void setHelpfulVotes(List<ReviewHelpfulVote> helpfulVotes) {
        this.helpfulVotes = helpfulVotes;
    }

    public static class ReviewBuilder {
        private Long id;
        private Long recipeId;
        private Long userId;
        private Short rating;
        private String comment;
        private String substitutionTip;
        private ReviewStatus status = ReviewStatus.VISIBLE;
        private Integer helpfulCount = 0;
        private Instant createdAt;
        private Instant updatedAt = Instant.now();
        private List<Cooksnap> cooksnaps = new ArrayList<>();
        private List<ReviewHelpfulVote> helpfulVotes = new ArrayList<>();

        ReviewBuilder() {
        }

        public ReviewBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ReviewBuilder recipeId(Long recipeId) {
            this.recipeId = recipeId;
            return this;
        }

        public ReviewBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public ReviewBuilder rating(Short rating) {
            this.rating = rating;
            return this;
        }

        public ReviewBuilder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public ReviewBuilder substitutionTip(String substitutionTip) {
            this.substitutionTip = substitutionTip;
            return this;
        }

        public ReviewBuilder status(ReviewStatus status) {
            this.status = status;
            return this;
        }

        public ReviewBuilder helpfulCount(Integer helpfulCount) {
            this.helpfulCount = helpfulCount;
            return this;
        }

        public ReviewBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ReviewBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ReviewBuilder cooksnaps(List<Cooksnap> cooksnaps) {
            this.cooksnaps = cooksnaps;
            return this;
        }

        public ReviewBuilder helpfulVotes(List<ReviewHelpfulVote> helpfulVotes) {
            this.helpfulVotes = helpfulVotes;
            return this;
        }

        public Review build() {
            return new Review(id, recipeId, userId, rating, comment, substitutionTip, status, helpfulCount, createdAt, updatedAt, cooksnaps, helpfulVotes);
        }
    }
}
