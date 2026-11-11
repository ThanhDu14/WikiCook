# Quickstart: kiểm chứng User Authentication & Roles (010)

Hướng dẫn chạy và kiểm chứng tính năng sau khi implement. Chi tiết API ở
[contracts/openapi.yaml](contracts/openapi.yaml), dữ liệu ở [data-model.md](data-model.md).

## 1. Điều kiện

- JDK 21, Maven 3.9, Node.js 22+, Docker (Compose v2), k6 cho mục 6.
- Migration đã có tới `V104__auth_mail.sql`; `spring.flyway.out-of-order` đã bật (research R11).
- Service `mailpit` đã thêm vào `src/backend/docker-compose.yml`.
- `src/backend/.env` (copy từ `.env.example`) có các biến dưới đây. Không commit file này. Thiếu `WIKICOOK_JWT_SECRET`,
  `WIKICOOK_EMAIL_HASH_KEY` thì mọi lệnh `docker compose` báo lỗi và dừng, kể cả `up -d db`.

| Biến | Ví dụ local | Ghi chú |
|---|---|---|
| `WIKICOOK_JWT_SECRET` | `openssl rand -base64 48` | ≥ 32 byte |
| `WIKICOOK_EMAIL_HASH_KEY` | `openssl rand -base64 48` | Không đổi sau khi có dữ liệu (research R11) |
| `WIKICOOK_PUBLIC_BASE_URL` | `http://localhost:5173` | Link trong email và redirect URI của Google; chạy cả stack bằng Compose thì là `http://localhost:3000` |
| `WIKICOOK_AUTH_COOKIE_SECURE` | `false` | Chỉ tắt ở local HTTP |
| `MAIL_HOST`, `MAIL_PORT` | `localhost`, `1025` | Trong Compose: `mailpit` |
| `WIKICOOK_MAIL_FROM` | `no-reply@wikicook.test` | |
| `WIKICOOK_APPEAL_CONTACT` | `support@wikicook.test` | Địa chỉ khiếu nại trong email đình chỉ, cấm; bỏ trống (kể cả `WIKICOOK_APPEAL_CONTACT=`) thì dùng `WIKICOOK_MAIL_FROM` |
| `WIKICOOK_AUTH_SIGN_IN_IP_LIMIT` | Bỏ trống | Số lần thử đăng nhập mỗi IP trong 15 phút, mặc định 100; chỉ nâng khi chạy k6 (mục 6) |
| `WIKICOOK_BOOTSTRAP_ADMIN_EMAIL`, `…_PASSWORD` | `admin@wikicook.test`, tự đặt | Chỉ tạo khi chưa có Administrator đang hoạt động |
| `WIKICOOK_GOOGLE_CLIENT_ID`, `…_SECRET` | Bỏ trống hoặc client thật | Bỏ trống: nút Google đưa về thông báo không khả dụng; redirect URI: `{PUBLIC_BASE_URL}/api/v1/auth/oauth2/callback/google` |

Seed test: tài khoản `member@wikicook.test` đã xác minh, `contributor@`, `moderator@`,
`unverified@wikicook.test` (đăng ký bằng `POST /auth/register`, rồi đổi vai trò bằng tài khoản
Administrator).

## 2. Chạy

```bash
# từ thư mục gốc repo
docker compose up -d db mailpit                    # PostgreSQL + Mailpit (http://localhost:8025)
cd src/backend && set -a && . ./.env && set +a      # Maven không tự đọc .env: nạp biến vào shell
                                                   # (PowerShell: Get-Content .env | % { if ($_ -match '^\s*([^#=]+)=(.*)$') { Set-Item "env:$($matches[1])" $matches[2] } })
mvn -pl wikicook-app -am spring-boot:run -Dspring-boot.run.profiles=local
                                                   # http://localhost:8080; thiếu WIKICOOK_JWT_SECRET thì app dừng lúc khởi động
cd src/frontend && npm install && npm run dev      # http://localhost:5173
```

## 3. Bộ test tự động

```bash
cd src/backend && mvn verify      # unit + Testcontainers + JaCoCo ≥ 70% cho auth.service
cd src/frontend && npm run lint && npm test && npm run build && npm run e2e
```

Kỳ vọng: mọi test xanh; `PermissionMatrixIT` đủ 9 dòng × 5 tác nhân (SC-004); `RouteProtectionIT` và
`SecuredRoutesIT` xanh, tức không route nào lọt và không còn mạo danh qua header; báo cáo JaCoCo của
`com.wikicook.auth.service` ≥ 70% cả line và branch. Mỗi acceptance scenario có ≥ 1 test (SC-007).

## 4. Kịch bản kiểm chứng thủ công

Đọc email ở Mailpit (`http://localhost:8025`). Thao tác trên `http://localhost:5173`. SC-003 (90% hoàn
thành ngay lần đầu) đo bằng buổi thử với người dùng, không nằm trong bảng này.

| # | Bước | Kết quả mong đợi | Spec |
|---|---|---|---|
| Q1 | `/register`: nhập tên, email mới, mật khẩu `Banhmi2026`, xác nhận, gửi hai lần liên tiếp | Một tài khoản Member, một email, tới màn "Kiểm tra hộp thư"; chưa đăng nhập | US2-1, FR-001, FR-003, Edge Cases |
| Q2 | Đăng ký lại cùng email, thêm khoảng trắng và chữ hoa | Bị từ chối, gợi ý đăng nhập hoặc đặt lại mật khẩu | US2-2, FR-002 |
| Q3 | Mật khẩu `abcdefgh`, `12345678`, `abc1`, chuỗi 129 ký tự, mật khẩu trùng email | Hiện đúng quy tắc sai cạnh trường, không tạo tài khoản | US2-3, FR-004 |
| Q4 | Đăng ký với mật khẩu 100 ký tự có dấu, rồi đăng nhập; sửa ký tự cuối rồi đăng nhập | Đăng ký không lỗi 500; đúng → vào, sửa ký tự cuối → sai | FR-004, research R3 |
| Q5 | Mở link trong email | "Email đã xác minh", mời đăng nhập; mở lại link → báo đã xác minh | US2-4, AF 1.12, SC-001 |
| Q6 | Đổi `expires_at` của token về quá khứ, mở link, bấm "Gửi link mới" | Báo hết hạn, có link mới, link trước không còn dùng được | US2-6, FR-005 |
| Q7 | Bấm gửi lại lần 4 trong 15 phút | Không có email mới, báo thời gian chờ và nút gửi lại đếm ngược tới lúc gửi được | FR-017, AF 1.9, AF 1.10 |
| Q8 | Đăng nhập tài khoản chưa xác minh, thử gửi công thức, thử thêm món vào meal plan | Vào được, có banner nhắc; gửi công thức 403 `EMAIL_NOT_VERIFIED`, meal plan vẫn dùng được | US2-5, FR-006 |
| Q9 | Đặt `created_at` lùi 8 ngày, chạy `UnverifiedAccountPurgeJob`; request kế tiếp ở phiên cũ | Tài khoản biến mất; 401; đăng ký lại cùng email được ngay; `blocked_emails` không đổi | FR-005, Edge Cases |
| Q10 | Mở `/meal-planner?week=…` khi là khách, đăng nhập với ` Member@WikiCook.test ` | Quay lại đúng tuần đó, hiện tên | US1-1, US1-7, FR-011, SC-002 |
| Q11 | Sai mật khẩu; rồi email chưa đăng ký; so sánh qua DevTools | Cùng thông báo "Email hoặc mật khẩu không đúng", ô mật khẩu bị xóa, thời gian tương đương | US1-2, US1-3, FR-008, NFR-003 |
| Q12 | Sai 5 lần liên tiếp; rồi nhập đúng mật khẩu | Màn khóa có giờ thử lại và câu trung tính; đúng mật khẩu vẫn bị từ chối; có email khóa ở Mailpit | SC-005, FR-012, Edge Cases |
| Q13 | Lặp Q12 với email chưa đăng ký | Cùng màn khóa; không có email; `audit_logs` không có email rõ | NFR-003, `OQ-010-02` |
| Q14 | Đăng nhập, "Đăng xuất", bấm Back | Không thấy nội dung thành viên; JWT cũ gọi API → 401 | US1-4, FR-010 |
| Q15 | Đăng nhập có "Ghi nhớ", đóng rồi mở lại trình duyệt | Vẫn đăng nhập | US1-5, FR-009 |
| Q16 | Đăng nhập không "Ghi nhớ", đóng trình duyệt; lần khác đặt `expires_at` về quá khứ | Phải đăng nhập lại ở cả hai | US1-6, FR-009 |
| Q17 | Đăng nhập ở hai trình duyệt, đăng xuất ở một | Trình duyệt kia vẫn dùng được | Edge Cases |
| Q18 | Đang đăng nhập mở `/login`, `/register`; dán `/login?redirect=//evil.com`, `?redirect=https://evil.com` | Về trang chủ; không rời WikiCook | Edge Cases, AF 2.10 |
| Q19 | *(Cần route `/recipes/{id}` của 060)* Khách bấm "Thêm vào meal plan" ở `/recipes/42` | Sang `/login`; đăng nhập xong quay lại, dialog mở lại | US3-1, 030 US2-4 |
| Q20 | *(Phần trang cần 100)* Member mở trang kiểm duyệt; Member gọi `PATCH /admin/users/{id}/role` | Màn "Bạn không có quyền xem trang này", không lộ nội dung; API 403 `ACCESS_DENIED` | US3-2, FR-021 |
| Q21 | *(Phần hàng đợi cần 100)* Moderator mở hàng đợi duyệt; gọi đổi vai trò | Hàng đợi vào được; đổi vai trò 403 | US3-3, FR-022 |
| Q22 | Administrator đổi vai trò Moderator → Member; người kia bấm tiếp một hành động kiểm duyệt | Bị từ chối ngay, thấy thông báo vai trò mới một lần mà không cần tải lại trang, không có email, có audit `ROLE_CHANGED` | US3-4, US3-5, FR-024, FR-024a |
| Q23 | Administrator đình chỉ Member đang đăng nhập; Member bấm tiếp một thao tác; rồi đăng nhập lại | Hết phiên, màn "bị đình chỉ đến…" kèm lý do; đúng mật khẩu vẫn không vào; có email kèm lý do, ngày kết thúc, địa chỉ khiếu nại | Edge Cases, FR-013, FR-024a |
| Q24 | Đặt `ends_at` về quá khứ, chạy `SuspensionExpiryJob` | `status = ACTIVE`, có email kết thúc đình chỉ; chạy lại không gửi thêm | FR-024a |
| Q25 | Cấm tài khoản; đăng nhập đúng mật khẩu | Màn bị cấm, không có ngày kết thúc, có link đóng tài khoản | FR-013, research R16 |
| Q26 | Administrator tự hạ vai trò mình; hạ Administrator cuối cùng; tác động Administrator khác khi chưa xác thực lại | 403 `SELF_ACTION_FORBIDDEN`; 409 `LAST_ADMINISTRATOR`; 403 `REAUTH_REQUIRED`, xác thực lại xong thì được | FR-022a, FR-023 |
| Q27 | "Quên mật khẩu" với email có và không đăng ký | Cùng thông báo, cùng thời gian; chỉ email có đăng ký nhận link | US4-1, FR-014 |
| Q28 | Yêu cầu reset 2 lần, dùng link cũ, rồi link mới; đặt mật khẩu mới | Link cũ không hợp lệ, link mới dùng được; mật khẩu cũ không vào; mọi phiên khác bị đăng xuất | US4-2, US4-3, US4-4, FR-015, FR-016, SC-006 |
| Q29 | Mở lại link đã dùng; đổi `expires_at` của link khác quá 30 phút | Báo không hợp lệ, có thể yêu cầu link mới | US4-3 |
| Q30 | "Tiếp tục với Google", tài khoản Google mới; đăng xuất, lặp lại | Tạo Member đã xác minh; lần 2 đúng tài khoản đó | US5-1, FR-018 |
| Q31 | Google có email trùng tài khoản đã xác minh | Vào tài khoản cũ, giữ dữ liệu, giữ vai trò | US5-2, FR-019 |
| Q32 | Tạo tài khoản chưa xác minh cùng email Google, thêm dị ứng và meal plan; đăng nhập Google | Tài khoản sạch, tên theo Google, mật khẩu cũ không còn dùng được, phiên cũ hết | US5-2, FR-019 |
| Q33 | Hủy ở màn Google; để client secret sai | Về đăng nhập với thông báo trung tính; không tạo tài khoản; đăng nhập email vẫn hoạt động | US5-3, NFR-005 |
| Q34 | Đặt hash của email vào `blocked_emails`; đăng ký và đăng nhập Google bằng email đó | Bị từ chối, thông báo trung tính, không nêu lý do | FR-026, `OQ-010-05` |
| Q35 | DevTools 360 px; đi qua toàn bộ form bằng bàn phím, bật screen reader | Không cuộn ngang, tiếng Việt, mọi trường có nhãn, lỗi được đọc cạnh trường | NFR-006, NFR-007 |
| Q36 | `GET /admin/audit-logs` bằng Administrator; xóa tài khoản rồi xem lại entry | Có đăng nhập, khóa, reset, vai trò, đình chỉ; entry còn, không có email hay tên | FR-025, NFR-008 |
| Q37 | Kiểm tra `password_hash`, log ứng dụng, phản hồi API | Chỉ thấy BCrypt; không có mật khẩu; email người khác chỉ hiện với Administrator | NFR-001, NFR-009 |
| Q38 | Moderator đình chỉ Member 30 ngày; thử 31 ngày, cả bằng Administrator; thử đình chỉ Moderator khác, cấm, gỡ án; đình chỉ tài khoản đang bị cấm | Lần đầu có hiệu lực ở request kế tiếp, có audit; còn lại lần lượt 403 `SUSPENSION_LIMIT_EXCEEDED`, 403 `ACCESS_DENIED`, 409 `ACCOUNT_NOT_ACTIVE` | US3-6, FR-022, FR-022a |
| Q39 | Cấm một tài khoản chưa xác minh, đặt `created_at` lùi 8 ngày, chạy `UnverifiedAccountPurgeJob`; đăng ký lại cùng email | Tài khoản biến mất, hash vào `blocked_emails`, đăng ký bị từ chối với thông báo trung tính | FR-005, FR-026 |
| Q40 | Đình chỉ một tài khoản chưa xác minh, đặt `created_at` lùi 8 ngày, chạy `UnverifiedAccountPurgeJob`; đặt `ends_at` về quá khứ, chạy `SuspensionExpiryJob`, rồi chạy lại `UnverifiedAccountPurgeJob` | Lần đầu tài khoản còn nguyên; sau khi hết đình chỉ thì bị xóa, `blocked_emails` không đổi | FR-005 |
| Q41 | Đang đăng nhập, gửi sai mật khẩu ở `POST /auth/reauthenticate` 5 lần; đăng nhập ở trình duyệt khác; đợi 15 phút | Lần sau 429 `PASSWORD_CHECK_LIMITED` có thời gian thử lại; đăng nhập vẫn được; hết 15 phút thử lại được | FR-022b |
| Q42 | *(Cần Google client thật, T178 và research mục 40)* Tài khoản chỉ Google xác thực lại: thành công; hủy ở Google; chọn tài khoản Google khác | Thành công: thao tác cần xác thực lại chạy được trong 5 phút. Hủy hoặc sai tài khoản: về trang đang dùng với `?reauth=cancelled`, vẫn đăng nhập, không tạo phiên mới | FR-022a, FR-022b |
| Q43 | Tạo tài khoản chưa xác minh cùng email Google, đình chỉ rồi cấm nó bằng Administrator; mỗi lần đăng nhập Google | Về màn trạng thái tài khoản có lý do, ngày kết thúc (đình chỉ) hoặc link đóng tài khoản (cấm); URL không còn fragment; tài khoản, án và dữ liệu không đổi, không có audit `ACCOUNT_RESET` | FR-019, FR-013 |
| Q44 | (a) Mở `/meal-planner`, `/shopping-list` khi có mạng, rồi DevTools → Offline ở tab đang mở. (b) `docker compose stop app`, tải lại hai trang (nginx vẫn phục vụ trang, API trả 502), rồi `docker compose start app`. (c) Xóa phiên trong DB rồi bật lại mạng | (a), (b) Không về `/login`; 030 hiện thông báo offline; phiên được kiểm tra lại khi có mạng hoặc khi backend chạy lại, không cần tải lại trang. (c) `/meal-planner` hiện hộp thoại "Đăng nhập lại", không rời trang; `/shopping-list` ở lại với banner. Tải lại trang khi mất mạng thật không thuộc 010 (research mục 41) | Edge Cases, 030 FR-028, 040 FR-020 |
| Q45 | Administrator gỡ đình chỉ và gỡ cấm hai tài khoản; Moderator thử gỡ | Hai tài khoản `ACTIVE`, có email, có audit `ACCOUNT_REINSTATED`; Moderator 403 `ACCESS_DENIED` | FR-022, FR-024a, 100 US3-8 |
| Q46 | Dùng hết 3 email xác minh trong 15 phút của một địa chỉ, xóa tài khoản đó bằng SQL, đăng ký lại cùng email | Tạo được tài khoản, không có email mới, màn kiểm tra email báo thời gian chờ; gửi lại được sau thời gian chờ | FR-002, FR-017 |

## 5. Kiểm tra API nhanh (curl)

```bash
B=localhost:8080/api/v1
curl -i -X POST $B/auth/login -H 'Content-Type: application/json' -c cookies.txt \
     -d '{"email":"member@wikicook.test","password":"Banhmi2026","rememberMe":false}'  # 200, Set-Cookie: wc_rt
TOKEN=...                                                # accessToken từ phản hồi trên
curl -H "Authorization: Bearer $TOKEN" $B/auth/me        # 200
curl $B/auth/me                                          # 401 UNAUTHENTICATED
curl -X POST -b cookies.txt -c cookies.txt -H 'X-WikiCook-Client: web' $B/auth/refresh   # 200, cookie mới
curl -X POST -b cookies.txt $B/auth/refresh              # 403 INVALID_CLIENT
curl -X POST -b cookies.txt -H 'X-WikiCook-Client: web' $B/auth/logout                   # 204
curl -H "Authorization: Bearer $TOKEN" $B/auth/me        # 401, JWT còn hạn nhưng phiên đã thu hồi
curl -H "X-User-Id: 1" $B/shopping-lists/active          # 401, header không còn tác dụng
curl -H "Authorization: Bearer $TOKEN" -X PATCH -H 'Content-Type: application/json' \
     -d '{"role":"ADMINISTRATOR"}' $B/admin/users/2/role # 403 ACCESS_DENIED với Member
```

## 6. Hiệu năng (trước Build v0.8)

Đo BCrypt cost 12 trên máy chạy thật trước. Chạy backend với `WIKICOOK_AUTH_SIGN_IN_IP_LIMIT=1000000`,
vì k6 gửi mọi request từ một IP và mặc định 100 lần/15 phút sẽ trả 429 sau khoảng 20 giây, rồi chạy
`k6 run src/backend/perf/auth-sign-in.k6.js` (kịch bản ở research R14, ngưỡng 429 = 0): đăng nhập 5 lần/giây với 100 VU trong 10 phút, p95 < 2 s (NFR-004); API có
bearer và refresh p95 < 500 ms ở 20 VU trong 5 phút, và ở 100 VU trong 10 phút trước `v1.0`.
