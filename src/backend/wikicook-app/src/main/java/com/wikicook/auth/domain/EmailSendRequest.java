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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * {@code email_send_requests} (FR-017): one row per verification or reset email asked for an address. No user id, so
 * addresses without an account are counted too and a 429 does not reveal whether an account exists (research R5).
 */
@Entity
@Table(name = "email_send_requests")
public class EmailSendRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "email_hash", nullable = false, updatable = false, length = 64)
    private String emailHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private TokenPurpose purpose;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EmailSendRequest() {
    }

    public EmailSendRequest(String emailHash, TokenPurpose purpose, Instant createdAt) {
        this.emailHash = emailHash;
        this.purpose = purpose;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getEmailHash() {
        return emailHash;
    }

    public TokenPurpose getPurpose() {
        return purpose;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
