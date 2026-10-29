# Tasks: Automated Smart Shopping List & Ingredient Aggregation (040-shopping-list)

**Input**: Design documents from `specs/040-shopping-list/` (`plan.md`, `spec.md`, `data-model.md`, `contracts/openapi.yaml`, `contracts/integration.md`, `research.md`, `quickstart.md`, `src/constitution.md`)

**Prerequisites**: `plan.md` (required), `spec.md` (required for user stories), `research.md`, `data-model.md`, `contracts/`

**Tests**: Test-First (TDD) approach is **MANDATORY** per Constitution Principle II. Test tasks MUST be written first and fail before implementation begins.

**Organization**: Tasks are grouped by phase and user story to enable independent implementation, rigorous testing, and incremental delivery of each story.

---

## Format: `- [ ] [TaskID] [P?] [Story?] Description with exact file path`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (`[US1]`, `[US2]`, `[US3]`, `[US4]`, `[US5]`)
- **Strict Atomicity**: Exactly 1 file or 1 single verification action per task.
- Exact file paths are derived exclusively from `plan.md` Project Structure. Files required for complete test coverage not initially listed in `plan.md` are annotated as *(bổ sung ngoài Project Structure)*.

---

## Path Conventions

- **Backend Source**: `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/`
- **Backend Tests**: `src/backend/wikicook-app/src/test/java/com/wikicook/shoppinglist/`
- **Flyway Migrations**: `src/backend/wikicook-db/src/main/resources/db/migration/`
- **Frontend Source**: `src/frontend/src/features/shopping-list/`
- **Frontend Styles**: `src/frontend/src/styles/`
- **Manual Test Documentation**: `docs/test/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize project build descriptors, database migrations, and shared feature folder scaffolding.

- [ ] T001 Verify and configure backend Maven dependencies (Java 21, Spring Boot 3.x, Spring Data JPA, Spring Web, Spring Security, Validation, Flyway, Testcontainers PostgreSQL, springdoc OpenAPI) in `src/backend/wikicook-app/pom.xml` (versions in parent `src/backend/pom.xml`)
- [ ] T002 [P] Verify and configure frontend npm package dependencies (React 18, TypeScript, Vite, Tailwind CSS, `idb` v8.x, Vitest, Testing Library, MSW) in `src/frontend/package.json`
- [ ] T003 [P] Create frontend directory structure for shopping list feature modules in `src/frontend/src/features/shopping-list/{components,hooks,services}` and `src/frontend/src/styles/`
- [ ] T004 Create Flyway database migration script defining 4 tables (`shopping_lists`, `shopping_list_items`, `shopping_list_item_sources`, `user_aisle_preferences`) and partial unique index `UNIQUE (user_id) WHERE status = 'ACTIVE'` in `src/backend/wikicook-db/src/main/resources/db/migration/V401__shopping_lists.sql`
- [ ] T005 Verify Flyway migration execution and assert table and index creation against PostgreSQL 16 container (`Run: cd src/backend && docker compose up -d db && docker compose run --rm migrate`, then `mvn -pl wikicook-app -am verify` for the Testcontainers check)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core domain entities, JPA repositories, integration ports, exception handling, and client offline database schema that MUST be completed before any user story can proceed.

**⚠️ CRITICAL**: No user story implementation can begin until this foundational phase is fully complete.

- [ ] T006 [P] Create `ShoppingListStatus` enum (`ACTIVE`, `ARCHIVED`) in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/domain/ShoppingListStatus.java`
- [ ] T007 [P] Create `ShoppingList` JPA entity with partial unique index constraint mapping in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/domain/ShoppingList.java` (depends on T006)
- [ ] T008 [P] Create `ShoppingListItem` JPA entity with `client_uuid` (UK), optimistic lock `version`, normalized `quantity`, and clear state flags in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/domain/ShoppingListItem.java`
- [ ] T009 [P] Create `ShoppingListItemSource` JPA entity for recipe and meal plan contribution provenance snapshots in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/domain/ShoppingListItemSource.java`
- [ ] T010 [P] Create `UserAislePreference` JPA entity with composite key `(user_id, ingredient_id)` in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/domain/UserAislePreference.java`
- [ ] T011 [P] Create `ShoppingListRepository` interface with `findByUserIdAndStatus(UUID userId, ShoppingListStatus status)` query in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/repository/ShoppingListRepository.java` (depends on T007)
- [ ] T012 [P] Create `ShoppingListItemRepository` interface with active items query by list ID and item query by `client_uuid` in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/repository/ShoppingListItemRepository.java` (depends on T008)
- [ ] T013 [P] Create `ShoppingListItemSourceRepository` interface with query by item ID in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/repository/ShoppingListItemSourceRepository.java` (depends on T009)
- [ ] T014 [P] Create `UserAislePreferenceRepository` interface with query by user ID and ingredient ID in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/repository/UserAislePreferenceRepository.java` (depends on T010)
- [ ] T015 Create Foundational JPA Repository integration test verifying entity mappings, foreign keys, and constraints with Testcontainers PostgreSQL 16 in `src/backend/wikicook-app/src/test/java/com/wikicook/shoppinglist/repository/ShoppingListRepositoryIT.java` (depends on T011, T012, T013, T014)
- [ ] T016 [P] Create `ShoppingListExceptionHandler` translating domain exceptions into RFC 7807 `ProblemDetail` structures without leaking internal stack traces in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/controller/ShoppingListExceptionHandler.java`
- [ ] T017 [P] Create `RecipeDetailPort` interface for decoupled communication with Recipe Module (060/020) in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/port/RecipeDetailPort.java`
- [ ] T018 [P] Create `MealPlanHandoffPort` interface for decoupled communication with Meal Planner Module (030/031) in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/port/MealPlanHandoffPort.java`
- [ ] T019 [P] Create `DietaryProfilePort` interface for decoupled communication with User Profile Module (011) in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/port/DietaryProfilePort.java`
- [ ] T020 [P] Create base IndexedDB database helper and schema migration using `idb` library in `src/frontend/src/features/shopping-list/services/offlineShoppingDb.ts`

**Checkpoint**: Foundation ready — Domain model, database schema, data access layer, ports, and offline storage primitives are verified.

---

## Phase 3: User Story 1 - Centralized List Management & Custom Items (Priority: P1 - Core MVP) 🎯 MVP

**Goal**: Deliver a centralized active shopping list lifecycle for home cooks: auto-initialize default active list, add/edit/delete ad-hoc custom grocery items, inline-edit list titles (1–60 chars), archive lists into historical drawer, and restore archived lists with reset purchase states (`isChecked = false`, `isCleared = false`).

**Independent Test**: Navigate to `/shopping-list`. Verify the active list initializes. Add custom item "Nước rửa chén, 1 chai", rename list title, verify persistence upon browser reload, archive the list to history, and restore it back to active state.

### Tests for User Story 1 (Test-First: Write FIRST, ensure they FAIL before implementation) ⚠️

- [ ] T021 [P] [US1] Create unit tests for `ShoppingListService` covering single active list retrieval/creation, capacity check ($\le 200$), title renaming, archive transaction, and restore reset logic in `src/backend/wikicook-app/src/test/java/com/wikicook/shoppinglist/service/ShoppingListServiceTest.java` *(bổ sung ngoài Project Structure)*
- [ ] T022 [P] [US1] Create integration tests with Testcontainers for US1 endpoints (`GET /active`, `PATCH /active`, `POST /items`, `DELETE /items/{clientUuid}`, `GET /history`, `POST /archive`, `POST /{id}/restore`, ownership 403, and capacity limit 422) in `src/backend/wikicook-app/src/test/java/com/wikicook/shoppinglist/controller/ShoppingListControllerIT.java`
- [ ] T023 [P] [US1] Create MSW mock handlers and API client tests for US1 list operations in `src/frontend/src/features/shopping-list/services/shoppingListApi.test.ts` *(bổ sung ngoài Project Structure)*
- [ ] T024 [P] [US1] Create unit tests for `useShoppingList` hook covering active list loading, custom item addition, archive trigger, and restore trigger in `src/frontend/src/features/shopping-list/hooks/useShoppingList.test.ts` *(bổ sung ngoài Project Structure)*
- [ ] T025 [P] [US1] Create component tests for `AddCustomItemDialog` modal (validation of name, quantity, unit, custom aisle assignment) in `src/frontend/src/features/shopping-list/components/AddCustomItemDialog.test.tsx` *(bổ sung ngoài Project Structure)*
- [ ] T026 [P] [US1] Create component tests for `ArchivedHistoryDrawer` (listing historical trips, pagination, restore confirmation trigger) in `src/frontend/src/features/shopping-list/components/ArchivedHistoryDrawer.test.tsx` *(bổ sung ngoài Project Structure)*

### Implementation for User Story 1

- [ ] T027 [P] [US1] Create Java Record DTOs (`ShoppingListResponse`, `ShoppingListItemDto`, `CreateCustomItemRequest`, `UpdateItemRequest`, `ArchiveListResponse`, `PageResponse`) in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/dto/`
- [ ] T028 [US1] Implement `ShoppingListService` for US1 (get/create active list, add custom item to "General" aisle, rename title, capacity validation $\le 200$, archive list + create fresh blank active in 1 transaction, restore archived list with reset status) in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/service/ShoppingListService.java` (depends on T021, T027)
- [ ] T029 [US1] Implement `ShoppingListController` endpoints for US1 (`GET /api/v1/shopping-lists/active`, `PATCH /api/v1/shopping-lists/active`, `POST /api/v1/shopping-lists/active/items`, `DELETE /api/v1/shopping-lists/active/items/{clientUuid}`, `GET /api/v1/shopping-lists/history`, `POST /api/v1/shopping-lists/active/archive`, `POST /api/v1/shopping-lists/{id}/restore`) in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/controller/ShoppingListController.java` (depends on T022, T028)
- [ ] T030 [P] [US1] Implement shared API client class for US1 operations adhering to single API layer rule in `src/frontend/src/features/shopping-list/services/shoppingListApi.ts` (depends on T023)
- [ ] T031 [US1] Implement `useShoppingList` React hook managing active list state, inline title editing, custom item addition, archive, and restore in `src/frontend/src/features/shopping-list/hooks/useShoppingList.ts` (depends on T024, T030)
- [ ] T032 [P] [US1] Implement `AddCustomItemDialog` modal component with touch-friendly input fields in `src/frontend/src/features/shopping-list/components/AddCustomItemDialog.tsx` (depends on T025)
- [ ] T033 [P] [US1] Implement `ArchivedHistoryDrawer` drawer component displaying archived trips and restore actions in `src/frontend/src/features/shopping-list/components/ArchivedHistoryDrawer.tsx` (depends on T026)
- [ ] T034 [US1] Implement main `/shopping-list` view skeleton with default title "Danh sách mua sắm [Ngày/Tháng]", empty state placeholders, and quick import triggers in `src/frontend/src/features/shopping-list/components/ChecklistScreen.tsx` (depends on T031, T032, T033)
- [ ] T035 [US1] Verify User Story 1 acceptance scenarios and run independent test (`Run: mvn test -Dtest=ShoppingListServiceTest,ShoppingListControllerIT && npm test -- run shoppingListApi useShoppingList AddCustomItemDialog ArchivedHistoryDrawer`)

**Checkpoint**: User Story 1 is fully functional and independently verified.

---

## Phase 4: User Story 2 - Intelligent Ingredient Deduplication & Scaling (Priority: P1 - Core MVP) 🎯 MVP

**Goal**: Automatically aggregate ingredients across standalone recipes and weekly meal plans: scale quantities by serving multipliers ($S_{\text{target}} / S_{\text{base}}$), perform bidirectional metric unit conversion ($g \leftrightarrow kg$, $ml \leftrightarrow l$ with 1000 threshold and max 2 decimal precision), round discrete units up (Ceiling rule for quả, củ, trứng), isolate qualitative culinary units ("thìa canh", "tép") on distinct sub-rows, capture provenance snapshots, and provide a pre-import staple exclusion modal.

**Independent Test**: Add Recipe A (300g beef for 2 servings scaled to 4 = 600g). Add Recipe B (400g beef for 4 servings). Verify list displays a single consolidated entry of "1 kg Thịt bò" with provenance showing 600g from Recipe A and 400g from Recipe B.

### Tests for User Story 2 (Test-First: Write FIRST, ensure they FAIL before implementation) ⚠️

- [ ] T036 [P] [US2] Create unit tests for `UnitConversionService` (metric conversion $g \leftrightarrow kg$, $ml \leftrightarrow l$, threshold 1000, $<1\text{kg} \to \text{g}$, continuous 2 decimal rounding) in `src/backend/wikicook-app/src/test/java/com/wikicook/shoppinglist/service/UnitConversionServiceTest.java`
- [ ] T037 [P] [US2] Create unit tests for `IngredientAggregationService` (deduplication by `ingredient_id`, serving multiplier positive 0.5 step, discrete ceiling rule 0.5 egg $\to$ 1 egg, qualitative units isolated, provenance breakdown snapshots, unchecking already purchased items upon merge) in `src/backend/wikicook-app/src/test/java/com/wikicook/shoppinglist/service/IngredientAggregationServiceTest.java`
- [ ] T038 [P] [US2] Create integration tests for recipe and meal plan import endpoints (`POST /import/recipe`, `POST /import/meal-plan` with MERGE and ARCHIVE modes, and `GET /items/{clientUuid}/provenance`) in `src/backend/wikicook-app/src/test/java/com/wikicook/shoppinglist/controller/ShoppingListControllerIT.java`
- [ ] T039 [P] [US2] Create component tests for `PreImportBottomSheet` (ingredient toggle checkboxes, 1-tap staple exclusion button "Bỏ chọn gia vị cơ bản", empty plan error prompt) in `src/frontend/src/features/shopping-list/components/PreImportBottomSheet.test.tsx` *(bổ sung ngoài Project Structure)*
- [ ] T040 [P] [US2] Create component tests for `ItemProvenanceModal` (displaying scaled recipe contributions and original dish titles) in `src/frontend/src/features/shopping-list/components/ItemProvenanceModal.test.tsx` *(bổ sung ngoài Project Structure)*

### Implementation for User Story 2

- [ ] T041 [P] [US2] Create Java Record DTOs (`ImportRecipeRequest`, `ImportMealPlanRequest`, `ItemProvenanceDto`, `RecipeContributionDto`, `ImportResultResponse`) in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/dto/`
- [ ] T042 [US2] Implement `UnitConversionService` (pure in-memory bidirectional metric conversions and continuous decimal rounding) in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/service/UnitConversionService.java` (depends on T036)
- [ ] T043 [US2] Implement `IngredientAggregationService` (core aggregation engine, serving scaling, discrete ceiling rule, qualitative sub-row isolation, excluded ingredients filter, provenance snapshot generation, uncheck purchased item on merge) in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/service/IngredientAggregationService.java` (depends on T037, T042)
- [ ] T044 [US2] Integrate `RecipeDetailPort` and `MealPlanHandoffPort` in `ShoppingListService` to ingest external recipe and meal plan payloads with capacity check ($\le 200$) in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/service/ShoppingListService.java` (depends on T017, T018, T043)
- [ ] T045 [US2] Implement recipe and meal plan import endpoints (`POST /api/v1/shopping-lists/active/import/recipe`, `POST /api/v1/shopping-lists/active/import/meal-plan`, and `GET /api/v1/shopping-lists/active/items/{clientUuid}/provenance`) in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/controller/ShoppingListController.java` (depends on T038, T044)
- [ ] T046 [P] [US2] Implement `useIngredientAggregation` hook managing client-side preview aggregation and scaling multipliers in `src/frontend/src/features/shopping-list/hooks/useIngredientAggregation.ts`
- [ ] T047 [P] [US2] Implement `PreImportBottomSheet` modal component with checklist and 1-tap "Bỏ chọn gia vị cơ bản" button in `src/frontend/src/features/shopping-list/components/PreImportBottomSheet.tsx` (depends on T039)
- [ ] T048 [US2] Implement serving selector + "Add to shopping list" entry button on the 020 recipe detail view (depends on T047) in `src/frontend/src/features/shopping-list/components/` (exact 020 view file TBD - view owned by recipe module; confirm path before implementation)
  - Acceptance: selector rescales every displayed quantity; button opens PreImportBottomSheet with serving count + scaled quantities; manual-edit flag/PATCH behaviour unchanged; no duplicate entry point.
  - Tests (test-first): Vitest selector rescale test; Vitest button tap calls RecipeDetailPort with scaled quantities and opens PreImportBottomSheet; manual 020 check.
- [ ] T049 [P] [US2] Implement `ItemProvenanceModal` drawer component displaying contribution breakdown in `src/frontend/src/features/shopping-list/components/ItemProvenanceModal.tsx` (depends on T040)
- [ ] T050 [US2] Update `shoppingListApi.ts` to include import and provenance fetching functions in `src/frontend/src/features/shopping-list/services/shoppingListApi.ts` (depends on T045)
- [ ] T051 [US2] Verify User Story 2 acceptance scenarios and run independent test (`Run: mvn test -Dtest=UnitConversionServiceTest,IngredientAggregationServiceTest && npm test -- run PreImportBottomSheet ItemProvenanceModal`)

**Checkpoint**: User Story 2 is fully functional and independently verified.

---

## Phase 5: User Story 3 - Supermarket Aisle Categorization & In-Store Checklist (Priority: P1 - Core MVP) 🎯 MVP

**Goal**: Automatically categorize ingredients into 6 supermarket departments (`PRODUCE`, `MEAT_SEAFOOD`, `DAIRY_EGGS`, `PANTRY_SPICES`, `FROZEN`, `GENERAL`), provide an ergonomic single-handed checklist with immediate optimistic feedback, update real-time progress metering, support non-destructive soft-clear (`is_cleared = true`) with a 5-second undo snackbar, and persist custom user aisle preferences (`user_aisle_preferences`).

**Independent Test**: Open a 15-item list across 6 aisles. Tap 5 checkboxes with one hand; verify items strike through, relocate to "Đã mua" subsection, update sticky progress bar, and test soft-clearing purchased items with 5s undo.

### Tests for User Story 3 (Test-First: Write FIRST, ensure they FAIL before implementation) ⚠️

- [ ] T052 [P] [US3] Create integration tests for aisle reclassification (`PATCH /items/{clientUuid}`), aisle preference persistence (`rememberAisle`), soft-clear (`POST /clear-purchased`), and undo-clear (`POST /undo-clear`) in `src/backend/wikicook-app/src/test/java/com/wikicook/shoppinglist/controller/ShoppingListControllerIT.java`
- [ ] T053 [P] [US3] Create component and interaction tests for `ChecklistScreen` (asserting immediate optimistic checkbox toggle, strikethrough styling, dynamic progress meter calculation, and undo snackbar appearance without waiting for network; latency $<50\text{ms}$ is delegated to manual test `TC-MAN-040-02`) in `src/frontend/src/features/shopping-list/components/ChecklistScreen.test.tsx` *(bổ sung ngoài Project Structure)*

### Implementation for User Story 3

- [ ] T054 [P] [US3] Create Java Record DTOs (`AisleGroupDto`, `ChangeAisleRequest`, `ClearPurchasedResponse`, `UndoClearRequest`) in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/dto/`
- [ ] T055 [US3] Implement aisle grouping logic, `UserAislePreference` upsert, soft-clear batching (`is_cleared = true`, `cleared_at`), and undo-clear within 5-second window in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/service/ShoppingListService.java` (depends on T052, T054)
- [ ] T056 [US3] Add aisle change and soft-clear/undo endpoints (`PATCH /api/v1/shopping-lists/active/items/{clientUuid}`, `POST /api/v1/shopping-lists/active/clear-purchased`, `POST /api/v1/shopping-lists/active/undo-clear`) in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/controller/ShoppingListController.java` (depends on T055)
- [ ] T057 [P] [US3] Implement `ShoppingItemRow` component with $\ge 48\times 48\text{px}$ touch target checkbox, line-through styling, quantity badge, manual edit indicator `(đã chỉnh sửa)`, and optimistic state toggle in `src/frontend/src/features/shopping-list/components/ShoppingItemRow.tsx`
- [ ] T058 [P] [US3] Implement `AisleSection` accordion component displaying aisle header icon, badge count of unpurchased items, and collapsible "Đã mua" subsection in `src/frontend/src/features/shopping-list/components/AisleSection.tsx` (depends on T057)
- [ ] T059 [US3] Assemble in-store checklist view in `ChecklistScreen.tsx` with sticky progress bar ("Đã mua X/Y món (%)"), soft-clear action, 5-second undo snackbar, and "Xem các món đã dọn dẹp" toggle (`?includeCleared=true`) in `src/frontend/src/features/shopping-list/components/ChecklistScreen.tsx` (depends on T053, T058)
- [ ] T060 [US3] Verify User Story 3 acceptance scenarios and run independent test (`Run: mvn test -Dtest=ShoppingListControllerIT#testAisleAndSoftClear && npm test -- run ChecklistScreen`)

**Checkpoint**: Core MVP (User Stories 1, 2, and 3) is complete, providing a fully functional smart shopping list and in-store checklist.

---

## Phase 6: User Story 4 - Local-First Offline Resilience & In-Store Background Sync (Priority: P2)

**Goal**: Guarantee uninterrupted in-store shopping in supermarket basements with zero mobile signal: store active lists and mutation events in IndexedDB (`idb`), allow unrestricted local reading and checking even if JWT expires, and perform resilient background synchronization within 3.0s upon reconnect using Last-Write-Wins with explicit priority for `is_checked = true`.

**Independent Test**: Load active list on mobile browser. Enable Airplane Mode. Check 4 items, edit 1 quantity, and add 1 custom item. Verify UI updates instantly with subtle offline indicator and zero blocking modals. Disable Airplane Mode; verify all queued mutations sync to backend within 3.0s.

### Tests for User Story 4 (Test-First: Write FIRST, ensure they FAIL before implementation) ⚠️

- [ ] T061 [P] [US4] Create unit tests for `ShoppingListSyncService` (verifying batch mutation processing, idempotency by `client_uuid`, LWW conflict resolution with `is_checked = true` priority, and 404 handling for mutations on non-existent items) in `src/backend/wikicook-app/src/test/java/com/wikicook/shoppinglist/service/ShoppingListSyncServiceTest.java`
- [ ] T062 [P] [US4] Create unit and mock tests for `useOfflineSync` hook (offline event detection, mutation queueing, automatic sync trigger on network restore, non-blocking offline banner, bypass auth redirect while offline) in `src/frontend/src/features/shopping-list/hooks/useOfflineSync.test.ts` *(bổ sung ngoài Project Structure)*

### Implementation for User Story 4

- [ ] T063 [P] [US4] Create Java Record DTOs (`SyncBatchRequest`, `ItemMutationDto`, `SyncBatchResponse`, `ItemSyncResultDto`) in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/dto/`
- [ ] T064 [US4] Implement `ShoppingListSyncService` executing idempotent batch synchronization with optimistic locking and `is_checked` priority rule in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/service/ShoppingListSyncService.java` (depends on T061, T063)
- [ ] T065 [US4] Implement `ShoppingListSyncController` endpoint (`POST /api/v1/shopping-lists/active/sync`) returning per-item accepted/rejected statuses in `src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/controller/ShoppingListSyncController.java` (depends on T064)
- [ ] T066 [US4] Expand `offlineShoppingDb.ts` to manage offline mutation queue store, pending status flags, and `client_uuid` generation in `src/frontend/src/features/shopping-list/services/offlineShoppingDb.ts` (depends on T020)
- [ ] T067 [US4] Implement `useOfflineSync` hook with `window.addEventListener('online'/'offline')`, background sync dispatcher, non-blocking offline indicator, and auth bypass guard in `src/frontend/src/features/shopping-list/hooks/useOfflineSync.ts` (depends on T062, T066)
- [ ] T068 [US4] Integrate offline banner ("Đang ngoại tuyến - Dữ liệu được lưu trên máy"), sync status transitions ("Đang lưu ngoại tuyến" $\to$ "Đã đồng bộ"), and logout unsaved mutation warning into `ChecklistScreen.tsx` in `src/frontend/src/features/shopping-list/components/ChecklistScreen.tsx` (depends on T067)
- [ ] T069 [US4] Verify User Story 4 acceptance scenarios and run independent test (`Run: mvn test -Dtest=ShoppingListSyncServiceTest && npm test -- run useOfflineSync`)

**Checkpoint**: User Story 4 is complete, guaranteeing high offline resilience in subterranean supermarket dead zones.

---

## Phase 7: User Story 5 - Export, Plaintext Copy & Print-Friendly Formatting (Priority: P3 - Utility)

**Goal**: Provide 1-tap structured plaintext export formatted with aisle headers and `[ ]` brackets for messaging apps (Zalo, SMS), and print-optimized high-contrast 2-column checklist layout via client-side CSS `@media print` without backend API overhead.

**Independent Test**: From an active 15-item list, tap "Sao chép danh sách". Paste into a text editor; verify structured aisle headers and `[ ]` brackets. Trigger browser print (Ctrl+P); verify layout formats into an ink-saving 2-column checklist hiding web navigation.

### Tests for User Story 5 (Test-First: Write FIRST, ensure they FAIL before implementation) ⚠️

- [ ] T070 [P] [US5] Create unit tests for plaintext list generation utility (verifying header formatting, aisle grouping, and `[ ]` item brackets) in `src/frontend/src/features/shopping-list/services/formatShoppingListText.test.ts` *(bổ sung ngoài Project Structure)*
- [ ] T071 [P] [US5] Create component and style render tests for print layout (verifying `@media print` classes, 2-column layout, and hidden elements) in `src/frontend/src/features/shopping-list/components/print-checklist.test.tsx` *(bổ sung ngoài Project Structure)*

### Implementation for User Story 5

- [ ] T072 [P] [US5] Implement `formatShoppingListText` utility function and clipboard copy handler with system toast notification in `src/frontend/src/features/shopping-list/services/shoppingListApi.ts` (depends on T070)
- [ ] T073 [P] [US5] Create print stylesheet `@media print` (monochrome, 2-column ink-saving paper format, hiding navigation headers, buttons, footers, and floating controls) in `src/frontend/src/styles/print-checklist.css` (depends on T071)
- [ ] T074 [US5] Add "Sao chép danh sách" and "In danh sách" action buttons to header menu in `ChecklistScreen.tsx` and link print stylesheet in `src/frontend/src/features/shopping-list/components/ChecklistScreen.tsx` (depends on T072, T073)
- [ ] T075 [US5] Verify User Story 5 acceptance scenarios and run independent test (`Run: npm test -- run formatShoppingListText print-checklist`)

**Checkpoint**: All 5 User Stories are complete, tested, and independently verified.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Validate cross-cutting non-functional requirements, record manual field test cases, run end-to-end verification flows, and guarantee deployment readiness.

- [ ] T076 Verify 200 items/list capacity limit enforcement at both service and controller layers (`Run: mvn test -Dtest=ShoppingListControllerIT#testCapacityLimitExceeded422`)
- [ ] T077 [P] Add manual test case specifications (`TC-MAN-040-01` Offline Mode & JWT Expiry, `TC-MAN-040-02` In-Store $<50\text{ms}$ Checklist Latency, `TC-MAN-040-03` Print Ink-Saving Layout) to `docs/test/test-cases.md` and link in `docs/test/test-plan.md`
- [ ] T078 Execute complete end-to-end cURL verification workflow (6 steps: active list $\to$ custom item $\to$ import recipe $\to$ offline sync $\to$ soft-clear/undo $\to$ archive) per `specs/040-shopping-list/quickstart.md`
- [ ] T079 Verify runtime springdoc OpenAPI endpoint generation matches `specs/040-shopping-list/contracts/openapi.yaml` (14 operations across 12 paths with RFC 7807 `ProblemDetail`)
- [ ] T080 Execute full project verification and assert JaCoCo service layer code coverage $\ge 70\%$ and frontend production build (`Run: mvn clean verify && cd src/frontend && npm run build && npm test`)
- [ ] T081 Final Constitution re-check and Definition of Done (DoD) verification before opening Pull Request for peer review

---

## Dependencies & Execution Order

### Phase Dependencies

```
┌──────────────────────────────────────┐
│ Phase 1: Setup (T001 - T005)         │
└──────────────────┬───────────────────┘
                   │
┌──────────────────▼───────────────────┐
│ Phase 2: Foundational (T006 - T020)  │  ◄── BLOCKS ALL USER STORIES
└──────────────────┬───────────────────┘
                   │
┌──────────────────▼───────────────────┐
│ Phase 3: User Story 1 (T021 - T035)  │  ◄── P1 Core MVP (List & Items Container)
└──────────┬───────────────────────────┘
           │
           ├───────────────────────────────────────┐
┌──────────▼───────────────────────────┐           │
│ Phase 4: User Story 2 (T036 - T051)  │           │
│ P1 Deduplication & Scaling Engine    │           │
└──────────┬───────────────────────────┘           │
           │                                       │
┌──────────▼───────────────────────────┐           │
│ Phase 5: User Story 3 (T052 - T060)  │           │
│ P1 In-Store Checklist & Aisle Sort   │           │
└──────────┬───────────────────────────┘           │
           │                                       │
┌──────────▼───────────────────────────┐           │
│ Phase 6: User Story 4 (T061 - T069)  │           │
│ P2 Local-First Offline & Sync        │           │
└──────────┬───────────────────────────┘           │
           │                                       │
┌──────────▼───────────────────────────┐           │
│ Phase 7: User Story 5 (T070 - T075)  │ ◄─────────┘ (Depends only on US1 List container)
│ P3 Plaintext Export & Print Layout   │
└──────────┬───────────────────────────┘
           │
┌──────────▼───────────────────────────┐
│ Phase 8: Polish (T076 - T081)        │
└──────────────────────────────────────┘
```

### User Story Dependencies

- **User Story 1 (P1)**: Depends on Foundational Phase (T006–T020). Has no dependencies on other stories. Serves as the core container.
- **User Story 2 (P1)**: Depends on US1 (`ShoppingListService` active list container) to store aggregated ingredients and provenance records.
- **User Story 3 (P1)**: Depends on US1 (list items) and US2 (deduplicated ingredients) to organize items into aisles and power checklist mechanics.
- **User Story 4 (P2)**: Depends on US1 (list state) and US3 (item checked/cleared states) to manage offline mutation queuing and conflict resolution.
- **User Story 5 (P3)**: Depends only on US1 (item list data) to format text and render print views. Can proceed in parallel with US2/US3/US4 if staffed.

### Internal Ordering Rules Within Each User Story

1. **Test-First (Constitution II)**: Unit and Integration test tasks MUST be implemented first and verified failing before writing business logic.
2. **Layer Order**: Models/DTOs $\to$ Services/Engine $\to$ Controllers/APIs $\to$ UI Components $\to$ Integration.
3. **Independent Verification**: Each story concludes with a checkpoint task verifying its independent test criteria before proceeding to the next priority.

---

## Parallel Execution Examples

### Parallel Opportunities in Phase 1 (Setup)
```bash
# Developer A (Backend Setup):
Task: "T001 Verify and configure backend Maven dependencies in src/backend/wikicook-app/pom.xml"

# Developer B (Frontend Setup):
Task: "T002 Verify and configure frontend npm package dependencies in src/frontend/package.json"
Task: "T003 Create frontend directory structure in src/frontend/src/features/shopping-list/"
```

### Parallel Opportunities in Phase 2 (Foundational Entities & Repositories)
```bash
# Launch domain entity creation concurrently:
Task: "T007 Create ShoppingList JPA entity"
Task: "T008 Create ShoppingListItem JPA entity"
Task: "T009 Create ShoppingListItemSource JPA entity"
Task: "T010 Create UserAislePreference JPA entity"

# Launch repository interfaces concurrently:
Task: "T011 Create ShoppingListRepository interface"
Task: "T012 Create ShoppingListItemRepository interface"
Task: "T013 Create ShoppingListItemSourceRepository interface"
Task: "T014 Create UserAislePreferenceRepository interface"
```

### Parallel Opportunities in Phase 3 (User Story 1 Tests)
```bash
# Launch all US1 test writing in parallel before implementation:
Task: "T021 Create unit tests for ShoppingListService"
Task: "T022 Create integration tests with Testcontainers in ShoppingListControllerIT"
Task: "T023 Create MSW mock handlers in shoppingListApi.test.ts"
Task: "T024 Create unit tests for useShoppingList hook"
Task: "T025 Create component tests for AddCustomItemDialog"
Task: "T026 Create component tests for ArchivedHistoryDrawer"
```

### Parallel Opportunities Across Workstreams (After US1 Foundation)
```bash
# Backend Developer:
Task: "T036 UnitConversionServiceTest" -> "T042 UnitConversionService" -> "T043 IngredientAggregationService"

# Frontend Developer:
Task: "T039 PreImportBottomSheet.test.tsx" -> "T047 PreImportBottomSheet.tsx"
Task: "T071 print-checklist.test.tsx" -> "T073 print-checklist.css" (US5 parallel work)
```

---

## Implementation Strategy

### 1. MVP First (P1 Stories: US1 + US2 + US3)

1. Complete **Phase 1: Setup** (T001–T005) $\to$ Build environment ready.
2. Complete **Phase 2: Foundational** (T006–T020) $\to$ Database schema, entities, and repositories locked.
3. Complete **Phase 3: User Story 1** (T021–T035) $\to$ Base shopping list container and custom item management functional.
4. Complete **Phase 4: User Story 2** (T036–T051) $\to$ Dynamic recipe scaling, metric deduplication, and provenance operational.
5. Complete **Phase 5: User Story 3** (T052–T060) $\to$ Supermarket aisle grouping, single-handed checklist, and soft-clear ready.
6. **STOP & DEMO MVP**: At this checkpoint, WikiCook has a market-competitive smart shopping list ready for kitchen and supermarket use.

### 2. Incremental Delivery (P2 & P3 Extensions)

1. Deliver **Phase 6: User Story 4** (T061–T069) $\to$ Adds offline resilience and background sync for basement supermarkets.
2. Deliver **Phase 7: User Story 5** (T070–T075) $\to$ Adds 1-tap plaintext sharing (Zalo/SMS) and print-friendly formatting.
3. Deliver **Phase 8: Polish** (T076–T081) $\to$ Executes 200-item stress test, documents manual field tests, asserts $\ge 70\%$ JaCoCo service coverage, and finalizes PR.

---

## Notes

- Every task strictly matches the format `- [ ] [TaskID] [P?] [Story?] Description with exact file path`.
- No tasks are combined; 1 task corresponds strictly to 1 file or 1 discrete verification action.
- No Jira ticket numbers or branch renames are introduced.
- Strict adherence to Constitution Principle II (Test-First) is enforced across all implementation phases.
