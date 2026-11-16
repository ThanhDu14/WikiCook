package com.wikicook.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** {@code user_suspensions}: a suspension (has {@code ends_at}) or a ban (does not); at most one is running per user. */
@Entity
@Table(name = "user_suspensions")
public class UserSuspension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private SanctionKind kind;

    @Column(nullable = false, updatable = false, length = 500)
    private String reason;

    @Column(name = "starts_at", nullable = false, updatable = false)
    private Instant startsAt;

    @Column(name = "ends_at", updatable = false)
    private Instant endsAt;

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @Column(name = "lifted_at")
    private Instant liftedAt;

    @Column(name = "lifted_by")
    private Long liftedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "lift_reason", length = 20)
    private LiftReason liftReason;

    protected UserSuspension() {
    }

    public UserSuspension(Long userId, SanctionKind kind, String reason, Instant startsAt, Instant endsAt,
            Long createdBy) {
        this.userId = userId;
        this.kind = kind;
        this.reason = reason;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdBy = createdBy;
    }

    /** Closes the sanction; {@code liftedBy} is null when it ended by itself or was replaced by a ban. */
    public void lift(Instant at, Long liftedBy, LiftReason reason) {
        this.liftedAt = at;
        this.liftedBy = liftedBy;
        this.liftReason = reason;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public SanctionKind getKind() {
        return kind;
    }

    public String getReason() {
        return reason;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Instant getLiftedAt() {
        return liftedAt;
    }

    public Long getLiftedBy() {
        return liftedBy;
    }

    public LiftReason getLiftReason() {
        return liftReason;
    }
}
