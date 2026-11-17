package com.wikicook.profile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Immutable;

/**
 * A catalogue change the owner has not seen yet (FR-019, research R15). Read-only here: the rows are written in the
 * transaction of the catalogue change itself, and deleted when the owner closes them.
 */
@Entity
@Table(name = "dietary_notices")
@Immutable
public class DietaryNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private NoticeKind kind;

    @Column(name = "subject_name", nullable = false, length = 100)
    private String subjectName;

    @Column(name = "replacement_name", length = 100)
    private String replacementName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DietaryNotice() {
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public NoticeKind getKind() {
        return kind;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public String getReplacementName() {
        return replacementName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
