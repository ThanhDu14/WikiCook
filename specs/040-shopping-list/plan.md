# Implementation Plan: Automated Smart Shopping List & Ingredient Aggregation

**Branch**: `feature/040-shopping-list` | **Date**: 2026-10-24 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/040-shopping-list/spec.md` (Status: Draft / Ready for Review; giả định đã được team phê chuẩn làm baseline lập kế hoạch kỹ thuật).

---

## Summary

Module **040-shopping-list** cung cấp giải pháp chuyển giao liền mạch từ kế hoạch thực đơn sang thực tế đi chợ siêu thị cho người dùng WikiCook:
1. **Tổng hợp đa nguồn (Multi-Source Ingestion)**: Thu nạp nguyên liệu từ công thức đơn lẻ kèm hệ số nhân khẩu phần ($S_{\text{target}} / S_{\text{base}}$), gộp hàng loạt từ lịch ăn tuần 7 ngày $\times$ 3 bữa (Spec 030/031), hoặc thêm món tạp hóa/gia dụng thủ công.
2. **Động cơ gộp thông minh & Quy đổi đơn vị**: Tự động gom nhóm theo mã nguyên liệu chuẩn (`ingredient_id`), quy đổi đơn vị metric hai chiều ($g \leftrightarrow kg$, $ml \leftrightarrow l$) bằng mã nguồn Java, làm tròn trần (`Ceiling`) cho đơn vị đếm (quả, củ, trứng) ở danh sách mua nhưng bảo toàn số lẻ ở chi tiết nguồn gốc (Provenance), và giữ nguyên các đơn vị định tính ("thìa canh", "tép") ở dòng độc lập.
3. **Phân nhóm quầy siêu thị & Tiện ích Checklist**: Tự động phân loại vào 6 quầy hàng chuẩn (`PRODUCE`, `MEAT_SEAFOOD`, `DAIRY_EGGS`, `PANTRY_SPICES`, `FROZEN`, `GENERAL`), hỗ trợ ghi nhớ quầy tùy biến (`user_aisle_preferences`), giao diện checklist 1 tay phản hồi tức thì ($< 50\text{ms}$ optimistic UI), dọn dẹp mềm non-destructive (`is_cleared = true`) kèm snackbar hoàn tác trong 5 giây.
4. **Ngoại tuyến kiên cường (Local-First Offline Resilience)**: Sử dụng `IndexedDB` (thư viện `idb`) lưu trữ danh sách và hàng đợi mutation tại client; sinh sẵn `client_uuid` cho mọi item; khi có mạng lại tự động batch sync lên server với cơ chế Last-Write-Wins có bảo vệ tuyệt đối trạng thái đã mua (`is_checked = true`); giữ an toàn hàng đợi ngay cả khi token hết hạn lúc đang ở tầng hầm siêu thị.
5. **Xuất bản & In ấn thuần Client**: Sao chép văn bản thuần (plaintext) 1-chạm gửi qua Zalo/SMS và định dạng in ấn `@media print` 2 cột tiết kiệm mực (không sinh endpoint thừa).

---

## Technical Context

**Language/Version**: Java 21 (LTS) cho backend; TypeScript 5.x + React 18 cho frontend.

**Primary Dependencies**: 
- *Backend*: Spring Boot 3.x (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`), Flyway Migration, `springdoc-openapi-starter-webmvc-ui` (v2.x).
- *Frontend*: Vite, Tailwind CSS 3.4, thư viện `idb` (v8.x, Promise wrapper nhẹ cho IndexedDB, xem mục Complexity Tracking).

**Storage**: PostgreSQL 16. Sử dụng đúng 4 bảng theo [Mục 4.8 của `full-schema.md`](../../docs/analysis-and-design/database/full-schema.md#48-shopping-list-040--d) được quản lý qua Flyway migration `V401__shopping_lists.sql`:
- `shopping_lists`: Khóa chính `id`, `user_id`, `title`, `status`, `archived_at`. Chỉ mục duy nhất có điều kiện: `UNIQUE (user_id) WHERE status = 'ACTIVE'`.
- `shopping_list_items`: `id`, `list_id`, `client_uuid` (UK), `ingredient_id` (NULL với món tự nhập), `custom_name`, `quantity` (normalized), `unit_id`, `aisle_category_id`, `is_checked`, `is_cleared`, `is_manually_edited`, `cleared_at`, `version` (optimistic locking).
- `shopping_list_item_sources`: `id`, `item_id`, `recipe_id`, `meal_plan_item_id`, `recipe_title_snapshot`, `contributed_quantity`, `unit_id`.
- `user_aisle_preferences`: Khóa chính phức hợp `(user_id, ingredient_id)`, `aisle_category_id`.

**Testing**: 
- *Backend*: JUnit 5, Mockito, Testcontainers với PostgreSQL thật (`postgres:16`, không dùng image `pgvector` vì 040 không dùng vector embedding; tuyệt đối không dùng H2 In-Memory DB), JaCoCo coverage $\ge 70\%$ cho package `service`.
- *Frontend*: Vitest, React Testing Library, Mock Service Worker (`msw`) để giả lập offline network interceptor.

**Target Platform**: Linux server (Docker Compose container hóa); trình duyệt web hiện đại (Chrome, Safari, Firefox, Edge) trên desktop và mobile từ 360 px trở lên.

**Project Type**: Ứng dụng web phân lớp (RESTful API backend + Single Page Application frontend).

**Performance Goals**: 
- Độ trễ phản hồi tương tác checkbox/gạch bỏ trên màn hình: $< 50\text{ms}$ (Optimistic UI, SC-002).
- Thời gian đồng bộ ngoại tuyến khi có mạng: $< 3.0\text{s}$ (SC-003).
- Thời gian gộp toàn bộ nguyên liệu của tuần 21 bữa ($80 - 120$ nguyên liệu thô): $< 1.0\text{s}$ (SC-004).
- Thời gian phản hồi API backend đọc/ghi P95: $< 500\text{ms}$.

**Constraints**: 
- Đúng 1 active list cho mỗi người dùng (FR-001).
- Trần tối đa 200 items/list (enforce ở Service backend và pre-check ở client).
- Không mất dữ liệu mutation khi offline dù Access/Refresh Token hết hạn.
- Mọi thao tác đều kiểm tra quyền sở hữu `user_id = principal`, ngăn chặn IDOR (Insecure Direct Object Reference).

**Scale/Scope**: Quản lý danh mục 6 quầy siêu thị, ~10.000 công thức tham chiếu, 1 màn hình checklist chính + 1 modal pre-import + 1 drawer lịch sử.

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc Hiến pháp | Đánh giá | Phương án giải quyết & Ghi chú |
|---|:---:|---|
| **I. Spec trước, code sau** | ⚠️ Có điều kiện | Bản đặc tả `specs/040-shopping-list/spec.md` đang ở trạng thái `Draft (Ready for Review)`. Bản plan này được thiết lập làm cơ sở thiết kế kỹ thuật. **Tuyệt đối không viết code hiện thực** trước khi spec được nghiệm thu chính thức và branch được gán task Jira chuẩn. Không tự chế mã Jira `SCRUM-xx`, không tự ý sửa spec hay constitution. |
| **II. Test-First (Không thương lượng)** | ✅ PASS | Mọi acceptance scenario và Functional Requirement (FR-001 đến FR-025) đều được ánh xạ vào ma trận kiểm thử tại Mục 7. Tầng Service cam kết JaCoCo $\ge 70\%$. Kiểm thử tích hợp dùng container PostgreSQL 16 thật qua Testcontainers, không dùng H2. Các kịch bản không automate được trong CI (Airplane Mode thật, đo trễ $<50\text{ms}$) được chuyển giao thành Manual Test Cases trong `docs/test/`. |
| **III. Kiến trúc phân lớp đơn giản** | ✅ PASS | Tuân thủ luồng: Controller $\to$ Service $\to$ Repository $\to$ Database. DTO Record 100%, không rò rỉ Entity. Module tổ chức theo feature `com.wikicook.shoppinglist`. Giao tiếp với Meal Planner (030/031), Recipe (060), Profile (011) qua Service Facade/Ports (đặc tả tại `contracts/integration.md`), không join bảng cross-module trong repository. |
| **IV. Bảo mật & dữ liệu người dùng** | ✅ PASS | Spring Security bảo vệ mọi endpoint `/api/v1/shopping-lists/**`. Định danh `user_id` lấy từ `SecurityContext`, URL path `{id}` bắt buộc kiểm tra quyền sở hữu (`user_id = principal`), trả 403 Forbidden nếu vi phạm. Bean Validation (`@Valid`) chặt chẽ phía server. Lỗi trả về dạng RFC 7807 `ProblemDetail`, không lộ stack trace. Cơ chế offline giữ hàng đợi mutation an toàn khi token hết hạn. |
| **V. Trải nghiệm nhất quán** | ✅ PASS | UI responsive từ 360 px, chuỗi tiếng Việt tập trung. Tái sử dụng component Button, Toast, Modal của hệ thống. **Quyết định phân trang**: Danh sách active tối đa 200 items nên không phân trang để phục vụ checklist đi siêu thị mượt mà liên tục (phù hợp mục đích danh sách cá nhân của Constitution V); Danh sách lịch sử `ARCHIVED` được phân trang (`page`, `size`) theo chuẩn. |
| **Ràng buộc kỹ thuật** | ✅ PASS | Java 21, Spring Boot 3, Flyway migration (`V401__shopping_lists.sql`), React 18 Vite, RESTful JSON `/api/v1`. |

**Kết quả Gate Phase 0**: **PASS (Đủ điều kiện nghiên cứu & thiết kế)**.  
**Re-check sau Phase 1**: **PASS** – Lược đồ quan hệ khớp 100% full-schema 4.8; contracts API khớp đầy đủ 14 endpoint; không vi phạm ranh giới module.

---

## Project Structure

### Documentation (this feature)

```text
specs/040-shopping-list/
├── plan.md              # Kế hoạch triển khai tổng thể (file này)
├── research.md          # Phase 0: Phân tích quyết định kỹ thuật R1 - R7
├── data-model.md        # Phase 1: Lược đồ CSDL, JPA Entities, Record DTOs, IndexedDB
├── quickstart.md        # Phase 1: Hướng dẫn chạy local & cURL testing
├── checklists/
│   └── requirements.md  # Danh mục kiểm tra yêu cầu
└── contracts/           # Phase 1: Giao ước API và giao tiếp liên module
    ├── openapi.yaml     # REST API contracts chuẩn OpenAPI 3.0.3
    └── integration.md   # Service Facade / Ports giao tiếp với 011, 020, 030, 031, 060
```

### Source Code (repository root)

```text
src/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/wikicook/shoppinglist/
│       │   │   ├── controller/
│       │   │   │   ├── ShoppingListController.java
│       │   │   │   ├── ShoppingListSyncController.java
│       │   │   │   └── ShoppingListExceptionHandler.java
│       │   │   ├── service/
│       │   │   │   ├── ShoppingListService.java
│       │   │   │   ├── IngredientAggregationService.java
│       │   │   │   ├── UnitConversionService.java
│       │   │   │   └── ShoppingListSyncService.java
│       │   │   ├── repository/
│       │   │   │   ├── ShoppingListRepository.java
│       │   │   │   ├── ShoppingListItemRepository.java
│       │   │   │   ├── ShoppingListItemSourceRepository.java
│       │   │   │   └── UserAislePreferenceRepository.java
│       │   │   ├── domain/
│       │   │   │   ├── ShoppingList.java
│       │   │   │   ├── ShoppingListItem.java
│       │   │   │   ├── ShoppingListItemSource.java
│       │   │   │   ├── UserAislePreference.java
│       │   │   │   └── ShoppingListStatus.java
│       │   │   ├── dto/
│       │   │   │   ├── ShoppingListResponse.java
│       │   │   │   ├── AisleGroupDto.java
│       │   │   │   ├── ShoppingListItemDto.java
│       │   │   │   ├── ItemProvenanceDto.java
│       │   │   │   ├── CreateCustomItemRequest.java
│       │   │   │   ├── UpdateItemRequest.java
│       │   │   │   ├── ImportRecipeRequest.java
│       │   │   │   ├── ImportMealPlanRequest.java
│       │   │   │   ├── SyncBatchRequest.java
│       │   │   │   └── SyncBatchResponse.java
│       │   │   └── port/
│       │   │       ├── RecipeDetailPort.java
│       │   │       ├── MealPlanHandoffPort.java
│       │   │       └── DietaryProfilePort.java
│       │   └── resources/
│       │       └── db/migration/
│       │           └── V401__shopping_lists.sql
│       └── test/java/com/wikicook/shoppinglist/
│           ├── service/
│           │   ├── IngredientAggregationServiceTest.java
│           │   ├── UnitConversionServiceTest.java
│           │   └── ShoppingListSyncServiceTest.java
│           └── controller/
│               ├── ShoppingListControllerIT.java
│               └── ShoppingListRepositoryIT.java
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

Tuân thủ Hiến pháp **Principle III** (YAGNI & Kiến trúc tối giản), mọi thư viện hoặc mẫu thiết kế bổ sung đều phải được giải trình lý do và so sánh với phương án đơn giản hơn:

| Thành phần bổ sung | Lý do sử dụng (Rationale) | Phương án đơn giản hơn đã loại bỏ & Tại sao |
|---|---|---|
| **Thư viện `idb` (v8.x) ở frontend** | Wrapper nhỏ gọn (~1.1 KB gzipped) viết bằng Promise cho API IndexedDB nguyên bản của trình duyệt. Cần thiết để xử lý đọc/ghi transaction bất đồng bộ không gây lag UI khi đi siêu thị (SC-002). | Dùng API `indexedDB` thuần: code callback cồng kềnh, dễ lỗi transaction leak; Dùng `localStorage`: đồng bộ (blocking main thread khi cuộn trang) và giới hạn 5MB không an toàn; Dùng Dexie.js: tính năng quá đồ sộ (~25 KB) vượt quá nhu cầu (vi phạm YAGNI). |
| **Cấu trúc Service Facade / Port** | Cho phép module 040 đọc dữ liệu từ module 060 (công thức), 030/031 (kế hoạch tuần), và 011 (hồ sơ ăn uống) mà không bị phụ thuộc vòng (circular dependency) và không join SQL cross-module. | Join bảng SQL trực tiếp trong Repository: Làm hỏng ranh giới module, khiến các thành viên merge code dễ bị xung đột Flyway và khó test cô lập; Dùng Microservices/gRPC: Quá tải và không phù hợp với đồ án môn học. |
| **Quy đổi đơn vị bằng Java Code thay vì Bảng CSDL** | Đơn vị quy đổi trong siêu thị chỉ gói gọn trong hệ metric chuẩn ($g \leftrightarrow kg, ml \leftrightarrow l$). Viết logic trong `UnitConversionService` giúp chạy cực nhanh trong bộ nhớ, không cần query DB mỗi lần tính toán. | Tạo bảng `unit_conversions` và bảng `units` phức tạp: Tốn thêm 2 bảng CSDL và query join chậm, đã được nhóm thống nhất loại bỏ trong `full-schema.md` mục 5 dòng 8. |
| **Canonical Server Engine vs Client Mirror** | Server luôn là chân lý tối thượng (Single Source of Truth) trong tính toán gộp và phân quầy. Client duy trì một bản mirror logic rút gọn để phục vụ xem trước tức thì và tính toán offline khi mất mạng. | Chỉ tính ở server: Khi offline trong tầng hầm người dùng không thể xem số lượng gộp; Chỉ tính ở client: Dữ liệu dễ bị gian lận hoặc sai lệch giữa các nền tảng thiết bị. |

---

## Traceability & Verification Matrix

Bảng ánh xạ toàn diện từ **Yêu cầu chức năng (FR)** và **Tiêu chí thành công (SC)** sang phương án kiểm thử cụ thể:

| Mã Yêu Cầu | Tóm tắt Nghiệp vụ | Phương án Hiện thực & Vị trí Mã nguồn | Loại Kiểm thử & Tên Test Case |
| :---: | :--- | :--- | :--- |
| **FR-001** | Đúng 1 active list/user | Partial Unique Index `uq_shopping_lists_active_user` + `ShoppingListService.getActiveList()` | `Integration`: `ShoppingListRepositoryIT.testOnlyOneActiveListPerUser()` |
| **FR-002** | Nút import từ Recipe Detail kèm serving multiplier | `POST /import/recipe` + `RecipeDetailPort` + `PreImportBottomSheet` | `Integration`: `ShoppingListControllerIT.testImportRecipeWithServings()`<br>`Frontend`: `PreImportBottomSheet.test.tsx` |
| **FR-003** | Công thức scale khẩu phần & Ceiling đơn vị đếm | `IngredientAggregationService.scaleAndCeil()` | `Unit`: `IngredientAggregationServiceTest.testDiscreteUnitCeilingRule()` (0.5 trứng $\to$ 1) |
| **FR-004** | Import từ Meal Plan tuần (MERGE / ARCHIVE) | `POST /import/meal-plan` + `MealPlanHandoffPort` | `Integration`: `ShoppingListControllerIT.testImportMealPlanMergeAndArchive()` |
| **FR-005** | Deduplication theo `ingredient_id` | Gom nhóm Map key `ingredient_id` trong `IngredientAggregationService` | `Unit`: `IngredientAggregationServiceTest.testDeduplicationByIngredientId()` |
| **FR-006** | Gộp số lượng metric tương thích ($g/kg, ml/l$) | `UnitConversionService.aggregateMetric()` | `Unit`: `UnitConversionServiceTest.testAggregateGramsAndKilograms()` (500g + 1kg = 1.5kg) |
| **FR-007** | Quy đổi metric 2 chiều ($<1kg \to g, \ge 1000g \to kg$) | `UnitConversionService.normalizeAndFormat()` | `Unit`: `UnitConversionServiceTest.testBidirectionalConversionThresholds()` |
| **FR-008** | Giữ sub-row cho đơn vị định tính ("thìa", "tép") | `IngredientAggregationService.handleIncompatibleUnits()` | `Unit`: `IngredientAggregationServiceTest.testQualitativeUnitsIsolated()` |
| **FR-009** | Lưu snapshot nguồn gốc (Provenance) | Bảng `shopping_list_item_sources` | `Integration`: `ShoppingListControllerIT.testProvenanceSnapshotPersistence()` |
| **FR-010** | Tên món tự nhập (Custom item) | `POST /items` + check `num_nonnulls` | `Integration`: `ShoppingListControllerIT.testAddCustomItemGeneralAisle()` |
| **FR-011** | Phân loại 6 quầy hàng siêu thị chuẩn | Enum / Table `aisle_categories` | `Unit`: `IngredientAggregationServiceTest.testAisleCategorizationDefault()` |
| **FR-012** | Đổi quầy hàng thủ công cho mặt hàng | `PATCH /items/{uuid}` với `aisleCategoryId` | `Integration`: `ShoppingListControllerIT.testChangeItemAisle()` |
| **FR-013** | Ghi nhớ quầy tùy biến (`user_aisle_preferences`) | `user_aisle_preferences` Upsert khi `rememberAisle=true` | `Integration`: `UserAislePreferenceRepositoryIT.testUpsertPreference()` |
| **FR-014** | Thao tác tick/gạch bỏ món trong giỏ hàng | `PATCH /items/{uuid}` + Optimistic UI | `Frontend`: `ChecklistScreen.test.tsx` (kiểm tra class `line-through`) |
| **FR-015** | Tự động chuyển xuống nhóm "Đã mua" & thanh tiến độ | React State Filter / Grouping | `Frontend`: `ChecklistScreen.test.tsx` (kiểm tra progress bar $X/Y$ items) |
| **FR-016** | Gộp thêm vào món đã checked thì tự uncheck | `ShoppingListService.mergeItem()` kiểm tra `isChecked` | `Unit`: `ShoppingListServiceTest.testMergeIntoCheckedItemResetsToUnchecked()` |
| **FR-017** | Dọn dẹp mềm non-destructive (`is_cleared = true`) | `POST /active/clear-purchased` | `Integration`: `ShoppingListControllerIT.testSoftClearPurchasedItems()` |
| **FR-018** | Hoàn tác dọn dẹp trong 5 giây | `POST /active/undo-clear` với `clearBatchId` | `Integration`: `ShoppingListControllerIT.testUndoClearPurchasedItems()` |
| **FR-019** | Lưu trữ danh sách vào lịch sử & tạo list trắng | `POST /active/archive` (trong cùng 1 transaction) | `Integration`: `ShoppingListControllerIT.testArchiveAndCreateFreshActive()` |
| **FR-020** | Ngoại tuyến không bị chặn bởi JWT | IndexedDB Cache + non-blocking auth banner | `Manual`: `TC-MAN-040-01` (xem dưới) |
| **FR-021** | Đồng bộ lô ngoại tuyến với `client_uuid` idempotent | `POST /active/sync` + `ShoppingListSyncService` | `Integration`: `ShoppingListSyncServiceIT.testBatchSyncIdempotency()` |
| **FR-022** | Giải quyết xung đột ưu tiên `is_checked = true` | Logic OR: `current.isChecked \|\| mutation.isChecked` | `Unit`: `ShoppingListSyncServiceTest.testConflictResolutionCheckedWins()` |
| **FR-023** | Sao chép Plaintext 1-chạm vào Clipboard | `navigator.clipboard.writeText()` định dạng nhóm quầy | `Frontend`: `useShoppingList.test.ts` (kiểm tra chuỗi plain text sinh ra) |
| **FR-024** | Định dạng in ấn 2 cột tiết kiệm mực | CSS `@media print` trong `print-checklist.css` | `Manual`: `TC-MAN-040-03` |
| **FR-025** | Khống chế trần tối đa 200 items/list | `ShoppingListService.checkCapacityLimit()` | `Integration`: `ShoppingListControllerIT.testCapacityLimitExceeded422()` |

---

## Manual Test Cases (Dành cho Kịch bản Thực địa)

Các kịch bản này được đăng ký vào `docs/test/test-cases.md` để nghiệm thu thủ công trong giai đoạn tasks:

### TC-MAN-040-01: Kiểm thử Ngoại tuyến Thực địa (In-Store Airplane Mode Test)
- **Mục tiêu**: Xác thực khả năng hoạt động liên tục khi mất sóng trong siêu thị và đồng bộ không mất dữ liệu trong $\le 3$ giây khi có sóng lại (SC-003, FR-020, FR-021).
- **Thiết bị**: Điện thoại thông minh chạy trình duyệt di động (Chrome/Safari).
- **Các bước thực hiện**:
  1. Đăng nhập vào WikiCook, mở trang `/shopping-list` với danh sách có sẵn 10 món.
  2. Bật **Chế độ máy bay (Airplane Mode)** trên điện thoại để ngắt hoàn toàn Wi-Fi và 4G/5G.
  3. Tick chọn 4 món vào giỏ hàng; thêm 1 món tùy chọn "Bánh mì sandwich, 1 ổ"; sửa số lượng 1 món từ 500g lên 1kg.
  4. Xác nhận giao diện phản hồi ngay lập tức, không xuất hiện popup lỗi mạng chặn màn hình.
  5. Tắt Chế độ máy bay để khôi phục kết nối Internet.
- **Kết quả mong đợi**:
  - Trong vòng $\le 3.0$ giây, biểu tượng trạng thái chuyển từ "Đang lưu ngoại tuyến" sang "Đã đồng bộ máy chủ".
  - F5 tải lại trang trên máy tính khác đăng nhập cùng tài khoản: Cả 4 món đã tick, món mới thêm và số lượng cập nhật hiển thị chính xác 100%.

### TC-MAN-040-02: Đo độ trễ phản hồi Checklist 1 tay trên Mobile
- **Mục tiêu**: Kiểm tra tiêu chí SC-002: Thao tác tick chọn và gạch bỏ phản hồi $< 50\text{ms}$ trên thiết bị tầm trung.
- **Thiết bị**: Thiết bị Android tầm trung (VD: chip Snapdragon 680 hoặc tương đương).
- **Các bước thực hiện**:
  1. Mở danh sách có 50 items trên mobile.
  2. Sử dụng 1 tay, liên tục bấm tick chọn 5 món ngẫu nhiên.
- **Kết quả mong đợi**:
  - Checkbox đổi màu, chữ chuyển gạch ngang và thanh tiến độ dịch chuyển tức thì, không có hiện tượng khựng/lag (Optimistic UI không chờ network roundtrip).

### TC-MAN-040-03: Kiểm tra Giao diện In ấn Tiết kiệm mực (@media print)
- **Mục tiêu**: Kiểm tra tiêu chí in ấn (FR-024).
- **Các bước thực hiện**:
  1. Mở danh sách mua sắm trên trình duyệt Desktop.
  2. Bấm phím tắt `Ctrl + P` (hoặc `Cmd + P` trên macOS).
- **Kết quả mong đợi**:
  - Toàn bộ thanh header, navigation, nút bấm floating, footer bị ẩn hoàn toàn.
  - Nội dung dàn thành 2 cột chữ đen nền trắng rõ nét, có ô vuông `[ ]` sẵn sàng cho việc in ra giấy.
