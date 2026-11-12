---

description: "Task list for 011 User Profile & Dietary Profile"
---

# Tasks: User Profile & Dietary Profile

**Input**: Design documents from `specs/011-user-profile-dietary/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/openapi.yaml](contracts/openapi.yaml),
[contracts/integration.md](contracts/integration.md), [quickstart.md](quickstart.md)

**Tests**: BẮT BUỘC (constitution Principle II, spec SC-008). Trong mỗi phase, task test đứng
trước task code và **phải chạy FAIL trước** khi viết code (Red → Green → Refactor). Mỗi task test
ghi mã acceptance scenario (`US1-3`…) hoặc edge case (`Edge: …`) mà nó phủ. Bảng phủ ở cuối file.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- **⛓ A / B / C / D / E**: task chạm điểm nối với người khác (A: 010, B: 060, C: 030/031, D: 040,
  E: 070/100). Cách làm tạm ghi ngay trong task và ở bảng "Phụ thuộc người khác".
- **🕓 #n**: chờ chốt, n là dòng trong bảng cuối [research.md](research.md). Task làm theo quyết định
  đang ghi ở dòng đó, nhưng **không đóng** cho tới khi dòng đó được duyệt; kết quả khác thì sửa task
  theo bảng, không tự giả định.

## Path Conventions

- Đường dẫn trong task là đường dẫn thật. Chạy test backend: `cd src/backend && mvn verify` (chưa có
  Maven wrapper).
- Đã có, không tạo task: `V010` (`allergens`), `V101` (`users`), `V210` (ba bảng `user_*`) và
  `com.wikicook.common.diet`. FR-021 dùng `DietRuleSql` như đang có; 011 chỉ thêm factory
  `DietRestrictions.hardRules` (T107b).
- `userId` lấy qua `com.wikicook.common.security.CurrentUser`. Test dùng `HeaderCurrentUser` +
  `X-Test-User`, kế thừa `com.wikicook.support.AbstractPostgresIT`, dùng `RecipeFixtures` có sẵn.
- 010 chưa có code. **Không tạo `com.wikicook.auth.api` giả trong nhánh 011** (research R19).
- Giao diện: class `wk-`, màn `PF01`–`PF08` trong `docs/analysis-and-design/ui-ux/screens/`; Tailwind
  chỉ dùng cho bố cục.

## Mã Jira (Sprint 04, `pa4-guide.md`)

| Mã | Phạm vi | Task | Người làm; người duyệt; hạn |
|---|---|---|---|
| SCRUM-72 | Tài liệu Spec Kit 010 + 011 | spec, plan, research, data-model, contracts, quickstart, tasks; T001 | Hiển; Hưng; 13/11 |
| SCRUM-75 | Backend 011 | Mọi task sửa `src/backend/**`: T002–T005, T007–T013, T016–T024, T027–T034a, T039–T047, phần backend của Phase 4–8, T134 | Hiển; Hưng; 18/11 |
| SCRUM-76 | Frontend 010 + 011 | Mọi task sửa `src/frontend/**`: T006, T014, T015, T025, T026, T035–T038, T048–T051, phần frontend của Phase 4–8, T131–T133 | Hưng; Hiển; 18/11 |

- Task tài liệu và kiểm chứng (T108, T130, T130a, T130b, T135–T140) ghi mã của phần chúng kiểm; T135 (thử với người dùng) chưa có mã trong Sprint 04.
- **Đề xuất phạm vi cho PA4, cần nhóm chốt**: SCRUM-75 yêu cầu hồ sơ và hồ sơ ăn uống lưu được qua API, có kiểm tra quyền và riêng tư, tương ứng Phase 1–4 và Phase 7 (US1, US2, US5), cộng T112 khi 010 Phase 2 đã merge. US3, US4 (Phase 5, 6) cần Phase 8 của 010, nên để sau 18/11.
- Commit theo Conventional Commits kèm mã, ví dụ `feat(profile): add dietary profile API (SCRUM-75)`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Điều kiện bắt đầu, dependency, cấu hình

- [ ] T001 🕓 #1 Xác nhận điều kiện của Nguyên tắc I trước khi code: mã Jira đã có (mục "Mã Jira"); PR SCRUM-72 được duyệt (`pa4-guide.md` ghi người duyệt là Hưng, các quyết định đang chờ Duy: mời cả hai, trừ khi nhóm chốt khác), Status của specs/011-user-profile-dietary/spec.md đổi khỏi "In Review"; tạo branch `feature/SCRUM-75-profile-backend` và dùng chung `feature/SCRUM-76-auth-profile-frontend` với 010. **Chặn mọi task còn lại**; ngoại lệ ghi ở plan, Complexity Tracking
- [ ] T002 [P] Thêm property version `3.15.3` và `com.twelvemonkeys.imageio:imageio-webp` ở `dependencyManagement` của src/backend/pom.xml, dependency vào src/backend/wikicook-app/pom.xml (plan, Complexity Tracking)
- [ ] T003 Thêm rule PACKAGE cho `com.wikicook.profile.service`, LINE và BRANCH ≥ 70%, cùng dạng rule của `com.wikicook.mealplan.service` trong src/backend/wikicook-app/pom.xml
- [ ] T004 ⛓ A Sửa src/backend/wikicook-app/src/main/resources/application.yml: `spring.servlet.multipart.max-file-size: 5MB`, `max-request-size: 6MB`, `resolve-lazily: true`, `server.tomcat.max-swallow-size: 6MB`, `wikicook.profile.media-dir: ${WIKICOOK_MEDIA_DIR:./.media}`, `wikicook.profile.write-limit-per-hour: 20`; thêm `spring.flyway.out-of-order: true` nếu T007 của 010 chưa merge (research R2, R7). Bên merge sau bỏ qua dòng này
- [ ] T005 [P] ⛓ A Sửa src/backend/docker-compose.yml: volume `media:/var/lib/wikicook/media` cho `app`, `WIKICOOK_MEDIA_DIR`, `FLYWAY_OUT_OF_ORDER: "true"` cho `migrate` (bỏ qua nếu 010 đã thêm); thêm `WIKICOOK_MEDIA_DIR` vào src/backend/.env.example; tạo và `chown` `/var/lib/wikicook/media` trước `USER wikicook` trong src/backend/Dockerfile
- [ ] T006 [P] Thêm `client_max_body_size 6m` trong `location /api/` của src/frontend/nginx.conf (research R7)
- [ ] T007 Spike ảnh trên `eclipse-temurin:21-jre-alpine`: JPEG CMYK, PNG alpha, WebP lossy, lossless, animated; ghi kết quả vào mục R7 của specs/011-user-profile-dietary/research.md, mẫu lưu ở src/backend/wikicook-app/src/test/resources/avatars/. Định dạng nào không đọc được thì báo Hiển trước khi làm tiếp, không tự bỏ

**Checkpoint**: `mvn verify`, `npm test` vẫn xanh như trước; ImageIO đã được kiểm chứng trên image thật.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema, entity, lỗi, giới hạn tần suất, nơi lưu file, điểm nối mặc định. CHẶN mọi user story.

### Tests cho Foundational (viết trước, phải FAIL)

- [ ] T008 [P] Integration test `V211MigrationIT` – `users.bio` `VARCHAR(300)` NULL; `profile_version`, `dietary_version` `INTEGER` NOT NULL mặc định 0; `max_cook_time_minutes` nhận 10, 240, từ chối 9, 241; `dietary_notices`, `profile_write_requests` đúng cột và `CHECK`; `DELETE users` dọn hai bảng mới theo `CASCADE`; đủ 10 mã dị ứng `GLUTEN`, `PEANUT`, `TREE_NUT`, `MILK`, `EGG`, `FISH`, `CRUSTACEAN`, `MOLLUSC`, `SOY`, `SESAME` trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/repository/V211MigrationIT.java
- [ ] T009 [P] Integration test `UserProfileMappingIT` – `ddl-auto=validate` chạy được với hai entity trên `users`; lưu `UserProfile` không đổi `email`, `last_sign_in_at` (`@DynamicUpdate`, research R4) trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/repository/UserProfileMappingIT.java
- [ ] T010 [P] Unit test `ProfileExceptionHandlerTest` – mọi `code` ở research R17, trừ `PROFILE_UNAVAILABLE` (T034a), đúng HTTP; `errors: [{ field, code }]`; `InvalidFormatException` của `householdSize: 2.5` vẫn có `field`; không lộ stack trace trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/ProfileExceptionHandlerTest.java
- [ ] T011 [P] Unit test `PlainTextLineValidatorTest` – `Edge: Display name`: từ chối ký tự điều khiển, chỉ khoảng trắng; đếm theo code point, emoji tính một trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/validation/PlainTextLineValidatorTest.java
- [ ] T012 [P] Integration test `ProfileWriteLimiterIT` – NFR-008: lần 21 trong một giờ → 429 `PROFILE_RATE_LIMITED` có `retryAfterSeconds`; `PROFILE_SAVE`, `AVATAR_UPLOAD` đếm riêng; gửi song song không vượt 20; dòng quá 1 giờ bị xóa khi đếm; transaction ngoài rollback vẫn tốn lượt trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/service/ProfileWriteLimiterIT.java
- [ ] T013 [P] Unit test `LocalDiskMediaStorageTest` – ghi file tạm rồi `ATOMIC_MOVE`, `load`, `delete`, khóa thoát khỏi thư mục gốc bị từ chối trong src/backend/wikicook-app/src/test/java/com/wikicook/common/media/LocalDiskMediaStorageTest.java
- [ ] T014 [P] Test `client.test.ts` – `api.postForm` gửi `FormData`, không tự đặt `Content-Type` trong src/frontend/src/shared/api/client.test.ts
- [ ] T015 [P] Test `codePoints.test.ts` – đếm emoji là một ký tự, khớp `@CodePointLength` trong src/frontend/src/features/profile/__tests__/codePoints.test.ts

### Implementation cho Foundational

- [ ] T016 Viết src/backend/wikicook-db/src/main/resources/db/migration/V211__profile_schema_updates.sql theo data-model.md: `users.bio VARCHAR(300)` NULL; `profile_version`, `dietary_version` `INTEGER NOT NULL DEFAULT 0`; bỏ `users_max_cook_time_minutes_check`, thêm `chk_users_max_cook_time CHECK BETWEEN 10 AND 240`; `dietary_notices`, kind `CHECK IN ('INGREDIENT_MERGED','INGREDIENT_REMOVED','ALLERGEN_REMOVED')`, `subject_name VARCHAR(100)` NOT NULL, `replacement_name VARCHAR(100)` NULL, `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`, index `(user_id, created_at DESC)`; `profile_write_requests`, kind `CHECK IN ('PROFILE_SAVE','AVATAR_UPLOAD')`, `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`, index `(user_id, kind, created_at DESC)`; seed 10 dị ứng `ON CONFLICT (code) DO NOTHING` (làm T008 xanh)
- [ ] T017 Tạo entity `UserProfile` (`@Table(name = "users")`, `@DynamicUpdate`, chỉ cột 011 theo data-model.md), `DietaryNotice`, enum `NoticeKind`, `ProfileWriteKind` trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/domain/ (dùng `DietType` có sẵn; làm T009 xanh)
- [ ] T018 Tạo `UserProfileRepository` (kèm projection tài khoản mức 2, không có hash), `DietaryNoticeRepository`, `ProfileWriteRequestRepository` (JDBC, không có entity: `pg_advisory_xact_lock(hashtextextended('profile-write:' || :userId, 0))`, xóa dòng quá 1 giờ, đếm, chèn) trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/repository/
- [ ] T019 Tạo `ProfileException` (`status`, `code`), `ProfileExceptionHandler` (`@RestControllerAdvice(basePackages = "com.wikicook.profile")`, `@Order(HIGHEST_PRECEDENCE)`, như `MealPlanExceptionHandler`), xử lý `MaxUploadSizeExceededException` → 413 `AVATAR_TOO_LARGE`, cần `resolve-lazily` ở T004 để lỗi phát sinh sau khi đã chọn handler, trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/controller/ProfileExceptionHandler.java (làm T010 xanh)
- [ ] T020 [P] Tạo `@PlainTextLine`, `PlainTextLineValidator` trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/validation/ (làm T011 xanh)
- [ ] T021 [P] Tạo `ProfileProperties` (`wikicook.profile.*`) trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/config/ProfileProperties.java
- [ ] T022 Tạo `ProfileWriteLimiter` (`REQUIRES_NEW`, chỉ điều phối các lệnh của `ProfileWriteRequestRepository`, không chứa SQL) trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/service/ProfileWriteLimiter.java (làm T012 xanh)
- [ ] T023 [P] Tạo `MediaStorage`, `LocalDiskMediaStorage` trong src/backend/wikicook-app/src/main/java/com/wikicook/common/media/ (làm T013 xanh)
- [ ] T024 ⛓ A Tạo `ProfileAuditPort`, `ProfileDefaultsConfig` (auto-configuration, bean `@ConditionalOnMissingBean`, audit mặc định không làm gì) trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/port/ProfileAuditPort.java và src/backend/wikicook-app/src/main/java/com/wikicook/profile/config/ProfileDefaultsConfig.java; đăng ký ở src/backend/wikicook-app/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports. Adapter thật ở T116
- [ ] T025 [P] ⛓ A Thêm `api.postForm` trong src/frontend/src/shared/api/client.ts, đi qua cùng `fetchJson` để có bearer và refresh của 010 (T051 của 010 cũng sửa file này: bên merge sau rebase) (làm T014 xanh)
- [ ] T026 [P] Tạo `strings.vi.ts`, `types.ts`, `lib/codePoints.ts`, `lib/avatarRules.ts` (JPEG, PNG, WebP, 5 MiB) trong src/frontend/src/features/profile/ (làm T015 xanh)

**Checkpoint**: Nền tảng xanh; các story bắt đầu được.

---

## Phase 3: User Story 1 - Set up my dietary profile (Priority: P1) 🎯 MVP

**Goal**: Người dùng lưu dị ứng, nguyên liệu loại trừ, kiểu ăn, ẩm thực, khẩu phần, thời gian nấu; 030, 031, 040 đọc được qua port.

**Independent Test**: Đăng nhập Member, chọn dị ứng "Đậu phộng", gõ "ngo" chọn "Ngò rí", chọn Vegetarian, 2 ẩm thực, khẩu phần 4, thời gian 45; lưu, tải lại, mọi giá trị đúng. Quickstart Q1–Q9.

### Tests for User Story 1 ⚠️ (viết trước, phải FAIL)

- [ ] T027 [P] [US1] Unit test `DietaryProfileServiceTest` – `Edge: Invalid data`: id dị ứng lạ → `UNKNOWN_ALLERGEN`; id nguyên liệu mới phải `APPROVED`, id đã lưu được giữ; id ẩm thực mới phải `is_active`; nội dung giống hệt → không ghi trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/service/DietaryProfileServiceTest.java
- [ ] T028 [P] [US1] Integration test `DietaryProfileControllerIT` – US1-1 (`empty = true`, 200), US1-2 (chỉ từ danh sách cố định), US1-4 (một kiểu ăn trong `NONE`, `VEGETARIAN`, `VEGAN`, `PESCATARIAN`, nhiều ẩm thực), US1-5 (0, 21, 2.5, 5, 241 → 400 có `errors[].field`; 1, 20, 10, 240, trống → 200), US1-6, US1-7, `Edge: Invalid data`, `Edge: 51st ingredient` (`EXCLUDED_LIMIT_EXCEEDED`) trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/DietaryProfileControllerIT.java
- [ ] T029 [P] [US1] Integration test `DietaryProfileAtomicIT` – FR-017, NFR-007: lỗi ở bảng `user_*` thứ ba rollback cả cột `users` và hai bảng trước; dòng không đổi giữ `created_at` trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/service/DietaryProfileAtomicIT.java
- [ ] T030 [P] [US1] Integration test `ProfileConflictIT` – `Edge: Two tabs`: `version` cũ, nội dung khác → 409 `PROFILE_CHANGED`, đọc `version` mới rồi gửi lại thì thắng cả form; `Edge: Repeated clicks`: gửi hai lần giống hệt → 200, `dietary_version` tăng một lần, một dòng audit (FR-041) trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/ProfileConflictIT.java
- [ ] T031 [P] [US1] Integration test `DietaryOptionsIT` – FR-009, FR-013: `GET /me/dietary-options` trả 10 dị ứng, 4 kiểu ăn, chỉ ẩm thực `is_active` trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/DietaryOptionsIT.java
- [ ] T032 [P] [US1] Integration test `IngredientSuggestionIT` – US1-3: "ngo", "Ngò", chữ hoa cho cùng kết quả; khớp đúng, đầu tên, đầu từ lên trước; chỉ `APPROVED`; `q` dưới 2 ký tự rỗng, không gọi DB; `%`, `_`, `\` được escape; tối đa 10 trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/IngredientSuggestionIT.java
- [ ] T033 [P] [US1] Integration test `IngredientSuggestionThirtyNamesIT` – **SC-007**: bộ 30 tên tiếng Việt, gồm tên có "đ", gõ không dấu, tên đúng nằm trong 5 kết quả đầu ở 100% ca; danh sách ở src/backend/wikicook-app/src/test/resources/ingredients/sc007-names.csv, test trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/IngredientSuggestionThirtyNamesIT.java
- [ ] T034 [P] [US1] ⛓ C, D Integration test `DietaryPortsIT` – US1-8, FR-023, FR-026, FR-026a: `findByUserId` trả đúng 6 trường, `householdSize` là `null` khi trống; không có dòng `users` → `PlannerProfile.empty()`; `getRestrictions` trả id **nguyên liệu** suy từ `ingredient_allergens`; lỗi DB ném `DietaryProfileUnavailableException`, không trả hồ sơ trống; bean thật thắng `DevStubConfig`, `DefaultPortsConfig` trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/port/DietaryPortsIT.java
- [ ] T034a [P] [US1] ⛓ C Integration test `ProfileUnavailableIT` – FR-026a: khi `DietaryProfileReader` ném lỗi, `GET /me/dietary-profile`, `GET /meal-plans/picker/recipes`, `GET /ai-planner/weeks/{weekStart}/options` đều trả 503 `PROFILE_UNAVAILABLE`, không lộ chi tiết, không trả hồ sơ trống trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/port/ProfileUnavailableIT.java
- [ ] T035 [P] [US1] Test `DietaryProfilePage.test.tsx` – US1-1, US1-6, US1-7; FR-039 (skeleton, trống, lỗi, dữ liệu); `Edge: Network loss`: "Thử lại", giữ dữ liệu đã gõ; `Edge: Two tabs`: 409 hiện "Tải lại", "Lưu đè"; `Edge: Unsaved changes`; `Edge: Repeated clicks`: nút tắt khi `inFlight` trong src/frontend/src/features/profile/__tests__/DietaryProfilePage.test.tsx
- [ ] T036 [P] [US1] ⛓ A Test `DietaryProfilePage.session.test.tsx` – `Edge: Session expired`: render trong `AppLayout`, 401 sau refresh (`useAuth().status = expired`) hiện `SessionExpiredDialog` của 010 (một dialog, trang không tự gắn), trang không chuyển `/login`, form giữ nguyên, không lưu vào tài khoản khác. Tạm: mock `useAuth` và dialog tới khi có T053, T077 của 010, trong src/frontend/src/features/profile/__tests__/DietaryProfilePage.session.test.tsx
- [ ] T037 [P] [US1] Test `ExcludedIngredientPicker.test.tsx` – US1-3; debounce 150 ms, `AbortController` hủy request cũ; chỉ thêm được mục trong catalogue; `Edge: 51st ingredient`; bàn phím (NFR-006) trong src/frontend/src/features/profile/__tests__/ExcludedIngredientPicker.test.tsx
- [ ] T038 [P] [US1] Test `DietaryForm.test.tsx` – US1-2, US1-4 (không có Keto, không hard-code ẩm thực), US1-5: lỗi cạnh trường; FR-010a: luôn có ghi chú giới hạn dữ liệu dị ứng, không có câu nào nói món an toàn trong src/frontend/src/features/profile/__tests__/DietaryForm.test.tsx

### Implementation for User Story 1

- [ ] T039 [P] [US1] Tạo record `DietaryProfileDto`, `DietaryProfileUpdateRequest` (`@Size(max = 50)`, `@Min(1) @Max(20)`, `@Min(10) @Max(240)`), `DietaryOptionsDto`, `AllergenDto`, `CuisineDto`, `IngredientSuggestionDto`, `DietaryNoticeDto` trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/dto/
- [ ] T040 [US1] ⛓ B, E Tạo `IngredientCatalogPort`, `CuisineCatalogPort` và stand-in `JdbcIngredientCatalog`, `JdbcCuisineCatalog` theo query ở research R6, khai báo trong `ProfileDefaultsConfig`, trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/port/ (bean thật của 060, 100 tự thay)
- [ ] T041 [US1] Tạo `DietaryLinkRepository` (`NamedParameterJdbcTemplate`, `DELETE … WHERE id NOT IN (:ids)`, `INSERT … ON CONFLICT DO NOTHING`, `findSnapshot(userId)`: một query gom `users` và `ARRAY(SELECT …)`) trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/repository/DietaryLinkRepository.java
- [ ] T042 [US1] Tạo `DietaryProfileUnavailableException` (unchecked) trong src/backend/wikicook-app/src/main/java/com/wikicook/common/diet/DietaryProfileUnavailableException.java; tạo `DietaryProfileReader` (gọi `DietaryLinkRepository.findSnapshot`, lỗi đọc ném exception này) trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/service/DietaryProfileReader.java
- [ ] T042a [US1] ⛓ C Thêm `@ExceptionHandler(DietaryProfileUnavailableException.class)` → 503 `PROFILE_UNAVAILABLE`, chi tiết cố định, trong src/backend/wikicook-app/src/main/java/com/wikicook/common/GlobalExceptionHandler.java; thêm ca này vào src/backend/wikicook-app/src/test/java/com/wikicook/common/GlobalExceptionHandlerTest.java (cùng T046 làm T034a xanh)
- [ ] T043 [US1] Cài `DietaryProfileService` theo 6 bước research R5; `UPDATE … WHERE dietary_version = :version`; audit `DIETARY_PROFILE_UPDATED` **không có `details`** trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/service/DietaryProfileService.java (làm T027, T029, T030 xanh)
- [ ] T044 [P] [US1] Cài `IngredientSuggestionService` trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/service/IngredientSuggestionService.java (làm T032, T033 xanh)
- [ ] T045 [US1] Tạo `DietaryProfileController` (`GET`, `PUT /me/dietary-profile`, `GET /me/dietary-options`), `IngredientSuggestionController`, gọi `ProfileWriteLimiter` với `PROFILE_SAVE` trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/controller/ (làm T028, T031 xanh)
- [ ] T046 [US1] ⛓ C, D Tạo `MealPlanDietaryAdapter` cài `com.wikicook.mealplan.port.DietaryProfilePort`, `ShoppingDietaryAdapter` cài `com.wikicook.shoppinglist.port.DietaryProfilePort`, là `@Component`, trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/port/ (làm T034 xanh; chữ ký port giữ nguyên)
- [ ] T047 [US1] ⛓ C Xóa `StubDietaryProfile`, `DevStubProperties.Profile`, bean ở src/backend/wikicook-app/src/main/java/com/wikicook/devstub/DevStubConfig.java; xóa `emptyDietaryProfile` ở src/backend/wikicook-app/src/main/java/com/wikicook/mealplan/port/DefaultPortsConfig.java; sửa `MealPlanWriteFacadeIT` (`@Primary`), `DevStubConfigTest`, `DefaultPortsConfigTest` trong src/backend/wikicook-app/src/test/java/com/wikicook/. Phần còn lại của `devstub` đi theo 010
- [ ] T048 [P] [US1] Tạo `dietaryApi.ts` trong src/frontend/src/features/profile/api/dietaryApi.ts
- [ ] T049 [US1] Tạo `useDietaryProfile`, `useIngredientSuggestions` trong src/frontend/src/features/profile/hooks/
- [ ] T050 [US1] Tạo `AllergenPicker` (có ghi chú FR-010a), `ExcludedIngredientPicker`, `CuisinePicker`, `DietTypeSelect`, `NumberField`, `ProfileSkeleton`, `DietaryEmptyState` theo `PF02`, `PF03` trong src/frontend/src/features/profile/components/ (làm T037, T038 xanh)
- [ ] T051 [US1] Tạo `DietaryProfilePage` (`useUnsavedChangesBlocker()` của 010, tạm dùng `useBlocker` tới khi có T077 của 010; `beforeunload`; hộp xung đột) trong src/frontend/src/features/profile/pages/DietaryProfilePage.tsx và thêm route `profile/dietary` trong src/frontend/src/app/routes.tsx (làm T035, T036 xanh)

**Checkpoint**: Quickstart Q1–Q9 đạt; 030, 031 đọc hồ sơ thật. Chạy bằng dev stub, chưa triển khai công khai (research R19).

---

## Phase 4: User Story 2 - Edit my public profile and avatar (Priority: P1)

**Goal**: Tên hiển thị, bio, avatar; trang công khai cho khách, không lộ dữ liệu riêng tư.

**Independent Test**: Đổi tên, viết bio, tải PNG 2 MB, lưu; mở trang công khai khi đã đăng xuất, thấy tên, avatar, bio, công thức đã xuất bản, không thấy email và dữ liệu ăn uống. Quickstart Q10–Q15.

### Tests for User Story 2 ⚠️ (viết trước, phải FAIL)

- [ ] T052 [P] [US2] Unit test `MyProfileServiceTest` – `Edge: Display name` (strip, 1–50 code point); `Edge: Bio` (tối đa 300, giữ xuống dòng); nội dung không đổi, không có ảnh mới → không ghi; audit `PROFILE_UPDATED` chỉ có tên trường trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/service/MyProfileServiceTest.java
- [ ] T053 [P] [US2] Integration test `MyProfileControllerIT` – US2-1, US2-2 (trống, chỉ khoảng trắng, 51 ký tự → 400, không ghi), `Edge: Bio`; `email` chỉ đọc; `GET /me/profile` không có hash; `Edge: Two tabs` với `profile_version` trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/MyProfileControllerIT.java
- [ ] T054 [P] [US2] Unit test `AvatarImageProcessorTest` – `Edge: Avatar not an image`: magic bytes sai → 415 `AVATAR_TYPE_NOT_ALLOWED`; không giải mã được → 422 `AVATAR_NOT_AN_IMAGE`; quá 6000 × 6000 → 422 `AVATAR_DIMENSIONS_TOO_LARGE` khi chưa giải mã; EXIF GPS bị loại; xoay theo Orientation; cắt vuông 512 × 512; nền trắng cho alpha trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/service/AvatarImageProcessorTest.java
- [ ] T055 [P] [US2] Integration test `AvatarUploadIT` – US2-3, US2-5, **SC-003** phía server (100% file không phải ảnh gửi thẳng bị từ chối); `AVATAR_TOO_LARGE`; DB lỗi thì file mới bị dọn, file cũ còn; lưu thành công thì file cũ mất sau commit; `GET /media/avatars/{file}` có `nosniff`, `image/jpeg`, tên sai mẫu → 404 (NFR-004) trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/AvatarUploadIT.java
- [ ] T056 [P] [US2] Integration test `PublicProfileIT` – US2-6: khách đọc được, JSON chỉ có `id, displayName, avatarUrl, bio, publishedRecipeCount`, công thức phân trang, chỉ `PUBLISHED`; không có công thức → `items` rỗng; không tồn tại → 404 `USER_NOT_FOUND` trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/PublicProfileIT.java
- [ ] T057 [P] [US2] 🕓 #11 Integration test `PublicProfileStatusIT` – `Edge: Suspended or banned`: `SUSPENDED` xem được; `BANNED` trả 404 giống hệt không tồn tại trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/PublicProfileStatusIT.java
- [ ] T058 [P] [US2] Integration test `PrivateDataStructureIT` – US2-7 phần cấu trúc: mọi route `/me/**` theo người gọi, không có route nào nhận id người khác; `X-Test-User` thứ hai chỉ thấy dữ liệu của mình. Toàn bộ US2-7 ở T112, trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/PrivateDataStructureIT.java
- [ ] T059 [P] [US2] Test `EditProfilePage.test.tsx` – US2-1, US2-2, `Edge: Display name`, `Edge: Bio`, `Edge: Unsaved changes`, `Edge: Network loss`, `Edge: Repeated clicks`, 4 trạng thái (FR-039), Gap 011-D trong src/frontend/src/features/profile/__tests__/EditProfilePage.test.tsx
- [ ] T060 [P] [US2] Test `AvatarPicker.test.tsx` – US2-3 (xem trước bằng `FileReader`, URL `data:`, không dùng `blob:`), US2-4, **SC-003** phía form (sai loại, quá 5 MB không có request), US2-5, bàn phím trong src/frontend/src/features/profile/__tests__/AvatarPicker.test.tsx
- [ ] T061 [P] [US2] Test `PublicProfilePage.test.tsx` – US2-6, không có công thức hiện trạng thái rỗng, 404 chung, không có "tham gia" (FR-005), Gap 011-F trong src/frontend/src/features/profile/__tests__/PublicProfilePage.test.tsx
- [ ] T062 [P] [US2] Test `MyAccountPage.test.tsx` – FR-001, liên kết tới hồ sơ, hồ sơ ăn uống, bảo mật, xóa tài khoản theo `PF01` trong src/frontend/src/features/profile/__tests__/MyAccountPage.test.tsx

### Implementation for User Story 2

- [ ] T063 [P] [US2] Tạo record `MyProfileDto`, `AccountInfoDto`, `ProfileUpdateRequest` (`@NotBlank`, `@CodePointLength(max = 50)`, `@PlainTextLine`, bio `@CodePointLength(max = 300)`), `PublicProfileDto`, `PublicRecipeCardDto` trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/dto/
- [ ] T064 [US2] ⛓ B Tạo `PublicRecipesPort`, stand-in `JdbcPublicRecipes` (dùng `idx_recipes_author_status`), khai báo trong `ProfileDefaultsConfig`, trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/port/ (060 cài bean thật)
- [ ] T065 [US2] Cài `AvatarImageProcessor` theo 6 bước research R7, chạy ngoài transaction, trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/service/AvatarImageProcessor.java (làm T054 xanh)
- [ ] T066 [US2] Cài `MyProfileService`: `ProfileWriteLimiter` (`AVATAR_UPLOAD` khi có file, ngược lại `PROFILE_SAVE`) → xử lý ảnh → ghi file → transaction `UPDATE … WHERE profile_version = :version` → `afterCommit` xóa file cũ, `afterCompletion(ROLLED_BACK)` xóa file mới, trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/service/MyProfileService.java (làm T052 xanh)
- [ ] T067 [P] [US2] 🕓 #11 Cài `PublicProfileService` chỉ `SELECT` cột công khai; `BANNED` → `USER_NOT_FOUND` trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/service/PublicProfileService.java (làm T056, T057 xanh)
- [ ] T068 [US2] Tạo `MyProfileController` (`GET`, `POST /me/profile`, multipart), `PublicProfileController`, `AvatarMediaController` trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/controller/ (làm T053, T055, T058 xanh)
- [ ] T069 [P] [US2] Tạo `profileApi.ts` dùng `api.postForm` trong src/frontend/src/features/profile/api/profileApi.ts
- [ ] T070 [US2] Tạo `useMyProfile` trong src/frontend/src/features/profile/hooks/useMyProfile.ts
- [ ] T071 [US2] Tạo `AvatarPicker` trong src/frontend/src/features/profile/components/AvatarPicker.tsx (làm T060 xanh)
- [ ] T072 [US2] Tạo `MyAccountPage`, `EditProfilePage`, `PublicProfilePage` theo `PF01`, `PF04`, `PF05` trong src/frontend/src/features/profile/pages/ và thêm route `profile`, `profile/edit`, `users/:id` trong src/frontend/src/app/routes.tsx (làm T059, T061, T062 xanh)

**Checkpoint**: Quickstart Q10–Q15 đạt. US2-7, US2-8 đóng ở Phase 8.

---

## Phase 5: User Story 3 - Change or set my password (Priority: P2)

**Goal**: Đổi mật khẩu, đăng xuất phiên khác; tài khoản chỉ Google đặt được mật khẩu.

**Independent Test**: Đăng nhập hai trình duyệt, đổi mật khẩu ở một bên, bên kia bị đăng xuất; tài khoản chỉ Google thấy "Đặt mật khẩu". Quickstart Q18–Q20.

**Cần 010**: backend chờ Phase 8 của 010. Frontend làm trước trên `msw`.

### Tests for User Story 3 ⚠️ (viết trước, phải FAIL)

- [ ] T073 [P] [US3] ⛓ A Unit test `PasswordChangeServiceTest` (mock `auth.api`) – US3-2 (422 `INVALID_CURRENT_PASSWORD`, không `hash`); US3-3 (mật khẩu mới trùng sau NFC → 422 `PASSWORD_SAME_AS_CURRENT`, gọi `checkCurrent` đúng một lần); không giữ transaction khi chạy BCrypt trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/service/PasswordChangeServiceTest.java
- [ ] T074 [P] [US3] ⛓ A Integration test `PasswordChangeIT` – US3-1, US3-3 (quy tắc 010 → 400 theo trường), US3-5, US3-6 (thiếu xác thực lại → 403 `REAUTH_REQUIRED`), `Edge: Stale tab` (409 `PASSWORD_NOT_SET`, `PASSWORD_ALREADY_SET`), `Edge: Repeated clicks` (`UPDATE` so hash cũ, hai request đồng thời chỉ một thắng) trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/PasswordChangeIT.java
- [ ] T075 [P] [US3] ⛓ A 🕓 #12 Integration test `PasswordCheckLimitIT` – US3-2, FR-030: lần sai thứ 5 trong 15 phút vẫn trả 422 `INVALID_CURRENT_PASSWORD`, lần thử kế tiếp trả 429 `PASSWORD_CHECK_LIMITED` có `retryAfterSeconds`; đăng nhập không bị khóa trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/PasswordCheckLimitIT.java
- [ ] T076 [P] [US3] ⛓ A Integration test `PasswordSessionRevocationIT` – **SC-005**, US3-4, FR-028: hai phiên JWT thật, đổi mật khẩu ở phiên A, 100% phiên khác bị từ chối ở request kế tiếp, phiên A tiếp tục; đặt mật khẩu cũng vậy trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/PasswordSessionRevocationIT.java
- [ ] T077 [P] [US3] ⛓ A Integration test `PasswordAuditIT` – FR-042: `PASSWORD_CHANGED`, `PASSWORD_SET` không có `details`, không có mật khẩu trong log trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/PasswordAuditIT.java
- [ ] T078 [P] [US3] Test `SecurityPage.test.tsx` trên `msw` – US3-1, US3-2, US3-3, US3-4, US3-5, US3-6 (`?reauth=cancelled` hiện lý do), `Edge: Stale tab`, `Edge: Network loss` (ô mật khẩu bị xóa), `Edge: Repeated clicks`, Gap 011-G, H, I trong src/frontend/src/features/profile/__tests__/SecurityPage.test.tsx

### Implementation for User Story 3

- [ ] T079 [P] [US3] Tạo record `ChangePasswordRequest`, `SetPasswordRequest` (không trim, không log; mật khẩu hiện tại dùng `@MaxCodePoints(128)` của 010, không dùng `@Size`, 010 research R3) trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/dto/
- [ ] T080 [US3] Tạo `UserCredentialRepository` (`UPDATE … WHERE password_hash = :hashCũ`, `WHERE password_hash IS NULL`) trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/repository/UserCredentialRepository.java
- [ ] T081 [US3] ⛓ A Cài `PasswordChangeService` theo bảng research R10, dùng `PasswordService`, `ReauthService`, `SessionControl`, `CurrentAccount.sessionId()` trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/service/PasswordChangeService.java (làm T073 xanh; cần T168, T117 của 010)
- [ ] T082 [US3] ⛓ A Tạo `PasswordController` (`POST /me/password`, `POST /me/password/initial`) trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/controller/PasswordController.java (làm T074, T075, T076, T077 xanh)
- [ ] T083 [P] [US3] Tạo `accountApi.ts` trong src/frontend/src/features/profile/api/accountApi.ts
- [ ] T084 [US3] ⛓ A Tạo `ChangePasswordForm`, `SetPasswordForm` trong src/frontend/src/features/profile/components/, `SecurityPage` theo `PF06` trong src/frontend/src/features/profile/pages/SecurityPage.tsx, route `profile/security` trong src/frontend/src/app/routes.tsx. Tạm: `<input type="password">` tới khi có `PasswordField` (T055 của 010); làm T078 xanh

**Checkpoint**: Quickstart Q18–Q20 đạt, cần 010.

---

## Phase 6: User Story 4 - Delete my account (Priority: P2)

**Goal**: Xóa tài khoản ngay, được hoặc mất cả, gồm nhánh bị đình chỉ và bị cấm.

**Independent Test**: Tài khoản có công thức đã xuất bản, bản nháp, công thức chờ duyệt, review, cooksnap, meal plan, shopping list, bộ sưu tập; xóa sau khi xác thực lại, mỗi mục đúng trạng thái, không đăng nhập lại được. Quickstart Q21–Q26.

**Cần 010**: như Phase 5.

### Tests for User Story 4 ⚠️ (viết trước, phải FAIL)

- [ ] T085 [P] [US4] ⛓ A Unit test `AccountDeletionServiceTest` (mock) – `InOrder` đúng bảng research R11: `confirmation` → xác thực lại hoặc `consume` → `AccountGuard` → khóa dòng → `BlockedEmails` chỉ khi `BANNED` → audit → hai port → `DELETE users`; `delete` thường sai → 422 `CONFIRMATION_MISMATCH` trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/service/AccountDeletionServiceTest.java
- [ ] T086 [P] [US4] ⛓ A, B Integration test `AccountDeletionIT` – US4-3, US4-4 (`author_id` NULL), US4-5, US4-7, US4-12 (đăng ký lại được, không có hash), `Edge: Other session` (phiên khác 401) trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/AccountDeletionIT.java
- [ ] T087 [P] [US4] ⛓ B 🕓 #6 Integration test `ArchivedRecipesOnDeletionIT` – US4-5, FR-033: `ARCHIVED` ở lại, tác giả NULL, vẫn ẩn; review và `collection_recipes` của người khác còn; revision đang mở của công thức được giữ bị xóa trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/ArchivedRecipesOnDeletionIT.java
- [ ] T088 [P] [US4] ⛓ E 🕓 #4 Integration test `ReviewCooksnapOnDeletionIT` – US4-6: `CooksnapCleanupPort.deleteAll` được gọi trong transaction. Tạm: mock port vì `V501` chưa có; khi có 070 kiểm tra thật review ẩn danh, điểm không đổi, cooksnap và file mất, trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/ReviewCooksnapOnDeletionIT.java
- [ ] T089 [P] [US4] ⛓ A Integration test `AccountDeletionGuardIT` – US4-2 (sai chữ, chưa xác thực lại → không xóa gì), US4-8 (409 `LAST_ADMINISTRATOR`), US4-10, `Edge: Suspended deletes` (403 `ACCOUNT_SUSPENDED` có `endsAt`; hết hạn thì xóa được; bị đình chỉ giữa lúc xóa → từ chối) trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/AccountDeletionGuardIT.java
- [ ] T090 [P] [US4] ⛓ A Integration test `AccountClosureIT` – US4-11, FR-037b, `Edge: Banned deletes`: `closureToken` thay bearer, hash vào `blocked_emails`, không có email rõ, đăng ký lại bị từ chối; token dùng lại → 410 `TOKEN_INVALID`; được gỡ cấm khi token còn hạn → 409 `ACCOUNT_STATE_CHANGED`; bị đình chỉ khi token còn hạn → 403 `ACCOUNT_SUSPENDED` trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/AccountClosureIT.java
- [ ] T091 [P] [US4] ⛓ A **SC-006**, US4-9 Integration test `AccountDeletionFailureIT` – buộc `AuthoredRecipeCleanupPort`, `CooksnapCleanupPort`, `BlockedEmails` và `DELETE users` lần lượt ném lỗi: mọi dòng còn nguyên, `closureToken` chưa bị tiêu thụ, file avatar còn, thử lại xóa được; kết thúc mỗi lần chạy kiểm FR-033–FR-036 trên các bảng đã có, `reviews`, `cooksnaps`, `collections` thêm ở T130a, trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/AccountDeletionFailureIT.java
- [ ] T092 [P] [US4] ⛓ A Integration test `AccountDeletionRepeatIT` – `Edge: Repeated clicks`: hai request đồng thời chỉ xóa một lần, một dòng audit; hai Administrator tự xóa đồng thời không deadlock trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/AccountDeletionRepeatIT.java
- [ ] T093 [P] [US4] ⛓ A 🕓 #12 Integration test `DeletionReauthLimitIT` – US4-13: sai mật khẩu ở `POST /auth/reauthenticate` cộng chung với đổi mật khẩu, 429 `PASSWORD_CHECK_LIMITED`, không xóa gì, đăng nhập không bị khóa trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/DeletionReauthLimitIT.java
- [ ] T094 [P] [US4] Test `DeleteAccountPage.test.tsx` trên `msw` – US4-1 (nút tắt tới khi gõ đúng `DELETE`; hai danh sách dữ liệu, có "đã lưu trữ"), US4-2, US4-3, US4-8, US4-10, US4-13, `Edge: Repeated clicks`, Gap 011-J, K, L, M trong src/frontend/src/features/profile/__tests__/DeleteAccountPage.test.tsx
- [ ] T095 [P] [US4] Test `AccountClosurePage.test.tsx` trên `msw` – US4-11: đọc `#token=`, xóa khỏi URL bằng `history.replaceState`; không có token thì không xóa được trong src/frontend/src/features/profile/__tests__/AccountClosurePage.test.tsx

### Implementation for User Story 4

- [ ] T096 [P] [US4] Tạo record `DeleteAccountRequest`, `AccountClosureRequest` trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/dto/
- [ ] T097 [US4] ⛓ B 🕓 #6 Tạo `AuthoredRecipeCleanupPort` và stand-in `JdbcAuthoredRecipeCleanup`: xóa `DRAFT`, `PENDING`, `REVISION_REQUESTED`, `REJECTED` và revision `DRAFT`, `PENDING`, `REVISION_REQUESTED` của công thức được giữ, khai báo trong `ProfileDefaultsConfig`, trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/port/ (060 cài bean thật, gồm xóa ảnh)
- [ ] T098 [US4] ⛓ E 🕓 #4 Tạo `CooksnapCleanupPort`, bean mặc định không làm gì trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/port/CooksnapCleanupPort.java (070 cài khi có `V501`)
- [ ] T099 [US4] Tạo `AccountRowRepository` (`SELECT email, status, avatar_storage_key … FOR UPDATE`, `DELETE FROM users`) trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/repository/AccountRowRepository.java
- [ ] T100 [US4] ⛓ A Cài `AccountDeletionService` một `@Transactional` theo bảng research R11, `afterCommit` xóa avatar; audit `ACCOUNT_DELETED` với `{"banned": …}` trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/service/AccountDeletionService.java (làm T085 xanh; cần T168, T166, T117, T092 của 010)
- [ ] T101 [US4] ⛓ A Tạo `AccountDeletionController` (`POST /me/deletion`), `AccountClosureController` (`POST /account-closure`) trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/controller/ (làm T086, T087, T088, T089, T090, T091, T092, T093 xanh)
- [ ] T102 [US4] Tạo `DeletionSummary`, `ConfirmDeletionForm` trong src/frontend/src/features/profile/components/; `DeleteAccountPage`, `AccountClosurePage`, `GoodbyePage` theo `PF07`, `PF08` trong src/frontend/src/features/profile/pages/; route `profile/delete`, `account-closure`, `goodbye` trong src/frontend/src/app/routes.tsx; sau khi xóa xóa token, báo tab khác, `location.replace('/goodbye')` (làm T094, T095 xanh)

**Checkpoint**: Quickstart Q21–Q26 đạt, cần 010; SC-006 xanh trên các bảng đã có, đóng hẳn ở T130a.

---

## Phase 7: User Story 5 - Consistent definition of hard rules and preferences (Priority: P2)

**Goal**: Một định nghĩa cho 020, 030, 031, dùng dữ liệu thật của 011. Code production mới duy nhất của 011 là factory `DietRestrictions.hardRules`.

**Independent Test**: Rà 020, 030, 031 theo FR-020–FR-026, mỗi phát biểu về dị ứng, loại trừ, kiểu ăn, thời gian, ẩm thực, khẩu phần đều khớp.

### Tests for User Story 5 ⚠️ (viết trước, phải FAIL)

- [ ] T102a [P] [US5] Unit test `DietRestrictionsTest` – FR-020, FR-021, FR-022: `hardRules` giữ dị ứng và nguyên liệu loại trừ, luôn trả `DietType.NONE`; hai tập rỗng thì `DietRuleSql.notViolating` trả `TRUE`; khi có dữ liệu thì không có điều kiện `kind` trong src/backend/wikicook-app/src/test/java/com/wikicook/common/diet/DietRestrictionsTest.java
- [ ] T103 [P] [US5] 🕓 #18 Integration test `DietRuleContractIT` – US5-1, **SC-002**, FR-021: lưu hồ sơ qua API 011, đọc qua `DietaryProfilePort`, `DietRuleSql.notViolating` với `DietRestrictions.hardRules(…)` loại đúng món chứa dị ứng hoặc nguyên liệu loại trừ; dữ liệu có link từ `RecipeFixtures`, không dùng dữ liệu cào, trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/port/DietRuleContractIT.java
- [ ] T104 [P] [US5] ⛓ C Integration test `HardRuleWarningIT` – US5-2: với hồ sơ thật, picker của 030 trả `conflicts` gồm `{ type: ALLERGEN, label: <tên> }`, `{ type: EXCLUDED_INGREDIENT, label: <tên> }` trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/port/HardRuleWarningIT.java
- [ ] T105 [P] [US5] ⛓ C 🕓 #7 Integration test `PreferencesNeverRemoveIT` – US5-3: với hồ sơ Vegetarian, thời gian tối đa 30, một ẩm thực ưa thích: `GET /meal-plans/picker/recipes` vẫn trả món lệch kiểu ăn, quá thời gian, khác ẩm thực; lần tạo của 031 không xác nhận không loại các món này, có xác nhận thì loại. Phần 031 viết khi C xong dòng 7, không `@Disabled` trong nhánh 011, trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/port/PreferencesNeverRemoveIT.java
- [ ] T106 [P] [US5] ⛓ C 🕓 #2 Sửa `ServingsPolicyTest` – US5-4: hộ 4 → 4, trống → 2 trong src/backend/wikicook-app/src/test/java/com/wikicook/mealplan/service/ServingsPolicyTest.java. Làm cùng commit với T109 để `mvn verify` không đỏ; trước đó T034 kiểm tra port trả `null`
- [ ] T107 [P] [US5] ⛓ C, D Integration test `ProfileEditKeepsPlansIT` – US5-5, FR-024: đổi khẩu phần và dị ứng, món đã lên lịch và shopping list không đổi; thao tác kế tiếp thấy giá trị mới trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/port/ProfileEditKeepsPlansIT.java

### Implementation for User Story 5

- [ ] T107b [US5] Thêm factory `public static DietRestrictions hardRules(Set<Long> allergenIds, Set<Long> excludedIngredientIds)`, sửa Javadoc theo FR-020–FR-022 (diet type chỉ cứng khi người dùng xác nhận) trong src/backend/wikicook-app/src/main/java/com/wikicook/common/diet/DietRestrictions.java (làm T102a, T103 xanh)
- [ ] T108 [US5] ⛓ B, C Rà 020, 030, 031 theo FR-020–FR-026, ghi từng chỗ còn lệch vào specs/011-user-profile-dietary/checklists/hard-rules-review.md; sửa spec thuộc về chủ spec (research, bảng cuối, mục 2, 3, 7)
- [ ] T109 [US5] ⛓ C 🕓 #2 C đổi mặc định thành 2 trong src/backend/wikicook-app/src/main/java/com/wikicook/mealplan/service/ServingsPolicy.java, sửa Javadoc `PlannerProfile`, `DraftItem` và chú thích `householdSize` ở specs/030-meal-planner/contracts/integration.md (làm T106 xanh). Không làm trong nhánh 011 nếu C chưa nhận

**Checkpoint**: SC-002 xanh trên bộ seed có link; bản rà không còn dòng mở của 011.

---

## Phase 8: Integration (010, 060, 070, 100, 020, 030, 031)

### 8a. Nối 010 (làm trước Phase 5, 6)

- [ ] T110 [P] ⛓ A Integration test `ProfileAuditIT` – FR-042, NFR-005: mỗi hành động có đúng một dòng; gieo dị ứng rồi quét `audit_logs.details`, không có giá trị ăn uống, email, tên, mật khẩu trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/ProfileAuditIT.java
- [ ] T111 [P] ⛓ A Integration test `AvatarCleanupListenerIT` – `AccountResetEvent`, `AccountPurgedEvent`: file avatar chỉ mất sau commit; 010 rollback thì file còn; không có avatar thì không lỗi trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/service/AvatarCleanupListenerIT.java
- [ ] T112 [P] ⛓ A **SC-004**, US2-7, US2-8 Integration test `DataBoundaryIT` – gieo email, dị ứng, khẩu phần; duyệt mọi route GET bằng **khách**, **Member thứ hai**, **Administrator** với JWT thật, quét JSON: 0 phản hồi cho khách và người khác có email hay dữ liệu ăn uống, 0 phản hồi cho Administrator có dữ liệu ăn uống; Administrator đọc được email ở `GET /admin/users/{id}`, không có lệnh ghi nào lên người khác trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/DataBoundaryIT.java
- [ ] T113 [P] ⛓ A Integration test `ProfileLogLeakIT` – NFR-004, NFR-005, SC-004: chụp log khi lưu hồ sơ, đổi mật khẩu, lỗi validate; không có mật khẩu, email, giá trị ăn uống trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/ProfileLogLeakIT.java
- [ ] T114 [P] ⛓ A Integration test `RestrictedAccountWriteIT` – `Edge: Suspended or banned`: bearer của tài khoản `SUSPENDED`, `BANNED` nhận 403 ở `POST /me/profile`, `PUT /me/dietary-profile`, `POST /me/dietary-profile/notices/ack`, `POST /me/password`, `POST /me/password/initial` trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/RestrictedAccountWriteIT.java
- [ ] T115 [P] ⛓ A Test `AdminAccountPanel.test.tsx` – US2-8, Gap 011-E: hiện cho Administrator, ẩn với vai trò khác, không có nút sửa, không có dữ liệu ăn uống trong src/frontend/src/features/profile/__tests__/AdminAccountPanel.test.tsx
- [ ] T116 ⛓ A Tạo adapter `ProfileAuditPort` → `AuditWriter`, `ProfileAudit` trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/port/AuditWriterAdapter.java và src/backend/wikicook-app/src/main/java/com/wikicook/profile/service/ProfileAudit.java (làm T110 xanh; cần T039 của 010)
- [ ] T117 ⛓ A Tạo `AvatarCleanupListener`: `@EventListener` đọc khóa, `registerSynchronization` xóa ở `afterCommit` trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/service/AvatarCleanupListener.java (làm T111 xanh; cần T146, T163 của 010)
- [ ] T118 ⛓ A Thêm vào src/backend/wikicook-app/src/main/java/com/wikicook/auth/security/PublicRoutes.java: `GET /api/v1/profiles/*`, `GET /api/v1/profiles/*/recipes`, `GET /api/v1/media/avatars/*`, `POST /api/v1/account-closure`; `RouteProtectionIT` của 010 phải xanh (cần T043 của 010)
- [ ] T119 ⛓ A Bọc `/profile*` bằng `RequireAuth` (không `offlineFirst`), thêm mục `/profile` vào src/frontend/src/app/AppLayout.tsx, sửa src/frontend/src/app/routes.tsx; **không** gắn thêm `SessionExpiredDialog` (010 đã gắn ở `AppLayout`), thay `useBlocker` tạm ở `DietaryProfilePage`, `EditProfilePage` bằng `useUnsavedChangesBlocker()` trong src/frontend/src/features/profile/pages/ (cần T053, T054, T077 của 010; làm T036 xanh với dialog thật)
- [ ] T120 ⛓ A Tạo `AdminAccountPanel` gọi `GET /admin/users/{id}`, gắn vào src/frontend/src/features/profile/pages/PublicProfilePage.tsx, trong src/frontend/src/features/profile/components/AdminAccountPanel.tsx (làm T115 xanh)
- [ ] T121 ⛓ A Thay input tạm bằng `PasswordField` trong src/frontend/src/features/profile/components/ChangePasswordForm.tsx và SetPasswordForm.tsx; bằng `TextField` trong src/frontend/src/features/profile/components/NumberField.tsx và src/frontend/src/features/profile/pages/EditProfilePage.tsx (cần T055 của 010)

### 8b. Thay đổi catalogue (FR-019)

- [ ] T122 [P] ⛓ B, E 🕓 #8 Integration test `DietaryCatalogChangesIT` – `Edge: Ingredient merged` (chuyển sang đích, đã loại trừ đích thì giữ một dòng, không vượt 50), `Edge: Ingredient deleted`, `Edge: Allergen removed`: mỗi trường hợp một `dietary_notices` đúng tên; chạy trong transaction bên gọi, rollback thì không đổi; không đổi `dietary_version` trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/service/DietaryCatalogChangesIT.java
- [ ] T123 [P] Integration test `DietaryNoticeAckIT` – FR-019: `GET /me/dietary-profile` trả `notices[]`, `POST …/notices/ack` xóa đúng của người gọi, bỏ qua id người khác trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/DietaryNoticeAckIT.java
- [ ] T124 [P] Test `CatalogueNotices.test.tsx` – `Edge: Ingredient merged`, `Edge: Ingredient deleted`, `Edge: Allergen removed`, Gap 011-C: hiện ở lần vào sau, đóng rồi không hiện lại trong src/frontend/src/features/profile/__tests__/CatalogueNotices.test.tsx
- [ ] T125 ⛓ B, E 🕓 #8 Cài `DietaryCatalogChanges` trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/api/DietaryCatalogChanges.java và `DietaryCatalogChangeService` trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/service/DietaryCatalogChangeService.java (làm T122 xanh). Chưa có merge của 100 thì interface chưa ai gọi, test gọi trực tiếp
- [ ] T126 Thêm record `AckNoticesRequest`, `POST /me/dietary-profile/notices/ack`, trả `notices[]` trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/controller/DietaryProfileController.java (làm T123 xanh)
- [ ] T127 Tạo `CatalogueNotices` trong src/frontend/src/features/profile/components/CatalogueNotices.tsx, gắn vào src/frontend/src/features/profile/pages/DietaryProfilePage.tsx (làm T124 xanh)

### 8c. Tên tác giả (FR-007) và lỗi đọc hồ sơ (FR-026a)

- [ ] T128 [P] ⛓ B, E 🕓 #11 Integration test `AuthorDirectoryIT` – FR-007: đổi tên, avatar thấy ngay ở lần đọc sau, không đổi nội dung đã lưu; tài khoản đã xóa không có trong kết quả; `banned = true` cho người bị cấm trong src/backend/wikicook-app/src/test/java/com/wikicook/profile/service/AuthorDirectoryIT.java
- [ ] T129 ⛓ B, E 🕓 #11 Cài `AuthorDirectory`, `AuthorView(long id, String displayName, String avatarUrl, boolean banned)` trong src/backend/wikicook-app/src/main/java/com/wikicook/profile/api/ (làm T128 xanh); 060, 070 tự hiển thị "Người dùng đã xóa"
- [ ] T130 ⛓ B, C 🕓 #10 Bàn giao FR-026a: giao diện 020, 030, 031 đọc 503 `PROFILE_UNAVAILABLE`, không tự chọn món, nói lý do; 020 kiểm tra cùng hành vi khi có code; xác nhận với Dự, Duy theo specs/011-user-profile-dietary/contracts/integration.md mục 2.1. Tạm: API đã trả 503 (T042a), giao diện hiện lỗi chung

### 8d. Bàn giao dữ liệu và kịch bản phụ thuộc module khác (SC-002, SC-006, SC-008)

- [ ] T130a ⛓ B, E Bàn giao US4-4, US4-6, FR-033, FR-035, FR-036: 060 và trang công thức hiện "Người dùng đã xóa", không tên, ảnh, link, kèm test; 070 cài `CooksnapCleanupPort`, mở rộng T088 với bảng thật (review ẩn danh, điểm không đổi, cooksnap và file mất); 080 khai báo `CASCADE` ở `V502`, thêm `collections` vào T086. Khi xong chạy lại T091, Q22 đủ mọi mục. SC-006, SC-008 chỉ đóng ở bước này
- [ ] T130b ⛓ B, C 🕓 #18 Điền `kind` và `ingredient_allergens` cho khoảng 2.900 nguyên liệu qua tools/angingon-scraper/mappings/ingredients.csv, moderator duyệt; chạy lại T103 trên dữ liệu đã duyệt. Trước đó không tuyên bố lọc dị ứng an toàn trên dữ liệu cào (FR-010a, SC-002)

---

## Phase 9: Polish & Cross-Cutting Concerns

- [ ] T131 [P] Playwright US1–US4 trên `fakeApi`, gồm 360 px không cuộn ngang, chạm ≥ 44 × 44 px (NFR-002), đi hết form bằng bàn phím (NFR-006) trong src/frontend/e2e/profile.spec.ts, src/frontend/e2e/fakeApi.ts
- [ ] T132 [P] Dựng các gap UI 011-A…M còn thiếu từ cùng component, kiểm tra `wk-` theo `PF01`–`PF08` trong src/frontend/src/features/profile/components/
- [ ] T133 [P] NFR-003: rà chuỗi hiển thị chỉ nằm ở src/frontend/src/features/profile/strings.vi.ts, mọi `code` và `errors[].code` ở research R17 có chuỗi tiếng Việt
- [ ] T134 [P] Viết src/backend/perf/profile.k6.js theo research R20 với catalogue 10.000 nguyên liệu; đo `GET /me/profile`, `GET`, `PUT /me/dietary-profile` p95 < 1 s, `GET /ingredients/suggestions` p95 < 150 ms với `q` dài 2 và 3 ký tự (NFR-001); ghi số đo vào mục R20 của specs/011-user-profile-dietary/research.md
- [ ] T135 [P] SC-001: thử với người dùng, 90% hoàn thành hồ sơ trong 2 phút; ghi kết quả vào docs/test/011-usability.md
- [ ] T136 [P] Đối chiếu `/v3/api-docs` với specs/011-user-profile-dietary/contracts/openapi.yaml, sửa chỗ lệch
- [ ] T137 ⛓ C 🕓 #17 Yêu cầu Dự cập nhật docs/analysis-and-design/database/full-schema.md theo bảng cuối research, mục 17
- [ ] T138 🕓 #12 Sau khi spec được duyệt, thêm `AF 4.12` và cập nhật các flow "Proposed" trong docs/requirements/use-case-specification.md
- [ ] T139 Viết ma trận truy vết FR, NFR, SC, scenario → test ở docs/test/011-traceability.md từ bảng phủ cuối file này
- [ ] T140 Chạy toàn bộ specs/011-user-profile-dietary/quickstart.md, `cd src/backend && mvn verify`, `npm run lint && npm test && npm run build`; JaCoCo `profile.service` ≥ 70%

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: T001 cần PR SCRUM-72 được duyệt (mã Jira đã có); T002 trở đi chỉ bắt đầu sau T001.
- **Foundational (Phase 2)**: sau Setup, CHẶN mọi user story.
- **US1, US2 (Phase 3, 4)**: sau Foundational, độc lập với nhau; không cần 010.
- **US5 (Phase 7)**: sau US1. **Làm trước US3, US4**: số phase theo thứ tự story, không theo thứ tự chạy.
- **Phase 8b, 8c**: sau US1; không cần 010, trừ khi nối thật với 060, 100.
- **Phase 8d**: T130a sau US4 và khi 060, 070, 080 có code. T130b không chặn code của 011 nhưng chặn việc tính dữ liệu cào vào SC-002.
- **Phase 8a**: sau US2 và sau Phase 2, 3, 5 của 010; riêng T111, T117 cần thêm T146 (Phase 7), T163 (Phase 8) của 010. **Làm trước US3, US4**.
- **US3, US4 (Phase 5, 6)**: backend sau Phase 8 của 010 (T092, T117, T166, T168, T168a); frontend làm trước trên `msw`.
- **Polish (Phase 9)**: sau các story muốn giao.

Thứ tự chạy: Phase 1 → 2 → 3, 4 → 7, 8b, 8c → (010 merge) → 8a → 5, 6 → 9.

### Within Each User Story

- Test viết trước và FAIL → DTO → port → repository → service → controller → hook → component → page.
- Không đánh dấu task xong khi test tương ứng chưa xanh.

### Phụ thuộc người khác (⛓)

| Task | Người | Cách làm tạm | Khi bên kia xong |
|---|---|---|---|
| T004, T005 | A | 011 tự thêm `out-of-order` nếu merge trước | Bên merge sau bỏ qua |
| T024, T116 | A | `ProfileAuditPort` không làm gì | Thêm adapter `AuditWriter` |
| T036, T119 | A | Mock `SessionExpiredDialog`; route chưa bọc `RequireAuth`, dev stub, không triển khai công khai | Nối component thật |
| T073–T077, T081, T082 | A | Không có stub `auth.api` (research R19). Làm lát A trước, frontend trên `msw`; backend tạo nhánh từ code 010 khi đủ T168 | Viết test và code |
| T085–T093, T100, T101 | A | Như trên | Viết test và code |
| T112, T113, T110, T111 | A | T058 kiểm tra cấu trúc bằng `X-Test-User` | Chạy với JWT thật |
| T118 | A | Chưa có Spring Security, mọi route đang mở | Thêm route, `RouteProtectionIT` xanh |
| T084, T121 | A | Input tạm | Dùng `PasswordField`, `TextField` |
| T040 | B, E | `JdbcIngredientCatalog`, `JdbcCuisineCatalog` | Bean thật tự thay |
| T064 | B | `JdbcPublicRecipes` | 060 cài bean thật |
| T097, T086, T087 | B | `JdbcAuthoredRecipeCleanup`, chưa xóa file ảnh | 060 cài bean thật, gồm file |
| T098, T088 | E | Port không làm gì, test mock | 070 cài port khi có `V501` |
| T122, T125 | B, E | Test gọi trực tiếp interface | 100 gọi khi merge, gộp `ingredient_allergens` |
| T128, T129 | B, E | 060, 070 chưa gọi | Dùng khi có màn tác giả |
| T034, T034a, T042a, T046, T047 | C, D | Không cần: chữ ký port giữ nguyên | C sửa test theo T047 nếu xung đột |
| T104, T105, T106, T107, T109 | C, D | Test bám code hiện có; `ServingsPolicy` đổi sau | C đổi code theo dòng 2, 7 |
| T130 | B, C | API trả 503, không chọn món, giao diện hiện lỗi chung | Hiện lý do ở giao diện |
| T130a | B, E, 080 | Chỉ kiểm phần bảng đã có; SC-006, SC-008 chưa đóng | Mỗi bên nối test của mình, 011 đóng SC |
| T130b | B, C | Chỉ demo lọc dị ứng trên bộ seed có link | Moderator duyệt xong mới tính dữ liệu cào vào SC-002 |
| T137 | C | `data-model.md` là nguồn tham chiếu của 011 | Cập nhật một lượt cùng 010 |

### Chờ chốt (🕓)

| Dòng | Task | Đang chờ | Không được giả định |
|---|---|---|---|
| #1 | T001 | Duyệt PR SCRUM-72 (mã Jira đã có) | Không bắt đầu code khi PR chưa được duyệt |
| #2 | T106, T109 | Dự sửa 030, 031 | 011 không sửa `ServingsPolicy` |
| #4 | T088, T098 | Chung sửa 070, chốt cấu trúc `cooksnaps` | Không viết query cooksnap trước khi có `V501` |
| #6 | T087, T097 | Duy duyệt FR-034, B xác nhận | Giữ `ARCHIVED` mới là quyết định chờ duyệt, đổi lại một điều kiện SQL |
| #7 | T105 | Dự sửa 031, 030 | Không đổi code picker trong nhánh 011 |
| #8 | T122, T125 | Chung thêm merge vào 100 | Không viết thao tác merge ở 011 |
| #10 | T130 | Duy duyệt FR-026a | Không sửa 020, 030, 031 trong nhánh 011 |
| #11 | T057, T067, T128, T129 | Duy duyệt; 010 chưa ghi quy tắc | Không thêm trạng thái nào khác cho hồ sơ bị cấm |
| #12 | T075, T093, T138 | Duy duyệt; T168a của 010 | Không đếm mật khẩu sai ở 011 |
| #17 | T137 | Spec được duyệt, Dự cập nhật một lượt cùng 010 | Không coi `full-schema.md` đã phản ánh `V211` trước khi cập nhật |
| #18 | T103, T130b | B, C điền mapping, chưa có người nhận và hạn | Không coi SC-002 đạt trên dữ liệu cào |
| #20 | Phần Google của T074 (US3-6), T084, T089, T094, T102 | 010 T149b thử xác thực lại với Google thật | Không coi xác thực lại bằng Google đã chạy được chỉ vì WireMock xanh; tài khoản có mật khẩu không bị chặn |

### Parallel Opportunities

- Setup: T002, T005, T006 song song.
- Foundational: mọi test [P]; T020, T021, T023, T025, T026 song song.
- Mỗi story: mọi task test [P] song song; backend và frontend của cùng story song song.
- Sau Foundational: US1 và US2 giao cho hai người được. Frontend US3, US4 song song với lát A.

---

## Parallel Example: User Story 1

```bash
# Viết toàn bộ test US1 cùng lúc (phải FAIL):
Task: "T027 DietaryProfileServiceTest in src/backend/wikicook-app/src/test/java/com/wikicook/profile/service/DietaryProfileServiceTest.java"
Task: "T028 DietaryProfileControllerIT in src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/DietaryProfileControllerIT.java"
Task: "T032 IngredientSuggestionIT in src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/IngredientSuggestionIT.java"
Task: "T033 IngredientSuggestionThirtyNamesIT in src/backend/wikicook-app/src/test/java/com/wikicook/profile/controller/IngredientSuggestionThirtyNamesIT.java"
Task: "T037 ExcludedIngredientPicker.test.tsx in src/frontend/src/features/profile/__tests__/ExcludedIngredientPicker.test.tsx"

# Sau đó code song song backend và frontend:
Task: "T039 DTO records in src/backend/wikicook-app/src/main/java/com/wikicook/profile/dto/"
Task: "T044 IngredientSuggestionService in src/backend/wikicook-app/src/main/java/com/wikicook/profile/service/IngredientSuggestionService.java"
Task: "T048 dietaryApi.ts in src/frontend/src/features/profile/api/dietaryApi.ts"
```

---

## Implementation Strategy

### MVP First (Setup + Foundational + US1 + US2 + US5)

1. Phase 1, Phase 2.
2. US1 → **dừng và kiểm chứng** quickstart Q1–Q9. 030, 031, 040 có hồ sơ thật.
3. US2 → kiểm chứng Q10–Q15.
4. US5 → SC-002 xanh trên bộ seed.

Chưa triển khai công khai ở bước này: chưa có Spring Security.

### Incremental Delivery

1. MVP lát A → demo nội bộ bằng dev stub.
2. 010 merge → Phase 8a → triển khai được, SC-004 xanh.
3. US3 → Q18–Q20, SC-005.
4. US4 → Q21–Q26, SC-006 phần bảng đã có; đóng hẳn ở T130a.
5. Phase 8b khi 100 có merge; Phase 9 trước Build v0.8.

### Parallel Team Strategy

1. A làm Setup, Foundational.
2. Một người US1, một người US2.
3. Khi 010 merge: một người 8a + US3, một người US4.

---

## Notes

- [P] = file khác nhau, không phụ thuộc task chưa xong.
- Commit theo Conventional Commits kèm mã Jira của phần đang làm (mục "Mã Jira"), ví dụ `feat(profile): add dietary profile API (SCRUM-75)`.
- Dừng ở mỗi Checkpoint để kiểm chứng story độc lập.

### Phủ acceptance scenario và edge case

| Mã | Task test |
|---|---|
| US1-1 | T028, T035 |
| US1-2 | T028, T038 |
| US1-3 | T032, T037 |
| US1-4 | T028, T038 |
| US1-5 | T028, T038 |
| US1-6 | T028, T035 |
| US1-7 | T028, T035 |
| US1-8 | T034 |
| US2-1 | T053, T059 |
| US2-2 | T053, T059 |
| US2-3 | T055, T060 |
| US2-4 | T060 |
| US2-5 | T055, T060 |
| US2-6 | T056, T061 |
| US2-7 | T058, T112 |
| US2-8 | T112, T115 |
| US3-1 | T074, T078 |
| US3-2 | T073, T075, T078 |
| US3-3 | T073, T074, T078 |
| US3-4 | T076, T078 |
| US3-5 | T074, T078 |
| US3-6 | T074, T078 |
| US4-1 | T094 |
| US4-2 | T089, T094 |
| US4-3 | T086, T094 |
| US4-4 | T086, T130a |
| US4-5 | T086, T087 |
| US4-6 | T088, T130a |
| US4-7 | T086, T130a |
| US4-8 | T089, T094 |
| US4-9 | T091 |
| US4-10 | T089, T094 |
| US4-11 | T090, T095 |
| US4-12 | T086 |
| US4-13 | T093, T094 |
| US5-1 | T103 |
| US5-2 | T104 |
| US5-3 | T105 |
| US5-4 | T106 |
| US5-5 | T107 |
| Edge: Display name | T011, T052, T059 |
| Edge: Bio | T052, T053, T059 |
| Edge: Avatar not an image | T054, T055 |
| Edge: Unsaved changes | T035, T059 |
| Edge: Network loss | T035, T059, T078 |
| Edge: Session expired | T036 |
| Edge: Invalid data | T027, T028 |
| Edge: Ingredient merged | T122, T124 |
| Edge: Ingredient deleted | T122, T124 |
| Edge: Allergen removed | T122, T124 |
| Edge: Two tabs | T030, T053, T035 |
| Edge: 51st ingredient | T028, T037 |
| Edge: Stale tab | T074, T078 |
| Edge: Other session | T086 |
| Edge: Repeated clicks | T030, T074, T092, T035, T059, T078, T094 |
| Edge: Suspended or banned | T057, T114 |
| Edge: Suspended deletes | T089 |
| Edge: Banned deletes | T090 |
