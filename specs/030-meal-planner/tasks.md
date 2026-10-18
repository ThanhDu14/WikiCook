---

description: "Task list for 030 Weekly Meal Planner (Manual)"
---

# Tasks: Weekly Meal Planner (Manual)

**Input**: Design documents from `specs/030-meal-planner/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/openapi.yaml](contracts/openapi.yaml),
[contracts/integration.md](contracts/integration.md), [quickstart.md](quickstart.md)

**Tests**: BẮT BUỘC (constitution Principle II, spec SC-008). Trong mỗi phase, task test đứng
trước task code và **phải chạy FAIL trước** khi viết code (Red → Green → Refactor). Mỗi task test
ghi mã acceptance scenario (`US1-3`…) hoặc edge case mà nó phủ.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- **⛓ A / B / D / 090**: task chạm điểm nối với người khác (A: 011/080, B: 060/020, D: 040,
  090: dinh dưỡng). Làm trước bằng bean/adapter mặc định để không bị chặn, rồi thay bằng cài đặt
  thật khi bên kia merge.

## Path Conventions

- Backend main: `src/backend/src/main/java/com/wikicook/mealplan/`
- Backend test: `src/backend/src/test/java/com/wikicook/mealplan/`
- Migration: `src/backend/src/main/resources/db/migration/`
- Frontend: `src/frontend/src/features/meal-planner/` (test trong `__tests__/` cùng thư mục)
- Tiền điều kiện: skeleton Build v0.5 đã có (`src/backend/pom.xml`, `src/frontend/package.json`,
  `docker-compose.yml`, Spring Security + JWT của spec 010, API client dùng chung
  `src/frontend/src/api/client.ts`).

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Dependency, khung thư mục, hạ tầng test

- [ ] T001 Kiểm tra skeleton Build v0.5 tồn tại (src/backend/pom.xml, src/frontend/package.json, docker-compose.yml) và tạo các package rỗng `controller`, `service`, `repository`, `domain`, `dto`, `port`, `api`, `config` trong src/backend/src/main/java/com/wikicook/mealplan/ cùng `service`, `repository`, `controller` trong src/backend/src/test/java/com/wikicook/mealplan/
- [ ] T002 [P] Thêm (nếu chưa có) `springdoc-openapi-starter-webmvc-ui`, `org.testcontainers:postgresql`, `org.testcontainers:junit-jupiter`, `spring-boot-testcontainers` vào src/backend/pom.xml
- [ ] T003 [P] Thêm `@dnd-kit/core`, `@dnd-kit/utilities` (dependencies) và `msw` (devDependencies) vào src/frontend/package.json – lý do ở plan.md Complexity Tracking
- [ ] T004 [P] Tạo base class `AbstractPostgresIT` dùng `PostgreSQLContainer` với image `pgvector/pgvector:pg16` (`asCompatibleSubstituteFor("postgres")`) + `@ServiceConnection`, chạy Flyway thật, trong src/backend/src/test/java/com/wikicook/support/AbstractPostgresIT.java
- [ ] T005 [P] Cấu hình msw cho Vitest: src/frontend/src/test/msw/server.ts, src/frontend/src/test/setup.ts (`beforeAll(server.listen)`…) và khai báo `setupFiles` trong src/frontend/vitest.config.ts
- [ ] T006 [P] Tạo thư mục src/frontend/src/features/meal-planner/{pages,components,hooks,lib,__tests__} và đăng ký route `/meal-planner` (placeholder) trong router của skeleton src/frontend/src/router.tsx

**Checkpoint**: `./mvnw verify` và `npm test` chạy được (chưa có test của 030).

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema, entity, lịch tuần, port, xử lý lỗi, API client – mọi user story đều cần

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Tests cho Foundational (viết trước, phải FAIL)

- [ ] T007 [P] Unit test `WeekCalendarTest` – `weekStartOf(instant)` theo `Asia/Ho_Chi_Minh` (Chủ nhật 17:30 UTC = thứ Hai 00:30 giờ VN → tuần mới), `isMonday`, `today()` với `Clock.fixed`, 7 ngày của tuần 2026-12-28 → 2027-01-03 (edge case qua năm) trong src/backend/src/test/java/com/wikicook/mealplan/service/WeekCalendarTest.java
- [ ] T008 [P] Integration test `MealPlanSchemaIT` (extends `AbstractPostgresIT`) – migration V301 chạy được; vi phạm `UNIQUE (user_id, week_start)`, `CHECK (EXTRACT(ISODOW FROM week_start) = 1)`, `UNIQUE (meal_plan_id, day_of_week, meal)`, `CHECK servings BETWEEN 1 AND 20`, `CHECK meal IN ('BREAKFAST','LUNCH','DINNER')`, `CHECK source IN ('MANUAL','AI','AUTO')` đều bị từ chối; xóa user → plan bị xóa (`CASCADE`, edge case "Account deleted"); xóa recipe → `recipe_id` thành NULL, `recipe_name_snapshot` giữ nguyên (`SET NULL`) trong src/backend/src/test/java/com/wikicook/mealplan/repository/MealPlanSchemaIT.java
- [ ] T009 [P] Integration test `MealPlanRepositoryIT` – `findOrCreate(userId, weekStart)` gọi đồng thời từ 2 thread chỉ tạo 1 dòng (FR-002); `findByUserIdAndWeekStart` không trả plan của user khác (FR-006); `lockItemsByIdsOrdered` trả item theo id tăng dần trong src/backend/src/test/java/com/wikicook/mealplan/repository/MealPlanRepositoryIT.java
- [ ] T010 [P] Web slice test `MealPlanExceptionHandlerTest` (`@WebMvcTest` với controller giả) – mỗi exception map đúng `status` + `code` (`VALIDATION_ERROR` 400, `INVALID_WEEK_START` 400, `SLOT_CHANGED` 409, `SLOT_OCCUPIED` 409 kèm `currentItem`, `RECIPE_NOT_AVAILABLE` 422), content type `application/problem+json`, body không có stack trace hay tên class trong src/backend/src/test/java/com/wikicook/mealplan/controller/MealPlanExceptionHandlerTest.java
- [ ] T011 [P] Unit test `MondayDateValidatorTest` – `2026-10-19` hợp lệ, `2026-10-20` không hợp lệ, `null` không hợp lệ trong src/backend/src/test/java/com/wikicook/mealplan/controller/MondayDateValidatorTest.java
- [ ] T012 [P] Test frontend `week.test.ts` – `currentWeekStart()` theo UTC+7 bất kể múi giờ máy (giả lập `TZ=America/New_York`), `addWeeks`, `formatWeekRange` ra "13/10 – 19/10/2026", tuần qua năm "28/12/2026 – 03/01/2027", `dayIndexOfToday` trong src/frontend/src/features/meal-planner/__tests__/week.test.ts
- [ ] T013 [P] Test frontend `mealPlanApi.test.ts` (msw) – mỗi hàm gọi đúng method/URL của contracts/openapi.yaml; ProblemDetail được chuyển thành `ApiError { status, code, currentItem? }`; lỗi mạng (`TypeError`) thành `ApiError { code: 'NETWORK' }` trong src/frontend/src/features/meal-planner/__tests__/mealPlanApi.test.ts

### Implementation cho Foundational

- [ ] T014 Viết migration src/backend/src/main/resources/db/migration/V301__meal_plans.sql đúng data-model.md mục 1: `meal_plans(id BIGINT GENERATED ALWAYS AS IDENTITY PK, user_id BIGINT NOT NULL REFERENCES users ON DELETE CASCADE, week_start DATE NOT NULL CHECK (EXTRACT(ISODOW FROM week_start) = 1), created_at, updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), UNIQUE (user_id, week_start))`; `meal_plan_items(id …, meal_plan_id BIGINT NOT NULL REFERENCES meal_plans ON DELETE CASCADE, day_of_week SMALLINT NOT NULL CHECK BETWEEN 1 AND 7, meal VARCHAR(10) NOT NULL CHECK IN ('BREAKFAST','LUNCH','DINNER'), recipe_id BIGINT NULL REFERENCES recipes ON DELETE SET NULL, recipe_name_snapshot VARCHAR(100) NOT NULL, servings SMALLINT NOT NULL CHECK BETWEEN 1 AND 20, source VARCHAR(10) NOT NULL CHECK IN ('MANUAL','AI','AUTO'), version INTEGER NOT NULL DEFAULT 0, created_at, updated_at, UNIQUE (meal_plan_id, day_of_week, meal))`; index `(recipe_id)` ⛓ A, B (cần `V101` users và `V202` recipes; nếu chưa merge, tạo migration stub chỉ dùng cho test tại src/backend/src/test/resources/db/migration/V100__test_stub_users_recipes.sql)
- [ ] T015 [P] Tạo enum `Meal` (BREAKFAST, LUNCH, DINNER – đúng thứ tự hiển thị FR-003), `ItemSource` (MANUAL, AI, AUTO), `DietType` (NONE, VEGETARIAN, VEGAN, PESCATARIAN) trong src/backend/src/main/java/com/wikicook/mealplan/domain/
- [ ] T016 [P] Tạo `DayOfWeekConverter` (`AttributeConverter<DayOfWeek, Short>`, ISO 1 = MONDAY … 7 = SUNDAY) trong src/backend/src/main/java/com/wikicook/mealplan/domain/DayOfWeekConverter.java
- [ ] T017 Tạo entity `MealPlan` (`Long id`, `Long userId`, `LocalDate weekStart`, `Instant createdAt/updatedAt`) và `MealPlanItem` (`@ManyToOne MealPlan`, `DayOfWeek day`, `Meal meal`, `Long recipeId` nullable, `String recipeNameSnapshot` tối đa 100 ký tự, `short servings`, `ItemSource source`, `@Version int version`) trong src/backend/src/main/java/com/wikicook/mealplan/domain/MealPlan.java và MealPlanItem.java (phụ thuộc T014–T016)
- [ ] T018 Tạo `MealPlanRepository` (`findByUserIdAndWeekStart`, `findOrCreate` dùng native `INSERT … ON CONFLICT (user_id, week_start) DO NOTHING` rồi `SELECT`) và `MealPlanItemRepository` (`findByMealPlanId`, `findSlot(planId, day, meal)`, `lockItemsByIdsOrdered(ids)` với `@Lock(PESSIMISTIC_WRITE)` + `ORDER BY id`, `deleteByMealPlanIdAndDay`) trong src/backend/src/main/java/com/wikicook/mealplan/repository/ (làm T009 xanh)
- [ ] T019 [P] Tạo `MealPlanClockConfig` (bean `Clock` zone `Asia/Ho_Chi_Minh`) và `WeekCalendar` (`today()`, `currentWeekStart()`, `weekStartOf(LocalDate)`, `isMonday`, `datesOf(weekStart)`) trong src/backend/src/main/java/com/wikicook/mealplan/config/MealPlanClockConfig.java và service/WeekCalendar.java (làm T007 xanh)
- [ ] T020 [P] Tạo các port và record theo contracts/integration.md mục 1: `DietaryProfilePort` + `PlannerProfile`, `CollectionRecipePort`, `RecipeSummaryPort` + `RecipeSummary`, `DayNutritionPort` + `DayNutrition` trong src/backend/src/main/java/com/wikicook/mealplan/port/
- [ ] T021 Tạo `DefaultPortsConfig` với bean `@ConditionalOnMissingBean`: profile rỗng (householdSize null, dietType NONE, các set rỗng) ⛓ A; bộ sưu tập rỗng ⛓ A; `DayNutritionPort` trả map rỗng ⛓ 090; và adapter tạm `JdbcRecipeSummaryAdapter` đọc `recipes` (title, total_time_minutes, servings, status, is_snack), ảnh `recipe_media` `role = 'COVER'` (`COALESCE(storage_key, external_url)`), diet type suy từ `ingredients.kind` (VEGAN nếu không có MEAT/SEAFOOD/EGG/DAIRY/HONEY; VEGETARIAN nếu không có MEAT/SEAFOOD; PESCATARIAN nếu không có MEAT; ngược lại NONE) ⛓ B trong src/backend/src/main/java/com/wikicook/mealplan/port/DefaultPortsConfig.java và port/JdbcRecipeSummaryAdapter.java
- [ ] T022 [P] Tạo annotation `@MondayDate` + `MondayDateValidator` trong src/backend/src/main/java/com/wikicook/mealplan/controller/MondayDate.java (làm T011 xanh)
- [ ] T023 Tạo exception `InvalidWeekStartException`, `SlotChangedException`, `SlotOccupiedException(PlanItemDto currentItem)`, `RecipeNotAvailableException` trong src/backend/src/main/java/com/wikicook/mealplan/service/exception/ và `MealPlanExceptionHandler` (`@RestControllerAdvice` cho package `mealplan`, trả `ProblemDetail` + property `code`, không lộ stack trace) trong src/backend/src/main/java/com/wikicook/mealplan/controller/MealPlanExceptionHandler.java (làm T010 xanh)
- [ ] T024 Cấu hình bảo mật: `/api/v1/meal-plans/**` yêu cầu đăng nhập, 401 trả ProblemDetail `UNAUTHENTICATED`; helper `CurrentUser.id()` lấy userId từ `SecurityContext` – thêm rule vào SecurityConfig của spec 010 (src/backend/src/main/java/com/wikicook/auth/config/SecurityConfig.java) và tạo src/backend/src/main/java/com/wikicook/mealplan/controller/CurrentUser.java
- [ ] T025 [P] Tạo type TypeScript khớp contracts/openapi.yaml (`WeekPlan`, `DayPlan`, `Slot`, `PlanItem`, `Day`, `Meal`, `ItemSource`, `PickerPage`, `PickerResult`, `ShoppingHandoff`, `ApiError`) trong src/frontend/src/features/meal-planner/types.ts
- [ ] T026 Tạo `mealPlanApi.ts` (getWeek, putSlot, patchServings, moveItem, deleteSlot, clearDay, getShoppingHandoff, searchPickerRecipes) dùng client chung src/frontend/src/api/client.ts trong src/frontend/src/api/mealPlanApi.ts (làm T013 xanh)
- [ ] T027 [P] Tạo `week.ts` (`currentWeekStart`, `addWeeks`, `formatWeekRange`, `dayIndexOfToday`, dùng `Intl.DateTimeFormat` với `timeZone: 'Asia/Ho_Chi_Minh'`) trong src/frontend/src/features/meal-planner/lib/week.ts (làm T012 xanh)
- [ ] T028 [P] Tạo string store tiếng Việt cho mọi nhãn, thông báo lỗi theo `code`, trạng thái rỗng/offline trong src/frontend/src/features/meal-planner/strings.vi.ts

**Checkpoint**: Migration chạy trên Testcontainers; T007–T013 xanh; chưa có endpoint nghiệp vụ.

---

## Phase 3: User Story 1 - Plan the week by filling empty slots (Priority: P1) 🎯 MVP

**Goal**: Xem tuần hiện tại 7×3, bấm ô trống mở picker (tìm tên, tab bộ sưu tập, xếp hạng theo bữa và hồ sơ), thêm món với servings mặc định, đổi servings, lưu ngay.

**Independent Test**: Đăng nhập, mở Meal Planner, thêm 3 công thức PUBLISHED vào 3 ô qua picker, tải lại trang → 3 món vẫn đúng ô với đúng servings (quickstart Q1–Q5).

### Tests for User Story 1 ⚠️ (viết trước, phải FAIL)

- [ ] T029 [P] [US1] Unit test `ServingsPolicyTest` – household 4 → 4 (US1-5); household null → servings công thức (US1-6); công thức 50 servings và household null → 20 (kẹp 1–20); servings công thức null → 1 trong src/backend/src/test/java/com/wikicook/mealplan/service/ServingsPolicyTest.java
- [ ] T030 [P] [US1] Unit test `MealPlanServiceTest` (Mockito, mock repository + port) – `getWeek` tuần chưa có plan trả 7 ngày × 3 ô rỗng và **không** gọi `findOrCreate`; `getWeek` có `today` khi tuần chứa hôm nay (US1-1); `putSlot` ô trống với `expected = null` tạo item `source = MANUAL`, `recipeNameSnapshot` = title cắt 100 ký tự; công thức không PUBLISHED hoặc `is_snack` → `RecipeNotAvailableException` (FR-010, spec Assumptions); `expected` không khớp → `SlotChangedException`; `patchServings` lưu giá trị mới (US1-7); đổi household không đổi servings món đã có (FR-013); cùng công thức ở hai ô được phép (edge case) trong src/backend/src/test/java/com/wikicook/mealplan/service/MealPlanServiceTest.java
- [ ] T031 [P] [US1] Integration test `RecipePickerQueryRepositoryIT` với seed 30 công thức – ô BREAKFAST + `q = "trung"` tìm ra "Trứng chiên" (không dấu, FR-007); 5 kết quả đầu đều `for_breakfast` khi có ≥ 5 món sáng khớp (US1-3, SC-007); trong nhóm hợp bữa, món thuộc ẩm thực ưa thích rồi món trong thời gian tối đa đứng trước (FR-007a); món chứa dị ứng, nguyên liệu loại trừ hoặc vi phạm kiểu ăn (VEGAN gặp DAIRY, VEGETARIAN gặp SEAFOOD như nước mắm) đứng cuối với `conflicts` đúng loại và tên (US1-4, FR-007b); chỉ trả `status = 'PUBLISHED'` (US6-3); không trả công thức `is_snack` (spec Assumptions); `scope = COLLECTIONS` chỉ trả id trong set đã lưu; phân trang `hasNext` đúng; không có `q` vẫn áp thứ tự FR-007a trong src/backend/src/test/java/com/wikicook/mealplan/repository/RecipePickerQueryRepositoryIT.java
- [ ] T032 [P] [US1] Integration test `MealPlanControllerUS1IT` (`@SpringBootTest` + MockMvc + `AbstractPostgresIT`) – `GET /api/v1/meal-plans/2026-10-19` 200 đúng shape `WeekPlan` (7 ngày, mỗi ngày 3 slot theo thứ tự BREAKFAST, LUNCH, DINNER); `GET …/2026-10-20` 400 `INVALID_WEEK_START`; không token 401; `PUT …/slots/TUESDAY/BREAKFAST` 200 trả `PlanItem`; `PATCH …/servings` với 0, 21, 2.5 → 400 `VALIDATION_ERROR` và giá trị cũ giữ nguyên (edge case servings); user B gọi cùng `weekStart` chỉ thấy plan rỗng của B (FR-006, NFR-004); hai token của cùng user (hai thiết bị) thấy cùng dữ liệu sau PUT (US1-9, SC-004) trong src/backend/src/test/java/com/wikicook/mealplan/controller/MealPlanControllerUS1IT.java
- [ ] T033 [P] [US1] Integration test `PickerControllerIT` – thiếu `meal` → 400; `size > 50` → 400; không token 401; response đúng `PickerPage` trong src/backend/src/test/java/com/wikicook/mealplan/controller/PickerControllerIT.java
- [ ] T034 [P] [US1] Test `useWeekPlan.test.ts` (msw) – tải tuần; `addDish` cập nhật state ngay (optimistic) rồi thay bằng item từ server; server 500 → khôi phục state cũ + phát lỗi (FR-019); không có hàm/nút "Lưu" riêng (US1-9) trong src/frontend/src/features/meal-planner/__tests__/useWeekPlan.test.ts
- [ ] T035 [P] [US1] Test `WeekGrid.test.tsx` – 7 cột thứ Hai → Chủ nhật, cột hôm nay có `aria-current="date"` (US1-1); ô có món hiện ảnh, tên, thời gian (US1-5); bấm ảnh/tên điều hướng `/recipes/{id}` (US1-8); ảnh lỗi → ảnh placeholder, tên và thời gian vẫn hiện (edge case); tên dài bị cắt và có `title` đầy đủ (edge case) trong src/frontend/src/features/meal-planner/__tests__/WeekGrid.test.tsx
- [ ] T036 [P] [US1] Test `RecipePicker.test.tsx` – mở từ ô trống với đúng bữa (US1-2); gõ tìm có debounce; tab "Từ bộ sưu tập của tôi" gửi `scope=COLLECTIONS`; render `suggestionsSlot` phía trên kết quả khi được truyền, không truyền thì không có khoảng trống (FR-007c); món có `conflicts` hiện nhãn cảnh báo nêu tên, chọn thì hỏi xác nhận, Hủy không thêm (US1-4); không có kết quả → empty state gợi ý tên khác (US1-10); chọn món bị archive giữa chừng (server 422) → thông báo + tải lại danh sách (edge case) trong src/frontend/src/features/meal-planner/__tests__/RecipePicker.test.tsx
- [ ] T037 [P] [US1] Test `ServingsInput.test.tsx` – đổi 1–20 gọi `onChange` (US1-7); 0, 21, số lẻ → hiện thông báo, giữ giá trị cũ, không gọi API (edge case) trong src/frontend/src/features/meal-planner/__tests__/ServingsInput.test.tsx

### Implementation for User Story 1

- [ ] T038 [P] [US1] Tạo DTO record theo data-model.md mục 3: `WeekPlanResponse`, `DayPlanDto`, `SlotDto`, `PlanItemDto`, `DayNutritionDto`, `PutSlotRequest` (`@NotNull Long recipeId`, `@Min(1) @Max(20) Integer servings`, `ItemSource source`, `ExpectedSlotState expected`), `PatchServingsRequest` (`@NotNull @Min(1) @Max(20) Integer servings`, `@NotNull ExpectedSlotState expected`), `ExpectedSlotState`, `PickerResultDto`, `PickerPageDto` trong src/backend/src/main/java/com/wikicook/mealplan/dto/
- [ ] T039 [P] [US1] Cài `ServingsPolicy.defaultServings(PlannerProfile, RecipeSummary)` trong src/backend/src/main/java/com/wikicook/mealplan/service/ServingsPolicy.java (làm T029 xanh)
- [ ] T040 [US1] Cài `MealPlanService.getWeek`, `putSlot` (thêm/thay; thay giữ servings cũ nếu request không gửi servings – US3-4), `patchServings`; ghép dữ liệu ô qua `RecipeSummaryPort.findByIds` một lần cho cả tuần; cập nhật `meal_plans.updated_at` khi item đổi; mọi truy vấn lọc theo `userId` trong src/backend/src/main/java/com/wikicook/mealplan/service/MealPlanService.java (làm T030 xanh)
- [ ] T041 [US1] Cài `RecipePickerQueryRepository` bằng `NamedParameterJdbcTemplate`, SQL tham số hóa: lọc `status = 'PUBLISHED' AND NOT is_snack` và tên bằng `f_unaccent(lower(title)) % f_unaccent(lower(:q))` hoặc `LIKE`; cột `conflict` và danh sách lý do từ `ingredient_allergens`, danh sách loại trừ, `ingredients.kind` theo kiểu ăn; `ORDER BY conflict ASC, meal_suited DESC, cuisine_preferred DESC, within_max_time DESC, similarity DESC, title ASC`; `LIMIT/OFFSET` trong src/backend/src/main/java/com/wikicook/mealplan/repository/RecipePickerQueryRepository.java (làm T031 xanh) ⛓ B (đề nghị B thêm index GIN trigram trên `f_unaccent(lower(title))` vào V202)
- [ ] T042 [US1] Cài `PickerService.search(userId, meal, q, scope, page, size)` lấy hồ sơ từ `DietaryProfilePort`, id bộ sưu tập từ `CollectionRecipePort`, rồi gọi `RecipePickerQueryRepository` trong src/backend/src/main/java/com/wikicook/mealplan/service/PickerService.java ⛓ A
- [ ] T043 [US1] Cài `MealPlanController` (`GET /api/v1/meal-plans/{weekStart}`, `PUT …/slots/{day}/{meal}`, `PATCH …/slots/{day}/{meal}/servings`, `@MondayDate` trên `weekStart`, `@Valid` body, OpenAPI annotation) và `PickerController` (`GET /api/v1/meal-plans/picker/recipes`) trong src/backend/src/main/java/com/wikicook/mealplan/controller/ (làm T032, T033 xanh)
- [ ] T044 [US1] Cài hook `useWeekPlan(weekStart)` (`useReducer`; mỗi thao tác: snapshot → áp optimistic → gọi API → thay bằng kết quả server hoặc khôi phục snapshot + toast lỗi) trong src/frontend/src/features/meal-planner/hooks/useWeekPlan.ts (làm T034 xanh)
- [ ] T045 [P] [US1] Cài `SlotCard` (ảnh + placeholder khi lỗi, tên cắt có `title`, thời gian, link chi tiết, `aria-label` "Thứ Ba, bữa sáng, {tên món}" cho NFR-003), `EmptySlot`, `ServingsInput` trong src/frontend/src/features/meal-planner/components/ (làm T035 phần ô, T037 xanh)
- [ ] T046 [US1] Cài `WeekGrid` (lưới desktop 7 × 3, tô cột hôm nay, render `SlotCard`/`EmptySlot`) trong src/frontend/src/features/meal-planner/components/WeekGrid.tsx (làm T035 xanh)
- [ ] T047 [US1] Cài `RecipePicker` (dialog, ô tìm debounce 300 ms, tab Tất cả / Từ bộ sưu tập, prop `suggestionsSlot?: ReactNode`, nhãn cảnh báo + hộp xác nhận, empty state, xử lý 422 bằng tải lại, dùng `RecipeCard` dùng chung) trong src/frontend/src/features/meal-planner/components/RecipePicker.tsx (làm T036 xanh)
- [ ] T048 [US1] Cài `MealPlannerPage` (tuần hiện tại, `WeekGrid`, mở `RecipePicker` từ ô trống, servings) và gắn vào route `/meal-planner` trong src/frontend/src/features/meal-planner/pages/MealPlannerPage.tsx

**Checkpoint**: US1 chạy độc lập – Independent Test ở trên + quickstart Q1–Q5 đạt.

---

## Phase 4: User Story 2 - Add a recipe to the plan from its detail page (Priority: P1)

**Goal**: Nút "Thêm vào meal plan" trên trang công thức, chọn tuần/ngày/bữa, hỏi trước khi thay; khách được chuyển sang đăng nhập rồi quay lại.

**Independent Test**: Mở một công thức PUBLISHED → "Thêm vào meal plan" → thứ Tư trưa tuần này → mở Meal Planner thấy món ở thứ Tư trưa (quickstart Q6, Q7).

### Tests for User Story 2 ⚠️ (viết trước, phải FAIL)

- [ ] T049 [P] [US2] Integration test `MealPlanControllerUS2IT` – `PUT` với `expected = null` vào ô đã có món → 409 `SLOT_OCCUPIED` có `currentItem.recipeName` (US2-3); gửi lại với `expected` của món đó → 200 thay thế (US2-3); ô trống → 200 (US2-2) trong src/backend/src/test/java/com/wikicook/mealplan/controller/MealPlanControllerUS2IT.java
- [ ] T050 [P] [US2] Test `AddToPlanDialog.test.tsx` – mặc định tuần hiện tại, có chọn ngày và bữa (US2-1); ô trống → xác nhận kèm link `/meal-planner?week=…` (US2-2); 409 `SLOT_OCCUPIED` → nêu tên món đang có và hỏi thay, "Hủy" không gọi thêm API (US2-3) trong src/frontend/src/features/meal-planner/__tests__/AddToPlanDialog.test.tsx
- [ ] T051 [P] [US2] Test `AddToPlanButton.test.tsx` – khách bấm → điều hướng `/login?redirect=` + `encodeURIComponent('/recipes/{id}?action=add-to-plan')`; trang mở với `action=add-to-plan` khi đã đăng nhập → dialog tự mở (US2-4, FR-009) trong src/frontend/src/features/meal-planner/__tests__/AddToPlanButton.test.tsx
- [ ] T052 [P] [US2] Test `MealPlannerPage.auth.test.tsx` – khách mở `/meal-planner?week=2026-10-26` → chuyển sang `/login` với `redirect` (đã encode) giữ nguyên tuần (US2-5, FR-001); API trả 401 khi đang sửa → thay đổi không lưu, chuyển đăng nhập, quay lại đúng tuần (edge case session hết hạn) trong src/frontend/src/features/meal-planner/__tests__/MealPlannerPage.auth.test.tsx

### Implementation for User Story 2

- [ ] T053 [US2] Trong `MealPlanService.putSlot`, khi `expected = null` mà ô có món → ném `SlotOccupiedException` kèm `PlanItemDto` hiện tại trong src/backend/src/main/java/com/wikicook/mealplan/service/MealPlanService.java (làm T049 xanh)
- [ ] T054 [US2] Cài `AddToPlanDialog` (chọn tuần, ngày, bữa; xử lý `SLOT_OCCUPIED` bằng hỏi thay) trong src/frontend/src/features/meal-planner/components/AddToPlanDialog.tsx (làm T050 xanh)
- [ ] T055 [US2] Cài `AddToPlanButton` (export cho trang chi tiết công thức `/recipes/{id}`; ẩn khi công thức `is_snack`; khách → `/login?redirect=…`; đọc `action=add-to-plan` để mở lại dialog) trong src/frontend/src/features/meal-planner/components/AddToPlanButton.tsx (làm T051 xanh) ⛓ B (B nhúng nút vào trang chi tiết của spec 060)
- [ ] T056 [US2] Thêm bảo vệ route cho `/meal-planner` (khách → `/login?redirect=…` đã encode, giữ `?week=`) và xử lý 401 trong `useWeekPlan` (chuyển đăng nhập, không áp optimistic) trong src/frontend/src/features/meal-planner/pages/MealPlannerPage.tsx và hooks/useWeekPlan.ts (làm T052 xanh)

**Checkpoint**: US1 + US2 chạy độc lập; quickstart Q6, Q7 đạt.

---

## Phase 5: User Story 3 - Rearrange, replace and remove dishes (Priority: P1)

**Goal**: Kéo-thả (desktop) và "Chuyển sang…" (mọi nơi) để chuyển/đổi chỗ, thay, xóa, xóa cả ngày; mọi lỗi lưu đều rollback.

**Independent Test**: Tuần có ≥ 2 món: kéo một món sang ô trống, sang ô có món, "Thay" ô thứ ba, xóa một món, xóa một ngày; tải lại → lịch khớp mọi thay đổi (quickstart Q8–Q12).

### Tests for User Story 3 ⚠️ (viết trước, phải FAIL)

- [ ] T057 [P] [US3] Unit test `SlotMoveServiceTest` – move sang ô trống đổi `day/meal` của item, ô nguồn trống (US3-1); move sang ô có món đổi **nội dung** (recipe_id, recipe_name_snapshot, servings, source) giữa hai dòng, id giữ nguyên (US3-2, research R5); `source` được giữ khi move/swap; `expected` nguồn hoặc đích không khớp → `SlotChangedException`, không gọi save; move vào chính ô đó → không đổi gì trong src/backend/src/test/java/com/wikicook/mealplan/service/SlotMoveServiceTest.java
- [ ] T058 [P] [US3] Integration test `SlotMoveAtomicityIT` (`@SpringBootTest` + `AbstractPostgresIT`) – lỗi giả lập sau lần update thứ nhất của swap (spy repository ném exception) → cả hai dòng giữ nguyên (NFR-005, SC-005); hai request swap đồng thời trên cùng hai item → một thành công, một 409, tổng số item và tập recipe không đổi (không mất/không nhân đôi món) trong src/backend/src/test/java/com/wikicook/mealplan/service/SlotMoveAtomicityIT.java
- [ ] T059 [P] [US3] Integration test `MealPlanControllerUS3IT` – `POST …/moves` trả `kind = MOVED` (1 item) và `SWAPPED` (2 item); `PUT` thay món giữ servings cũ (US3-4); `DELETE …/slots/{day}/{meal}?itemId&version` 204 (US3-5); `DELETE …/days/{day}` xóa cả 3 ô, 204 cả khi ngày đã trống (US3-6); tab A xóa món, tab B đổi servings món đó với version cũ → 409 `SLOT_CHANGED` (edge case hai tab) trong src/backend/src/test/java/com/wikicook/mealplan/controller/MealPlanControllerUS3IT.java
- [ ] T060 [P] [US3] Test `WeekGrid.dnd.test.tsx` – kéo bằng `KeyboardSensor` (Space → mũi tên → Space) sang ô trống thì move, sang ô có món thì swap (US3-1, US3-2); Esc khi đang kéo hoặc thả ngoài ô → không gọi API (edge case); thông báo `aria-live` khi nhấc/thả (NFR-003) trong src/frontend/src/features/meal-planner/__tests__/WeekGrid.dnd.test.tsx
- [ ] T061 [P] [US3] Test `MoveToMenu.test.tsx` – chỉ dùng bàn phím: mở menu món → "Chuyển sang…" → chọn ngày + bữa → gọi `moveItem` đúng tham số; ô đích có món thì hiện "Đổi chỗ với {tên}" (US3-3, FR-015) trong src/frontend/src/features/meal-planner/__tests__/MoveToMenu.test.tsx
- [ ] T062 [P] [US3] Test `SlotMenu.test.tsx` – "Thay" mở picker và giữ servings (US3-4); "Xóa" làm ô trống (US3-5); "Xóa cả ngày" hỏi xác nhận, Hủy không đổi gì, Xác nhận làm trống ngày (US3-6) trong src/frontend/src/features/meal-planner/__tests__/SlotMenu.test.tsx
- [ ] T063 [P] [US3] Bổ sung `useWeekPlan.test.ts` – move/swap/remove/clearDay lỗi 500 → lịch về trạng thái trước + thông báo "Thay đổi chưa được lưu" (US3-7, FR-019); 409 `SLOT_CHANGED` → thông báo "Kế hoạch đã thay đổi" và tải lại tuần (edge case) trong src/frontend/src/features/meal-planner/__tests__/useWeekPlan.test.ts

### Implementation for User Story 3

- [ ] T064 [US3] Cài `SlotMoveService.move(userId, weekStart, MoveRequest)` – `@Transactional`, khóa item nguồn/đích bằng `lockItemsByIdsOrdered`, kiểm tra `expected`, move = đổi `day/meal`, swap = đổi nội dung hai dòng, trả `MoveResult` trong src/backend/src/main/java/com/wikicook/mealplan/service/SlotMoveService.java (làm T057, T058 xanh)
- [ ] T065 [US3] Thêm `MealPlanService.removeSlot(userId, weekStart, day, meal, itemId, version)` và `clearDay(userId, weekStart, day)` trong src/backend/src/main/java/com/wikicook/mealplan/service/MealPlanService.java
- [ ] T066 [US3] Thêm endpoint `POST /api/v1/meal-plans/{weekStart}/moves`, `DELETE …/slots/{day}/{meal}`, `DELETE …/days/{day}` cùng DTO `MoveRequest`, `MoveResult`, `PlacedItem` trong src/backend/src/main/java/com/wikicook/mealplan/controller/MealPlanController.java và dto/ (làm T059 xanh)
- [ ] T067 [P] [US3] Cài `SlotMenu` (Thay, Xóa, Chuyển sang…) và `MoveToMenu` (chọn ngày + bữa của cùng tuần, thao tác được bằng bàn phím) trong src/frontend/src/features/meal-planner/components/SlotMenu.tsx và MoveToMenu.tsx (làm T061, T062 xanh)
- [ ] T068 [US3] Tích hợp `@dnd-kit/core` vào `WeekGrid` (`DndContext`, `useDraggable` trên `SlotCard`, `useDroppable` trên mỗi ô, `KeyboardSensor` + `PointerSensor`, `announcements` tiếng Việt, chỉ bật khi được truyền `enableDrag`) trong src/frontend/src/features/meal-planner/components/WeekGrid.tsx (làm T060 xanh)
- [ ] T069 [US3] Thêm `moveItem`, `replaceDish`, `removeDish`, `clearDay` vào `useWeekPlan` (optimistic + rollback; 409 → tải lại) và nút "Xóa cả ngày" kèm hộp xác nhận ở tiêu đề mỗi ngày trong src/frontend/src/features/meal-planner/hooks/useWeekPlan.ts và components/WeekGrid.tsx (làm T063 xanh)

**Checkpoint**: MVP (US1 + US2 + US3) hoàn chỉnh; quickstart Q1–Q12 đạt; spec 031 có thể dựa vào các thao tác sửa.

---

## Phase 6: User Story 4 - Move between weeks and start an empty week (Priority: P2)

**Goal**: Tuần trước/sau/"Tuần này", tiêu đề khoảng ngày, empty state với "Thêm món" và "Tạo bằng AI", tuần cũ vẫn sửa được.

**Independent Test**: Sang tuần sau → thấy empty state có "Tạo bằng AI"; thêm một món; về tuần này rồi sang lại → món vẫn ở tuần sau (quickstart Q13, Q14).

### Tests for User Story 4 ⚠️ (viết trước, phải FAIL)

- [ ] T070 [P] [US4] Test `WeekHeader.test.tsx` – "Tuần trước"/"Tuần sau" đổi `weekStart` ±7 ngày và hiện "13/10 – 19/10/2026" (US4-1); "Tuần này" về tuần hiện tại (US4-2); tuần 28/12/2026 hiện đúng khoảng qua năm (edge case); có prop `aiAction` thì render nó cạnh điều hướng tuần, không có thì không hiện gì (FR-031) trong src/frontend/src/features/meal-planner/__tests__/WeekHeader.test.tsx
- [ ] T071 [P] [US4] Test `EmptyWeekState.test.tsx` – tuần `empty = true` hiện thông báo + "Thêm món" + phần tử `aiAction` (US4-3); không có `aiAction` (031 chưa đăng ký) thì không có nút "Tạo bằng AI", "Thêm món" vẫn dùng được (FR-031) trong src/frontend/src/features/meal-planner/__tests__/EmptyWeekState.test.tsx
- [ ] T072 [P] [US4] Integration test `MealPlanControllerUS4IT` – `GET` và `PUT` trên tuần đã qua (2026-09-28 với `Clock` cố định 2026-10-18) đều 200 (US4-5, FR-023); thêm món ở tuần sau, `GET` tuần này rồi tuần sau → món vẫn còn trong src/backend/src/test/java/com/wikicook/mealplan/controller/MealPlanControllerUS4IT.java

### Implementation for User Story 4

- [ ] T073 [P] [US4] Cài `WeekHeader` (nút trước/sau/tuần này, khoảng ngày bằng `formatWeekRange`, slot `aiAction?: ReactNode`) trong src/frontend/src/features/meal-planner/components/WeekHeader.tsx (làm T070 xanh)
- [ ] T074 [P] [US4] Cài `EmptyWeekState` (prop `onAddDish`, `aiAction?: ReactNode`) trong src/frontend/src/features/meal-planner/components/EmptyWeekState.tsx (làm T071 xanh)
- [ ] T075 [US4] Đưa `weekStart` vào URL (`/meal-planner?week=YYYY-MM-DD`, mặc định tuần hiện tại), nối `WeekHeader` + `EmptyWeekState`; "Tạo bằng AI" lấy từ registry tùy chọn `aiPlannerEntry` (spec 031 đăng ký component `AiGenerateButton({ weekStart })`, tự quyết định bật/tắt/ẩn và mở dialog): trang render nó vào `aiAction` của `EmptyWeekState` khi tuần rỗng, và của `WeekHeader` khi tuần đã có món (031 FR-001: tuần còn ít nhất một ô trống); chưa đăng ký thì ẩn (FR-031). Test: thêm vào T071 trường hợp tuần có món → nút nằm ở header, tuần rỗng → chỉ ở empty state trong src/frontend/src/features/meal-planner/pages/MealPlannerPage.tsx và src/frontend/src/features/meal-planner/lib/aiPlannerEntry.ts (làm T072 phía UI)

**Checkpoint**: US4 chạy độc lập; quickstart Q13, Q14 đạt.

---

## Phase 7: User Story 5 - Send the week to the Shopping List (Priority: P2)

**Goal**: "Tạo shopping list" bàn giao các món khả dụng + servings của tuần cho flow import của spec 040.

**Independent Test**: Tuần có món → "Tạo shopping list" → flow 040 nhận đúng các món khả dụng với servings (quickstart Q16).

### Tests for User Story 5 ⚠️ (viết trước, phải FAIL)

- [ ] T076 [P] [US5] Unit test `ShoppingHandoffServiceTest` – chỉ trả item `available` với `mealPlanItemId, recipeId, servings, day, meal` (US5-1); đếm `skippedUnavailable` (US5-3); tuần chưa có plan → items rỗng, skipped 0 trong src/backend/src/test/java/com/wikicook/mealplan/service/ShoppingHandoffServiceTest.java
- [ ] T077 [P] [US5] Integration test `ShoppingHandoffControllerIT` – `GET /api/v1/meal-plans/{weekStart}/shopping-handoff` 200 đúng schema `ShoppingHandoff`; 401 khi chưa đăng nhập; user khác không thấy món của mình trong src/backend/src/test/java/com/wikicook/mealplan/controller/ShoppingHandoffControllerIT.java
- [ ] T078 [P] [US5] Test `CreateShoppingListButton.test.tsx` – `availableItemCount = 0` → nút bị tắt kèm gợi ý cần ít nhất một món (US5-2); có món → gọi handoff, `skippedUnavailable > 0` thì báo số món bị bỏ qua, rồi điều hướng `/shopping-list/import?weekStart=…` (US5-1, US5-3) trong src/frontend/src/features/meal-planner/__tests__/CreateShoppingListButton.test.tsx

### Implementation for User Story 5

- [ ] T079 [US5] Cài `ShoppingHandoffService` và `MealPlanShoppingFacade` (interface trong `api/`, cài đặt dùng service) theo contracts/integration.md mục 2.1 trong src/backend/src/main/java/com/wikicook/mealplan/service/ShoppingHandoffService.java và api/MealPlanShoppingFacade.java (làm T076 xanh) ⛓ D
- [ ] T080 [US5] Thêm endpoint `GET …/shopping-handoff` và DTO `ShoppingHandoffDto` trong src/backend/src/main/java/com/wikicook/mealplan/controller/MealPlanController.java và dto/ShoppingHandoffDto.java (làm T077 xanh)
- [ ] T081 [US5] Cài `CreateShoppingListButton` và gắn vào `WeekHeader` trong src/frontend/src/features/meal-planner/components/CreateShoppingListButton.tsx (làm T078 xanh) ⛓ D (chốt route `/shopping-list/import`)

**Checkpoint**: US5 chạy độc lập (với 040 giả lập bằng route placeholder nếu D chưa xong).

---

## Phase 8: User Story 6 - Recipes that are no longer available (Priority: P2)

**Goal**: Ô có công thức bị ARCHIVED/xóa giữ tên, nhãn "Không còn khả dụng", có Thay/Xóa, không mở trang chi tiết.

**Independent Test**: Thêm một công thức, archive nó bằng tài khoản admin, mở lại planner → ô giữ tên, hiện "Không còn khả dụng", có "Thay" (quickstart Q15).

### Tests for User Story 6 ⚠️ (viết trước, phải FAIL)

- [ ] T082 [P] [US6] Bổ sung `MealPlanServiceTest` – công thức `status = ARCHIVED` → `PlanItemDto.available = false`, `recipeName` = snapshot, `coverImageUrl = null` (US6-1, FR-020); `recipe_id = NULL` (công thức bị xóa) cũng `available = false`; item không khả dụng không tính vào `availableItemCount` trong src/backend/src/test/java/com/wikicook/mealplan/service/MealPlanServiceTest.java
- [ ] T083 [P] [US6] Integration test `UnavailableRecipeIT` – archive công thức qua SQL rồi `GET` tuần → `available = false`; `PUT` công thức ARCHIVED → 422 `RECIPE_NOT_AVAILABLE` (US6-3) trong src/backend/src/test/java/com/wikicook/mealplan/controller/UnavailableRecipeIT.java
- [ ] T084 [P] [US6] Test `SlotCard.unavailable.test.tsx` – hiện tên + nhãn "Không còn khả dụng" + "Thay" + "Xóa" (US6-1); bấm tên không điều hướng, hiện thông báo công thức đã bị gỡ (US6-2) trong src/frontend/src/features/meal-planner/__tests__/SlotCard.unavailable.test.tsx

### Implementation for User Story 6

- [ ] T085 [US6] Tính `available` trong `MealPlanService` khi ghép dữ liệu ô (`recipeId == null` hoặc `RecipeSummary.status != PUBLISHED` → không khả dụng, dùng `recipeNameSnapshot`, bỏ ảnh/link) và `availableItemCount` trong src/backend/src/main/java/com/wikicook/mealplan/service/MealPlanService.java (làm T082, T083 xanh)
- [ ] T086 [US6] Thêm biến thể không khả dụng cho `SlotCard` (nhãn, không link, thông báo khi bấm tên, menu chỉ còn Thay/Xóa) trong src/frontend/src/features/meal-planner/components/SlotCard.tsx (làm T084 xanh)

**Checkpoint**: US6 chạy độc lập; quickstart Q15 đạt.

---

## Phase 9: User Story 7 - Use the planner on a phone and handle a lost connection (Priority: P3)

**Goal**: Xem từng ngày trên màn hình hẹp, "Chuyển sang…" thay kéo-thả; mất mạng thì thay lịch bằng thông báo offline và tự tải lại khi có mạng.

**Independent Test**: Màn hình 360 px → xem từng ngày; tắt mạng → thông báo offline + "Thử lại"; bật mạng → lịch tự tải lại (quickstart Q17, Q18).

### Tests for User Story 7 ⚠️ (viết trước, phải FAIL)

- [ ] T087 [P] [US7] Test `DayView.test.tsx` – hiện một ngày, bắt đầu từ hôm nay, nút ngày trước/sau (US7-1, FR-026); không có kéo-thả, menu "Chuyển sang…" có mặt (US7-1) trong src/frontend/src/features/meal-planner/__tests__/DayView.test.tsx
- [ ] T088 [P] [US7] Test `useOnlineStatus.test.ts` – theo `navigator.onLine`; sự kiện `offline`/`online`; `reportNetworkError()` chuyển sang offline trong src/frontend/src/features/meal-planner/__tests__/useOnlineStatus.test.ts
- [ ] T089 [P] [US7] Test `MealPlannerPage.offline.test.tsx` – mở trang khi offline → không có lịch, có `OfflineNotice` + "Thử lại" (US7-2); mất mạng khi đang mở → lịch bị thay, không sửa được (US7-3); lưu thất bại do mất mạng → báo chưa lưu rồi hiện offline (US7-4); sự kiện `online` hoặc "Thử lại" khi đã có mạng → gọi lại `getWeek` và hiện lịch (US7-5); không ghi gì vào `localStorage`/`sessionStorage` (FR-027) trong src/frontend/src/features/meal-planner/__tests__/MealPlannerPage.offline.test.tsx
- [ ] T090 [P] [US7] Test `useIsDesktop.test.ts` – `matchMedia('(min-width: 1024px)')` đổi giá trị khi resize trong src/frontend/src/features/meal-planner/__tests__/useIsDesktop.test.ts

### Implementation for User Story 7

- [ ] T091 [P] [US7] Cài `useIsDesktop` trong src/frontend/src/features/meal-planner/hooks/useIsDesktop.ts (làm T090 xanh)
- [ ] T092 [P] [US7] Cài `useOnlineStatus` trong src/frontend/src/features/meal-planner/hooks/useOnlineStatus.ts (làm T088 xanh)
- [ ] T093 [US7] Cài `DayView` (một ngày, hôm nay trước, điều hướng ngày, dùng `SlotCard` + `SlotMenu`, vùng chạm ≥ 44 px) trong src/frontend/src/features/meal-planner/components/DayView.tsx (làm T087 xanh)
- [ ] T094 [US7] Cài `OfflineNotice` và nối vào `MealPlannerPage`: chọn `WeekGrid` (desktop, `enableDrag`) hoặc `DayView` theo `useIsDesktop`; offline → thay lịch bằng `OfflineNotice`; `useWeekPlan` báo lỗi mạng qua `reportNetworkError`; khi online → tải lại tuần trong src/frontend/src/features/meal-planner/components/OfflineNotice.tsx và pages/MealPlannerPage.tsx (làm T089 xanh)

**Checkpoint**: Mọi user story chạy độc lập; quickstart Q17, Q18 đạt.

---

## Phase 10: Integration (090, 031)

**Purpose**: Điểm nối với dinh dưỡng (FR-032) và AI planner (FR-030, contracts/integration.md mục 2.2, 3)

- [ ] T095 [P] Bổ sung `MealPlanServiceTest` – `getWeek` gọi `DayNutritionPort.totals` đúng **một lần** với recipe id khả dụng theo từng ngày; ngày port không trả → `nutrition = null`; servings của món không đổi tổng (090 Assumptions) trong src/backend/src/test/java/com/wikicook/mealplan/service/MealPlanServiceTest.java
- [ ] T096 [P] Test `DayNutritionBadge.test.tsx` – hiện "~{kcal} kcal/người", disclaimer "Số liệu dinh dưỡng do AI ước tính, chỉ mang tính tham khảo", "{n} món chưa có số liệu" khi `missingDishes > 0`; `nutrition = null` mà ngày có món → "Chưa có số liệu dinh dưỡng"; trên `DayView` badge nằm ở tiêu đề ngày (FR-032) trong src/frontend/src/features/meal-planner/__tests__/DayNutritionBadge.test.tsx
- [ ] T097 Gắn `DayNutritionPort` vào `MealPlanService.getWeek` (`DayPlanDto.nutrition`) trong src/backend/src/main/java/com/wikicook/mealplan/service/MealPlanService.java (làm T095 xanh) ⛓ 090
- [ ] T098 Cài `DayNutritionBadge` và đặt ở tiêu đề cột (`WeekGrid`) và tiêu đề ngày (`DayView`) trong src/frontend/src/features/meal-planner/components/DayNutritionBadge.tsx (làm T096 xanh) ⛓ 090
- [ ] T099 [P] Integration test `MealPlanWriteFacadeIT` – `emptySlots` chỉ trả ô trống; `plannedRecipeIds` trả id công thức của mọi món trong tuần, không trùng, tuần chưa có kế hoạch → tập rỗng, không lẫn món của người dùng khác; `saveDraft` lưu nhiều item trong một transaction với `source` AI/AUTO, bỏ qua ô đã có món (`skippedOccupied`) và công thức không PUBLISHED (`skippedUnavailable`), servings null → `ServingsPolicy` trong src/backend/src/test/java/com/wikicook/mealplan/service/MealPlanWriteFacadeIT.java
- [ ] T100 Cài `MealPlanWriteFacade` theo contracts/integration.md mục 2.2 trong src/backend/src/main/java/com/wikicook/mealplan/api/MealPlanWriteFacade.java và service/MealPlanWriteService.java (làm T099 xanh)
- [ ] T101 [P] Test `WeekGrid.draft.test.tsx` – prop `draftItems` hiện món nháp kèm nhãn "AI"/"Auto" và ô "Chọn món"; move/thay/xóa trong chế độ nháp gọi `onDraftChange` thay vì API (031 FR-017) trong src/frontend/src/features/meal-planner/__tests__/WeekGrid.draft.test.tsx
- [ ] T102 Thêm prop `draftItems`, `onDraftChange` và nhãn nguồn AI/Auto cho `WeekGrid`, `DayView`, `SlotCard` trong src/frontend/src/features/meal-planner/components/ (làm T101 xanh)

**Checkpoint**: 090 và 031 có đủ điểm nối để tích hợp.

---

## Phase 11: Polish & Cross-Cutting Concerns

**Purpose**: Hiệu năng, accessibility, độ phủ, kiểm chứng và tài liệu

- [ ] T103 [P] Viết script k6 200 VU: `GET` tuần (p95 < 2 s) và `PUT`/`PATCH`/`POST moves` (p95 < 1 s) theo NFR-001 trong src/backend/perf/meal-planner.k6.js
- [ ] T104 [P] Kiểm tra accessibility thủ công (chỉ bàn phím, NVDA/VoiceOver đọc "ngày, bữa, món", tương phản, vùng chạm ở 360 px) và ghi kết quả vào docs/test/030-meal-planner-a11y.md (NFR-002, NFR-003)
- [ ] T105 [P] So sánh OpenAPI do springdoc sinh (`/v3/api-docs`) với specs/030-meal-planner/contracts/openapi.yaml, sửa chỗ lệch trong controller/DTO hoặc trong contract
- [ ] T106 Đảm bảo JaCoCo rule ≥ 70% line coverage cho `com.wikicook.mealplan.service` (thêm rule vào src/backend/pom.xml nếu skeleton chưa có) và `./mvnw verify` xanh
- [ ] T107 Chạy quickstart Q1–Q19 (specs/030-meal-planner/quickstart.md) và ghi kết quả vào docs/test/030-meal-planner-acceptance.md
- [ ] T108 [P] Lập ma trận truy vết acceptance scenario (US1-1 … US7-5) và edge case → test tương ứng trong docs/test/030-meal-planner-traceability.md (SC-008)
- [ ] T109 [P] Đồng bộ specs/030-meal-planner sang src/specs/030-meal-planner theo src/README.md mục 5

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: cần skeleton Build v0.5 – bắt đầu ngay khi skeleton có.
- **Foundational (Phase 2)**: sau Setup – CHẶN mọi user story. T014 cần bảng `users`, `recipes` (A, B) hoặc migration stub cho test.
- **US1 (Phase 3)**: sau Foundational. Là nền cho US2–US7 ở frontend (`MealPlannerPage`, `useWeekPlan`, `WeekGrid`, `SlotCard`).
- **US2, US3 (Phase 4, 5)**: sau US1; US2 và US3 độc lập với nhau (backend có thể song song; frontend US3 sửa `WeekGrid.tsx`/`useWeekPlan.ts` nên làm sau T046/T044).
- **US4, US5, US6 (Phase 6–8)**: sau US1; độc lập với nhau và với US2/US3.
- **US7 (Phase 9)**: sau US1 và US3 (`DayView` dùng `SlotMenu`/`MoveToMenu` của US3).
- **Integration (Phase 10)**: T097/T098 sau US1 (cần 090 cài `DayNutritionPort` để có số liệu thật); T100 sau US3 + US6; T102 sau US3.
- **Polish (Phase 11)**: sau các story muốn giao.

### Within Each User Story

- Test viết trước và FAIL → DTO/model → service → controller → hook → component → page.
- Không đánh dấu task xong khi test tương ứng chưa xanh.

### Phụ thuộc người khác (⛓) – không chặn nhờ bean mặc định

| Task | Người | Bean/adapter tạm | Khi bên kia xong |
|---|---|---|---|
| T014 | A, B | migration stub test `V100__test_stub_users_recipes.sql` | Xóa stub, dùng `V101`, `V202` thật |
| T021, T042 | A | `DietaryProfilePort`, `CollectionRecipePort` rỗng | A cài bean thật, bean mặc định tự tắt |
| T021, T041, T055 | B | `JdbcRecipeSummaryAdapter`; index trigram đề xuất | B cài `RecipeSummaryPort`, nhúng `AddToPlanButton`, thêm index |
| T079, T081 | D | route placeholder `/shopping-list/import` | D cài flow import gọi `MealPlanShoppingFacade` |
| T097, T098 | 090 | `DayNutritionPort` trả rỗng → "Chưa có số liệu dinh dưỡng" | Module `nutrition` cài port |

### Parallel Opportunities

- Setup: T002–T006 song song.
- Foundational: các test T007–T013 song song; T015, T016, T019, T020, T022, T025, T027, T028 song song.
- Mỗi story: mọi task test [P] song song; backend và frontend của cùng story song song.
- Sau US1: US2 (backend), US4, US5, US6 có thể giao cho người khác nhau cùng lúc.

---

## Parallel Example: User Story 1

```bash
# Viết toàn bộ test US1 cùng lúc (phải FAIL):
Task: "T029 ServingsPolicyTest in src/backend/src/test/java/com/wikicook/mealplan/service/ServingsPolicyTest.java"
Task: "T030 MealPlanServiceTest in src/backend/src/test/java/com/wikicook/mealplan/service/MealPlanServiceTest.java"
Task: "T031 RecipePickerQueryRepositoryIT in src/backend/src/test/java/com/wikicook/mealplan/repository/RecipePickerQueryRepositoryIT.java"
Task: "T034 useWeekPlan.test.ts in src/frontend/src/features/meal-planner/__tests__/useWeekPlan.test.ts"
Task: "T036 RecipePicker.test.tsx in src/frontend/src/features/meal-planner/__tests__/RecipePicker.test.tsx"

# Sau đó code song song backend và frontend:
Task: "T038 DTO records in src/backend/src/main/java/com/wikicook/mealplan/dto/"
Task: "T039 ServingsPolicy in src/backend/src/main/java/com/wikicook/mealplan/service/ServingsPolicy.java"
Task: "T045 SlotCard, EmptySlot, ServingsInput in src/frontend/src/features/meal-planner/components/"
```

---

## Implementation Strategy

### MVP First (Setup + Foundational + US1 + US2 + US3)

1. Phase 1 Setup → Phase 2 Foundational (CHẶN mọi story).
2. Phase 3 US1 → **dừng và kiểm chứng** quickstart Q1–Q5.
3. Phase 4 US2 → kiểm chứng Q6, Q7.
4. Phase 5 US3 → kiểm chứng Q8–Q12. **MVP xong**: lập kế hoạch tuần đầy đủ bằng tay và là nền
   cho spec 031 (031 dựa vào thao tác sửa của US3).

### Incremental Delivery

1. MVP → demo nội bộ.
2. US4 (chuyển tuần) + US6 (công thức không khả dụng) → demo.
3. US5 (shopping list) khi D có flow import.
4. US7 (mobile, offline) → demo trên điện thoại.
5. Phase 10 khi 090/031 sẵn sàng; Phase 11 trước Build v0.8.

### Parallel Team Strategy

1. C làm Setup + Foundational (có thể nhờ A/B review migration và port).
2. Sau US1: một người làm US3 (DnD, move/swap), một người làm US4 + US6, US5 chờ D.
3. US7 sau US3.

---

## Notes

- [P] = file khác nhau, không phụ thuộc task chưa xong.
- Mã scenario (US1-3…) trong mô tả test dùng cho ma trận truy vết T108.
- Commit theo Conventional Commits kèm mã Jira, ví dụ `feat(mealplan): add move/swap API (SCRUM-xx)`.
- Dừng ở mỗi Checkpoint để kiểm chứng story độc lập.
