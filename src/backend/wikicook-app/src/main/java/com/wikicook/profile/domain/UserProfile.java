package com.wikicook.profile.domain;

import com.wikicook.mealplan.domain.DietType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.DynamicUpdate;

/**
 * The columns of {@code users} that spec 011 owns (data-model.md, research R4); {@code UserAccount} of 010 maps the
 * rest of the same row. Neither entity inserts: the row is created at registration. {@code @DynamicUpdate} writes only
 * the columns that changed, so a save here can not put a stale value over what 010 or the dietary writer stored
 * meanwhile. Email, role, status and the hash are not mapped on purpose: they are read with a projection and the hash
 * is written only by {@code UserCredentialRepository}.
 *
 * <p>{@code profileVersion} is the optimistic lock of the public profile form (Edge: Two tabs). The dietary form has its
 * own {@code dietaryVersion}, checked in SQL by {@code DietaryLinkRepository}, so saving an avatar never reports a
 * conflict to it.
 */
@Entity
@Table(name = "users")
@DynamicUpdate
public class UserProfile {

    @Id
    private Long id;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(name = "bio", length = 300)
    private String bio;

    @Column(name = "avatar_storage_key", length = 255)
    private String avatarStorageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "diet_type", nullable = false, length = 15)
    private DietType dietType;

    @Column(name = "household_size")
    private Short householdSize;

    @Column(name = "max_cook_time_minutes")
    private Short maxCookTimeMinutes;

    @Version
    @Column(name = "profile_version", nullable = false)
    private int profileVersion;

    @Column(name = "dietary_version", nullable = false)
    private int dietaryVersion;

    protected UserProfile() {
    }

    public Long getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarStorageKey() {
        return avatarStorageKey;
    }

    public void setAvatarStorageKey(String avatarStorageKey) {
        this.avatarStorageKey = avatarStorageKey;
    }

    public DietType getDietType() {
        return dietType;
    }

    public Short getHouseholdSize() {
        return householdSize;
    }

    public Short getMaxCookTimeMinutes() {
        return maxCookTimeMinutes;
    }

    public int getProfileVersion() {
        return profileVersion;
    }

    public int getDietaryVersion() {
        return dietaryVersion;
    }
}
