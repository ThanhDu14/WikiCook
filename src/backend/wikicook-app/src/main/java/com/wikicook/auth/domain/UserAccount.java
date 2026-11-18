package com.wikicook.auth.domain;

import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * {@code users}, only the columns spec 010 owns. {@code diet_type}, {@code household_size} and
 * {@code max_cook_time_minutes} belong to 011 and are deliberately not mapped; {@code avatar_storage_key} is mapped
 * read-only so {@code /auth/me} shows the picture without a second query. {@link DynamicUpdate} writes only the
 * changed columns, so a 010 update never overwrites a {@code display_name} that 011 just changed (data-model.md).
 */
@Entity
@Table(name = "users")
@DynamicUpdate
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "google_sub")
    private String googleSub;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Role role = Role.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_sign_in_at")
    private Instant lastSignInAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "pending_role_notice_from", length = 15)
    private Role pendingRoleNoticeFrom;

    /** Owned by 011: never inserted or updated from here, only read. */
    @Column(name = "avatar_storage_key", insertable = false, updatable = false)
    private String avatarStorageKey;

    @Column(name = "is_system", nullable = false)
    private boolean system;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserAccount() {
    }

    /** A new, unverified Member (FR-001, FR-003); {@code email} must already be normalized (FR-002). */
    public UserAccount(String email, String passwordHash, String displayName, Instant now) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getLastSignInAt() {
        return lastSignInAt;
    }

    public Role getPendingRoleNoticeFrom() {
        return pendingRoleNoticeFrom;
    }

    public String getAvatarStorageKey() {
        return avatarStorageKey;
    }

    public boolean isSystem() {
        return system;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setGoogleSub(String googleSub) {
        this.googleSub = googleSub;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public void setEmailVerifiedAt(Instant emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public void setLastSignInAt(Instant lastSignInAt) {
        this.lastSignInAt = lastSignInAt;
    }

    public void setPendingRoleNoticeFrom(Role pendingRoleNoticeFrom) {
        this.pendingRoleNoticeFrom = pendingRoleNoticeFrom;
    }
}
