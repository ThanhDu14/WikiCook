package com.wikicook.review.domain;

import com.wikicook.review.dto.ReportReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id")
    private Long reporterId;

    @Column(name = "recipe_id")
    private Long recipeId;

    @Column(name = "review_id")
    private Long reviewId;

    @Column(name = "cooksnap_id")
    private Long cooksnapId;

    @Column(name = "reported_user_id")
    private Long reportedUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 15)
    private ReportReason reason;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "status", nullable = false, length = 10)
    private String status = "OPEN";

    @Column(name = "handled_by")
    private Long handledBy;

    @Column(name = "handled_at")
    private Instant handledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Report() {
    }

    public Report(Long id, Long reporterId, Long recipeId, Long reviewId, Long cooksnapId,
                  Long reportedUserId, ReportReason reason, String details, String status,
                  Long handledBy, Instant handledAt, Instant createdAt) {
        this.id = id;
        this.reporterId = reporterId;
        this.recipeId = recipeId;
        this.reviewId = reviewId;
        this.cooksnapId = cooksnapId;
        this.reportedUserId = reportedUserId;
        this.reason = reason;
        this.details = details;
        this.status = status != null ? status : "OPEN";
        this.handledBy = handledBy;
        this.handledAt = handledAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long reporterId;
        private Long recipeId;
        private Long reviewId;
        private Long cooksnapId;
        private Long reportedUserId;
        private ReportReason reason;
        private String details;
        private String status = "OPEN";
        private Long handledBy;
        private Instant handledAt;
        private Instant createdAt = Instant.now();

        public Builder id(Long id) { this.id = id; return this; }
        public Builder reporterId(Long reporterId) { this.reporterId = reporterId; return this; }
        public Builder recipeId(Long recipeId) { this.recipeId = recipeId; return this; }
        public Builder reviewId(Long reviewId) { this.reviewId = reviewId; return this; }
        public Builder cooksnapId(Long cooksnapId) { this.cooksnapId = cooksnapId; return this; }
        public Builder reportedUserId(Long reportedUserId) { this.reportedUserId = reportedUserId; return this; }
        public Builder reason(ReportReason reason) { this.reason = reason; return this; }
        public Builder details(String details) { this.details = details; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder handledBy(Long handledBy) { this.handledBy = handledBy; return this; }
        public Builder handledAt(Instant handledAt) { this.handledAt = handledAt; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public Report build() {
            return new Report(id, reporterId, recipeId, reviewId, cooksnapId, reportedUserId,
                    reason, details, status, handledBy, handledAt, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getReporterId() { return reporterId; }
    public void setReporterId(Long reporterId) { this.reporterId = reporterId; }

    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }

    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }

    public Long getCooksnapId() { return cooksnapId; }
    public void setCooksnapId(Long cooksnapId) { this.cooksnapId = cooksnapId; }

    public Long getReportedUserId() { return reportedUserId; }
    public void setReportedUserId(Long reportedUserId) { this.reportedUserId = reportedUserId; }

    public ReportReason getReason() { return reason; }
    public void setReason(ReportReason reason) { this.reason = reason; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getHandledBy() { return handledBy; }
    public void setHandledBy(Long handledBy) { this.handledBy = handledBy; }

    public Instant getHandledAt() { return handledAt; }
    public void setHandledAt(Instant handledAt) { this.handledAt = handledAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

