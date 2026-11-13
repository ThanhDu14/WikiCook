package com.wikicook.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * {@code user_sessions}: one refresh-token session. {@code id} is the {@code sid} claim of the access token; only the
 * SHA-256 of the refresh token is stored (research R1, R2). CHAR(64) columns need {@code SqlTypes.CHAR} or
 * {@code ddl-auto: validate} refuses to start (research R11).
 */
@Entity
@Table(name = "user_sessions")
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "previous_token_hash", length = 64)
    private String previousTokenHash;

    @Column(name = "rotated_at")
    private Instant rotatedAt;

    @Column(name = "remember_me", nullable = false, updatable = false)
    private boolean rememberMe;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "reauthenticated_at")
    private Instant reauthenticatedAt;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserSession() {
    }

    public UserSession(Long userId, String tokenHash, boolean rememberMe, Instant lastActivityAt, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.rememberMe = rememberMe;
        this.lastActivityAt = lastActivityAt;
        this.expiresAt = expiresAt;
        this.createdAt = lastActivityAt;
    }

    /** Issues a new refresh token: the current hash becomes {@code previous_token_hash} (R2). */
    public void rotate(String newTokenHash, Instant at) {
        this.previousTokenHash = this.tokenHash;
        this.tokenHash = newTokenHash;
        this.rotatedAt = at;
    }

    public void revoke(Instant at) {
        if (revokedAt == null) {
            revokedAt = at;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getPreviousTokenHash() {
        return previousTokenHash;
    }

    public Instant getRotatedAt() {
        return rotatedAt;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getReauthenticatedAt() {
        return reauthenticatedAt;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setReauthenticatedAt(Instant reauthenticatedAt) {
        this.reauthenticatedAt = reauthenticatedAt;
    }
}
