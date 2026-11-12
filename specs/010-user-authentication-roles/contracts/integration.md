# Contracts nội bộ – User Authentication & Roles (010)

Các interface Java giữa module `auth` và module khác trong cùng ứng dụng Spring Boot. Đây là chữ ký đề
xuất để các bên cùng duyệt. Kiểu dùng chung nằm ở `com.wikicook.common.security`; kiểu riêng của 010
nằm ở `com.wikicook.auth.api`, để module khác không import `auth.service`.

## 1. Những gì 010 cần từ module khác

010 không có port đọc dữ liệu của module khác. Điều 010 cần là **listener xóa dữ liệu** và **thông tin
truyền vào lệnh**. Sự kiện chạy **đồng bộ, trong transaction của 010** (`@EventListener`, không dùng
`@TransactionalEventListener`): listener ném lỗi thì cả thao tác rollback (`OQ-010-07`). Chưa có
listener thì không có gì xảy ra, nên 010 chạy được trước các module kia.

```java
/** com.wikicook.auth.api. Google tiếp quản tài khoản chưa xác minh (FR-019). */
public record AccountResetEvent(long userId) {}

/** com.wikicook.auth.api. Tài khoản chưa xác minh quá 7 ngày (FR-005). */
public record AccountPurgedEvent(long userId) {}
```

Ngay sau listener, 010 chạy `DELETE FROM users`; mọi bảng `CASCADE` trong full-schema mục 6 tự sạch.
Listener chỉ cần lo phần `CASCADE` không làm được:

| Module | Bắt buộc | Lý do |
|---|---|---|
| 060 (B) | **Có**: xóa mọi công thức của `userId` | `recipes.author_id` là `SET NULL`, không xóa thì bản nháp mồ côi |
| 011 (A) | Có: xóa file avatar | `avatar_storage_key` nằm ở storage, bio và dị ứng đi theo `users` |
| 080 (A) | Không: `collections` đã `CASCADE` | Chỉ cần nếu sau này có file đi kèm |
| 030, 031, 040 | Không | `meal_plans`, `ai_requests`, `shopping_lists` đã `CASCADE`. Spec chỉ nêu 011, 060, 080; thiết kế xóa rồi tạo lại phủ luôn phần "any other data" của FR-019 |

Lệnh của 100 mang sẵn lý do và ngày kết thúc cho email (FR-024a), qua tham số `AccountAdminFacade`.
Không có quy trình khiếu nại: email ghi địa chỉ từ `wikicook.auth.appeal-contact` (research R8).

## 2. Interface mà 010 cung cấp

### 2.1. Cho mọi module

```java
/** com.wikicook.common.security – đã có, giữ nguyên id(). 010 cung cấp bean thật. */
public interface CurrentUser {
    /** users.id; không có người đăng nhập → UnauthenticatedException (401 UNAUTHENTICATED). */
    long id();
}

/** com.wikicook.common.security – mở rộng cho module cần vai trò. */
public interface CurrentAccount extends CurrentUser {
    Role role();
    boolean emailVerified();
    long sessionId();
}

/** com.wikicook.common.security – dùng trên controller hoặc service (FR-006, FR-021). */
@VerifiedEmailRequired   // 403 EMAIL_NOT_VERIFIED (FR-006)
@ModeratorOnly           // Moderator trở lên (FR-021)
@AdministratorOnly       // (FR-021, FR-022)
```

Quy ước mới cho mọi controller: không đọc user id từ header, query hay body; route `/api/**` mặc định
cần đăng nhập. Lỗi thiếu quyền ở tầng method đi qua `SecurityExceptionAdvice` toàn cục (403
`ACCESS_DENIED` hoặc `EMAIL_NOT_VERIFIED`, research R15); advice riêng của module không được bắt
`AccessDeniedException` hay `Exception`. 020 đã có route công khai (`GET /api/v1/recipes/search`, `/search/options`, `/featured`), 010 khai báo sẵn ở `PublicRoutes`; module nào có thêm route công khai (060, ...) thêm vào `PublicRoutes` và `RouteProtectionIT`
sẽ kiểm tra phần còn lại.

### 2.2. Cho Profile và Account deletion (spec 011, A)

```java
/** com.wikicook.auth.api */
public interface AccountGuard {
    /**
     * Tự xóa (FR-023, FR-026). Đang đình chỉ: 403 ACCOUNT_SUSPENDED kèm reason, endsAt.
     * Administrator đang hoạt động cuối cùng: 409 LAST_ADMINISTRATOR.
     */
    void assertSelfDeletionAllowed(long userId);
}

public interface BlockedEmails {
    /** Gọi trong cùng transaction xóa của 011, trước khi xóa users, chỉ khi tài khoản BANNED (FR-026). */
    void block(String normalizedEmail);
}

public interface SessionControl {
    /** Đổi mật khẩu: giữ phiên hiện tại (011 FR-028). */
    void revokeAllExcept(long userId, long keepSessionId);
    void revokeAll(long userId);
}

public interface ReauthService {
    /** 403 REAUTH_REQUIRED nếu chưa xác thực lại trong khoảng maxAge (011 FR-029, FR-031; 010 FR-022a). */
    void requireRecent(long sessionId, Duration maxAge);
}

public interface PasswordService {
    /** Cùng quy tắc FR-004 và cùng encoder (research R3). */
    void validate(String password, String normalizedEmail);
    String hash(String password);
    /**
     * Đi qua bulkhead. Bộ đếm chung với /auth/reauthenticate, tách khỏi sign-in (FR-022b, research
     * R17): 5 lần sai trong 15 phút → 429 PASSWORD_CHECK_LIMITED kèm retryAfterSeconds.
     */
    boolean checkCurrent(long userId, String password);
}

public interface ClosureTokens {
    /**
     * Trả userId nếu token ACCOUNT_CLOSURE hợp lệ, dùng một lần (research R16). Chạy trong transaction
     * của bên gọi, không dùng REQUIRES_NEW: 011 rollback thì token dùng lại được (011 FR-037b, US4 AS9).
     */
    OptionalLong consume(String token);
}
```

011 trực tiếp: `PF06`, `W-PF06` (đổi, đặt mật khẩu) dùng lại `PasswordField` và `POST
/auth/reauthenticate`; tài khoản chỉ Google chuyển tới `GET /auth/oauth2/google?intent=reauth&redirect=…`; hủy hoặc lỗi thì
quay về `redirect` với `?reauth=cancelled` (research R7). Sau khi
011 xóa `users`, email đăng ký lại được ngay (FR-002): `login_attempts` đi theo `CASCADE`; bộ đếm
`email_send_requests` không gắn tài khoản và tự hết tác dụng sau 15 phút.

### 2.3. Cho Admin & Moderation (spec 100, E)

```java
/** com.wikicook.auth.api. REST tương đương ở contracts/openapi.yaml. */
public interface AccountAdminFacade {
    AccountAdminView changeRole(long actorId, long targetId, Role newRole);
    /** Moderator trở lên; mọi kỳ đình chỉ tối đa 30 ngày; Moderator chỉ đình chỉ Member, Contributor (FR-022, FR-022a). */
    AccountAdminView suspend(long actorId, long targetId, String reason, Instant endsAt);
    AccountAdminView ban(long actorId, long targetId, String reason);
    /** Gỡ sớm; hết hạn tự nhiên do SuspensionExpiryJob của 010. */
    AccountAdminView reinstate(long actorId, long targetId, String reason);
}

/** com.wikicook.auth.api. Một đường ghi nhật ký cho mọi module. */
public interface AuditWriter {
    /** details không được chứa email hay tên hiển thị (FR-025). */
    void record(Long actorId, String action, String targetType, Long targetId, Map<String, ?> details);
}
```

Thứ bậc, người tự thao tác, Administrator cuối cùng, xác thực lại và audit nằm bên trong facade; 100
không phải lặp lại. 100 đọc block list chỉ gián tiếp qua lúc đăng ký bị từ chối. Bảng `audit_logs`,
`user_suspensions` do `V102` tạo; E bỏ khỏi `V503` và thêm cột bằng migration riêng (research R11).

### 2.4. Cho nội dung (spec 060, 070)

| Spec | Dùng | Ghi chú |
|---|---|---|
| 060 | `@VerifiedEmailRequired` ở submit; lưu nháp chỉ cần đăng nhập | FR-006, 060 FR tương ứng |
| 060 | `Role.CONTRIBUTOR` để hiện badge | Dữ liệu hiển thị, không qua `RoleHierarchy` |
| 070 | `@VerifiedEmailRequired` ở review, rate, cooksnap, comment | FR-006 |
| 080 | Chỉ cần đăng nhập, không cần xác minh | 080 Edge Cases, FR-006 chỉ chặn đăng nội dung |
| 050 | Không cần | Chế độ nấu chạy ở client |

### 2.5. Thay đổi cho module đã có (030, 031, 040)

| Spec | Thay đổi | Research |
|---|---|---|
| 030, 031 | `CurrentUserId`, `AiPlannerController` giữ nguyên qua `ObjectProvider<CurrentUser>`; test dùng `HeaderCurrentUser` đã `@Primary` | R13 |
| 030 | `MealPlannerPage`, `AddToPlanButton` chỉ chuyển `/login` khi `useAuth().status === 'guest'`; người hết phiên dùng `SessionExpiredDialog` | R12 |
| 020 | `SearchViewerPort` có adapter thật `AuthSearchViewer` (id của người đăng nhập, rỗng với khách), thay `DevHeaderSearchViewer`; `RecipeSearchControllerIT` bỏ `X-Dev-User-Id` | R13 |
| 040 | `ShoppingListController` bỏ `X-User-Id`, dùng `CurrentUser` | R13 |
| 040 | Hàng đợi offline IndexedDB gắn `userId`, chỉ đồng bộ khi khớp người đăng nhập | Research bảng cuối, mục 19 |
| 040 | `POST /auth/refresh` như research 040 giả định; hết phiên không xóa IndexedDB, giữ nguyên | R1 |
| 040 | Đăng ký `addSignOutGuard` để cảnh báo mutation chưa đồng bộ trước khi đăng xuất | R12 |

## 3. Frontend – điểm nối với spec khác

| Điểm | Hướng | Mô tả |
|---|---|---|
| `shared/api/client.ts` | 010 → mọi feature | Gắn bearer (trừ `/auth/*` công khai), refresh một lần khi 401, rồi ném lại 401 gốc. Feature không xử lý token (R12) |
| `useAuth()` | 010 → mọi feature | `status` (`loading`, `guest`, `authenticated`, `offline`, `expired`), `account`, `signOut()`, `addSignOutGuard(fn)`. `account.role` chỉ để ẩn nút, server vẫn kiểm tra (FR-021). `offline` chỉ có khi app đã tải; app shell offline thuộc 040, 050 (research bảng cuối, mục 41) |
| `RequireAuth`, `RequireRole` | 010 → `routes.tsx` | Bọc `/meal-planner`; `/shopping-list*` dùng `RequireAuth offlineFirst`. Khi `expired`, không route nào bị chuyển trang: route `offlineFirst` để 040 hiện banner không chặn (040 FR-020), 050 dùng tương tự; route khác hiện `SessionExpiredDialog`. 100 bọc trang quản trị bằng `RequireRole minRole` |
| `/login?redirect=` | 030, 060, 070, 080 → 010 | Giữ đúng quy ước `encodeURIComponent(path + search)` của `AddToPlanButton`, `MealPlannerPage` |
| `?action=add-to-plan` | 010 → 030 | Sau đăng nhập quay lại `/recipes/{id}?action=add-to-plan`, 030 tự mở dialog (030 R10) |
| Save sau đăng nhập (`UC-080-01`) | 010 → 080 | Cùng cách, 080 đặt `action` riêng trong `redirect` |
| `SignInPrompt` (AU08) | 010 → 060, 070, 080 | Component chung cho hành động của khách, thay vì mỗi nơi tự viết |
| `SessionExpiredDialog` | 010 → mọi trang, 011, 060 | Gắn sẵn ở `AppLayout`, mở khi `expired` trên route không `offlineFirst`; feature **không** tự gắn thêm. Form dài (011, 060) dùng `useUnsavedChangesBlocker()` để cảnh báo khi rời trang (Edge Cases) |
| `PasswordField`, `TextField` | 010 → 011 | Đặt ở `shared/ui`, gồm checklist quy tắc FR-004 |
| `RoleChangeNotice` | 010 → `AppLayout` | `client.ts` thấy header `X-WikiCook-Role-Notice` thì tải lại `/auth/me`, trừ khi `account.roleNotice` đã có hoặc đang tải lại; đọc `account.roleNotice`, hiện một lần, gọi `/auth/role-notice/ack` |
| `/account-status` | 010 | Đăng nhập email truyền `code`, `reason`, `endsAt`, `closureToken` qua state của router; Google truyền qua fragment, trang đọc rồi xóa khỏi URL (FR-013) |
| `EmailVerificationBanner` | 010 → `AppLayout` | Nhắc xác minh, nút gửi lại (AF 2.7) |
| `/account-closure#token=…` | 010 → 011 | Màn AU07 dẫn tới trang đóng tài khoản của 011 khi có `closureToken` (research R16) |
| Trang chủ, tìm kiếm | 020 → 010 | `/` (`HomePage`) và `/search` đã công khai ở `main`, 010 không bọc `RequireAuth`; `RedirectIfAuthenticated` đưa người đã đăng nhập từ `/login`, `/register` về `/` |
| `cooking/:recipeId` | 050 → 010 | Nằm ngoài `AppLayout`, khách được dùng, chạy ở client; không có `SessionExpiredDialog` ở đó (050 NFR-004) |
| Route | 010 | `/login`, `/register`, `/check-email`, `/verify-email`, `/forgot-password`, `/reset-password`, `/account-status`, `/access-denied`, `/auth/callback` |
