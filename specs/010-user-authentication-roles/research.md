# Research: User Authentication & Roles – 010

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Ngày**: 2026-11-06

Mỗi mục ghi theo dạng **Decision / Rationale / Alternatives considered**. Không còn mục
NEEDS CLARIFICATION nào trong Technical Context sau phần này; những điểm phụ thuộc người khác hoặc
cần sửa spec được ghi ở mục "Điểm cần chốt với nhóm" cuối file. Quyết định của Hiển ngày 30/10/2026
trong `docs/requirements/use-case-specification.md` mục 9.3 (`OQ-010-xx`) được dùng làm đầu vào; câu
nào chưa vào spec thì ghi rõ ở bảng cuối.

---

## R1. Mô hình phiên và kiểm tra ở mỗi request

- **Decision**: Hai lớp token, và mọi request có bearer đều đối chiếu với DB.
  - **Access token**: JWT HS256, sống **15 phút**, claim `sub` (user id), `sid`
    (`user_sessions.id`), `iss`, `iat`, `exp`. **Không có claim vai trò**. Ký và xác minh bằng
    `NimbusJwtEncoder`/`NimbusJwtDecoder` (`spring-boot-starter-oauth2-resource-server`), khóa
    `WIKICOOK_JWT_SECRET` ≥ 32 byte.
  - **Refresh token**: chuỗi ngẫu nhiên 256 bit, chỉ lưu SHA-256 ở `user_sessions.token_hash`.
  - **Mỗi request**: sau khi chữ ký hợp lệ, `SessionJwtAuthenticationConverter` chạy **một query theo
    khóa chính** (`user_sessions` JOIN `users`, lấy `role`, `status`, `email_verified_at`,
    `revoked_at`, `expires_at`, `last_activity_at`) rồi quyết định theo thứ tự:

    | Bước | Điều kiện | Kết quả |
    |---|---|---|
    | 1 | Không có dòng (phiên đã bị dọn, tài khoản đã bị xóa) | 401 `UNAUTHENTICATED` |
    | 2 | `users.status` khác `ACTIVE` | 403 `ACCOUNT_SUSPENDED` / `ACCOUNT_BANNED` kèm lý do, ngày kết thúc (AF 2.14) |
    | 3 | `revoked_at` có giá trị hoặc `expires_at` đã qua | 401 `UNAUTHENTICATED` |
    | 4 | Hợp lệ | Authority lấy từ `users.role` hiện tại + `EMAIL_VERIFIED` nếu đã xác minh |

    Bước 2 đứng trước bước 3 vì đình chỉ thu hồi phiên ngay trong transaction (R10); nếu đảo thứ tự,
    người bị đình chỉ chỉ thấy "hết phiên" thay vì thông báo của FR-013.
  - Mọi 401 của API nghiệp vụ dùng đúng code `UNAUTHENTICATED`, vì `AddToPlanDialog` của 030 đang
    so sánh `codeOf(error) === 'UNAUTHENTICATED'` và `UnauthenticatedException` đã trả code này.

  | Yêu cầu | Cách đạt hiệu lực ở request kế tiếp |
  |---|---|
  | FR-010 đăng xuất | `revoked_at` của đúng phiên đó; bước 3 chặn ngay cả khi JWT còn hạn |
  | FR-016 đặt lại mật khẩu | Thu hồi mọi phiên của tài khoản trong cùng transaction |
  | FR-024 đổi vai trò | Authority đọc từ DB nên không cần thu hồi phiên |
  | FR-024 đình chỉ, cấm | Bước 2 |
  | Edge case xóa sau 7 ngày | Bước 1 (`ON DELETE CASCADE`) |

- **Rationale**: "Stateless" của `architecture.md` vẫn đúng ở tầng HTTP (không `HttpSession`, chạy
  được nhiều instance), nhưng FR-010, FR-016, FR-024 buộc trạng thái phiên phải có nguồn chung. Một
  query theo PK là chi phí nhỏ nhất đạt "next request"; JWT giữ lại để từ chối token giả mà không
  chạm DB và tương thích `bearerAuth` của openapi 030, 040 cùng giả định 15 phút +
  `POST /api/v1/auth/refresh` của research 040. Không đổi các giả định này.
- **Alternatives considered**: JWT thuần có claim vai trò – vai trò cũ còn hiệu lực tới 15 phút, vi
  phạm FR-024 (đây chính là mô tả ở Clarification của spec 100, xem bảng cuối); cache trong tiến
  trình 5–30 giây – vi phạm "next request" và không vô hiệu được giữa các instance; Redis blacklist
  – thêm một service cho việc PostgreSQL làm được (R5); `users.token_version` trong JWT – bắt được
  đổi vai trò nhưng không đăng xuất được một thiết bị; token opaque hoàn toàn – đơn giản hơn nhưng
  đổi hợp đồng `bearer JWT` mà 030, 040 và project plan đã ghi.

## R2. Lưu token ở trình duyệt, CSRF/XSS, xoay vòng và FR-009

- **Decision**:
  - **Access token** chỉ nằm trong bộ nhớ JS (module `shared/api/session.ts`, thay chỗ lưu `localStorage` đang có ở `main`), mất khi tải lại
    trang; lúc khởi động `AuthProvider` gọi `POST /auth/refresh` để lấy lại.
  - **Refresh token** nằm trong cookie `wc_rt`: `HttpOnly`, `Secure` (tắt được ở local bằng
    `WIKICOOK_AUTH_COOKIE_SECURE=false`), `SameSite=Strict`, `Path=/api/v1/auth`.
  - **CSRF**: cookie chỉ được đọc ở `/auth/refresh`, `/auth/logout`, các endpoint còn lại xác thực
    bằng header `Authorization` nên không bị CSRF. Hai endpoint đó yêu cầu thêm header
    `X-WikiCook-Client: web` (không có → 403 `INVALID_CLIENT`). `SameSite=Strict` + header tùy chỉnh
    + không bật CORS (frontend cùng origin qua nginx/Vite proxy) là đủ; tắt CSRF filter của Spring
    Security cho `/api/**`.
  - **XSS**: không lưu token trong `localStorage`; không `dangerouslySetInnerHTML` (constitution
    IV); nginx thêm CSP `default-src 'self'; img-src 'self' https: data:; style-src 'self'
    'unsafe-inline'; frame-ancestors 'none'; base-uri 'self'` và `Referrer-Policy: no-referrer`.
    Lộ XSS vẫn dùng được token trong phiên đang mở, nhưng không lấy được refresh token mang đi.
    nginx không kế thừa `add_header` từ cấp `server` vào `location` đã có `add_header` riêng (`/` và
    `/assets/` trong `src/frontend/nginx.conf` đều có), nên hai header này phải lặp ở từng `location`
    (hoặc qua một file `include`), và được kiểm bằng `curl -I`.
  - **Xoay vòng**: mỗi lần refresh sinh token mới, đưa hash cũ vào `previous_token_hash` +
    `rotated_at` trong transaction có `SELECT … FOR UPDATE` trên dòng phiên. Gửi lại hash cũ trong
    **10 giây** (hai tab, mạng chậm) → cấp access token mới, không đổi cookie. Sau 10 giây → coi là
    bị đánh cắp: thu hồi phiên, audit `REFRESH_TOKEN_REUSED`, trả 401. Trong một tab, frontend gộp
    refresh đồng thời (R12).

  | Trường hợp | `remember_me` | Cookie | `expires_at` |
  |---|---|---|---|
  | Có "Ghi nhớ (30 ngày)" | `true` | `Max-Age` = thời gian còn lại | `created_at + 30 ngày`, không gia hạn |
  | Không chọn | `false` | Session cookie (mất khi đóng trình duyệt) | Trượt: `last_activity_at + 24 giờ`, trần `created_at + 30 ngày` |

  - **Hoạt động** là mọi request có phiên hợp lệ; `last_activity_at`, `expires_at` chỉ cập nhật khi
    đã cũ hơn 5 phút (`UPDATE … WHERE last_activity_at < :threshold`) nên không ghi DB mỗi request.
    Frontend **không** tự refresh theo timer: tab để mở không được coi là đang hoạt động.
- **Rationale**: US1 AS5, AS6. Session cookie là cách duy nhất để "đóng trình duyệt là hết phiên"
  mà không cần script. Tính năng khôi phục phiên của trình duyệt có thể giữ session cookie; khi đó
  mốc 24 giờ phía server vẫn áp dụng. Thời hạn 30 ngày không trượt vì FR-009 ghi "up to 30 days".
- **Alternatives considered**: Lưu JWT trong `localStorage` – mất tài khoản khi lộ XSS và không đạt
  "đóng trình duyệt"; access token trong cookie – mọi endpoint phải chống CSRF;
  `CookieCsrfTokenRepository` – nhiều bước hơn cho đúng hai endpoint; không xoay vòng – token bị lộ
  dùng được tới 30 ngày; bắt lỗi reuse tuyệt đối (không có 10 giây) – hai tab cùng refresh sẽ tự
  đăng xuất nhau.

## R3. Giới hạn 72 byte của BCrypt

- **Decision**: `PrehashedBCryptPasswordEncoder implements PasswordEncoder`: chuẩn hóa Unicode
  **NFC** → UTF-8 → **SHA-256** → Base64 (44 ký tự ASCII) → `BCryptPasswordEncoder(12)`. Giá trị lưu
  ở `password_hash` vẫn là chuỗi BCrypt 60 ký tự. Không trim mật khẩu.
  `@ValidPassword` tự viết kiểm tra FR-004: độ dài 8–128 tính bằng **code point** sau NFC (không dùng
  `@Size`, vốn đếm UTF-16), có `\p{L}`, có `\p{Nd}`, khác email đã chuẩn hóa (không phân biệt hoa
  thường). Frontend dùng cùng quy tắc: `[...value.normalize('NFC')].length`.
- **Rationale**: Đã đọc mã nguồn `spring-security-crypto` **6.5.11** (phiên bản BOM Spring Boot
  3.5.16 quản lý, đã đối chiếu SHA-1 với Maven Central): `BCrypt.hashpw` có
  `if (!for_check && passwordb.length > 72) throw new IllegalArgumentException("password cannot be
  more than 72 bytes")`, còn `checkpw` đi qua nhánh `for_check = true`, không ném lỗi và chỉ dùng 72
  byte đầu. Vậy nếu dùng encoder mặc định, mật khẩu hợp lệ theo FR-004 có thể gây **500 lúc đăng
  ký**: "ế" chiếm 3 byte UTF-8, chỉ 25 ký tự như vậy đã là 75 byte; 128 ký tự tệ nhất là 512 byte.
  Pre-hash đưa mọi mật khẩu về 44 byte không chứa NUL, nên toàn bộ 128 ký tự đều có tác dụng. Hash
  lưu trữ vẫn là BCrypt, đúng constitution IV. NFC để cùng một mật khẩu tiếng Việt gõ từ bộ gõ dựng
  sẵn hay tổ hợp vẫn khớp nhau; "kept exactly as typed" của spec là về khoảng trắng đầu cuối và NFC
  không đổi nội dung nhìn thấy.
  Ở `LoginRequest` và `ReauthenticateRequest`, mức tối đa 128 cũng đếm bằng code point
  (`@MaxCodePoints(128)` tự viết, không dùng `@Size`). Nếu dùng `@Size`, mật khẩu 128 emoji
  (256 đơn vị UTF-16) đăng ký được nhưng không đăng nhập được.
- **Alternatives considered**: Giới hạn 72 byte – trái FR-004 (128 ký tự) và khó giải thích với
  người dùng tiếng Việt; bắt `IllegalArgumentException` rồi báo lỗi – cùng lý do; cắt 72 byte –
  hai mật khẩu khác nhau thành một; digest thô thay vì Base64 – có thể chứa byte NUL; HMAC có
  pepper – mất hoặc đổi khóa là mọi mật khẩu vô hiệu; Argon2 – constitution yêu cầu BCrypt.

## R4. Khóa tài khoản (FR-012) và chống dò email (NFR-003)

- **Decision**: Đếm theo **email đã chuẩn hóa, kể cả email chưa đăng ký** (quyết định `OQ-010-02`).
  - `login_attempts` là bộ đếm khóa: đổi `email_normalized` thành `email_hash` (R11), dòng chỉ cần
    giữ 1 ngày. `users.locked_until` được ghi kèm khi email có tài khoản, để 100 và 011 đọc nhanh;
    nguồn quyết định khi đăng nhập là `login_attempts`, nên email lạ và email thật đi cùng một đường.
  - Luồng `SignInService`, **không giữ transaction trong lúc chạy BCrypt**:

    | Bước | Việc | Ghi chú |
    |---|---|---|
    | 1 | Transaction ngắn có `pg_advisory_xact_lock(hashtextextended(email_hash, 0))`: kiểm tra IP (R5) và khóa | Đang khóa → 423 `ACCOUNT_LOCKED` + `retryAt`, không tính thêm lần thử, không chạy BCrypt |
    | 2 | Chèn trước dòng `login_attempts` với `success = false`, rồi commit | Gửi song song cũng không vượt 5 lần (SC-005) |
    | 3 | So khớp mật khẩu qua bulkhead (R14): có tài khoản dùng hash thật; không có tài khoản, hoặc `password_hash` NULL, dùng `dummyHash` sinh lúc khởi động cùng cost | Thời gian như nhau |
    | 4a | Sai: nếu đủ 5 lần liên tiếp trong 15 phút thì ghi `locked_until`, thêm outbox `ACCOUNT_LOCKED` (chỉ khi có tài khoản), audit `ACCOUNT_LOCKED`; trả 401 `INVALID_CREDENTIALS` | Lần sai thứ 5 cũng trả 423 |
    | 4b | Đúng: đổi dòng thành `success = true`, xóa `locked_until`, kiểm tra `status` (FR-013), tạo phiên, audit | Trạng thái chỉ lộ khi mật khẩu đúng |

  - **Điều kiện khóa**: 5 lần sai liên tiếp, lần đầu và lần cuối cách nhau không quá 15 phút; mở lại
    15 phút sau lần sai thứ 5. Lần thử bị từ chối vì đang khóa không gia hạn khóa.
  - Màn khóa giống nhau với mọi email, có giờ thử lại và câu trung tính "Nếu email này đã đăng ký,
    chúng tôi đã gửi thông báo" (AF 2.3).
  - Audit `SIGN_IN_FAILED` với email lạ có `target_id = NULL`, `details.emailHash`; không lưu email
    đọc được.
  - Khóa không chặn Google sign-in và đặt lại mật khẩu thành công **không** gỡ khóa (FR-012, chốt
    2026-11-06).
  - Đăng ký được phép báo email đã tồn tại (US2 AS2); NFR-003 chỉ áp dụng cho sign-in và reset.
- **Rationale**: Chỉ khóa email thật thì màn khóa tự nó xác nhận email tồn tại. Đặt dòng trước khi so
  mật khẩu làm cho advisory lock rất ngắn mà vẫn khép được cửa sổ tranh chấp. Không chạy BCrypt khi
  đang khóa giúp giảm tải khi bị tấn công; nhánh khóa giống nhau với email thật và email lạ nên không
  lộ thông tin. Ghi email và gửi mail đi qua outbox nên không kéo dài thời gian phản hồi.
- **Alternatives considered**: Chỉ dùng `users.locked_until` – không có chỗ cho email lạ; khóa cả
  transaction trong lúc chạy BCrypt – giữ kết nối pool khoảng 0,3 giây mỗi lần thử; đếm rồi cộng sau
  khi so mật khẩu – các lần gửi song song cùng vượt qua kiểm tra; `Thread.sleep` cân bằng thời gian
  – kém chính xác hơn so khớp thật với `dummyHash`.

## R5. Giới hạn FR-017 và câu hỏi Redis

- **Decision**: **Không thêm Redis cho 010**. Dùng bảng PostgreSQL với advisory lock theo khóa.
  - FR-017: bảng mới `email_send_requests (email_hash, purpose, created_at)`. Trước khi gửi, trong
    transaction có advisory lock, đếm dòng 15 phút gần nhất theo `(email_hash, purpose)`; đủ 3 →
    429 `EMAIL_RATE_LIMITED` + `retryAfterSeconds`.
  - Đếm theo địa chỉ, **kể cả email chưa đăng ký**, tách riêng từng `purpose` (đúng AF 1.10 và AF 3.2);
    lần gửi đầu lúc đăng ký cũng tính. Reset nói "thử lại sau X phút" theo `OQ-010-06` mà không lộ
    email có đăng ký hay không, vì email lạ cũng bị đếm y hệt.
  - Giới hạn theo IP của `OQ-010-02`: 100 lần thử đăng nhập mỗi IP trong 15 phút, đếm trên
    `login_attempts.ip_address` → 429 `TOO_MANY_REQUESTS`. Áp dụng cho mọi email để không lộ gì.
    Ngưỡng đặt ở `wikicook.auth.sign-in.ip-limit` (mặc định 100, cửa sổ 15 phút), vì mọi IT chạy
    từ 127.0.0.1 trên cùng một container và k6 chạy từ một máy: test để mức cao (10 000), riêng
    `SignInThrottleIT` đặt lại 100; k6 nâng ngưỡng bằng `WIKICOOK_AUTH_SIGN_IN_IP_LIMIT` (R14).
    `AuthTestSupport.resetThrottle()` xóa `login_attempts`, `email_send_requests`,
    `password_check_failures` trước mỗi test.
  - Đăng ký không bị từ chối vì giới hạn (FR-002, FR-017): quá 3 lần vẫn tạo tài khoản, không cấp token,
    không gửi mail. `RegisterResponse` có `verificationEmailSent = false`, `retryAfterSeconds` (số giây
    tới lúc gửi lại được) và `verificationExpiresAt = null` (chưa có link nào); màn AU03 hiện thời gian
    chờ từ `retryAfterSeconds`.
  - Dọn các dòng này sau 1 ngày (R9).
- **Rationale**: Compose chỉ có `db`, `app`, `migrate`, `web`; constitution chỉ yêu cầu PostgreSQL +
  backend + frontend. Spec 031 R11 cũng đã đếm trên `ai_requests` chứ không dùng Redis, nên hiện
  không còn tính năng nào cần Redis, dù `architecture.md` và project plan (mục 4.4.4) có ghi. Lưu
  lượng các endpoint này rất nhỏ; đồng bộ giữa nhiều instance có sẵn nhờ DB. Một service mới phải
  ghi Complexity Tracking mà không đem lại lợi ích đo được.
- **Alternatives considered**: Redis `INCR` + TTL – nhanh hơn nhưng không cần ở quy mô này, thêm
  container và phụ thuộc cho CI; đếm trên `one_time_tokens` như full-schema ghi – chỉ có email đã đăng
  ký nên hành vi 429 lộ tài khoản, và không tính được yêu cầu bị từ chối; Bucket4j trong bộ nhớ – sai
  khi có nhiều instance.

## R6. `one_time_tokens`

- **Decision**:
  - Sinh: 32 byte `SecureRandom`, Base64URL, gửi qua email; DB chỉ lưu SHA-256 (`token_hash`).
  - Link: `{publicBaseUrl}/verify-email#token=…`, `{publicBaseUrl}/reset-password#token=…`. Token nằm
    ở **fragment** nên không vào log của proxy hay header `Referer`; SPA đọc rồi `POST` lên API và
    xóa khỏi URL bằng `history.replaceState`. Bộ quét link của email chỉ `GET` trang nên không tiêu
    thụ token.
  - Tiêu thụ nguyên tử: `UPDATE one_time_tokens SET used_at = now() WHERE token_hash = ? AND used_at
    IS NULL AND expires_at > now() RETURNING user_id`, cùng transaction với việc cập nhật tài khoản.
  - Vô hiệu link cũ: khi cấp token mới, đặt `used_at` cho các token chưa dùng cùng `(user_id,
    purpose)`, trong cùng transaction. Áp dụng cho reset (FR-015) và cho xác minh (quyết định
    `OQ-010-03`).
  - Phân biệt lỗi: quá hạn → 410 `TOKEN_EXPIRED`; đã dùng, bị thay thế, không tồn tại → 410
    `TOKEN_INVALID`. Link xác minh đã dùng của tài khoản đã xác minh → 200 `ALREADY_VERIFIED` (AF 1.12).
  - Đặt lại mật khẩu thành công: đổi hash, tiêu thụ token, thu hồi mọi phiên, audit
    `PASSWORD_RESET`, trong một transaction (FR-016). Mật khẩu vi phạm FR-004 không tiêu thụ token
    (AF 3.5). Reset **không** đánh dấu đã xác minh email, tránh hợp thức hóa dữ liệu do người khác
    tạo trước (đúng tinh thần FR-019).
  - `ResetPasswordRequest` không có email nên `@ValidPassword` chỉ kiểm tra độ dài, chữ, số. So với email
    làm ở `PasswordResetService` sau khi đọc tài khoản từ token và trước khi tiêu thụ token; trùng → 400
    `VALIDATION_ERROR` với `errors[].code = PASSWORD_EQUALS_EMAIL` (FR-004).
  - Xác minh không tự đăng nhập; đăng ký cũng không (Basic Flow bước 9, 11).
  - Tài khoản chỉ Google, bị đình chỉ, bị cấm: phản hồi vẫn trung tính nhưng không cấp link (FR-014,
    chốt `OQ-010-10`).
- **Rationale**: `used_at` đủ biểu diễn "không còn dùng được", không cần cột mới. UPDATE có điều kiện
  bảo đảm hai request cùng link chỉ một cái thắng. Hash SHA-256 đủ vì token đã có 256 bit ngẫu nhiên.
- **Alternatives considered**: JWT làm link – không vô hiệu từng link được; token ở query string –
  lộ qua log và `Referer`; BCrypt cho token – chậm mà không thêm an toàn; cột `invalidated_at`
  riêng – chưa có màn nào cần phân biệt "đã dùng" và "bị thay thế".

## R7. Google sign-in

- **Decision**: Authorization Code + PKCE phía server bằng `spring-boot-starter-oauth2-client`
  (`oauth2Login`), registration `google` của `CommonOAuth2Provider`.
  - Luồng: nút trên trang là link điều hướng tới `GET /api/v1/auth/oauth2/google?redirect=…`
    → Google → `GET /api/v1/auth/oauth2/callback/google` → backend đặt cookie `wc_rt`, chuyển về
    `/auth/callback?redirect=…`; SPA gọi `/auth/refresh` như lúc tải lại trang.
  - Không dùng `HttpSession`: `state`, `nonce`, PKCE verifier và `redirect` nằm trong
    `CookieOAuth2AuthorizationRequestRepository` (cookie `wc_oauth`, HMAC, HttpOnly, `SameSite=Lax`,
    5 phút; `Lax` vì Google quay lại bằng điều hướng cấp cao nhất).
  - URL công khai cấu hình bằng `WIKICOOK_PUBLIC_BASE_URL`, không suy từ header: nginx hiện đặt
    `Host $host`, làm mất cổng 3000.
  - **Không dùng `issuer-uri`** vì Spring sẽ gọi Google lúc khởi động. Timeout 5 giây cho token
    endpoint và JWKS.
  - **NFR-005**: đường email/mật khẩu không gọi Google. Google lỗi hoặc timeout → failure handler
    chuyển về `/login?error=google_unavailable`. Thiếu `WIKICOOK_GOOGLE_CLIENT_ID` (local, CI) →
    không bật `oauth2Login`, endpoint mở đầu trả về cùng địa chỉ đó; ứng dụng vẫn khởi động.

  | Trạng thái lúc callback | Xử lý | Nguồn |
  |---|---|---|
  | Không có email hoặc `email_verified = false` | `/login?error=google_no_verified_email` | FR-018 |
  | Người dùng hủy | `/login?error=google_cancelled` | US5 AS3 |
  | Tài khoản bị đình chỉ, cấm, **kể cả chưa xác minh** | `/account-status#code=…&reason=…&endsAt=…`, không tiếp quản, không đổi gì; bị cấm thì thêm `closureToken` vào cùng fragment. Nếu kỳ đình chỉ đã hết thì gọi `finishExpired` (R9) rồi đi tiếp như tài khoản đang hoạt động | AF 4.6, FR-013, FR-019 |
  | Khớp `google_sub` | Đăng nhập | US5 |
  | Khớp email đã xác minh | Ghi `google_sub`, đăng nhập, giữ dữ liệu và vai trò | FR-019 |
  | Khớp email **chưa xác minh** | Tiếp quản (dưới đây) | FR-019 |
  | Không khớp, email trong Blocked Email | `/login?error=registration_refused` | FR-026 |
  | Không khớp | Tạo Member đã xác minh | US5 AS1 |

  Bảng xét từ trên xuống. Dòng đình chỉ, cấm phải đứng trước tiếp quản: tiếp quản xóa `users` nên
  `user_suspensions` bị `CASCADE`, tức người bị xử lý thoát án bằng Google, đúng đường FR-005 đã chặn.
  Lý do và ngày kết thúc đi qua fragment vì FR-013 bắt buộc hiển thị và người dùng đã chứng minh danh
  tính bằng Google; fragment không vào log proxy hay `Referer`, trang AU07 đọc rồi xóa bằng
  `history.replaceState` như R6. Đăng nhập email truyền cùng dữ liệu qua state của router.

  - **Tiếp quản tài khoản chưa xác minh**, một transaction, được hoặc mất cả (quyết định `OQ-010-07`):
    (1) `publishEvent(AccountResetEvent)` đồng bộ để 011, 060, 080 xóa dữ liệu của mình và file đi
    kèm; (2) `DELETE FROM users` – mọi bảng `CASCADE` trong full-schema mục 6 (phiên, token,
    `meal_plans`, `shopping_lists`, `collections`, dị ứng…) tự sạch; (3) `INSERT` tài khoản mới cùng
    email, `google_sub`, tên từ Google, `email_verified_at = now()`, không có `password_hash`;
    (4) audit `ACCOUNT_RESET` ghi id cũ và id mới. Listener nào ném lỗi → rollback, chuyển về
    `/login?error=google_unavailable`, không có gì đổi.
  - Tên Google: trim, cắt 50 code point; rỗng thì dùng phần trước `@` (AF 4.8).
  - Phiên Google: `remember_me = false` (quyết định `OQ-010-08`).
  - Xác thực lại (R10, 011 FR-029, FR-031): `GET /auth/oauth2/google?intent=reauth`.
    **Chưa kiểm chứng với Google thật** (bảng cuối, mục 40). Tài liệu OpenID Connect của Google (đọc
    ngày 2026-11-07) không có `max_age` và không có giá trị `prompt=login`; `auth_time` chỉ có khi xin
    bằng tham số `claims={"id_token":{"auth_time":{"essential":true}}}` **và** bật trong cài đặt của
    client; discovery document cũng không liệt kê `auth_time` trong `claims_supported`. Thiết kế đề
    xuất: gửi `prompt=login` cùng tham số `claims` đó; ghi thời điểm bắt đầu vào `wc_oauth`; callback
    **bắt buộc** có `auth_time` không sớm hơn thời điểm đó. Thiếu `auth_time` thì xử lý như hủy
    (`?reauth=cancelled`), không bao giờ chấp nhận khi không chứng minh được người dùng vừa đăng nhập
    lại. WireMock không chứng minh được điểm này, nên phải thử với client thật (T149b) trước T149a.

    | Bước | Việc | Lý do |
    |---|---|---|
    | Bắt đầu | Đọc `wc_rt` (điều hướng từ SPA nên có cookie), lưu `sid` vào `wc_oauth`; không có phiên hợp lệ → `/login` | `wc_rt` là `SameSite=Strict`, không được gửi ở callback đi từ Google |
    | Callback | Lấy `sid` và thời điểm bắt đầu từ `wc_oauth` (có HMAC), phiên còn hiệu lực, `google_sub` trùng tài khoản của phiên, `auth_time` có mặt và không sớm hơn thời điểm bắt đầu | Không để tài khoản Google khác xác thực thay; không chấp nhận phiên Google cũ |
    | Thành công | Đặt `reauthenticated_at` của đúng phiên đó, chuyển về `redirect`; không tạo phiên, không đổi `wc_rt` | Không thuộc đường sign-in |
    | Hủy, lỗi, sai tài khoản | Chuyển về `redirect` với `?reauth=cancelled`, không về `/login` | Người dùng vẫn đăng nhập; `RedirectIfAuthenticated` sẽ đưa họ khỏi `/login` (FR-022b) |

- **Rationale**: UC-010-04 mô tả chuyển hướng tới màn đồng ý của Google và client secret; Authorization
  Code cũng là cách duy nhất ép đăng nhập lại cho 011. Dùng lớp sẵn của Spring cho `state`, `nonce`,
  đổi code và kiểm tra ID token, đúng constitution IV. Xóa rồi tạo lại an toàn hơn sửa tại chỗ:
  FR-019 yêu cầu sạch "any other data", trong khi 030, 031, 040 đang có bảng theo `user_id` nhưng
  không nằm trong danh sách listener; `CASCADE` phủ cả module tương lai. Vì `recipes.author_id` là
  `SET NULL`, listener của 060 là **bắt buộc**, nếu không bản nháp sẽ mồ côi (bảng cuối).
- **Alternatives considered**: Google Identity Services (nút JS trả ID token) – không có client
  secret như UC mô tả, khó ép xác thực lại, phụ thuộc script ngoài và CSP; tự viết OAuth bằng
  `RestClient` – tự chịu `state`/`nonce`/kiểm tra token; sửa tài khoản tại chỗ giữ `id` – phải chờ
  mọi module cài listener và dễ bỏ sót dữ liệu; lưu `state` trong `HttpSession` – cần sticky session
  và lệch mô hình không `HttpSession` của R1.

## R8. Gửi email

- **Decision**: `spring-boot-starter-mail` (`JavaMailSender`). Local dùng **Mailpit** (image
  `axllent/mailpit`, SMTP 1025, giao diện 8025), thêm một service trong `src/backend/docker-compose.yml`.
  Hai kênh, vì token thô không được lưu DB (R6):

  | Email | FR | Kênh | Khi lỗi |
  |---|---|---|---|
  | `VERIFY_EMAIL` | FR-005 | Link: sau commit, `@Async` trên virtual thread, thử 3 lần | Người dùng bấm gửi lại (Edge Cases) |
  | `PASSWORD_RESET` | FR-015 | Như trên | Như trên |
  | `ACCOUNT_LOCKED` | FR-012 | Outbox | Thử lại với backoff |
  | `ACCOUNT_SUSPENDED`, `ACCOUNT_BANNED` | FR-024a | Outbox | Thử lại với backoff |
  | `SUSPENSION_ENDED`, `BAN_LIFTED` | FR-024a | Outbox | Thử lại với backoff |

  - **Outbox** `email_outbox`: ghi cùng transaction với hành động, chỉ lưu `user_id`, `template`,
    `payload` (lý do, ngày kết thúc), không có token và không chép email. Dispatcher
    15 giây một lần, lấy lô bằng `FOR UPDATE SKIP LOCKED`, tối đa 6 lần (1, 5, 15 phút, 1, 6 giờ), rồi
    `failed_at` + log `ERROR`.
  - **Khiếu nại** (FR-024a, chốt 2026-11-07): spec 100 không có quy trình khiếu nại và không thêm. Email
    đình chỉ, cấm ghi địa chỉ từ `wikicook.auth.appeal-contact` (`WIKICOOK_APPEAL_CONTACT`, mặc định là
    `WIKICOOK_MAIL_FROM`), đọc lúc gửi nên không lưu trong `payload`. Administrator xem xét thủ công
    rồi gỡ án bằng `reinstatement`. Bỏ `appealUrl` khỏi API và route `/support/appeal`. Compose truyền
    biến trống thành chuỗi rỗng, mà placeholder mặc định của Spring chỉ áp dụng khi biến không tồn
    tại, nên `AuthProperties` tự lùi về `mail-from` khi `appeal-contact` rỗng hoặc chỉ có khoảng
    trắng, và Compose dùng `${WIKICOOK_APPEAL_CONTACT:-${WIKICOOK_MAIL_FROM:-…}}`.
  - **Health**: có `spring-boot-starter-mail` và actuator thì health indicator của mail tự bật; SMTP
    không tới được sẽ làm `/actuator/health` DOWN, Compose coi `app` là unhealthy và `web` không khởi
    động. Tắt bằng `management.health.mail.enabled: false`: email là kênh có thử lại, không phải điều
    kiện để app phục vụ request.
  - **Thời gian phản hồi**: `POST /auth/password/forgot` đẩy toàn bộ việc tra tài khoản và gửi mail
    sang tác vụ `@Async`, trả 202 ngay, nên email có hay không không đổi thời gian (NFR-003). Giới
    hạn FR-017 vẫn kiểm tra đồng bộ trên `email_hash`.
  - **Mẫu**: mỗi email một cặp `resources/mail/<template>.html` + `.txt` tiếng Việt, tiêu đề ở
    `MailRenderer`. Thay biến `${name}` bằng hàm nhỏ, escape HTML cho biến. Không email nào chứa mật
    khẩu.
- **Rationale**: Link hỏng có đường tự sửa theo Edge Cases, nên chấp nhận mất; thông báo đình chỉ
  không có đường đó nên phải bền. `SKIP LOCKED` cho phép nhiều instance. Bảy mẫu đơn giản chưa đáng một
  template engine.
- **Alternatives considered**: Một outbox cho tất cả – phải lưu token thô, trái R6; gửi đồng bộ trong
  request – chậm, lộ thời gian và lỗi SMTP thành 500; Thymeleaf – thêm phụ thuộc cho 7 mẫu;
  MailHog – đã ngừng cập nhật; dịch vụ thật như SendGrid ở local – cần tài khoản, tốn hạn mức.
  Nhà cung cấp SMTP thật gắn với `TODO(HOSTING)` (bảng cuối).

## R9. Tác vụ định kỳ

- **Decision**: `@Scheduled` trong `com.wikicook.auth.job`, bật/tắt bằng
  `wikicook.auth.jobs.enabled` (tắt trong test, gọi trực tiếp). Lịch theo `Asia/Ho_Chi_Minh`, giống
  `EmbeddingReconcileJob`. `AuthJobsConfig` tự khai báo `@EnableScheduling` để không phụ thuộc
  `wikicook-ai`, nhưng `AiAsyncConfig` đã bật lịch cho cả ứng dụng, nên cờ đặt trên lớp cấu hình
  **không** tắt được `@Scheduled`. Vì vậy mỗi job luôn là bean có `run()` công khai (test gọi trực
  tiếp), còn phương thức `@Scheduled` chỉ gọi `run()` khi `wikicook.auth.jobs.enabled = true`. Không
  đặt `@ConditionalOnProperty` lên bean job, vì test sẽ không inject được. Mỗi lô 100 dòng, một
  transaction, mở đầu bằng `pg_try_advisory_xact_lock(<khóa job>)`: instance không lấy được khóa thì
  bỏ qua lượt này.

  | Job | Lịch | Việc | Nguồn |
  |---|---|---|---|
  | `UnverifiedAccountPurgeJob` | Mỗi giờ | `email_verified_at IS NULL`, `created_at` quá 7 ngày, `NOT is_system`, `status <> 'SUSPENDED'` (hết đình chỉ thì lượt chạy kế tiếp xóa): phát `AccountPurgedEvent`, `DELETE users`, audit `ACCOUNT_PURGED_UNVERIFIED`. Không thêm vào Blocked Email, trừ khi `status = 'BANNED'`: thêm hash trong cùng transaction | FR-005, FR-026 |
  | `SuspensionExpiryJob` | 5 phút | `kind = 'SUSPENSION'`, `ends_at` đã qua, `lifted_at IS NULL`: đặt `lifted_at = ends_at`, `lift_reason = 'EXPIRED'`, `users.status = 'ACTIVE'`, outbox `SUSPENSION_ENDED`, audit | FR-024a |
  | `AuthRetentionJob` | 03:30 hằng ngày | `login_attempts`, `email_send_requests`, `password_check_failures` > 1 ngày; `one_time_tokens` đã dùng hoặc hết hạn > 7 ngày; `user_sessions` hết hạn hoặc thu hồi > 90 ngày; `email_outbox` xong > 30 ngày | Full-schema 4.1, FR-025 |
  | `OutboxDispatcher` | 15 giây | R8 | FR-012, FR-024a |

  - **Thao tác quản trị trong khoảng trống 5 phút**: job chỉ chạy 5 phút một lần, nên `AccountAdminService` cũng gọi
    `finishExpired(userId)` (giao dịch riêng `REQUIRES_NEW`, commit ngay) **trước khi** đọc `users.status`, cho mọi thao tác (đổi vai trò,
    đình chỉ, cấm, gỡ). Nếu không: đình chỉ lại một án vừa hết nhận 409 sai, cấm ghi nhầm án tự hết thành "gỡ sớm"
    và mất email `SUSPENSION_ENDED`, gỡ ghi `ACCOUNT_REINSTATED` cho án đã hết (góp ý của Duy). Phải là giao dịch riêng: gỡ án tài khoản vừa hết hạn trả 409 `ACCOUNT_NOT_SANCTIONED`, và nếu `finishExpired` nằm chung giao dịch với thao tác thì ngoại lệ rollback cả `status = ACTIVE` lẫn outbox `SUSPENSION_ENDED`.
  - **Đăng nhập ngay sau `ends_at`**: `SignInService` và `GoogleSignInService` không chờ job: nếu kỳ đình chỉ đã hết thì gọi cùng
    hàm `finishExpired(userId)` của job, hàm này idempotent nhờ `UPDATE … WHERE lifted_at IS NULL`,
    nên email chỉ gửi một lần.
  - **`audit_logs` không bị job xóa**: NFR-008 ghi "ít nhất 90 ngày", còn spec 100 yêu cầu append-only
    (bảng cuối).
- **Rationale**: Advisory lock có sẵn trong PostgreSQL, tự nhả theo transaction; 031 đã dùng cách
  này. Không có tác vụ nào cần chính xác tới giây, trừ kết thúc đình chỉ, đã xử lý ở đường đăng nhập.
- **Alternatives considered**: ShedLock – thêm thư viện và bảng cho việc advisory lock làm được;
  Quartz – quá lớn; `pg_cron` – logic email nằm ở ứng dụng, không ở DB; tính "hết đình chỉ" lúc đọc,
  không có job – không ai kích hoạt email FR-024a nếu người dùng không quay lại.

## R10. Phân quyền

- **Decision**: Hai lớp, mặc định từ chối.
  - **Lớp URL** (`SecurityFilterChain`, stateless): chỉ các route khai báo công khai mới mở
    (`/api/v1/auth/**` trừ `me`, `role-notice`, `reauthenticate`; `GET` `GET /api/v1/recipes/search`, `/api/v1/recipes/search/options`, `/api/v1/recipes/featured` của 020 (đã có ở `main`: khách tìm kiếm, 020 AS 3.4), `GET` đọc công thức khi 060 có;
    `/actuator/health`, tài liệu OpenAPI). Phần còn lại của `/api/**` cần xác thực; mọi thứ khác
    `denyAll`. `BearerTokenAuthenticationFilter` trả 401 với bearer hỏng hoặc hết hạn **kể cả ở route
    `permitAll`**, nên `SecurityConfig` dùng một `BearerTokenResolver` riêng bỏ qua header
    `Authorization` ở route công khai. Nhờ vậy JWT hết hạn gửi kèm `/auth/refresh`, `/auth/login`
    không làm hỏng việc lấy token mới; client cũng không gắn bearer ở các route đó (R12).
  - **Lớp method**: `@EnableMethodSecurity` + `RoleHierarchy` `ADMINISTRATOR > MODERATOR > CONTRIBUTOR
    > MEMBER`. Meta-annotation dùng chung ở `com.wikicook.common.security`: `@ModeratorOnly`,
    `@AdministratorOnly`, `@VerifiedEmailRequired`. Ràng buộc theo đối tượng nằm ở Service, không
    nằm ở annotation.

  | Dòng ma trận | Kiểm tra server | Bên áp dụng |
  |---|---|---|
  | Đọc công thức đã xuất bản | Route công khai: tìm kiếm của 020 đã khai báo ở `PublicRoutes`; chi tiết công thức do 060 thêm | 020, 060 |
  | Bookmark, meal plan, shopping list | Xác thực (URL) | Đã có 030, 040; 080 |
  | Review, rate, cooksnap | `@VerifiedEmailRequired` | 070 |
  | Submit recipe | `@VerifiedEmailRequired` (FR-006); lưu nháp chỉ cần xác thực (060 FR) | 060 |
  | Badge Contributor | Dữ liệu hiển thị, không phải quyền: `role = CONTRIBUTOR` | 060 |
  | Moderate | `@ModeratorOnly` | 100 |
  | Danh mục | `@AdministratorOnly` | 100 |
  | Đình chỉ | `@ModeratorOnly` + `AccountActionPolicy` | 010 |
  | Đổi vai trò, cấm, gỡ án sớm | `@AdministratorOnly` + `AccountActionPolicy` | 010 |

  - **`AccountActionPolicy`** (FR-022, FR-022a, FR-023), chạy trong `AccountAdminService`:
    không tự thao tác lên mình (403 `SELF_ACTION_FORBIDDEN`); chỉ vai trò cao hơn tác động vai trò
    thấp hơn, trừ Administrator → Administrator; thao tác lên Administrator khác hoặc thăng cấp lên
    Administrator cần xác thực lại trong 5 phút (403 `REAUTH_REQUIRED`); luôn audit. Tài khoản
    `is_system` không bị tác động. Mọi kỳ đình chỉ có `endsAt` tối đa 30 ngày kể từ lúc đình chỉ, kể cả với Administrator,
    quá hạn → 403 `SUSPENSION_LIMIT_EXCEEDED`. Moderator chỉ đình chỉ Member, Contributor; người khác → 403
    `ACCESS_DENIED`. Tài khoản không ở `ACTIVE` không đình chỉ được (409 `ACCOUNT_NOT_ACTIVE`), kể cả
    với Administrator, để một án ngắn không ghi đè lệnh cấm. Cấm tài khoản đang `BANNED` cũng 409
    `ACCOUNT_NOT_ACTIVE`; cấm tài khoản đang `SUSPENDED` thì đóng án cũ trước khi chèn án mới
    (`lift_reason = 'REPLACED_BY_BAN'`). Gỡ án tài khoản đang `ACTIVE` (sau `finishExpired`) trả 409
    `ACCOUNT_NOT_SANCTIONED`, để màn "Gỡ án" của 100 biết là không có gì để gỡ thay vì tưởng đã gỡ và gửi email.
  - **Administrator cuối cùng**: trong transaction, `SELECT id FROM users WHERE role =
    'ADMINISTRATOR' AND status = 'ACTIVE' ORDER BY id FOR UPDATE` rồi đếm; hành động làm số còn lại về 0 → 409
    `LAST_ADMINISTRATOR`. Khóa dòng để hai Administrator hạ cấp nhau đồng thời vẫn còn một người; `ORDER BY id` để các
    transaction khóa cùng thứ tự, tránh deadlock khi hai Administrator tự xóa. 011 dùng cùng hàm qua
    `AccountGuard`, và `assertSelfDeletionAllowed` từ chối đình chỉ bằng 403 `ACCOUNT_SUSPENDED` kèm
    `reason`, `endsAt`, Administrator cuối cùng bằng 409 `LAST_ADMINISTRATOR`.
  - **Đình chỉ, cấm** (`AccountAdminService`): trong một transaction đổi `users.status`, chèn
    `user_suspensions`, thu hồi mọi phiên, thêm outbox, audit.
  - **Đổi vai trò**: đổi `users.role`, audit; đặt `pending_role_notice_from` (R11). Không gửi email.
  - **Xác thực lại**: `POST /auth/reauthenticate` (hoặc `intent=reauth` với Google) đặt
    `user_sessions.reauthenticated_at`; `ReauthService.requireRecent(sessionId, 5 phút)`. Mật khẩu sai đi qua bộ đếm của R17.
  - **Ranh giới REST với 100**: 010 cung cấp `/api/v1/admin/users/{id}/…` và `GET
    /api/v1/admin/audit-logs` không có UI, để US3 AS4, NFR-008 có bằng chứng trước khi 100 làm. Màn
    AD07, AD10 của 100 gọi lại các endpoint này hoặc `AccountAdminFacade`.
  - **Administrator đầu tiên** (Assumptions): `AdminBootstrapRunner` đọc
    `WIKICOOK_BOOTSTRAP_ADMIN_EMAIL`, `…_PASSWORD`; chỉ tạo khi chưa có Administrator đang hoạt động.
  - **Vai trò theo spec** (chốt 2026-11-06, bảng cuối, mục 1): Moderator được đình chỉ theo FR-022,
    FR-022a; gỡ án sớm chỉ Administrator làm, vì 100 không giao việc này cho Moderator.
  - **Test đủ ma trận**: `PermissionMatrixIT` tham số hóa 9 dòng × 5 tác nhân (Guest, Member đã xác
    minh, Contributor, Moderator, Administrator) = 45 ca, thêm Member chưa xác minh cho FR-006. Dòng
    của module chưa có dùng `MatrixProbeController` chỉ tồn tại trong test, đổi sang endpoint thật khi
    module đó vào. `RouteProtectionIT` duyệt `RequestMappingHandlerMapping`: mọi route `/api/v1/**`
    không có trong danh sách công khai phải trả 401 khi không có token. Test FR-024: hạ vai trò rồi
    gửi lại cùng JWT → 403.
- **Rationale**: FR-021 yêu cầu chặn ở server trên mọi request; project plan ghi "method-level rules".
  Mặc định từ chối để endpoint mới của module khác không vô tình công khai. `RouteProtectionIT` biến
  quy tắc đó thành gate của CI, phục vụ gate "RBAC matrix 100% pass" của `v0.8` và SC-004.
- **Alternatives considered**: Chỉ kiểm tra ở URL – không biểu diễn được email đã xác minh và theo đối
  tượng; kiểm tra thủ công trong từng Service – dễ sót, khó rà; bảng quyền trong DB – ma trận cố định,
  không có màn quản trị quyền; dùng `RoleHierarchy` cho badge – Moderator sẽ có badge, sai ma trận.

## R11. Dữ liệu còn thiếu và ranh giới với E

- **Decision**: Thêm ba migration của A, số 1xx vì chỉ phụ thuộc `users`:

  | File | Nội dung | Yêu cầu |
  |---|---|---|
  | `V102__audit_and_suspensions.sql` | `audit_logs` (không FK ở `actor_id`, có trigger cấm UPDATE, DELETE), `user_suspensions` đúng cột full-schema 4.10 (chuyển sớm từ `V503` của E); thêm `lift_reason`, `UNIQUE (user_id) WHERE lifted_at IS NULL` | FR-013, FR-024, FR-025 |
  | `V103__auth_schema_updates.sql` | `blocked_emails`, `password_check_failures`; `users.pending_role_notice_from`; `login_attempts.email_hash` thay `email_normalized`; cột xoay vòng và xác thực lại của `user_sessions`; `one_time_tokens.purpose` thêm `ACCOUNT_CLOSURE` | FR-012, FR-024a, FR-026, R2, R16 |
  | `V104__auth_mail.sql` | `email_send_requests`, `email_outbox` | FR-012, FR-017, FR-024a |

  - **`audit_logs` + `user_suspensions`**: 010 không chạy được nếu thiếu (FR-013, FR-025), còn `V5xx`
    chỉ đến ở `v1.0`. E bỏ hai bảng khỏi `V503`, thêm cột bằng migration riêng nếu cần; A không đụng
    `reports`, `moderation_actions`. Cột giữ nguyên `ends_at`, `created_by` của full-schema.
  - **Blocked Email**: `blocked_emails (email_hash CHAR(64) PK, created_at)`; hash là
    HMAC-SHA256 của email chuẩn hóa với `WIKICOOK_EMAIL_HASH_KEY`. Dùng cùng `EmailHasher` với
    `login_attempts`, `email_send_requests`. Không đổi khóa sau khi có dữ liệu, vì sẽ làm mất hiệu lực
    danh sách.
  - **Thông báo đổi vai trò (FR-024a)**: một cột `users.pending_role_notice_from VARCHAR(15)` – vai trò
    trước khi đổi, `NULL` = không có thông báo. Vai trò mới là `users.role`; quyền thay đổi do frontend
    tính từ ma trận. Đổi nhiều lần trước khi xem: giữ vai trò ban đầu; quay lại đúng vai trò cũ thì
    đặt `NULL`. `GET /auth/me` trả notice, `POST /auth/role-notice/ack` xóa. Đúng quy ước "thông tin
    1–1 lưu thành cột" của full-schema mục 1.
  - **Báo ở request kế tiếp** (FR-024a, chốt 2026-11-07): `/auth/me` chỉ được gọi lúc khởi động, nên người
    đang mở app sẽ không thấy notice nếu chỉ có đường này. `SessionSnapshot` thêm
    `pendingRoleNoticeFrom`; `AccountStateHeaderFilter` (sau `BearerTokenAuthenticationFilter`) thêm
    `X-WikiCook-Role-Notice: 1` vào mọi phản hồi có bearer khi cột khác NULL, gồm cả 403 sau khi bị hạ
    vai trò. Client thấy header thì tải lại `/auth/me` một lần, nên `account.role` cũng được cập nhật.
    Không tốn thêm query vì cột nằm trong query kiểm tra phiên. Để không lặp: filter **không** gắn
    header vào phản hồi của `GET /auth/me` (thông báo đã nằm trong body) và `POST
    /auth/role-notice/ack` (snapshot đọc trước khi xóa); client bỏ qua header khi
    `account.roleNotice` đã có hoặc đang có một lần tải lại chưa xong.
  - **Map kiểu với `ddl-auto: validate`**: đã đọc `ColumnDefinitions.hasMatchingType` và
    `Dialect.equivalentTypes` của Hibernate **6.6.53** (phiên bản BOM quản lý): `String` mặc định là
    VARCHAR, `isVarcharType` không gồm `CHAR`, nên cột `CHAR(64)` làm ứng dụng không khởi động nếu không
    khai báo. Dùng `@JdbcTypeCode(SqlTypes.CHAR)` cho `CHAR(64)`, `@JdbcTypeCode(SqlTypes.JSON)` cho
    `JSONB`, `@JdbcTypeCode(SqlTypes.INET)` với `InetAddress` cho `INET`. Repo chưa có tiền lệ, nên phải
    có test đọc ghi thật (`EntityMappingIT`).
  - **FR-025 và `login_attempts`**: đây là mâu thuẫn thật: `ON DELETE CASCADE` và cột email rõ không
    thể vừa giữ 90 ngày vừa ẩn danh. Tách vai trò: **`audit_logs` là nhật ký** của FR-025, NFR-008;
    **`login_attempts` chỉ là bộ đếm khóa**, giữ 1 ngày, `CASCADE` là đúng (xóa tài khoản thì email
    tự do ngay, FR-002).
  - **Ẩn danh**: quy tắc viết chứ không phải bước dọn: `details` của `audit_logs` **không bao giờ** chứa
    email hay tên hiển thị. `actor_id` và `target_id` đều **không có khóa ngoại**, nên khi tài khoản bị xóa
    chúng chỉ còn là con số không phân giải được và không có `UPDATE` nào chạy, phù hợp append-only của 100.
    Một khóa ngoại `ON DELETE SET NULL` ở `actor_id` sẽ chạy `UPDATE` lúc xóa người dùng (011, purge sau 7 ngày,
    Google tiếp quản) và bị trigger của 100 từ chối; bỏ khóa ngoại thay vì nới trigger (góp ý của Duy, 2026-11-12).
    `V102` tạo luôn trigger cấm UPDATE, DELETE (NFR-004 của 100).
  - **Flyway**: DB đã chạy tới `V403`, nên `V102`–`V104` là migration thấp hơn mức hiện tại. Mặc định
    Flyway sẽ báo lỗi validate ("resolved migration not applied"). Cần bật
    `spring.flyway.out-of-order: true` và `FLYWAY_OUT_OF_ORDER: "true"` cho service `migrate`.
    `login_attempts` chưa được code nào ghi (đã grep), nên đổi cột không mất dữ liệu.
- **Rationale**: Giữ dải số của từng người (full-schema mục 7). Mọi FK của ba file chỉ tới `users`.
- **Alternatives considered**: 010 chờ `V503` – chặn Sprint 4, vì `V5xx` thuộc `v1.0`; bảng
  `role_change_notices` riêng – chỉ một thông báo chờ mỗi người, cột là đủ; đổi FK `login_attempts`
  sang `SET NULL` và giữ 90 ngày – vẫn có email rõ, trùng việc với `audit_logs`; đặt tên `V505+` để
  khỏi cần out-of-order – phá dải số và chạy sau khi E tạo bảng, không dùng được ở Sprint 4.

## R12. Frontend

- **Decision**:
  - **Trạng thái**: `AuthProvider` (Context + `useReducer`) đặt trên `RouterProvider`, trạng thái
    `loading | guest | authenticated | offline | expired`, giữ `CurrentAccount`. Lúc khởi động gọi `/auth/refresh` rồi
    `/auth/me`. Không thêm thư viện. Trong `loading` hiện khung trang, chưa render route cần đăng nhập.
  - **Client dùng chung** (`shared/api/client.ts`): bỏ `X-User-Id`, gắn `Authorization` từ
    `session.ts`, gửi `credentials: 'same-origin'`. **Không** gắn `Authorization` cho `/auth/*`, trừ
    `/auth/me`, `/auth/role-notice/ack`, `/auth/reauthenticate` (R10). Gặp 401 ở request không thuộc
    `/auth/*`: chờ một lần refresh rồi gửi lại đúng một lần; refresh lỗi → ném lại 401 gốc (code
    `UNAUTHENTICATED`) để `useWeekPlan`, `AddToPlanDialog` của 030 chạy như cũ. Refresh thành công
    phát sự kiện `auth:refreshed` kèm `account` để `AuthProvider` cập nhật trạng thái.
  - **Gọi đồng thời**: `refreshPromise` dùng chung (single-flight): N request 401 cùng chờ một promise.
    Giữa các tab: `BroadcastChannel('wikicook-auth')` báo đăng xuất; refresh trùng được R2 xử lý.
  - **403** có code `ACCOUNT_SUSPENDED`, `ACCOUNT_BANNED`: client phát sự kiện, xóa token, tới
    `/account-status` (AU07). `ACCESS_DENIED` → `/access-denied` (AU09). `EMAIL_NOT_VERIFIED` → banner.
  - **Guard**: `RequireAuth` chuyển khách tới `/login?redirect=<đường dẫn>`; `RequireRole minRole`
    cho trang quản trị; `RedirectIfAuthenticated` cho `/login`, `/register` (Edge Cases).
  - **Không có mạng** (Edge Cases, 030 FR-028, 040 FR-019, FR-020, 050 NFR-004): refresh lỗi mạng hoặc 5xx
    khác với 401. `offline`: chưa biết phiên còn không, `RequireAuth` vẫn render trang để feature tự xử lý
    (030 hiện thông báo offline, 040 đọc IndexedDB). Refresh được thử lại khi có sự kiện `online` **và**
    theo thời gian (5, 15, 30 giây, sau đó mỗi 30 giây), vì backend khởi động lại trả 5xx mà trình duyệt
    vẫn online nên không có sự kiện `online`. Sự kiện `auth:refreshed` của client cũng đưa `offline` về
    `authenticated`. `expired`: refresh trả 401 sau khi đã `authenticated` hoặc `offline`. `RequireAuth`
    **không** chuyển trang khi `expired`: route `offlineFirst` (`/shopping-list*`, sau này chế độ nấu)
    để feature hiện banner không chặn; route khác hiện `SessionExpiredDialog` (đặt ở `AppLayout`) với nút
    "Đăng nhập lại" tới `/login?redirect=`, nên form đang nhập không bị gỡ khỏi trang (Edge Cases). Khách
    mới, online, 401 lúc khởi động vẫn là `guest` và bị chuyển `/login`. 030 hiện tự chuyển `/login` khi
    gặp 401 (`MealPlannerPage`, `AddToPlanButton`); 010 sửa để 030 chỉ làm vậy với khách (T026a, T054a).
  - **Phạm vi offline**: chỉ áp dụng khi app đã tải (tab đang mở, hoặc nginx vẫn phục vụ trang khi
    backend lỗi). Repo chưa có service worker và `index.html` trả `Cache-Control: no-cache`, nên tải lại
    trang khi mất mạng thật sẽ ra trang lỗi của trình duyệt. App shell offline thuộc 040, 050 (bảng
    cuối, mục 41).
  - **Redirect an toàn** (`safeRedirect(value)`): nhận khi bắt đầu đúng một `/`, không có `//` hay
    `/\` ở đầu, không chứa ký tự điều khiển, `new URL(value, origin).origin === origin`, và không trỏ
    về trang auth. Sai → `/` (AF 2.10). Backend kiểm tra lại cùng quy tắc với `redirect` của Google.
  - **Khôi phục cho 030**: giữ nguyên quy ước `encodeURIComponent(path + search)` đang có ở
    `AddToPlanButton`, `MealPlannerPage`; sau đăng nhập `navigate(safeRedirect(redirect), { replace:
    true })`, `?action=add-to-plan` tự mở lại dialog.
  - **AU08**: `SignInPrompt` dùng chung cho hành động của khách, "Để sau" không đổi gì.
  - **Hết phiên khi đang nhập form**: `SessionExpiredDialog` (Gap 010-G) gắn sẵn ở `AppLayout`, mở khi
    `status = expired` trên route không `offlineFirst`, không rời trang. Form dài (060) bật `useBlocker`
    của React Router để cảnh báo trước khi rời trang, kể cả khi bấm "Đăng nhập lại".
  - **Trước đăng xuất**: `useAuth().addSignOutGuard(fn)` cho module đăng ký hàm trả `Promise<boolean>`; có
    guard trả `false` thì hủy đăng xuất. 040 dùng để cảnh báo mutation chưa đồng bộ (research 040,
    Logout Rules). Hết phiên hay bị thu hồi không đi qua guard.
  - **Sau đăng xuất**: xóa token, `window.location.replace('/')`, API có `Cache-Control: no-store`
    (mặc định của Spring Security), nên nút Back không hiện lại dữ liệu; thêm kiểm tra ở `pageshow`
    khi `event.persisted` (US1 AS4).
  - **Giao diện**: dựng theo `W-AU01`, `AU02`–`AU09` và token CSS `wikicook-soft-editorial.css`
    (`.wk-field`, `.wk-input`, `.wk-error`, `.wk-btn`) đã map trong `tailwind.config.ts`. `shared/ui`
    hiện trống: đưa `TextField`, `PasswordField` vào đây để 011 dùng lại. Lỗi nằm cạnh trường,
    `aria-describedby`, `role="alert"` cho lỗi chung (NFR-007). Chuỗi ở `features/auth/strings.vi.ts`.
    Các màn còn thiếu theo Gap 010-A…N dựng từ cùng component.
- **Rationale**: Constitution III, không thêm phụ thuộc. Client giữ một đường refresh duy nhất nên các
  feature không phải biết về token.
- **Alternatives considered**: Redux/TanStack Query – chưa cần; refresh theo timer – phá mốc 24 giờ
  không hoạt động (R2); Web Locks giữa các tab – đã có khoảng 10 giây của R2 nên không cần;
  kiểm tra redirect bằng `startsWith('/')` – để lọt `//evil.com`.

## R13. Gỡ dev stub và `X-User-Id`

- **Decision**: Làm trong cùng nhánh với bước bật Spring Security, không để lại khoảng trung gian. Thêm
  starter security làm **mọi** request `/api/**` chưa có JWT thành 401, nên phải sửa test cùng lúc.

  | Vị trí | Việc |
  |---|---|
  | `ShoppingListController` | Bỏ mọi `@RequestHeader("X-User-Id")`, lấy `CurrentUser.id()`; Service giữ nguyên |
  | `shared/api/client.ts` | Bỏ `X-User-Id: 1`, thêm bearer (R12) |
  | `com.wikicook.devstub` | Xóa `StubCurrentUser`, `DevUserProvisioner` + test; giữ `StubDietaryProfile` tới khi 011 có `DietaryProfilePort`, như package doc ghi |
  | `mealplan/support/HeaderCurrentUser` | Chuyển sang `com.wikicook.support`, thêm `@Primary`, thêm `SecurityFilterChain` `HIGHEST_PRECEDENCE` permit-all chỉ cho test |
  | 5 IT của 030 | Không đổi nội dung: vẫn dùng `X-Test-User` |
  | `AiPlannerControllerUS1IT` | Bỏ `dev-stubs.enabled`, chuyển từ `X-Dev-User-Id` sang `HeaderCurrentUser`, thay `DevUserProvisioner.ensure` bằng `AuthTestSupport` tạo dòng `users` |
| `WikiCookApplicationIT` | `unknownPathReturnsProblemDetail` đang chờ 404; với mặc định từ chối, route lạ không có token là 401. Đổi kỳ vọng thành 401 `UNAUTHENTICATED`; 404 kiểm tra bằng JWT thật ở `SecurityFilterChainIT`. `aiPlannerWithoutSignedInUserIs401` giữ nguyên, sửa chú thích |
  | `F1`, `F3`, `F4` (040) | `X-User-Id` → `X-Test-User` |
  | `ShoppingListControllerTest`, 2 `*ExceptionHandlerTest` (`@WebMvcTest`) | Tắt filter (`addFilters = false`), mock `CurrentUser` |
  | `src/test/server.ts`, `e2e/fakeApi.ts` | Mặc định trả `POST /auth/refresh` (401 hoặc tài khoản giả), vì msw đang `onUnhandledRequest: 'error'` |
  | `perf/meal-planner.k6.js` | `setup()` đăng nhập tài khoản test, bỏ `X-Dev-User-Id` |
  | `search/port/DevHeaderSearchViewer`, `SearchPortsConfig` (020, đã có ở `main`) | Thêm `AuthSearchViewer` (id của người đăng nhập, rỗng với khách) thắng nhờ `@ConditionalOnMissingBean`; xóa `DevHeaderSearchViewer`, giữ `guestSearchViewer` |
  | `RecipeSearchControllerIT`, `AiPlannerControllerUS2IT`, `DraftSafetyIT`, `NotEnoughRecipesIT`, `RateLimitIT` | Cùng cách với `AiPlannerControllerUS1IT`: bỏ `dev-stubs.enabled`, `X-Dev-User-Id` |
  | `src/frontend/src/shared/api/session.ts`, `client.ts` (ACT-01, đã có ở `main`) | Bỏ danh tính dev (`getDevUserId`, `X-User-Id`, `X-Dev-User-Id`); token chỉ ở bộ nhớ, thay chỗ lưu `localStorage` hiện có |
  | `docker-compose.yml`, `.env.example` | Bỏ `WIKICOOK_DEV_STUBS_USER_ID`, thêm biến của 010. `WIKICOOK_JWT_SECRET`, `WIKICOOK_EMAIL_HASH_KEY` không có mặc định (constitution IV), dùng `${VAR:?…}` để báo lỗi rõ; `WIKICOOK_PUBLIC_BASE_URL` mặc định `http://localhost:3000`, `WIKICOOK_AUTH_COOKIE_SECURE` mặc định `false` vì Compose là môi trường local |

  Thêm `SecuredRoutesIT` **không** dùng filter giả: với JWT thật, kiểm tra 401 và cách ly người dùng trên
  `/meal-plans`, `/shopping-lists`, `/ai-planner`, chứng minh không còn cách mạo danh qua header.
- **Rationale**: `X-User-Id` là lỗ mạo danh thật: bất kỳ ai sửa header là thành người khác. Giữ dòng
  chảy test của 030, 031, 040 ở `X-Test-User` để diff nhỏ và không phải dựng phiên trong từng IT; phần
  bảo mật có test riêng ở `SecuredRoutesIT`. Bean test phải `@Primary` vì khi 010 có bean thật,
  `ObjectProvider<CurrentUser>.getIfAvailable()` sẽ lỗi nếu có hai bean.
- **Alternatives considered**: Dùng JWT thật trong mọi IT – phải tạo dòng `user_sessions` ở 14 file,
  không tăng độ phủ so với `SecuredRoutesIT`; giữ header dưới cờ dev – vẫn là cửa mạo danh nếu cấu hình
  nhầm; xóa cả package `devstub` – `DietaryProfilePort` chưa có cài đặt thật.

## R14. Kiểm thử và hiệu năng

- **Decision**:
  - **Backend**: JUnit 5 + Mockito cho `auth.service`; `*IT` với Testcontainers
    `pgvector/pgvector:pg16` như hiện tại (Failsafe); `spring-security-test` cho MockMvc; WireMock (đã
    quản lý version ở pom cha) đóng vai Google ở `GoogleSignInIT`; `JavaMailSender` giả ghi lại thư
    (không cần SMTP thật trong CI). Không H2.
  - **Mỗi acceptance scenario ≥ 1 test** (SC-007, constitution II), đánh số `US1-1`… trong tên test.
  - **JaCoCo**: thêm rule PACKAGE cho `com.wikicook.auth.service`, LINE và BRANCH ≥ 70% vào
    `wikicook-app/pom.xml`, theo gate `v0.8`.
  - **Frontend**: Vitest + RTL + msw cho form, `AuthProvider`, single-flight, `safeRedirect`; Playwright
    cho US1, US3 Guest, US4 trên `fakeApi`.
  - **Tốc độ test**: `wikicook.auth.bcrypt-strength=4` trong test, có một test khẳng định mặc định 12.
  - **BCrypt cost 12**: giữ theo full-schema và project plan. Vì đây là tác vụ CPU, chạy trên virtual
    thread, nên bọc mọi lần encode và matches bằng **bulkhead** `Semaphore(max(2, cores − 1))` để không
    chiếm hết carrier thread của request khác. `dummyHash` đi cùng bulkhead (NFR-003).
  - **Ngân sách NFR-004**: mỗi lần đăng nhập gồm BCrypt và khoảng 4 thao tác DB ngắn. Số liệu BCrypt
    cost 12 **chưa đo được** trong lúc lập plan (máy lập plan không có JDK 21 và Docker). Giả định làm
    việc 200–300 ms/lần trên một lõi, tức khoảng 10–15 lần/giây trên máy 4 lõi; task đầu của phần hiệu
    năng phải đo lại rồi cập nhật dòng này.
  - **k6**: `src/backend/perf/auth-sign-in.k6.js`, tách tag:

    | Kịch bản | Tải | Ngưỡng | Nguồn |
    |---|---|---|---|
    | Đăng nhập | 100 VU, `constant-arrival-rate` 5 lần/giây, 10 phút | p95 < 2 s, không có 5xx | NFR-004 |
    | Refresh + `/auth/me` | 20 VU, 5 phút (`v0.8`); 100 VU, 10 phút (`v1.0`) | p95 < 500 ms | Project plan 4.4.2 |
    | API có bearer (kèm query phiên) | Cùng hai mức trên | p95 < 500 ms | Project plan 4.4.2 |

    k6 chạy từ một máy, nên backend phải nâng `WIKICOOK_AUTH_SIGN_IN_IP_LIMIT` (R5), mỗi VU dùng tài
    khoản riêng trong một nhóm tạo sẵn, và có ngưỡng `http_reqs{status:429}` = 0; không có ngưỡng này thì
    429 vẫn qua được điều kiện "không có 5xx" và p95 đo trên phản hồi bị từ chối.

  - Nếu đo vượt ngưỡng: tăng lõi hoặc giới hạn bulkhead trước; hạ cost xuống 11 chỉ sau khi nhóm đồng ý.
- **Rationale**: Gate `v0.8` ghi "P95 < 500 ms cho non-AI"; đăng nhập không thể đạt mức đó vì BCrypt cố
  ý chậm, nên phải tách khỏi gate đó và dùng ngưỡng 2 giây của NFR-004 (bảng cuối). Vòng lặp đóng 100
  VU không nghỉ sẽ xếp hàng tới nhiều giây vì giới hạn CPU, không phản ánh "normal load"; open model
  5 lần/giây tương ứng mỗi người đăng nhập một lần mỗi 20 giây, vẫn cao hơn nhiều thực tế.
- **Alternatives considered**: H2 – constitution cấm; hạ cost ngay – giảm bảo vệ trước khi có số đo;
  không có bulkhead – một đợt đăng nhập làm chậm mọi API; mở rộng pool Hikari trước khi đo – không có
  căn cứ.

## R15. Lỗi trả về client

- **Decision**: `ProblemDetail` với trường `code`, cùng kiểu với 030, 031. Filter security ghi JSON qua
  `ProblemResponseWriter` dùng chung cho entry point và access-denied handler (lỗi ở filter không đi
  qua `@RestControllerAdvice`). Danh sách đầy đủ ở [contracts/openapi.yaml](contracts/openapi.yaml).
  Lỗi trường dùng `errors: [{ field, code }]` để frontend hiển thị cạnh trường (NFR-007).
  - **Lỗi phân quyền ở tầng method**: `@ModeratorOnly`, `@AdministratorOnly`, `@VerifiedEmailRequired`
    ném `AccessDeniedException` (`AuthorizationDeniedException`) **bên trong** controller, nên lỗi đi qua
    `@RestControllerAdvice` chứ không qua `ExceptionTranslationFilter`. `GlobalExceptionHandler` hiện bắt
    `Exception.class` và không có `@Order`, còn advice của 030, 031, 040 giới hạn `basePackages`. Vì vậy
    cần một advice **toàn cục** riêng, `SecurityExceptionAdvice` (`@Order(HIGHEST_PRECEDENCE)`), **chỉ**
    bắt `AccessDeniedException`: trả 403 `EMAIL_NOT_VERIFIED` khi handler method (hoặc lớp) có
    `@VerifiedEmailRequired` và người gọi thiếu authority `EMAIL_VERIFIED`, còn lại 403 `ACCESS_DENIED`.
    Nếu không, mọi lỗi thiếu quyền ở module khác thành 500. `AuthExceptionHandler` vẫn giới hạn
    `basePackages = "com.wikicook.auth"`: advice của 030, 031 cũng ở `HIGHEST_PRECEDENCE` và bắt
    `MethodArgumentNotValidException`, nên một advice toàn cục cùng mức mà bắt lỗi validation sẽ tranh
    với chúng theo thứ tự không xác định. `MatrixProbeController` của test đặt ngoài
    gói `auth` để `PermissionMatrixIT` kiểm đúng trường hợp này.
- **Rationale**: Frontend phân nhánh theo `code`, chuỗi nằm ở string store (constitution V).
- **Alternatives considered**: Thông báo do server viết – trộn chuỗi hiển thị vào API; hai mã khác nhau
  cho hết hạn và thu hồi phiên – 030 chỉ biết `UNAUTHENTICATED`.

## R16. Đường đóng tài khoản cho người bị cấm (`OQ-010-04`)

- **Decision**: FR-013 vẫn đúng: không có phiên. Khi mật khẩu (hoặc Google) đúng và tài khoản `BANNED`,
  phản hồi 403 `ACCOUNT_BANNED` kèm `closureToken`: token một lần, `purpose = 'ACCOUNT_CLOSURE'`, 10
  phút, cơ chế như R6. Màn AU07 hiện link `/account-closure#token=<closureToken>` (chính là "link" của FR-013) tới trang đóng tài khoản của 011, token nằm ở fragment như R6; endpoint xóa của 011 nhận
  token này thay cho bearer và gọi `ClosureTokens.consume`. Chỉ có token mới mở được hành động đó, nên
  không phải là phiên.
- **Rationale**: `OQ-010-04` hoãn tới plan; 011 FR-037b, FR-031 cần một đường vào. Cách này không phải
  sửa `users.status` hay `user_sessions`, và người có token đã chứng minh đúng mật khẩu.
- **Alternatives considered**: Phiên hạn chế (cờ trên `user_sessions`) như gợi ý của OQ – phải dạy mọi
  kiểm tra về một loại phiên thứ hai, dễ lọt quyền; yêu cầu liên hệ quản trị – trái FR-026.

## R17. Giới hạn mật khẩu sai khi đã đăng nhập (FR-022b, `OQ-011-05`)

- **Decision**: Bảng `password_check_failures (id, user_id, created_at)` ở `V103`.
  `PasswordCheckLimiter` nằm trong `PasswordService.checkCurrent`; `POST /auth/reauthenticate` và đổi
  mật khẩu của 011 đều đi qua hàm này. Cùng cách đặt trước của R4, **không giữ transaction trong lúc
  chạy BCrypt**:

  | Bước | Việc | Ghi chú |
  |---|---|---|
  | 1 | Transaction ngắn có `pg_advisory_xact_lock` theo `userId`: đọc 5 dòng mới nhất; đủ 5, dòng đầu và dòng cuối cách nhau không quá 15 phút, và chưa quá 15 phút từ dòng mới nhất → 429 `PASSWORD_CHECK_LIMITED` + `retryAfterSeconds`, không chạy BCrypt | Mốc mở lại tính từ lần sai thứ 5, giống FR-012 |
  | 2 | Chèn trước một dòng, commit | Gửi song song cũng không vượt 5 lần |
  | 3 | So khớp qua bulkhead (R14) | |
  | 4a | Sai: giữ dòng, trả `false` | |
  | 4b | Đúng: xóa mọi dòng của người này, trả `true` | "Liên tiếp" như FR-012 |

  `AuthRetentionJob` dọn dòng quá 1 ngày. Tài khoản chỉ Google không có mật khẩu để đoán nên không đi
  qua đây.
- **Rationale**: Không có giới hạn này, phiên bị đánh cắp đoán được mật khẩu qua màn xóa tài khoản.
  Tách khỏi `login_attempts` để kẻ giữ phiên bị đánh cắp không khóa được đăng nhập của chủ tài khoản.
  429 thay vì 423 vì đây không phải khóa tài khoản.
- **Alternatives considered**: Dùng `login_attempts` – khóa cả đăng nhập, trái `OQ-011-05`; đặt bộ đếm
  ở 011 – `/auth/reauthenticate` là của 010 nên không đi qua được; bộ đếm trong bộ nhớ – sai khi nhiều
  instance.

---

## Phụ lục: phủ FR/NFR

| Yêu cầu | Thiết kế chính | Hợp đồng / dữ liệu |
|---|---|---|
| FR-001 | R3, R6 | `POST /auth/register`, `RegisterRequest` |
| FR-002 | R6, R11 | `chk_users_email_normalized`, `EmailNormalizer` |
| FR-003 | R7 | `users.role` mặc định `MEMBER` |
| FR-004 | R3 | `@ValidPassword` |
| FR-005 | R6, R8, R9 | `one_time_tokens`, `/auth/verification/*`, `UnverifiedAccountPurgeJob` |
| FR-006 | R10 | `@VerifiedEmailRequired`, 403 `EMAIL_NOT_VERIFIED` |
| FR-007 | R3, R4 | `POST /auth/login` |
| FR-008 | R4 | 401 `INVALID_CREDENTIALS` |
| FR-009 | R2 | `user_sessions`, cookie `wc_rt` |
| FR-010 | R1, R12 | `POST /auth/logout` |
| FR-011 | R12 | `safeRedirect`, `redirect` |
| FR-012 | R4, R8 | `login_attempts`, `locked_until`, 423 `ACCOUNT_LOCKED` |
| FR-013 | R1, R4, R9 | 403 `ACCOUNT_SUSPENDED`, `ACCOUNT_BANNED`, `user_suspensions` |
| FR-014 | R5, R8 | `POST /auth/password/forgot` |
| FR-015 | R6 | `one_time_tokens`, `POST /auth/password/reset` |
| FR-016 | R1, R6 | `SessionService.revokeAll` |
| FR-017 | R5 | `email_send_requests`, 429 `EMAIL_RATE_LIMITED` |
| FR-018 | R7 | `/auth/oauth2/*`, `users.google_sub` |
| FR-019 | R7 | `AccountResetEvent` |
| FR-020 | R10 | `users.role`, `Role` |
| FR-021 | R10 | `SecurityFilterChain`, method security, `PermissionMatrixIT` |
| FR-022 | R10 | `/admin/users/{id}/…` |
| FR-022a | R10 | `AccountActionPolicy`, `/auth/reauthenticate` |
| FR-022b | R7, R17 | `password_check_failures`, 429 `PASSWORD_CHECK_LIMITED`, `?reauth=cancelled` |
| FR-023 | R10 | 409 `LAST_ADMINISTRATOR`, `AccountGuard` |
| FR-024 | R1, R10 | Authority đọc từ DB mỗi request |
| FR-024a | R8, R9, R11 | `email_outbox`, `pending_role_notice_from`, `X-WikiCook-Role-Notice`, `/auth/role-notice/ack`, `wikicook.auth.appeal-contact` |
| FR-025 | R11 | `audit_logs` |
| FR-026 | R11, R16 | `blocked_emails`, `BlockedEmails`, 422 `REGISTRATION_REFUSED` |
| NFR-001 | R3 | Không log thân request auth; DTO không có mật khẩu ở phản hồi |
| NFR-002 | R2, Plan | Cookie `Secure`, HSTS ở proxy TLS; `TODO(HOSTING)` |
| NFR-003 | R4, R5, R8 | `dummyHash`, đếm theo email kể cả email lạ, `forgot` bất đồng bộ |
| NFR-004 | R14 | `auth-sign-in.k6.js` |
| NFR-005 | R7 | Không dùng `issuer-uri`, timeout 5 giây, `google_unavailable` |
| NFR-006 | R12 | Trang từ 360 px, `strings.vi.ts` |
| NFR-007 | R12 | `TextField`, `aria-describedby`, `role="alert"` |
| NFR-008 | R9, R11 | `audit_logs`, `GET /admin/audit-logs` |
| NFR-009 | R10, R11 | Email chỉ ở `/auth/me` và API admin; `details` không có email |

---

## Điểm cần chốt với nhóm

Hiển chốt ngày 2026-11-06. Các dòng "đã vào spec" có hiệu lực khi Duy duyệt PR, theo quy định ở
`use-case-specification.md` mục 9.3.

| # | Với | Nội dung | Quyết định | Trạng thái |
|---|---|---|---|---|
| 1 | A, E, Duy | **Moderator đình chỉ**: 010 FR-022 + ma trận chỉ cho Administrator; 100 FR-011 + ma trận 100 cho Moderator đình chỉ tối đa 30 ngày; 010 FR-022a và màn AU07 ngụ ý Moderator có thể tác động vai trò thấp hơn | Sửa 010 theo 100: Moderator đình chỉ Member, Contributor đang `ACTIVE`, tối đa 30 ngày; chỉ Administrator cấm, đổi vai trò, gỡ án sớm (R10). Spec 100 không nói ai gỡ án, nên chốt theo hướng hẹp quyền | Đã vào spec, chờ Duy duyệt PR |
| 2 | E | **Tên cột**: `ends_at`, `created_by`, `kind` (full-schema) và `suspended_until`, `suspended_by`, `status` (spec 100) | Giữ tên full-schema vì `V102` tạo bảng từ đó. "Hết hạn" = `lift_reason = 'EXPIRED'` (cột mới, không suy từ `lifted_by` vì khóa ngoại `SET NULL` làm mất người gỡ khi tài khoản bị xóa) | Đã sửa spec 100 ngày 2026-11-07; chờ E xác nhận |
| 3 | Hiển, Duy | **Tên vai trò**: project plan (`Standard User`, `Culinary Creator`, `Platform Administrator`, thiếu Moderator) khác spec | Dùng tên spec và `users.role` | Chờ sửa project plan 4.4.2, 4.4.5 |
| 4 | Cả nhóm | **Mã Jira** cho 010 còn thiếu | Theo `pa4-guide.md` (Sprint 04): SCRUM-72 tài liệu Spec Kit 010 + 011, SCRUM-74 backend 010, SCRUM-76 frontend 010 + 011 | **Đã có** (2026-11-11); tasks.md mục "Mã Jira" |
| 5 | E | **Clarification của 100** ghi không đẩy người bị đình chỉ ra ngay, chờ JWT hết hạn 15–30 phút, trái 010 FR-024 và Integration Points của 010 | 010 FR-024 là chuẩn (R1) | Đã sửa Clarification của 100 ngày 2026-11-07; chờ E xác nhận |
| 6 | E | **`audit_logs`**: spec 100 có `old_value`, `new_value`, `performed_at`, full-schema có `details`, `created_at`. Spec 100 đòi append-only, NFR-008 chỉ cần ≥ 90 ngày, full-schema ghi "giữ ≥ 90 ngày" | Theo full-schema, `details = {old, new}`; append-only; job không xóa audit (R9). Giới hạn dung lượng để sau `v1.0` | Đã sửa spec 100 ngày 2026-11-07; chờ E xác nhận |
| 7 | E | **Ranh giới migration**: `V102` tạo sớm hai bảng của `V503` | `V102` tạo hai bảng; A không đụng `reports`, `moderation_actions` | Chờ E bỏ khỏi `V503` |
| 8 | E | **Ai kết thúc đình chỉ**: 010 Integration Points ghi 100 báo cho 010 khi đình chỉ hết hạn, nhưng chưa có module 100 | 010 tự chạy `SuspensionExpiryJob`; 100 chỉ gọi `reinstate` khi gỡ sớm | Đã vào spec (Integration Points), chờ Duy duyệt PR |
| 9 | Duy, Hiển | **Quyết định 30/10 chưa vào spec**: `OQ-010-03` (link xác minh cũ), `OQ-010-05` (thông báo trung tính), `OQ-010-06` ("thử lại sau X phút"), `OQ-010-07` (tiếp quản được hoặc mất cả), `OQ-010-02` (đếm cả email lạ, giới hạn IP), `OQ-010-08` (phiên Google) | Ghi cả sáu quyết định vào spec, gồm giới hạn theo IP ở FR-012; mức 100 lần mỗi 15 phút nằm ở R5 | Đã vào spec, chờ Duy duyệt PR |
| 10 | Hiển, Duy | **`OQ-010-02`, `OQ-010-10`**: reset có gỡ khóa không (AU07 ghi "đặt lại mật khẩu ngay", UC nói không); khóa có chặn Google không; tài khoản chỉ Google, đình chỉ, cấm có nhận link không; đăng ký, xác minh có tự đăng nhập không | Không gỡ khóa; không chặn Google; không cấp link cho ba loại đó (vẫn trả trung tính); không tự đăng nhập (FR-005, FR-012, FR-014) | Đã vào spec, chờ Duy duyệt PR |
| 11 | Hiển, A (011), Duy | **`OQ-010-04`** đường đóng tài khoản cho người bị cấm | `closureToken`, 10 phút, không tạo phiên (R16, FR-013) | Đã vào spec, chờ Duy duyệt PR; 011 nhận token khi plan 011 |
| 12 | Hiển, Dự | **Redis và "stateless"**: `architecture.md` (mục 2, 3) và project plan 4.4.4, 4.4.6 ghi Redis 7, nhưng Compose không có, 031 cũng đếm bằng PostgreSQL | Không thêm Redis (R5); JWT + kiểm tra phiên ở DB | Chờ sửa `architecture.md`, project plan |
| 13 | Hiển, Dự, Chung | **Gate hiệu năng**: "P95 < 500 ms non-AI" của `v0.8`, `v1.0` không áp dụng được cho đăng nhập; spec chưa định nghĩa tải "100 concurrent users" | Đăng nhập theo NFR-004 (< 2 s), không theo gate 500 ms; kịch bản ở R14 | Chờ sửa project plan; task hiệu năng đo lại BCrypt |
| 14 | Hiển, Duy | **Demo D1**: project plan ghi "Guest bị từ chối HTTP 403", còn khách chưa đăng nhập theo thiết kế nhận 401 (403 dành cho đã đăng nhập mà thiếu quyền) | Giữ 401 cho khách, 403 cho đã đăng nhập mà thiếu quyền | Chờ sửa D1 trong project plan |
| 15 | Hiển, Duy | **FR-005 và tài khoản bị cấm**: tài khoản chưa xác minh bị cấm, sau 7 ngày bị xóa và email tự do, trái mục đích FR-026 | Vẫn xóa sau 7 ngày, nhưng `BANNED` thì block hash (FR-005, FR-026, R9) | Đã vào spec, chờ Duy duyệt PR |
| 16 | E, Duy | **"Reason category" (FR-013)**: `user_suspensions.reason` là chữ tự do, không có danh mục; AU07 hiện cả câu lý do | Không có danh mục; hiện nguyên `reason` (FR-013) | Đã vào spec, chờ Duy duyệt PR |
| 17 | E, Duy | **Thăng lên Administrator**: dropdown của 100 chỉ có Member ↔ Contributor ↔ Moderator; 010 không cấm và chỉ yêu cầu xác thực lại khi tác động Administrator khác | Cho phép, cần xác thực lại và luôn audit (FR-022a) | Đã vào spec; chờ E bổ sung vào 100 |
| 18 | B (060), A (011, 080) | **Listener xóa dữ liệu** (`OQ-010-07`): `recipes.author_id` là `SET NULL` nên 060 **bắt buộc** xóa công thức trong `AccountResetEvent`, `AccountPurgedEvent` | Listener của 060 là bắt buộc; lỗi thì rollback cả thao tác (FR-019) | Chờ B và Duy xác nhận 060 có handler (`OQ-010-07`) |
| 19 | C, D | **Sửa test và client**: 030, 031, 040 đổi theo R13; 040 cần gắn `userId` cho hàng đợi IndexedDB, vì research 040 giữ hàng đợi khi hết phiên và người đăng nhập tiếp theo có thể đồng bộ nhầm vào tài khoản của mình | Sửa test và client cùng nhánh 010 (R13); D gắn `userId` cho hàng đợi trước khi nối đăng nhập thật | Chờ C, D nhận việc |
| 20 | Dự | **`full-schema.md` cần cập nhật** sau khi duyệt: `V102`–`V104`, cột mới, `audit_logs.actor_id` không khóa ngoại và trigger cấm UPDATE, DELETE, `user_suspensions.lift_reason`, 4 bảng mới, `login_attempts`, retention; bỏ hai bảng khỏi `V503`; mục 7 còn ghi `V302__ai.sql` và thiếu `V303`, `V304`, `V402`, `V403` | Cập nhật sau khi PR spec được duyệt | Chờ Dự |
| 21 | Hiển, Dự | **`TODO(HOSTING)`**: TLS cho NFR-002, nhà cung cấp SMTP thật, Google OAuth client và redirect URI theo `WIKICOOK_PUBLIC_BASE_URL` | Local dùng Mailpit; hosting chưa chốt | **Mở**, không chặn tasks; chốt trước demo |
| 22 | Chung | **UI**: 14 Gap 010-A…N chưa có màn; `OQ-010-09` bỏ "Đổi địa chỉ email" khỏi AU03; AU07 ghi "Tài khoản tạm khóa… đã gửi email", lộ tài khoản; chữ lỗi spec tiếng Anh trong khi NFR-006 mặc định tiếng Việt | Theo đề xuất; bỏ "Đổi địa chỉ email" khỏi AU03; thông báo hiển thị tiếng Việt (Clarifications 2026-11-06) | Chờ Chung |
| 23 | A (011), Duy | **NFR-009**: 011 Integration Points yêu cầu đổi thành hai mức (hồ sơ ăn uống chỉ chủ tài khoản xem, Administrator cũng không), spec 010 chưa đổi | Hai mức theo 011; audit không chứa dữ liệu ăn uống (NFR-009, FR-025) | Đã vào spec, chờ Duy duyệt PR |
| 24 | Hiển, Duy | **FR-017**: chung cho hai loại email hay từng loại; lần gửi đầu có tính không | Tách theo loại, lần đầu có tính (FR-017) | Đã vào spec, chờ Duy duyệt PR |
| 25 | E | **Spec 070** ghi tài khoản bị xóa thuộc spec 010; thực tế là spec 011 | Tham chiếu đúng là spec 011 | Chờ E sửa spec 070 |
| 26 | Hiển | **Spec 010 Status** là "Ready for Implementation (Sprint 02)" nhưng mâu thuẫn ở các mục 1, 5, 9 còn mở | Status đổi thành "In Review"; `StubDietaryProfile` ở lại tới 011 (R13) | Chờ Duy duyệt PR |
| 27 | Hiển, Duy | **FR-005 và tài khoản bị đình chỉ**: tài khoản chưa xác minh đang bị đình chỉ, sau 7 ngày bị xóa và email tự do, nên người đó thoát án bằng cách đăng ký lại | Hoãn xóa đến khi hết đình chỉ; vẫn chưa xác minh thì xóa ở lượt chạy kế tiếp (FR-005, R9) | Đã vào spec, chờ Duy duyệt PR |
| 28 | Hiển, Duy | **`OQ-011-05`**: `POST /auth/reauthenticate` không có giới hạn, nên phiên bị đánh cắp đoán được mật khẩu qua màn xóa của 011 | Một bộ đếm mỗi người, 5 lần sai trong 15 phút, chung với đổi mật khẩu của 011, không khóa đăng nhập (FR-022b, R17) | Đã vào spec, chờ Duy duyệt PR |
| 29 | Hiển, Duy | **Điểm nối với 011** (011 research, bảng cuối, mục 13): `intent` khác nhau giữa integration và openapi; hủy Google về `/login`; `wc_rt` không có ở callback; link AU07; thứ tự khóa Administrator; `UserAccount` ghi đè cột của 011; mã lỗi của `AccountGuard` | `intent=reauth`, `?reauth=cancelled`, `sid` trong `wc_oauth` (R7); `/account-closure#token=…` (R16); `ORDER BY id`, mã lỗi (R10); `@DynamicUpdate` (data-model) | Đã vào spec và tasks, chờ Duy duyệt PR |
| 30 | Hiển, Duy | **Google và tài khoản bị xử lý** (analyze I2): FR-019 tiếp quản tài khoản chưa xác minh, xóa luôn án đang chạy | Không tiếp quản tài khoản đình chỉ, cấm; từ chối như FR-013 (R7) | Đã vào spec, chờ Duy duyệt PR |
| 31 | E, Duy | **Khiếu nại** (analyze I3): 010 nói 100 sở hữu, spec 100 không có | Không có quy trình; email ghi `WIKICOOK_APPEAL_CONTACT`, Administrator xử lý thủ công (R8) | Đã vào spec 010, 100; chờ Duy duyệt, E xác nhận |
| 32 | E, Duy | **Gỡ lệnh cấm** (analyze I4): 100 ghi không thể hoàn tác, 010 FR-022, FR-024a cho gỡ | Gỡ được, chỉ Administrator; sửa spec 100, thêm scenario và FR | Đã vào spec 100; chờ E xác nhận |
| 33 | Hiển, Duy | **Notice đổi vai trò ở request kế tiếp** (analyze U1) | Header `X-WikiCook-Role-Notice`, client tải lại `/auth/me` (R11) | Đã vào spec, chờ Duy duyệt PR |
| 34 | C, D, Duy | **Mở app khi mất mạng** (analyze I1): 030 FR-028, 040 FR-020, 050 NFR-004 | Thêm `offline`, `expired`, `RequireAuth offlineFirst` (R12) | Đã vào spec, chờ Duy duyệt PR; C, D xác nhận |
| 35 | E | **Gỡ cấm và công thức**: 100 Edge Case 5 chuyển công thức sang `ARCHIVED` khi cấm, nhưng không nói gỡ cấm thì khôi phục không | Chưa quyết | **Mở**, thuộc 100, không chặn 010 |
| 36 | A, E, Duy | **Giới hạn 30 ngày** (analyze I6): ma trận và US3 của 100 áp dụng cho cả Administrator, 010 chỉ giới hạn Moderator | Mọi kỳ đình chỉ tối đa 30 ngày; cần lâu hơn thì cấm (FR-022, R10) | Đã vào spec, chờ Duy duyệt PR |
| 37 | Hiển, Duy | **Giới hạn email khi đăng ký** (analyze U5): FR-017 tính lần gửi đầu nhưng FR-002 yêu cầu email tự do ngay | Vẫn tạo tài khoản, không gửi mail, `verificationEmailSent = false` (FR-017, R5) | Đã vào spec, chờ Duy duyệt PR |
| 38 | Hiển, Dự | **Compose khi chưa có `.env`** (analyze U6): thêm secret bắt buộc thì mọi lệnh `docker compose` dừng nếu chưa copy `.env.example`, kể cả `up -d db` | Chấp nhận, vì không được commit secret (constitution IV); gate `v0.5` "clean clone" đã qua, cần ghi vào gate `v0.8` | Chờ Dự xác nhận |
| 39 | D | **Cảnh báo trước đăng xuất** (analyze I7): research 040 cần modal khi còn mutation chưa đồng bộ | 010 cung cấp `addSignOutGuard`, 040 đăng ký (R12) | Chờ D |
| 40 | Hiển, Dự | **Xác thực lại bằng Google** (analyze U11): tài liệu Google không có `max_age`, `prompt=login`; `auth_time` phải xin qua `claims` và bật trong cài đặt client | Đề xuất ở R7: `prompt=login` + `claims` xin `auth_time`, thiếu `auth_time` thì từ chối. Thử với client thật (T149b); nếu Google không trả `auth_time`, nhóm chọn cách khác cho tài khoản chỉ Google (ví dụ xác nhận qua link email) và sửa 011 FR-029, FR-031 | **Mở**, chặn T149a và bước Google thật của Q42; cần client của T178 |
| 41 | C, D, Chung | **App shell offline** (analyze U12): chưa có service worker, tải lại trang khi mất mạng ra trang lỗi của trình duyệt; 030 FR-028, 040 FR-019, 050 NFR-004 giả định app đã mở được | 010 chỉ xử lý phiên khi app đã tải (Edge Cases, Assumptions). Ai làm service worker, cache những gì thì 040, 050 quyết | **Mở**, không chặn 010 |
| 42 | E | **Tên `action` của audit** (analyze I14): 100 lọc `USER_SUSPENDED`, 010 ghi `ACCOUNT_SUSPENDED`; 100 dòng 293 suy trạng thái từ `user_suspensions`, trái `users.status` (full-schema 4.10); `reports`, `moderation_actions` của 100 còn lệch full-schema | Dùng tên của 010 (data-model mục 1) cho thao tác tài khoản; trạng thái đọc từ `users.status` | Chờ E sửa spec 100 |
| 43 | Hiển, Duy | **Use-case 010 chưa theo spec** (analyze I13): AF 1.7, 1.13, 2.2, 2.3, 2.5, 2.9, 2.14, 3.8, 4.2, 4.6, mục 9.3 `OQ-010-02`, `OQ-010-04`, `OQ-010-10` | Sửa `use-case-specification.md` mục 9 theo spec sau khi Duy duyệt PR (T189) | Chờ Duy duyệt PR |
