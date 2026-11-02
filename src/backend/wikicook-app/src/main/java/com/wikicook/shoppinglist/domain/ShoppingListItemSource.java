package com.wikicook.shoppinglist.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "shopping_list_item_sources")
public class ShoppingListItemSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private ShoppingListItem shoppingListItem;

    @Column(name = "recipe_id")
    private Long recipeId;

    @Column(name = "meal_plan_item_id")
    private Long mealPlanItemId;

    @Column(name = "recipe_title_snapshot", nullable = false, length = 100)
    private String recipeTitleSnapshot;

    @Column(name = "contributed_quantity", precision = 10, scale = 3)
    private BigDecimal contributedQuantity;

    @Column(name = "unit_id")
    private Long unitId;

    @Transient
    private String unitName;

    @Column(name = "scaled_servings", nullable = false)
    private Integer scaledServings = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ShoppingListItemSource() {}

    public ShoppingListItemSource(ShoppingListItem shoppingListItem, Long recipeId, Long mealPlanItemId,
                                  String recipeTitleSnapshot, BigDecimal contributedQuantity, Long unitId, String unitName,
                                  Integer scaledServings) {
        this.shoppingListItem = shoppingListItem;
        this.recipeId = recipeId;
        this.mealPlanItemId = mealPlanItemId;
        this.recipeTitleSnapshot = recipeTitleSnapshot;
        this.contributedQuantity = contributedQuantity;
        this.unitId = unitId;
        this.unitName = unitName;
        this.scaledServings = scaledServings != null ? scaledServings : 1;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ShoppingListItem getShoppingListItem() { return shoppingListItem; }
    public void setShoppingListItem(ShoppingListItem shoppingListItem) { this.shoppingListItem = shoppingListItem; }

    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }

    public Long getMealPlanItemId() { return mealPlanItemId; }
    public void setMealPlanItemId(Long mealPlanItemId) { this.mealPlanItemId = mealPlanItemId; }

    public String getRecipeTitleSnapshot() { return recipeTitleSnapshot; }
    public void setRecipeTitleSnapshot(String recipeTitleSnapshot) { this.recipeTitleSnapshot = recipeTitleSnapshot; }

    public BigDecimal getContributedQuantity() { return contributedQuantity; }
    public void setContributedQuantity(BigDecimal contributedQuantity) { this.contributedQuantity = contributedQuantity; }

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }

    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }

    public Integer getScaledServings() { return scaledServings; }
    public void setScaledServings(Integer scaledServings) { this.scaledServings = scaledServings; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
