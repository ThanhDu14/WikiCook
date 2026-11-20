# BÁO CÁO NGHIỆM THU KIỂM THỬ SPEC KIT & CHẤT LƯỢNG MÃ NGUỒN (SCRUM-79)
## Phần E: System Verification, Quality Gates & Demo Evidence

| Thông tin | Chi tiết |
|---|---|
| **Dự án** | WikiCook — Nền tảng Chia sẻ Công thức Nấu ăn Thông minh (Nhóm 05) |
| **Giai đoạn** | Sprint 04 / Đợt nộp bài tập lớn số 4 (PA4) |
| **Nhiệm vụ Jira** | `SCRUM-79` — `[Testing-Demo]` Verify Spec Kit Tests & Record YouTube Video Demo for Part E |
| **Người thực hiện** | **Trần Nguyễn Công Chung** (`@itzchugnn` - MSSV: 24120272) — QA Lead, Compliance Auditor & Member E |
| **Người kiểm tra** | **Lê Quốc Hưng** (`@LqHung06`) — Scrum Master & PM *(Chờ ký duyệt sau khi hoàn thành video demo)* |
| **Phạm vi kiểm thử** | Toàn bộ 2 nhóm chức năng trọng yếu: **Nhóm 1 (`010 + 011`)** và **Nhóm 2 (`070`)** |
| **Ngày lập báo cáo** | 2026-11-19 (Cập nhật đối chiếu: 2026-11-20) |
| **Kết quả tổng quan** | **654 Unit Tests Backend (BUILD SUCCESS)**, **918 Component Tests Frontend (120 files pass 100%)**. Integration Tests chạy qua `mvn verify` (Testcontainers). Video demo đang chuẩn bị thực hiện. |

---

## 1. Mục tiêu & Yêu cầu Bàn giao (Scope & Deliverables)

Căn cứ theo yêu cầu của nhiệm vụ Jira `SCRUM-79` và Tiêu chí Đánh giá Phần E:
1. **Yêu cầu a (Kiểm chứng Test Spec Kit — ĐÃ HOÀN THÀNH)**: Thực thi kiểm chứng toàn bộ bộ test được sinh qua Spec Kit cho cả 2 nhóm chức năng (`010+011` và `070`) trên cả Backend (`mvn test` / `mvn verify`) và Frontend (`npm test`), thu thập số liệu chính xác từ báo cáo Surefire/Vitest và lưu bằng chứng kết quả kiểm thử.
2. **Yêu cầu b (Video Demo End-to-End — ĐANG TIẾN HÀNH)**: Xây dựng kịch bản phân cảnh (Storyboard) chuẩn hóa khớp 100% với REST API và CSDL thật, tiến hành quay Video Demo chi tiết luồng hoạt động end-to-end (`UI -> REST API -> PostgreSQL Database`) của cả 2 nhóm chức năng.
3. **Yêu cầu c (Xuất bản YouTube — CHỜ GHI HÌNH)**: Đăng tải video lên YouTube (chế độ Unlisted/Public) và tích hợp đường link vào `README.md`, `sprint-04-report.md` và tài liệu nộp bài.

---

## 2. Kết quả Kiểm thử Tự động Spec Kit (Yêu cầu a)

### 2.1. Kiểm thử Backend (Spring Boot 3.5.16 & JUnit 5)
- **Lệnh thực thi**: `mvn test -f src/backend/pom.xml` (chạy tầng Unit Tests qua Maven Surefire)
- **Thời gian hoàn thành**: 25.943 giây
- **Trạng thái**: `BUILD SUCCESS` (0 Failure, 0 Error, 0 Skipped)
- **Tổng số ca kiểm thử**: **654 / 654 unit tests pass 100%**
- **Phạm vi Integration Tests (`mvn verify`)**: Khi thực thi lệnh `mvn verify`, Maven Failsafe kích hoạt thêm toàn bộ các lớp Integration Test (`*IT`: khoảng 30 lớp trong `auth`, 22 lớp trong `profile`, 5 lớp trong `review`) chạy trên môi trường container PostgreSQL thật qua Testcontainers, nâng tổng số ca kiểm thử được kiểm chứng lên khoảng 1.410 tests theo đúng quy định của Hiến pháp dự án.

#### Bảng phân rã chi tiết kết quả Backend Unit Test theo Module (trích xuất trực tiếp từ Surefire Reports):

| Nhóm chức năng | Module / Package Java | Số ca kiểm thử | Kết quả | Các thành phần trọng yếu được kiểm chứng |
|---|---|:---:|:---:|---|
| **Nhóm 1** | `com.wikicook.auth` (`010`) | 237 | **237 passed** | Đăng ký tài khoản, JWT Session, mã hóa BCrypt, kiểm tra mật khẩu, giới hạn tần suất gửi email khôi phục mật khẩu, xử lý ngoại lệ RFC 7807 `ProblemDetail`. |
| **Nhóm 1** | `com.wikicook.profile` (`011`) | 82 | **82 passed** | Hồ sơ ăn uống (`DietaryProfileServiceTest`), danh mục dị ứng (`user_allergens`), nguyên liệu loại trừ, xử lý ảnh đại diện avatar bóc tách metadata. |
| **Nhóm 2** | `com.wikicook.review` (`070`) | 53 | **53 passed** | Cơ chế tính điểm rating trung bình không tăng `version`, bóc tách EXIF/GPS TwelveMonkeys, Pessimistic Locking chống race condition, cascade delete, xử lý báo cáo kiểm duyệt. |
| **Phụ trợ E2E** | `com.wikicook.aiplanner` | 110 | **110 passed** | Sinh thực đơn tự động, kiểm tra ràng buộc khẩu phần và dị ứng, thuật toán dự phòng khi LLM timeout. |
| **Phụ trợ E2E** | `com.wikicook.mealplan` | 50 | **50 passed** | Lập lịch ăn uống tuần, chuyển đổi món ăn giữa các ngày. |
| **Phụ trợ E2E** | `com.wikicook.shoppinglist` | 46 | **46 passed** | Tự động tổng hợp danh sách nguyên liệu mua sắm từ thực đơn tuần. |
| **Phụ trợ E2E** | `com.wikicook.search` | 38 | **38 passed** | Tìm kiếm món ăn, bộ lọc nâng cao, phân trang và sắp xếp. |
| **Phụ trợ E2E** | `com.wikicook.common` | 23 | **23 passed** | Xử lý lỗi toàn cục RFC 7807, cấu hình bảo mật chung và tiện ích dị ứng. |
| **Phụ trợ E2E** | `com.wikicook.devstub` | 15 | **15 passed** | Bộ giả lập phục vụ môi trường kiểm thử cục bộ. |
| **TỔNG CỘNG** | **Toàn bộ Backend Unit Tests** | **654** | **654 passed** | **Toàn bộ Unit Test vượt qua rào cản chất lượng (BUILD SUCCESS)** |

---

### 2.2. Kiểm thử Frontend (React 18, Vite & Vitest)
- **Lệnh thực thi**: `npm test -- --run` tại thư mục `src/frontend`
- **Thời gian hoàn thành**: 26.27 giây
- **Trạng thái**: **120 / 120 Test Files passed (100%)**
- **Tổng số ca kiểm thử**: **918 / 918 component tests passed (100%)**

#### Bảng phân rã chi tiết kết quả Frontend theo Feature:

| Nhóm chức năng | Thư mục Feature | Số Test Files | Số Tests Pass | Các thành phần UI được kiểm chứng |
|---|---|:---:|:---:|---|
| **Nhóm 1** | `src/features/auth` (`010`) | 12 files | 84 tests | Form đăng nhập, form đăng ký, quên mật khẩu, đặt lại mật khẩu (`ResetPasswordPage`), xác thực email, quản lý phiên và bảo vệ route. *(Trang đổi mật khẩu hiện chưa triển khai theo phạm vi MVP)*. |
| **Nhóm 1** | `src/features/profile` (`011`) | 7 files | 62 tests | Giao diện chọn dị ứng, nguyên liệu loại trừ (`ExcludedIngredientPicker`), khẩu phần ăn uống (`DietaryProfilePage`), chọn avatar (`AvatarPicker`), trang tài khoản cá nhân và trang hồ sơ công khai. |
| **Nhóm 2** | `src/features/reviews` (`070`) | 11 files | 78 tests | Component `ReviewList`, form chấm sao `ReviewForm`, modal phóng to ảnh `CooksnapLightbox`, hộp thoại báo cáo `ReportDialog`, nút bình chọn hữu ích. |
| **Phụ trợ E2E** | `ai-planner`, `shopping-list`, `search`, `shared` | 90 files | 694 tests | Toàn bộ giao diện tạo thực đơn AI, giỏ đi chợ, tìm kiếm và các component dùng chung. |
| **TỔNG CỘNG** | **Toàn bộ Frontend** | **120 files** | **918 passed** | **100% Pass Rate trên toàn bộ các bộ test component** |

> [!NOTE]
> **Ghi nhận về kiểm thử End-to-End (Playwright):** Trong bộ test E2E trình duyệt bằng Playwright, hiện có 1 ca kiểm thử thuộc module `meal-planner` (nhập nguyên liệu vào giỏ đi chợ) đang ở trạng thái fail trên nhánh `main` do thay đổi App Shell route trước đó. Lỗi này thuộc phạm vi theo dõi riêng của nhóm bảo trì App Shell, không ảnh hưởng đến độ tin cậy của các luồng kiểm thử thuộc Nhóm 1 (`010+011`) và Nhóm 2 (`070`).

---

### 2.3. Báo cáo Độ phủ Mã nguồn JaCoCo (Code Coverage Audit)
Căn cứ theo quy định của Hiến pháp WikiCook (`.specify/memory/constitution.md` - Principle II: Test-First): Tầng Service xử lý nghiệp vụ bắt buộc phải đạt độ phủ dòng tối thiểu $\ge 70\%$.

- **Tệp báo cáo**: `src/backend/wikicook-app/target/site/jacoco/jacoco.csv` (và `index.html`)
- **Độ phủ dòng thực tế tầng `com.wikicook.review.service`**:
  - Khi chạy Unit Test (`mvn test`): **353 / 464 dòng (76.08%)** $\to$ **Đạt chuẩn** (vượt ngưỡng tối thiểu).
  - Khi chạy kèm Integration Test (`mvn verify`): **358 / 464 dòng (77.16%)** $\to$ **Đạt chuẩn**.
- **Độ phủ dòng thực tế tầng `com.wikicook.auth.service`**: Đạt khoảng **97%** dòng được bao phủ $\to$ **Đạt chuẩn**.
- **Độ phủ dòng thực tế tầng `com.wikicook.profile.service`**: Đạt khoảng **93%** dòng được bao phủ $\to$ **Đạt chuẩn**.

---

## 3. Kịch bản Phân cảnh Video Demo End-to-End (Yêu cầu b)

Video demo được thiết kế theo cấu trúc song song: **Màn hình Trình duyệt (Frontend UI) $\leftrightarrow$ Network DevTools (REST API) $\leftrightarrow$ IntelliJ Database / psql (PostgreSQL 16 Database)** để chứng minh tính toàn vẹn end-to-end qua dữ liệu thật.

- **Thời lượng dự kiến**: 4 phút 30 giây
- **Chất lượng**: 1080p Full HD, giọng thuyết minh tiếng Việt chuẩn, rõ ràng.

### Bảng Storyboard chi tiết:

| Thời lượng | Phân cảnh | Thao tác trên Giao diện (UI) | Quan sát Network (REST API) | Kiểm chứng Cơ sở dữ liệu (PostgreSQL) | Lời thuyết minh trọng tâm |
|---|---|---|---|---|---|
| **00:00 – 00:30** | **Mở đầu** | Mở trang chủ WikiCook `http://localhost:3000`, hiển thị danh sách container Docker (`docker compose ps`). | — | — | Giới thiệu họ tên (Trần Nguyễn Công Chung), vai trò QA Lead, đại diện Nhóm 05 demo nghiệm thu Phần E. |
| **00:30 – 01:15** | **Nhóm 1: Đăng ký & Đăng nhập** | Điền form đăng ký tài khoản `chung_qa@wikicook.local`, mở Mailpit (`localhost:8025`) bấm link xác thực email, sau đó đăng nhập tại `/login`. | `POST /api/v1/auth/register` (201 Created)<br>`GET /api/v1/auth/verify?token=...`<br>`POST /api/v1/auth/login` (200 OK trả về JWT) | `SELECT id, email, role, status FROM users WHERE email = 'chung_qa@wikicook.local';` | Chứng minh tài khoản mới được khởi tạo với trạng thái ACTIVE và nhận JWT token bảo mật. |
| **01:15 – 02:15** | **Nhóm 1: Hồ sơ ăn uống & Dị ứng** | Vào trang Hồ sơ cá nhân (`/profile/edit`) cập nhật Bio. Vào mục Hồ sơ ăn uống (`/profile/dietary`): chọn dị ứng **Đậu phộng**, **Giáp xác (tôm, cua)**, chế độ ăn **Ăn chay (Vegetarian)**, khẩu phần **4 người**. Bấm Lưu. | `PUT /api/v1/me/dietary-profile` (200 OK) | `SELECT id, email, diet_type, servings FROM users WHERE id = ...;`<br>`SELECT u.email, a.code, a.name_vi FROM user_allergens ua JOIN allergens a ON ua.allergen_id = a.id JOIN users u ON ua.user_id = u.id WHERE u.id = ...;` | Thấy rõ thông tin dị ứng ghi vào bảng `user_allergens`, khẩu phần và chế độ ăn ghi vào bảng `users`; dữ liệu đồng bộ chính xác xuống CSDL. |
| **02:15 – 03:15** | **Nhóm 2: Đánh giá & Tải ảnh Cooksnap** | Mở công thức đã xuất bản (ví dụ ID 1 hoặc ID 12). Chọn 5 sao, nhập nhận xét, mẹo thay thế (`substitution_tip`), đính kèm ảnh Cooksnap. Bấm Gửi. | `POST /api/v1/recipes/1/reviews` (Multipart/form-data, 201 Created) | `SELECT id, rating, comment FROM reviews WHERE recipe_id = 1;`<br>`SELECT storage_key FROM cooksnaps WHERE review_id = ...;`<br>`SELECT rating_avg, review_count, version FROM recipes WHERE id = 1;` | Điểm rating trên thanh tiêu đề nhảy số tức thì. Chứng minh ảnh lưu dạng WebP sạch, và cột `recipes.version` **không bị tăng**, tránh xung đột phiên bản với tác giả. |
| **03:15 – 03:45** | **Bảo mật EXIF / GPS** | Kiểm tra siêu dữ liệu hình ảnh Cooksnap tải lên hệ thống. | `GET /api/v1/media/cooksnaps/{storageKey}` | — | Chứng minh TwelveMonkeys đã bóc tách sạch 100% tọa độ GPS/EXIF, bảo vệ quyền riêng tư người dùng theo Hiến pháp Principle IV. |
| **03:45 – 04:15** | **Nhóm 2: Lightbox, Sửa & Xóa review** | Tích chọn bộ lọc "Có ảnh", bấm vào ảnh mở Lightbox xem toàn màn hình. Sau đó bấm Sửa đánh giá (đổi sang 4 sao) và bấm Xóa đánh giá. | `PUT /api/v1/reviews/{id}`<br>`DELETE /api/v1/reviews/{id}` | `SELECT rating_avg, review_count FROM recipes WHERE id = 1;`<br>`SELECT * FROM reviews WHERE id = ...;` | Điểm rating của công thức tự động tính toán lại; bản ghi review và cooksnap bị cascade delete sạch sẽ, giải phóng partial unique index. |
| **04:15 – 04:30** | **Kết luận & Bằng chứng Test** | Trình chiếu terminal chạy `BUILD SUCCESS` 654 test Backend và 918 test Frontend. | — | — | Tuyên bố hoàn thành toàn bộ kiểm chứng tự động và kịch bản end-to-end cho 2 nhóm chức năng. |

---

## 4. Đường dẫn Video Demo YouTube (Yêu cầu c)

- **Trạng thái xuất bản**: Đang chuẩn bị ghi hình theo Storyboard Mục 3. Link video sẽ được cập nhật đồng bộ ngay sau khi hoàn tất.
- **Link Video YouTube**: `[ĐIỀN LINK YOUTUBE TẠI ĐÂY - Ví dụ: https://youtu.be/xxxxxxxxxxx]` *(Chế độ Unlisted/Public)*
- **Độ phân giải**: 1080p Full HD (Thuyết minh tiếng Việt).
- **Vị trí tích hợp đồng bộ sau khi ghi hình**:
  1. `README.md` (Mục Demo Videos)
  2. `docs/management/reports/sprint-04/sprint-04-report.md` (Bảng deliverables và Section 1)
  3. `docs/test/scrum-79-spec-kit-test-report.md`

---

## 5. Kết luận & Biên bản Nghiệm thu (QA Sign-Off)

Căn cứ trên các bằng chứng kiểm thử thực tế tại đợt nghiệm thu Sprint 04:
- [x] **Yêu cầu a — Kiểm thử Spec Kit Tự động**: Đạt tỷ lệ 654/654 Backend unit tests (`mvn test`), 918/918 Frontend component tests (`npm test`) thành công 100%. Sẵn sàng thực thi ~1.410 tests qua `mvn verify` (Testcontainers).
- [x] **Yêu cầu a — Độ phủ JaCoCo**: Tầng dịch vụ `review.service` đạt 76.08% (unit test) / 77.16% (integration test), `auth.service` đạt ~97%, `profile.service` đạt ~93% (tuân thủ nghiêm ngặt Hiến pháp Principle II $\ge 70\%$).
- [x] **Yêu cầu a — Bảo mật & Quyền riêng tư**: Bóc tách 100% metadata GPS/EXIF trên hình ảnh tải lên (tuân thủ Hiến pháp Principle IV).
- [ ] **Yêu cầu b — Video Demo End-to-End**: Kịch bản Storyboard đã được chuẩn hóa khớp mã nguồn; đang tiến hành ghi hình thực tế.
- [ ] **Yêu cầu c — Xuất bản YouTube**: Chờ hoàn thiện video để đính kèm link công khai.

*Xác nhận của QA Lead:*  
**Trần Nguyễn Công Chung** (`@itzchugnn` - MSSV: 24120272) — QA Lead & Compliance Auditor, WikiCook Group 05

*Xác nhận của Scrum Master / PM:*  
**Lê Quốc Hưng** (`@LqHung06`) — *(Ký duyệt sau khi nghiệm thu video demo hoàn tất)*
