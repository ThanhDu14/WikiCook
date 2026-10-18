# Research: Weekly Meal Planner (Manual) – 030

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Ngày**: 2026-10-18

Mỗi mục ghi theo dạng **Decision / Rationale / Alternatives considered**. Không còn mục
NEEDS CLARIFICATION nào trong Technical Context sau phần này; những điểm phụ thuộc người khác
được ghi ở mục "Điểm cần chốt với nhóm" cuối file.

---

## R1. Vị trí mã nguồn trong repo

- **Decision**: Backend là một Maven project tại `src/backend/` (package gốc `com.wikicook`,
  module tính năng `com.wikicook.mealplan`); frontend React + TypeScript + Vite tại
  `src/frontend/`. Migration Flyway tại `src/backend/src/main/resources/db/migration/`.
- **Rationale**: `docs/management/project-plan.md` (Build v0.5, Sprint 3) ghi deliverable là
  `src/backend/`, `src/frontend/`, `docker-compose.yml`; tách hai build giúp CI chạy
  `mvn verify` và `vite build` độc lập. Tên package `com.wikicook` và TypeScript lấy theo
  `src/README.md`.
- **Alternatives considered**: Bố cục `src/main/` + `src/web/` của `src/README.md` – đặt
  `pom.xml` ngay trong `src/` lẫn với `specs/`, `templates/` dùng cho nộp bài, dễ nhầm.
- **Trạng thái**: ✅ Đã chốt 2026-10-18. Mục 4 của `src/README.md` cần sửa lại cho khớp.

## R2. Múi giờ và cách xác định tuần

- **Decision**: Mọi phép tính "hôm nay", "tuần này" dùng `ZoneId.of("Asia/Ho_Chi_Minh")` qua
  một bean `java.time.Clock` (`MealPlanClockConfig`) để test cố định được thời gian.
  `weekStart` truyền trên URL dạng `YYYY-MM-DD`, server kiểm tra là thứ Hai (nếu không → 400
  `INVALID_WEEK_START`). DB giữ `CHECK (EXTRACT(ISODOW FROM week_start) = 1)` (full-schema 4.6).
  Frontend tính tuần hiện tại cũng theo UTC+7 (`Intl.DateTimeFormat` với `timeZone`), không
  theo giờ máy.
- **Rationale**: FR-002, Assumptions; tuần qua tháng/năm (29/12 – 04/01) tự đúng vì chỉ cộng
  ngày trên `LocalDate`.
- **Alternatives considered**: Dùng ISO week number (`2026-W43`) – khó đọc và lệch quanh năm
  mới; dùng giờ trình duyệt – sai với người dùng đang ở múi giờ khác.

## R3. Khi nào tạo dòng `meal_plans`

- **Decision**: Tạo lười (lazy). `GET` một tuần chưa có kế hoạch trả về tuần rỗng, không ghi DB.
  Thao tác ghi đầu tiên gọi `MealPlanRepository.findOrCreate(userId, weekStart)` dùng
  `INSERT ... ON CONFLICT (user_id, week_start) DO NOTHING` rồi `SELECT`, an toàn khi hai
  request đến cùng lúc.
- **Rationale**: FR-002 (tối đa một plan/tuần) được DB đảm bảo; người dùng lướt nhiều tuần
  không sinh hàng nghìn dòng rỗng.
- **Alternatives considered**: Tạo khi `GET` – sinh dữ liệu rác; bắt `DataIntegrityViolation`
  rồi đọc lại – được nhưng làm hỏng transaction hiện tại của JPA.

## R4. Định danh ô và phát hiện xung đột

- **Decision**: Ô được định danh bằng `(weekStart, day, meal)` của người dùng đang đăng nhập;
  URL không chứa `user_id` hay `meal_plan_id`. Mỗi lệnh ghi gửi kèm **trạng thái mong đợi** của
  ô: `expectedItemId` + `expectedVersion` (hoặc `null` nghĩa là "mong ô đang trống"). Không khớp
  → `409 SLOT_CHANGED` kèm tuần mới nhất; frontend rollback và tải lại tuần. Cột
  `meal_plan_items.version` map `@Version`.
- **Rationale**: Edge case "slot changed elsewhere before an edit is saved → refused, reload".
  "Last saved change wins" vẫn đúng với các thao tác lần lượt trên dữ liệu mới nhất. Không có
  id người khác trên URL nên FR-006/NFR-004 đúng theo cấu trúc.
- **Alternatives considered**: `If-Match`/ETag cho cả tuần – một thay đổi ở ô khác cũng làm
  thao tác bị từ chối, quá chặt; không kiểm tra gì – vi phạm edge case trên.

## R5. Move / swap nguyên tử

- **Decision**: Move vào ô trống = `UPDATE` `day_of_week`, `meal` của item. Swap = **đổi nội
  dung** (recipe_id, recipe_name_snapshot, servings, source) giữa hai dòng, vị trí giữ nguyên.
  Cả hai chạy trong một `@Transactional` với `SELECT ... FOR UPDATE` trên các item liên quan
  theo thứ tự id tăng dần (tránh deadlock), sau đó kiểm tra `expected*` của cả nguồn và đích.
- **Rationale**: NFR-005, SC-005. Đổi nội dung không bao giờ đụng `UNIQUE (meal_plan_id,
  day_of_week, meal)` nên không cần constraint `DEFERRABLE` hay ô tạm.
- **Alternatives considered**: Đổi `day/meal` của hai dòng với constraint `DEFERRABLE
  INITIALLY DEFERRED` – đúng nhưng phải sửa DDL chung và khó viết bằng JPA; xóa + chèn lại –
  mất `id` và làm hỏng tham chiếu `shopping_list_item_sources.meal_plan_item_id` của spec 040.

## R6. Xếp hạng trong hộp chọn công thức (FR-007, FR-007a, FR-007b)

- **Decision**: Một native query chỉ-đọc `RecipePickerQueryRepository` trong module `mealplan`,
  tham số hóa hoàn toàn (`NamedParameterJdbcTemplate`), trả về trang 20 dòng:
  - Lọc: `status = 'PUBLISHED' AND NOT is_snack` (món ăn vặt/đồ uống không vào planner, khớp
    full-schema 4.5 và spec Assumptions); nếu có `q` thì
    `f_unaccent(lower(title)) % f_unaccent(lower(:q)) OR f_unaccent(lower(title)) LIKE '%' || f_unaccent(lower(:q)) || '%'`.
  - Cột tính `conflict` = có nguyên liệu dính dị ứng (`ingredient_allergens` ∩ dị ứng người
    dùng) hoặc nằm trong danh sách loại trừ, hoặc vi phạm kiểu ăn theo `ingredients.kind`
    (VEGETARIAN: không MEAT/SEAFOOD; VEGAN: không MEAT/SEAFOOD/EGG/DAIRY/HONEY; PESCATARIAN:
    không MEAT). Trả về danh sách lý do để hiện nhãn cảnh báo.
  - `ORDER BY conflict ASC, meal_suited DESC, cuisine_preferred DESC, within_max_time DESC,
    similarity DESC, title ASC`. `meal_suited` đọc `for_breakfast/for_lunch/for_dinner` theo bữa
    của ô.
  - Hồ sơ (dị ứng, loại trừ, kiểu ăn, ẩm thực ưa thích, thời gian tối đa) lấy qua
    `DietaryProfilePort` (spec 011) rồi truyền vào làm tham số; query không đọc bảng `user_*`.
- **Rationale**: Thứ tự FR-007a là thứ tự sắp xếp nhiều tiêu chí, làm trong SQL thì phân trang
  đúng và nhanh (index trigram + index bữa ở full-schema 4.5). Quy tắc kiểu ăn khớp 031 FR-009a.
- **Alternatives considered**: Gọi API tìm kiếm của spec 020 – thứ tự xếp khác (020 không xếp
  theo bữa) và chưa có; xếp hạng trong Java sau khi lấy hết – không phân trang được với 10.000
  công thức. **Cần B đồng ý** thêm index GIN trigram trên `f_unaccent(lower(title))` của
  `recipes` (hoặc dùng chung index của spec 020 nếu có).

## R7. Phụ thuộc module khác qua port

- **Decision**: Module `mealplan` khai báo các interface (port) trong
  `com.wikicook.mealplan.port`, module sở hữu dữ liệu cài đặt:

  | Port | Cài đặt bởi | Dùng cho |
  |---|---|---|
  | `DietaryProfilePort` | `user`/`profile` (011, A) | Household size (FR-012), dị ứng/loại trừ/kiểu ăn/ẩm thực/thời gian (FR-007a/b) |
  | `CollectionRecipePort` | `collection` (080, A) | Tab "Từ bộ sưu tập của tôi" (FR-007) |
  | `RecipeSummaryPort` | `recipe` (060, B) | Ảnh bìa, tên, tổng thời gian, servings, trạng thái của nhiều công thức một lần |
  | `DayNutritionPort` | `nutrition` (090, C) | Tổng calo/người/ngày (FR-032) |
  | `SlotSuggestionPort` | `aiplanner` (031, C) | "Gợi ý cho bạn" (FR-007c) – gọi từ frontend qua endpoint của 031, port chỉ dùng nếu cần |

  Khi module kia chưa có, dùng bean `@ConditionalOnMissingBean` mặc định: profile rỗng, bộ sưu
  tập rỗng, dinh dưỡng `null` (UI hiện "Chưa có số liệu dinh dưỡng").
- **Rationale**: Prompt yêu cầu không truy cập bảng module khác trực tiếp; nhóm làm song song nên
  030 phải chạy được trước khi 011/080/090 xong.
- **Alternatives considered**: Repository dùng chung – phá ranh giới module; gọi HTTP nội bộ –
  thừa với monolith.

## R8. Hiển thị công thức không còn khả dụng (FR-020)

- **Decision**: Item "không khả dụng" khi `recipe_id IS NULL` (công thức bị xóa, FK `SET NULL`)
  hoặc công thức có `status <> 'PUBLISHED'`. DTO trả `available=false`, `recipeName` từ
  `recipe_name_snapshot`, không trả ảnh/link. Snapshot cắt còn 100 ký tự khi lưu.
- **Rationale**: Khớp full-schema 4.6 và 6 (xóa công thức → `SET NULL`, giữ tên).
- **Alternatives considered**: Xóa item khi công thức bị archive – người dùng mất thông tin, vi
  phạm User Story 6.

## R9. Giá trị servings mặc định (FR-012)

- **Decision**: Server quyết định servings khi client không gửi: `household_size` từ
  `DietaryProfilePort`, nếu `null` thì `recipes.servings` của công thức, kẹp vào 1–20 (công thức
  cho phép tới 50). Client chỉ gửi `servings` khi người dùng đổi.
- **Rationale**: Một quy tắc ở một chỗ, spec 031 dùng lại khi confirm (031 FR-002).
- **Alternatives considered**: Client tự tính – trùng logic ở hai nơi, dễ lệch.

## R10. "Thêm vào meal plan" từ trang chi tiết (FR-008, FR-009)

- **Decision**: Cùng endpoint `PUT .../slots/{day}/{meal}` với `expectedItemId=null`. Nếu ô đã có
  món, server trả `409 SLOT_OCCUPIED` kèm tên món hiện tại; dialog hỏi "Thay thế?" rồi gửi lại
  với `expectedItemId/expectedVersion` của món đó. Khách: frontend chuyển tới
  `/login?redirect=` + `encodeURIComponent('/recipes/{id}?action=add-to-plan')`; sau đăng nhập
  trang công thức đọc `action` và mở lại dialog. Công thức "Thêm vào meal plan" là
  `is_snack` thì nút bị ẩn (spec Assumptions).
- **Quy ước route frontend**: đường dẫn tiếng Anh, kebab-case, giống `/meal-planner` và
  `/shopping-list` đã dùng: `/login`, `/recipes/{id}`, `/meal-planner?week=YYYY-MM-DD`,
  `/shopping-list/import?weekStart=YYYY-MM-DD`. Chữ tiếng Việt chỉ nằm trong giao diện
  (`strings.vi.ts`), không nằm trong URL. Cần A (010) và B (060) xác nhận cho `/login` và
  `/recipes/{id}`.
- **Rationale**: Không cần endpoint riêng; câu hỏi "thay thế" dựa trên trạng thái thật ở server.
- **Alternatives considered**: Endpoint `POST /recipes/{id}/add-to-plan` riêng – trùng logic.

## R11. Lỗi trả về client

- **Decision**: `ProblemDetail` (RFC 9457, có sẵn trong Spring 6) với trường mở rộng `code`:
  `VALIDATION_ERROR`, `INVALID_WEEK_START`, `SLOT_CHANGED`, `SLOT_OCCUPIED`,
  `RECIPE_NOT_AVAILABLE`, `UNAUTHENTICATED`. Không trả stack trace (constitution IV).
- **Rationale**: Frontend phân nhánh theo `code`, chuỗi tiếng Việt nằm ở string store frontend.
- **Alternatives considered**: Object lỗi tự định nghĩa – không chuẩn, phải viết lại handler.

## R12. Trạng thái và optimistic update ở frontend

- **Decision**: Hook `useWeekPlan(weekStart)` dùng `useReducer`; mỗi thao tác: lưu snapshot →
  áp thay đổi lên state → gọi API → thành công thì thay bằng item trả về, thất bại thì khôi phục
  snapshot và hiện toast lỗi (FR-019). Gọi API qua lớp client dùng chung `src/frontend/src/api/`.
  Không thêm thư viện quản lý state/fetch.
- **Rationale**: Constitution III (không thêm thư viện khi không cần); chỉ một trang dùng dữ liệu
  này.
- **Alternatives considered**: TanStack Query – tiện cho optimistic update nhưng là phụ thuộc mới
  cho một trang; có thể cân nhắc lại nếu nhóm dùng cho cả dự án.

## R13. Kéo-thả

- **Decision**: `@dnd-kit/core` (+ `@dnd-kit/utilities`), chỉ bật ở layout desktop (≥ 1024 px,
  breakpoint `lg` của Tailwind). Bàn phím và màn hình cảm ứng dùng menu "Chuyển sang…" (FR-015);
  dnd-kit có `KeyboardSensor` và thông báo `aria-live` cho NFR-003.
- **Rationale**: HTML5 Drag and Drop gốc không hỗ trợ bàn phím, khó test với RTL và lỗi trên
  Safari; dnd-kit nhỏ (~10 kB gzip), đang được bảo trì, có API cho accessibility.
- **Alternatives considered**: `react-beautiful-dnd` – đã ngừng bảo trì; `react-dnd` – không có
  sẵn hỗ trợ bàn phím. Ghi trong Complexity Tracking của plan.

## R14. Mất kết nối (FR-027 – FR-029)

- **Decision**: Hook `useOnlineStatus` = `navigator.onLine` + sự kiện `online`/`offline` + đánh
  dấu offline khi `fetch` ném `TypeError` (lỗi mạng). Khi offline: thay lịch bằng
  `<OfflineNotice>` với nút "Thử lại"; khi `online` hoặc bấm "Thử lại" thành công → tải lại tuần.
  Không dùng Service Worker cache hay Local Storage cho dữ liệu plan.
- **Rationale**: Clarification 2026-10-08 (không xem offline). SC-006 yêu cầu quay lại trong 5 s
  → sự kiện `online` kích hoạt ngay.
- **Alternatives considered**: Ping server định kỳ – tốn request; dùng chung offline mode của
  spec 050 – ngược yêu cầu.

## R15. Bàn giao sang Shopping List (FR-024, FR-025)

- **Decision**: Module `mealplan` cung cấp `MealPlanShoppingFacade.getHandoff(userId,
  weekStart)` (Java, dùng nội bộ bởi spec 040) và endpoint
  `GET /api/v1/meal-plans/{weekStart}/shopping-handoff` (để frontend biết số món bỏ qua trước khi
  mở flow 040). Nút "Tạo shopping list" chuyển tới route import của 040 với `weekStart`. Chi tiết
  ở [contracts/integration.md](contracts/integration.md).
- **Rationale**: 040 tự dựng preview và chọn gộp/tạo mới (040 FR-004); 030 chỉ đưa danh sách món
  khả dụng + servings + id item (cho `shopping_list_item_sources.meal_plan_item_id`).
- **Alternatives considered**: 030 gọi thẳng API tạo shopping list – lấn sang phần của D.

## R16. Tổng calo theo ngày (FR-032)

- **Decision**: Response tuần có `days[].nutrition` lấy từ `DayNutritionPort.totals(recipeIdsByDay)`
  – một lần gọi cho cả tuần. Mỗi ngày: `kcalPerPerson`, `includedDishes`, `missingDishes`; `null`
  khi không có món nào có ước tính. Servings của món không ảnh hưởng (090 Assumptions).
- **Rationale**: Không thêm request; spec 090 sở hữu cách tính.
- **Alternatives considered**: Frontend gọi API 090 riêng cho từng ngày – 7 request mỗi tuần.

## R17. Kiểm thử

- **Decision**: Backend: JUnit 5 + Mockito cho `MealPlanService`, `SlotMoveService`,
  `ServingsPolicy`, `WeekCalendar`; `@DataJpaTest` + Testcontainers (image
  `pgvector/pgvector:pg16` vì `V001__extensions.sql` bật `vector`) cho repository và query picker;
  `@SpringBootTest` + MockMvc cho controller (bao gồm 401, 409, quyền sở hữu). Frontend: Vitest +
  RTL cho `WeekGrid`, `DayView`, `RecipePicker`, `MoveToMenu`, `OfflineNotice`, `AddToPlanDialog`;
  API mock bằng `msw`.
- **Rationale**: Constitution II; SC-008 mỗi acceptance scenario ≥ 1 test.
- **Alternatives considered**: H2 – constitution cấm; mock `fetch` thủ công – dài, khó đọc hơn
  `msw` (dev dependency, ghi trong Complexity Tracking).

---

## Điểm cần chốt với nhóm

| # | Với | Nội dung | Đề xuất trong plan |
|---|---|---|---|
| 1 | Hiển (Tech Lead) | Layout mã nguồn | ✅ Đã chốt `src/backend`, `src/frontend` (R1) |
| 2 | B (060/020) | Index trigram trên tiêu đề công thức; `RecipeSummaryPort` | B cài port, index thêm trong `V202` |
| 3 | A (011/080) | `DietaryProfilePort`, `CollectionRecipePort` | Interface do C định nghĩa, A cài |
| 4 | D (040) | Route import và `MealPlanShoppingFacade` | Theo [contracts/integration.md](contracts/integration.md) |
| 5 | Cả nhóm | Mã Jira cho 030 | Cần trước khi tạo branch `feature/SCRUM-xx-meal-planner` |
