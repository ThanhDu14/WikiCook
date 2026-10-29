package com.wikicook.shoppinglist.domain;

import java.io.Serializable;
import java.util.Objects;

public class UserAislePreferenceId implements Serializable {
    private Long userId;
    private Long ingredientId;

    public UserAislePreferenceId() {}

    public UserAislePreferenceId(Long userId, Long ingredientId) {
        this.userId = userId;
        this.ingredientId = ingredientId;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getIngredientId() { return ingredientId; }
    public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserAislePreferenceId that = (UserAislePreferenceId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(ingredientId, that.ingredientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, ingredientId);
    }
}
