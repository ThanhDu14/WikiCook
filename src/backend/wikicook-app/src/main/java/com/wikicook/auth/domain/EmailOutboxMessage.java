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
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** {@code email_outbox}: a notification email waiting to be sent (research R8); the address is read from users then. */
@Entity
@Table(name = "email_outbox")
public class EmailOutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private MailTemplate template;

    /** Reason and dates only, never a token. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false)
    private Map<String, Object> payload;

    @Column(nullable = false)
    private short attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EmailOutboxMessage() {
    }

    public EmailOutboxMessage(Long userId, MailTemplate template, Map<String, Object> payload, Instant nextAttemptAt,
            Instant createdAt) {
        this.userId = userId;
        this.template = template;
        this.payload = payload;
        this.nextAttemptAt = nextAttemptAt;
        this.createdAt = createdAt;
    }

    public void markSent(Instant at) {
        this.sentAt = at;
    }

    /** Failed attempt: retry at {@code retryAt}, or give up when it is null. */
    public void markFailed(Instant at, Instant retryAt) {
        attempts++;
        if (retryAt == null) {
            failedAt = at;
        } else {
            nextAttemptAt = retryAt;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public MailTemplate getTemplate() {
        return template;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public short getAttempts() {
        return attempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
