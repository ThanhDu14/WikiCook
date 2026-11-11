# Data Model: User Authentication & Roles – 010

**Feature**: [spec.md](spec.md) | **Research**: [research.md](research.md)

Bảng gốc lấy từ [full-schema.md mục 4.1, 4.10](../../docs/analysis-and-design/database/full-schema.md#41-tài-khoản-bảo-mật-và-hồ-sơ-010-011--a),
migration `V101__users_auth.sql` (A) đã có. Cột `created_at`, `updated_at` theo quy ước chung. Spec
này **thêm ba migration** ở `src/backend/wikicook-db/src/main/resources/db/migration/` (research R11):

| File | Thay đổi | Thuộc về |
|---|---|---|
| `V102__audit_and_suspensions.sql` | Tạo `audit_logs`, `user_suspensions` (chuyển sớm từ `V503`) | A làm, E sở hữu nghiệp vụ 100 |
| `V103__auth_schema_updates.sql` | Tạo `blocked_emails`, `password_check_failures`; sửa `users`, `user_sessions`, `login_attempts`, `one_time_tokens` | A |
| `V104__auth_mail.sql` | Tạo `email_send_requests`, `email_outbox` | A |

Cần `spring.flyway.out-of-order: true` và `FLYWAY_OUT_OF_ORDER: "true"` vì DB đã ở `V403`.
Sau khi nhóm duyệt, [full-schema.md](../../docs/analysis-and-design/database/full-schema.md) phải được
cập nhật theo file này (bảng "Điểm cần chốt" trong research, mục 20).

## 1. Bảng sở hữu

**Quy ước map kiểu** (`ddl-auto: validate`, research R11): mọi cột `CHAR(64)` dùng
`@JdbcTypeCode(SqlTypes.CHAR)` + `@Column(length = 64)` cho field `String`; cột `JSONB` dùng
`@JdbcTypeCode(SqlTypes.JSON)`; cột `INET` dùng `@JdbcTypeCode(SqlTypes.INET)` với `java.net.InetAddress`.
Thiếu các khai báo này thì ứng dụng không khởi động được.

### `users` → entity `UserAccount`

010 chỉ map các cột của mình; `avatar_storage_key`, `diet_type`, `household_size`,
`max_cook_time_minutes` thuộc 011 (`ddl-auto=validate` chỉ kiểm tra cột đã map). Entity có
`@DynamicUpdate`, để cập nhật của 010 không ghi đè `display_name` mà 011 vừa đổi (011 research R4).

| Cột | Kiểu | Ràng buộc | Field Java | FR |
|---|---|---|---|---|
| `id` | `BIGINT` identity | PK | `Long id` | |
| `email` | `VARCHAR(254)` | NOT NULL, UK, `CHECK (email = lower(trim(email)))` | `String email` | FR-002 |
| `password_hash` | `VARCHAR(100)` | NULL (Google-only, hệ thống) | `String passwordHash` | FR-007, NFR-001 |
| `google_sub` | `VARCHAR(255)` | NULL, UK | `String googleSub` | FR-018, FR-019 |
| `display_name` | `VARCHAR(50)` | NOT NULL | `String displayName` | FR-001 |
| `role` | `VARCHAR(15)` | NOT NULL, `CHECK`, mặc định `MEMBER` | `@Enumerated(STRING) Role role` | FR-003, FR-020 |
| `status` | `VARCHAR(10)` | NOT NULL, `CHECK IN ('ACTIVE','SUSPENDED','BANNED')` | `@Enumerated(STRING) AccountStatus status` | FR-013, FR-024 |
| `email_verified_at` | `TIMESTAMPTZ` | NULL | `Instant emailVerifiedAt` | FR-005, FR-006 |
| `locked_until` | `TIMESTAMPTZ` | NULL | `Instant lockedUntil` | FR-012 |
| `last_sign_in_at` | `TIMESTAMPTZ` | NULL | `Instant lastSignInAt` | Key Entities |
| `pending_role_notice_from` 🆕 | `VARCHAR(15)` | NULL, `CHECK` như `role` | `@Enumerated(STRING) Role pendingRoleNoticeFrom` | FR-024a |
| `is_system` | `BOOLEAN` | NOT NULL | `boolean system` | |
| `created_at`, `updated_at` | `TIMESTAMPTZ` | NOT NULL | `Instant` | FR-005 (mốc 7 ngày) |

- 🆕 Index `(created_at) WHERE email_verified_at IS NULL` cho `UnverifiedAccountPurgeJob`.
- `User` không có trạng thái "deleted": spec 011 xóa hẳn dòng.
- `Role`, `AccountStatus` đặt ở `com.wikicook.common.security` để module khác không phụ thuộc `auth`.

### `user_sessions` → entity `UserSession`

| Cột | Kiểu | Ràng buộc | Field Java | FR |
|---|---|---|---|---|
| `id` | `BIGINT` identity | PK; là claim `sid` của JWT | `Long id` | FR-010 |
| `user_id` | `BIGINT` | NOT NULL, FK → `users` `CASCADE` | `Long userId` | |
| `token_hash` | `CHAR(64)` | NOT NULL, UK | `String tokenHash` | |
| `previous_token_hash` 🆕 | `CHAR(64)` | NULL | `String previousTokenHash` | R2 |
| `rotated_at` 🆕 | `TIMESTAMPTZ` | NULL | `Instant rotatedAt` | R2 |
| `remember_me` | `BOOLEAN` | NOT NULL | `boolean rememberMe` | FR-009 |
| `last_activity_at` | `TIMESTAMPTZ` | NOT NULL | `Instant lastActivityAt` | FR-009 |
| `expires_at` | `TIMESTAMPTZ` | NOT NULL | `Instant expiresAt` | FR-009 |
| `revoked_at` | `TIMESTAMPTZ` | NULL | `Instant revokedAt` | FR-010, FR-016, FR-024 |
| `reauthenticated_at` 🆕 | `TIMESTAMPTZ` | NULL | `Instant reauthenticatedAt` | FR-022a |
| `user_agent` | `VARCHAR(255)` | NULL | `String userAgent` | |

- Index sẵn có `(user_id) WHERE revoked_at IS NULL` phục vụ thu hồi hàng loạt; 🆕 index
  `(previous_token_hash) WHERE previous_token_hash IS NOT NULL` để phát hiện dùng lại; 🆕 index
  `(expires_at)` cho `AuthRetentionJob`.
- Query mỗi request là JOIN theo PK `users` và `user_sessions` (research R1), không cần index mới.

### `one_time_tokens` → entity `OneTimeToken`

| Cột | Kiểu | Ràng buộc | Field Java | FR |
|---|---|---|---|---|
| `id` | `BIGINT` identity | PK | `Long id` | |
| `user_id` | `BIGINT` | NOT NULL, FK → `users` `CASCADE` | `Long userId` | |
| `purpose` | `VARCHAR(20)` | NOT NULL, `CHECK IN ('EMAIL_VERIFICATION','PASSWORD_RESET','ACCOUNT_CLOSURE')` 🆕 | `@Enumerated(STRING) TokenPurpose purpose` | FR-005, FR-015, R16 |
| `token_hash` | `CHAR(64)` | NOT NULL, UK; SHA-256 | `String tokenHash` | FR-005, FR-015 |
| `expires_at` | `TIMESTAMPTZ` | NOT NULL; 24 giờ, 30 phút, 10 phút | `Instant expiresAt` | |
| `used_at` | `TIMESTAMPTZ` | NULL; đã dùng hoặc bị token mới thay | `Instant usedAt` | FR-015 |

### `login_attempts` → entity `LoginAttempt`

Chỉ là bộ đếm khóa, giữ **1 ngày** (không phải 90 ngày như full-schema; nhật ký ở `audit_logs`).

| Cột | Kiểu | Ràng buộc | Field Java | FR |
|---|---|---|---|---|
| `id` | `BIGINT` identity | PK | `Long id` | |
| `user_id` | `BIGINT` | NULL, FK → `users` `CASCADE`; NULL khi email lạ | `Long userId` | |
| `email_hash` 🆕 | `CHAR(64)` | NOT NULL; thay `email_normalized` | `String emailHash` | FR-012, NFR-003 |
| `success` | `BOOLEAN` | NOT NULL; chèn trước là `false` | `boolean success` | FR-012 |
| `ip_address` | `INET` | NULL | `InetAddress ipAddress` | R5 |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | `Instant createdAt` | |

- 🆕 Index `(email_hash, created_at DESC)`, `(ip_address, created_at DESC)`. Bảng chưa có dữ liệu
  (chưa có code ghi), nên `V103` đổi cột trực tiếp.

### `audit_logs` → entity `AuditLog` *(tạo ở `V102`, chỉ `created_at`, append-only)*

| Cột | Kiểu | Ràng buộc | Field Java | FR |
|---|---|---|---|---|
| `id` | `BIGINT` identity | PK | `Long id` | |
| `actor_id` | `BIGINT` | NULL, FK → `users` `SET NULL` | `Long actorId` | FR-025 |
| `action` | `VARCHAR(50)` | NOT NULL | `String action` | FR-025 |
| `target_type` | `VARCHAR(30)` | NULL; cố ý không FK | `String targetType` | |
| `target_id` | `BIGINT` | NULL | `Long targetId` | |
| `details` | `JSONB` | NULL; **không bao giờ chứa email, tên hiển thị** | `Map<String,Object> details` | FR-025, NFR-009 |
| `ip_address` | `INET` | NULL | `InetAddress ipAddress` | |

- Index `(created_at DESC)`, `(target_type, target_id)`.
- `action` của 010: `SIGN_IN_SUCCEEDED`, `SIGN_IN_FAILED`, `ACCOUNT_LOCKED`, `SIGNED_OUT`,
  `PASSWORD_RESET`, `REFRESH_TOKEN_REUSED`, `ROLE_CHANGED`, `ACCOUNT_SUSPENDED`, `ACCOUNT_BANNED`,
  `ACCOUNT_REINSTATED`, `ACCOUNT_RESET`, `ACCOUNT_PURGED_UNVERIFIED`.
- `details.reason` của `SIGN_IN_FAILED`: `BAD_CREDENTIALS`, `LOCKED`, `SUSPENDED`, `BANNED`; email lạ có
  thêm `details.emailHash`.
- Spec 100 lọc nhật ký theo đúng các tên này cho thao tác tài khoản (không dùng `USER_SUSPENDED`), xem
  research bảng cuối, mục 42.

### `user_suspensions` → entity `UserSuspension` *(tạo ở `V102`)*

| Cột | Kiểu | Ràng buộc | Field Java | FR |
|---|---|---|---|---|
| `id` | `BIGINT` identity | PK | `Long id` | |
| `user_id` | `BIGINT` | NOT NULL, FK → `users` `CASCADE` | `Long userId` | FR-013 |
| `kind` | `VARCHAR(10)` | NOT NULL, `CHECK IN ('SUSPENSION','BAN')` | `@Enumerated(STRING) SanctionKind kind` | FR-013 |
| `reason` | `VARCHAR(500)` | NOT NULL | `String reason` | FR-013, FR-024a |
| `starts_at` | `TIMESTAMPTZ` | NOT NULL | `Instant startsAt` | |
| `ends_at` | `TIMESTAMPTZ` | NULL, `CHECK ((kind = 'BAN') = (ends_at IS NULL))` | `Instant endsAt` | FR-013 |
| `created_by` | `BIGINT` | NULL, FK → `users` `SET NULL` | `Long createdBy` | FR-025 |
| `lifted_at` | `TIMESTAMPTZ` | NULL | `Instant liftedAt` | FR-024a |
| `lifted_by` | `BIGINT` | NULL, FK → `users` `SET NULL`; NULL + có `lifted_at` = hết hạn tự nhiên | `Long liftedBy` | FR-024a |

- 🆕 `UNIQUE (user_id) WHERE lifted_at IS NULL`: mỗi tài khoản tối đa một án đang chạy. Cấm khi đang
  đình chỉ thì đóng án cũ trước.
- 🆕 Index `(ends_at) WHERE lifted_at IS NULL AND kind = 'SUSPENSION'` cho `SuspensionExpiryJob`.
- `users.status` đổi cùng transaction với bảng này.

### `blocked_emails` → entity `BlockedEmail` 🆕 *(`V103`)*

| Cột | Kiểu | Ràng buộc | Field Java | FR |
|---|---|---|---|---|
| `email_hash` | `CHAR(64)` | PK; HMAC-SHA256 của email chuẩn hóa | `String emailHash` | FR-026 |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | `Instant createdAt` | |

Không có FK và không có email rõ. Chỉ 010 đọc (registration, Google).

### `password_check_failures` → entity `PasswordCheckFailure` 🆕 *(`V103`, chỉ `created_at`, giữ 1 ngày)*

Bộ đếm mật khẩu sai khi đã đăng nhập, tách khỏi `login_attempts` (research R17).

| Cột | Kiểu | Ràng buộc | Field Java | FR |
|---|---|---|---|---|
| `id` | `BIGINT` identity | PK | `Long id` | |
| `user_id` | `BIGINT` | NOT NULL, FK → `users` `CASCADE` | `Long userId` | FR-022b |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | `Instant createdAt` | |

Index `(user_id, created_at DESC)`.

### `email_send_requests` → entity `EmailSendRequest` 🆕 *(`V104`, chỉ `created_at`, giữ 1 ngày)*

| Cột | Kiểu | Ràng buộc | Field Java | FR |
|---|---|---|---|---|
| `id` | `BIGINT` identity | PK | `Long id` | |
| `email_hash` | `CHAR(64)` | NOT NULL | `String emailHash` | FR-017 |
| `purpose` | `VARCHAR(20)` | NOT NULL, `CHECK IN ('EMAIL_VERIFICATION','PASSWORD_RESET')` | `@Enumerated(STRING) TokenPurpose purpose` | FR-017 |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | `Instant createdAt` | |

Index `(email_hash, purpose, created_at DESC)`. Không có `user_id`, vì phải đếm cả email chưa đăng ký.

### `email_outbox` → entity `EmailOutboxMessage` 🆕 *(`V104`)*

| Cột | Kiểu | Ràng buộc | Field Java | FR |
|---|---|---|---|---|
| `id` | `BIGINT` identity | PK | `Long id` | |
| `user_id` | `BIGINT` | NOT NULL, FK → `users` `CASCADE`; email đọc từ `users` lúc gửi | `Long userId` | |
| `template` | `VARCHAR(30)` | NOT NULL, `CHECK IN ('ACCOUNT_LOCKED','ACCOUNT_SUSPENDED','ACCOUNT_BANNED','SUSPENSION_ENDED','BAN_LIFTED')` | `@Enumerated(STRING) MailTemplate template` | FR-012, FR-024a |
| `payload` | `JSONB` | NOT NULL; lý do, ngày; không có token | `Map<String,Object> payload` | FR-024a |
| `attempts` | `SMALLINT` | NOT NULL, mặc định 0 | `short attempts` | |
| `next_attempt_at` | `TIMESTAMPTZ` | NOT NULL | `Instant nextAttemptAt` | |
| `sent_at`, `failed_at` | `TIMESTAMPTZ` | NULL | `Instant` | |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | `Instant createdAt` | |

Index `(next_attempt_at) WHERE sent_at IS NULL AND failed_at IS NULL`. Email có link (FR-005, FR-015)
không đi qua bảng này vì không được lưu token thô (research R6, R8).

## 2. Bảng chỉ đọc

010 không đọc bảng của module khác. Thay vào đó, xóa một tài khoản chưa xác minh có tác động tới các
bảng sau qua FK (full-schema mục 6); phải biết để hiểu `AccountResetEvent`, `AccountPurgedEvent`:

| Nguồn | Hành vi khi `DELETE users` | Việc của module |
|---|---|---|
| `user_allergens`, `user_excluded_ingredients`, `user_preferred_cuisines` (011), `collections` (080), `meal_plans` (030), `ai_requests.user_id` (031), `shopping_lists` (040) | `CASCADE` | Không cần làm gì, trừ file đi kèm (avatar của 011) |
| `recipes.author_id` (060) | `SET NULL` | **060 phải xóa công thức trong listener**, nếu không bản nháp mồ côi |
| `reviews`, `cooksnaps` (070) | `SET NULL` | Tài khoản chưa xác minh không tạo được (FR-006) |

## 3. Value object và DTO chính (record Java)

| Tên | Trường | Ghi chú |
|---|---|---|
| `RegisterRequest` | `displayName, email, password, confirmPassword` | FR-001 |
| `RegisterResponse` | `email, verificationEmailSent, verificationExpiresAt?, retryAfterSeconds?` | Không tạo phiên. Quá giới hạn FR-017: `verificationEmailSent = false`, `verificationExpiresAt = null` (chưa cấp link), `retryAfterSeconds` cho thông báo "thử lại sau X phút" |
| `LoginRequest` | `email, password, rememberMe` | Password không trim; tối đa 128 code point (`@MaxCodePoints`), không dùng `@Size` |
| `AuthResponse` | `accessToken, expiresIn, account` | Refresh token đi qua cookie |
| `CurrentAccountDto` | `id, displayName, email, role, emailVerified, roleNotice?` | NFR-009: email chỉ của chính mình |
| `RoleNoticeDto` | `previousRole, newRole` | FR-024a |
| `ConfirmTokenRequest` | `token` | |
| `VerificationResult` | `status` (`VERIFIED`, `ALREADY_VERIFIED`) | AF 1.12 |
| `ResendVerificationRequest` | `email?, token?` | Đúng một trong hai; `token` cho link hết hạn (AU04) |
| `ForgotPasswordRequest` | `email` | Luôn 202, trừ 429 |
| `ResetTokenInfo` | `email, expiresAt` | Màn AU06 hiện email |
| `ResetPasswordRequest` | `token, newPassword, confirmPassword` | |
| `ReauthenticateRequest` | `password` | FR-022a; tối đa 128 code point (`@MaxCodePoints`) |
| `ChangeRoleRequest` | `role` | |
| `SuspendRequest` | `reason, endsAt` | `endsAt` tối đa 30 ngày, mọi vai trò (FR-022) |
| `BanRequest` | `reason` | |
| `ReinstateRequest` | `reason?` | |
| `AccountAdminDto` | `id, displayName, email, role, status, emailVerified, activeSanction?` | Email chỉ cho Administrator |
| `AuditLogDto` | `id, actorId, action, targetType, targetId, details, ipAddress, createdAt` | NFR-008 |
| `SessionSnapshot` | `sessionId, userId, role, status, emailVerified, revokedAt, expiresAt, lastActivityAt, pendingRoleNoticeFrom` | Kết quả query mỗi request, nội bộ |

## 4. Quy tắc kiểm tra (Bean Validation + Service)

| Quy tắc | Nơi kiểm tra | Nguồn |
|---|---|---|
| Email: trim + lowercase rồi mới validate, ≤ 254, đúng định dạng | `EmailNormalizer`, `@Email`, `CHECK` DB | FR-002, Edge Cases |
| Tên hiển thị: trim, 1–50 ký tự | `@NotBlank`, `@Size` | FR-001, AF 1.5 |
| Mật khẩu: 8–128 code point sau NFC, có `\p{L}`, có `\p{Nd}`, khác email | `@ValidPassword` | FR-004, research R3 |
| Mật khẩu khi đăng nhập, xác thực lại: tối đa 128 code point, quá thì 400 và không tính lần thử | `@MaxCodePoints(128)` | FR-004, FR-007, research R3 |
| Mật khẩu đặt lại khác email của tài khoản | `PasswordResetService`, sau khi đọc tài khoản từ token, trước khi tiêu thụ token → 400 `PASSWORD_EQUALS_EMAIL` | FR-004, research R6 |
| Xác nhận mật khẩu khớp | Bean Validation mức lớp | FR-001 |
| Email không trùng | `INSERT … ON CONFLICT (email) DO NOTHING` → 409 `EMAIL_TAKEN` | FR-002, double-click |
| Email không thuộc Blocked Email | `RegistrationService`, `GoogleSignInService` → 422 `REGISTRATION_REFUSED` | FR-026 |
| Khóa đăng nhập | `SignInThrottle` (advisory lock) | FR-012, SC-005 |
| Tối đa 3 email/15 phút mỗi địa chỉ, mỗi loại | `EmailRequestLimiter` | FR-017 |
| Token còn hạn, chưa dùng | UPDATE có điều kiện | FR-005, FR-015 |
| `endsAt` ở tương lai; `reason` 1–500 ký tự | Bean Validation | FR-013 |
| Không tự tác động lên mình; đúng thứ bậc | `AccountActionPolicy` | FR-022a |
| Còn ít nhất một Administrator đang hoạt động | `AccountGuard`, `SELECT … FOR UPDATE` | FR-023 |
| Xác thực lại trong 5 phút | `ReauthService` | FR-022a |
| Tối đa 5 lần sai mật khẩu trong 15 phút, chung với đổi mật khẩu của 011 | `PasswordCheckLimiter` trong `checkCurrent` → `429 PASSWORD_CHECK_LIMITED` | FR-022b |
| Email đã xác minh khi đăng nội dung | `@VerifiedEmailRequired` | FR-006 |

## 5. Trạng thái

```text
  tài khoản:  (chưa có) ── register ──▶ UNVERIFIED ── verify ──▶ VERIFIED
                               │                  └─ Google cùng email: xóa, tạo mới ──▶ VERIFIED
                               └─ quá 7 ngày ──▶ (đã xóa, email tự do)

  status:     ACTIVE ── suspend ──▶ SUSPENDED ── hết hạn / reinstate ──▶ ACTIVE
              ACTIVE, SUSPENDED ── ban ──▶ BANNED ── reinstate ──▶ ACTIVE
              BANNED ── tự xóa (011) ──▶ (đã xóa, hash vào blocked_emails)

  khóa:       không khóa ── 5 lần sai ──▶ khóa 15 phút ── hết giờ ──▶ không khóa

  phiên:      ACTIVE ── refresh ──▶ ACTIVE (token mới)
              ACTIVE ── logout / reset / suspend / ban / dùng lại token cũ ──▶ REVOKED
              ACTIVE ── quá expires_at ──▶ EXPIRED

  token:      UNUSED ── consume ──▶ USED;  UNUSED ── token mới ──▶ USED;  UNUSED ── quá hạn ──▶ EXPIRED
```

- `UNVERIFIED`, `VERIFIED`, `EXPIRED`, `khóa` không phải cột riêng: suy ra từ `email_verified_at`,
  `expires_at`, `login_attempts`.
- Đổi vai trò không đổi trạng thái; chỉ đặt `pending_role_notice_from` và có hiệu lực ở request kế tiếp.
- Google chỉ tiếp quản tài khoản chưa xác minh đang `ACTIVE`; đang `SUSPENDED`, `BANNED` thì không đổi gì (FR-019).
- Khóa (FR-012) độc lập với `status`: hết khóa không gửi email.
