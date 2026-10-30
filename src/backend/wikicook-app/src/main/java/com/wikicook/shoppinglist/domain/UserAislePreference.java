package com.wikicook.shoppinglist.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_aisle_preferences")
@IdClass(UserAislePreferenceId.class)
public class UserAislePreference {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "ingredient_id", nullable = false)
    private Long ingredientId;

    @Column(name = "aisle_category_id", nullable = false)
    private Long aisleCategoryId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UserAislePreference() {}

    public UserAislePreference(Long userId, Long ingredientId, Long aisleCategoryId) {
        this.userId = userId;
        this.ingredientId = ingredientId;
        this.aisleCategoryId = aisleCategoryId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getIngredientId() { return ingredientId; }
    public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }

    public Long getAisleCategoryId() { return aisleCategoryId; }
    public void setAisleCategoryId(Long aisleCategoryId) { this.aisleCategoryId = aisleCategoryId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
