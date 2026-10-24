# Research: Automated Smart Shopping List & Ingredient Aggregation – 040

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Date**: 2026-10-24

This document records all technical research decisions (Phase 0 Research) for the **040-shopping-list** module, following the **Decision / Rationale / Alternatives considered** structure. All technical questions (NEEDS CLARIFICATION) are fully resolved before proceeding to Phase 1 (Data Model & Contracts).

---

## R1. Ingredient Aggregation Algorithm, Unit Conversion, and Quantity Storage

- **Decision**:
   1. **Bidirectional Metric Conversion in Java (Backend)**:
      - Mass: $1000\text{ g} \leftrightarrow 1\text{ kg}$. Values $\ge 1000\text{ g} \to \text{kg}$; values $< 1\text{ kg} \to \text{g}$.
      - Volume: $1000\text{ ml} \leftrightarrow 1\text{ l}$. Values $\ge 1000\text{ ml} \to \text{l}$; values $< 1\text{ l} \to \text{ml}$.
      - Arithmetic precision: Use `BigDecimal` with `HALF_UP` rounding to at most 2 decimal places (`scale = 2`) when producing display DTOs.
   2. **Rounding Rules for Discrete / Countable Units (Discrete Units)**:
      - For physical count units (fruit, roots, heads, cans, packs, eggs, pieces, slices): Any fractional result after applying the serving multiplier is **rounded up (`Ceiling`)** in the main shopping display quantity (`display_quantity`), for example: 0.5 egg $\to$ 1 egg, 1.25 heads of cabbage $\to$ 2 heads. This ensures the cook does not buy too few ingredients.
      - The exact decimal quantity is fully preserved in ingredient provenance details (`shopping_list_item_sources.contributed_quantity`).
   3. **Non-Convertible Qualitative Units**:
      - Traditional culinary volume units ("thìa canh", "thìa cà phê") or approximate units ("nhánh", "tép", "vừa đủ") without a fixed density are **never forcibly aggregated into grams or ml**. They remain separate sub-rows or independent rows with the same ingredient name.
   4. **Serving Multiplier**:
      - Multiplier: $\text{Multiplier} = S_{\text{target}} / S_{\text{base}}$. $S_{\text{target}}$ must be a positive multiple of 0.5. If the source recipe does not specify $S_{\text{base}}$, the system defaults to $\text{Multiplier} = 1.0\times$ and returns the warning flag `is_default_servings = true` in the DTO.
   5. **Database Storage Decision (Database Schema)**:
      - The `shopping_list_items` table has only one `quantity NUMERIC(10,3)` column and `unit_id BIGINT`:
        * The `quantity` column **always stores a normalized value** in the smallest base unit: mass in grams (`g`), volume in milliliters (`ml`), and countable units as actual counts. `unit_id` references the ID of that normalized base unit.
        * The DTO returned to the client automatically derives the corresponding `display_quantity` and `display_unit` (kg, l).
        * The `shopping_list_item_sources` table stores snapshots of the original `contributed_quantity` and `unit_id` for provenance tracking (provenance modal).
- **Rationale**: Complies with FR-003, FR-005, FR-006, FR-007, FR-008, Q1, Q9. Fully matches the schema in Section 4.8 of `docs/analysis-and-design/database/full-schema.md` (only one `quantity NUMERIC(10,3)` column).
- **Alternatives considered**:
   - Storing both `display_quantity` and `normalized_quantity` in the DB: Rejected because it violates the approved schema in Section 4.8 of `full-schema.md` and increases the risk of inconsistent data.
   - Converting "thìa canh" and "thìa cà phê" to ml using international standards (15 ml, 5 ml): Rejected because Vietnamese seasoning densities vary significantly (fish sauce is heavier than cooking oil, salt is heavier than MSG), causing inaccuracies in actual cooking (Q1).

---

## R2. Offline-First Behavior, Local Storage, and Synchronization Protocol (Sync Protocol)

- **Decision**:
   1. **Client-Side Storage (Local-First)**:
      - Use `IndexedDB` through the lightweight [`idb`](https://github.com/jakearchibald/idb) library (Jake Archibald's standard ~1KB Promise wrapper) for the React SPA.
      - The client maintains two Object Stores:
        * `active_list`: Contains all current list data (items, aisles, checked/cleared status).
        * `mutation_queue`: Stores offline actions in chronological order (`{ clientUuid, action: 'TOGGLE_CHECK' | 'UPDATE_QTY' | 'ADD_CUSTOM' | 'DELETE' | 'SOFT_CLEAR', payload, clientTimestamp }`).
   2. **`client_uuid` Identification**:
      - Every item created on the client (both offline and online) is assigned a `UUIDv4` (`client_uuid`) at creation. The `shopping_list_items` table has a `UNIQUE (client_uuid)` constraint (full-schema 4.8).
   3. **Batch Sync Idempotency**:
      - When connectivity returns, the client sends the entire `mutation_queue` to `POST /api/v1/shopping-lists/active/sync`.
      - Server processing is **idempotent**: If a newly added item's `client_uuid` already exists, the server skips creation; for updates, the server compares `version` and `clientTimestamp`.
   4. **Conflict Resolution Algorithm**:
      - Apply per-row **Last-Write-Wins** based on the incrementing `version` column.
      - **Give ABSOLUTE priority to purchased status (`is_checked = true`)**: Once a device has successfully checked an item (`is_checked = true`), later uncheck operations delayed by connectivity loss cannot overwrite it with an unchecked state (using logical OR: `is_checked_final = current.is_checked || mutation.is_checked`). This prevents users from buying duplicates or overlooking items already in the cart.
   5. **Handling Token Expiration While Offline**:
      - In a supermarket basement, if the Access Token (15-minute JWT) expires, the client still allows checking items, editing quantities, and adding items locally in IndexedDB without redirecting to login (FR-020).
      - When connectivity returns, the client automatically invokes `POST /api/v1/auth/refresh`.
      - **If the Refresh Token has also expired**: The client **DOES NOT** clear IndexedDB! The application displays a non-blocking banner: *"Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại để đồng bộ giỏ hàng lên máy chủ"*. The mutation queue remains safely preserved and is automatically flushed to the server after successful login.
   6. **Logout Rules**:
      - When the user explicitly logs out, the system checks for unsynchronized data in `mutation_queue` and displays a warning modal if any exists: *"Bạn còn [N] thay đổi chưa được đồng bộ lên máy chủ. Đăng xuất lúc này sẽ hủy bỏ các thay đổi này"*. The session and IndexedDB are cleared only after confirmation.
- **Rationale**: Satisfies FR-020, FR-021, FR-022, SC-002 (<50ms), SC-003 (sync <3s), Q5, Q6. Fully aligns with Constitution IV (user data protection).
- **Alternatives considered**:
   - Using `localStorage`: Rejected because of its 5MB capacity limit, synchronous behavior (blocking I/O causes frame stutter when scrolling large lists), and lack of safe transactions.
   - Using WebSockets for real-time synchronization: Rejected because the MVP covers a single user's personal list; WebSockets consume mobile battery and are explicitly outside the specification's scope (Out of Scope section).

---

## R3. Pre-Import Modal, Staple Filtering, and Meal Plan Mode

- **Decision**:
  1. **Pre-Import Checklist Modal**:
      - When the user clicks "Thêm vào danh sách mua sắm" from Recipe Detail (020) or Meal Planner (030/031), the client **always displays a pre-import Bottom Sheet / Modal**.
      - The modal displays ingredients with the serving multiplier applied, all selected by default. Users can deselect individual items.
   2. **One-Tap "Bỏ chọn gia vị cơ bản" Utility (Staple Filter)**:
      - Default staples for a Vietnamese kitchen: *Muối, Đường, Dầu ăn, Nước mắm, Hạt tiêu, Bột ngọt / Hạt nêm*.
      - **Resolution mechanism**: The system resolves the `ingredient_id` values of these six staples through module 060's ingredient catalogue (`IngredientSummaryPort`). If the catalogue lacks entries or names differ, fall back to the configured canonical name list in application configuration (`wikicook.staples.names=muối,đường,dầu ăn,nước mắm,tiêu,bột ngọt,hạt nêm`).
   3. **Import API Body**:
      - `POST /api/v1/shopping-lists/import/recipe`: Accepts `recipeId`, `targetServings`, and `excludedIngredientIds` (IDs of items deselected in the modal).
      - Supports a client-generated `Idempotency-Key` header (UUID) to prevent repeated clicks (double-click within 2 seconds).
   4. **Weekly Meal Plan Import Mode (030/031)**:
      - `POST /api/v1/shopping-lists/import/meal-plan`: Accepts `mealPlanId`, `mode` (`MERGE` or `ARCHIVE_AND_NEW`), `servingOverrides[]`, and `excludedIngredientIds`.
      - If the user has no active shopping list, the system creates one automatically without a mode-selection modal.
      - **FR-016 also applies to imports**: If an imported ingredient matches an existing item with `is_checked = true`, the system automatically adds the new quantity and resets the status to `is_checked = false`, notifying the user that more of the item must be purchased.
- **Rationale**: Satisfies FR-002, FR-004, FR-016, Q3, Q4, Q10.
- **Alternatives considered**:
   - Aggregating directly into the list without a confirmation modal: Rejected because users often already have staple seasonings at home; automatic aggregation clutters the list and causes frustration.

---

## R4. 200-Item Capacity Limit, Soft-Clear, and Undo

- **Decision**:
   1. **Enforce the 200 Items/List Limit**:
      - Client: Check the existing item count + anticipated new items. If $> 200$, disable Import and display a warning asking the user to archive the old list or remove some items.
      - Backend Service: Check `COUNT(*) FROM shopping_list_items WHERE list_id = :listId AND is_cleared = false`. If it exceeds 200, throw `ListCapacityExceededException` $\to$ HTTP 422 `LIST_CAPACITY_EXCEEDED`.
   2. **Soft-Clear Mechanism ("Xóa món đã mua")**:
      - The "Dọn dẹp món đã mua" operation is a **non-destructive soft-clear**: `UPDATE shopping_list_items SET is_cleared = true, cleared_at = NOW(), version = version + 1 WHERE list_id = :listId AND is_checked = true AND is_cleared = false`.
      - The `POST /api/v1/shopping-lists/active/clear-purchased` endpoint returns the result with a clear batch identifier `clearBatchId` (or timestamp `clearedAt`).
      - The UI displays a Snackbar: *"Đã dọn dẹp [N] món đã mua"* with an **"Hoàn tác"** button for 5 seconds.
   3. **Undo Mechanism**:
      - Clicking Undo within 5 seconds calls `POST /api/v1/shopping-lists/active/undo-clear` with `clearBatchId`: The server restores exactly the items just cleared (`SET is_cleared = false, cleared_at = NULL`).
      - To restore individual items from the "Xem món đã dọn dẹp" section at the bottom of the page, the client uses `PATCH /api/v1/shopping-lists/items/{clientUuid}` with payload `{ "isCleared": false }`.
   4. **Archive & Restore Lifecycle**:
      - `POST /api/v1/shopping-lists/active/archive`: Changes the current list to `ARCHIVED` (`archived_at = NOW()`), preserves all purchased and soft-cleared items, and **immediately creates a new empty active shopping list** in the same database transaction.
      - `POST /api/v1/shopping-lists/{id}/restore`: Runs within a single transaction with locking: (1) Archive the current active list; (2) Change list `{id}` to `ACTIVE`, reset all `is_checked = false` and `is_cleared = false`, and retain item names, quantities, and aisles. Strictly satisfies `UNIQUE (user_id) WHERE status = 'ACTIVE'`.
- **Rationale**: Satisfies FR-001, FR-017, FR-018, FR-019, FR-025, Q11, Q13. Ensures database integrity and a smooth user experience.
- **Alternatives considered**:
   - Hard delete (permanently deleting from the database when clearing): Rejected because users could not review their shopping history afterward or undo accidental actions.

---

## R5. Supermarket Aisle Categorization & User Customization

- **Decision**:
   1. **Six Standard Aisles (Fixed Taxonomy)**:
      - Based on `aisle_categories` (full-schema Sections 3.1 & 4.8):
        * `PRODUCE` (Vegetables & Fruit) - display_order: 1
        * `MEAT_SEAFOOD` (Meat & Seafood) - display_order: 2
        * `DAIRY_EGGS` (Eggs & Dairy) - display_order: 3
        * `PANTRY_SPICES` (Seasonings & Dry Goods) - display_order: 4
        * `FROZEN` (Frozen Food) - display_order: 5
        * `GENERAL` (Other Supplies / General Merchandise) - display_order: 6
   2. **Automatic Aisle Assignment Rules**:
      - Recipe imports: Read `aisle_category_id` from `ingredients` (managed by module 060).
      - Custom items: If no match exists in the system ingredient catalogue, default to `GENERAL`.
   3. **Remember Custom Aisles (`user_aisle_preferences`)**:
      - When the user changes an item's aisle in the UI:
        * The client calls `PATCH /api/v1/shopping-lists/items/{clientUuid}` with `rememberAisle: true/false`.
        * If `rememberAisle = true` and the item references a valid `ingredient_id`: The server automatically upserts into `user_aisle_preferences (user_id, ingredient_id, aisle_category_id)`.
        * For subsequent imports by this user, the personal aisle preference always overrides the system default.
- **Rationale**: Satisfies FR-011, FR-012, FR-013, Q12.
- **Alternatives considered**:
   - Allowing arbitrary user-defined aisle names: Rejected because it disrupts the layout structure and complicates standard icon display on mobile.

---

## R6. Layered Architecture, Package Structure, and Module Boundaries

- **Decision**:
  1. **Backend Package Structure (Package by Feature)**:
      - Source package: `com.wikicook.shoppinglist`
       * `controller/`: `ShoppingListController`, `ShoppingListSyncController`, `ShoppingListExceptionHandler`
       * `service/`: `ShoppingListService`, `IngredientAggregationService`, `UnitConversionService`, `ShoppingListSyncService`
       * `repository/`: `ShoppingListRepository`, `ShoppingListItemRepository`, `ShoppingListItemSourceRepository`, `UserAislePreferenceRepository`
       * `domain/`: `ShoppingList`, `ShoppingListItem`, `ShoppingListItemSource`, `UserAislePreference`, `ShoppingListStatus` (Enum)
        * `dto/`: Record DTOs (`ShoppingListResponse`, `AisleGroupDto`, `ShoppingListItemDto`, `ItemSourceDto`, `SyncBatchRequest`, `SyncBatchResponse`, etc.)
        * `port/`: Facade interfaces for external communication:
          - `RecipeDetailPort`: Reads recipes, base servings, ingredients, and default aisles from module 060.
          - `MealPlanPort`: Reads weekly dishes and serving overrides from modules 030/031.
          - `DietaryProfilePort`: Reads allergies/dietary exclusions from module 011 (for warnings when needed).
   2. **Constitution Compliance (Constitution Principles)**:
      - Principle III (Layered Architecture): Controllers call only Services; Services call only Repositories or Ports. All communication outside the module uses DTO interfaces, with no cross-module table joins.
      - Principle IV (Security): Obtain `userId` from `SecurityContext` (`Authentication.getPrincipal()`); never accept `userId` from the client request body or URL path.
- **Rationale**: Satisfies Principles III and IV and the Layered Client-Server architecture in `architecture.md`.
- **Alternatives considered**:
   - Pure Hexagonal Architecture with complex Ports & Adapters: Simplified to Service Facades/Ports appropriate for the scale of a student project (YAGNI).

---

## R7. Testing Strategy & Quality Assurance (Test Strategy)

- **Decision**:
  1. **Backend Test Automation**:
      - Unit Tests: `IngredientAggregationServiceTest`, `UnitConversionServiceTest`, `ShoppingListSyncServiceTest` (JUnit 5 + Mockito). Measure coverage with JaCoCo, committing to line coverage $\ge 70\%$ and branch coverage $\ge 70\%$.
      - Integration Tests: `ShoppingListControllerIT`, `ShoppingListRepositoryIT` use `@SpringBootTest` / `@DataJpaTest` with **Testcontainers PostgreSQL (`postgres:16`)**. Never use H2 In-Memory DB, to avoid differences in `NUMERIC(10,3)` behavior and PostgreSQL partial-index syntax.
  2. **Frontend Test Automation**:
      - Component & Hook Tests: Test `useShoppingList`, `useOfflineSync`, and `ChecklistScreen` with Vitest + React Testing Library + Mock Service Worker (`msw`).
   3. **Manual Test Cases for Field Scenarios**:
      - Scenarios that cannot run automatically in standard CI environments are documented as manual test cases in `docs/test/test-cases.md`:
        * *TC-MAN-040-01 (Offline In-Store & Airplane Mode)*: Enable airplane mode on a real phone, perform 10 check and quantity-edit operations, disable airplane mode, and verify automatic synchronization within $\le 3$ seconds (SC-003).
        * *TC-MAN-040-02 (Mobile Responsiveness & Single-Handed Checklist)*: Measure checkbox response latency $< 50\text{ms}$ on mid-range Android/iOS devices (SC-002).
        * *TC-MAN-040-03 (Print Stylesheet)*: Verify actual print output in Chrome/Safari, with headers/footers fully removed and two ink-saving columns rendered correctly.
- **Rationale**: Satisfies Principle II (Test-First), SC-001, SC-002, SC-003, SC-004.

---

## Decisions Agreed with the Team (Sign-off)

1. ✅ **Unit Conversion**: Implemented in Java code within `UnitConversionService`, without a `unit_conversions` database table (agreed in `full-schema.md`, Section 5, row 8).
2. ✅ **Idempotent Sync**: Identify every item with a client-side `client_uuid` from creation.
3. ✅ **Git Branch Name**: Continue working on `feature/040-shopping-list`; do not independently rename it to follow `SCRUM-xx` before a Jira task has been created.
