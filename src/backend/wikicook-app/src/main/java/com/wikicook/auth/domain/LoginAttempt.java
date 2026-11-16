package com.wikicook.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.net.InetAddress;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * {@code login_attempts}: the lockout counter of FR-012, kept for one day. Counts by the hash of the email, also for
 * emails with no account (research R4); the audit trail is {@code audit_logs}.
 */
@Entity
@Table(name = "login_attempts")
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null when the email has no account. */
    @Column(name = "user_id", updatable = false)
    private Long userId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "email_hash", nullable = false, updatable = false, length = 64)
    private String emailHash;

    @Column(nullable = false)
    private boolean success;

    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "ip_address", updatable = false)
    private InetAddress ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LoginAttempt() {
    }

    public LoginAttempt(Long userId, String emailHash, boolean success, InetAddress ipAddress, Instant createdAt) {
        this.userId = userId;
        this.emailHash = emailHash;
        this.success = success;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmailHash() {
        return emailHash;
    }

    public boolean isSuccess() {
        return success;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
