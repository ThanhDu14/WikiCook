# Implementation Plan: User Profile & Dietary Profile

**Branch**: `feature/SCRUM-75-profile-backend`, `feature/SCRUM-76-auth-profile-frontend` (tài liệu: `feature/SCRUM-72-spec-kit-010-011`) | **Date**: 2026-11-06 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/011-user-profile-dietary/spec.md`

## Summary

Một nơi để người dùng đã đăng nhập giữ hồ sơ công khai (tên hiển thị, bio, avatar), hồ sơ ăn uống
(dị ứng, nguyên liệu loại trừ, kiểu ăn, ẩm thực ưa thích, khẩu phần gia đình, thời gian nấu tối đa)
và các thao tác an toàn tài khoản (đổi hoặc đặt mật khẩu, xóa tài khoản). 011 là nguồn sự thật của
hồ sơ ăn uống cho 020, 030, 031, 040 và cung cấp định nghĩa quy tắc bắt buộc, sở thích.

Cách làm: module `com.wikicook.profile` (controller → service → repository) trên schema thật: hồ sơ
ăn uống là cột của `users` cộng ba bảng `user_*`, không có `dietary_profiles`, `diet_types` như bản đầu của spec (research R2). Migration mới `V211` thêm `bio`, sửa CHECK thời gian nấu thành 10–240, thêm hai
cột version, bảng `dietary_notices`, `profile_write_requests` và seed 10 chất gây dị ứng. Ba mức dữ
liệu được bảo đảm bằng cấu trúc: chủ tài khoản chỉ đi qua `/me/**`, không có id trên URL; khách đi
qua `/profiles/{id}` với DTO không có trường riêng tư; Administrator dùng lại `GET
/admin/users/{id}` của 010, nơi không có dữ liệu ăn uống (R3). Lưu hồ sơ ăn uống thay toàn bộ trong
một transaction, có khóa theo `dietary_version`; lưu lại nội dung y hệt là no-op nên bấm hai lần
không sinh hiệu ứng trùng (R5, R12). Gợi ý nguyên liệu dùng cột `name_normalized` và GIN trigram có
sẵn (R6). Avatar được kiểm tra magic bytes, giải mã, cắt vuông 512 px và mã hóa lại JPEG, lưu trên
volume cục bộ qua `MediaStorage` (R7). Hai port `DietaryProfilePort` của 030 và 040 có adapter thật,
tự thay stub nhờ `@ConditionalOnMissingBean` (R8). Xóa tài khoản chạy trong một transaction của 011,
gọi `auth.api` của 010 và port của 060, 070, gồm cả nhánh tài khoản bị cấm qua `closureToken` (R11).
Frontend thêm `features/profile` với 8 route, dựng từ `PF01`–`PF08`.

US1, US2, US5 chạy được trước khi 010 merge; US3, US4 cần `auth.api` của 010 (R19).

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5 + React 18 (frontend)

**Primary Dependencies**: Spring Boot 3.5.16 (Web, Data JPA, Validation; Security qua 010),
springdoc-openapi, Flyway, `com.twelvemonkeys.imageio:imageio-webp` (mới, đọc WebP); Vite, Tailwind
CSS 3.4, React Router 6

**Storage**: PostgreSQL 16 (+ `unaccent`, `pg_trgm`); `users` (`V101`), `allergens` (`V010`),
`user_allergens`, `user_excluded_ingredients`, `user_preferred_cuisines` (`V210`); mới ở
`V211__profile_schema_updates.sql`. File avatar: local disk, volume Docker `media` (chốt cho bản
demo)

**Testing**: JUnit 5 + Mockito, `@SpringBootTest` + Testcontainers (`pgvector/pgvector:pg16`),
JaCoCo; Vitest + React Testing Library + `msw`, Playwright; k6 cho NFR-001. Chạy bằng `mvn verify`,
chưa có Maven wrapper

**Target Platform**: Linux server (Docker Compose); trình duyệt hiện đại desktop và mobile từ 360 px

**Project Type**: Web application (backend REST + SPA)

**Performance Goals**: mở trang hồ sơ, lưu hồ sơ ăn uống p95 < 1 s; gợi ý nguyên liệu p95 ≤ 300 ms
sau phím cuối với 10.000 nguyên liệu (NFR-001)

**Constraints**: Ba mức dữ liệu do server thực thi (FR-006, NFR-005); lưu hồ sơ và xóa tài khoản
được hoặc mất cả (FR-017, FR-032, NFR-007); audit không chứa dữ liệu ăn uống (FR-042); không thêm
Redis (NFR-008, 010 R5); chữ ký port 030, 040 giữ nguyên; `PlannerProfile.householdSize` vẫn trả
`null` khi trống

**Scale/Scope**: Đồ án: ~100 người dùng đồng thời, 8 route frontend mới, 14 endpoint, 2 adapter
port, 1 migration

Không còn mục NEEDS CLARIFICATION – xem [research.md](research.md) (R1–R20). Mâu thuẫn giữa các spec
đã chốt ngày 2026-11-06 ở "Điểm cần chốt với nhóm" cuối research; các spec liên quan chưa sửa.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Ghi chú |
|---|---|---|
| I. Spec trước, code sau | ⚠️ Có điều kiện | Spec ở **In Review**; mã Jira đã có (SCRUM-72 tài liệu, SCRUM-75 backend, SCRUM-76 frontend). Plan được lập để nhóm duyệt cùng; **không bắt đầu implement** trước khi PR SCRUM-72 được duyệt (T001). Spec đã sửa theo bảng cuối research (mục 1, 3, 6, 10–13, 16, 18), chờ Duy duyệt PR. |
| II. Test-First | ✅ | Mỗi acceptance scenario có test (SC-008); tasks sẽ đặt test trước code; JaCoCo ≥ 70% cho `profile.service`; Testcontainers PostgreSQL thật, không H2 (R20). |
| III. Kiến trúc phân lớp | ⚠️ Có ngoại lệ | Controller → Service → Repository; DTO record, không trả entity; package theo tính năng `profile`; module khác qua port và `auth.api`. SQL chỉ nằm trong `repository/` và stand-in `Jdbc*`, không nằm trong `service/`; thư viện mới và repository dùng JDBC ghi ở Complexity Tracking. |
| IV. Bảo mật & dữ liệu | ✅ | `userId` lấy từ `CurrentUser`, không có id trên URL của chủ tài khoản; Bean Validation mọi input; query tham số hóa; ProblemDetail không lộ stack trace; bio và tên hiển thị render bằng React (tự escape); avatar được mã hóa lại, phục vụ với `nosniff`. Không đổi quy tắc BCrypt, dùng `PasswordService` của 010. |
| V. Trải nghiệm nhất quán | ✅ | Responsive từ 360 px, chuỗi ở `features/profile/strings.vi.ts`; dùng lại `PasswordField`, `TextField`, `SessionExpiredDialog` của 010; danh sách công khai phân trang; AI disclaimer không áp dụng. |
| Ràng buộc kỹ thuật | ✅ | Java 21, Spring Boot 3, PostgreSQL 16 + Flyway, React 18 + Vite, `/api/v1`, OpenAPI. Không dùng `ddl-auto=update`. |

**Kết quả gate**: CHƯA ĐẠT Nguyên tắc I (ngoại lệ ghi ở Complexity Tracking). Mã Jira đã có; chưa được
chạy `/speckit-implement` tới khi PR SCRUM-72 được duyệt (T001).

**Re-check sau Phase 1**: PASS, trừ điều kiện của Nguyên tắc I – thêm 2 bảng và 3 cột vào `users`, đều có FR hoặc lý do ở research
R2, R4, R13, R15; không có bảng `dietary_profiles`, `diet_types`; contracts chỉ dùng
`/api/v1/me/**`, `/api/v1/profiles/**`, `/api/v1/ingredients/suggestions`,
`/api/v1/media/avatars/**`, `/api/v1/account-closure`; module khác đi qua port, không vi phạm mới.
Phụ thuộc mới và stand-in JDBC vào bảng của module khác ghi ở Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/011-user-profile-dietary/
├── plan.md              # File này
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/
│   ├── openapi.yaml     # REST API /api/v1/me, /profiles, /media/avatars, /account-closure
│   └── integration.md   # Port và interface với 010, 020, 030, 031, 040, 060, 070, 100
└── tasks.md             # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
src/backend/
├── pom.xml                            # quản lý version imageio-webp
├── Dockerfile                         # tạo /var/lib/wikicook/media cho user wikicook
├── docker-compose.yml                 # volume media, WIKICOOK_MEDIA_DIR, FLYWAY_OUT_OF_ORDER
├── .env.example                       # thêm WIKICOOK_MEDIA_DIR
├── perf/profile.k6.js                 # NFR-001
├── wikicook-db/src/main/resources/db/migration/
│   └── V211__profile_schema_updates.sql
└── wikicook-app/
    ├── pom.xml                        # imageio-webp; rule JaCoCo cho profile.service
    └── src/
        ├── main/java/com/wikicook/
        │   ├── profile/
        │   │   ├── controller/   MyProfileController, DietaryProfileController, IngredientSuggestionController,
        │   │   │                 PublicProfileController, AvatarMediaController, PasswordController,
        │   │   │                 AccountDeletionController, AccountClosureController,
        │   │   │                 ProfileExceptionHandler
        │   │   ├── service/      MyProfileService, AvatarImageProcessor, DietaryProfileService,
        │   │   │                 DietaryProfileReader, DietaryCatalogChangeService,
        │   │   │                 IngredientSuggestionService, PublicProfileService, PasswordChangeService,
        │   │   │                 AccountDeletionService, ProfileWriteLimiter, ProfileAudit,
        │   │   │                 AvatarCleanupListener
        │   │   ├── repository/   UserProfileRepository, DietaryLinkRepository, DietaryNoticeRepository,
        │   │   │                 ProfileWriteRequestRepository, UserCredentialRepository,
        │   │   │                 AccountRowRepository
        │   │   ├── domain/       UserProfile, DietaryNotice, NoticeKind, ProfileWriteKind
        │   │   ├── dto/          MyProfileDto, AccountInfoDto, ProfileUpdateRequest, PublicProfileDto,
        │   │   │                 DietaryProfileDto, DietaryProfileUpdateRequest, DietaryOptionsDto,
        │   │   │                 ChangePasswordRequest, SetPasswordRequest, DeleteAccountRequest, ...
        │   │   ├── validation/   PlainTextLine, PlainTextLineValidator
        │   │   ├── port/         MealPlanDietaryAdapter, ShoppingDietaryAdapter, PublicRecipesPort,
        │   │   │                 IngredientCatalogPort, CuisineCatalogPort,
        │   │   │                 AuthoredRecipeCleanupPort, CooksnapCleanupPort, ProfileAuditPort,
        │   │   │                 Jdbc* (stand-in tới khi 060, 100 có)
        │   │   ├── api/          DietaryCatalogChanges, AuthorDirectory, AuthorView
        │   │   └── config/       ProfileProperties, ProfileDefaultsConfig
        │   ├── common/media/     MediaStorage, LocalDiskMediaStorage
        │   ├── common/diet/      DietRestrictions (thêm factory hardRules, giữ DietRuleSql),
        │   │                     DietaryProfileUnavailableException
        │   ├── common/GlobalExceptionHandler.java   # 503 PROFILE_UNAVAILABLE (FR-026a)
        │   ├── auth/security/PublicRoutes.java   # 010 sở hữu, thêm route công khai của 011
        │   └── devstub/          # bỏ StubDietaryProfile; phần còn lại theo 010 R13
        ├── main/resources/application.yml   # multipart, flyway.out-of-order, wikicook.profile.*
        └── test/java/com/wikicook/profile/
            ├── service/          unit test (Mockito)
            ├── repository/       *IT + Testcontainers
            ├── port/             DietaryPortsIT, DietRuleContractIT
            └── controller/       *IT, gồm DataBoundaryIT, AccountDeletionIT

src/frontend/
├── nginx.conf                         # client_max_body_size cho /api/
├── e2e/profile.spec.ts, e2e/fakeApi.ts
└── src/
    ├── app/routes.tsx                 # 8 route, RequireAuth khi 010 có
    ├── shared/api/client.ts           # api.postForm cho FormData
    └── features/profile/
        ├── api/          profileApi.ts, dietaryApi.ts, accountApi.ts
        ├── pages/        MyAccountPage, DietaryProfilePage, EditProfilePage, PublicProfilePage,
        │                 SecurityPage, DeleteAccountPage, AccountClosurePage, GoodbyePage
        ├── components/   AllergenPicker, ExcludedIngredientPicker, CuisinePicker, DietTypeSelect,
        │                 NumberField, AvatarPicker, ProfileSkeleton, DietaryEmptyState,
        │                 CatalogueNotices, ChangePasswordForm, SetPasswordForm,
        │                 DeletionSummary, ConfirmDeletionForm, AdminAccountPanel
        ├── hooks/        useMyProfile, useDietaryProfile, useIngredientSuggestions
        ├── lib/          avatarRules.ts, codePoints.ts
        ├── strings.vi.ts
        ├── types.ts
        └── __tests__/    Vitest + RTL + msw
```

**Structure Decision**: Web application theo bố cục thật của repo: backend Maven multi-module ở
`src/backend` (code trong `wikicook-app`, migration trong `wikicook-db`), frontend ở `src/frontend`,
giống 010 và khác đường dẫn cũ `src/backend/src/...` của plan 030. Module `profile` tự chứa đủ lớp;
điểm nối nằm ở `profile/port` và `profile/api`. `MediaStorage` đặt ở `common/media` vì 060, 070 cũng
cần lưu ảnh.

## Phases (tóm tắt để /speckit-tasks chia việc)

| Thứ tự | Nội dung | User story | Cần 010? |
|---|---|---|---|
| 1 | `V211`, `out-of-order`, entity, repository, `ProfileExceptionHandler`, `ProfileDefaultsConfig`, `ProfileWriteLimiter`; spike ảnh WebP trên Alpine | nền tảng | Không |
| 2 | `GET`, `PUT /me/dietary-profile`, `/me/dietary-options`, gợi ý nguyên liệu; hai adapter port, bỏ `StubDietaryProfile`; `PF02`, `PF03` | US1 (P1) | Không |
| 3 | `/me/profile`, `MediaStorage`, `AvatarImageProcessor`, `/media/avatars`, `/profiles/{id}`; `PF01`, `PF04`, `PF05` | US2 (P1) | Không |
| 4 | `DietRestrictions.hardRules`, `DietRuleContractIT` với dữ liệu 011 | US5 (P2) | Không |
| 5 | `DietaryCatalogChanges`, `dietary_notices`, `CatalogueNotices`, `AuthorDirectory` | FR-019, FR-007 | Không |
| 6 | Nối `AuditWriter`, `PublicRoutes`, `RequireAuth`, `useAuth`, `AdminAccountPanel`, `AvatarCleanupListener`, `DataBoundaryIT` với JWT thật | US2, FR-042 | **Có** (Phase 2, 3, 5 của 010; `AvatarCleanupListener` cần thêm Phase 7, 8) |
| 7 | Đổi, đặt mật khẩu; `PF06` | US3 (P2) | **Có** (Phase 8 của 010) |
| 8 | Xóa tài khoản, `closureToken`, hai port cleanup; `PF07`, `PF08`, `AccountClosurePage` | US4 (P2) | **Có** (Phase 8 của 010) |
| 9 | `profile.k6.js`, gap UI `011-A`…`M`, cập nhật `full-schema.md` | hoàn thiện | Có |

## Complexity Tracking

| Bổ sung | Lý do cần | Phương án đơn giản hơn bị loại vì |
|---|---|---|
| `com.twelvemonkeys.imageio:imageio-webp` | ImageIO của JDK 21 không đọc được WebP, mà FR-004 cho phép WebP và NFR-004 buộc giải mã ở server (R7) | Chỉ kiểm tra magic bytes – không phát hiện file giả, không xóa EXIF; bỏ WebP – trái FR-004; thư viện native – khó chạy trên Alpine |
| `MediaStorage` + volume `media` | Avatar là file, 060 và 070 cũng cần; test và đổi nơi lưu sau này không đụng service (R7) | Ghi `Files` thẳng trong service – mỗi module tự viết lại đường dẫn; lưu `BYTEA` trong DB – phình bảng `users` và backup |
| Bảng `dietary_notices` | FR-019 báo chủ hồ sơ ở lần vào sau, tên nguyên liệu đã xóa không còn trong catalogue (R15) | Tính lúc đọc – nguyên liệu đã xóa không còn dòng nào để so |
| Bảng `profile_write_requests` | NFR-008 không dùng Redis, theo hướng 010 R5 (R13) | Redis – thêm service; đếm trong bộ nhớ – sai khi nhiều instance |
| Cột `users.profile_version`, `users.dietary_version` | Edge case hai tab: tab cũ phải được báo; `users.updated_at` đổi cả khi đăng nhập nên không dùng được (R5) | So `updated_at` – báo xung đột giả; không kiểm tra – tab cũ ghi đè âm thầm |
| Entity thứ hai trên `users` (`UserProfile`) | 010 đã để các cột hồ sơ cho 011 (010 data-model); giữ ranh giới hai module (R4) | Dùng chung `UserAccount` – `profile` phụ thuộc `auth.domain` và 010 không chạy độc lập được |
| Plan lập khi spec còn Draft, chưa có mã Jira (ngoại lệ Nguyên tắc I) | Plan được lập để nhóm duyệt cùng spec; chính plan đã phát hiện các điểm spec phải sửa (research, bảng cuối) | Chờ duyệt rồi mới lập plan – nhóm phải duyệt spec còn mâu thuẫn với schema và 010. Ngoại lệ chỉ áp dụng cho việc lập plan, không áp dụng cho code: T001 chặn mọi task còn lại |
| `NamedParameterJdbcTemplate` trong `DietaryLinkRepository`, `ProfileWriteRequestRepository`, `UserCredentialRepository`, `AccountRowRepository` (ngoại lệ Nguyên tắc III) | Cần `ON CONFLICT`, `ARRAY(SELECT …)`, `pg_advisory_xact_lock`, `UPDATE` có điều kiện và `FOR UPDATE` trên bảng không map entity (R5, R10, R11, R13); cùng cách `RecipePickerQueryRepository` của 030 | Entity cho ba bảng nối – `DELETE` hết rồi `INSERT` lại, mất `created_at`; map `password_hash` vào entity – có đường lọt ra DTO |
| Stand-in JDBC đọc `recipes`, `ingredients`, `cuisines` | 060, 100 chưa có code; cùng cách `JdbcRecipeSummaryAdapter` của 030 (R8) | Chờ 060, 100 – US1, US2 không làm được |
| `spring.flyway.out-of-order: true` | `V211` thấp hơn `V403` đã chạy; 010 R11 đã cần cùng cấu hình, repo hiện chưa có (R2) | Đổi số thành `V405` – phá dải số; xóa DB – mất dữ liệu dev |
