# Nhật ký sử dụng AI – Trần Nguyễn Công Chung (itzchugnn)

File này ghi lại toàn bộ lịch sử sử dụng công cụ AI của **Trần Nguyễn Công Chung** (mã **E**, QA Lead, GitHub: `@itzchugnn`) trong suốt quá trình phát triển dự án WikiCook, tuân thủ nghiêm ngặt quy chế học thuật của môn học tại `AI Usage Guidelines.pdf`. Mỗi lần sử dụng AI là một mục mới, mục mới nhất ở cuối.

| # | Ngày | Nội dung thực hiện | Module / Spec / Tài liệu |
|---|---|---|---|
| 1 | 2026-10-19 | Tìm hiểu cấu trúc repository WikiCook, tech stack và xác định phạm vi công việc thành viên E | Chung – phân tích repo |
| 2 | 2026-10-19 | Xác định chi tiết 2 spec (070, 100), các điểm giao thoa CSDL và thiết lập quy tắc soạn thảo | `docs/management/w1-spec-design-assignment.md`, `.agents/rules/` |
| 3 | 2026-10-19 | Tạo đặc tả chức năng (Feature Specification) cho Đánh giá cộng đồng & Cooksnap (`070`) bằng `/speckit-specify` | `specs/070-review-cooksnap/` |
| 4 | 2026-10-19 | Làm rõ yêu cầu kỹ thuật và hành vi biên cho spec 070 (Interactive Clarification) bằng `/speckit-clarify` | `specs/070-review-cooksnap/` |
| 5 | 2026-10-19 | Tạo đặc tả chức năng (Feature Specification) cho Dashboard Quản trị & Kiểm duyệt nội dung (`100`) bằng `/speckit-specify` | `specs/100-admin-moderation/` |
| 6 | 2026-10-19 | Làm rõ yêu cầu kỹ thuật và hành vi biên cho spec 100 (Interactive Clarification) bằng `/speckit-clarify` | `specs/100-admin-moderation/` |

---

## Mục 1: Khởi tạo & Tìm hiểu cấu trúc repository WikiCook

| Trường | Nội dung |
|---|---|
| **Ngày** | 2026-10-19 |
| **Người thực hiện** | Trần Nguyễn Công Chung (`itzchugnn`) |
| **Tài liệu liên quan** | Toàn bộ repo WikiCook: `README.md`, `docs/`, `specs/`, `src/`, `.specify/` |
| **Công cụ AI** | Google Antigravity (Gemini 3.8 Flash) |
| **Mục đích** | Hiểu tổng quan cấu trúc dự án, tech stack, vai trò thành viên, và tiến độ hiện tại của repository để xác định khối lượng công việc thành viên E cần hoàn thành trong tuần. |
| **Nhánh Git** | `Chung` |

### Prompt đã dùng

```text
Hãy duyệt qua toàn bộ repository WikiCook này giúp tôi. Tôi muốn nắm tổng quan về cấu trúc thư mục, tech stack (Backend, Frontend, Database), các tài liệu quan trọng trong thư mục docs/ và danh sách các spec hiện có để bắt đầu công việc của mình.
```

### Tóm tắt kết quả & Hiệu chỉnh của sinh viên:
- AI quét toàn bộ cấu trúc thư mục, phân tích tài liệu kiến trúc, bản đề xuất dự án (`project-proposal.md`) và constitution.
- AI tổng hợp chuẩn xác tech stack: Spring Boot 3 / Java 21 (Backend), React 18 + Vite (Frontend), PostgreSQL 16 với pgvector, Redis Cache, và Flyway quản lý migration.
- Sinh viên kiểm chứng cấu trúc các nhánh, nhận diện danh sách 7 spec đã có trong `specs/` và xác nhận vai trò QA Lead của mình trong team contract.

---

## Mục 2: Phân tích phân công thành viên E & Thiết lập quy tắc soạn thảo

| Trường | Nội dung |
|---|---|
| **Ngày** | 2026-10-19 |
| **Người thực hiện** | Trần Nguyễn Công Chung (`itzchugnn`) |
| **Tài liệu liên quan** | `docs/management/w1-spec-design-assignment.md`, `.agents/rules/spec-authoring-rule.md`, `docs/ai_log/` |
| **Công cụ AI** | Google Antigravity (Gemini 3.8 Flash) |
| **Mục đích** | Rà soát phân công chi tiết cho thành viên E: 2 spec bắt buộc (`070`, `100`), 7 bảng CSDL phụ trách, các điểm giao chéo giữa các thành viên, quy chuẩn viết AI log (`SCRUM-41`), và tạo file rule cục bộ để AI tuân thủ. |
| **Nhánh Git** | `Chung` |

### Quá trình phân tích & Đánh giá:
- Sinh viên yêu cầu AI đọc `w1-spec-design-assignment.md` và kiểm tra nghĩa vụ viết AI log theo `project-plan.md` (nhiệm vụ `SCRUM-41`, rủi ro `RSK-04`).
- Thống nhất các nguyên tắc kỹ thuật trong `.agents/rules/spec-authoring-rule.md`: không rò rỉ chi tiết cài đặt code vào spec, dùng tiếng Anh chuyên ngành chuẩn mực, tuân thủ cấu trúc thư mục cấp sẵn (`NN0`), và ghi nhật ký AI đầy đủ.

---

## Mục 3: Viết spec 070 (Community Reviews, Cooksnaps & Interactive Ratings) bằng `/speckit-specify`

| Trường | Nội dung |
|---|---|
| **Ngày** | 2026-10-19 |
| **Người thực hiện** | Trần Nguyễn Công Chung (`itzchugnn`) |
| **Spec liên quan** | 070 – Đánh giá cộng đồng, Ảnh thực hành Cooksnap & Xếp hạng tương tác (`specs/070-review-cooksnap/`) |
| **Công cụ AI** | Google Antigravity (Claude Opus 4.6 Thinking), lệnh Spec Kit `/speckit-specify` |
| **Mục đích** | Soạn thảo bản đặc tả yêu cầu chức năng (`spec.md`) và checklist chất lượng (`checklists/requirements.md`) cho Module 3.7 theo đúng template Spec Kit. |
| **Nhánh Git** | `Chung` (chuẩn bị cho branch `feature/070-review-cooksnap`) |

### Quá trình phân tích & Đánh giá 6 bài toán nghiệp vụ trước khi sinh spec:
1. **Toàn vẹn điểm đánh giá & Chống nghẽn ghi (`rating_avg`):** Điểm trung bình và tổng số review phải được lưu denormalize trên bảng `recipes` để trang tìm kiếm/lọc (spec `020`) không phải chạy truy vấn aggregate tốn kém. Việc cập nhật điểm phải được thiết kế dạng transaction atomic hoặc trigger để chống race condition khi nhiều người cùng đánh giá.
2. **Quyền riêng tư hình ảnh (EXIF Metadata Stripping):** Ảnh người dùng chụp trong bếp có thể chứa tọa độ GPS và thông tin thiết bị nhạy cảm. Hệ thống bắt buộc phải loại bỏ toàn bộ dữ liệu EXIF ở tầng xử lý trước khi lưu trữ.
3. **Mỗi người dùng một đánh giá (Idempotency):** Ràng buộc UNIQUE trên cặp `(recipe_id, user_id)` để đảm bảo mỗi người chỉ đánh giá 1 lần, muốn đổi ý thì sửa bài cũ, tránh tình trạng spam vote dồn dập.
4. **Phân cấp quyền hạn (RBAC):** Khách vãng lai (Guest) được đọc review và xem ảnh Cooksnap để tạo sự tin tưởng, nhưng bắt buộc đăng nhập (Member trở lên) mới được đánh giá, upload ảnh hoặc báo cáo vi phạm.
5. **Độc lập tính năng nhưng gắn kết luồng người dùng:** Luồng gợi ý đánh giá sau khi nấu phụ thuộc vào sự kiện hoàn thành của spec `050-cooking-mode`, nhưng tính năng review vẫn hoạt động độc lập 100% trực tiếp trên trang chi tiết công thức.
6. **Xử lý xóa tầng (Cascading vs Soft Delete):** Xóa đánh giá sẽ xóa theo ảnh Cooksnap liên quan, nhưng nếu công thức bị tác giả lưu trữ (`ARCHIVED`), các đánh giá cũ vẫn bảo toàn trong CSDL cho mục đích thống kê của Admin.

### Prompt đã sử dụng:

```text
/speckit-specify SPECIFY_FEATURE_DIRECTORY=specs/070-review-cooksnap
Tạo đặc tả yêu cầu chức năng (Feature Specification) cho tính năng "070-review-cooksnap: Community Reviews, Cooksnaps & Interactive Ratings (Đánh giá cộng đồng, Ảnh thực hành Cooksnap & Xếp hạng tương tác)" theo đúng cấu trúc spec-template.md của dự án WikiCook.

Mô tả nghiệp vụ chi tiết:
1. Bối cảnh & Mục tiêu:
Người dùng cần tham khảo đánh giá thực tế và xem ảnh món ăn thực tế đã nấu (Cooksnap) từ những người khác trước khi quyết định nấu. Tác giả công thức cần nhận phản hồi để hoàn thiện món ăn.
2. Phân cấp tính năng & Ưu tiên:
- User Story 1 (P1 - MVP): Đánh giá sao (1-5 sao) kèm nhận xét văn bản và tải lên ảnh Cooksnap (tối đa 3 ảnh, <= 5MB/ảnh, strip EXIF metadata).
- User Story 2 (P2): Chỉnh sửa hoặc xóa đánh giá của chính mình (chỉ tác giả đánh giá hoặc Moderator/Admin mới được xóa).
- User Story 3 (P2): Duyệt, sắp xếp và lọc đánh giá (theo Mới nhất, Đánh giá cao nhất, Đánh giá thấp nhất, Chỉ xem có ảnh Cooksnap).
- User Story 4 (P2): Báo cáo vi phạm nội dung không phù hợp (chọn lý do: Spam, Quấy rối, Nội dung không liên quan, Kèm ghi chú). Tạo bản ghi trong bảng reports để Admin xử lý ở spec 100.
- User Story 5 (P3): Gợi ý đánh giá sau khi hoàn thành nấu ăn từ Chế độ nấu ăn (spec 050).
- User Story 6 (P3): Bầu chọn đánh giá hữu ích ("Thích/Helpful") với cơ chế toggle.
3. Các trường hợp biên (Edge Cases):
- Race condition khi nhiều người đồng thời review ảnh hưởng rating_avg.
- Mất kết nối mạng khi đang tải ảnh Cooksnap.
- Tải lên file độc hại hoặc vượt quá kích thước.
- Đánh giá trên công thức đã bị xóa/lưu trữ (ARCHIVED).
- Tài khoản người dùng bị xóa: giữ lại đánh giá nhưng hiển thị tên tác giả là "Người dùng ẩn danh".
- Chưa có đánh giá nào (Empty State).
4. Thực thể dữ liệu:
- reviews: id, recipe_id, user_id, rating (1-5), comment, created_at, updated_at.
- cooksnaps: id, review_id, image_url, thumbnail_url, sort_order, created_at.
- reports: id, reporter_id, target_type ('REVIEW', 'RECIPE', 'COMMENT'), target_id, reason, status ('PENDING', 'RESOLVED', 'DISMISSED'), created_at.
- Cập nhật trên recipes: thêm rating_avg NUMERIC(2,1) và review_count INTEGER.
5. Tiêu chí thành công:
- SC-001: Điểm trung bình và số lượng hiển thị tức thì sau khi gửi (< 1.5s).
- SC-002: Tỷ lệ tải ảnh thành công >= 95% trong điều kiện mạng ổn định.
- SC-003: 100% ảnh upload được xóa sạch EXIF geolocation metadata.
- SC-004: Ngăn chặn 100% trường hợp 1 user tạo nhiều hơn 1 review cho 1 recipe.
- SC-005: Xử lý giao diện mượt mà với công thức có hơn 1,000 lượt đánh giá.
```

### Kết quả do AI sinh ra & Kiểm chứng của sinh viên:
- File đặc tả đã tạo: `specs/070-review-cooksnap/spec.md`.
- File checklist chất lượng: `specs/070-review-cooksnap/checklists/requirements.md`.
- Sinh viên kiểm tra:
  + Cấu trúc tiếng Anh chuyên ngành chuẩn mực, bao phủ 6 User Stories, 19 FR, 6 NFR.
  + Toàn bộ kịch bản chấp nhận viết theo chuẩn Gherkin Given-When-Then.
  + Ma trận phân quyền RBAC 5 vai trò × 8 hành vi rành mạch.

---

## Mục 4: Làm rõ yêu cầu kỹ thuật và hành vi biên cho spec 070 bằng `/speckit-clarify`

| Trường | Nội dung |
|---|---|
| **Ngày** | 2026-10-19 |
| **Người thực hiện** | Trần Nguyễn Công Chung (`itzchugnn`) |
| **Spec liên quan** | 070 – Community Reviews, Cooksnaps & Interactive Ratings (`specs/070-review-cooksnap/`) |
| **Công cụ AI** | Google Antigravity (Gemini 3.8 Flash), lệnh Spec Kit `/speckit-clarify` |
| **Mục đích** | Rà soát toàn diện các điểm mập mờ trong tài liệu đặc tả, tiến hành vòng lặp hỏi đáp tương tác để chuẩn hóa hành vi biên trước khi chuyển sang bước thiết kế CSDL và kiến trúc. |
| **Nhánh Git** | `Chung` (chuẩn bị cho branch `feature/070-review-cooksnap`) |

### Quá trình làm rõ & Quyết định kỹ thuật:
- **Câu hỏi 1 (Tần suất đánh giá của một người dùng):** Làm rõ việc một người dùng có thể gửi nhiều đánh giá cho cùng một món ăn hay không.
  + *Quyết định chọn:* Mỗi người dùng chỉ được viết **duy nhất một đánh giá** cho mỗi công thức. Muốn thay đổi điểm hoặc nhận xét thì dùng tính năng chỉnh sửa bài cũ. Ràng buộc `UNIQUE(recipe_id, user_id)`.
  + *Cập nhật vào Spec:* Bổ sung mục `## Clarifications`, cập nhật `FR-001` và `Edge Case 1`.
- **Câu hỏi 2 (Quy tắc xóa đánh giá kèm ảnh Cooksnap):** Khi người dùng xóa bài đánh giá, các ảnh Cooksnap đính kèm sẽ được xử lý ra sao?
  + *Quyết định chọn:* Xóa bài đánh giá sẽ **xóa toàn bộ** ảnh Cooksnap liên kết (`CASCADE` logic ở tầng ứng dụng/DB). Tệp trên cloud storage được gắn cờ xóa mềm (soft delete) và dọn sạch sau 30 ngày.
  + *Cập nhật vào Spec:* Bổ sung mục `## Clarifications`, cập nhật User Story 2 (Scenario 3) và `FR-005`.
- **Câu hỏi 3 (Quyền hạn của khách vãng lai - Guest):** Khách chưa đăng nhập có được xem và tương tác với đánh giá không?
  + *Quyết định chọn:* Khách vãng lai có quyền **xem 100%** đánh giá và ảnh Cooksnap công khai; tuy nhiên các nút "Viết đánh giá", "Tải ảnh", "Hữu ích", "Báo cáo" yêu cầu đăng nhập (điều hướng sang trang Login với redirect URL trở lại món ăn).
  + *Cập nhật vào Spec:* Bổ sung mục `## Clarifications`, hoàn thiện ma trận RBAC và `FR-007`.
- **Câu hỏi 4 (Định lượng upload ảnh Cooksnap):** Giới hạn số lượng và dung lượng tệp tải lên cho mỗi bài đánh giá.
  + *Quyết định chọn:* Tối đa **3 ảnh / bài đánh giá**, dung lượng mỗi tệp $\le 5\text{ MB}$, định dạng hỗ trợ JPEG, PNG, WebP. Tự động chuyển đổi và nén WebP thumbnail.
  + *Cập nhật vào Spec:* Bổ sung mục `## Clarifications`, cập nhật `FR-002`, `NFR-003`, `Edge Case 3`.
- **Câu hỏi 5 (Phương thức tính và lưu trữ `rating_avg`):** Cách tính toán điểm trung bình để tối ưu hiệu năng trang chủ và tìm kiếm.
  + *Quyết định chọn:* Lưu cứng hai cột `rating_avg NUMERIC(2,1)` và `review_count INTEGER` trên bảng `recipes`. Cập nhật bằng giao thức khóa hàng (`SELECT FOR UPDATE`) tại Service layer khi có biến động đánh giá. Phối hợp với Thành viên B (phụ trách spec `060-recipe-editor`).
  + *Cập nhật vào Spec:* Bổ sung mục `## Clarifications`, cập nhật `FR-014` và ghi chú thống nhất điểm giao với B.
- **Câu hỏi 6 (Số phận đánh giá khi công thức bị xóa/lưu trữ):** Xử lý đánh giá khi tác giả chuyển công thức sang `ARCHIVED`.
  + *Quyết định chọn:* Đánh giá được **giữ nguyên trong CSDL** (không xóa vật lý) nhưng ẩn khỏi giao diện công chúng vì trang chi tiết công thức `ARCHIVED` chỉ có tác giả và Admin truy cập được.
  + *Cập nhật vào Spec:* Bổ sung mục `## Clarifications`, cập nhật `Edge Case 4` và `FR-015`.

### Kết quả phiên làm rõ & Đánh giá chất lượng:
- **Số câu hỏi hoàn thành:** 6 câu hỏi kỹ thuật chuyên sâu.
- **File đặc tả đã cập nhật:** `specs/070-review-cooksnap/spec.md`.
- **Checklist chất lượng yêu cầu:** `specs/070-review-cooksnap/checklists/requirements.md` đạt 16/16 tiêu chí (100% PASSED).
- **Trạng thái:** Không còn điểm mù (`[NEEDS CLARIFICATION]`). Đạt chuẩn 100% sẵn sàng cho bước `/speckit-plan`.

---

## Mục 5: Viết spec 100 (Admin Dashboard & Content Moderation) bằng `/speckit-specify`

| Trường | Nội dung |
|---|---|
| **Ngày** | 2026-10-19 |
| **Người thực hiện** | Trần Nguyễn Công Chung (`itzchugnn`) |
| **Spec liên quan** | 100 – Bảng điều khiển Quản trị & Kiểm duyệt nội dung (`specs/100-admin-moderation/`) |
| **Công cụ AI** | Google Antigravity (Claude Opus 4.6 Thinking), lệnh Spec Kit `/speckit-specify` |
| **Mục đích** | Soạn thảo bản đặc tả yêu cầu chức năng (`spec.md`) và checklist chất lượng (`checklists/requirements.md`) cho Module 3.10 theo phân công. |
| **Nhánh Git** | `Chung` (chuẩn bị cho branch `feature/100-admin-moderation`) |

### Quá trình phân tích & Đánh giá 6 bài toán quản trị trước khi sinh spec:
1. **Quy tắc máy trạng thái công thức (Recipe State Machine):** Phối hợp chặt chẽ với spec `060` của B. Công thức chỉ có thể duyệt nếu đang ở `PENDING`. Moderator có 3 hành động: Phê duyệt (`PUBLISHED`), Từ chối (`REJECTED`), hoặc Yêu cầu chỉnh sửa (`REVISION_REQUESTED`).
2. **Nguyên tắc phân định quyền hạn Moderator vs Administrator:**
   - **Moderator (Kiểm duyệt viên):** Duyệt/từ chối công thức, xử lý báo cáo vi phạm, đình chỉ tài khoản tạm thời ($\le 30$ ngày). Tuyệt đối không được cấp quyền cấm vĩnh viễn hoặc thăng cấp tài khoản.
   - **Administrator (Quản trị viên tối cao):** Toàn quyền hệ thống, cấm vĩnh viễn (`BANNED`), thay đổi vai trò người dùng, quản lý danh mục (Taxonomy), ghim công thức nổi bật (Featured), và xem nhật ký kiểm toán (Audit Log).
3. **Chống tự kiểm duyệt (Self-Moderation Prevention):** Kiểm duyệt viên hoặc Admin không được phép tự duyệt công thức do chính mình đăng tải để tránh thiên vị hoặc xung đột lợi ích.
4. **Bảo vệ tính toàn vẹn của Audit Log (Append-Only):** Mọi hành động nhạy cảm (duyệt bài, cấm tài khoản, đổi quyền) đều phải ghi vết vào `audit_logs`. Bảng này thiết kế chỉ cho phép INSERT, nghiêm cấm UPDATE hoặc DELETE để đảm bảo tính pháp lý kiểm toán.
5. **Cơ chế sự kiện bất đồng bộ khi duyệt bài:** Khi công thức chuyển trạng thái `PUBLISHED`, hệ thống kích hoạt sự kiện `RECIPE_PUBLISHED` để tạo vector embedding (spec `031`) và tính toán dinh dưỡng (spec `090`). Sự kiện này phải là non-blocking: nếu dịch vụ AI gặp sự cố, bài viết vẫn được xuất bản thành công.
6. **Xử lý xung đột kiểm duyệt đồng thời (Concurrent Moderation Conflict):** Khi 2 kiểm duyệt viên cùng mở một báo cáo hoặc công thức, người duyệt trước sẽ khóa bản ghi; người thứ hai sẽ nhận thông báo kèm trạng thái cập nhật mới nhất thay vì ghi đè hành động.

### Prompt đã sử dụng:

```text
/speckit-specify SPECIFY_FEATURE_DIRECTORY=specs/100-admin-moderation
Tạo đặc tả yêu cầu chức năng (Feature Specification) cho tính năng "100-admin-moderation: Admin Dashboard & Content Moderation (Bảng điều khiển Quản trị & Kiểm duyệt nội dung)" theo đúng cấu trúc spec-template.md của dự án WikiCook.

Mô tả nghiệp vụ chi tiết:
1. Bối cảnh & Mục tiêu:
Cung cấp cổng thông tin quản trị tập trung cho Quản trị viên (Admin) và Kiểm duyệt viên (Moderator) để kiểm soát chất lượng nội dung công thức, xử lý khiếu nại báo cáo vi phạm, quản lý người dùng, phân loại danh mục, ghim món ăn tiêu biểu và theo dõi vết kiểm toán toàn diện.
2. Phân cấp tính năng & Ưu tiên:
- User Story 1 (P1 - MVP): Hàng đợi duyệt công thức (Recipe Moderation Queue) với bộ lọc, xem trước nội dung, và 3 nút hành động: Duyệt (PUBLISHED), Từ chối (REJECTED kèm lý do), Yêu cầu sửa (REVISION_REQUESTED kèm ghi chú).
- User Story 2 (P1 - MVP): Xử lý hàng đợi báo cáo vi phạm (Report Resolution Queue) cho công thức, đánh giá, bình luận. Hành động: Bỏ qua (DISMISSED) hoặc Xử lý vi phạm (RESOLVED: ẩn nội dung, cảnh cáo tác giả, đình chỉ người dùng).
- User Story 3 (P2): Quản lý người dùng & Đình chỉ tài khoản (User Management & Suspension). Tìm kiếm người dùng, xem lịch sử vi phạm, đình chỉ tạm thời (Moderator <= 30 ngày) hoặc cấm vĩnh viễn (Admin).
- User Story 4 (P2): Quản lý danh mục & Ẩm thực (Category & Cuisine Taxonomy Management - Admin only). CRUD danh mục món ăn và phong cách ẩm thực kèm kiểm tra ràng buộc không được xóa danh mục đang có công thức liên kết.
- User Story 5 (P2): Quản lý công thức nổi bật trên Trang chủ (Featured Recipes Curation - Admin only). Chọn và sắp xếp tối đa 10 công thức nổi bật cho slider trang chủ.
- User Story 6 (P3): Nhật ký kiểm toán & Thống kê tổng quan (Audit Trail & Platform Analytics). Bảng điều khiển KPI (tổng người dùng, công thức chờ duyệt, báo cáo tồn đọng) và bảng tra cứu Audit Log chỉ đọc.
3. Các trường hợp biên (Edge Cases):
- Tự duyệt bài của chính mình (Self-moderation prevention).
- Xung đột kiểm duyệt đồng thời (hai Moderator cùng thao tác một bài).
- Vi phạm máy trạng thái công thức.
- Xóa danh mục cha khi còn danh mục con hoặc công thức.
- Admin cố gắng tự hạ quyền của chính mình (Last Admin Protection).
- Tác vụ AI ngầm bị lỗi khi công thức được xuất bản.
4. Thực thể dữ liệu:
- moderation_actions: id, moderator_id, target_type, target_id, action, notes, created_at.
- user_suspensions: id, user_id, suspended_by, reason, duration_days, expires_at, is_permanent, created_at.
- featured_recipes: id, recipe_id, curated_by, display_order, is_active, created_at.
- audit_logs: id, actor_id, action, target_type, target_id, metadata (JSONB), created_at.
- Dẫn chiếu bảng reports từ spec 070.
5. Tiêu chí thành công:
- SC-001: Thời gian xử lý hàng đợi duyệt và báo cáo hiển thị tức thì (< 1.0s).
- SC-002: 100% hành vi kiểm duyệt được ghi vết đầy đủ vào audit_logs.
- SC-003: Không có trường hợp người dùng bị cấm vẫn có thể tạo nội dung mới.
- SC-004: Ngăn chặn 100% việc tự duyệt bài của bản thân.
- SC-005: Sự cố dịch vụ AI không làm gián đoạn luồng xuất bản công thức.
```

### Kết quả do AI sinh ra & Kiểm chứng của sinh viên:
- File đặc tả đã tạo: `specs/100-admin-moderation/spec.md`.
- File checklist chất lượng: `specs/100-admin-moderation/checklists/requirements.md`.
- Sinh viên kiểm tra:
  + Cấu trúc tiếng Anh chuyên ngành chuẩn mực, bao phủ 6 User Stories, 18 FR, 6 NFR.
  + Ma trận RBAC phân định 5 vai trò × 14 hành động quản trị cực kỳ chặt chẽ.
  + Toàn bộ 10 Edge Cases được bao bọc đầy đủ giải pháp xử lý.

---

## Mục 6: Làm rõ yêu cầu kỹ thuật và hành vi biên cho spec 100 bằng `/speckit-clarify`

| Trường | Nội dung |
|---|---|
| **Ngày** | 2026-10-19 |
| **Người thực hiện** | Trần Nguyễn Công Chung (`itzchugnn`) |
| **Spec liên quan** | 100 – Bảng điều khiển Quản trị & Kiểm duyệt nội dung (`specs/100-admin-moderation/`) |
| **Công cụ AI** | Google Antigravity (Gemini 3.8 Flash), lệnh Spec Kit `/speckit-clarify` |
| **Mục đích** | Rà soát toàn diện các điểm mù kỹ thuật trong đặc tả spec 100, giải quyết câu hỏi nghiệp vụ kiểm duyệt và phân quyền để hoàn thiện tài liệu theo đúng ngân sách Spec Kit. |
| **Nhánh Git** | `Chung` (chuẩn bị cho branch `feature/100-admin-moderation`) |

### Quá trình làm rõ & Quyết định kỹ thuật:
- **Câu hỏi 1 (Lưu trữ và hiển thị ghi chú `REVISION_REQUESTED`):** Khi Moderator yêu cầu chỉnh sửa, lý do được lưu ở đâu và tác giả xem như thế nào?
  + *Quyết định chọn:* Lưu trong bảng `moderation_actions` (cột `notes` TEXT). Tác giả nhìn thấy ghi chú giải thích trực tiếp trên trang quản lý công thức cá nhân (spec `060-recipe-editor`), kèm theo nút bấm kích hoạt lại chế độ chỉnh sửa và nút "Gửi duyệt lại".
  + *Cập nhật vào Spec:* Bổ sung mục `## Clarifications`, cập nhật `FR-003` và Scenario 4 của User Story 1.
- **Câu hỏi 2 (Quyền can thiệp nội dung của Admin):** Admin/Moderator có quyền trực tiếp chỉnh sửa câu chữ trong công thức của người dùng hay không?
  + *Quyết định chọn:* **Tuyệt đối không.** Admin/Moderator chỉ có quyền Phê duyệt, Từ chối hoặc Yêu cầu sửa. Nếu nội dung có sai sót, phải gửi `REVISION_REQUESTED` kèm hướng dẫn để tác giả tự sửa. Quyết định này nhằm bảo toàn quyền sở hữu trí tuệ và chống tranh chấp nội dung.
  + *Cập nhật vào Spec:* Bổ sung mục `## Clarifications`, cập nhật `Assumptions & Dependencies`.
- **Câu hỏi 3 (Khả năng nộp lại sau khi bị từ chối `REJECTED`):** Tác giả có được sửa và gửi duyệt lại một công thức đã bị `REJECTED` không?
  + *Quyết định chọn:* Có. Tác giả được phép mở lại bản nháp, khắc phục các vi phạm được ghi trong lý do từ chối và bấm "Gửi duyệt". Khi đó trạng thái công thức quay lại `PENDING`. Lịch sử lý do từ chối cũ vẫn được lưu trong `moderation_actions` để đối chiếu.
  + *Cập nhật vào Spec:* Bổ sung mục `## Clarifications`, cập nhật `Edge Case 3` và `FR-004`.
- **Câu hỏi 4 (Giới hạn quyền xử phạt giữa Moderator và Admin):** Ranh giới quyền xử lý tài khoản vi phạm giữa 2 cấp quản trị.
  + *Quyết định chọn:* **Moderator** chỉ được phép đình chỉ tài khoản tạm thời tối đa 30 ngày. Quyền cấm vĩnh viễn (`BANNED`) và quyền thay đổi vai trò (Role assignment) là đặc quyền duy nhất của **Administrator**.
  + *Cập nhật vào Spec:* Bổ sung mục `## Clarifications`, cập nhật ma trận RBAC và `FR-008`.
- **Câu hỏi 5 (Cơ chế thu hồi quyền truy cập khi tài khoản bị đình chỉ/cấm):** Người dùng bị cấm khi đang đăng nhập có bị kick văng ngay lập tức không?
  + *Quyết định chọn:* Do hệ thống sử dụng stateless JWT, người dùng sẽ không bị ngắt kết nối ngay tức thì mà quyền sẽ bị chặn khi JWT hết hạn (thời gian sống 15–30 phút) và client gọi refresh token (lúc này Server kiểm tra cờ trạng thái tài khoản và từ chối cấp token mới, buộc đăng xuất). Đối với các thao tác nhạy cảm (đăng công thức, bình luận), backend kiểm tra trạng thái tài khoản trực tiếp trong CSDL tại thời điểm gọi API.
  + *Cập nhật vào Spec:* Bổ sung mục `## Clarifications`, cập nhật `FR-009` và `Edge Case 5`.
- **Câu hỏi 6 (Tính bất biến của Audit Log):** Admin cấp cao có thể chỉnh sửa hoặc xóa nhật ký kiểm toán trong trường hợp đặc biệt không?
  + *Quyết định chọn:* **Tuyệt đối không.** Bảng `audit_logs` được cấu hình cơ chế chỉ ghi (Append-Only). Backend không cung cấp bất kỳ API nào cho phép `UPDATE` hoặc `DELETE` trên bảng này nhằm đảm bảo tính toàn vẹn và giá trị chứng thực kiểm toán.
  + *Cập nhật vào Spec:* Bổ sung mục `## Clarifications`, cập nhật `NFR-005` và `FR-015`.

### Kết quả phiên làm rõ & Đánh giá chất lượng:
- **Số câu hỏi hoàn thành:** 6 câu hỏi kỹ thuật kiểm toán và an toàn hệ thống.
- **File đặc tả đã cập nhật:** `specs/100-admin-moderation/spec.md`.
- **Checklist chất lượng yêu cầu:** `specs/100-admin-moderation/checklists/requirements.md` đạt 16/16 tiêu chí (100% PASSED).
- **Trạng thái:** Không còn bất kỳ điểm mù nào. Sẵn sàng 100% để bước vào giai đoạn thiết kế CSDL và lập kế hoạch kỹ thuật (`/speckit-plan`).

---

## Tổng kết tuần 1 & Kế hoạch tiếp theo của Thành viên E (Chung)

1. **Kết quả đạt được:**
   - Hoàn thành 100% việc soạn thảo 2 bản đặc tả chất lượng cao: [specs/070-review-cooksnap/spec.md](file:///e:/WikiCook-main/specs/070-review-cooksnap/spec.md) và [specs/100-admin-moderation/spec.md](file:///e:/WikiCook-main/specs/100-admin-moderation/spec.md).
   - Đầy đủ 2 file checklist chất lượng kiểm định 16/16 tiêu chí (100% PASSED): [070 checklist](file:///e:/WikiCook-main/specs/070-review-cooksnap/checklists/requirements.md) và [100 checklist](file:///e:/WikiCook-main/specs/100-admin-moderation/checklists/requirements.md).
   - Thống nhất toàn bộ các điểm giao với các thành viên khác trong nhóm (B, D, A, C).
   - Nhật ký sử dụng AI được chuẩn hóa hoàn toàn theo định dạng Spec Kit chuẩn (`/speckit-specify`, `/speckit-clarify`).

2. **Kế hoạch tiếp theo (Thứ 5 – Thứ 6):**
   - **Thứ 5 (15/10):** Hỗ trợ review chéo spec cho thành viên khác theo bảng phân công và chuẩn bị wireframe UI bản đầu cho các màn hình của module 070 và 100.
   - **Thứ 6 (16/10):** Chạy lệnh `/speckit-plan` để sinh `data-model.md` khớp đặc tả CSDL chung của nhóm, cập nhật 7 bảng CSDL (`reviews`, `cooksnaps`, `reports`, `moderation_actions`, `user_suspensions`, `featured_recipes`, `audit_logs`) vào tài liệu CSDL chung, kèm bảng phân quyền RBAC chi tiết.
