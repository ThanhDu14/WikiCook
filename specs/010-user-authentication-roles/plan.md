# Implementation Plan: User Authentication & Roles

**Branch**: `feature/SCRUM-74-auth-backend`, `feature/SCRUM-76-auth-profile-frontend` (tài liệu: `feature/SCRUM-72-spec-kit-010-011`) | **Date**: 2026-11-06 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/010-user-authentication-roles/spec.md`

## Summary

Nền tảng danh tính cho toàn bộ WikiCook: đăng ký email + xác minh, đăng nhập, "Ghi nhớ" 30 ngày, đăng
xuất, đặt lại mật khẩu, Google sign-in, khóa tài khoản sau 5 lần sai, bốn vai trò với ma trận quyền,
đình chỉ/cấm tài khoản, nhật ký kiểm toán. 010 cũng thay `StubCurrentUser` và header `X-User-Id` bằng
danh tính thật, nên sau tính năng này không ai mạo danh được người khác.

Cách làm: module `com.wikicook.auth` (controller → service → repository) trên Spring Security 6.5.11.
Access JWT 15 phút trong bộ nhớ trình duyệt, refresh token xoay vòng trong cookie HttpOnly và
`user_sessions`. Mỗi request có bearer đối chiếu `user_sessions` + `users` bằng một query theo khóa
chính, nhờ đó đăng xuất, đặt lại mật khẩu, đổi vai trò, đình chỉ có hiệu lực ở request kế tiếp (research
R1). Mật khẩu được pre-hash SHA-256 rồi mới BCrypt cost 12, vì `BCrypt.hashpw` của 6.5.11 ném lỗi với
chuỗi trên 72 byte (R3). Khóa và giới hạn gửi email đếm theo hash của email trong PostgreSQL, kể cả email
chưa đăng ký, để không lộ tài khoản; không thêm Redis (R4, R5). Google dùng Authorization Code qua
`oauth2Login`, không có `HttpSession`; tiếp quản tài khoản chưa xác minh là xóa rồi tạo lại trong một
transaction (R7). Email có link gửi sau commit, email thông báo đi qua outbox (R8). Migration `V102`–
`V104` bổ sung `audit_logs`, `user_suspensions`, `blocked_emails`, bảng email và cột cho phiên (R11).
Frontend thêm `features/auth`, `AuthProvider`, refresh một đường duy nhất trong `shared/api/client.ts`,
guard theo vai trò và redirect chỉ về trang nội bộ (R12).

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5 + React 18 (frontend)

**Primary Dependencies**: Spring Boot 3.5.16 (Web, Data JPA, Validation, Security 6.5.11 theo BOM,
OAuth2 Resource Server + Nimbus JOSE, OAuth2 Client, Mail), springdoc-openapi, Flyway; Vite, Tailwind
CSS 3.4, React Router 6. Chưa có trong pom: security, oauth2, mail (xem Complexity Tracking)

**Storage**: PostgreSQL 16; `users`, `user_sessions`, `one_time_tokens`, `login_attempts` (`V101`), thêm
`audit_logs`, `user_suspensions`, `blocked_emails`, `password_check_failures`, `email_send_requests`, `email_outbox`
(`V102`–`V104`)

**Testing**: JUnit 5 + Mockito, `spring-security-test`, `@SpringBootTest` + Testcontainers
(`pgvector/pgvector:pg16`), WireMock cho Google, JaCoCo; Vitest + React Testing Library + `msw`, Playwright

**Target Platform**: Linux server (Docker Compose); trình duyệt hiện đại desktop và mobile từ 360 px

**Project Type**: Web application (backend REST + SPA)

**Performance Goals**: p95 đăng nhập < 2 s với 100 người dùng đồng thời (NFR-004); API có bearer p95 <
500 ms theo gate `v0.8`, `v1.0`

**Constraints**: Hiệu lực ở request kế tiếp (FR-010, FR-016, FR-024); không lộ email qua nội dung hay thời
gian (NFR-003); chạy được khi Google lỗi (NFR-005); secret qua biến môi trường; không `HttpSession`; hỗ
trợ nhiều instance; TLS phụ thuộc `TODO(HOSTING)` (NFR-002)

**Scale/Scope**: Đồ án: ~100 người dùng đồng thời, 9 route frontend mới, 20 endpoint, 4 job nền, phủ route
của 030, 031, 040 và mọi module sau này

Không còn mục NEEDS CLARIFICATION – xem [research.md](research.md) (R1–R17).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Ghi chú |
|---|---|---|
| I. Spec trước, code sau | ⚠️ Có điều kiện | Mã Jira đã có (SCRUM-72 tài liệu, SCRUM-74 backend, SCRUM-76 frontend). `spec.md` đã sửa ngày 2026-11-06, 2026-11-07, 2026-11-09 (mâu thuẫn FR-022 ↔ 100 FR-011 và các quyết định `OQ-010-xx`), Status là "In Review". **Không bắt đầu implement** trước khi PR SCRUM-72 được duyệt (T002). |
| II. Test-First | ✅ | Mỗi acceptance scenario có test (SC-007); tasks đặt test trước code; JaCoCo ≥ 70% cho `auth.service`; Testcontainers PostgreSQL thật, không H2; ma trận quyền 45 ca (R10, R14). |
| III. Kiến trúc phân lớp | ✅ | Controller → Service → Repository; DTO record, không trả entity; package theo tính năng `auth`; module khác qua `auth.api`, `common.security` và sự kiện. Thư viện mới ghi ở Complexity Tracking. |
| IV. Bảo mật & dữ liệu | ✅ | Spring Security; mật khẩu lưu dạng BCrypt (pre-hash chỉ để xử lý giới hạn 72 byte, R3); secret ở biến môi trường, `.env.example` không có giá trị thật; Bean Validation; query tham số hóa; ProblemDetail không lộ stack trace; không `dangerouslySetInnerHTML`, chỉ escape biến trong mẫu email. |
| V. Trải nghiệm nhất quán | ✅ | Responsive từ 360 px, chuỗi ở `features/auth/strings.vi.ts`; `TextField`, `PasswordField` đưa vào `shared/ui` cho 011 dùng lại; AI disclaimer không áp dụng. |
| Ràng buộc kỹ thuật | ✅ | Java 21, Spring Boot 3, PostgreSQL 16 + Flyway, React 18 + Vite, `/api/v1`, OpenAPI. Không dùng `ddl-auto=update`. |

**Kết quả gate**: PASS (điều kiện của Nguyên tắc I phải xong trước `/speckit-implement`).

**Re-check sau Phase 1**: PASS – thêm 6 bảng, đều có FR hoặc lý do ghi ở research R5, R8, R11, R17; contracts chỉ
dùng `/api/v1/auth/**`, `/api/v1/admin/**`; module khác đi qua `auth.api`; lệch `architecture.md`
("stateless", Redis) được ghi ở Complexity Tracking; không có vi phạm mới.

## Project Structure

### Documentation (this feature)

```text
specs/010-user-authentication-roles/
├── plan.md              # File này
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/
│   ├── openapi.yaml     # REST API /api/v1/auth, /api/v1/admin
│   └── integration.md   # Interface và sự kiện với 011, 030, 031, 040, 060, 070, 080, 100
└── tasks.md             # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
src/backend/
├── pom.xml
├── docker-compose.yml                 # thêm mailpit, biến của 010, FLYWAY_OUT_OF_ORDER
├── .env.example                       # thêm biến của 010, bỏ WIKICOOK_DEV_STUBS_USER_ID
├── perf/auth-sign-in.k6.js            # mới; meal-planner.k6.js chuyển sang đăng nhập thật
├── wikicook-db/src/main/resources/db/migration/
│   ├── V102__audit_and_suspensions.sql
│   ├── V103__auth_schema_updates.sql
│   └── V104__auth_mail.sql
└── wikicook-app/
    ├── pom.xml                        # starter security, oauth2, mail; rule JaCoCo cho auth.service
    └── src/
        ├── main/java/com/wikicook/
        │   ├── auth/
        │   │   ├── controller/   AuthController, VerificationController, PasswordResetController,
        │   │   │                 AccountAdminController, AuditLogController, AuthExceptionHandler,
        │   │   │                 SecurityExceptionAdvice
        │   │   ├── service/      RegistrationService, SignInService, SignInThrottle, SessionService,
        │   │   │                 AccessTokenService, OneTimeTokenService, EmailVerificationService,
        │   │   │                 PasswordResetService, GoogleSignInService, AccountAdminService,
        │   │   │                 AccountActionPolicy, AccountPurgeService, ReauthServiceImpl,
        │   │   │                 EmailRequestLimiter, EmailHasher, EmailNormalizer, PasswordServiceImpl,
        │   │   │                 PasswordCheckLimiter, BlockedEmailService,
        │   │   │                 AuditService
        │   │   ├── security/     SecurityConfig, PublicRoutes, PublicRouteBearerTokenResolver,
        │   │   │                 SessionJwtAuthenticationConverter,
        │   │   │                 SecurityContextCurrentAccount, PrehashedBCryptPasswordEncoder,
        │   │   │                 BcryptBulkhead, ClientHeaderFilter, AccountStateHeaderFilter, RefreshCookie,
        │   │   │                 ProblemResponseWriter, CookieOAuth2AuthorizationRequestRepository,
        │   │   │                 GoogleLoginHandlers
        │   │   ├── repository/   UserAccountRepository, UserSessionRepository, OneTimeTokenRepository,
        │   │   │                 LoginAttemptRepository, AuditLogRepository, UserSuspensionRepository,
        │   │   │                 BlockedEmailRepository, PasswordCheckFailureRepository,
        │   │   │                 EmailSendRequestRepository, EmailOutboxRepository
        │   │   ├── domain/       UserAccount, UserSession, OneTimeToken, LoginAttempt, AuditLog,
        │   │   │                 UserSuspension, BlockedEmail, PasswordCheckFailure, EmailSendRequest,
        │   │   │                 EmailOutboxMessage, TokenPurpose, SanctionKind, MailTemplate
        │   │   ├── dto/          RegisterRequest, LoginRequest, AuthResponse, CurrentAccountDto,
        │   │   │                 AccountAdminDto, AuditLogDto, SuspendRequest, BanRequest, ...
        │   │   ├── validation/   ValidPassword, ValidPasswordValidator, MaxCodePoints
        │   │   ├── mail/         LinkMailer, OutboxDispatcher, MailRenderer
        │   │   ├── job/          AuthJobsConfig, UnverifiedAccountPurgeJob, SuspensionExpiryJob,
        │   │   │                 AuthRetentionJob
        │   │   ├── api/          AccountGuard, BlockedEmails, SessionControl, ReauthService,
        │   │   │                 PasswordService, ClosureTokens, AccountAdminFacade, AuditWriter,
        │   │   │                 AccountResetEvent, AccountPurgedEvent
        │   │   └── config/       AuthProperties, AdminBootstrapRunner
        │   ├── common/security/  CurrentUser (giữ), UnauthenticatedException (giữ), CurrentAccount,
        │   │                     Role, AccountStatus, VerifiedEmailRequired, ModeratorOnly,
        │   │                     AdministratorOnly
        │   ├── shoppinglist/controller/ShoppingListController.java   # bỏ X-User-Id
        │   └── devstub/          # xóa StubCurrentUser, DevUserProvisioner; StubDietaryProfile ở lại tới 011
        ├── main/resources/
        │   ├── application.yml   # security, mail, flyway.out-of-order, wikicook.auth.*
        │   └── mail/             verify-email, password-reset, account-locked, account-suspended,
        │                         account-banned, suspension-ended, ban-lifted (.html + .txt)
        └── test/java/com/wikicook/
            ├── auth/
            │   ├── service/      unit test (Mockito), SignInThrottleIT, EmailRequestLimiterIT, …
            │   ├── security/     PrehashedBCryptPasswordEncoderTest, SessionCheckIT, SecurityFilterChainIT, …
            │   ├── controller/   SignInIT, RegistrationIT, PasswordResetIT, GoogleSignInIT (WireMock),
            │   │                 PermissionMatrixIT, RouteProtectionIT, SecuredRoutesIT, AccountAdminIT
            │   ├── repository/   AuthSchemaIT, EntityMappingIT, FlywayOutOfOrderIT
            │   ├── validation/, mail/, config/, api/   test theo tasks.md
            │   └── job/          UnverifiedAccountPurgeJobIT, SuspensionExpiryJobIT, OutboxDispatcherIT
            └── support/          HeaderCurrentUser (chuyển từ mealplan/support, @Primary + filter chain test),
                                  AuthTestSupport, RecordingMailSender, MatrixProbeController (ngoài gói auth)

src/frontend/
├── nginx.conf                         # CSP, Referrer-Policy
├── e2e/auth.spec.ts, auth-reset.spec.ts, auth-responsive.spec.ts, auth-a11y.spec.ts
├── e2e/fakeApi.ts                     # thêm refresh, me
└── src/
    ├── app/routes.tsx, AppLayout.tsx  # route auth, guard, banner, thông báo vai trò
    ├── shared/
    │   ├── api/client.ts, authToken.ts, safeRedirect.ts
    │   └── ui/TextField.tsx, PasswordField.tsx
    ├── features/auth/
    │   ├── AuthProvider.tsx, useAuth.ts, RequireAuth.tsx, RequireRole.tsx,
    │   │   RedirectIfAuthenticated.tsx
    │   ├── api/authApi.ts, types.ts
    │   ├── pages/        LoginPage, RegisterPage, CheckEmailPage, VerifyEmailPage,
    │   │                 ForgotPasswordPage, ResetPasswordPage, AccountStatusPage,
    │   │                 AccessDeniedPage, AuthCallbackPage
    │   ├── components/   GoogleButton, PasswordRules, SignInPrompt, SessionExpiredDialog,
    │   │                 RoleChangeNotice, EmailVerificationBanner
    │   ├── lib/passwordRules.ts
    │   ├── strings.vi.ts
    │   └── __tests__/    Vitest + RTL + msw
    └── test/server.ts    # mặc định cho /auth/refresh
```

**Structure Decision**: Web application theo bố cục thật của repo: backend Maven multi-module ở
`src/backend` (code trong `wikicook-app`, migration trong `wikicook-db`), frontend ở `src/frontend`. Module
`auth` tự chứa đủ lớp; điểm nối với module khác nằm ở `auth/api` và `common/security`, để các feature
không phụ thuộc `auth.service`. Không đụng `src/specs/`.

## Phases (tóm tắt để /speckit-tasks chia việc)

| Thứ tự | Nội dung | User story |
|---|---|---|
| 1 | Starter vào pom, `V102`–`V104`, `out-of-order`, entity, `SecurityConfig`, JWT, bean `CurrentUser`; `HeaderCurrentUser`, sửa test 030/031/040, bỏ `X-User-Id` khỏi controller (R13) | nền tảng |
| 2 | Frontend nền: `client.ts`, `AuthProvider`, guard, `safeRedirect`, `TextField`, `PasswordField`, route | nền tảng |
| 3 | Đăng nhập, phiên, refresh, đăng xuất, khóa, `dummyHash`, bulkhead; `LoginPage`, màn khóa, Back sau đăng xuất | US1 (P1) |
| 4 | Đăng ký, `@ValidPassword`, xác minh, `LinkMailer`, FR-017; `RegisterPage`, `CheckEmailPage`, `VerifyEmailPage`, banner | US2 (P1) |
| 5 | Ma trận quyền, `AccountActionPolicy`, API admin, xác thực lại, `AdminBootstrapRunner`, audit; `PermissionMatrixIT`, `RouteProtectionIT`, `SecuredRoutesIT`, AU08, AU09, `RoleChangeNotice` | US3 (P1) |
| 6 | Đặt lại mật khẩu, thu hồi phiên; `ForgotPasswordPage`, `ResetPasswordPage` | US4 (P2) |
| 7 | Google sign-in, tiếp quản, `AccountResetEvent`, WireMock IT; `GoogleButton`, `AuthCallbackPage` | US5 (P3) |
| 8 | Outbox, email đình chỉ/cấm/khóa, 3 job, Blocked Email, `closureToken`, `auth.api` cho 011 và 100 | tích hợp |
| 9 | Gỡ `StubCurrentUser` và biến môi trường, CSP, chuyển k6 030, `auth-sign-in.k6.js`, đo BCrypt | hoàn thiện |

## Complexity Tracking

| Bổ sung | Lý do cần | Phương án đơn giản hơn bị loại vì |
|---|---|---|
| `spring-boot-starter-oauth2-resource-server` (kéo theo Spring Security, Nimbus JOSE) | Constitution IV bắt buộc Spring Security; có sẵn ký, xác minh JWT (R1) | Tự viết JWT – rủi ro bảo mật; thư viện JWT riêng như jjwt – vẫn phải tự nối vào filter chain |
| `spring-boot-starter-oauth2-client` | Google Authorization Code, `state`, `nonce`, kiểm tra ID token (R7) | Tự viết OAuth – tự chịu kiểm tra an toàn; Google Identity Services – không ép xác thực lại được |
| `spring-boot-starter-mail` + service Mailpit ở Compose | FR-005, FR-012, FR-015, FR-024a cần gửi email; dev không tốn dịch vụ ngoài (R8) | Chỉ log link – không kiểm chứng được template và SC-001; SMTP thật ở local – cần tài khoản |
| `spring-security-test`, `wiremock-standalone` (test scope ở `wikicook-app`) | MockMvc với security; giả lập Google trong CI (R14) | Mock thủ công `OAuth2User` – bỏ qua đúng phần cần test là đổi code và kiểm tra token |
| Một query DB ở mỗi request có bearer (lệch "stateless" của `architecture.md`) | FR-010, FR-016, FR-024 yêu cầu hiệu lực ở request kế tiếp (R1) | JWT thuần – quyền cũ còn tới 15 phút; cache – vẫn trễ và sai giữa instance |
| Không thêm Redis (lệch `architecture.md`, project plan) | Compose không có; PostgreSQL đủ cho FR-012, FR-017 ở quy mô đồ án (R5) | Thêm Redis – thêm service, thêm phụ thuộc CI mà không có tính năng nào cần |
| Pre-hash SHA-256 trước BCrypt | `BCrypt.hashpw` 6.5.11 ném lỗi trên 72 byte, FR-004 cho 128 ký tự có dấu (R3) | Giới hạn 72 byte – trái FR-004; cắt chuỗi – bỏ qua phần sau của mật khẩu |
| `BcryptBulkhead` (Semaphore) | BCrypt chiếm CPU trên virtual thread, có thể làm đói API khác (R14) | Không giới hạn – một đợt đăng nhập ảnh hưởng toàn hệ thống |
| Bảng `email_send_requests` | FR-017 phải đếm cả email lạ để 429 không lộ tài khoản (R5) | Đếm trên `one_time_tokens` – chỉ có email đã đăng ký |
| Bảng `password_check_failures` | FR-022b cần bộ đếm tách khỏi đăng nhập (R17) | Dùng `login_attempts` – kẻ giữ phiên bị đánh cắp khóa được đăng nhập của chủ tài khoản |
| Bảng `email_outbox` và hai kênh gửi | Email FR-012, FR-024a không có đường gửi lại; link không được lưu token thô (R8) | Một kênh outbox – phải lưu token thô; gửi trong request – chậm và lộ thời gian |
| `CookieOAuth2AuthorizationRequestRepository` | `oauth2Login` mặc định cần `HttpSession`, hệ thống không dùng (R7) | `HttpSession` – cần sticky session khi nhiều instance |
| `spring.flyway.out-of-order: true` | `V102`–`V104` thấp hơn `V403` đã chạy (R11) | Đổi số `V5xx` – phá dải số; xóa DB – mất dữ liệu dev, không dùng được cho build đã tag |
| Xóa rồi tạo lại `users` khi tiếp quản bằng Google | FR-019 yêu cầu sạch mọi dữ liệu, kể cả của module tương lai (R7) | Sửa tại chỗ – dựa vào listener của mọi module, dễ sót |
