---

description: "Task list for 010 User Authentication & Roles"
---

# Tasks: User Authentication & Roles

**Input**: Design documents from `specs/010-user-authentication-roles/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/openapi.yaml](contracts/openapi.yaml),
[contracts/integration.md](contracts/integration.md), [quickstart.md](quickstart.md)

**Tests**: BẮT BUỘC (constitution Principle II, spec SC-007). Trong mỗi phase, task test đứng
trước task code và **phải chạy FAIL trước** khi viết code (Red → Green → Refactor). Mỗi task test
ghi mã acceptance scenario (`US1-3`…) hoặc edge case (`Edge: Brute force`…) mà nó phủ.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- **⛓ A / B / C / D / E**: task chạm điểm nối với người khác (A: 011/080, B: 020/060, C: 030/031/090,
  D: 040/050, E: 070/100). Mỗi task ghi **cách làm trước** để không bị chặn, phần còn lại ở bảng
  "Phụ thuộc người khác".
- **🚧 CHẶN**: điểm trong "Điểm cần chốt với nhóm" (research.md) chưa có quyết định hoặc chưa được
  duyệt. Task này không tự quyết thay nhóm; làm xong mới mở các task nó chặn.

## Quy ước

- Backend là Maven multi-module; đường dẫn trong task là đường dẫn thật. Chưa có Maven wrapper, chạy
  `cd src/backend && mvn verify`.
- Test Java tên `*IT` chạy bằng Failsafe, kế thừa `AbstractPostgresIT` (`pgvector/pgvector:pg16`, không
  H2). Hỗ trợ test dùng chung ở `src/backend/wikicook-app/src/test/java/com/wikicook/support/`.
- Mọi bước dùng `Clock` chung ở `com.wikicook.common.ClockConfig`, để test cố định được thời gian.
- Secret chỉ ở biến môi trường; `.env.example` không có giá trị thật (constitution IV).
- Giao diện: class `wk-` của Soft Editorial (`docs/analysis-and-design/ui-ux/css/wikicook-soft-editorial.css`,
  đã nạp qua `src/frontend/src/styles/soft-editorial.css`), Tailwind chỉ dùng cho bố cục. Bản desktop theo
  màn `W-AU*`, mobile theo `AU*` trong `docs/analysis-and-design/ui-ux/screens/`. Màn chưa có
  (Gap 010-A…N) dựng từ cùng component.
- Task 010 chỉ cung cấp API và quy tắc cho 011, 100; màn hình và luồng của hai spec đó không nằm ở đây.

## Mã Jira (Sprint 04, `pa4-guide.md`)

| Mã | Phạm vi | Task | Người làm; người duyệt; hạn |
|---|---|---|---|
| SCRUM-72 | Tài liệu Spec Kit 010 + 011 | spec, plan, research, data-model, contracts, quickstart, tasks; T001, T002 | Hiển; Hưng; 13/11 |
| SCRUM-74 | Backend 010 | Mọi task sửa `src/backend/**`: T003–T009, T011–T022, T028–T047, phần backend của Phase 3–8, T170–T174, T176, T177, T184 | Hiển; Dự; 17/11 |
| SCRUM-76 | Frontend 010 + 011 | Mọi task sửa `src/frontend/**`: T010, T023–T027, T026a, T048–T055, T054a, phần frontend của Phase 3–8, T175, T180–T182, T185 | Hưng; Hiển; 18/11 |

- Task tài liệu và kiểm chứng (T178, T179, T183, T186–T189) ghi mã của phần chúng kiểm; T187 (thử với người dùng) chưa có mã trong Sprint 04.
- **Đề xuất phạm vi cho PA4, cần nhóm chốt**: SCRUM-74 trong `pa4-guide.md` chỉ yêu cầu "approved 010 slice" (đăng ký, đăng nhập, đăng xuất, phiên, băm mật khẩu, filter, phân quyền), tương ứng Phase 1–5 (MVP). Phase 6–9 (đặt lại mật khẩu, Google, email thông báo, job, polish) để sau 17/11, không tính là xong trong PA4.
- **Merge cùng lúc**: Phase 2 bật Spring Security và bỏ `X-User-Id`, nên backend SCRUM-74 và frontend SCRUM-76 (T023, T049–T054a) phải vào `main` cùng một đợt, sau Checkpoint US1. Cách làm: nhánh frontend tạo từ nhánh backend, hoặc hai PR merge liền nhau.
- Commit theo Conventional Commits kèm mã, ví dụ `feat(auth): add sign-in API (SCRUM-74)`, `feat(auth): add login page (SCRUM-76)`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Cổng trước khi code, dependency, biến môi trường, cấu hình test

- [x] T001 Mã Jira (research mục 4, đã có 2026-11-11): SCRUM-72 cho tài liệu, SCRUM-74 backend, SCRUM-76 frontend (mục "Mã Jira" dưới đây); đã thay `SCRUM-xx` ở specs/010-user-authentication-roles/spec.md và plan.md. Nhánh tài liệu `feature/SCRUM-72-spec-kit-010-011`; nhánh code tạo lúc bắt đầu T003 (SCRUM-74) và T010 (SCRUM-76)
- [ ] T002 🚧 CHẶN (research "Điểm cần chốt với nhóm" mục 1, 8, 9, 10, 11, 15, 16, 17, 23, 24, 26, 27, 30–34, 36, 37: đã có quyết định, chờ duyệt): PR SCRUM-72 được duyệt rồi đổi `Status` khỏi "In Review". `pa4-guide.md` ghi người duyệt SCRUM-72 là **Hưng**, còn các quyết định trên đang chờ **Duy** (PO): mời cả hai vào PR, trừ khi nhóm chốt khác. Nếu người duyệt đổi quyết định nào thì sửa spec, plan, research và file này trước. Chặn mọi task từ T003 trở đi
- [ ] T003 Thêm `spring-boot-starter-oauth2-resource-server`, `spring-boot-starter-oauth2-client`, `spring-boot-starter-mail` và test scope `spring-security-test`, `org.wiremock:wiremock-standalone` vào src/backend/wikicook-app/pom.xml (version do BOM và `dependencyManagement` của src/backend/pom.xml quản lý; lý do ở plan.md Complexity Tracking). Từ đây mọi `/api/**` chưa có JWT là 401, nên làm cùng nhánh với Phase 2, không merge riêng
- [ ] T004 Thêm rule PACKAGE cho `com.wikicook.auth.service`, counter LINE và BRANCH, `minimum` 0.70 vào src/backend/wikicook-app/pom.xml (R14, gate `v0.8`)
- [ ] T005 [P] Thêm vào src/backend/.env.example, không có giá trị thật: `WIKICOOK_JWT_SECRET` (≥ 32 byte), `WIKICOOK_EMAIL_HASH_KEY` (không đổi sau khi có dữ liệu), `WIKICOOK_PUBLIC_BASE_URL`, `WIKICOOK_AUTH_COOKIE_SECURE`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `WIKICOOK_MAIL_FROM`, `WIKICOOK_APPEAL_CONTACT`, `WIKICOOK_GOOGLE_CLIENT_ID`, `WIKICOOK_GOOGLE_CLIENT_SECRET`, `WIKICOOK_BOOTSTRAP_ADMIN_EMAIL`, `WIKICOOK_BOOTSTRAP_ADMIN_PASSWORD`, kèm chú thích cách sinh `openssl rand -base64 48`
- [ ] T006 [P] Sửa src/backend/docker-compose.yml: thêm service `mailpit` (image `axllent/mailpit`, cổng 1025, 8025), truyền các biến của T005 vào service `app` với `MAIL_HOST: mailpit`, `WIKICOOK_JWT_SECRET`, `WIKICOOK_EMAIL_HASH_KEY` dùng `${VAR:?…}` để báo lỗi rõ khi thiếu, `WIKICOOK_PUBLIC_BASE_URL` mặc định `http://localhost:3000`, `WIKICOOK_AUTH_COOKIE_SECURE` mặc định `false`, `MAIL_PORT: ${MAIL_PORT:-1025}`, `WIKICOOK_MAIL_FROM: ${WIKICOOK_MAIL_FROM:-no-reply@wikicook.local}`, `WIKICOOK_APPEAL_CONTACT: ${WIKICOOK_APPEAL_CONTACT:-${WIKICOOK_MAIL_FROM:-no-reply@wikicook.local}}`, `WIKICOOK_AUTH_SIGN_IN_IP_LIMIT: ${WIKICOOK_AUTH_SIGN_IN_IP_LIMIT:-100}`; **không** thêm `depends_on: mailpit` cho `app`; thêm `FLYWAY_OUT_OF_ORDER: "true"` cho service `migrate` (R5, R8, R11)
- [ ] T007 Sửa src/backend/wikicook-app/src/main/resources/application.yml: `spring.flyway.out-of-order: true`; `spring.mail.*` từ biến môi trường; `management.health.mail.enabled: false` để SMTP lỗi không làm `/actuator/health` DOWN (R8); `server.forward-headers-strategy: native` để `remoteAddr` đúng sau nginx; khối `wikicook.auth` (`jwt-secret`, `email-hash-key`, `public-base-url`, `cookie-secure` mặc định `true`, `bcrypt-strength` mặc định `12`, `access-token-ttl` `15m`, `mail-from` từ `WIKICOOK_MAIL_FROM`, `appeal-contact` từ `WIKICOOK_APPEAL_CONTACT` mặc định rỗng, `sign-in.ip-limit` từ `WIKICOOK_AUTH_SIGN_IN_IP_LIMIT` mặc định `100`, `jobs.enabled` mặc định `true`, `google.client-id`, `google.client-secret` từ biến môi trường, mặc định rỗng). **Không** khai báo `spring.security.oauth2.client.registration` (Spring Boot từ chối `client-id` rỗng lúc khởi động) và không khai báo `issuer-uri` (R7, NFR-005)
- [ ] T008 [P] Thêm giá trị dev vào src/backend/wikicook-app/src/main/resources/application-local.yml: `wikicook.auth.cookie-secure: false`, `public-base-url: http://localhost:5173`, mail tới `localhost:1025`
- [ ] T009 [P] Tạo src/backend/wikicook-app/src/test/resources/config/application.yml (bổ sung, không che `application.yml` chính): giá trị giả cố định chỉ dùng cho test, không phải secret thật, `wikicook.auth.bcrypt-strength: 4`, `wikicook.auth.jobs.enabled: false`, `wikicook.auth.sign-in.ip-limit: 10000` (mọi IT chạy từ 127.0.0.1 trên cùng container, R5), để 26 context test hiện có khởi động được và CI không cần biến môi trường
- [ ] T010 [P] Tạo thư mục src/frontend/src/features/auth/{api,pages,components,lib,__tests__} và src/frontend/src/features/auth/strings.vi.ts (nhóm chuỗi theo màn, câu lỗi theo `code`; "Email hoặc mật khẩu không đúng" cho `INVALID_CREDENTIALS`, NFR-006)

**Checkpoint**: Hai task 🚧 xong; dependency tải được. `mvn verify` tạm đỏ tới hết Phase 2 vì mọi route bị 401.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema, encoder, JWT, kiểm tra phiên mỗi request, `CurrentUser` thật, bỏ `X-User-Id`, client dùng chung – mọi user story đều cần

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Tests cho Foundational (viết trước, phải FAIL)

- [ ] T011 [P] Tạo hỗ trợ test: `AuthTestSupport` (tạo tài khoản theo vai trò, trạng thái, xác minh, kể cả với `id` cho trước; tạo dòng `user_sessions` và JWT thật; `resetThrottle()` xóa `login_attempts`, `email_send_requests`, `password_check_failures`, mọi IT của 010 gọi trong `@BeforeEach` vì container dùng chung, R5; **không** có hàm dọn `audit_logs`: bảng có trigger cấm UPDATE, DELETE, nên test đếm dòng audit phải lọc theo `target_id` hoặc `action` của dữ liệu do chính test tạo, R11) trong src/backend/wikicook-app/src/test/java/com/wikicook/support/AuthTestSupport.java, `RecordingMailSender` (`@Primary JavaMailSender` ghi lại thư) trong src/backend/wikicook-app/src/test/java/com/wikicook/support/RecordingMailSender.java
- [ ] T012 [P] Integration test `AuthSchemaIT` – `V102`–`V104` chạy được; vi phạm bị từ chối: `CHECK ((kind = 'BAN') = (ends_at IS NULL))`, `UNIQUE (user_id) WHERE lifted_at IS NULL`, `CHECK IN ('EMAIL_VERIFICATION','PASSWORD_RESET','ACCOUNT_CLOSURE')` của `one_time_tokens`, `CHECK IN ('EMAIL_VERIFICATION','PASSWORD_RESET')` của `email_send_requests`, `template` của `email_outbox`; xóa `users` → `login_attempts`, `email_outbox`, `user_sessions` bị `CASCADE`, `audit_logs` không đổi dòng nào (`actor_id`, `target_id` giữ nguyên, không có UPDATE: bảng có trigger cấm UPDATE, DELETE, `UPDATE` và `DELETE` trực tiếp đều bị từ chối), `CHECK ((lifted_at IS NULL) = (lift_reason IS NULL))` (FR-025) trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/repository/AuthSchemaIT.java
- [ ] T012a [P] Integration test `EntityMappingIT` (`ddl-auto: validate`, DB thật) – context khởi động được và đọc ghi đúng mọi cột đặc biệt: `CHAR(64)` (`token_hash`, `email_hash`, `previous_token_hash`), `INET` (`ip_address`, cả IPv4 và IPv6), `JSONB` (`details`, `payload`) (research R11) trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/repository/EntityMappingIT.java
- [ ] T013 [P] Integration test `FlywayOutOfOrderIT` – migrate tới `target = 403` rồi migrate tiếp với `outOfOrder = true`: `V102`–`V104` được áp dụng, validate không lỗi (R11) trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/repository/FlywayOutOfOrderIT.java
- [ ] T014 [P] Unit test `PrehashedBCryptPasswordEncoderTest` (FR-004, FR-007, NFR-001, R3) – 25 ký tự "ế" (75 byte) và 128 ký tự emoji (512 byte) encode không lỗi; hai mật khẩu chỉ khác ký tự thứ 100 không khớp; bản NFC và NFD của cùng mật khẩu khớp nhau; kết quả là chuỗi BCrypt 60 ký tự, cost 12 khi dùng mặc định; `Edge: Password with leading/trailing spaces`: `" abc12345 "` khác `"abc12345"` trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/security/PrehashedBCryptPasswordEncoderTest.java
- [ ] T015 [P] Unit test `BcryptBulkheadTest` – không quá `max(2, cores − 1)` thao tác chạy cùng lúc, permit được trả khi có exception (R14) trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/security/BcryptBulkheadTest.java
- [ ] T016 [P] Unit test `EmailNormalizerTest`, `EmailHasherTest` – `Edge: Email with leading/trailing spaces or mixed case`, FR-002: `" An@Mail.com "` → `an@mail.com`; `EmailHasher` ra 64 ký tự hex, ổn định, đổi khóa thì đổi hash, không chứa email rõ trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/service/EmailNormalizerTest.java và EmailHasherTest.java
- [ ] T017 [P] Unit test `AccessTokenServiceTest` – claim `sub`, `sid`, `iss`, `iat`, `exp`, **không có claim vai trò**; `exp` = 15 phút; chữ ký sai và token hết hạn bị từ chối (R1) trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/service/AccessTokenServiceTest.java
- [ ] T018 [P] Integration test `SessionCheckIT` (MockMvc, JWT thật) – bốn bước của R1 đúng thứ tự: không có dòng → 401 `UNAUTHENTICATED`; `status` khác `ACTIVE` → 403 `ACCOUNT_SUSPENDED`/`ACCOUNT_BANNED` kèm `reason`, `endsAt`, kể cả khi phiên đã thu hồi; `revoked_at` hoặc `expires_at` đã qua → 401; hợp lệ → authority theo `users.role` hiện tại, có `EMAIL_VERIFIED` khi đã xác minh; đúng một query cho mỗi request; `last_activity_at` chỉ đổi khi cũ hơn 5 phút trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/security/SessionCheckIT.java
- [ ] T019 [P] Unit test `SecurityContextCurrentAccountTest` – `id()`, `role()`, `emailVerified()`, `sessionId()` lấy từ principal; không đăng nhập → `UnauthenticatedException` (401 `UNAUTHENTICATED`) trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/security/SecurityContextCurrentAccountTest.java
- [ ] T019a [P] ⛓ B Unit test `AuthSearchViewerTest` – khách hoặc chưa đăng nhập → `Optional.empty()` (020 AS 3.4: khách thấy kết quả không lọc); người đăng nhập → id của họ; header `X-Dev-User-Id`, `X-Test-User` không có tác dụng trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/security/AuthSearchViewerTest.java
- [ ] T020 [P] Web slice test `AuthExceptionHandlerTest` – mỗi exception map đúng status + `code` trong enum của contracts/openapi.yaml, `application/problem+json`, `errors: [{ field, code }]` cho `VALIDATION_ERROR` với mã ở mô tả `errors[].code` của openapi (NFR-007), không có stack trace hay tên class (constitution IV); `AccessDeniedException` ném từ controller ở **gói khác `auth`** ra 403 `ACCESS_DENIED`, không thành 500 dù có `GlobalExceptionHandler`; handler có `@VerifiedEmailRequired` với người chưa xác minh ra 403 `EMAIL_NOT_VERIFIED`; lỗi validation ở controller của 030 vẫn do `MealPlanExceptionHandler` trả, đúng định dạng cũ (R15) trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/AuthExceptionHandlerTest.java
- [ ] T021 [P] Integration test `SecurityFilterChainIT` – không có token → 401 ProblemDetail ghi từ filter; `/actuator/health` và tài liệu OpenAPI mở; khách (không token) gọi `GET /api/v1/recipes/search`, `/api/v1/recipes/search/options`, `/api/v1/recipes/featured` của 020 nhận 200, không 401 (020 AS 3.4, R10); route ngoài `/api/**` không khai báo bị từ chối; route `/api/v1/**` không tồn tại: không token → 401 `UNAUTHENTICATED`, có JWT thật → 404 `application/problem+json`; không tạo `HttpSession`; thiếu `X-WikiCook-Client` ở `/auth/refresh`, `/auth/logout` → 403 `INVALID_CLIENT`; JWT hết hạn hoặc chữ ký sai gửi kèm `/auth/refresh`, `/auth/login` không làm 401, request xử lý như không có bearer (R10); `/actuator/health` vẫn `UP` khi `spring.mail.host` trỏ tới máy không tồn tại (R8); phản hồi API có `Cache-Control: no-store` trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/security/SecurityFilterChainIT.java
- [ ] T022 [P] ⛓ D Sửa `ShoppingListControllerTest` (13 chỗ dùng `X-User-Id`): mock `CurrentUser`, `addFilters = false`, thêm ca gửi `X-User-Id` khác người đăng nhập vẫn dùng `CurrentUser.id()` trong src/backend/wikicook-app/src/test/java/com/wikicook/shoppinglist/controller/ShoppingListControllerTest.java. Cách làm trước: sửa thẳng ở nhánh 010, báo D review
- [ ] T023 [P] Bổ sung `client.test.ts` và `session.test.ts` (FR-010, FR-024, R12) – `main` đã có `addIdentity` gửi `X-User-Id`, `X-Dev-User-Id` từ `getDevUserId()` (ACT-01): không còn header nào trong hai header đó, không còn `getDevUserId`, `setDevUserId`, `GUEST`; gắn `Authorization` từ `session`, nhưng **không** gắn cho `/auth/refresh`, `/auth/login`, `/auth/logout`, `/auth/register` và các `/auth/*` công khai khác (chỉ `/auth/me`, `/auth/role-notice/ack`, `/auth/reauthenticate` có bearer); refresh thành công phát `auth:refreshed` kèm `account`; 401 ở route ngoài `/auth/*` → refresh một lần rồi gửi lại đúng một lần; refresh lỗi → ném lại 401 gốc code `UNAUTHENTICATED`; 5 request 401 đồng thời chỉ gọi 1 lần refresh; 403 `ACCOUNT_SUSPENDED`, `ACCOUNT_BANNED` xóa token và phát sự kiện trong src/frontend/src/shared/api/client.test.ts
- [ ] T024 [P] Test `safeRedirect.test.ts` – `Edge: Redirect after sign-in`, FR-011: nhận `/meal-planner?week=2026-10-26`; về `/` với `//evil.com`, `/\evil.com`, `https://evil.com`, `javascript:alert(1)`, chuỗi có ký tự điều khiển, `/login` trong src/frontend/src/shared/api/safeRedirect.test.ts
- [ ] T025 [P] Test `AuthProvider.test.tsx` – `loading` → `authenticated` khi refresh thành công, → `guest` khi 401 lúc khởi động; `Edge: No connection when the app opens`: lỗi mạng hoặc 5xx → `offline`, không về `guest`; sự kiện `online` gọi lại refresh; khi vẫn online mà backend trả 5xx thì thử lại theo thời gian (5, 15, 30 giây rồi mỗi 30 giây, fake timer) và về `authenticated` khi backend chạy lại; `auth:refreshed` từ client đưa `offline` về `authenticated`; 401 sau khi đã `authenticated` hoặc `offline` → `expired`; không render route cần đăng nhập trong `loading`; `signOut()` chờ các guard đã đăng ký (có guard trả `false` thì hủy), rồi xóa token và báo tab khác qua `BroadcastChannel('wikicook-auth')` trong src/frontend/src/features/auth/__tests__/AuthProvider.test.tsx
- [ ] T026 [P] Test `guards.test.tsx` – `RequireAuth` chuyển khách tới `/login?redirect=` + `encodeURIComponent(path + search)`; `offline` render trang con, không chuyển hướng (`Edge: No connection when the app opens`, 030 FR-028, 040 FR-020); `expired` cũng render trang con, không chuyển hướng ở mọi route (`Edge: Session expires while filling a form`; route không `offlineFirst` để `SessionExpiredDialog` hỏi đăng nhập lại, route `offlineFirst` để feature hiện banner); khách mới (`guest`) vẫn bị chuyển `/login`; `Edge: Already signed in`: `RedirectIfAuthenticated` đưa người đã đăng nhập khỏi `/login`, `/register` về `/` trong src/frontend/src/features/auth/__tests__/guards.test.tsx
- [ ] T026a [P] ⛓ C Sửa src/frontend/src/features/meal-planner/__tests__/MealPlannerPage.auth.test.tsx và AddToPlanButton.test.tsx – `Edge: Session expires while filling a form`, `Edge: No connection when the app opens`: người đang đăng nhập nhận 401 `UNAUTHENTICATED` (refresh lỗi, `status = expired`) thì **không** chuyển `/login`, trang giữ nguyên và `SessionExpiredDialog` hỏi đăng nhập lại; khách (`status = guest`) vẫn chuyển `/login?redirect=` như cũ (030 US2-4, R12). Cách làm trước: sửa ở nhánh 010, báo C review
- [ ] T027 [P] Test `TextField.test.tsx`, `PasswordField.test.tsx` – nhãn hiển thị gắn với input, lỗi nối bằng `aria-describedby`, dùng class `wk-field`, `wk-input`, `wk-error`, `is-error`; nút hiện/ẩn mật khẩu thao tác được bằng bàn phím (NFR-007) trong src/frontend/src/shared/ui/TextField.test.tsx và PasswordField.test.tsx

### Implementation cho Foundational

- [ ] T028 ⛓ E Viết src/backend/wikicook-db/src/main/resources/db/migration/V102__audit_and_suspensions.sql đúng data-model.md: `audit_logs` (`actor_id BIGINT` **không FK**, `action VARCHAR(50) NOT NULL`, `target_type VARCHAR(30)`, `target_id BIGINT`, `details JSONB`, `ip_address INET`, `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`; index `(created_at DESC)`, `(target_type, target_id)`; trigger `BEFORE UPDATE OR DELETE` ném `audit_logs is append-only and cannot be updated or deleted`) và `user_suspensions` (`kind VARCHAR(10) NOT NULL CHECK IN ('SUSPENSION','BAN')`, `reason VARCHAR(500) NOT NULL`, `starts_at`, `ends_at` với `CHECK ((kind = 'BAN') = (ends_at IS NULL))`, `created_by`, `lifted_at`, `lifted_by`, `lift_reason VARCHAR(20) CHECK IN ('EXPIRED','LIFTED','REPLACED_BY_BAN')` với `CHECK ((lifted_at IS NULL) = (lift_reason IS NULL))`; `UNIQUE (user_id) WHERE lifted_at IS NULL`; index `(ends_at) WHERE lifted_at IS NULL AND kind = 'SUSPENSION'`). Cách làm trước: tạo ngay vì `V503` của E chưa có; E bỏ hai bảng khỏi `V503` (research mục 7)
- [ ] T029 Viết src/backend/wikicook-db/src/main/resources/db/migration/V103__auth_schema_updates.sql: `blocked_emails (email_hash CHAR(64) PRIMARY KEY, created_at)`; `password_check_failures (id, user_id FK CASCADE, created_at)`, index `(user_id, created_at DESC)`; `users.pending_role_notice_from VARCHAR(15)` với `CHECK` như `role`; index `users (created_at) WHERE email_verified_at IS NULL`; `user_sessions` thêm `previous_token_hash CHAR(64)`, `rotated_at`, `reauthenticated_at` và index `(previous_token_hash) WHERE previous_token_hash IS NOT NULL`, `(expires_at)`; `login_attempts` bỏ `email_normalized`, thêm `email_hash CHAR(64) NOT NULL`, index `(email_hash, created_at DESC)`, `(ip_address, created_at DESC)`; `one_time_tokens.purpose` đổi `CHECK` thành `IN ('EMAIL_VERIFICATION','PASSWORD_RESET','ACCOUNT_CLOSURE')`
- [ ] T030 [P] Viết src/backend/wikicook-db/src/main/resources/db/migration/V104__auth_mail.sql: `email_send_requests (id, email_hash CHAR(64) NOT NULL, purpose VARCHAR(20) NOT NULL CHECK IN ('EMAIL_VERIFICATION','PASSWORD_RESET'), created_at)`, index `(email_hash, purpose, created_at DESC)`; `email_outbox (id, user_id NOT NULL FK CASCADE, template VARCHAR(30) NOT NULL CHECK IN ('ACCOUNT_LOCKED','ACCOUNT_SUSPENDED','ACCOUNT_BANNED','SUSPENSION_ENDED','BAN_LIFTED'), payload JSONB NOT NULL, attempts SMALLINT NOT NULL DEFAULT 0, next_attempt_at NOT NULL, sent_at, failed_at, created_at)`, index `(next_attempt_at) WHERE sent_at IS NULL AND failed_at IS NULL`
- [ ] T031 [P] Tạo `Role` (MEMBER, CONTRIBUTOR, MODERATOR, ADMINISTRATOR), `AccountStatus` (ACTIVE, SUSPENDED, BANNED), `CurrentAccount extends CurrentUser` (giữ nguyên `id()`, thêm `role()`, `emailVerified()`, `sessionId()`) trong src/backend/wikicook-app/src/main/java/com/wikicook/common/security/ (FR-020)
- [ ] T032 [P] Tạo meta-annotation `@VerifiedEmailRequired`, `@ModeratorOnly`, `@AdministratorOnly` trong src/backend/wikicook-app/src/main/java/com/wikicook/common/security/ (FR-006, FR-021)
- [ ] T033 Tạo entity `UserAccount` (chỉ cột của 010, không map `avatar_storage_key`, `diet_type`, `household_size`, `max_cook_time_minutes`, có `@DynamicUpdate`), `UserSession`, `OneTimeToken`, `LoginAttempt`, `AuditLog`, `UserSuspension`, `BlockedEmail`, `PasswordCheckFailure`, `EmailSendRequest`, `EmailOutboxMessage` và enum `TokenPurpose`, `SanctionKind`, `LiftReason`, `MailTemplate` đúng data-model.md mục 1, theo quy ước map kiểu ở đó (`CHAR(64)` → `@JdbcTypeCode(SqlTypes.CHAR)` + `@Column(length = 64)`, `JSONB` → `@JdbcTypeCode(SqlTypes.JSON)`, `INET` → `@JdbcTypeCode(SqlTypes.INET)` với `InetAddress`) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/domain/ (phụ thuộc T028, T029, T030, T031; làm T012, T012a xanh)
- [ ] T034 Tạo các repository trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/repository/, gồm `UserSessionRepository.findSnapshot(sessionId)` trả `SessionSnapshot` (`user_sessions` JOIN `users` theo PK, `LEFT JOIN` án đang chạy)
- [ ] T035 [P] Tạo `AuthProperties` (`@ConfigurationProperties("wikicook.auth")`, `@Validated`: `jwt-secret` ≥ 32 byte, `email-hash-key` không rỗng; `appealContact()` trả `mail-from` khi `appeal-contact` rỗng hoặc chỉ có khoảng trắng, vì Compose truyền biến trống thành chuỗi rỗng, R8; `sign-in.ip-limit`) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/config/AuthProperties.java
- [ ] T036 [P] Tạo `EmailNormalizer` (`lower(trim)`), `EmailHasher` (HMAC-SHA256, `WIKICOOK_EMAIL_HASH_KEY`) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/service/ (làm T016 xanh)
- [ ] T037 [P] Tạo `PrehashedBCryptPasswordEncoder` (NFC → UTF-8 → SHA-256 → Base64 → `BCryptPasswordEncoder(strength)`) và `BcryptBulkhead` (`Semaphore(max(2, cores − 1))`, bọc cả encode, matches, `dummyHash`) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/security/ (làm T014, T015 xanh)
- [ ] T038 [P] Tạo `AccessTokenService` dùng `NimbusJwtEncoder`/`NimbusJwtDecoder`, HS256, 15 phút trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/service/AccessTokenService.java (làm T017 xanh)
- [ ] T039 Tạo `AuditService` implements `AuditWriter` (sau T033, T034), `details` từ chối key `email`, `displayName`; ghi cùng transaction của lời gọi; phần đọc `search` thêm ở T119 vì cần `AuditLogDto` của T115, trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/service/AuditService.java và interface trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/api/AuditWriter.java (FR-025)
- [ ] T040 Tạo `ProblemResponseWriter`, exception domain của 010, `AuthExceptionHandler` (`@RestControllerAdvice(basePackages = "com.wikicook.auth")`, mọi `code` ở contracts/openapi.yaml, gồm `VALIDATION_ERROR` có `errors[]`) và `SecurityExceptionAdvice` (`@RestControllerAdvice` **toàn cục**, `@Order(Ordered.HIGHEST_PRECEDENCE)`, **chỉ** bắt `AccessDeniedException`, chọn `EMAIL_NOT_VERIFIED` hay `ACCESS_DENIED` theo `@VerifiedEmailRequired` trên handler method), để `GlobalExceptionHandler` không biến lỗi thiếu quyền thành 500 mà vẫn không đụng lỗi validation của 030, 031, 040 (R15) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/security/ProblemResponseWriter.java và src/backend/wikicook-app/src/main/java/com/wikicook/auth/controller/AuthExceptionHandler.java (R15, làm T020 xanh)
- [ ] T041 Tạo `SessionJwtAuthenticationConverter` theo bốn bước R1 và cập nhật hoạt động tối đa 5 phút một lần (`UPDATE … WHERE last_activity_at < :threshold`) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/security/SessionJwtAuthenticationConverter.java (FR-010, FR-016, FR-024; làm T018 xanh)
- [ ] T042 Tạo bean thật `SecurityContextCurrentAccount` cho `CurrentUser` và `CurrentAccount` trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/security/SecurityContextCurrentAccount.java; `ObjectProvider<CurrentUser>` của 030, 031 nhận bean này, `DevStubConfig` tự lùi nhờ `@ConditionalOnMissingBean` (làm T019 xanh)
- [ ] T042a ⛓ B Tạo `AuthSearchViewer` implements `com.wikicook.search.port.SearchViewerPort` (`currentUserId()` = id của người đăng nhập, rỗng khi khách hoặc không có JWT hợp lệ, không đọc header nào) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/security/AuthSearchViewer.java; bean thật thắng `devHeaderSearchViewer` và `guestSearchViewer` của `SearchPortsConfig` nhờ `@ConditionalOnMissingBean`; làm T019a xanh. Cách làm trước: sửa ở nhánh 010, báo B (020) review
- [ ] T043 Tạo `SecurityConfig` (stateless, tắt CSRF filter cho `/api/**`, `oauth2ResourceServer` với converter, `@EnableMethodSecurity`, `RoleHierarchy` `ADMINISTRATOR > MODERATOR > CONTRIBUTOR > MEMBER`, entry point và access-denied handler qua `ProblemResponseWriter`), `PublicRoutes` (`/api/v1/auth/**` trừ `me`, `role-notice`, `reauthenticate`; `GET` `GET /api/v1/recipes/search`, `/api/v1/recipes/search/options`, `/api/v1/recipes/featured` của 020, đã có ở `main`; `/actuator/health`; tài liệu OpenAPI; mặc định từ chối), `ClientHeaderFilter` (`X-WikiCook-Client: web`), `PublicRouteBearerTokenResolver` (bỏ qua `Authorization` ở route của `PublicRoutes`, dùng cho `oauth2ResourceServer`) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/security/ (FR-021, R10; làm T021 xanh)
- [ ] T044 ⛓ C Chuyển `HeaderCurrentUser` từ src/backend/wikicook-app/src/test/java/com/wikicook/mealplan/support/ sang src/backend/wikicook-app/src/test/java/com/wikicook/support/HeaderCurrentUser.java, thêm `@Primary` và `SecurityFilterChain` `HIGHEST_PRECEDENCE` permit-all chỉ cho test; đổi import ở 5 IT của 030 (`MealPlanControllerIT`, `MealPlanControllerUS4IT`, `PickerControllerIT`, `ShoppingHandoffControllerIT`, `UnavailableRecipeIT`), giữ nguyên `X-Test-User` (R13). Cách làm trước: sửa ở nhánh 010, báo C review
- [ ] T045 ⛓ C Sửa src/backend/wikicook-app/src/test/java/com/wikicook/aiplanner/controller/AiPlannerControllerUS1IT.java: bỏ `wikicook.dev-stubs.enabled`, dùng `HeaderCurrentUser.HEADER` thay `StubCurrentUser.HEADER` (4 chỗ). Cùng cách cho `AiPlannerControllerUS2IT`, `DraftSafetyIT`, `NotEnoughRecipesIT`, `RateLimitIT` (cùng thư mục) và src/backend/wikicook-app/src/test/java/com/wikicook/search/controller/RecipeSearchControllerIT.java (đang dùng `wikicook.dev-stubs.enabled` và `X-Dev-User-Id`; với tìm kiếm, người dùng đăng nhập là JWT thật của `AuthTestSupport` hoặc `HeaderCurrentUser` qua `AuthSearchViewer`, khách là không gửi gì, ca header `abc` → 400 bỏ vì header dev không còn). `StubCurrentUser.id()` hiện tự tạo dòng `users` cho **mọi** header, nên mọi user của test (cả `NEXT_USER`) phải được `AuthTestSupport` tạo với đúng `id` trước request, không chỉ chỗ `provisioner.ensure` ở dòng 244; xóa trường `provisioner` vì T170 xóa lớp này; `invalidDevUserHeaderIs400` mất ý nghĩa (header test không còn là đầu vào của người dùng, `HeaderCurrentUser` để `parseLong` ném lỗi): xóa test này, phần "không có người đăng nhập → 401" đã có ở `WikiCookApplicationIT`; sửa `AiPlannerExceptionHandlerTest`, `MealPlanExceptionHandlerTest` (`@WebMvcTest`) sang `addFilters = false` (R13). Cách làm trước: như T044
- [ ] T045a [P] ⛓ C Sửa src/backend/wikicook-app/src/test/java/com/wikicook/WikiCookApplicationIT.java: `unknownPathReturnsProblemDetail` đổi kỳ vọng từ 404 sang 401 `UNAUTHENTICATED` vì `/api/**` mặc định từ chối (404 được T021 kiểm tra bằng JWT thật); `aiPlannerWithoutSignedInUserIs401` giữ nguyên, sửa chú thích (R13). Cách làm trước: như T044
- [ ] T046 [P] ⛓ D Đổi `X-User-Id` thành `X-Test-User` và import `HeaderCurrentUser` trong src/backend/wikicook-app/src/test/java/com/wikicook/shoppinglist/F1_CrossRequestImportMergesIT.java, F3_GetActiveCreatesListForNewUserIT.java, F4_ArchiveRestoreIT.java (R13). Cách làm trước: như T022
- [ ] T047 ⛓ D Bỏ mọi `@RequestHeader("X-User-Id")` trong src/backend/wikicook-app/src/main/java/com/wikicook/shoppinglist/controller/ShoppingListController.java, lấy `CurrentUser.id()`, Service giữ nguyên, để người dùng không mạo danh được nhau (làm T022 xanh)
- [ ] T048 Thêm handler mặc định `POST /api/v1/auth/refresh` → 401 trong src/frontend/src/test/server.ts, vì `setup.ts` đang `onUnhandledRequest: 'error'`; thêm `/api/v1/auth/refresh`, `/api/v1/auth/me` (tài khoản giả) vào src/frontend/e2e/fakeApi.ts để e2e 030 chạy tiếp
- [ ] T049 [P] Sửa src/frontend/src/shared/api/session.ts (đã có ở `main`, ACT-01): bỏ phần danh tính dev (`getDevUserId`, `setDevUserId`, `GUEST`, khóa `wk.devUserId`); giữ `getAccessToken`, `setAccessToken` nhưng đổi chỗ lưu từ `localStorage` (khóa `wk.accessToken`) sang biến trong bộ nhớ, và xóa khóa cũ nếu còn (R2); sửa các test đang gọi `getDevUserId` trong src/frontend/src/features/cooking-mode/__tests__/ (`alarmCoordinator.test.ts`, `CookingLifecycle.regression.test.tsx`, `sessionPersistence.test.ts`) và src/frontend/src/shared/api/session.test.ts (làm T023 xanh)
- [ ] T050 [P] Tạo `safeRedirect.ts` theo R12 trong src/frontend/src/shared/api/safeRedirect.ts (làm T024 xanh)
- [ ] T051 Sửa src/frontend/src/shared/api/client.ts: bỏ `addIdentity` dev (`X-User-Id`, `X-Dev-User-Id`, `getDevUserId`), chỉ còn bearer từ `session.ts` (T049), gắn bearer, `credentials: 'same-origin'`, `X-WikiCook-Client: web` ở `/auth/*`, `refreshPromise` single-flight, gửi lại đúng một lần, sự kiện cho 403 trạng thái tài khoản, báo riêng lỗi mạng hoặc 5xx của refresh, không đổi thành 401 (làm T023 xanh). `api.postForm` (011 T025) cũng sửa file này: bên merge sau rebase
- [ ] T052 [P] Tạo type khớp contracts/openapi.yaml (`Role`, `CurrentAccount`, `AuthResponse`, `RoleNotice`, `Problem` theo `code`) trong src/frontend/src/features/auth/types.ts
- [ ] T052a [P] Tạo `authApi.ts` với `refresh`, `me` cho `AuthProvider` trong src/frontend/src/features/auth/api/authApi.ts
- [ ] T053 Tạo `AuthProvider.tsx`, `useAuth.ts` (Context + `useReducer`, `loading | guest | authenticated | offline | expired`, nghe sự kiện `online` và thử lại theo thời gian khi `offline`, nghe `auth:refreshed` của client, có `addSignOutGuard(fn)`, dùng `authApi.ts` của T052a), bọc `RouterProvider` trong src/frontend/src/app/App.tsx (làm T025 xanh)
- [ ] T054 Tạo `RequireAuth.tsx`, `RequireRole.tsx`, `RedirectIfAuthenticated.tsx` trong src/frontend/src/features/auth/; `RequireAuth` chỉ chuyển `/login` khi `guest`, có prop `offlineFirst` để `SessionExpiredDialog` biết route nào để feature tự hiện banner; bọc `/meal-planner` bằng `RequireAuth`, `/shopping-list`, `/shopping-list/history` bằng `RequireAuth offlineFirst` trong src/frontend/src/app/routes.tsx; `/` (`HomePage`), `/search` và `cooking/:recipeId` (chế độ nấu, 050) **không** bọc: khách được dùng, và `cooking/:recipeId` nằm ngoài `AppLayout` nên không có `SessionExpiredDialog`, chế độ nấu chạy ở client (050 NFR-004); cập nhật src/frontend/src/app/App.test.tsx (làm T026 xanh)
- [ ] T054a ⛓ C Sửa `signIn` của src/frontend/src/features/meal-planner/pages/MealPlannerPage.tsx và src/frontend/src/features/meal-planner/components/AddToPlanButton.tsx: chỉ chuyển `/login?redirect=` khi `useAuth().status === 'guest'`; khi `expired` không làm gì, để `SessionExpiredDialog` của T077 xử lý (làm T026a xanh; dialog hiện được sau T077). Cách làm trước: như T026a
- [ ] T055 [P] Tạo `TextField.tsx`, `PasswordField.tsx` trong src/frontend/src/shared/ui/ (làm T027 xanh)

**Checkpoint**: `mvn verify` xanh trở lại, gồm test của 030, 031, 040; `npm test` xanh; `/api/**` không có JWT là 401; Q44 (a), (b) đạt phần 030; chưa có đường đăng nhập, nên **chưa merge vào `main`** trước Checkpoint của US1.

---

## Phase 3: User Story 1 - Sign in with email and password (Priority: P1) 🎯 MVP

**Goal**: Người dùng đã có tài khoản đăng nhập, ở lại phiên theo "Ghi nhớ", đăng xuất có hiệu lực ngay; sai mật khẩu không lộ thông tin; 5 lần sai thì khóa.

**Independent Test**: Tạo sẵn tài khoản đã xác minh, mở trang đăng nhập, nhập đúng → về trang chủ có tên; đăng xuất → trang thành viên không vào được (quickstart Q10–Q18).

### Tests for User Story 1 ⚠️ (viết trước, phải FAIL)

- [ ] T056 [P] [US1] Integration test `SignInThrottleIT` – `Edge: Brute force`, FR-012, SC-005: 5 lần sai liên tiếp trong 15 phút khóa 15 phút, lần sai thứ 5 đã trả 423 `ACCOUNT_LOCKED` kèm `retryAt`; lần thử khi đang khóa không gia hạn; mở lại đúng 15 phút sau lần sai thứ 5 (`Clock` cố định); lần sai cách nhau quá 15 phút không khóa; đăng nhập đúng reset bộ đếm; 10 request song song không quá 5 lần so mật khẩu; email chưa đăng ký khóa giống hệt, `users.locked_until` chỉ ghi khi có tài khoản; với `sign-in.ip-limit = 100` (đặt riêng cho test này), lần thứ 101 cùng IP trong 15 phút → 429 `TOO_MANY_REQUESTS` có `Retry-After` và `retryAfterSeconds` (FR-012, R5) trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/service/SignInThrottleIT.java
- [ ] T057 [P] [US1] Unit test `SessionServiceTest` – FR-009: `remember_me = true` → `expires_at = created_at + 30 ngày`, không gia hạn; `false` → trượt `last_activity_at + 24 giờ`, trần `created_at + 30 ngày`; token 256 bit, chỉ lưu SHA-256; xoay vòng đưa hash cũ vào `previous_token_hash`; gửi lại hash cũ trong 10 giây → access token mới, không đổi cookie; sau 10 giây → thu hồi phiên, audit `REFRESH_TOKEN_REUSED` trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/service/SessionServiceTest.java
- [ ] T058 [P] [US1] Unit test `SignInServiceTest` – FR-007, FR-008: `password_hash` NULL và email lạ đều so với `dummyHash` đúng một lần; kiểm tra `status` chỉ sau khi mật khẩu đúng (FR-013); `SIGN_IN_FAILED` với email lạ có `target_id = NULL`, `details.emailHash` (FR-025); không gọi Google (NFR-005) trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/service/SignInServiceTest.java
- [ ] T059 [P] [US1] Integration test `SignInIT` – US1-1 (200, `accessToken`, `Set-Cookie: wc_rt` có `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/api/v1/auth`), US1-2, US1-3 (cùng status, `code`, body, 401 `INVALID_CREDENTIALS`, FR-008), US1-7 + `Edge: Email with leading/trailing spaces or mixed case`, `Edge: Password with leading/trailing spaces`, `Edge: Very long input` (email > 254, password > 128 code point → 400, không tính lần thử, FR-007; mật khẩu 128 emoji đã đăng ký vẫn đăng nhập được, R3), `Edge: Suspended / banned account` (đúng mật khẩu → 403 kèm `reason`, `endsAt`; sai mật khẩu → 401, FR-013), audit `SIGN_IN_SUCCEEDED`, `SIGN_IN_FAILED`, `ACCOUNT_LOCKED` (FR-025), `last_sign_in_at` được ghi, outbox có `ACCOUNT_LOCKED` chỉ khi có tài khoản trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/SignInIT.java
- [ ] T060 [P] [US1] Integration test `SessionLifecycleIT` – US1-4 + FR-010 (đăng xuất, JWT cũ còn hạn → 401, audit `SIGNED_OUT`), US1-5 (cookie có `Max-Age`, refresh được trong 30 ngày), US1-6 (cookie không `Max-Age`; quá 24 giờ không hoạt động → 401), `Edge: Simultaneous sessions` (đăng xuất một thiết bị, thiết bị kia vẫn dùng được), refresh hai lần đồng thời không tự đăng xuất, thiếu cookie → 401 `UNAUTHENTICATED` trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/SessionLifecycleIT.java
- [ ] T061 [P] [US1] Integration test `SignInTimingIT` – NFR-003: với `bcrypt-strength = 10`, trung vị thời gian phản hồi của sai mật khẩu, email lạ và tài khoản không có mật khẩu lệch nhau không quá 20% (50 mẫu mỗi nhóm; mỗi mẫu một email hoặc tài khoản riêng để không chạm khóa 5 lần, `resetThrottle()` giữa các nhóm để không chạm giới hạn IP, R5) trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/SignInTimingIT.java
- [ ] T062 [P] [US1] Test `LoginPage.test.tsx` – US1-1 (về đúng `redirect`), US1-2 (lỗi chung, ô mật khẩu bị xóa), US1-3, US1-5 (gửi `rememberMe`), `Edge: Redirect after sign-in`, 423 chuyển tới màn khóa, 403 tới `/account-status`, nhãn và thông báo tiếng Việt trong src/frontend/src/features/auth/__tests__/LoginPage.test.tsx
- [ ] T063 [P] [US1] Test `AccountStatusPage.test.tsx` – `Edge: Brute force` (giờ thử lại, câu trung tính "Nếu email này đã đăng ký, chúng tôi đã gửi thông báo", nút "Đặt lại mật khẩu"), `Edge: Suspended / banned account` (đến ngày, lý do; bị cấm không có ngày), dữ liệu lấy từ state của router (đăng nhập email) hoặc fragment `#code=…&reason=…&endsAt=…` (Google), fragment bị xóa khỏi URL sau khi đọc (FR-013, R7) trong src/frontend/src/features/auth/__tests__/AccountStatusPage.test.tsx
- [ ] T064 [P] [US1] Test `AppLayout.test.tsx` – US1-1 (hiện tên), US1-4 (`signOut` xóa token, `location.replace('/')`, `pageshow` có `persisted` kiểm tra lại phiên) trong src/frontend/src/app/AppLayout.test.tsx
- [ ] T065 [P] [US1] Test `SessionExpiredDialog.test.tsx` – `Edge: Session expires while filling a form`: render `AppLayout` với một route có form và `RequireAuth`; `status` chuyển `expired` (401 sau khi refresh lỗi) thì dialog hiện, **không** chuyển `/login`, nội dung form giữ nguyên; "Đăng nhập lại" đi tới `/login?redirect=` và form có `useBlocker` cảnh báo trước khi rời; route `offlineFirst` không hiện dialog trong src/frontend/src/features/auth/__tests__/SessionExpiredDialog.test.tsx
- [ ] T066 [P] [US1] E2E Playwright trên `fakeApi`: đăng nhập, quay lại `redirect`, đăng xuất, Back không hiện nội dung (US1-1, US1-4) trong src/frontend/e2e/auth.spec.ts

### Implementation for User Story 1

- [ ] T067 [P] [US1] Tạo record `LoginRequest`, `AuthResponse`, `CurrentAccountDto`, `RoleNoticeDto` trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/dto/ và `@MaxCodePoints` (đếm code point, không dùng `@Size` cho mật khẩu, R3) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/validation/MaxCodePoints.java; `LoginRequest.toString()` che mật khẩu (NFR-001)
- [ ] T068 [US1] Cài `SessionService` (tạo, xoay vòng với `SELECT … FOR UPDATE`, khoảng 10 giây, thu hồi một phiên, thu hồi tất cả) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/service/SessionService.java (làm T057 xanh)
- [ ] T069 [US1] Cài `SignInThrottle` theo R4: transaction ngắn có `pg_advisory_xact_lock(hashtextextended(email_hash, 0))`, kiểm tra IP, kiểm tra khóa, chèn trước `login_attempts`, chỉ nguồn quyết định khi đăng nhập là `login_attempts` trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/service/SignInThrottle.java (làm T056 xanh)
- [ ] T070 [US1] Cài `SignInService`: **không giữ transaction trong lúc chạy BCrypt**; nhánh sai ghi `locked_until`, outbox `ACCOUNT_LOCKED`, audit; nhánh đúng đổi `success = true`, xóa `locked_until`, kiểm tra `status`, tạo phiên, ghi `last_sign_in_at`, audit trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/service/SignInService.java (làm T058 xanh)
- [ ] T071 [P] [US1] Tạo `RefreshCookie` (`wc_rt`, `HttpOnly`, `Secure` theo `cookie-secure`, `SameSite=Strict`, `Path=/api/v1/auth`, `Max-Age` khi `remember_me`) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/security/RefreshCookie.java
- [ ] T072 [US1] Tạo `AuthController` (`POST /auth/login`, `/auth/refresh`, `/auth/logout`, `GET /auth/me`; đăng xuất ghi audit `SIGNED_OUT`) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/controller/AuthController.java (làm T059, T060, T061 xanh)
- [ ] T073 [P] [US1] Thêm `login`, `logout` vào `authApi.ts` trong src/frontend/src/features/auth/api/authApi.ts
- [ ] T074 [US1] Tạo `LoginPage.tsx` theo màn `W-AU01-Login` (không có `AU01` mobile, cùng trang responsive từ 360 px): `wk-field`, `wk-input`, `wk-error`, `wk-btn--primary wk-btn--block`, "Ghi nhớ (30 ngày)", "Quên mật khẩu?", cảnh báo khóa sau 5 lần sai trong src/frontend/src/features/auth/pages/LoginPage.tsx (làm T062 xanh)
- [ ] T075 [P] [US1] Tạo `AccountStatusPage.tsx` theo màn `AU07-Locked`, `W-AU07-Locked` với class `wk-state`, `wk-banner--danger`; bỏ câu lộ tài khoản, dùng câu trung tính của AF 2.3; đọc dữ liệu từ state của router hoặc fragment rồi `history.replaceState` trong src/frontend/src/features/auth/pages/AccountStatusPage.tsx (làm T063 xanh)
- [ ] T076 [US1] Thêm tên người dùng, nút "Đăng xuất" vào src/frontend/src/app/AppLayout.tsx và kiểm tra `pageshow` (làm T064 xanh). `AppLayout` trên `main` đã có header mới (`Header`, thanh dưới trên mobile với tab "Tôi" chưa có liên kết): đặt tên và nút ở chỗ tài khoản của header và biến tab "Tôi" thành liên kết (khách: "Đăng nhập"), không thêm một thanh thứ hai
- [ ] T077 [US1] Tạo `SessionExpiredDialog.tsx` dùng `wk-dialog` (Gap 010-G) trong src/frontend/src/features/auth/components/SessionExpiredDialog.tsx, gắn vào src/frontend/src/app/AppLayout.tsx, mở khi `status = expired` trên route không `offlineFirst`; xuất hook `useUnsavedChangesBlocker()` bọc `useBlocker` cho form dài (060) (làm T065 xanh)
- [ ] T078 [US1] Thêm route `/login`, `/account-status` bọc `RedirectIfAuthenticated` đúng chỗ trong src/frontend/src/app/routes.tsx (làm T066 xanh)

**Checkpoint**: Quickstart Q10–Q18 đạt, trừ phần email ở Q12 kiểm tra ở Phase Integration (outbox đã có dòng). Có thể merge: đã đăng nhập thật được.

---

## Phase 4: User Story 2 - Register a new account and verify email (Priority: P1) 🎯 MVP

**Goal**: Khách tạo tài khoản Member, nhận link xác minh 24 giờ; chưa xác minh vẫn đăng nhập được nhưng không đăng được nội dung.

**Independent Test**: Đăng ký bằng email mới, mở link trong email, đăng nhập bằng thông tin vừa tạo (quickstart Q1–Q8).

### Tests for User Story 2 ⚠️ (viết trước, phải FAIL)

- [ ] T079 [P] [US2] Unit test `ValidPasswordValidatorTest` – US2-3, FR-004: 8–128 code point sau NFC (7 và 129 bị từ chối, 128 emoji qua được), có `\p{L}`, có `\p{Nd}`, khác email không phân biệt hoa thường, mỗi vi phạm một `code` riêng trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/validation/ValidPasswordValidatorTest.java
- [ ] T080 [P] [US2] Integration test `EmailRequestLimiterIT` – FR-017: 3 lần/15 phút mỗi địa chỉ, tách theo `purpose`, lần gửi lúc đăng ký có tính, email chưa đăng ký bị đếm y hệt; lần thứ 4 → 429 `EMAIL_RATE_LIMITED` + `retryAfterSeconds`; 8 request song song vẫn chỉ 3 lần qua trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/service/EmailRequestLimiterIT.java
- [ ] T081 [P] [US2] Integration test `OneTimeTokenServiceIT` – FR-005, FR-015: 32 byte `SecureRandom`, DB chỉ có SHA-256; tiêu thụ nguyên tử (2 thread, 1 thắng); hết hạn không dùng được; token mới vô hiệu token cũ cùng `(user_id, purpose)` trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/service/OneTimeTokenServiceIT.java
- [ ] T082 [P] [US2] Integration test `RegistrationIT` – US2-1 (201, vai trò MEMBER, chưa xác minh, không tạo phiên, FR-001, FR-003), US2-2 (409 `EMAIL_TAKEN`, FR-002), US2-3, `Edge: Double-click on submit` (2 request cùng lúc: một tài khoản, một email), quá giới hạn FR-017 vẫn tạo tài khoản, `verificationEmailSent = false`, `verificationExpiresAt = null`, có `retryAfterSeconds`, không có token và email (FR-002), `Edge: Very long input` (email > 254, password > 128, tên > 50), tên 1–50 ký tự sau trim, `Edge: Email with leading/trailing spaces or mixed case`, email trong `blocked_emails` → 422 `REGISTRATION_REFUSED` không nêu lý do (FR-026) trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/RegistrationIT.java
- [ ] T083 [P] [US2] Integration test `EmailVerificationIT` – US2-4 (link trong 24 giờ, không tự đăng nhập), US2-6 (410 `TOKEN_EXPIRED`, gửi lại bằng `token`), link đã dùng → 200 `ALREADY_VERIFIED`, token lạ → 410 `TOKEN_INVALID`, gửi lại vô hiệu link trước, không gia hạn mốc 7 ngày (FR-005), `Edge: Email delivery failure` (SMTP lỗi vẫn 202, gửi lại được) trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/EmailVerificationIT.java
- [ ] T084 [P] [US2] Unit test `MailRendererTest`, `LinkMailerTest` – biến được escape HTML, có bản `.html` và `.txt` tiếng Việt, link dạng `/verify-email#token=…` theo `public-base-url`, không có mật khẩu; thử 3 lần khi SMTP lỗi rồi log trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/mail/MailRendererTest.java và LinkMailerTest.java
- [ ] T085 [P] [US2] Test `passwordRules.test.ts` – cùng quy tắc FR-004 với backend, đếm `[...value.normalize('NFC')].length` trong src/frontend/src/features/auth/__tests__/passwordRules.test.ts
- [ ] T086 [P] [US2] Test `RegisterPage.test.tsx` – US2-1, US2-2 (giữ giá trị, xóa mật khẩu, link đăng nhập và đặt lại), US2-3 (checklist quy tắc), `Edge: Double-click on submit` (nút khóa khi đang gửi) trong src/frontend/src/features/auth/__tests__/RegisterPage.test.tsx
- [ ] T087 [P] [US2] Test `CheckEmailPage.test.tsx`, `VerifyEmailPage.test.tsx` – US2-4, US2-6, đọc token từ fragment rồi `replaceState` xóa khỏi URL, 429 và `verificationEmailSent = false` hiện "thử lại sau X phút" với X từ `retryAfterSeconds`, nút "Gửi lại email" đếm ngược tới lúc đó (FR-017, AF 1.9), link hết hạn nhắc tài khoản chưa xác minh bị xóa sau 7 ngày (AF 1.11), không có "Đổi địa chỉ email" trong src/frontend/src/features/auth/__tests__/CheckEmailPage.test.tsx và VerifyEmailPage.test.tsx
- [ ] T088 [P] [US2] Test `EmailVerificationBanner.test.tsx` – US2-5: chưa xác minh thấy nhắc và nút gửi lại; 403 `EMAIL_NOT_VERIFIED` hiện banner trong src/frontend/src/features/auth/__tests__/EmailVerificationBanner.test.tsx

### Implementation for User Story 2

- [ ] T089 [P] [US2] Tạo record `RegisterRequest`, `RegisterResponse`, `ConfirmTokenRequest`, `VerificationResult`, `ResendVerificationRequest` (đúng một trong `email`, `token`) và `@ValidPassword`, `ValidPasswordValidator` trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/dto/ và src/backend/wikicook-app/src/main/java/com/wikicook/auth/validation/ (làm T079 xanh)
- [ ] T090 [P] [US2] Cài `EmailRequestLimiter` trên `email_send_requests` có advisory lock trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/service/EmailRequestLimiter.java (làm T080 xanh)
- [ ] T091 [P] [US2] Cài `OneTimeTokenService` theo R6 trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/service/OneTimeTokenService.java (làm T081 xanh)
- [ ] T092 [P] [US2] Cài `BlockedEmailService` implements `BlockedEmails` trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/service/BlockedEmailService.java và interface trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/api/BlockedEmails.java (FR-026)
- [ ] T093 [US2] Tạo `MailRenderer`, `LinkMailer` (sau commit, `@Async` trên virtual thread, thử 3 lần) và mẫu verify-email trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/mail/ và src/backend/wikicook-app/src/main/resources/mail/ (FR-005; làm T084 xanh)
- [ ] T094 [US2] Cài `RegistrationService` (`INSERT … ON CONFLICT (email) DO NOTHING`, chỉ bên thắng gửi email; quá giới hạn thì vẫn tạo tài khoản, không cấp token, `verificationEmailSent = false`, `verificationExpiresAt = null`, `retryAfterSeconds` từ `EmailRequestLimiter`) và `EmailVerificationService` trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/service/
- [ ] T095 [US2] Tạo `POST /auth/register` ở `AuthController` và `VerificationController` (`/auth/verification/confirm`, `/auth/verification/resend`) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/controller/ (làm T082, T083 xanh)
- [ ] T096 [P] [US2] Tạo `passwordRules.ts` và `PasswordRules.tsx` (checklist "Ít nhất 8 ký tự", "Có chữ cái", "Có chữ số") trong src/frontend/src/features/auth/lib/passwordRules.ts và src/frontend/src/features/auth/components/PasswordRules.tsx (làm T085 xanh)
- [ ] T097 [US2] Tạo `RegisterPage.tsx` theo màn `AU02-Register`, `W-AU02-Register`: bộ đếm "9/50", checklist, chỗ cho nút Google ở US5; nối `register` trong `authApi.ts` trong src/frontend/src/features/auth/pages/RegisterPage.tsx (làm T086 xanh)
- [ ] T098 [US2] Tạo `CheckEmailPage.tsx` theo `AU03`, `W-AU03` (không có "Đổi địa chỉ email", `OQ-010-09`; đếm ngược theo `retryAfterSeconds`) và `VerifyEmailPage.tsx` theo `AU04`, `W-AU04` + Gap 010-B trong src/frontend/src/features/auth/pages/ (làm T087 xanh)
- [ ] T099 [US2] Tạo `EmailVerificationBanner.tsx` dùng `wk-banner--warning` và gắn vào src/frontend/src/app/AppLayout.tsx (Gap 010-F; làm T088 xanh)
- [ ] T100 [US2] Thêm route `/register`, `/check-email`, `/verify-email` trong src/frontend/src/app/routes.tsx

**Checkpoint**: Quickstart Q1–Q8, Q46 đạt. Q8 phần gửi công thức dùng `MatrixProbeController` tới khi 060 có endpoint thật.

---

## Phase 5: User Story 3 - Role-based access (Priority: P1) 🎯 MVP

**Goal**: Ma trận quyền được chặn ở server trên mọi request; đổi vai trò và đình chỉ có hiệu lực ở request kế tiếp; không bao giờ mất Administrator cuối cùng.

**Independent Test**: Đăng nhập mỗi vai trò, thử một hành động tiêu biểu; hành động được phép thành công, hành động cấm bị từ chối (quickstart Q19–Q23, Q26, Q36, Q38).

### Tests for User Story 3 ⚠️ (viết trước, phải FAIL)

- [ ] T101 [P] [US3] Integration test `PermissionMatrixIT` – FR-021, SC-004, US3-1, US3-2, US3-3: tham số hóa đủ **9 dòng ma trận × 5 tác nhân** (Guest, Member đã xác minh, Contributor, Moderator, Administrator) = 45 ca, thêm Member chưa xác minh cho FR-006; mọi ca **gọi API trực tiếp bằng JWT thật qua `TestRestTemplate`, không qua giao diện**, kỳ vọng đúng status và `code` (Guest 401, thiếu quyền 403); dòng của module chưa có dùng `MatrixProbeController` ⛓ B, E, đặt **ngoài** gói `auth` để lỗi thiếu quyền đi qua đúng đường của module khác (R15) trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/PermissionMatrixIT.java và src/backend/wikicook-app/src/test/java/com/wikicook/support/MatrixProbeController.java
- [ ] T102 [P] [US3] Integration test `RouteProtectionIT` – FR-021: duyệt `RequestMappingHandlerMapping`, mọi route `/api/v1/**` ngoài `PublicRoutes` trả 401 khi không có token, route mới của module khác tự được kiểm tra; các route của `PublicRoutes` (gồm tìm kiếm của 020) trả khác 401 khi không có token trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/RouteProtectionIT.java
- [ ] T103 [P] [US3] ⛓ C, D Integration test `SecuredRoutesIT` – không dùng filter giả: JWT thật trên `/api/v1/meal-plans`, `/api/v1/shopping-lists`, `/api/v1/ai-planner`; gửi `X-User-Id`, `X-Dev-User-Id`, `X-Test-User` không có tác dụng; hai người dùng không đọc được dữ liệu của nhau (R13) trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/SecuredRoutesIT.java
- [ ] T104 [P] [US3] Unit test `AccountActionPolicyTest` – FR-022, FR-022a: đủ tổ hợp người làm × đối tượng × hành động; tự thao tác → `SELF_ACTION_FORBIDDEN`; Moderator đình chỉ Member, Contributor được; mọi vai trò tối đa 30 ngày (31 ngày → `SUSPENSION_LIMIT_EXCEEDED`, kể cả Administrator), Moderator hoặc Administrator làm đích → `ACCESS_DENIED`; thao tác lên Administrator khác và thăng lên Administrator cần `REAUTH_REQUIRED`; `is_system` không bị tác động trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/service/AccountActionPolicyTest.java
- [ ] T105 [P] [US3] Integration test `AccountAdminIT` – US3-4 (có hiệu lực ở request kế tiếp, audit `ROLE_CHANGED`, FR-024), US3-5, US3-6 (30 ngày được; 31 ngày, gỡ án, cấm, đích Moderator bị từ chối; đình chỉ tài khoản không `ACTIVE` và cấm tài khoản đã `BANNED` → 409 `ACCOUNT_NOT_ACTIVE`; cấm tài khoản đang `SUSPENDED` đóng án cũ trước với `lift_reason = REPLACED_BY_BAN`; **`ends_at` đã qua nhưng `SuspensionExpiryJob` chưa chạy**: đình chỉ lại thành công, cấm ghi `EXPIRED` chứ không phải `REPLACED_BY_BAN`, `SUSPENSION_ENDED` được gửi một lần, gỡ trả 409 `ACCOUNT_NOT_SANCTIONED` không ghi `ACCOUNT_REINSTATED` và vẫn còn `status = ACTIVE`, outbox `SUSPENSION_ENDED` sau phản hồi 409 (không bị rollback); gỡ tài khoản `ACTIVE` → 409 `ACCOUNT_NOT_SANCTIONED`; xóa Administrator đã gỡ án thì `lift_reason` vẫn là `LIFTED`), `Edge: Suspended / banned account` (phiên đang chạy hết ở request kế tiếp, thu hồi mọi phiên cùng transaction, outbox `ACCOUNT_SUSPENDED`, `ACCOUNT_BANNED`, FR-024a), `Edge: Last active Administrator` (409 `LAST_ADMINISTRATOR`; hai Administrator hạ cấp nhau đồng thời vẫn còn một, FR-023), reinstate gửi outbox `BAN_LIFTED`, `SUSPENSION_ENDED`, audit `ACCOUNT_REINSTATED` trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/AccountAdminIT.java
- [ ] T106 [P] [US3] Integration test `ReauthenticationIT` – FR-022a: `POST /auth/reauthenticate` đúng mật khẩu mở 5 phút, sau 5 phút lại 403 `REAUTH_REQUIRED`, sai mật khẩu 401 và đi qua bulkhead trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/ReauthenticationIT.java
- [ ] T107 [P] [US3] Integration test `RoleNoticeIT` – FR-024a: `GET /auth/me` có `roleNotice` sau khi đổi, ack xóa, đổi nhiều lần giữ vai trò ban đầu, đổi về đúng vai trò cũ thì `NULL`, không có email; mọi phản hồi có bearer kèm `X-WikiCook-Role-Notice: 1` tới khi ack, kể cả 403 sau khi bị hạ vai trò (US3-5); phản hồi của `GET /auth/me` và `POST /auth/role-notice/ack` không có header này (R11) trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/RoleNoticeIT.java
- [ ] T108 [P] [US3] Integration test `AuditLogIT` – NFR-008, FR-025: `GET /admin/audit-logs` chỉ Administrator, lọc, phân trang, đủ các `action` của 010; không có endpoint sửa, xóa; `details` không có email, tên hiển thị, dữ liệu ăn uống; sau khi xóa tài khoản entry còn và vẫn ẩn danh; entry cũ hơn 90 ngày vẫn đọc được trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/AuditLogIT.java
- [ ] T109 [P] [US3] Integration test `AdminBootstrapRunnerIT` – spec Assumptions: tạo Administrator đã xác minh khi chưa có; không tạo khi đã có Administrator đang hoạt động; bỏ trống biến thì không làm gì trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/config/AdminBootstrapRunnerIT.java
- [ ] T110 [P] [US3] Integration test `EmailVisibilityIT` – NFR-009: `/auth/me` chỉ có email của chính mình, không có hồ sơ ăn uống hay `householdSize`; `GET /admin/users/{id}` chỉ Administrator; phản hồi đình chỉ cho Moderator không có `email` trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/EmailVisibilityIT.java
- [ ] T111 [P] [US3] Test `SignInPrompt.test.tsx` – US3-1, AF 2.9: dialog AU08, "Đăng nhập" giữ `redirect` về đúng chỗ, "Tạo tài khoản" tới `/register` giữ `redirect`, "Để sau" không đổi gì (hành động của khách dùng dialog này; mở thẳng trang thành viên thì `RequireAuth` chuyển `/login`, như 030) trong src/frontend/src/features/auth/__tests__/SignInPrompt.test.tsx
- [ ] T112 [P] [US3] Test `RequireRole.test.tsx` – US3-2, US3-3: thiếu vai trò tới `/access-denied`, không render nội dung trang; đủ vai trò vào được; 403 `ACCESS_DENIED` từ API cũng về màn này trong src/frontend/src/features/auth/__tests__/RequireRole.test.tsx
- [ ] T113 [P] [US3] Test `RoleChangeNotice.test.tsx` – US3-5, FR-024a: nêu vai trò mới và quyền thay đổi theo ma trận, nhấn mạnh quyền mất khi bị hạ, hiện một lần rồi gọi ack, xuất hiện khi `account.roleNotice` đổi mà không tải lại trang trong src/frontend/src/features/auth/__tests__/RoleChangeNotice.test.tsx
- [ ] T113a [P] [US3] Test `client.roleNotice.test.ts` – FR-024a: phản hồi có `X-WikiCook-Role-Notice` làm tải lại `/auth/me` đúng một lần dù nhiều phản hồi đến cùng lúc, cập nhật `account.role`; không tải lại khi `account.roleNotice` đã có hoặc đang tải lại, nên không lặp dù người dùng chưa đóng thông báo; sau ack không gọi nữa trong src/frontend/src/shared/api/client.roleNotice.test.ts

### Implementation for User Story 3

- [ ] T114 [US3] Sửa contracts: bỏ `email` khỏi `required` của schema `AccountAdmin` trong specs/010-user-authentication-roles/contracts/openapi.yaml và ghi rõ chỉ trả email khi người gọi là Administrator, vì `POST /admin/users/{id}/suspension` cho Moderator mà NFR-009 không cho Moderator xem email (data-model.md đã ghi "Email chỉ cho Administrator")
- [ ] T115 [P] [US3] Tạo record `ChangeRoleRequest`, `SuspendRequest` (`reason` 1–500 ký tự, `endsAt` ở tương lai), `BanRequest`, `ReinstateRequest`, `ReauthenticateRequest` (mật khẩu `@MaxCodePoints(128)`), `AccountAdminDto` (`email` nullable), `AuditLogDto` trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/dto/
- [ ] T116 [P] [US3] Cài `AccountActionPolicy` theo R10 trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/service/AccountActionPolicy.java (làm T104 xanh)
- [ ] T117 [P] [US3] Cài `ReauthServiceImpl` implements `ReauthService` (cột `user_sessions.reauthenticated_at`) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/service/ReauthServiceImpl.java và interface trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/api/ReauthService.java
- [ ] T118 [US3] Cài `AccountAdminService` implements `AccountAdminFacade`: đổi vai trò (đặt `pending_role_notice_from`, không gửi email), đình chỉ, cấm (đang `SUSPENDED` thì đóng án cũ với `lift_reason = REPLACED_BY_BAN`, đã `BANNED` thì `ACCOUNT_NOT_ACTIVE`), reinstate (`lift_reason = LIFTED`, tài khoản `ACTIVE` thì `ACCOUNT_NOT_SANCTIONED`); **mọi thao tác gọi `finishExpired(userId)` trước khi đọc `users.status`, trong giao dịch riêng `REQUIRES_NEW` commit ngay**, vì gỡ án tài khoản vừa hết hạn trả 409 `ACCOUNT_NOT_SANCTIONED` và rollback sẽ làm mất `status = ACTIVE` lẫn outbox `SUSPENSION_ENDED` (R9); đổi `users.status`, chèn `user_suspensions`, thu hồi phiên, thêm outbox, audit trong một transaction; kiểm tra Administrator cuối cùng bằng `SELECT id FROM users WHERE role = 'ADMINISTRATOR' AND status = 'ACTIVE' ORDER BY id FOR UPDATE` trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/service/AccountAdminService.java và interface trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/api/AccountAdminFacade.java (FR-022, FR-023, FR-024)
- [ ] T119 [US3] Tạo `AccountAdminController` (`GET /admin/users/{id}`, `PATCH …/role`, `POST …/ban`, `POST …/reinstatement` `@AdministratorOnly`; `POST …/suspension` `@ModeratorOnly`, bỏ `email` khỏi phản hồi cho Moderator) và `AuditLogController` (thêm `AuditService.search(filter, page)` trả trang `AuditLogDto` mới nhất trước; controller chỉ gọi hàm này, không gọi Repository) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/controller/ (làm T105, T108, T110 xanh)
- [ ] T120 [US3] Thêm `POST /auth/reauthenticate`, `POST /auth/role-notice/ack` và `roleNotice` ở `GET /auth/me` trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/controller/AuthController.java (làm T106, T107 xanh)
- [ ] T120a [US3] Thêm `pendingRoleNoticeFrom` vào `SessionSnapshot` và `findSnapshot`; tạo `AccountStateHeaderFilter` đặt sau `BearerTokenAuthenticationFilter`, thêm `X-WikiCook-Role-Notice: 1` khi cột khác NULL, trừ `GET /auth/me` và `POST /auth/role-notice/ack`, không thêm query, trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/security/AccountStateHeaderFilter.java và src/backend/wikicook-app/src/main/java/com/wikicook/auth/security/SecurityConfig.java (FR-024a; làm T107 xanh)
- [ ] T121 [P] [US3] Cài `AdminBootstrapRunner` trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/config/AdminBootstrapRunner.java (làm T109 xanh)
- [ ] T122 [P] [US3] Tạo `SignInPrompt.tsx` theo màn `AU08-SignInPrompt`, `W-AU08-SignInPrompt` dùng `wk-dialog`, `wk-dialog__actions` trong src/frontend/src/features/auth/components/SignInPrompt.tsx (làm T111 xanh)
- [ ] T123 [P] [US3] Tạo `AccessDeniedPage.tsx` theo màn `AU09-AccessDenied`, `W-AU09-AccessDenied` dùng `wk-state` trong src/frontend/src/features/auth/pages/AccessDeniedPage.tsx (làm T112 xanh)
- [ ] T124 [US3] Tạo `RoleChangeNotice.tsx` (Gap 010-H) và gắn vào src/frontend/src/app/AppLayout.tsx; `client.ts` đọc `X-WikiCook-Role-Notice`, phát sự kiện, `AuthProvider` tải lại `/auth/me` một lần trong src/frontend/src/shared/api/client.ts và src/frontend/src/features/auth/AuthProvider.tsx (làm T113, T113a xanh)
- [ ] T125 [US3] Thêm route `/access-denied` trong src/frontend/src/app/routes.tsx và xử lý `ACCESS_DENIED`, `EMAIL_NOT_VERIFIED` ở src/frontend/src/features/auth/AuthProvider.tsx

**Checkpoint**: Quickstart Q19–Q23, Q26, Q36, Q38, Q45 đạt, riêng Q19 chờ route của 060 và phần trang của Q20, Q21 chờ 100 (đã phủ bằng `PermissionMatrixIT`, `RequireRole.test.tsx`); `PermissionMatrixIT` 45/45. **MVP xong**: mọi module có danh tính thật và quyền được chặn ở server.

---

## Phase 6: User Story 4 - Reset a forgotten password (Priority: P2)

**Goal**: Người quên mật khẩu tự lấy lại quyền truy cập qua email; đặt lại xong mọi phiên khác bị đăng xuất.

**Independent Test**: Yêu cầu đặt lại cho tài khoản có sẵn, mở link, đặt mật khẩu mới; mật khẩu mới vào được, mật khẩu cũ không (quickstart Q27–Q29).

### Tests for User Story 4 ⚠️ (viết trước, phải FAIL)

- [ ] T126 [P] [US4] Unit test `PasswordResetServiceTest` – FR-014: tài khoản chỉ Google, bị đình chỉ, bị cấm không nhận link, phản hồi không đổi; FR-015: 30 phút; thành công không đặt `email_verified_at` (R6) trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/service/PasswordResetServiceTest.java
- [ ] T127 [P] [US4] Integration test `PasswordResetIT` – US4-1, US4-2 (đổi hash, tiêu thụ token, FR-016 thu hồi mọi phiên, audit `PASSWORD_RESET`), US4-3 (đã dùng hoặc quá 30 phút → 410), US4-4 (link mới nhất dùng được, link trước vô hiệu), mật khẩu sai quy tắc không tiêu thụ token (AF 3.5), mật khẩu trùng email → 400 `PASSWORD_EQUALS_EMAIL`, không tiêu thụ token (FR-004), reset không gỡ khóa (FR-012), `Edge: Email delivery failure` (gửi lại được, giới hạn 3 lần/15 phút, FR-017), 429 như nhau với email có và không đăng ký trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/PasswordResetIT.java
- [ ] T128 [P] [US4] Integration test `ForgotPasswordTimingIT` – NFR-003: email có và không đăng ký cùng 202, cùng body, trung vị thời gian lệch không quá 20% vì việc gửi chạy nền trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/ForgotPasswordTimingIT.java
- [ ] T129 [P] [US4] Test `ForgotPasswordPage.test.tsx` – US4-1: luôn hiện câu trung tính, 429 hiện "thử lại sau X phút", có "Gửi lại link" và "Quay lại đăng nhập" trong src/frontend/src/features/auth/__tests__/ForgotPasswordPage.test.tsx
- [ ] T130 [P] [US4] Test `ResetPasswordPage.test.tsx` – US4-2, US4-3: token từ fragment, hiện email từ `validate`, checklist quy tắc, 410 hiện màn link không hợp lệ và cách yêu cầu link mới (Gap 010-I), thành công mời đăng nhập (Gap 010-J) trong src/frontend/src/features/auth/__tests__/ResetPasswordPage.test.tsx
- [ ] T131 [P] [US4] E2E Playwright trên `fakeApi`: quên mật khẩu → link → đặt mật khẩu mới → đăng nhập (US4-2) trong src/frontend/e2e/auth-reset.spec.ts

### Implementation for User Story 4

- [ ] T132 [P] [US4] Tạo record `ForgotPasswordRequest`, `ResetTokenInfo`, `ResetPasswordRequest` (`toString()` che mật khẩu, NFR-001) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/dto/
- [ ] T133 [P] [US4] Thêm mẫu password-reset (`.html`, `.txt`, tiếng Việt, ghi hiệu lực 30 phút) trong src/backend/wikicook-app/src/main/resources/mail/
- [ ] T134 [US4] Cài `PasswordResetService`: giới hạn đồng bộ trên `email_hash`, tra tài khoản và gửi mail trong tác vụ `@Async`; so mật khẩu với email sau khi đọc tài khoản từ token (FR-004); đặt mật khẩu trong một transaction (đổi hash, tiêu thụ token, `revokeAll`, audit) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/service/PasswordResetService.java (làm T126 xanh)
- [ ] T135 [US4] Tạo `PasswordResetController` (`POST /auth/password/forgot`, `/auth/password/reset/validate`, `/auth/password/reset`) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/controller/PasswordResetController.java (làm T127, T128 xanh)
- [ ] T136 [P] [US4] Tạo `ForgotPasswordPage.tsx` theo màn `AU05-ForgotPassword`, `W-AU05-ForgotPassword` trong src/frontend/src/features/auth/pages/ForgotPasswordPage.tsx (làm T129 xanh)
- [ ] T137 [P] [US4] Tạo `ResetPasswordPage.tsx` theo màn `AU06-ResetPassword`, `W-AU06-ResetPassword` + Gap 010-I, 010-J, 010-K trong src/frontend/src/features/auth/pages/ResetPasswordPage.tsx (làm T130 xanh)
- [ ] T138 [US4] Thêm route `/forgot-password`, `/reset-password` trong src/frontend/src/app/routes.tsx và nối `forgotPassword`, `validateResetToken`, `resetPassword` trong src/frontend/src/features/auth/api/authApi.ts (làm T131 xanh)

**Checkpoint**: Quickstart Q27–Q29 đạt.

---

## Phase 7: User Story 5 - Sign in with Google (Priority: P3)

**Goal**: Đăng nhập bằng Google có email đã xác minh; không tạo tài khoản trùng; Google lỗi không ảnh hưởng đăng nhập email.

**Independent Test**: "Tiếp tục với Google" với tài khoản Google mới → có Member và đã đăng nhập; đăng xuất, lặp lại → đúng tài khoản đó (quickstart Q30–Q34). Trong CI dùng WireMock.

### Tests for User Story 5 ⚠️ (viết trước, phải FAIL)

- [ ] T139 [P] [US5] Unit test `CookieOAuth2AuthorizationRequestRepositoryTest` – lưu và đọc lại `state`, `nonce`, PKCE, `redirect`; cookie `wc_oauth` có `HttpOnly`, `SameSite=Lax`, 5 phút; sửa nội dung → HMAC sai → từ chối trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/security/CookieOAuth2AuthorizationRequestRepositoryTest.java
- [ ] T140 [P] [US5] Unit test `GoogleSignInServiceTest` – FR-018: tên trim, cắt 50 code point, rỗng thì lấy phần trước `@`; `remember_me = false`; `redirect` ngoài WikiCook bị bỏ (FR-011); bảng xử lý ở R7 đủ từng dòng trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/service/GoogleSignInServiceTest.java
- [ ] T141 [P] [US5] Integration test `GoogleSignInIT` (WireMock) – US5-1 (tạo Member đã xác minh, FR-003), US5-2 (email đã xác minh: giữ dữ liệu và vai trò, FR-019), US5-3 (hủy → `/login?error=google_cancelled`, không tạo tài khoản), URL chuyển tới Google có `code_challenge` và `code_challenge_method=S256` (PKCE), `Edge: Google account without an email address`, `email_verified = false`, email trong `blocked_emails` → `/login?error=registration_refused` (FR-026), tài khoản bị đình chỉ, cấm, kể cả chưa xác minh → `/account-status#code=…&reason=…&endsAt=…` (FR-013) (ca kỳ đình chỉ đã hết nằm ở T155 vì `finishExpired` tới T164 mới có), khóa FR-012 không chặn Google, lần 2 đúng tài khoản cũ qua `google_sub`, thành công ghi audit `SIGN_IN_SUCCEEDED` và `last_sign_in_at`, bị từ chối ghi `SIGN_IN_FAILED` (FR-025) trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/GoogleSignInIT.java
- [ ] T142 [P] [US5] Integration test `GoogleTakeoverIT` – `Edge: Google sign-in matches an unverified email/password account`, US5-2, FR-019: có `meal_plans`, `shopping_lists`, dị ứng của tài khoản cũ; sau tiếp quản mọi dữ liệu đó mất, mật khẩu cũ không còn dùng được, phiên cũ hết, tên theo Google, đã xác minh, audit `ACCOUNT_RESET`, listener nhận `AccountResetEvent`; tài khoản chưa xác minh đang đình chỉ hoặc cấm → `/account-status`, không phát `AccountResetEvent`, `user_suspensions` và dữ liệu giữ nguyên; listener ném lỗi → rollback toàn bộ, tài khoản cũ nguyên vẹn, `/login?error=google_unavailable` (`OQ-010-07`) trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/GoogleTakeoverIT.java
- [ ] T143 [P] [US5] Integration test `GoogleUnavailableIT` – NFR-005: WireMock 500 và timeout 5 giây → `/login?error=google_unavailable`; thiếu `WIKICOOK_GOOGLE_CLIENT_ID` thì ứng dụng vẫn khởi động, `GET /auth/oauth2/google` về cùng địa chỉ đó; không gọi mạng lúc khởi động; `POST /auth/login` vẫn 200 trong cả ba trường hợp trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/GoogleUnavailableIT.java
- [ ] T144 [P] [US5] Integration test `GoogleReauthIT` – FR-022a, 011 FR-029: `intent=reauth` thêm `prompt=login` và `claims={"id_token":{"auth_time":{"essential":true}}}` (theo kết quả T149b), `auth_time` sớm hơn lúc bắt đầu bị từ chối, ID token thiếu `auth_time` bị từ chối như hủy (R7), thành công đặt `reauthenticated_at` của đúng phiên, không tạo phiên mới; callback không có `wc_rt` vẫn tìm được phiên từ `wc_oauth`; tài khoản Google khác với phiên, hủy hoặc lỗi về `redirect` với `?reauth=cancelled` trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/GoogleReauthIT.java
- [ ] T145 [P] [US5] Test `GoogleButton.test.tsx`, `AuthCallbackPage.test.tsx` – nút là link điều hướng có `redirect`; `/auth/callback` gọi refresh rồi `navigate(safeRedirect(...))`; từng `error` hiện câu trung tính (US5-3, `Edge: Google account without an email address`, Gap 010-L, 010-M, 010-N) trong src/frontend/src/features/auth/__tests__/GoogleButton.test.tsx và AuthCallbackPage.test.tsx

### Implementation for User Story 5

- [ ] T146 [US5] Cài `AccountPurgeService`: phát sự kiện đồng bộ, `DELETE FROM users` trong cùng transaction; record `AccountResetEvent(long userId)` trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/service/AccountPurgeService.java và src/backend/wikicook-app/src/main/java/com/wikicook/auth/api/AccountResetEvent.java
- [ ] T147 [P] [US5] Tạo `CookieOAuth2AuthorizationRequestRepository` trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/security/CookieOAuth2AuthorizationRequestRepository.java (làm T139 xanh)
- [ ] T148 [US5] Cài `GoogleSignInService` theo bảng R7, xét từ trên xuống (đình chỉ, cấm đứng trước tiếp quản; kỳ đình chỉ đã hết thì gọi `finishExpired` trước, sau khi T164 có hàm này), gồm tiếp quản một transaction: phát `AccountResetEvent`, `DELETE`, `INSERT` lại cùng email, audit id cũ và id mới; mọi lần đăng nhập ghi audit và `last_sign_in_at` (FR-025) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/service/GoogleSignInService.java (FR-018, FR-019; làm T140 xanh)
- [ ] T149 [US5] Bật `oauth2Login` có điều kiện trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/security/SecurityConfig.java: chỉ khi có `google.client-id` mới tạo `ClientRegistrationRepository` từ `CommonOAuth2Provider.GOOGLE`, `redirect-uri` theo `public-base-url`, resolver dùng `OAuth2AuthorizationRequestCustomizers.withPkce()` (`baseUri /api/v1/auth/oauth2`, callback `/api/v1/auth/oauth2/callback/*`, timeout 5 giây) và tạo `GoogleLoginHandlers` (thành công đặt `wc_rt`, chuyển `/auth/callback`; thất bại chuyển `/login?error=`; tài khoản bị đình chỉ, cấm chuyển `/account-status#code=…&reason=…&endsAt=…`; `intent=reauth`) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/security/GoogleLoginHandlers.java (làm T141, T142, T143, T144 xanh)
- [ ] T149b 🚧 CHẶN (research "Điểm cần chốt với nhóm" mục 40, trạng thái **Mở**; cần Google client của T178): thử với client thật xem Google có ép đăng nhập lại với `prompt=login` và có trả `auth_time` khi gửi `claims` và bật cài đặt không; ghi kết quả vào specs/010-user-authentication-roles/research.md R7. Nếu không được, dừng lại để nhóm chọn cách khác và sửa spec 010, 011 trước. Chặn T149a
- [ ] T149a [US5] Sau T149b, xử lý `intent=reauth` theo bảng R7 trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/security/GoogleLoginHandlers.java và CookieOAuth2AuthorizationRequestRepository.java: lưu `sid` và thời điểm bắt đầu vào `wc_oauth`, gửi `prompt=login` cùng `claims` xin `auth_time`, kiểm tra `google_sub` trùng tài khoản của phiên và `auth_time` có mặt, không sớm hơn thời điểm bắt đầu, không tạo phiên; chấp nhận `intent` chữ thường (FR-022b; làm T144 xanh)
- [ ] T150 [P] [US5] Tạo `GoogleButton.tsx`, `AuthCallbackPage.tsx` trong src/frontend/src/features/auth/components/GoogleButton.tsx và src/frontend/src/features/auth/pages/AuthCallbackPage.tsx (làm T145 xanh)
- [ ] T151 [US5] Gắn `GoogleButton` vào `LoginPage.tsx`, `RegisterPage.tsx`, hiện thông báo theo `?error=` trong src/frontend/src/features/auth/pages/, thêm route `/auth/callback` trong src/frontend/src/app/routes.tsx

**Checkpoint**: Quickstart Q30–Q34, Q43 đạt với WireMock; Q42 đạt sau T149b; bước với Google thật chờ client ở T178.

---

## Phase 8: Integration (Email, job, 011, 100, 060)

**Purpose**: Email thông báo, tác vụ định kỳ và bề mặt API cho 011, 100. Chỉ cung cấp API và quy tắc; màn hình, luồng xóa tài khoản của 011 và màn quản trị của 100 không nằm ở đây.

### Tests (viết trước, phải FAIL)

- [ ] T152 [P] Integration test `OutboxDispatcherIT` – FR-012, FR-024a: gửi đủ 5 mẫu, email đọc từ `users` lúc gửi, payload không có token; SMTP lỗi → thử lại theo 1, 5, 15 phút, 1, 6 giờ rồi `failed_at`; 2 dispatcher song song không gửi trùng (`FOR UPDATE SKIP LOCKED`); với `wikicook.auth.jobs.enabled = false`, phương thức `@Scheduled` không làm gì còn `run()` gọi trực tiếp vẫn chạy (R9, vì `AiAsyncConfig` đã bật lịch cho cả ứng dụng) trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/job/OutboxDispatcherIT.java
- [ ] T153 [P] Unit test `NotificationTemplatesTest` – FR-024a: thư đình chỉ có lý do, ngày kết thúc, địa chỉ khiếu nại từ `appeal-contact`; thư cấm không có ngày, vẫn có địa chỉ khiếu nại; `appeal-contact` rỗng hoặc chỉ khoảng trắng thì dùng `mail-from`; biến được escape; thư khóa FR-012 có giờ thử lại trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/mail/NotificationTemplatesTest.java
- [ ] T154 [P] Integration test `UnverifiedAccountPurgeJobIT` – `Edge: Unverified account reaches 7 days`, FR-005: quá 7 ngày bị xóa cùng dữ liệu, phát `AccountPurgedEvent`, audit `ACCOUNT_PURGED_UNVERIFIED`, phiên cũ → 401, email đăng ký lại được ngay, không vào `blocked_emails`; gửi lại link không gia hạn; `BANNED` → xóa và thêm hash (FR-026); `SUSPENDED` → chưa xóa, hết đình chỉ mà vẫn chưa xác minh thì lượt sau xóa; `is_system` không bị xóa; 2 instance chỉ một chạy trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/job/UnverifiedAccountPurgeJobIT.java
- [ ] T155 [P] Integration test `SuspensionExpiryJobIT` – FR-024a: quá `ends_at` → `lifted_at = ends_at`, `lifted_by` NULL, `lift_reason = EXPIRED`, `status = ACTIVE`, outbox `SUSPENSION_ENDED`, audit; chạy lại không gửi thêm; đăng nhập bằng mật khẩu hoặc Google ngay sau `ends_at` gọi `finishExpired`, email chỉ một lần (FR-013) trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/job/SuspensionExpiryJobIT.java
- [ ] T156 [P] Integration test `AuthRetentionJobIT` – `login_attempts`, `email_send_requests`, `password_check_failures` > 1 ngày; `one_time_tokens` đã dùng hoặc hết hạn > 7 ngày; `user_sessions` hết hạn hoặc thu hồi > 90 ngày; `email_outbox` xong > 30 ngày; **`audit_logs` không bị đụng** (NFR-008) trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/job/AuthRetentionJobIT.java
- [ ] T157 [P] Integration test `AccountClosureTokenIT` – FR-013, R16: đúng mật khẩu với tài khoản `BANNED` → 403 `ACCOUNT_BANNED` có `closureToken`, 10 phút, dùng một lần, không tạo dòng `user_sessions`; sai mật khẩu không có token; token không gọi được API nào khác; Google chuyển token qua fragment của `/account-status`; `consume` gọi trong transaction đang mở của bên gọi, transaction đó rollback thì token vẫn dùng được (011 FR-037b, 011 T091), gọi ngoài transaction thì báo lỗi trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/AccountClosureTokenIT.java
- [ ] T158 [P] ⛓ A, E Integration test `AuthApiContractIT` – mọi interface ở contracts/integration.md mục 2.2, 2.3: `AccountGuard` từ chối tự xóa khi đang đình chỉ hoặc là Administrator cuối cùng (FR-023, FR-026); `BlockedEmails.block` lưu hash, không có email rõ, đăng ký và Google bị từ chối (FR-026); `SessionControl.revokeAllExcept` giữ đúng phiên; `ClosureTokens.consume` tham gia transaction của bên gọi, rollback thì token dùng lại được; `PasswordService` cùng quy tắc và encoder; sau khi xóa `users` email đăng ký lại được ngay (FR-002). Cách làm trước: test gọi interface bằng code giả lập phía 011, 100 trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/api/AuthApiContractIT.java
- [ ] T158a [P] Integration test `PasswordCheckLimiterIT` – FR-022b: 5 lần sai trong 15 phút qua `checkCurrent` và `POST /auth/reauthenticate` cộng chung, lần sau 429 `PASSWORD_CHECK_LIMITED` có `retryAfterSeconds`, không chạy BCrypt; 15 phút sau lần sai thứ 5 mở lại; đúng mật khẩu xóa bộ đếm; gửi song song không vượt 5 lần; đăng nhập không bị khóa trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/service/PasswordCheckLimiterIT.java
- [ ] T159 [P] Test `AccountStatusPage.closure.test.tsx` – có `closureToken` thì hiện link `/account-closure#token=…` của 011, không có thì ẩn; token đọc từ fragment rồi xóa khỏi URL trong src/frontend/src/features/auth/__tests__/AccountStatusPage.closure.test.tsx

### Implementation

- [ ] T160 [P] Thêm mẫu account-locked, account-suspended, account-banned, suspension-ended, ban-lifted (`.html`, `.txt`, tiếng Việt) trong src/backend/wikicook-app/src/main/resources/mail/ (làm T153 xanh)
- [ ] T161 Cài `OutboxDispatcher` (15 giây, `FOR UPDATE SKIP LOCKED`, tối đa 6 lần) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/mail/OutboxDispatcher.java (FR-012, FR-024a; làm T152 xanh)
- [ ] T162 [P] Tạo `AuthJobsConfig` (`@EnableScheduling`, lịch `Asia/Ho_Chi_Minh`, `pg_try_advisory_xact_lock(<khóa job>)`, mỗi lô 100 dòng). Không đặt điều kiện lên bean job: mỗi job có `run()` công khai, phương thức `@Scheduled` chỉ gọi `run()` khi `wikicook.auth.jobs.enabled = true` (R9) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/job/AuthJobsConfig.java
- [ ] T163 ⛓ A, B Tạo `UnverifiedAccountPurgeJob` (mỗi giờ) dùng `AccountPurgeService` và record `AccountPurgedEvent(long userId)` trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/job/UnverifiedAccountPurgeJob.java và src/backend/wikicook-app/src/main/java/com/wikicook/auth/api/AccountPurgedEvent.java (FR-005; làm T154 xanh). Cách làm trước: chưa có listener thì sự kiện không làm gì
- [ ] T164 Tạo `SuspensionExpiryJob` (5 phút) với `finishExpired(userId)` idempotent, `@Transactional(propagation = REQUIRES_NEW)`, gọi từ `AccountAdminService` (T118), `SignInService` và `GoogleSignInService` trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/job/SuspensionExpiryJob.java, src/backend/wikicook-app/src/main/java/com/wikicook/auth/service/SignInService.java và GoogleSignInService.java (làm T155 xanh)
- [ ] T165 [P] Tạo `AuthRetentionJob` (03:30 hằng ngày) trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/job/AuthRetentionJob.java (làm T156 xanh)
- [ ] T166 Cài `ClosureTokens` (`ACCOUNT_CLOSURE`, 10 phút, `consume` chạy trong transaction của bên gọi, không dùng `REQUIRES_NEW`) và trả `closureToken` ở `SignInService`, `GoogleLoginHandlers` trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/api/ClosureTokens.java và src/backend/wikicook-app/src/main/java/com/wikicook/auth/service/OneTimeTokenService.java (làm T157 xanh)
- [ ] T167 [P] Hiện link đóng tài khoản trong src/frontend/src/features/auth/pages/AccountStatusPage.tsx (làm T159 xanh)
- [ ] T168 ⛓ A, E Cài `AccountGuard` (403 `ACCOUNT_SUSPENDED` kèm `reason`, `endsAt`; 409 `LAST_ADMINISTRATOR`), `SessionControl`, `PasswordService`, `PasswordServiceImpl` theo contracts/integration.md trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/api/ và src/backend/wikicook-app/src/main/java/com/wikicook/auth/service/ (làm T158 xanh). Không viết màn hay endpoint thuộc 011, 100
- [ ] T168a Cài `PasswordCheckLimiter` theo R17, gọi trong `PasswordServiceImpl.checkCurrent`; `POST /auth/reauthenticate` chuyển sang `checkCurrent`, thêm 429 trong src/backend/wikicook-app/src/main/java/com/wikicook/auth/service/PasswordCheckLimiter.java và AuthController.java (FR-022b; làm T158a xanh)
- [ ] T169 🚧 CHẶN (research "Điểm cần chốt với nhóm" mục 18, `OQ-010-07`) ⛓ B: B và Duy xác nhận 060 có handler xóa công thức của `AccountResetEvent`, `AccountPurgedEvent`, vì `recipes.author_id` là `SET NULL`; sau đó bỏ ghi chú "chờ 060" và thêm kiểm tra bản nháp bị xóa vào src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/GoogleTakeoverIT.java. Trước khi có 060, bảng `recipes` chưa có đường tạo bản nháp nên 010 không bị chặn

**Checkpoint**: Quickstart Q9, Q12 (phần email), Q23, Q24, Q25, Q39, Q40, Q41 đạt; 011 và 100 gọi được API của 010.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Gỡ dev stub, hiệu năng, bảo mật, accessibility, độ phủ, kiểm chứng và tài liệu

- [ ] T170 Xóa khỏi `com.wikicook.devstub`: `StubCurrentUser`, `DevUserProvisioner`, bean tương ứng trong `DevStubConfig`, `userId` trong `DevStubProperties`, test `StubCurrentUserTest`, `DevUserProvisionerIT` (gọi thẳng `DevUserProvisioner`, sẽ không biên dịch được), phần tương ứng của `DevStubConfigTest`; `DevHeaderSearchViewer` và nhánh `devHeaderSearchViewer` trong src/backend/wikicook-app/src/main/java/com/wikicook/search/port/ (giữ `guestSearchViewer` làm mặc định khi thiếu bean); sửa dòng `X-Dev-User-Id` trong src/backend/wikicook-ai/README.md; sửa package-info trong src/backend/wikicook-app/src/main/java/com/wikicook/devstub/ và src/backend/wikicook-app/src/test/java/com/wikicook/devstub/; bỏ `WIKICOOK_DEV_STUBS_USER_ID`, `X-Dev-User-Id` khỏi src/backend/docker-compose.yml, src/backend/.env.example, src/backend/wikicook-app/src/main/resources/application.yml (R13)
- [ ] T171 ⛓ A Xóa nốt package src/backend/wikicook-app/src/main/java/com/wikicook/devstub/ (`StubDietaryProfile`, `DevStubConfig`, `DevStubProperties`, `package-info`), dòng `com.wikicook.devstub.DevStubConfig` trong src/backend/wikicook-app/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports, test trong src/backend/wikicook-app/src/test/java/com/wikicook/devstub/ và phần `dev-stubs` của `DefaultPortsConfigTest`, khi 011 cài `DietaryProfilePort` thật (R13, research mục 26). Cách làm trước: `StubDietaryProfile` ở lại, không ảnh hưởng đăng nhập
- [ ] T172 [P] ⛓ C Sửa src/backend/perf/meal-planner.k6.js: `setup()` đăng nhập tài khoản test, bỏ `X-Dev-User-Id` và lệnh bật dev stub trong chú thích đầu file (R13). Cách làm trước: sửa ở nhánh 010, báo C review
- [ ] T173 [P] Viết src/backend/perf/auth-sign-in.k6.js (NFR-004, R14), tách tag: đăng nhập 100 VU `constant-arrival-rate` 5 lần/giây 10 phút, p95 < 2 s, không 5xx; refresh + `/auth/me` và API có bearer p95 < 500 ms ở 20 VU 5 phút, 100 VU 10 phút; mỗi VU một tài khoản trong nhóm tạo sẵn, backend chạy với `WIKICOOK_AUTH_SIGN_IN_IP_LIMIT` nâng cao, ngưỡng `http_reqs{status:429}` = 0 để 429 không qua được điều kiện "không có 5xx" (R5, R14)
- [ ] T174 Đo BCrypt cost 12 trên máy chạy thật, chạy T173, ghi số đo vào docs/test/010-user-authentication-roles-performance.md và thay giả định 200–300 ms ở specs/010-user-authentication-roles/research.md R14. Vượt ngưỡng: tăng lõi hoặc giới hạn bulkhead trước; hạ cost cần nhóm đồng ý
- [ ] T175 [P] Thêm `Content-Security-Policy` theo R2 và `Referrer-Policy: no-referrer` trong src/frontend/nginx.conf, lặp ở **từng** `location` có `add_header` riêng (`/`, `/assets/`) hoặc qua một file `include`, vì nginx không kế thừa `add_header` từ cấp `server` (R2); kiểm tra bằng `curl -I` trên `/`, `/shopping-list`, một file `/assets/…`, và trang vẫn tải được
- [ ] T176 [P] Integration test `NoSecretInLogsIT` – NFR-001: `OutputCaptureExtension` không thấy mật khẩu, token, refresh cookie trong log của đăng ký, đăng nhập, reset; phản hồi và `toString()` của DTO không có mật khẩu hay `password_hash` trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/NoSecretInLogsIT.java
- [ ] T177 [P] Integration test `TransportDefaultsIT` – NFR-002: `cookie-secure` mặc định `true`, cookie `wc_rt`, `wc_oauth` có `Secure` khi không ghi đè, link email dùng `public-base-url` trong src/backend/wikicook-app/src/test/java/com/wikicook/auth/security/TransportDefaultsIT.java
- [ ] T178 🚧 CHẶN (research "Điểm cần chốt với nhóm" mục 21 `TODO(HOSTING)`, trạng thái **Mở**): Hiển và Dự chốt nơi triển khai, TLS, nhà cung cấp SMTP thật, Google OAuth client và redirect URI, rồi ghi vào specs/010-user-authentication-roles/quickstart.md mục 1. Không tự chọn. Chặn T179, T149b và bước Google thật của Q30, Q42; không chặn code khác
- [ ] T179 Sau T178, kiểm tra trên môi trường có TLS: HTTP chuyển HTTPS, có HSTS ở proxy, cookie `Secure` hoạt động (NFR-002); ghi kết quả vào docs/test/010-user-authentication-roles-acceptance.md
- [ ] T180 [P] E2E Playwright `auth-responsive.spec.ts` – NFR-006: mọi trang auth ở 360 px không cuộn ngang, chuỗi tiếng Việt, trong src/frontend/e2e/auth-responsive.spec.ts
- [ ] T181 [P] E2E Playwright `auth-a11y.spec.ts` – NFR-007: đi hết form đăng nhập, đăng ký, quên mật khẩu, đặt mật khẩu chỉ bằng bàn phím; mọi trường có nhãn hiển thị (`getByLabel`); lỗi nằm cạnh trường qua `aria-describedby`, lỗi chung `role="alert"` trong src/frontend/e2e/auth-a11y.spec.ts
- [ ] T182 Kiểm tra accessibility cho NFR-007 như 030: chạy axe `wcag2a`, `wcag2aa`, `wcag21aa` trên mọi màn auth, thêm một lượt NVDA hoặc VoiceOver thật, ghi kết quả vào docs/test/010-user-authentication-roles-a11y.md. axe chạy ngoài repo, không thêm dependency
- [ ] T183 [P] So `/v3/api-docs` với specs/010-user-authentication-roles/contracts/openapi.yaml bằng script: đường dẫn, method, `operationId`, field, enum `code`; ghi chênh lệch vào docs/test/010-user-authentication-roles-acceptance.md
- [ ] T184 Chạy `cd src/backend && mvn verify`, xác nhận JaCoCo `com.wikicook.auth.service` line và branch ≥ 70% theo rule ở src/backend/wikicook-app/pom.xml; thêm test nếu thiếu
- [ ] T185 Chạy `cd src/frontend && npm run lint && npm test && npm run build && npm run e2e`, xác nhận `eslint-plugin-jsx-a11y` không có lỗi ở src/frontend/src/features/auth/
- [ ] T186 Chạy quickstart Q1–Q46 (specs/010-user-authentication-roles/quickstart.md) và ghi kết quả vào docs/test/010-user-authentication-roles-acceptance.md
- [ ] T187 [P] Tổ chức buổi thử với người dùng cho SC-003 (≥ 90% đăng ký, đăng nhập được ngay lần đầu), đo SC-001, SC-002, SC-006, ghi vào docs/test/010-user-authentication-roles-acceptance.md
- [ ] T188 [P] Lập ma trận truy vết acceptance scenario (US1-1 … US5-3) và 16 edge case → test tương ứng, kèm FR/NFR → task, trong docs/test/010-user-authentication-roles-traceability.md (SC-007)
- [ ] T189 ⛓ E, Dự Ghi các đề mục tài liệu cần sửa sau khi merge (research mục 2, 3, 5, 6, 7, 12, 13, 14, 20, 25, 41, 42) vào docs/test/010-user-authentication-roles-acceptance.md; E xác nhận phần đã sửa ở spec 100 ngày 2026-11-07 và sửa tên `action`, dòng 293 (mục 42); người phụ trách tự sửa spec 070, `architecture.md`, project plan, `full-schema.md` (gồm mục 4.10: `audit_logs.actor_id` không khóa ngoại và có trigger cấm UPDATE, DELETE; thêm `user_suspensions.lift_reason`). Task này không sửa thay họ. Hiển sửa `docs/requirements/use-case-specification.md` mục 9 theo spec sau khi Duy duyệt PR: AF 1.7, 1.13, 2.2, 2.3, 2.5, 2.9, 2.14, 3.8, 4.2, 4.6 và `OQ-010-02`, `OQ-010-04`, `OQ-010-10` (mục 43)

**Checkpoint**: Quickstart Q35, Q37 đạt và Q1–Q46 có kết quả ghi lại; `mvn verify`, `npm test`, k6 đạt ngưỡng; ma trận truy vết đủ 26 acceptance scenario, 16 edge case.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: T001 đã xong; T002 🚧 phải xong trước mọi việc khác.
- **Foundational (Phase 2)**: sau Setup – CHẶN mọi user story. T003 làm `mvn verify` đỏ cho tới cuối phase, nên cả phase ở một nhánh.
- **US1 (Phase 3)**: sau Foundational. Là nền cho mọi story khác (`SignInService`, `SessionService`, `LoginPage`).
- **US2 (Phase 4)**: sau Foundational; backend độc lập với US1, frontend dùng lại trang và `authApi.ts` của US1.
- **US3 (Phase 5)**: sau US1 (cần phiên thật cho ma trận quyền); T103 chứng minh R13 đã xong.
- **US4 (Phase 6)**: sau US1 (`revokeAll`) và US2 (`OneTimeTokenService`, `EmailRequestLimiter`, `MailRenderer`).
- **US5 (Phase 7)**: sau US1; `AccountPurgeService` dùng lại ở Phase 8.
- **Integration (Phase 8)**: sau US3, US5; T161 mở phần email của Q12, Q23.
- **Polish (Phase 9)**: sau các story muốn giao. T170 sau Checkpoint US1 và US3. T174 sau T173. T179 sau T178. T171 chờ 011.

### Within Each User Story

- Test viết trước và FAIL → DTO → service → controller → API client → component → page → route.
- Không đánh dấu task xong khi test tương ứng chưa xanh.
- Dòng test ghi mã scenario; dòng code ghi "làm … xanh" để biết test nào mở khóa.

### Phụ thuộc người khác (⛓)

| Task | Người | Cách làm trước để không bị chặn | Khi bên kia xong |
|---|---|---|---|
| T028 | E | 010 tạo `audit_logs`, `user_suspensions` ở `V102` | E bỏ hai bảng khỏi `V503`, thêm cột bằng migration riêng |
| T044, T045, T172 | C | Sửa ở nhánh 010, giữ `X-Test-User`, chỉ đổi import và header | C review, không cần đổi logic test |
| T026a, T054a | C | Sửa ở nhánh 010: 030 chỉ chuyển `/login` với khách, người hết phiên dùng hộp thoại của 010 | C review; 030 US2-4 (khách) không đổi |
| T022, T046, T047 | D | Sửa ở nhánh 010, Service của 040 giữ nguyên | D review |
| T019a, T042a, T045 (phần tìm kiếm), T170 (phần `DevHeaderSearchViewer`) | B (020) | Sửa ở nhánh 010: `AuthSearchViewer` thay `DevHeaderSearchViewer`, đổi test `RecipeSearchControllerIT`, giữ `guestSearchViewer` | B review |
| T103 | C, D | Dùng route có sẵn của 030, 031, 040 | Không cần việc thêm |
| T101 | B, E | `MatrixProbeController` trong test cho review, submit, moderate, danh mục | Đổi từng dòng sang endpoint thật của 060, 070, 100 |
| T163, T169 | A, B | Sự kiện không có listener thì không làm gì; `recipes` chưa có đường tạo nháp | 060 và 011 thêm listener theo contracts/integration.md mục 1 |
| T158, T168 | A, E | Test gọi interface, giả lập phía 011, 100 | 011 gọi `AccountGuard`, `BlockedEmails`, `SessionControl`, `ClosureTokens`; 100 gọi `AccountAdminFacade`, `AuditWriter` |
| T171 | A | `StubDietaryProfile` ở lại | Xóa nốt package khi 011 có `DietaryProfilePort` |
| T189 | E, Dự, Hiển | Không chặn code | Sửa spec 100, 070, `architecture.md`, project plan, `full-schema.md` |
| Hàng đợi offline 040 | D | Frontend hiện chưa có IndexedDB (`idb` chưa được import), nên 010 không có gì để sửa | Khi làm hàng đợi, D gắn `userId` và chỉ đồng bộ khi khớp người đăng nhập (research mục 19) |
| T054 | D | `/shopping-list*` dùng `RequireAuth offlineFirst`, ở lại trang khi `expired` | D hiện banner "Phiên đăng nhập đã hết hạn" theo `useAuth().status` (040 FR-020); 050 dùng cùng prop |
| App shell offline | C, D, Chung | 010 chỉ xử lý phiên khi app đã tải; Q44 kiểm bằng tab đang mở và backend dừng | 040, 050 quyết định service worker (research mục 41) |
| T053 | D | Chưa có guard thì đăng xuất ngay | D đăng ký `addSignOutGuard` để cảnh báo mutation chưa đồng bộ (research 040, Logout Rules) |
| Spec 100 | E | Đã sửa ngày 2026-11-07 theo spec 010: cột, Clarification, gỡ cấm, khiếu nại | E xác nhận; quyết định công thức `ARCHIVED` khi gỡ cấm (research mục 35); đổi tên `action` theo 010 và sửa dòng 293 (mục 42) |
| Trang chủ `/`, `/search` | B | Đã có ở `main` (`HomePage`, `SearchResultsPage`, `RecipeSearchController`): 010 khai báo công khai `GET /api/v1/recipes/search`, `/search/options`, `/featured` ở `PublicRoutes` (T043) | 020 thêm route công khai mới (ví dụ chi tiết công thức) vào `PublicRoutes`, `RouteProtectionIT` kiểm tra phần còn lại |
| Gap 010-A…N, AU03, AU07 | Chung | Dựng từ component chung, bỏ "Đổi địa chỉ email", dùng câu trung tính | Chung cập nhật prototype (research mục 22) |

### Task chặn (🚧)

| Task | Điểm trong research | Trạng thái | Chặn |
|---|---|---|---|
| T001 | Mục 4 | **Đã xong** (2026-11-11): SCRUM-72, SCRUM-74, SCRUM-76 | Không còn chặn |
| T002 | Mục 1, 8, 9, 10, 11, 15, 16, 17, 23, 24, 26, 27, 30–34, 36, 37 | Đã quyết, chờ duyệt PR SCRUM-72 (Hưng theo `pa4-guide.md`, Duy cho các quyết định) | Mọi task từ T003 |
| T178 | Mục 21 | **Mở**, chưa có quyết định | T179, T149b, bước Google thật của Q30, Q42 |
| T149b | Mục 40 | **Mở**, cần client thật của T178 | T149a, Q42; phần Google của 011 FR-029, FR-031 |
| T169 | Mục 18 | Đã quyết, chờ B và Duy xác nhận | Hoàn tất FR-005, FR-019 khi 060 có dữ liệu |

### Parallel Opportunities

- Setup: T005, T006, T008, T009, T010 song song.
- Foundational: mọi task test [P] song song; T031, T032, T035, T036, T037, T038, T049, T050, T052, T055 song song; T039 sau T033, T034.
- Mỗi story: mọi task test [P] song song; backend và frontend của cùng story song song.
- Sau US1: US2, US3, US5 có thể giao cho người khác nhau cùng lúc; US4 sau US2.
- Phase 8: các job và mẫu email độc lập nhau.

---

## Parallel Example: User Story 1

```bash
# Viết toàn bộ test US1 cùng lúc (phải FAIL):
Task: "T056 SignInThrottleIT in src/backend/wikicook-app/src/test/java/com/wikicook/auth/service/SignInThrottleIT.java"
Task: "T057 SessionServiceTest in src/backend/wikicook-app/src/test/java/com/wikicook/auth/service/SessionServiceTest.java"
Task: "T059 SignInIT in src/backend/wikicook-app/src/test/java/com/wikicook/auth/controller/SignInIT.java"
Task: "T062 LoginPage.test.tsx in src/frontend/src/features/auth/__tests__/LoginPage.test.tsx"
Task: "T063 AccountStatusPage.test.tsx in src/frontend/src/features/auth/__tests__/AccountStatusPage.test.tsx"

# Sau đó code song song backend và frontend:
Task: "T067 DTO records in src/backend/wikicook-app/src/main/java/com/wikicook/auth/dto/"
Task: "T071 RefreshCookie in src/backend/wikicook-app/src/main/java/com/wikicook/auth/security/RefreshCookie.java"
Task: "T075 AccountStatusPage in src/frontend/src/features/auth/pages/AccountStatusPage.tsx"
```

---

## Implementation Strategy

### MVP First (Setup + Foundational + US1 + US2 + US3)

1. Phase 1 Setup, xong T002 🚧 → Phase 2 Foundational (CHẶN mọi story).
2. Phase 3 US1 → **dừng và kiểm chứng** quickstart Q10–Q18. Từ đây merge được vì đã đăng nhập thật.
3. Phase 4 US2 → kiểm chứng Q1–Q8.
4. Phase 5 US3 → kiểm chứng Q19–Q23, Q26, Q36, Q38. **MVP xong**: mọi module có danh tính thật, không
   còn mạo danh qua header, ma trận quyền 45/45.

### Incremental Delivery

1. MVP → demo nội bộ, đáp ứng F-01 của Build `v0.8` phần đăng nhập và RBAC.
2. US4 (đặt lại mật khẩu) → demo.
3. US5 (Google) → demo với WireMock, Google thật sau T178.
4. Phase 8 khi 011, 100, 060 cần API.
5. Phase 9 trước Build `v0.8`; T171 khi 011 xong.

### Parallel Team Strategy

1. A làm Setup + Foundational; C, D review phần test và controller của mình.
2. Sau US1: một người làm US2 rồi US4, một người làm US3, một người làm US5.
3. E và A cùng duyệt T158; B xác nhận T169.

---

## Notes

- [P] = file khác nhau, không phụ thuộc task chưa xong.
- Mã scenario (US1-3…) và edge case trong mô tả test dùng cho ma trận truy vết T188.
- Commit theo Conventional Commits kèm mã Jira của phần đang làm (mục "Mã Jira"), ví dụ `feat(auth): add sign-in API (SCRUM-74)`.
- Dừng ở mỗi Checkpoint để kiểm chứng story độc lập.
- Không sửa `src/specs/` (bản sao cũ).
