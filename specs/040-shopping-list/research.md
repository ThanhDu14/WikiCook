# Research: Automated Smart Shopping List & Ingredient Aggregation – 040

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Ngày**: 2026-10-24

Tài liệu này ghi lại toàn bộ các quyết định nghiên cứu kỹ thuật (Phase 0 Research) cho module **040-shopping-list**, tuân thủ cấu trúc **Decision / Rationale / Alternatives considered**. Toàn bộ các câu hỏi kỹ thuật (NEEDS CLARIFICATION) được giải quyết triệt để trước khi chuyển sang Phase 1 (Data Model & Contracts).

---

## R1. Thuật toán gộp nguyên liệu, quy đổi đơn vị và lưu trữ số lượng

- **Decision**:
  1. **Quy đổi Metric hai chiều trong Java (Backend)**:
     - Khối lượng: $1000\text{ g} \leftrightarrow 1\text{ kg}$. Giá trị $\ge 1000\text{ g} \to \text{kg}$; giá trị $< 1\text{ kg} \to \text{g}$.
     - Thể tích: $1000\text{ ml} \leftrightarrow 1\text{ l}$. Giá trị $\ge 1000\text{ ml} \to \text{l}$; giá trị $< 1\text{ l} \to \text{ml}$.
     - Độ chính xác số học: Sử dụng `BigDecimal` với phép làm tròn `HALF_UP` tối đa 2 chữ số thập phân (`scale = 2`) khi xuất ra DTO hiển thị.
  2. **Quy tắc làm tròn đơn vị rời rạc / đếm được (Discrete Units)**:
     - Đối với các đơn vị đếm thực tế (quả, củ, bắp, lon, gói, quả trứng, miếng, lát): Giá trị sau khi nhân hệ số khẩu phần nếu có phần lẻ sẽ được **làm tròn lên trần (`Ceiling`)** ở số lượng hiển thị mua sắm chính (`display_quantity`), ví dụ: 0.5 quả trứng $\to$ 1 quả, 1.25 bắp cải $\to$ 2 bắp. Điều này đảm bảo người nội trợ không bị thiếu nguyên liệu khi đi chợ.
     - Số lượng thập phân chính xác vẫn được bảo toàn nguyên vẹn trong chi tiết nguồn gốc nguyên liệu (`shopping_list_item_sources.contributed_quantity`).
  3. **Đơn vị định tính không quy đổi**:
     - Các đơn vị thể tích ẩm thực truyền thống ("thìa canh", "thìa cà phê") hoặc đơn vị ước lượng ("nhánh", "tép", "vừa đủ") không có khối lượng riêng cố định sẽ **không bao giờ gộp ép vào gram hay ml**. Chúng được giữ thành các dòng con riêng biệt (sub-rows) hoặc dòng độc lập cùng tên nguyên liệu.
  4. **Serving Multiplier (Hệ số khẩu phần)**:
     - Hệ số nhân: $\text{Multiplier} = S_{\text{target}} / S_{\text{base}}$. $S_{\text{target}}$ bắt buộc là bội số dương của 0.5. Nếu công thức nguồn không khai báo $S_{\text{base}}$, hệ thống mặc định $\text{Multiplier} = 1.0\times$ và trả về cờ cảnh báo `is_default_servings = true` trong DTO.
  5. **Quyết định lưu trữ CSDL (Database Schema)**:
     - Bảng `shopping_list_items` chỉ có một cột `quantity NUMERIC(10,3)` và `unit_id BIGINT`:
       * Cột `quantity` **luôn lưu giá trị đã chuẩn hóa (normalized)** về đơn vị cơ sở nhỏ nhất: Khối lượng lưu theo Gram (`g`), Thể tích lưu theo Milliliter (`ml`), Đơn vị đếm lưu theo số lượng thực tế. `unit_id` trỏ vào id của đơn vị cơ sở chuẩn hóa đó.
       * DTO trả ra client sẽ tự động suy luận `display_quantity` và `display_unit` (kg, l) tương ứng.
       * Bảng `shopping_list_item_sources` lưu snapshot `contributed_quantity` và `unit_id` gốc để phục vụ truy vết nguồn gốc (provenance modal).
- **Rationale**: Tuân thủ FR-003, FR-005, FR-006, FR-007, FR-008, Q1, Q9. Khớp hoàn toàn với schema của `docs/analysis-and-design/database/full-schema.md` Mục 4.8 (chỉ có 1 cột `quantity NUMERIC(10,3)`).
- **Alternatives considered**:
  - Lưu cả 2 cột `display_quantity` và `normalized_quantity` trong DB: Bị loại vì vi phạm schema đã được phê duyệt tại Mục 4.8 của `full-schema.md` và làm tăng nguy cơ bất đồng bộ dữ liệu.
  - Quy đổi "thìa canh", "thìa cà phê" sang ml theo chuẩn quốc tế (15 ml, 5 ml): Bị loại vì mật độ gia vị Việt Nam chênh lệch lớn (nước mắm nặng hơn dầu ăn, muối nặng hơn bột ngọt) gây sai lệch khi nấu thực tế (Q1).

---

## R2. Cơ chế Offline-First, Local Storage và Giao thức đồng bộ (Sync Protocol)

- **Decision**:
  1. **Lưu trữ phía Client (Local-First)**:
     - Sử dụng `IndexedDB` thông qua thư viện nhẹ [`idb`](https://github.com/jakearchibald/idb) (wrapper Promise chuẩn ~1KB của Jake Archibald) cho SPA React.
     - Client duy trì 2 Object Store:
       * `active_list`: Chứa toàn bộ dữ liệu danh sách hiện hành (items, quầy, trạng thái checked/cleared).
       * `mutation_queue`: Hàng đợi lưu các hành động offline theo thứ tự thời gian (`{ clientUuid, action: 'TOGGLE_CHECK' | 'UPDATE_QTY' | 'ADD_CUSTOM' | 'DELETE' | 'SOFT_CLEAR', payload, clientTimestamp }`).
  2. **Định danh `client_uuid`**:
     - Mọi item tạo mới tại client (khi offline lẫn online) đều được sinh sẵn một mã `UUIDv4` (`client_uuid`). Bảng `shopping_list_items` có ràng buộc `UNIQUE (client_uuid)` (full-schema 4.8).
  3. **Batch Sync Idempotency**:
     - Khi thiết bị khôi phục kết nối mạng, client gửi toàn bộ `mutation_queue` lên endpoint `POST /api/v1/shopping-lists/active/sync`.
     - Phía server xử lý theo cơ chế **Idempotent**: Nếu `client_uuid` của item thêm mới đã tồn tại, server bỏ qua lệnh tạo; với các lệnh cập nhật, server so sánh `version` và `clientTimestamp`.
  4. **Thuật toán giải quyết xung đột (Conflict Resolution)**:
     - Áp dụng **Last-Write-Wins** theo từng dòng dữ liệu dựa trên cột `version` tăng dần.
     - **Ưu tiên TUYỆT ĐỐI trạng thái đã mua (`is_checked = true`)**: Nếu một thiết bị đã check món thành công (`is_checked = true`), các thao tác uncheck đến sau có độ trễ do mất sóng sẽ không được phép ghi đè trạng thái uncheck (sử dụng toán tử logic OR: `is_checked_final = current.is_checked || mutation.is_checked`). Điều này ngăn ngừa người dùng mua thừa hoặc bỏ sót món đồ đã nằm trong giỏ.
  5. **Xử lý hết hạn Token khi đang ngoại tuyến**:
     - Khi ở tầng hầm siêu thị, nếu Access Token (JWT 15 phút) hết hạn, client vẫn cho phép tick chọn, sửa số lượng, thêm món cục bộ vào IndexedDB mà không bị đá văng ra màn hình đăng nhập (FR-020).
     - Khi có mạng trở lại, client tự động kích hoạt `POST /api/v1/auth/refresh`.
     - **Nếu Refresh Token cũng hết hạn**: Client **KHÔNG** xóa IndexedDB! Ứng dụng hiển thị một banner thông báo không chặn (non-blocking banner): *"Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại để đồng bộ giỏ hàng lên máy chủ"*. Hàng đợi mutation vẫn được giữ nguyên an toàn, sau khi người dùng đăng nhập lại thành công sẽ tự động flush lên server.
  6. **Quy tắc Đăng xuất (Logout)**:
     - Khi người dùng chủ động bấm Đăng xuất, hệ thống kiểm tra nếu `mutation_queue` còn dữ liệu chưa đồng bộ sẽ hiển thị modal cảnh báo: *"Bạn còn [N] thay đổi chưa được đồng bộ lên máy chủ. Đăng xuất lúc này sẽ hủy bỏ các thay đổi này"*. Người dùng xác nhận thì mới xóa sạch session và IndexedDB.
- **Rationale**: Đáp ứng FR-020, FR-021, FR-022, SC-002 (<50ms), SC-003 (sync <3s), Q5, Q6. Khớp hoàn toàn với Constitution IV (bảo vệ dữ liệu người dùng).
- **Alternatives considered**:
  - Dùng `localStorage`: Bị loại vì dung lượng giới hạn 5MB, tính đồng bộ (blocking I/O làm giật khung hình khi cuộn danh sách lớn), và không hỗ trợ transaction an toàn.
  - Dùng WebSockets để đồng bộ thời gian thực: Bị loại vì phạm vi MVP chỉ là danh sách cá nhân của 1 user, WebSockets gây tiêu tốn pin trên thiết bị di động và spec đã xác định out-of-scope (mục Out of Scope).

---

## R3. Pre-Import Modal, Lọc Gia vị cơ bản (Staple Filtering) và Chế độ Meal Plan

- **Decision**:
  1. **Pre-Import Checklist Modal**:
     - Khi người dùng bấm "Thêm vào danh sách mua sắm" từ Recipe Detail (020) hoặc Meal Planner (030/031), client **luôn hiển thị Bottom Sheet / Modal tiền nhập liệu**.
     - Modal hiển thị danh sách nguyên liệu đã được tính toán hệ số khẩu phần, mặc định tích chọn 100%. Người dùng có thể bỏ tick từng món.
  2. **Tiện ích 1-chạm "Bỏ chọn gia vị cơ bản" (Staple Filter)**:
     - Danh sách gia vị cơ bản mặc định của bếp Việt: *Muối, Đường, Dầu ăn, Nước mắm, Hạt tiêu, Bột ngọt / Hạt nêm*.
     - **Cơ chế phân giải**: Hệ thống phân giải mã `ingredient_id` của 6 gia vị này thông qua danh mục nguyên liệu của module 060 (`IngredientSummaryPort`). Nếu catalogue chưa có hoặc tên bị lệch, fallback theo danh sách cấu hình tên chuẩn trong file cấu hình ứng dụng (`wikicook.staples.names=muối,đường,dầu ăn,nước mắm,tiêu,bột ngọt,hạt nêm`).
  3. **Body API Import**:
     - `POST /api/v1/shopping-lists/import/recipe`: nhận `recipeId`, `targetServings`, và danh sách `excludedIngredientIds` (danh sách ID các món người dùng đã bỏ chọn ở modal).
     - Hỗ trợ header `Idempotency-Key` (UUID) do client sinh ra để chống bấm liên tiếp (double click trong 2 giây).
  4. **Chế độ Import từ Weekly Meal Plan (030/031)**:
     - `POST /api/v1/shopping-lists/import/meal-plan`: nhận `mealPlanId`, `mode` (`MERGE` hoặc `ARCHIVE_AND_NEW`), `servingOverrides[]`, và `excludedIngredientIds`.
     - Nếu user chưa có active shopping list, hệ thống tự động tạo mới mà không cần modal quyết định chế độ.
     - **Quy tắc FR-016 áp dụng cho cả Import**: Nếu một nguyên liệu nhập vào trùng với một món đã có trong danh sách nhưng món đó đang có trạng thái `is_checked = true`, hệ thống tự động gộp thêm số lượng mới và chuyển trạng thái về `is_checked = false` kèm thông báo cho người dùng biết món đó cần mua thêm.
- **Rationale**: Đáp ứng FR-002, FR-004, FR-016, Q3, Q4, Q10.
- **Alternatives considered**:
  - Tự động gộp thẳng vào list không cần modal xác nhận: Bị loại vì người dùng thường đã có sẵn gia vị cơ bản ở nhà, việc gộp tự động gây rác danh sách và khó chịu.

---

## R4. Giới hạn tải 200 Items, Cơ chế Soft-Clear và Hoàn tác (Undo)

- **Decision**:
  1. **Enforce Giới hạn 200 items/list**:
     - Phía Client: Kiểm tra tổng số lượng item hiện có + số lượng item dự kiến thêm mới. Nếu $> 200$, vô hiệu hóa nút Import và hiển thị cảnh báo yêu cầu lưu trữ list cũ hoặc bỏ bớt món.
     - Phía Backend Service: Kiểm tra `COUNT(*) FROM shopping_list_items WHERE list_id = :listId AND is_cleared = false`. Nếu vượt quá 200, ném ngoại lệ `ListCapacityExceededException` $\to$ HTTP 422 `LIST_CAPACITY_EXCEEDED`.
  2. **Cơ chế Soft-Clear ("Xóa món đã mua")**:
     - Thao tác "Dọn dẹp món đã mua" là **Non-destructive soft-clear**: `UPDATE shopping_list_items SET is_cleared = true, cleared_at = NOW(), version = version + 1 WHERE list_id = :listId AND is_checked = true AND is_cleared = false`.
     - Endpoint `POST /api/v1/shopping-lists/active/clear-purchased` trả về kết quả kèm mã đợt dọn dẹp `clearBatchId` (hoặc timestamp `clearedAt`).
     - Giao diện hiển thị Snackbar thông báo: *"Đã dọn dẹp [N] món đã mua"* kèm nút **"Hoàn tác"** trong 5 giây.
  3. **Cơ chế Hoàn tác (Undo)**:
     - Khi bấm Hoàn tác trong 5 giây, client gọi `POST /api/v1/shopping-lists/active/undo-clear` với `clearBatchId`: Server khôi phục đúng các món vừa dọn (`SET is_cleared = false, cleared_at = NULL`).
     - Với các thao tác khôi phục từng món đơn lẻ từ khu vực "Xem món đã dọn dẹp" ở chân trang, client sử dụng `PATCH /api/v1/shopping-lists/items/{clientUuid}` với payload `{ "isCleared": false }`.
  4. **Vòng đời Archive & Restore**:
     - `POST /api/v1/shopping-lists/active/archive`: Chuyển list hiện tại sang `ARCHIVED` (`archived_at = NOW()`), bảo toàn toàn bộ món đã mua và món soft-cleared, đồng thời **khởi tạo ngay một active shopping list trắng mới** trong cùng một database transaction.
     - `POST /api/v1/shopping-lists/{id}/restore`: Chạy trong một transaction duy nhất có khóa cấp: (1) Archive active list hiện tại; (2) Chuyển list `{id}` thành `ACTIVE`, reset toàn bộ `is_checked = false`, `is_cleared = false`, giữ nguyên tên món, số lượng, quầy. Thỏa mãn tuyệt đối ràng buộc `UNIQUE (user_id) WHERE status = 'ACTIVE'`.
- **Rationale**: Đáp ứng FR-001, FR-017, FR-018, FR-019, FR-025, Q11, Q13. Bảo đảm tính toàn vẹn CSDL và trải nghiệm người dùng không bị giật lag.
- **Alternatives considered**:
  - Hard delete (xóa vĩnh viễn khỏi CSDL khi dọn dẹp): Bị loại vì người dùng không thể xem lại lịch sử hóa đơn sau khi đi chợ xong và không thể hoàn tác nếu bấm nhầm.

---

## R5. Phân nhóm quầy siêu thị (Aisle Categorization) & Tùy biến người dùng

- **Decision**:
  1. **6 Quầy hàng tiêu chuẩn (Fixed Taxonomy)**:
     - Dựa trên bảng `aisle_categories` (full-schema Mục 3.1 & 4.8):
       * `PRODUCE` (Rau củ & Trái cây) - display_order: 1
       * `MEAT_SEAFOOD` (Thịt & Hải sản) - display_order: 2
       * `DAIRY_EGGS` (Trứng & Sữa) - display_order: 3
       * `PANTRY_SPICES` (Gia vị & Đồ khô) - display_order: 4
       * `FROZEN` (Thực phẩm đông lạnh) - display_order: 5
       * `GENERAL` (Đồ dùng khác / Bách hóa tổng hợp) - display_order: 6
  2. **Quy tắc phân quầy tự động**:
     - Khi import từ công thức: Lấy `aisle_category_id` từ bảng `ingredients` (do module 060 quản lý).
     - Khi thêm món thủ công (custom item): Nếu không khớp danh mục nguyên liệu hệ thống, mặc định phân vào quầy `GENERAL`.
  3. **Ghi nhớ quầy tùy biến (`user_aisle_preferences`)**:
     - Khi người dùng đổi quầy của một món trên giao diện:
       * Client gọi `PATCH /api/v1/shopping-lists/items/{clientUuid}` kèm cờ `rememberAisle: true/false`.
       * Nếu `rememberAisle = true` và món đó liên kết với `ingredient_id` hợp lệ: Server tự động thực hiện Upsert vào bảng `user_aisle_preferences (user_id, ingredient_id, aisle_category_id)`.
       * Trong các lần import tiếp theo của người dùng này, quầy tùy biến cá nhân sẽ luôn ghi đè lên quầy mặc định của hệ thống.
- **Rationale**: Đáp ứng FR-011, FR-012, FR-013, Q12.
- **Alternatives considered**:
  - Cho phép người dùng tự tạo tên quầy tùy ý: Bị loại vì làm vỡ cấu trúc layout và gây khó khăn khi hiển thị icon chuẩn trên mobile.

---

## R6. Kiến trúc phân lớp, Package Structure và Ranh giới Module

- **Decision**:
  1. **Backend Package Structure (Package by Feature)**:
     - Gói mã nguồn: `com.wikicook.shoppinglist`
       * `controller/`: `ShoppingListController`, `ShoppingListSyncController`, `ShoppingListExceptionHandler`
       * `service/`: `ShoppingListService`, `IngredientAggregationService`, `UnitConversionService`, `ShoppingListSyncService`
       * `repository/`: `ShoppingListRepository`, `ShoppingListItemRepository`, `ShoppingListItemSourceRepository`, `UserAislePreferenceRepository`
       * `domain/`: `ShoppingList`, `ShoppingListItem`, `ShoppingListItemSource`, `UserAislePreference`, `ShoppingListStatus` (Enum)
       * `dto/`: Record DTOs (`ShoppingListResponse`, `AisleGroupDto`, `ShoppingListItemDto`, `ItemSourceDto`, `SyncBatchRequest`, `SyncBatchResponse`, v.v.)
       * `port/`: Các Interface Facade giao tiếp ra bên ngoài:
         - `RecipeDetailPort`: Đọc công thức, khẩu phần base, danh sách nguyên liệu và quầy mặc định từ module 060.
         - `MealPlanPort`: Đọc danh sách món ăn và khẩu phần override theo tuần từ module 030/031.
         - `DietaryProfilePort`: Đọc danh sách dị ứng/kiêng khem từ module 011 (phục vụ hiển thị cảnh báo nếu cần).
  2. **Tuân thủ Hiến pháp (Constitution Principles)**:
     - Principle III (Kiến trúc phân lớp): Controller chỉ gọi Service, Service chỉ gọi Repository hoặc Port. Mọi giao tiếp ra ngoài module đều qua interface DTO, không join bảng chéo module.
     - Principle IV (Bảo mật): Lấy `userId` từ `SecurityContext` (`Authentication.getPrincipal()`), tuyệt đối không nhận `userId` từ Client Request Body hay URL path.
- **Rationale**: Đáp ứng Principle III, IV và kiến trúc Layered Client-Server trong `architecture.md`.
- **Alternatives considered**:
  - Kiến trúc Hexagonal thuần túy với Ports & Adapters phức tạp: Được rút gọn thành Service Facade/Ports đơn giản phù hợp với quy mô dự án sinh viên (YAGNI).

---

## R7. Chiến lược kiểm thử & Đảm bảo chất lượng (Test Strategy)

- **Decision**:
  1. **Backend Test Automation**:
     - Unit Tests: `IngredientAggregationServiceTest`, `UnitConversionServiceTest`, `ShoppingListSyncServiceTest` (JUnit 5 + Mockito). Đo độ phủ bằng JaCoCo, cam kết line coverage $\ge 70\%$, branch coverage $\ge 70\%$.
     - Integration Tests: `ShoppingListControllerIT`, `ShoppingListRepositoryIT` dùng `@SpringBootTest` / `@DataJpaTest` kết hợp **Testcontainers PostgreSQL (`postgres:16`)**. Tuyệt đối không dùng H2 In-Memory DB để tránh sai lệch về kiểu `NUMERIC(10,3)` và cú pháp PostgreSQL partial index.
  2. **Frontend Test Automation**:
     - Component & Hook Tests: `useShoppingList`, `useOfflineSync`, `ChecklistScreen` kiểm thử bằng Vitest + React Testing Library + Mock Service Worker (`msw`).
  3. **Kiểm thử thủ công (Manual Test Cases) cho các kịch bản thực địa**:
     - Nhóm kịch bản không thể chạy tự động trong môi trường CI thông thường được chuyển thành tài liệu kịch bản kiểm thử thủ công trong `docs/test/test-cases.md`:
       * *TC-MAN-040-01 (Offline In-Store & Airplane Mode)*: Bật chế độ máy bay trên điện thoại thật, thực hiện 10 thao tác tick chọn và sửa số lượng, tắt chế độ máy bay, xác nhận dữ liệu tự động sync trong $\le 3$ giây (SC-003).
       * *TC-MAN-040-02 (Mobile Responsiveness & Single-Handed Checklist)*: Đo độ trễ phản hồi checkbox $< 50\text{ms}$ trên thiết bị Android/iOS tầm trung (SC-002).
       * *TC-MAN-040-03 (Print Stylesheet)*: Kiểm tra giao diện in thực tế trên trình duyệt Chrome/Safari loại bỏ sạch header/footer và render đúng 2 cột tiết kiệm mực.
- **Rationale**: Đáp ứng Principle II (Test-First), SC-001, SC-002, SC-003, SC-004.

---

## Điểm đã chốt với nhóm (Sign-off)

1. ✅ **Quy đổi đơn vị**: Thực hiện bằng Java code trong `UnitConversionService`, không lưu bảng `unit_conversions` vào DB (thống nhất theo `full-schema.md` mục 5 dòng 8).
2. ✅ **Idempotent Sync**: Định danh mọi item bằng `client_uuid` phía client ngay từ lúc sinh ra.
3. ✅ **Tên nhánh Git**: Tiếp tục làm việc trên nhánh `feature/040-shopping-list`, không tự ý đổi tên theo `SCRUM-xx` khi chưa tạo task Jira.
