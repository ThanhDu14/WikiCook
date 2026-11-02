package com.wikicook.shoppinglist.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "units")
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name_vi", nullable = false, length = 30)
    private String nameVi;

    @Column(name = "unit_type", nullable = false, length = 10)
    private String unitType;

    @Column(name = "requires_quantity", nullable = false)
    private boolean requiresQuantity = true;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Unit() {}

    public Unit(Long id, String code, String nameVi, String unitType) {
        this.id = id;
        this.code = code;
        this.nameVi = nameVi;
        this.unitType = unitType;
        this.requiresQuantity = true;
        this.isActive = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getNameVi() { return nameVi; }
    public void setNameVi(String nameVi) { this.nameVi = nameVi; }

    public String getUnitType() { return unitType; }
    public void setUnitType(String unitType) { this.unitType = unitType; }

    public boolean isRequiresQuantity() { return requiresQuantity; }
    public void setRequiresQuantity(boolean requiresQuantity) { this.requiresQuantity = requiresQuantity; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
