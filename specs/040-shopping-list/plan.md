# Implementation Plan: Automated Smart Shopping List & Ingredient Aggregation

**Branch**: `feature/040-shopping-list` | **Date**: 2026-10-24 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/040-shopping-list/spec.md` (Status: Draft / Ready for Review; assumed to have been approved by the team as the technical planning baseline).

---

## Summary

The **040-shopping-list** module provides WikiCook users with a seamless transition from meal planning to actual supermarket shopping:
1. **Multi-Source Ingestion**: Import ingredients from individual recipes with a serving multiplier ($S_{\text{target}} / S_{\text{base}}$), aggregate in bulk from a weekly plan of 7 days $\times$ 3 meals (Specs 030/031), or manually add grocery/household items.
2. **Smart Aggregation Engine & Unit Conversion**: Automatically group by standard ingredient ID (`ingredient_id`), perform bidirectional metric conversion ($g \leftrightarrow kg$, $ml \leftrightarrow l$) in Java, round countable units (fruit, roots, eggs) up (`Ceiling`) in the shopping list while preserving fractional quantities in provenance details, and retain qualitative units ("thìa canh", "tép") in separate rows.
3. **Supermarket Aisle Categorization & Checklist Utilities**: Automatically classify items into six standard aisles (`PRODUCE`, `MEAT_SEAFOOD`, `DAIRY_EGGS`, `PANTRY_SPICES`, `FROZEN`, `GENERAL`), remember custom aisle preferences (`user_aisle_preferences`), provide an instantly responsive one-handed checklist ($< 50\text{ms}$ optimistic UI), and support non-destructive soft-clear (`is_cleared = true`) with a 5-second undo snackbar.
4. **Local-First Offline Resilience**: Use `IndexedDB` (the `idb` library) to store the list and mutation queue on the client; generate `client_uuid` for every item in advance; automatically batch-sync to the server when connectivity returns using Last-Write-Wins with absolute protection for purchased status (`is_checked = true`); preserve the queue even if tokens expire in a supermarket basement.
5. **Client-Only Export & Printing**: One-tap plaintext copying for sharing via Zalo/SMS and an ink-saving two-column `@media print` layout (without unnecessary endpoints).

---

## Technical Context

**Language/Version**: Java 21 (LTS) for the backend; TypeScript 5.x + React 18 for the frontend.

**Primary Dependencies**: 
- *Backend*: Spring Boot 3.x (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`), Flyway Migration, `springdoc-openapi-starter-webmvc-ui` (v2.x).
- *Frontend*: Vite, Tailwind CSS 3.4, `idb` (v8.x, a lightweight Promise wrapper for IndexedDB; see Complexity Tracking).

**Storage**: PostgreSQL 16. Uses exactly four tables from [Section 4.8 of `full-schema.md`](../../docs/analysis-and-design/database/full-schema.md#48-shopping-list-040--d), managed through Flyway migration `V401__shopping_lists.sql`:
- `shopping_lists`: Primary key `id`, `user_id`, `title`, `status`, `archived_at`. Partial unique index: `UNIQUE (user_id) WHERE status = 'ACTIVE'`.
- `shopping_list_items`: `id`, `list_id`, `client_uuid` (UK), `ingredient_id` (NULL for custom items), `custom_name`, `quantity` (normalized), `unit_id`, `aisle_category_id`, `is_checked`, `is_cleared`, `is_manually_edited`, `cleared_at`, `version` (optimistic locking).
- `shopping_list_item_sources`: `id`, `item_id`, `recipe_id`, `meal_plan_item_id`, `recipe_title_snapshot`, `contributed_quantity`, `unit_id`.
- `user_aisle_preferences`: Composite primary key `(user_id, ingredient_id)`, `aisle_category_id`.

**Testing**: 
- *Backend*: JUnit 5, Mockito, Testcontainers with real PostgreSQL (`postgres:16`, not the `pgvector` image because 040 does not use vector embeddings; never H2 In-Memory DB), JaCoCo coverage $\ge 70\%$ for the `service` package.
- *Frontend*: Vitest, React Testing Library, Mock Service Worker (`msw`) to simulate an offline network interceptor.

**Target Platform**: Linux server (containerized with Docker Compose); modern web browsers (Chrome, Safari, Firefox, Edge) on desktop and mobile from 360 px upward.

**Project Type**: Layered web application (RESTful API backend + Single Page Application frontend).

**Performance Goals**: 
- On-screen checkbox/strikethrough response latency: $< 50\text{ms}$ (Optimistic UI, SC-002).
- Offline synchronization time after connectivity returns: $< 3.0\text{s}$ (SC-003).
- Aggregation time for all ingredients in a 21-meal week ($80 - 120$ raw ingredients): $< 1.0\text{s}$ (SC-004).
- Backend read/write API response time P95: $< 500\text{ms}$.

**Constraints**: 
- Exactly one active list per user (FR-001).
- Maximum 200 items/list (enforced in the backend Service and pre-checked on the client).
- No mutation data loss while offline, even if Access/Refresh Tokens expire.
- Every operation checks ownership with `user_id = principal`, preventing IDOR (Insecure Direct Object Reference).

**Scale/Scope**: Six supermarket aisle categories, approximately 10,000 referenced recipes, one main checklist screen + one pre-import modal + one history drawer.

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Constitution Principle | Assessment | Resolution & Notes |
|---|:---:|---|
| **I. Spec Before Code** | ⚠️ Conditional | `specs/040-shopping-list/spec.md` is `Draft (Ready for Review)`. This plan serves as the technical design basis. **Do not write implementation code** until the specification is formally accepted and the branch has a proper Jira task assigned. Do not invent a Jira key `SCRUM-xx` or independently modify the spec or constitution. |
| **II. Test-First (Non-Negotiable)** | ✅ PASS | Every acceptance scenario and Functional Requirement (FR-001 through FR-025) is mapped in the test matrix in Section 7. The Service layer commits to JaCoCo $\ge 70\%$. Integration tests use real PostgreSQL 16 through Testcontainers, not H2. Scenarios that cannot be automated in CI (real Airplane Mode, measuring latency $<50\text{ms}$) are handed over as Manual Test Cases in `docs/test/`. |
| **III. Simple Layered Architecture** | ✅ PASS | Follows Controller $\to$ Service $\to$ Repository $\to$ Database. Uses Record DTOs throughout, without exposing entities. Organized by feature under `com.wikicook.shoppinglist`. Communicates with Meal Planner (030/031), Recipe (060), and Profile (011) through Service Facades/Ports (specified in `contracts/integration.md`), without cross-module table joins in repositories. |
| **IV. Security & User Data** | ✅ PASS | Spring Security protects every `/api/v1/shopping-lists/**` endpoint. `user_id` comes from `SecurityContext`; URL path `{id}` requires ownership checks (`user_id = principal`), returning 403 Forbidden on violation. Strict server-side Bean Validation (`@Valid`). Errors use RFC 7807 `ProblemDetail` without stack traces. Offline behavior safely preserves the mutation queue when tokens expire. |
| **V. Consistent User Experience** | ✅ PASS | Responsive UI from 360 px, centralized Vietnamese strings. Reuses system Button, Toast, and Modal components. **Pagination decision**: The active list has at most 200 items and is not paginated to support a smooth, uninterrupted shopping checklist (consistent with the personal-list purpose of Constitution V); `ARCHIVED` history lists use standard pagination (`page`, `size`). |
| **Technical Constraints** | ✅ PASS | Java 21, Spring Boot 3, Flyway migration (`V401__shopping_lists.sql`), React 18 Vite, RESTful JSON `/api/v1`. |

**Phase 0 Gate Result**: **PASS (Eligible for Research & Design)**.  
**Re-check After Phase 1**: **PASS** – The relational schema fully matches full-schema 4.8; API contracts cover all 14 endpoints; module boundaries are respected.

---

## Project Structure

### Documentation (this feature)

```text
specs/040-shopping-list/
├── plan.md              # Overall implementation plan (this file)
├── research.md          # Phase 0: Technical decisions R1 - R7
├── data-model.md        # Phase 1: DB schema, JPA Entities, Record DTOs, IndexedDB
├── quickstart.md        # Phase 1: Local setup & cURL testing guide
├── checklists/
│   └── requirements.md  # Requirements checklist
└── contracts/           # Phase 1: API and inter-module contracts
    ├── openapi.yaml     # REST API contracts in OpenAPI 3.0.3
    └── integration.md   # Service Facades / Ports for 011, 020, 030, 031, 060
```

### Source Code (repository root)

```text
src/
├── backend/                     # Maven multi-module (SCRUM-48 skeleton)
│   ├── pom.xml                  # parent: wikicook-db, wikicook-ai, wikicook-app
│   ├── docker-compose.yml       # PostgreSQL 16 + pgvector, Flyway "migrate" tool
│   ├── wikicook-db/src/main/resources/db/migration/
│   │   └── V401__shopping_lists.sql
│   └── wikicook-app/
│       ├── pom.xml
│       └── src/
│           ├── main/
│           │   ├── java/com/wikicook/shoppinglist/
│           │   │   ├── controller/
│           │   │   │   ├── ShoppingListController.java
│           │   │   │   ├── ShoppingListSyncController.java
│           │   │   │   └── ShoppingListExceptionHandler.java
│           │   │   ├── service/
│           │   │   │   ├── ShoppingListService.java
│           │   │   │   ├── IngredientAggregationService.java
│           │   │   │   ├── UnitConversionService.java
│           │   │   │   └── ShoppingListSyncService.java
│           │   │   ├── repository/
│           │   │   │   ├── ShoppingListRepository.java
│           │   │   │   ├── ShoppingListItemRepository.java
│           │   │   │   ├── ShoppingListItemSourceRepository.java
│           │   │   │   └── UserAislePreferenceRepository.java
│           │   │   ├── domain/
│           │   │   │   ├── ShoppingList.java
│           │   │   │   ├── ShoppingListItem.java
│           │   │   │   ├── ShoppingListItemSource.java
│           │   │   │   ├── UserAislePreference.java
│           │   │   │   └── ShoppingListStatus.java
│           │   │   ├── dto/
│           │   │   │   ├── ShoppingListResponse.java
│           │   │   │   ├── AisleGroupDto.java
│           │   │   │   ├── ShoppingListItemDto.java
│           │   │   │   ├── ItemProvenanceDto.java
│           │   │   │   ├── CreateCustomItemRequest.java
│           │   │   │   ├── UpdateItemRequest.java
│           │   │   │   ├── ImportRecipeRequest.java
│           │   │   │   ├── ImportMealPlanRequest.java
│           │   │   │   ├── SyncBatchRequest.java
│           │   │   │   └── SyncBatchResponse.java
│           │   │   └── port/
│           │   │       ├── RecipeDetailPort.java
│           │   │       ├── MealPlanHandoffPort.java
│           │   │       └── DietaryProfilePort.java
│           │   └── resources/
│           │       └── application.yml
│           └── test/java/com/wikicook/shoppinglist/
│               ├── service/
│               │   ├── IngredientAggregationServiceTest.java
│               │   ├── UnitConversionServiceTest.java
│               │   └── ShoppingListSyncServiceTest.java
│               └── controller/
│                   ├── ShoppingListControllerIT.java
│                   └── ShoppingListRepositoryIT.java
└── frontend/
    └── src/
        ├── features/shopping-list/
        │   ├── components/
        │   │   ├── ChecklistScreen.tsx
        │   │   ├── AisleSection.tsx
        │   │   ├── ShoppingItemRow.tsx
        │   │   ├── PreImportBottomSheet.tsx
        │   │   ├── ItemProvenanceModal.tsx
        │   │   ├── AddCustomItemDialog.tsx
        │   │   └── ArchivedHistoryDrawer.tsx
        │   ├── hooks/
        │   │   ├── useShoppingList.ts
        │   │   ├── useOfflineSync.ts
        │   │   └── useIngredientAggregation.ts
        │   └── services/
        │       ├── shoppingListApi.ts
        │       └── offlineShoppingDb.ts
        └── styles/
            └── print-checklist.css
```

---

## Complexity Tracking

Under Constitution **Principle III** (YAGNI & Simple Architecture), every additional library or design pattern must be justified and compared with simpler alternatives:

| Additional Component | Rationale | Rejected Simpler Alternatives & Reasons |
|---|---|---|
| **Frontend `idb` Library (v8.x)** | Compact (~1.1 KB gzipped) Promise wrapper for the browser's native IndexedDB API. Needed for asynchronous transactional reads/writes without UI lag while shopping (SC-002). | Native `indexedDB`: verbose callbacks, prone to transaction leaks; `localStorage`: synchronous (blocks the main thread while scrolling) and its 5MB limit is unsafe; Dexie.js: excessive functionality (~25 KB) beyond requirements (violates YAGNI). |
| **Service Facade / Port Structure** | Allows module 040 to read data from 060 (recipes), 030/031 (weekly plans), and 011 (dietary profiles) without circular dependencies or cross-module SQL joins. | Direct SQL joins in Repository: break module boundaries, increase Flyway merge conflicts, and hinder isolated testing; Microservices/gRPC: excessive and unsuitable for the course project. |
| **Java Unit Conversion Instead of Database Tables** | Shopping conversions are limited to standard metric units ($g \leftrightarrow kg, ml \leftrightarrow l$). Logic in `UnitConversionService` runs quickly in memory without database queries for each calculation. | Complex `unit_conversions` and `units` tables: require two extra database tables and slow joins, already rejected by the team in `full-schema.md`, Section 5, row 8. |
| **Canonical Server Engine vs Client Mirror** | The server remains the Single Source of Truth for aggregation and aisle assignment. The client maintains simplified mirrored logic for instant previews and offline calculations. | Server-only calculations: users cannot see aggregated quantities while offline in a basement; client-only calculations: data is vulnerable to tampering or inconsistencies across devices/platforms. |

---

## Traceability & Verification Matrix

Comprehensive mapping from **Functional Requirements (FR)** and **Success Criteria (SC)** to specific verification approaches:

| Requirement ID | Business Summary | Implementation Approach & Source Location | Test Type & Test Case Name |
| :---: | :--- | :--- | :--- |
| **FR-001** | Exactly one active list/user | Partial Unique Index `uq_shopping_lists_active_user` + `ShoppingListService.getActiveList()` | `Integration`: `ShoppingListRepositoryIT.testOnlyOneActiveListPerUser()` |
| **FR-002** | Recipe Detail import button with serving multiplier | `POST /import/recipe` + `RecipeDetailPort` + `PreImportBottomSheet` | `Integration`: `ShoppingListControllerIT.testImportRecipeWithServings()`<br>`Frontend`: `PreImportBottomSheet.test.tsx` |
| **FR-003** | Serving scaling & Ceiling for countable units | `IngredientAggregationService.scaleAndCeil()` | `Unit`: `IngredientAggregationServiceTest.testDiscreteUnitCeilingRule()` (0.5 egg $\to$ 1) |
| **FR-004** | Weekly Meal Plan import (MERGE / ARCHIVE) | `POST /import/meal-plan` + `MealPlanHandoffPort` | `Integration`: `ShoppingListControllerIT.testImportMealPlanMergeAndArchive()` |
| **FR-005** | Deduplication by `ingredient_id` | Group by Map key `ingredient_id` in `IngredientAggregationService` | `Unit`: `IngredientAggregationServiceTest.testDeduplicationByIngredientId()` |
| **FR-006** | Aggregate compatible metric quantities ($g/kg, ml/l$) | `UnitConversionService.aggregateMetric()` | `Unit`: `UnitConversionServiceTest.testAggregateGramsAndKilograms()` (500g + 1kg = 1.5kg) |
| **FR-007** | Bidirectional metric conversion ($<1kg \to g, \ge 1000g \to kg$) | `UnitConversionService.normalizeAndFormat()` | `Unit`: `UnitConversionServiceTest.testBidirectionalConversionThresholds()` |
| **FR-008** | Retain sub-rows for qualitative units ("thìa", "tép") | `IngredientAggregationService.handleIncompatibleUnits()` | `Unit`: `IngredientAggregationServiceTest.testQualitativeUnitsIsolated()` |
| **FR-009** | Persist provenance snapshots | `shopping_list_item_sources` table | `Integration`: `ShoppingListControllerIT.testProvenanceSnapshotPersistence()` |
| **FR-010** | Categorize into six standard supermarket aisles | Enum / Table `aisle_categories` | `Unit`: `IngredientAggregationServiceTest.testAisleCategorizationDefault()` |
| **FR-011** | Manual aisle changes + remembered custom aisles | `PATCH /items/{uuid}` with `aisleCategoryId`; upsert `user_aisle_preferences` when `rememberAisle=true` | `Integration`: `ShoppingListControllerIT.testChangeItemAisle()` + `UserAislePreferenceRepositoryIT.testUpsertPreference()` |
| **FR-012** | Manually entered item name (Custom item) | `POST /items` + check `num_nonnulls` | `Integration`: `ShoppingListControllerIT.testAddCustomItemGeneralAisle()` |
| **FR-013** | Per-item checkbox, minimum 48×48 touch target | Checklist UI checkbox 48px | `Frontend`: `ChecklistScreen.test.tsx` (touch target checks) |
| **FR-014** | Update checkmark, strikethrough, and progress counter within <50ms | `PATCH /items/{uuid}` + Optimistic UI | `Frontend`: `ChecklistScreen.test.tsx` + `Manual`: `TC-MAN-040-02` (measure <50ms) |
| **FR-015** | Move checked items into collapsible "Đã mua" group | React State Filter / Grouping | `Frontend`: `ChecklistScreen.test.tsx` (check progress X/Y) |
| **FR-016** | Automatically uncheck an already checked item when more is merged | `ShoppingListService.mergeItem()` checks `isChecked` | `Unit`: `ShoppingListServiceTest.testMergeIntoCheckedItemResetsToUnchecked()` |
| **FR-017** | Manual quantity edits flag `is_manually_edited` without changing source recipe | `PATCH /items/{uuid}` quantity → set `is_manually_edited=true` | `Integration`: `ShoppingListControllerIT.testManualQuantityEditSetsFlag()` |
| **FR-018** | Soft-clear + 5s undo + cleared-item visibility toggle + preservation on archive | `POST /active/clear-purchased`; `POST /active/undo-clear` with `clearBatchId` | `Integration`: `ShoppingListControllerIT.testSoftClearPurchasedItems()` + `testUndoClearPurchasedItems()` |
| **FR-019** | Cache the entire active list on the client; offline read/check/edit/add/delete | IndexedDB (`idb`) + mutation queue | `Manual`: `TC-MAN-040-01` |
| **FR-020** | JWT expiration does not block offline use | IndexedDB Cache + non-blocking auth banner | `Manual`: `TC-MAN-040-01` |
| **FR-021** | Batch sync <3s + LWW + conflict priority for `is_checked = true` | `POST /active/sync` + `ShoppingListSyncService`; OR logic | `Integration`: `ShoppingListSyncServiceIT.testBatchSyncIdempotency()` + `Unit`: `ShoppingListSyncServiceTest.testConflictResolutionCheckedWins()` |
| **FR-022** | Archive + restore (reset `is_checked`/`is_cleared`, retain names/quantities/aisles) | `POST /active/archive` (creates fresh active list in the same transaction); `POST /{id}/restore` | `Integration`: `ShoppingListControllerIT.testArchiveAndCreateFreshActive()` |
| **FR-023** | One-tap plaintext copy to Clipboard | `navigator.clipboard.writeText()` formatted by aisle group | `Frontend`: `useShoppingList.test.ts` (verify generated plaintext) |
| **FR-024** | Ink-saving two-column print format | CSS `@media print` in `print-checklist.css` | `Manual`: `TC-MAN-040-03` |
| **FR-025** | Enforce a maximum of 200 items/list | `ShoppingListService.checkCapacityLimit()` | `Integration`: `ShoppingListControllerIT.testCapacityLimitExceeded422()` |

---

## Manual Test Cases (For Field Scenarios)

These scenarios are registered in `docs/test/test-cases.md` for manual acceptance during the tasks phase:

### TC-MAN-040-01: Offline Field Verification (In-Store Airplane Mode Test)
- **Objective**: Verify uninterrupted operation during supermarket connectivity loss and lossless synchronization within $\le 3$ seconds after connectivity returns (SC-003, FR-020, FR-021).
- **Device**: Smartphone running a mobile browser (Chrome/Safari).
- **Steps**:
   1. Log in to WikiCook and open `/shopping-list` with an existing list of 10 items.
   2. Enable **Airplane Mode** on the phone to fully disconnect Wi-Fi and 4G/5G.
   3. Check four items into the cart; add one custom item "Bánh mì sandwich, 1 ổ"; change one item's quantity from 500g to 1kg.
   4. Verify immediate UI response without a blocking network-error popup.
   5. Disable Airplane Mode to restore Internet connectivity.
- **Expected Results**:
   - Within $\le 3.0$ seconds, the status icon changes from "Đang lưu ngoại tuyến" to "Đã đồng bộ máy chủ".
   - Press F5 on another computer logged into the same account: all four checked items, the newly added item, and the updated quantity are displayed with 100% accuracy.

### TC-MAN-040-02: Measure One-Handed Mobile Checklist Response Latency
- **Objective**: Verify SC-002: checking and striking through items responds within $< 50\text{ms}$ on a mid-range device.
- **Device**: Mid-range Android device (e.g., Snapdragon 680 or equivalent).
- **Steps**:
   1. Open a list containing 50 items on mobile.
   2. Using one hand, check five random items in succession.
- **Expected Results**:
   - Checkbox color, text strikethrough, and progress bar update instantly, without stutter/lag (Optimistic UI does not wait for a network roundtrip).

### TC-MAN-040-03: Verify the Ink-Saving Print Layout (@media print)
- **Objective**: Verify the printing requirement (FR-024).
- **Steps**:
   1. Open the shopping list in a desktop browser.
   2. Press `Ctrl + P` (or `Cmd + P` on macOS).
- **Expected Results**:
   - Headers, navigation, floating buttons, and footers are completely hidden.
   - Content is arranged in two clear black-on-white columns, with `[ ]` checkboxes ready for printing.
