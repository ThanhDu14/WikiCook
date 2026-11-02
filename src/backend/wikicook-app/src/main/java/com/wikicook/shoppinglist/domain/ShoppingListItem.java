package com.wikicook.shoppinglist.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shopping_list_items")
public class ShoppingListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    private ShoppingList shoppingList;

    @Column(name = "client_uuid", nullable = false, unique = true)
    private UUID clientUuid;

    @Column(name = "ingredient_id")
    private Long ingredientId;

    @Column(name = "custom_name", length = 100)
    private String customName;

    @Column(precision = 10, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_id")
    private Long unitId;

    @Transient
    private String unitName;

    @Column(name = "aisle_category_id", nullable = false)
    private Long aisleCategoryId = 6L; // Default: General

    @Column(name = "is_checked", nullable = false)
    private boolean isChecked = false;

    @Column(name = "is_cleared", nullable = false)
    private boolean isCleared = false;

    @Column(name = "is_manually_edited", nullable = false)
    private boolean isManuallyEdited = false;

    @Column(name = "cleared_at")
    private Instant clearedAt;

    @Version
    @Column(nullable = false)
    private Integer version = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "shoppingListItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShoppingListItemSource> sources = new ArrayList<>();

    public ShoppingListItem() {}

    public ShoppingListItem(ShoppingList shoppingList, UUID clientUuid, Long ingredientId, String customName,
                            BigDecimal quantity, Long unitId, String unitName, Long aisleCategoryId) {
        this.shoppingList = shoppingList;
        this.clientUuid = clientUuid != null ? clientUuid : UUID.randomUUID();
        this.ingredientId = ingredientId;
        this.customName = customName;
        this.quantity = quantity;
        this.unitId = unitId;
        this.unitName = unitName;
        this.aisleCategoryId = aisleCategoryId != null ? aisleCategoryId : 6L;
        this.isChecked = false;
        this.isCleared = false;
        this.isManuallyEdited = false;
        this.version = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addSource(ShoppingListItemSource source) {
        sources.add(source);
        source.setShoppingListItem(this);
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ShoppingList getShoppingList() { return shoppingList; }
    public void setShoppingList(ShoppingList shoppingList) { this.shoppingList = shoppingList; }

    public UUID getClientUuid() { return clientUuid; }
    public void setClientUuid(UUID clientUuid) { this.clientUuid = clientUuid; }

    public Long getIngredientId() { return ingredientId; }
    public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }

    public String getCustomName() { return customName; }
    public void setCustomName(String customName) { this.customName = customName; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }

    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }

    public Long getAisleCategoryId() { return aisleCategoryId; }
    public void setAisleCategoryId(Long aisleCategoryId) { this.aisleCategoryId = aisleCategoryId; }

    public boolean isChecked() { return isChecked; }
    public void setChecked(boolean checked) { isChecked = checked; }

    public boolean isCleared() { return isCleared; }
    public void setCleared(boolean cleared) { isCleared = cleared; }

    public boolean isManuallyEdited() { return isManuallyEdited; }
    public void setManuallyEdited(boolean manuallyEdited) { isManuallyEdited = manuallyEdited; }

    public Instant getClearedAt() { return clearedAt; }
    public void setClearedAt(Instant clearedAt) { this.clearedAt = clearedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<ShoppingListItemSource> getSources() { return sources; }
    public void setSources(List<ShoppingListItemSource> sources) { this.sources = sources; }
}
