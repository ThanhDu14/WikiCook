package com.wikicook.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** {@code password_check_failures} (FR-022b, R17): wrong passwords while signed in, counted apart from sign-in. */
@Entity
@Table(name = "password_check_failures")
public class PasswordCheckFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PasswordCheckFailure() {
    }

    public PasswordCheckFailure(Long userId, Instant createdAt) {
        this.userId = userId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
