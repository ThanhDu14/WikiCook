# Data Model: Automated Smart Shopping List & Ingredient Aggregation – 040

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu này đặc tả chi tiết mô hình dữ liệu (Phase 1 Data Model) cho module **040-shopping-list**, bao gồm lược đồ CSDL quan hệ (PostgreSQL 16), các thực thể JPA (Java 21), DTO Java Records và cấu trúc lưu trữ ngoại tuyến tại Client (IndexedDB).

---

## 1. Lược đồ CSDL Quan hệ (PostgreSQL 16)

Thiết kế tuân thủ 100% theo [Mục 4.8 của `docs/analysis-and-design/database/full-schema.md`](../../docs/analysis-and-design/database/full-schema.md#48-shopping-list-040--d), được khởi tạo trong migration Flyway `V401__shopping_lists.sql`.

```mermaid
erDiagram
    users ||--o{ shopping_lists : "owns"
    users ||--o{ user_aisle_preferences : "configures"
    shopping_lists ||--o{ shopping_list_items : "contains"
    shopping_list_items ||--o{ shopping_list_item_sources : "derived_from"
    aisle_categories ||--o{ shopping_list_items : "classifies"
    aisle_categories ||--o{ user_aisle_preferences : "categorizes"
    ingredients |o--o{ shopping_list_items : "references"
    ingredients ||--o{ user_aisle_preferences : "customizes"
    units ||--o{ shopping_list_items : "measures"
    units ||--o{ shopping_list_item_sources : "measures"
    recipes |o--o{ shopping_list_item_sources : "source_recipe"
    meal_plan_items |o--o{ shopping_list_item_sources : "source_meal"

    shopping_lists {
        bigint id PK
        bigint user_id FK "CASCADE"
        varchar title "NOT NULL, max 100"
        varchar status "CHECK IN ('ACTIVE','ARCHIVED')"
        timestamptz archived_at "NULL"
    }

    shopping_list_items {
        bigint id PK
        bigint list_id FK "CASCADE"
        uuid client_uuid UK "NOT NULL, generated on client"
        bigint ingredient_id FK "NULL for custom item, RESTRICT"
        varchar custom_name "NULL, max 100"
        numeric quantity "NUMERIC(10,3), normalized value"
        bigint unit_id FK "RESTRICT, normalized unit"
        bigint aisle_category_id FK "RESTRICT"
        boolean is_checked "NOT NULL DEFAULT false"
        boolean is_cleared "NOT NULL DEFAULT false"
        boolean is_manually_edited "NOT NULL DEFAULT false"
        timestamptz cleared_at "NULL"
        integer version "NOT NULL DEFAULT 0, optimistic locking"
    }

    shopping_list_item_sources {
        bigint id PK
        bigint item_id FK "CASCADE"
        bigint recipe_id FK "SET NULL"
        bigint meal_plan_item_id FK "SET NULL"
        varchar recipe_title_snapshot "NOT NULL, max 100"
        numeric contributed_quantity "NUMERIC(10,3)"
        bigint unit_id FK "RESTRICT"
    }

    user_aisle_preferences {
        bigint user_id PK, FK "CASCADE"
        bigint ingredient_id PK, FK "RESTRICT"
        bigint aisle_category_id FK "RESTRICT"
    }
```

### 1.1. Bảng `shopping_lists`
Lưu trữ thông tin danh sách mua sắm của người dùng.

| Cột | Kiểu dữ liệu | Nullable | Ràng buộc / Mặc định | Ý nghĩa nghiệp vụ |
|---|---|---|---|---|
| `id` | `BIGINT` | NOT NULL | `PRIMARY KEY GENERATED ALWAYS AS IDENTITY` | Khóa chính duy nhất của danh sách |
| `user_id` | `BIGINT` | NOT NULL | `REFERENCES users(id) ON DELETE CASCADE` | Chủ sở hữu danh sách |
| `title` | `VARCHAR(100)` | NOT NULL | Mặc định: `'Danh sách mua sắm'` | Tên danh sách (1–60 ký tự cho phép sửa) |
| `status` | `VARCHAR(10)` | NOT NULL | `CHECK (status IN ('ACTIVE', 'ARCHIVED'))` | Trạng thái hoạt động |
| `archived_at` | `TIMESTAMPTZ` | NULL | Mặc định: `NULL` | Thời điểm lưu trữ khi tạo list mới |

**Chỉ mục & Ràng buộc độc nhất:**
- `CREATE UNIQUE INDEX uq_shopping_lists_active_user ON shopping_lists(user_id) WHERE status = 'ACTIVE';`
  $\rightarrow$ Đảm bảo mỗi người dùng có **duy nhất 1 danh sách active** tại một thời điểm (FR-001).

---

### 1.2. Bảng `shopping_list_items`
Lưu trữ các nguyên liệu hoặc mặt hàng cụ thể trong danh sách.

| Cột | Kiểu dữ liệu | Nullable | Ràng buộc / Mặc định | Ý nghĩa nghiệp vụ |
|---|---|---|---|---|
| `id` | `BIGINT` | NOT NULL | `PRIMARY KEY GENERATED ALWAYS AS IDENTITY` | Khóa chính cơ sở dữ liệu |
| `list_id` | `BIGINT` | NOT NULL | `REFERENCES shopping_lists(id) ON DELETE CASCADE` | Thuộc danh sách nào |
| `client_uuid` | `UUID` | NOT NULL | `UNIQUE` | Khóa định danh từ client, hỗ trợ offline idempotency |
| `ingredient_id` | `BIGINT` | NULL | `REFERENCES ingredients(id) ON DELETE RESTRICT` | Liên kết nguyên liệu chuẩn (NULL nếu là custom item) |
| `custom_name` | `VARCHAR(100)` | NULL | | Tên món tự nhập thủ công (NULL nếu có `ingredient_id`) |
| `quantity` | `NUMERIC(10,3)` | NULL | | Số lượng chuẩn hóa (Normalized Quantity: gram, ml, số đếm) |
| `unit_id` | `BIGINT` | NULL | `REFERENCES units(id) ON DELETE RESTRICT` | Đơn vị chuẩn hóa lưu trữ tương ứng |
| `aisle_category_id` | `BIGINT` | NOT NULL | `REFERENCES aisle_categories(id) ON DELETE RESTRICT` | Quầy siêu thị phân loại mặt hàng này |
| `is_checked` | `BOOLEAN` | NOT NULL | `DEFAULT false` | Trạng thái đã mua (đã tick vào giỏ hàng) |
| `is_cleared` | `BOOLEAN` | NOT NULL | `DEFAULT false` | Trạng thái dọn dẹp mềm (soft-clear) |
| `is_manually_edited` | `BOOLEAN` | NOT NULL | `DEFAULT false` | Cờ ghi nhận user đã can thiệp sửa số lượng thủ công |
| `cleared_at` | `TIMESTAMPTZ` | NULL | `DEFAULT NULL` | Thời điểm thực hiện dọn dẹp (phục vụ undo theo đợt) |
| `version` | `INTEGER` | NOT NULL | `DEFAULT 0` | Khóa phiên bản phục vụ Optimistic Locking & LWW |

**Ràng buộc toàn vẹn bảng (Table Check Constraint):**
- `CHECK (num_nonnulls(ingredient_id, custom_name) >= 1)`: Đảm bảo món đồ phải có mã nguyên liệu hoặc có tên tự do, không được rỗng cả hai.

---

### 1.3. Bảng `shopping_list_item_sources`
Lưu trữ vết nguồn gốc (Provenance) cấu thành nên số lượng của mỗi nguyên liệu đã gộp (FR-009).

| Cột | Kiểu dữ liệu | Nullable | Ràng buộc / Mặc định | Ý nghĩa nghiệp vụ |
|---|---|---|---|---|
| `id` | `BIGINT` | NOT NULL | `PRIMARY KEY GENERATED ALWAYS AS IDENTITY` | Khóa chính dòng đóng góp |
| `item_id` | `BIGINT` | NOT NULL | `REFERENCES shopping_list_items(id) ON DELETE CASCADE` | Thuộc dòng nguyên liệu nào |
| `recipe_id` | `BIGINT` | NULL | `REFERENCES recipes(id) ON DELETE SET NULL` | Nguồn từ công thức nào (nếu công thức bị xóa thì set null) |
| `meal_plan_item_id`| `BIGINT` | NULL | `REFERENCES meal_plan_items(id) ON DELETE SET NULL`| Nguồn từ ô nào trong lịch ăn tuần 030/031 |
| `recipe_title_snapshot`| `VARCHAR(100)`| NOT NULL | | Tên công thức chụp lại tại thời điểm thêm |
| `contributed_quantity`| `NUMERIC(10,3)`| NULL | | Số lượng cụ thể do món này đóng góp vào tổng |
| `unit_id` | `BIGINT` | NULL | `REFERENCES units(id) ON DELETE RESTRICT` | Đơn vị của lượng đóng góp |

---

### 1.4. Bảng `user_aisle_preferences`
Lưu trữ tùy biến quầy hàng cá nhân của từng người dùng đối với từng nguyên liệu (FR-013, Q12).

| Cột | Kiểu dữ liệu | Nullable | Ràng buộc / Mặc định | Ý nghĩa nghiệp vụ |
|---|---|---|---|---|
| `user_id` | `BIGINT` | NOT NULL | `REFERENCES users(id) ON DELETE CASCADE` | Người dùng tùy biến |
| `ingredient_id` | `BIGINT` | NOT NULL | `REFERENCES ingredients(id) ON DELETE RESTRICT` | Nguyên liệu được tùy biến |
| `aisle_category_id`| `BIGINT`| NOT NULL | `REFERENCES aisle_categories(id) ON DELETE RESTRICT`| Quầy người dùng mong muốn |

**Khóa chính phức hợp (Composite PK):**
- `PRIMARY KEY (user_id, ingredient_id)`: Mỗi người dùng có tối đa 1 tùy biến quầy cho 1 nguyên liệu.

---

## 2. Các thực thể JPA (Backend Entities - Java 21)

### 2.1. `ShoppingList`
```java
package com.wikicook.shoppinglist.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shopping_lists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoppingList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ShoppingListStatus status;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @OneToMany(mappedBy = "shoppingList", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ShoppingListItem> items = new ArrayList<>();
}
```

### 2.2. `ShoppingListItem`
```java
package com.wikicook.shoppinglist.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shopping_list_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Column(name = "aisle_category_id", nullable = false)
    private Long aisleCategoryId;

    @Column(name = "is_checked", nullable = false)
    private boolean checked;

    @Column(name = "is_cleared", nullable = false)
    private boolean cleared;

    @Column(name = "is_manually_edited", nullable = false)
    private boolean manuallyEdited;

    @Column(name = "cleared_at")
    private Instant clearedAt;

    @Version
    @Column(nullable = false)
    private Integer version;

    @OneToMany(mappedBy = "shoppingListItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ShoppingListItemSource> sources = new ArrayList<>();
}
```

---

## 3. Java Records DTO (API Transfer Contracts)

Tuân thủ nguyên tắc **Constitution III** (DTO Record, không trả JPA Entity ra ngoài API).

### 3.1. DTO Trả về (Responses)
```java
package com.wikicook.shoppinglist.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShoppingListResponse(
    Long id,
    String title,
    String status,
    int totalItems,
    int checkedItems,
    int clearedItems,
    List<AisleGroupDto> aisleGroups
) {}

public record AisleGroupDto(
    Long aisleId,
    String aisleCode,
    String aisleName,
    int displayOrder,
    List<ShoppingListItemDto> items
) {}

public record ShoppingListItemDto(
    UUID clientUuid,
    Long ingredientId,
    String displayName,
    BigDecimal displayQuantity,
    String displayUnit,
    BigDecimal normalizedQuantity,
    String normalizedUnit,
    Long aisleCategoryId,
    boolean isChecked,
    boolean isCleared,
    boolean isManuallyEdited,
    int sourceRecipeCount,
    List<ItemProvenanceDto> sources
) {}

public record ItemProvenanceDto(
    Long sourceId,
    Long recipeId,
    Long mealPlanItemId,
    String recipeTitleSnapshot,
    BigDecimal contributedQuantity,
    String unitName
) {}

public record ClearPurchasedResponse(
    String clearBatchId,
    int clearedCount,
    Instant clearedAt
) {}

public record UndoClearResponse(
    int restoredCount,
    String message
) {}

public record SyncBatchResponse(
    int processedCount,
    int acceptedCount,
    int rejectedCount,
    List<SyncItemResultDto> itemResults
) {}

public record SyncItemResultDto(
    UUID clientUuid,
    String status, // "ACCEPTED", "CONFLICT_RESOLVED", "REJECTED"
    Integer serverVersion,
    String message
) {}
```

### 3.2. DTO Nhận vào (Requests)
```java
package com.wikicook.shoppinglist.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateCustomItemRequest(
    @NotNull UUID clientUuid,
    @NotBlank @Size(min = 1, max = 100) String customName,
    @Positive BigDecimal quantity,
    String unitName,
    Long aisleCategoryId
) {}

public record UpdateItemRequest(
    Boolean isChecked,
    Boolean isCleared,
    @Positive BigDecimal quantity,
    String unitName,
    Long aisleCategoryId,
    Boolean rememberAisle
) {}

public record ImportRecipeRequest(
    @NotNull Long recipeId,
    @NotNull @DecimalMin("0.5") BigDecimal targetServings,
    List<Long> excludedIngredientIds
) {}

public record ImportMealPlanRequest(
    @NotNull Long mealPlanId,
    @NotBlank String mode, // "MERGE" | "ARCHIVE_AND_NEW"
    List<ServingOverrideDto> servingOverrides,
    List<Long> excludedIngredientIds
) {}

public record ServingOverrideDto(
    Long mealPlanItemId,
    BigDecimal targetServings
) {}

public record SyncBatchRequest(
    @NotEmpty List<SyncMutationDto> mutations
) {}

public record SyncMutationDto(
    @NotNull UUID clientUuid,
    @NotBlank String action, // "CREATE" | "TOGGLE_CHECK" | "UPDATE_QTY" | "CHANGE_AISLE" | "DELETE" | "SOFT_CLEAR"
    Integer expectedVersion,
    Long ingredientId,
    String customName,
    BigDecimal quantity,
    String unitName,
    Long aisleCategoryId,
    Boolean isChecked,
    Boolean isCleared,
    Instant clientTimestamp
) {}

public record UpdateTitleRequest(
    @NotBlank @Size(min = 1, max = 60) String title
) {}
```

---

## 4. Client-side IndexedDB Schema (Offline Store)

Được quản lý trong `src/frontend/src/services/offlineShoppingDb.ts` sử dụng thư viện `idb`:

### Database Name: `wikicook_shopping_offline` (Version 1)

1. **Object Store: `active_list`** (KeyPath: `key`)
   - Chứa snapshot danh sách mua sắm hiện hành:
   ```typescript
   interface ActiveListRecord {
     key: 'CURRENT_LIST';
     listId: number;
     title: string;
     lastUpdated: number;
     items: Record<string, OfflineShoppingItem>; // Keyed by clientUuid
   }
   ```

2. **Object Store: `mutation_queue`** (KeyPath: `clientUuid`)
   - Hàng đợi mutation ngoại tuyến, sắp xếp theo timestamp:
   ```typescript
   interface OfflineMutationRecord {
     clientUuid: string;
     action: 'CREATE' | 'TOGGLE_CHECK' | 'UPDATE_QTY' | 'CHANGE_AISLE' | 'DELETE' | 'SOFT_CLEAR';
     payload: any;
     clientTimestamp: number;
     syncAttempts: number;
     lastError?: string;
   }
   ```
