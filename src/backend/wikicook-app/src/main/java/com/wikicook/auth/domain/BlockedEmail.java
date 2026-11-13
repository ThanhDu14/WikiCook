package com.wikicook.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** {@code blocked_emails} (FR-026): HMAC-SHA256 of an email that may not register again; no email in clear. */
@Entity
@Table(name = "blocked_emails")
public class BlockedEmail {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "email_hash", nullable = false, length = 64)
    private String emailHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BlockedEmail() {
    }

    public BlockedEmail(String emailHash, Instant createdAt) {
        this.emailHash = emailHash;
        this.createdAt = createdAt;
    }

    public String getEmailHash() {
        return emailHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
