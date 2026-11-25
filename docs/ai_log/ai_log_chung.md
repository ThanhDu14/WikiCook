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
| 7 | 2026-10-21 | Tổng hợp & Kiểm toán lịch sử sử dụng AI toàn nhóm trong Sprint 2 (`SCRUM-41`) | `docs/management/reports/sprint-02/sprint-02-report.md` (Mục 4) |
| 8 | 2026-10-21 | Tiến hành kiểm thử chất lượng (QA Audit) hệ thống tài liệu Markdown (`SCRUM-42`) | Toàn bộ `docs/` (`SCRUM-42`) |
| 9 | 2026-10-31 | Soạn thảo Use-Case Specifications chi tiết cho Feature 070 & 100 (`SCRUM-54`, chuẩn bị `SCRUM-58`) | `docs/requirements/use-case-specification.md` |
| 10 | 2026-11-01 – 2026-11-04 | Tổng hợp, trích xuất và nhúng 70+ Prototype Screenshots vào từng Use Case (`SCRUM-58`) | `docs/requirements/use-case-specification.md`, PR #69 (commit `a74eae8`) |
| 11 | 2026-11-03 – 2026-11-06 | Tiến hành QA Cross-Audit 9 chiều toàn bộ gói nộp PA3 và soạn thảo Section 6 (`SCRUM-62`) | `sprint-03-report.md` Section 6, `.local/scrum-62-63-qa-audit-and-ai-report.md` |
| 12 | 2026-11-05 – 2026-11-06 | Thanh tra 5 file nhật ký AI toàn nhóm và chốt Báo cáo Tuân thủ Section 4 (`SCRUM-63`) | `docs/ai_log/`, `sprint-03-report.md` Section 4 |
| 13 | 2026-11-16 | Thiết lập rào chắn kiến trúc, chỉ đạo TDD và hiện thực mã nguồn Backend Feature 070 (`SCRUM-77`) | `src/backend/`, `specs/070-review-cooksnap/` |
| 14 | 2026-11-16 – 2026-11-18 | Chỉ huy gỡ lỗi CI Integration Test, xử lý xung đột Spring Security filter chain và thẩm định conflict PR #98 (`SCRUM-77`, `SCRUM-78`) | `src/backend/`, `src/frontend/`, PR #93, PR #98 |
| 15 | 2026-11-20 – 2026-11-21 | Thẩm định kiểm thử toàn hệ thống (mvn verify), chuẩn hóa JaCoCo, hoàn tất báo cáo và sản xuất video demo YouTube (`SCRUM-79`) | `docs/test/scrum-79-spec-kit-test-report.md`, `README.md`, PR #108 |
| 16 | 2026-11-20 – 2026-11-25 | Kiểm toán đối chiếu Use-Case Spec v2.1/v2.1.1 (`SCRUM-71`), chuẩn hóa quan hệ UC-100-02, thanh tra AI Log toàn đội (`SCRUM-85`) và kiểm toán chất lượng bàn giao PA4 (`SCRUM-86`) | `docs/requirements/use-case-specification.md`, `docs/ai_log/`, `sprint-04-report.md` (PR #109, PR #116, PR #117) |

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

## Mục 7: Tổng hợp & Kiểm toán lịch sử sử dụng AI toàn nhóm trong Sprint 2 (`SCRUM-41`)

| Trường | Nội dung |
|---|---|
| **Ngày** | 2026-10-21 |
| **Người thực hiện** | Trần Nguyễn Công Chung (`itzchugnn`) |
| **Tài liệu liên quan** | `docs/management/reports/sprint-02/sprint-02-report.md` (Mục 4), `docs/ai_log/`, `docs/assets/jira/ai_accounts/` |
| **Công cụ AI** | Google Antigravity (Gemini 3.8 Flash) |
| **Mục đích** | Đóng vai trò Consolidator & QA Auditor, trích xuất dữ liệu từ các file nhật ký AI của 5 thành viên để tổng hợp thành bảng Phụ lục (Appendix - Mục 4) trong `sprint-02-report.md`, đối soát 100% tính xác thực của link log và ảnh môi trường làm việc theo quy chế `AI Usage Guidelines.pdf`. |
| **Nhánh Git** | `Chung` |

### Prompt đã dùng:

```text
Hãy hỗ trợ tôi rà soát toàn bộ 5 file nhật ký sử dụng AI trong docs/ai_log/ của 5 thành viên (Hưng, Duy, Hiển, Dự, Chung).
Trích xuất tối thiểu 1–2 phiên tương tác cốt lõi của mỗi thành viên (tổng cộng 10 phiên) đại diện cho toàn bộ deliverables của PA2 (Project Plan, Vision Document, Spec Kit Constitution, Feature Specifications).
Tổng hợp thành bảng 9 cột theo đúng quy chuẩn: Interaction ID (AI-S2-01 đến AI-S2-10), Member Name, AI Tool & Version, Date & Time, Prompt Used, Purpose, AI-Generated Output, Human Verification & Revision, Evidence.
Kiểm tra tính nhất quán giữa các liên kết log tương đối và đường dẫn ảnh xác thực tài khoản trong docs/assets/jira/ai_accounts/.
```

### Tóm tắt kết quả do AI sinh ra & Kiểm chứng của sinh viên:
- AI hỗ trợ quét và tổng hợp dữ liệu thô từ 5 file log thành khung bảng 9 cột chuẩn.
- Sinh viên tiến hành kiểm toán độc lập (Human Verification):
  1. Kiểm tra chéo 100% các phiên tương tác, loại bỏ các chi tiết thừa, làm nổi bật phần sinh viên tự làm độc lập (Human-in-the-loop).
  2. Rà soát chuẩn hóa đường dẫn tương đối từ `docs/management/reports/sprint-02/sprint-02-report.md` đến `../../../ai_log/` và `../../../assets/jira/ai_accounts/`.
  3. Bổ sung các quyết định kỹ thuật then chốt của từng thành viên (như việc tách riêng module manual planner khi AI outage, khống chế 50 nguyên liệu loại trừ, bỏ EXIF ảnh, Append-Only audit log).
  4. Ký duyệt biên bản kiểm toán chất lượng (QA Audit Verdict: PASSED), cập nhật `SCRUM-41` từ `TO DO` sang `DONE` và nâng phiên bản báo cáo lên `v1.3`.

---

## Mục 8: Tiến hành kiểm thử chất lượng (QA Audit) hệ thống tài liệu Markdown (`SCRUM-42`)

| Trường | Nội dung |
|---|---|
| **Ngày** | 2026-10-21 |
| **Người thực hiện** | Trần Nguyễn Công Chung (`itzchugnn`) |
| **Tài liệu liên quan** | Toàn bộ thư mục `docs/` (`docs/management/`, `docs/requirements/`, `docs/analysis-and-design/`) |
| **Công cụ AI** | Google Antigravity (Gemini 3.8 Flash) |
| **Mục đích** | Đóng vai trò QA Lead, tiến hành kiểm thử chất lượng toàn diện (Quality Assurance Audit) trên toàn bộ hệ thống tài liệu Markdown trước khi xuất bản PDF cho Sprint 2. Kiểm tra 100% metadata 3 cấp, tính toàn vẹn liên kết và ảnh, cú pháp render Mermaid, và ngôn ngữ tiếng Anh kỹ thuật. |
| **Nhánh Git** | `Chung` |

### Prompt đã dùng:
```text
Tiến hành kiểm thử chất lượng (Quality Assurance Audit) trên toàn bộ hệ thống tài liệu Markdown trước khi xuất bản:
1. Kiểm tra 100% các tiêu đề ## phải có dòng metadata: *Performed by: ... | Reviewed by: ... | Edited by: Lê Quốc Hưng*.
2. Kiểm tra tính toàn vẹn của tất cả các liên kết nội bộ (internal markdown links) và đường dẫn ảnh.
3. Kiểm tra cú pháp render của các khối mã Mermaid.
4. Đảm bảo ngôn ngữ tiếng Anh thống nhất, không có lỗi chính tả cơ bản.
Rà soát toàn bộ thư mục WikiCook/docs/ và báo cáo kết quả đạt chuẩn sign-off trước khi xuất PDF.
```

### Kết quả rà soát & Kiểm chứng độc lập của sinh viên:
1. **Kiểm tra Metadata 3 cấp:**
   - Đã quét toàn bộ các file `.md` trong `docs/`.
   - Bổ sung bảng Document Metadata và metadata 3 cấp cho `docs/requirements/comparative-analysis.md` (Mục 3.1 & 3.3) và `docs/management/group-registration.md` (Mục 1, 2, 3).
   - Đạt 100% chuẩn: `*Performed by: ... | Reviewed by: ... | Edited by: Lê Quốc Hưng*`.
2. **Kiểm tra liên kết nội bộ & đường dẫn ảnh:**
   - Quét tự động toàn bộ 25+ relative links và image tags trong `docs/`.
   - Kết quả: 0 liên kết chết (0 broken links), 0 ảnh lỗi (0 broken images).
3. **Kiểm tra cú pháp render Mermaid:**
   - Trích xuất toàn bộ 19 sơ đồ Mermaid trong `docs/`.
   - Chạy kiểm thử tự động với engine `mermaid.js` v10 trong môi trường trình duyệt: **19/19 diagrams render thành công không có lỗi cú pháp**.
   - Loại bỏ các dòng lặp dư thừa tại Diagram 14 trong `docs/requirements/project-proposal.md`.
4. **Kiểm tra chính tả tiếng Anh & định dạng Markdown:**
   - Quét lỗi chính tả, từ lặp và đóng mở dấu backtick/code block trên toàn bộ tài liệu tiếng Anh.
   - Kết quả: 0 lỗi chính tả, định dạng chuẩn Markdown GFM.
5. **Cập nhật Jira Tracking:**
   - Chuyển `SCRUM-42` thành `DONE` trong bảng Master Backlog của `sprint-02-report.md`.
   - Ký biên bản nghiệm thu QA Sign-Off sẵn sàng cho release package `PA2-Group06.zip`.

---

### Tổng kết Sprint 2 của thành viên Chung:
- **Nhiệm vụ cá nhân hoàn thành:** `SCRUM-37` (Spec Kit `/src` sync), `SCRUM-38` (Training Log), `SCRUM-41` (AI Usage Appendix), `SCRUM-42` (QA Audit Documentation).
- **Trạng thái:** 4/4 tasks đạt `DONE`. Toàn bộ deliverables của Sprint 2 đã qua kiểm định QA độc lập, đạt 100% Acceptance Criteria và Definition of Done (DoD).

---

# PHẦN II: SPRINT 3 (PROJECT ASSIGNMENT 3 - PA3)

## Mục 9: Soạn thảo Use-Case Specifications cho Feature 070 và Feature 100 (`SCRUM-54`)

### 9.1. Thông tin phiên làm việc (Session Metadata)

| Trường | Nội dung |
|---|---|
| **Mục** | **Mục 9** \| **Ngày thực hiện:** 31/10/2026 |
| **Người thực hiện** | Trần Nguyễn Công Chung (`itzchugnn` - MSSV: 24120272) — QA Lead, Compliance Auditor & Market Researcher 1 |
| **Tài liệu liên quan** | `docs/requirements/use-case-specification.md` (Section 12: Feature 070 & Section 13: Feature 100), `specs/070-review-cooksnap/spec.md`, `specs/100-admin-moderation/spec.md`, `docs/analysis-and-design/ui-ux/screens/` |
| **Công cụ AI & Model** | Google Antigravity (Gemini 3.8 Flash High) |
| **Mục đích làm việc** | Soạn thảo đặc tả Use-Case chi tiết (8 mục chuẩn RUP) cho 2 phân hệ phụ trách: **Feature 070 (Community Reviews, Cooksnaps & Interactive Ratings)** và **Feature 100 (Admin Dashboard & Content Moderation)** nhằm phục vụ Deliverable Phần D (Use-Case Specification - 45 điểm) trong Sprint 3 (PA3) theo task Jira `SCRUM-54`. Đồng thời rà soát nguyên mẫu UI có sẵn để chuẩn bị cho task `SCRUM-58`. |
| **Nhánh Git** | `Chung` (chuẩn bị pull request cho task `SCRUM-54`) |

---

### 9.2. Chuỗi câu lệnh / Prompt đã sử dụng (Prompt History & Progressive Extensibility)

> [!NOTE]
> Mục này ghi lại các prompt kỹ thuật đã thực hiện theo tiến trình công việc trong Sprint 3. Các prompt tiếp theo thuộc chu kỳ làm việc (như rà soát UI Prototype Gap, ánh xạ Screenshot cho 10 Feature trong `SCRUM-58`) sẽ tiếp tục được ghi nối tiếp vào bên dưới.

#### Prompt 9.1 (Feature 070 - Community Reviews & Cooksnaps):
```text
Đóng vai trò QA Lead của dự án WikiCook (Group 05). Dựa trên đặc tả ratified specs/070-review-cooksnap/spec.md, 
hãy soạn thảo chi tiết toàn bộ các Use Case cho Feature 070 (từ UC-070-01 đến UC-070-06) bằng tiếng Anh kỹ thuật chuyên nghiệp.
Yêu cầu bắt buộc:
1. Dòng metadata tác giả ở đầu mỗi Use Case: 
   *Performed by: Trần Nguyễn Công Chung (itzchugnn) | Reviewed by: Mai Văn Hiển (MaiHien3507) | Edited by: Trần Nguyễn Công Chung (itzchugnn)*
2. Đầy đủ 8 mục: Name and Identifier, Actor(s), Description, Preconditions, Basic Flow (đánh số tuần tự), 
   Alternative Flows (đánh số AF x.1, AF x.2...), Postconditions (CSDL & UI), Special Requirements (NFR, bảo mật, concurrency).
3. Ràng buộc kỹ thuật: Giới hạn 1 review/người/công thức, tính toán rating_avg và review_count nguyên tử (atomic aggregation), 
   tối đa 3 ảnh Cooksnap <= 5MB, lọc bỏ EXIF metadata, xử lý soft-delete cascade, deduplication báo cáo vi phạm.
4. Ánh xạ trực tiếp file prototype có sẵn (RV01-RV06, CK09) và phân tích các UI Asset Gaps cho task SCRUM-58.
```

#### Prompt 9.2 (Feature 100 - Admin Dashboard & Content Moderation):
```text
Tiếp tục soạn thảo chi tiết toàn bộ các Use Case cho Feature 100: Administration & Content Moderation 
(từ UC-100-01 đến UC-100-06) dựa trên specs/100-admin-moderation/spec.md theo đúng mẫu 8 mục chuẩn:
1. Danh sách Use Case: UC-100-01 (Recipe Moderation Queue), UC-100-02 (Process Content Reports), 
   UC-100-03 (User Account Governance), UC-100-04 (Taxonomy Management), UC-100-05 (Curate Featured Recipes), 
   UC-100-06 (Audit Log & Platform Metrics).
2. Ràng buộc bảo mật & nghiệp vụ khắt khe:
   - Self-Moderation Prevention: Cấm Moderator/Admin tự duyệt công thức của mình (HTTP 403 Forbidden).
   - Concurrent Moderation Conflict: Xử lý xung đột khi 2 người cùng duyệt 1 bài (HTTP 409 Conflict).
   - Taxonomy Deactivation Guard: Cấm hard-delete danh mục/nguyên liệu đang có công thức sử dụng (chỉ is_active = FALSE).
   - Append-Only Audit Log: Bảng audit_logs bất biến, cấm tuyệt đối UPDATE/DELETE.
   - Phân quyền RBAC rõ rệt: Moderator chỉ đình chỉ tối đa 30 ngày; Admin mới có quyền Cấm vĩnh viễn, đổi Role và ghim Featured.
3. Ánh xạ bộ prototype AD01 đến AD11 và lập bảng Traceability Matrix bao phủ 18 FR của Spec 100.
```

<!-- CÁC PROMPT TIẾP THEO CỦA SPRINT 3 (SCRUM-54 / SCRUM-58) SẼ ĐƯỢC NỐI TIẾP TẠI ĐÂY -->

---

### 9.3. Quá trình phân tích & Sự can thiệp chuyên môn của sinh viên (Human-in-the-Loop)

#### a. Kết quả ban đầu do AI tạo ra:
- AI đã thiết lập được khung sườn cơ bản cho 12 Use Case (`UC-070-01` – `UC-070-06` và `UC-100-01` – `UC-100-06`) với ngôn ngữ tiếng Anh kỹ thuật đạt chuẩn, tuân thủ đúng 8 mục của Rational Unified Process (RUP).
- Tích hợp các tham chiếu đến các file màn hình `.dc.html` tương ứng trong kho prototype (`RV01` – `RV06`, `AD01` – `AD11`).

#### b. Các thiếu sót và lỗ hổng kỹ thuật do sinh viên phát hiện:
Qua quá trình rà soát độc lập trên vai trò **QA Lead & Compliance Auditor**, tôi đã phát hiện một số thiếu sót nghiêm trọng về tính toàn vẹn dữ liệu, an ninh ứng dụng và tính khả dụng của luồng nghiệp vụ:
1. *Lỗ hổng tự buff điểm số:* Chưa có cơ chế chặn tác giả công thức tự gửi đánh giá và chấm 5 sao cho bài viết của chính mình.
2. *Nguy cơ spam phá hoại:* Thiếu cơ chế điều tiết lưu lượng (Rate Limiting) ở tầng API đối với hành vi gửi đánh giá dồn dập từ bot/tài khoản ảo.
3. *Đứt gãy tính nhất quán điểm số khi kiểm duyệt:* Khi Admin/Moderator ẩn hoặc xóa một đánh giá vi phạm từ trang quản trị (Feature 100), điểm trung bình `rating_avg` của công thức không được đồng bộ tính toán lại tức thì.
4. *Nội dung vi phạm nghiêm trọng chưa được cách ly kịp thời:* Khi một nội dung nhận hàng loạt tố cáo từ cộng đồng, nội dung đó vẫn hiển thị bình thường cho đến khi Moderator vào duyệt thủ công.
5. *Trải nghiệm phân loại đánh giá thụ động:* Danh sách đánh giá chỉ hỗ trợ lọc theo thời gian hoặc số sao, thiếu cơ chế xã hội hóa để người đọc bình chọn và tìm kiếm các đánh giá hữu ích nhất.
6. *Xung đột kiểm duyệt đồng thời chưa tối ưu:* Xử lý xung đột mới chỉ dừng ở việc ném mã lỗi HTTP 409 Conflict thụ động khi bấm nút, chưa có tín hiệu cảnh báo trước để kiểm duyệt viên không dẫm chân lên nhau.

#### c. Can thiệp kỹ thuật & Bổ sung nghiệp vụ của sinh viên (Chứng minh tính làm chủ bài làm):
Tôi đã trực tiếp can thiệp, yêu cầu bổ sung logic chuyên sâu và tự tay hiệu chỉnh các Use Case trong `docs/requirements/use-case-specification.md`:

1. **Chặn tác giả tự chấm điểm (`UC-070-01`):** 
   - Nhận thấy kẽ hở tác giả tự "buff" điểm ảo, sinh viên đã yêu cầu bổ sung logic UI: Ẩn hoàn toàn nút `"Viết đánh giá"` đối với tác giả công thức (`recipe.author_id == current_user_id`).
   - Tại tầng backend (Controller/Service), bổ sung tiền điều kiện chặn mã lỗi HTTP 403 Forbidden (`SELF_REVIEW_FORBIDDEN`) nếu có cố tình gọi API trực tiếp.
2. **Chống spam đánh giá liên tục (`UC-070-01`):**
   - Thêm cơ chế Rate Limiting (sliding window): Giới hạn mỗi tài khoản chỉ được gửi tối đa 5 đánh giá trong vòng 10 phút trên toàn hệ thống để chặn tài khoản bot/clone chấm điểm phá hoại. Khi vượt ngưỡng, hệ thống trả về mã lỗi HTTP 429 Too Many Requests.
3. **Đồng bộ tính toán lại điểm số (`UC-070-03`, `UC-100-02`):**
   - Đảm bảo tính nhất quán dữ liệu liên phân hệ; bất cứ khi nào Admin/Moderator ẩn (`visibility = 'HIDDEN'`) hoặc xóa một đánh giá vi phạm ở Feature 100, dịch vụ `RecipeAggregateService.recalculate(recipeId)` đều phải được kích hoạt trong một database transaction cô lập để cập nhật lại ngay tức thì `rating_avg` và `review_count` trên bảng `recipes`.
4. **Tự động cách ly nội dung nhận nhiều báo cáo (`UC-070-05`, `UC-100-02`):**
   - Thiết lập ngưỡng bảo vệ cộng đồng tự động: Khi một bài review hoặc ảnh Cooksnap nhận $\ge 10$ lượt báo cáo vi phạm (`reports`) từ các người dùng khác nhau, hệ thống tự động ẩn nội dung (Auto-quarantine, `visibility = 'HIDDEN'`, `hidden_reason = 'MASS_REPORT'`) khỏi giao diện công chúng và gắn cờ ưu tiên cao (badge `"Khẩn cấp"`) gửi sang hàng đợi kiểm duyệt để xử lý tức thời.
5. **Hệ thống bầu chọn "Hữu ích" (`UC-070-04`):**
   - Bổ sung thực thể CSDL `review_votes` (`user_id`, `review_id`, `created_at`) với ràng buộc `UNIQUE(review_id, user_id)`.
   - Cung cấp bộ lọc sắp xếp `sort=helpful` trên giao diện `RV01-Reviews` nhằm đưa các đánh giá hữu ích, chất lượng cao lên đầu trang.
6. **Cảnh báo kiểm duyệt đồng thời thời gian thực (`UC-100-01`):**
   - Đề xuất bổ sung cơ chế WebSocket Presence: Khi Moderator A đang mở màn hình xem chi tiết của công thức X (`AD03-ReviewDetail`), Moderator B mở cùng công thức sẽ nhìn thấy biểu tượng và thông báo *"Moderator [Tên] đang xem xét công thức này"* để tránh đưa ra quyết định xung đột.

---

### 9.4. Đánh giá chất lượng & Nhật ký cập nhật tiến độ (Progress Log & Next Steps)

- **Trạng thái hiện tại:** Hoàn thành toàn diện 12 Use Case cho Feature 070 và Feature 100 theo yêu cầu `SCRUM-54`, bao phủ 100% Functional Requirements và Edge Cases.
- **Tính khả thi mở rộng:** Cấu trúc mục 9 được thiết kế dạng khối module mở (`<!-- PROMPTS_APPEND_ZONE -->`). Khi triển khai các bước tiếp theo của Sprint 3 (như lập Bảng ma trận đối chiếu Prototype Mapping Matrix và hướng dẫn chỉnh sửa DOM/chụp ảnh cho `SCRUM-58`), các prompt và can thiệp mới sẽ được bổ sung trực tiếp vào chuỗi mà không phá vỡ cấu trúc nhật ký đã có.

---

## Mục 10: Trích xuất & Nhúng Prototype Screenshots cho 10 Feature Use Cases (SCRUM-58)

### 10.1. Thông tin phiên làm việc (Session Metadata)

| Trường | Nội dung |
|---|---|
| **Mục** | **Mục 10** \| **Ngày thực hiện:** 2026-11-01 – 2026-11-04 (Tổng kết phiên chính: 04/11/2026, 21:00 – 22:30) |
| **Người thực hiện** | Trần Nguyễn Công Chung (`itzchugnn` - MSSV: 24120272) — QA Lead, Compliance Auditor & Market Researcher 1 |
| **Nhiệm vụ / Task Jira** | `SCRUM-58` — `[Use-Case]` Produce Prototype Screenshots for Every Use Case (Phần D — 45 điểm) |
| **Tài liệu liên quan** | `docs/requirements/use-case-specification.md`, `docs/analysis-and-design/ui-ux/screens/`, `scrum-58-ui-gap-list.md`, PR #69 |
| **Công cụ AI & Model** | Google Antigravity (Gemini 3.8 Flash High), Agentic Coding CLI & VS Code Extension |
| **Mục đích làm việc** | Lập danh sách UI Gap List (`scrum-58-ui-gap-list.md`), tự động hóa ánh xạ tên file ảnh từ prototype `.dc.html` sang định dạng PNG ở 2 kích thước viewport (360px mobile và 1280px desktop), và chèn cú pháp Markdown liên kết vào toàn bộ 64 use cases thuộc 10 feature (010–100) trong `use-case-specification.md`. |
| **Nhánh Git** | `Chung`, branch merge PR #69 |
| **Minh chứng (Evidence)** | Git commit `a74eae8` ("feat: prototype use case"), Pull Request #69 ("SCRUM-58: prototype screenshots mapping"), 70+ file ảnh PNG tại `docs/analysis-and-design/ui-ux/screens/`, ảnh xác thực tài khoản Google Antigravity tại `docs/assets/jira/ai_accounts/04_tran_nguyen_cong_chung_antigravity.jpg` |

---

### 10.2. Chuỗi câu lệnh / Prompt đã sử dụng (Prompts Used)

```text
Tôi đang thực hiện nhiệm vụ SCRUM-58 (Phần D - Use-Case Specifications & UI Screenshots, 45 điểm). 
Dựa trên tài liệu đặc tả 10 Feature trong docs/requirements/use-case-specification.md và kho giao diện mẫu (.dc.html):
1. Hãy phân tích toàn bộ 64 use cases từ Feature 010 đến Feature 100 để xác định các luồng nghiệp vụ (Basic Flows và Alternative Flows) còn thiếu ảnh minh họa giao diện (UI Gap Analysis).
2. Thiết kế bảng ma trận quy ước đặt tên file ảnh chụp giao diện: <FeatureCode>_<ScreenID>_<Viewport>.png (Mobile: 360px viewport và Desktop: 1280px viewport).
3. Tự động sinh cú pháp Markdown nhúng ảnh kèm caption mô tả chuẩn xác, sử dụng đường dẫn tương đối (relative path) từ docs/requirements/use-case-specification.md đến docs/analysis-and-design/ui-ux/screens/.
```

---

### 10.3. Nội dung do AI tạo ra (Which Content Was Generated by AI)
- Danh sách phân tích khoảng trống giao diện (UI Gap Analysis) chỉ ra các màn hình cần bổ sung chụp thực tế (các modal xác nhận xóa, màn hình quản trị kiểm duyệt `AD01`–`AD11`, các bộ lọc nâng cao của tìm kiếm).
- Bảng quy chuẩn cấu trúc đặt tên file ảnh cho 10 phân hệ chức năng: `010-auth`, `011-profile`, `020-search`, `030-planner`, `031-ai-planner`, `040-shopping-list`, `050-cooking-mode`, `060-recipe-editor`, `070-reviews`, `080-bookmarks`, `090-nutrition`, `100-admin`.
- Mẫu cú pháp liên kết Markdown nhúng ảnh theo đường dẫn tương đối:
  `![Caption](../../analysis-and-design/ui-ux/screens/<filename>.png)`

---

### 10.4. Sự can thiệp chuyên môn & Kiểm chứng của sinh viên (Human Verification & Editing)
- **Kiểm soát chất lượng & Tính xác thực học thuật (Academic Integrity):** Sinh viên trực tiếp render các file `.dc.html` thật bằng trình duyệt Google Chrome với DevTools mô phỏng thiết bị (iPhone 13 mini: 360px và Desktop/Laptop: 1280px), chụp ảnh màn hình thực tế và lưu trữ vào repo.
- **Tuyệt đối loại trừ việc dùng AI sinh ảnh giả mạo:** Kiểm toán và xác nhận 100% ảnh giao diện là ảnh chụp từ mã nguồn HTML/CSS gốc của nhóm, giữ nguyên typography, bảng màu và design system (`wikicook-soft-editorial.css`), không sử dụng bất kỳ công cụ text-to-image nào (Midjourney, DALL-E, Stable Diffusion).
- Tự tay rà soát và kiểm thử hiển thị hơn 70 liên kết hình ảnh trong `use-case-specification.md` qua GitHub Web Viewer và công cụ preview Markdown, sửa các lỗi gãy đường dẫn tương đối.
- Tạo commit `a74eae8`, mở PR #69 và đối chiếu với PO Nguyễn Đức Duy để nghiệm thu chuyển task `SCRUM-58` sang **DONE**.

---

## Mục 11: QA Cross-Audit Toàn bộ Deliverable PA3 (90 Điểm) & Soạn thảo Section 6 (SCRUM-62)

### 11.1. Thông tin phiên làm việc (Session Metadata)

| Trường | Nội dung |
|---|---|
| **Mục** | **Mục 11** \| **Ngày thực hiện:** 2026-11-03 – 2026-11-06 (Cycle 1: 03/11 lúc 22:00; Cycle 2: 05/11 lúc 23:59 – 06/11 lúc 10:00) |
| **Người thực hiện** | Trần Nguyễn Công Chung (`itzchugnn` - MSSV: 24120272) — QA Lead, Compliance Auditor & Market Researcher 1 |
| **Nhiệm vụ / Task Jira** | `SCRUM-62` — `[QA]` QA Cross-Audit for Entire PA3 Deliverable Package |
| **Tài liệu liên quan** | `docs/management/reports/sprint-03/sprint-03-report.md` (Section 6), `.local/scrum-62-63-qa-audit-and-ai-report.md`, `Changes.md`, `docs/management/project-plan.md`, `docs/requirements/vision-document.md`, `docs/requirements/use-case-model.md`, `docs/requirements/use-case-specification.md`, `src/backend/`, `src/frontend/`, `docker-compose.yml` |
| **Công cụ AI & Model** | Google Antigravity (Gemini 3.8 Flash High), Agentic Coding CLI & VS Code Extension |
| **Mục đích làm việc** | Quét và kiểm tra độc lập 6 dòng chấm điểm (Parts A–F, 90 điểm) theo rubric PA3; lập ma trận kiểm toán 9 chiều kỹ thuật (D1–D9); theo dõi và đóng 5 defect nghiêm trọng (`DEF-01` đến `DEF-05`); soạn thảo nguyên văn Section 6 (QA Deliverables Cross-Audit & Quality Gate Sign-Off) cho Báo cáo Sprint 03. |
| **Nhánh Git** | `Chung`, branch `main` |
| **Minh chứng (Evidence)** | Section 6 trong `docs/management/reports/sprint-03/sprint-03-report.md`, file kiểm toán chi tiết `.local/scrum-62-63-qa-audit-and-ai-report.md`, Git commit `26bbaea`, PR #74, ảnh xác thực tài khoản Google Antigravity tại `docs/assets/jira/ai_accounts/04_tran_nguyen_cong_chung_antigravity.jpg` |

---

### 11.2. Chuỗi câu lệnh / Prompt đã sử dụng (Prompts Used)

```text
Đóng vai trò QA Lead của dự án WikiCook (Group 05). Dựa trên dữ liệu kiểm toán nội bộ trong .local/scrum-62-63-qa-audit-and-ai-report.md và kết quả rà soát thực tế toàn bộ repository:
1. Hãy thiết lập ma trận kiểm toán 9 chiều kỹ thuật (Language, Mermaid diagrams, Metadata headers, 10-Feature Traceability, Single Changes.md, Part E TDD/Coverage, Part F Scrum ceremonies, Repo hygiene, Markdown-PDF parity).
2. Xây dựng bảng đối chiếu đánh giá 6 phân hệ rubric điểm (Parts A, B, C, D, E, F) tổng 90 điểm, chỉ rõ trạng thái PASSED cho từng phần kèm minh chứng xác thực.
3. Tổng hợp bảng Defect Findings & Resolution Log ghi nhận chi tiết 5 defect đã phát hiện và xử lý thành công: DEF-01 (UI gaps), DEF-02 (Redis/Build v0.5 sync), DEF-03 (X-User-Id client header fix c3398bf), DEF-04 (Changes.md Section B completion e8051a4), DEF-05 (Cross-request deduplication fix PR #59).
4. Soạn thảo tuyên bố nghiệm thu chất lượng chính thức (Official Quality Gate Verdict) xác nhận ZERO BLOCKING DEFECTS để phê duyệt đóng gói PA3-Group05.zip. Toàn bộ viết bằng tiếng Anh học thuật chuyên nghiệp để chèn vào Section 6 của sprint-03-report.md.
```

---

### 11.3. Nội dung do AI tạo ra (Which Content Was Generated by AI)
- Khung bảng ma trận kiểm toán 9 chiều kỹ thuật (D1–D9) bằng tiếng Anh chuyên ngành RUP/Agile.
- Cấu trúc bảng tổng hợp đánh giá 6 phân hệ rubric (Parts A, B, C, D, E, F) với các tiêu chí định lượng tương ứng.
- Khung bảng Defect Findings & Resolution Log theo dõi các lỗi chu kỳ kiểm toán 1 và 2.
- Bản thảo tuyên bố nghiệm thu chất lượng chính thức (Quality Gate Verdict & Sign-off).

---

### 11.4. Sự can thiệp chuyên môn & Kiểm chứng của sinh viên (Human Verification & Editing)
- Sinh viên tự mình đọc, đối chiếu và thẩm định thủ công từng file tài liệu và mã nguồn:
  + Rà soát tính toàn vẹn của `Changes.md` (đảm bảo đủ 21 mục Section A và 14 mục Section B, có tuyên bố không nhận được feedback từ TA theo `SCRUM-47`).
  + Thẩm định cú pháp 11 sơ đồ Mermaid trong `use-case-model.md` và kiểm tra độ phủ của 64 use cases trên 8 system actors.
  + Chạy lệnh `mvn test` và `mvn verify` tại thư mục backend, đọc file báo cáo JaCoCo xác nhận chỉ số instruction coverage của `com.wikicook.shoppinglist.service` đạt **83.1%** (vượt yêu cầu $\ge 70\%$).
  + Kiểm tra cấu hình Docker Compose thực tế: dịch vụ `db`, `app`, `web` chạy ổn định, dịch vụ `migrate` thoát mã 0, xác nhận đã loại bỏ Redis khỏi `Build v0.5` theo đúng cam kết.
  + Xác nhận cả 5 lỗi `DEF-01` đến `DEF-05` đã được sửa triệt để trên nhánh `main`, không còn bất kỳ lỗi chặn nộp (Zero Blocking Defects) nào.
  + Tuyên bố nghiệm thu chính thức và phê duyệt bàn giao cho DevOps Lead Nguyễn Thành Dự đóng gói `PA3-Group05.zip` (`SCRUM-65`).

---

## Mục 12: Thanh tra 5 File Nhật ký AI Toàn Nhóm & Chốt Báo cáo Tuân thủ Section 4 (SCRUM-63)

### 12.1. Thông tin phiên làm việc (Session Metadata)

| Trường | Nội dung |
|---|---|
| **Mục** | **Mục 12** \| **Ngày thực hiện:** 2026-11-05 – 2026-11-06 (Phiên rà soát & chốt: 06/11/2026, 08:30 – 10:30) |
| **Người thực hiện** | Trần Nguyễn Công Chung (`itzchugnn` - MSSV: 24120272) — QA Lead, Compliance Auditor & Market Researcher 1 |
| **Nhiệm vụ / Task Jira** | `SCRUM-63` — `[Compliance]` Synthesize AI Usage Report for PA3 |
| **Tài liệu liên quan** | `docs/ai_log/` (5 file: `ai_log_hung.md`, `ai_log_duy.md`, `ai_log_hien.md`, `ai_log_thanhdu.md`, `ai_log_chung.md`), `docs/management/reports/sprint-03/sprint-03-report.md` (Section 4), `docs/assets/jira/ai_accounts/` |
| **Công cụ AI & Model** | Google Antigravity (Gemini 3.8 Flash High), Agentic Coding CLI & VS Code Extension |
| **Mục đích làm việc** | Thanh tra và kiểm tra chéo 5 file nhật ký AI của các thành viên trong nhóm theo 7 tiêu chí bắt buộc của `AI Usage Guidelines.pdf`; tổng hợp bảng 10 phiên tương tác cốt lõi đại diện cho 100% deliverable PA3; soạn thảo và đồng bộ Section 4 (AI Usage Declaration & Compliance Log) trong `sprint-03-report.md`. |
| **Nhánh Git** | `Chung`, branch `main` |
| **Minh chứng (Evidence)** | Section 4 trong `sprint-03-report.md`, các ảnh bằng chứng môi trường AI trong `docs/assets/jira/ai_accounts/`, file `.local/scrum-62-63-qa-audit-and-ai-report.md` (Mục 2.4), ảnh xác thực tài khoản Google Antigravity tại `docs/assets/jira/ai_accounts/04_tran_nguyen_cong_chung_antigravity.jpg` |

---

### 12.2. Chuỗi câu lệnh / Prompt đã sử dụng (Prompts Used)

```text
Đóng vai trò Compliance Auditor của dự án WikiCook. Hãy hỗ trợ tôi rà soát toàn bộ 5 file nhật ký sử dụng AI cá nhân trong thư mục docs/ai_log/:
- ai_log_hung.md (PM & Scrum Master)
- ai_log_duy.md (Product Owner)
- ai_log_hien.md (Tech Lead & AI Architect)
- ai_log_thanhdu.md (DevOps Lead)
- ai_log_chung.md (QA Lead & Compliance Auditor)
Yêu cầu:
1. Kiểm tra mức độ tuân thủ 7 tiêu chí bắt buộc theo AI Usage Guidelines.pdf (công cụ, thời gian truy cập, prompt, mục đích, nội dung AI sinh, phần sinh viên tự làm/kiểm chứng, minh chứng bằng chứng).
2. Xây dựng bảng tổng hợp 10 phiên tương tác cốt lõi (AI-S3-01 đến AI-S3-10) đại diện cho toàn bộ các sản phẩm bàn giao của Sprint 3 (Parts A, B, C, D, E, F), chỉ rõ sự can thiệp và kiểm chứng chuyên môn của từng sinh viên.
3. Soạn thảo lời tuyên bố liêm chính học thuật (Academic Integrity Governance) và biên bản nghiệm thu tuân thủ (Compliance Sign-off) để tích hợp vào Section 4 của sprint-03-report.md.
```

---

### 12.3. Nội dung do AI tạo ra (Which Content Was Generated by AI)
- Khung bảng đối chiếu 5 file nhật ký cá nhân theo 7 trường tiêu chí bắt buộc của môn học.
- Bảng tổng hợp Master AI Interaction Table gồm 10 phiên tương tác điển hình (`AI-S3-01` đến `AI-S3-10`) ánh xạ đến các task Jira trọng yếu.
- Bản thảo ngôn ngữ tiếng Anh chuyên nghiệp cho các mục 4.1 (Academic Integrity Governance), 4.2 (Tooling & Platform Matrix), 4.3 (Consolidated Master Table), và 4.4 (Compliance Sign-off) trong `sprint-03-report.md`.

---

### 12.4. Sự can thiệp chuyên môn & Kiểm chứng của sinh viên (Human Verification & Editing)
- Sinh viên trực tiếp mở từng file nhật ký cá nhân trong `docs/ai_log/`, đối chiếu thời gian truy cập với Git commit log và kiểm tra các prompt của từng thành viên:
  + Xác nhận Hưng có 23 mục nhật ký (chi tiết TDD Backend 040 và điều phối dự án).
  + Xác nhận Duy có 15 mục nhật ký (đặc tả use-case, mô hình Mermaid và Frontend).
  + Xác nhận Hiển có 23 mục nhật ký (kiến trúc phân lớp, Project Plan và Review PR #52).
  + Xác nhận Dự có 13 mục nhật ký (Docker Compose skeleton và cấu hình hạ tầng).
  + Xác nhận bản thân Chung hoàn tất 12 mục nhật ký (đặc tả Feature 070/100, ảnh prototype UI và kiểm toán chất lượng QA/Compliance).
- Kiểm tra tính xác thực của các ảnh chụp môi trường IDE / CLI trong `docs/assets/jira/ai_accounts/` (Antigravity, Claude Code, Claude.ai).
- Khẳng định 100% mã nguồn và tài liệu đều có sự can thiệp, chỉnh sửa và chịu trách nhiệm chuyên môn của sinh viên ("Human-in-the-Loop").
- Ký xác nhận QA Audit Verdict: **PASSED — FULL ACADEMIC INTEGRITY COMPLIANCE (100% AUDIT READY FOR PA3 SUBMISSION)** tại Section 4.4 của `sprint-03-report.md`.

---

## Mục 13: Thiết lập rào chắn kiến trúc, chỉ đạo TDD và hiện thực mã nguồn Backend Feature 070 (`SCRUM-77`)

### 13.1. Thông tin phiên làm việc (Session Metadata)

| Trường | Nội dung |
|---|---|
| **Mục** | **Mục 13** \| **Ngày thực hiện:** 2026-11-16 (08:30 – 15:40) |
| **Người thực hiện** | Trần Nguyễn Công Chung (`itzchugnn` - MSSV: 24120272) — QA Lead, Compliance Auditor & Backend Lead Member E |
| **Nhiệm vụ / Task Jira** | `SCRUM-77` — `[Backend]` Implement Community Reviews, Cooksnaps & Interactive Ratings |
| **Tài liệu liên quan** | `specs/070-review-cooksnap/` (`spec.md`, `plan.md`, `tasks.md`, `data-model.md`, `contracts/openapi.yaml`), `.specify/memory/constitution.md`, `src/backend/wikicook-app/pom.xml`, Flyway `V501__community.sql`, mã nguồn Java package `com.wikicook.review` |
| **Công cụ AI & Model** | Google Antigravity (Gemini 2.5 Pro / Flash), Agentic Coding CLI & Sandbox Terminal |
| **Mục đích làm việc** | Chỉ đạo công cụ AI hiện thực toàn bộ mã nguồn Backend cho Feature 070 trên nhánh `feature/SCRUM-77-review-backend`, áp đặt tuyệt đối các quy tắc của dự án WikiCook và kiểm soát chất lượng qua chu trình TDD. |
| **Nhánh Git** | `feature/SCRUM-77-review-backend` |
| **Minh chứng (Evidence)** | Commit `ef55350`, 53/53 unit tests pass (Surefire `BUILD SUCCESS`), JaCoCo line coverage đạt 76.08% (353/464 dòng) tại `target/site/jacoco/index.html`, ảnh xác thực tài khoản Google Antigravity tại `docs/assets/jira/ai_accounts/04_tran_nguyen_cong_chung_antigravity.jpg`. |

#### Dòng tóm tắt theo mẫu AI Usage Notes
**Google Antigravity.** *Gemini 2.5 Pro / Flash, Google DeepMind, Antigravity IDE & CLI*, truy cập 08:30 đến 15:40 ngày 16/11/2026; prompt: thiết lập 3 giai đoạn khởi động bắt buộc và 9 ràng buộc kiến trúc bất di bất dịch (Pessimistic Locking, không bump version recipe, TwelveMonkeys gỡ 100% EXIF/GPS, TransactionSynchronization dọn file, JaCoCo $\ge 70\%$); AI sinh mã nguồn Flyway V501, Domain Entities, DTOs Record, Services, Controllers và 53 tests; sinh viên trực tiếp thẩm định cấu trúc package, kiểm tra logic chống race condition, kiểm toán báo cáo JaCoCo và nghiệm thu 38 task trong `tasks.md`.

---

### 13.2. Chuỗi câu lệnh / Prompt đã sử dụng (Prompts Used)

```text
/goal 
Chào bạn, tôi là Trần Nguyễn Công Chung (@itzchugnn - 24120272), đảm nhiệm vai trò QA Lead, Compliance Auditor & Backend Lead cho Feature F-07 (Member E) trong dự án WikiCook (CSC13002).

Tôi đang tiếp tục phiên làm việc để hiện thực mã nguồn Backend cho nhiệm vụ SCRUM-77: Backend Implementation cho Feature 070 (Community Reviews, Cooksnaps & Interactive Ratings) trên nhánh làm việc: feature/SCRUM-77-review-backend.

TRƯỚC KHI VIẾT CODE HOẶC ĐỀ XUẤT BẤT KỲ THAY ĐỔI NÀO, BẠN BẮT BUỘC PHẢI THỰC HIỆN 3 GIAI ĐOẠN KHỞI ĐỘNG SAU:

GIAI ĐOẠN 1: ĐỌC VÀ NẮM VỮNG HỒ SƠ THIẾT KẾ & QUY TẮC BẤT DI BẤT DỊCH
Hãy chủ động đọc kỹ các tài liệu kỹ thuật sau trong repository:
1. .specify/memory/constitution.md (Hiến pháp WikiCook: TDD Test-First, JaCoCo >= 70%, bảo mật EXIF/GPS, không rò rỉ JPA Entity).
2. specs/070-review-cooksnap/tasks.md (Danh mục 80 nhiệm vụ T001 -> T080, lưu ý đồng bộ song song với src/specs/070-review-cooksnap/tasks.md).
3. specs/070-review-cooksnap/plan.md (Kế hoạch kiến trúc phân lớp, cấu trúc package, và giải pháp kỹ thuật).
4. specs/070-review-cooksnap/data-model.md (Lược đồ DB Flyway V501, JPA Entities, Record DTOs).
5. specs/070-review-cooksnap/contracts/openapi.yaml (Hợp đồng API chuẩn 6 endpoints + report).

CÁC NGUYÊN TẮC VÀ RÀNG BUỘC KỸ THUẬT QUAN TRỌNG (TUYỆT ĐỐI KHÔNG VI PHẠM):
- Đồng bộ rating không làm tăng recipes.version (tránh lỗi 409 sai cho tác giả công thức).
- Khóa dòng bi quan (Pessimistic Locking): gọi recipeRepository.lockById(recipeId) (SELECT ... FOR UPDATE) trước khi ghi review.
- Chủ sở hữu xóa review = Hard Delete (giải phóng unique index để được review lại); HIDDEN CHỈ dùng cho Kiểm duyệt (Admin Moderation).
- Bảo mật Media: GET /api/v1/media/cooksnaps/{storageKey} chỉ trả file khi CẢ Cooksnap và Review cha đều ở trạng thái VISIBLE.
- Gỡ bỏ 100% EXIF/GPS tags bảo vệ riêng tư nhà riêng (TwelveMonkeys), nén WebP, tạo thumbnail 400x400.
- Bảng cooksnaps bắt buộc có cột review_id BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE.
- Tên cột chuẩn: storage_key, status, substitution_tip. Tuyệt đối KHÔNG dùng: photo_url, is_hidden, deleted_at.
- Không dùng trigger DB hay @UpdateTimestamp tự động đổi updated_at trên bảng reviews.
- TDD Test-First: viết Unit Test & Testcontainers PostgreSQL 16 trước khi viết code chạy, cam kết JaCoCo service layer >= 70%.

GIAI ĐOẠN 2: KIỂM TRA TIẾN ĐỘ THỰC TẾ & TRẠNG THÁI SOURCE CODE HIỆN TẠI...
GIAI ĐOẠN 3: BÁO CÁO TỔNG QUAN & BẮT ĐẦU TRIỂN KHAI CÁC BƯỚC TIẾP THEO...
```

---

### 13.3. Nội dung do AI tạo ra (Which Content Was Generated by AI)
- Bản phác thảo mã nguồn di trú cơ sở dữ liệu Flyway `V501__community.sql` (bảng `reviews`, `cooksnaps`, `helpful_votes` kèm các chỉ mục và ràng buộc toàn vẹn).
- Khung lớp thực thể JPA Entities (`ReviewEntity`, `CooksnapEntity`, `HelpfulVoteEntity`) và các DTO dạng Java Record tuân thủ nguyên tắc không rò rỉ Entity ra ngoài tầng Web.
- Các lớp dịch vụ xử lý nghiệp vụ (`ReviewServiceImpl`, `CooksnapServiceImpl`, `LocalStorageServiceImpl`) và các REST Controller theo OpenAPI contract.
- Bộ 53 ca kiểm thử tự động (Unit Tests & Integration Tests) bao gồm kiểm thử logic tính rating, kiểm thử xác thực quyền tác giả và kiểm thử cascade delete.

---

### 13.4. Sự can thiệp chuyên môn & Kiểm chứng của sinh viên (Human Verification & Editing)

#### Bảng quyết định kỹ thuật của sinh viên (Sử dụng / Hiệu chỉnh / Bác bỏ)

| Thành phần mã nguồn | Quyết định của sinh viên | Phân tích lý do kỹ thuật & Can thiệp chuyên môn |
|---|---|---|
| **Cơ chế đồng bộ Rating** | **Bác bỏ** đề xuất dùng JPA `save()` thông thường; **Bắt buộc** dùng Native SQL Update | Nếu dùng JPA Entity cập nhật điểm rating trung bình vào bảng `recipes`, cơ chế `@Version` sẽ tự động tăng version. Điều này dẫn đến việc tác giả công thức khi đang chỉnh sửa món ăn sẽ bị lỗi xung đột `409 OptimisticLockException`. Sinh viên yêu cầu viết hàm cập nhật riêng bằng native query không đụng chạm đến version. |
| **Xử lý Race Condition khi ghi nhận Review** | **Bắt buộc áp dụng** Pessimistic Lock (`PESSIMISTIC_WRITE`) | Khi nhiều người dùng cùng lúc gửi đánh giá cho 1 công thức, việc tính toán lại `rating_avg` và `review_count` sẽ bị sai lệch (lost update). Sinh viên chỉ đạo gọi `recipeRepository.lockById(recipeId)` phát lệnh `SELECT ... FOR UPDATE` trước khi thực hiện ghi dữ liệu. |
| **Bảo vệ quyền riêng tư ảnh Cooksnap** | **Bác bỏ** việc lưu trực tiếp file gốc; **Bắt buộc** bóc tách EXIF qua TwelveMonkeys | Ảnh chụp từ smartphone luôn chứa tọa độ GPS và thông tin thiết bị, gây lộ địa chỉ nhà riêng người dùng (vi phạm Hiến pháp IV). Sinh viên chỉ định rõ giải pháp dùng TwelveMonkeys loại bỏ metadata cấp độ nhị phân trước khi nén WebP và tạo thumbnail $400\times 400$. |
| **Quản lý vòng đời tệp tin đĩa cứng** | **Áp đặt** `TransactionSynchronization` | Tránh tình trạng file còn tồn tại trên đĩa khi giao dịch cơ sở dữ liệu bị Rollback. Sinh viên yêu cầu triển khai `LocalStorageServiceImpl` gắn liền với transaction Spring: chỉ lưu vĩnh viễn khi Commit thành công và tự xóa file rác nếu Rollback. |
| **Toàn vẹn xóa dữ liệu** | **Phân định rõ ràng**: Hard Delete vs Moderation Hidden | Khi người dùng xóa review của mình, phải là Hard Delete (xóa sạch bản ghi và file liên quan) để giải phóng Partial Unique Index `(recipe_id, user_id)`, cho phép họ đánh giá lại sau này. Trạng thái `HIDDEN` tuyệt đối chỉ dành cho kiểm duyệt viên (`Admin Moderation`). |

#### Kiểm chứng chất lượng & Nghiệm thu
- **Kiểm tra kiến trúc:** Trực tiếp yêu cầu AI giải trình vai trò của từng file và thư mục trong `com.wikicook.review`, khẳng định 100% Controller trả về Record DTOs, không rò rỉ JPA Entity ra ngoài.
- **Kiểm toán độ phủ (JaCoCo Audit):** Trực tiếp kiểm tra file `target/site/jacoco/index.html`, xác nhận tầng `com.wikicook.review.service` đạt **76.08%** độ phủ dòng (353 / 464 dòng), vượt tiêu chuẩn tối thiểu $\ge 70\%$.
- **Đồng bộ Spec Kit:** Kiểm tra đối chiếu và đánh dấu hoàn thành 38 nhiệm vụ backend trong cả 2 file `specs/070-review-cooksnap/tasks.md` và `src/specs/070-review-cooksnap/tasks.md` với độ lệch bằng 0 (`zero diff`).

---

## Mục 14: Chỉ huy gỡ lỗi CI Integration Test, xử lý xung đột Spring Security filter chain và thẩm định conflict PR #98 (`SCRUM-77`, `SCRUM-78`)

### 14.1. Thông tin phiên làm việc (Session Metadata)

| Trường | Nội dung |
|---|---|
| **Mục** | **Mục 14** \| **Ngày thực hiện:** 2026-11-16 – 2026-11-18 (15:40 ngày 16/11 – 21:45 ngày 18/11) |
| **Người thực hiện** | Trần Nguyễn Công Chung (`itzchugnn` - MSSV: 24120272) — QA Lead, Compliance Auditor & Backend Lead Member E |
| **Nhiệm vụ / Task Jira** | `SCRUM-77` (Backend PR #93) & `SCRUM-78` (Frontend PR #98 QA Review) |
| **Tài liệu liên quan** | `src/backend/wikicook-app/pom.xml`, `.github/workflows/backend.yml`, `PublicRoutes.java`, `ReviewCurrentUserId.java`, `WikiCookApplicationIT.java`, `CooksnapMediaControllerIT.java`, `ReviewRepositoryIT.java`, `src/frontend/src/app/routes.tsx`, `MyAccountPage.tsx` |
| **Công cụ AI & Model** | Google Antigravity (Gemini 2.5 Pro / Flash), Git CLI |
| **Mục đích làm việc** | Chỉ huy khắc phục xung đột khi đồng bộ nhánh `main`, trực tiếp chẩn đoán và khắc phục 6 lỗi kiểm thử tích hợp (Failsafe Integration Tests) do xung đột bộ lọc Spring Security trên GitHub Actions CI, và thẩm định phát hiện điểm nghẽn xung đột cho PR #98 của Frontend. |
| **Nhánh Git** | `feature/SCRUM-77-review-backend`, nhánh thẩm định `pr-98` |
| **Minh chứng (Evidence)** | Commit `c50ce4e`, `9073af1`, `2bc29b2`, `92fc710`; PR #93 merge thành công vào `main` (commit `355bfcb`); PR #98 giải quyết dứt điểm conflict và merge vào `main` (commit `0ba3e2f`), ảnh xác thực tài khoản Google Antigravity tại `docs/assets/jira/ai_accounts/04_tran_nguyen_cong_chung_antigravity.jpg`. |

#### Dòng tóm tắt theo mẫu AI Usage Notes
**Google Antigravity.** *Gemini 2.5 Pro / Flash, Google DeepMind*, truy cập các ngày 16/11 đến 18/11/2026; prompt: chỉ đạo giải quyết xung đột khi merge `main`, phân tích tĩnh tệp CI workflow, cung cấp log Failsafe, yêu cầu bóc tách nguyên nhân Spring Security chặn mã 401 trên route công khai, và kéo PR #98 về máy để kiểm toán conflict; AI phân tích nguyên nhân và đề xuất code; sinh viên trực tiếp phát hiện lỗi ngầm `UnauthenticatedException` trong `ReviewCurrentUserId.java`, sửa quan hệ hai chiều Hibernate Cascade trong `ReviewRepositoryIT`, duyệt commit `92fc710` đưa PR #93 pass 100% CI, và chỉ ra vị trí xung đột trong `routes.tsx` cho tác giả Frontend PR #98.

---

### 14.2. Chuỗi câu lệnh / Prompt đã sử dụng (Prompts Used)

```text
Prompt 1 (Phân tích phân kỳ nhánh và xung đột phụ thuộc khi đồng bộ origin/main):
"Hãy kiểm tra tình trạng của nhánh feature/SCRUM-77-review-backend và origin/main. Phân tích cây lịch sử git log, kiểm tra xem việc merge các PR vừa qua có gây xung đột trong tệp cấu hình Maven pom.xml hoặc các bean cấu hình toàn cục hay không. Báo cáo chi tiết các file bị conflict và giải pháp giữ nguyên tính toàn vẹn của Feature 070."

Prompt 2 (Chẩn đoán lỗi CI Failsafe: Bóc tách SecurityFilterChain, RFC 7807 và Hibernate Persistence Context):
"[INFO] Results:
[ERROR] Failures: 
[ERROR]   WikiCookApplicationIT.appliesAllMigrations:29 expected: 403 but was: 501
[ERROR]   CooksnapMediaControllerIT.testHiddenCooksnapReturns404:127 Status expected:<404> but was:<401>
[ERROR]   CooksnapMediaControllerIT.testServeCooksnapWebp:88 Status expected:<200> but was:<401>
[ERROR]   ReviewControllerIT.testGetRecipeReviewsPublic:172 Status expected:<200> but was:<401>
[ERROR]   ReviewControllerIT.testToggleHelpfulVote:209 JSON path "$.type" expected:<https://wikicook.com/errors/self-helpful-vote-forbidden> but was:<https://wikicook.com/errors/self-upvote-forbidden>
[ERROR] Errors: 
[ERROR]   ReviewRepositoryIT.testCascadeDeleteCooksnaps:104 » InvalidDataAccessApiUsage org.hibernate.TransientObjectException

Tôi đã trích xuất log lỗi từ GitHub Actions CI. Qua phân tích ban đầu, có 4 nhóm vấn đề kỹ thuật cốt lõi cần giải quyết:
1. Xung đột Spring Security: Module Auth (PR #89) đã thắt chặt SecurityFilterChain mặc định chặn /api/** với mã 401. Hãy đối chiếu PublicRoutes.java và cấu hình lại để đưa endpoint tải ảnh cooksnap vào BEARER_IGNORED và endpoint xem review công khai vào OPTIONALLY_SIGNED_IN đúng theo OpenAPI contract.
2. Lỗi ngữ cảnh người dùng unauthenticated: Kiểm tra lớp ReviewCurrentUserId.java, đảm bảo khi khách vãng lai gọi getOptionalId() thì không được để CurrentUser.id() ném UnauthenticatedException ra ngoài Controller.
3. Lệch chuẩn URI ProblemDetails RFC 7807: Cập nhật ReviewExceptionHandler đồng bộ chuẩn URI self-helpful-vote-forbidden.
4. Vi phạm Hibernate Persistence Context: Trong ReviewRepositoryIT, lỗi TransientObjectException xảy ra do quan hệ hai chiều giữa Review và Cooksnap chưa được đồng bộ trước khi xóa cascading. Hãy thiết lập quan hệ hai chiều đầy đủ trong test setup.
Yêu cầu thực hiện sửa đổi chính xác, không làm suy giảm tỷ lệ test coverage JaCoCo đã đạt."

Prompt 3 (Nghiệm thu chất lượng, kiểm toán hồi quy và an toàn commit):
"Rà soát toàn diện lại toàn bộ diff của các file vừa chỉnh sửa: PublicRoutes.java, ReviewCurrentUserId.java, ReviewExceptionHandler.java, WikiCookApplicationIT.java và ReviewRepositoryIT.java. Xác nhận không còn code thừa, không vi phạm nguyên tắc Clean Architecture, và không phát sinh hồi quy trước khi tôi tạo commit đẩy lên PR #93."

Prompt 4 (Thẩm định chéo QA Lead, bóc tách xung đột AST/Route trên PR #98 Frontend):
"Với tư cách QA Lead nghiệm thu Feature 070, hãy fetch pull request #98 (refs/pull/98/head) về nhánh cục bộ pr-98. Chúng ta cần đối chiếu toàn bộ 11 test suites Frontend (MSW mock handler, Vitest) với OpenAPI contract Backend đã merge.
Đặc biệt, GitHub đang cảnh báo conflict với main. Hãy thực hiện phân tích 3 chiều (three-way merge tree) để bóc tách chính xác từng dòng xung đột tại src/frontend/src/app/routes.tsx và MyAccountPage.tsx. Đưa ra khuyến nghị kỹ thuật giải quyết xung đột (giữ cả hai route của Feature 070 và Feature 011, chọn CSS Tailwind responsive) để tôi hướng dẫn tác giả Frontend fix và hoàn tất merge."

Prompt 5 (Dọn dẹp môi trường kiểm thử và bảo toàn nhánh làm việc):
"Kiểm tra lại toàn bộ trạng thái của PR #98 trên remote sau khi tác giả đã merge commit giải quyết conflict. Khi PR đã đạt trạng thái 'Clean / Able to merge', hãy chuyển con trỏ git về lại nhánh làm việc chính feature/SCRUM-77-review-backend, xóa nhánh tạm pr-98 để đảm bảo workspace sạch sẽ, và báo cáo để tôi tiến hành duyệt PR trên GitHub."
```

---

### 14.3. Nội dung do AI tạo ra (Which Content Was Generated by AI)
- Báo cáo phân tích nguyên nhân gốc rễ (Root-Cause Analysis) của 6 lỗi Failsafe Integration Tests từ tệp log build và cấu hình `.github/workflows/backend.yml`.
- Bản thảo bổ sung danh sách route công khai cho `PublicRoutes.java` và bản cập nhật xử lý ngoại lệ ProblemDetails RFC 7807 trong `ReviewExceptionHandler.java`.
- Bảng phân tích chi tiết các điểm xung đột mã nguồn 3 chiều (`git merge-tree`) trên PR #98 tại `routes.tsx` và `MyAccountPage.tsx`.

---

### 14.4. Sự can thiệp chuyên môn & Kiểm chứng của sinh viên (Human Verification & Editing)

Quá trình đưa mã nguồn lên nhánh `main` gặp phải các bài toán hóc búa về kiến trúc phân tầng và bảo mật, sinh viên đã trực tiếp can thiệp giải quyết 5 nhóm sự cố kỹ thuật phức tạp:

1. **Chế ngự hành vi phỏng đoán của AI & Chuyển sang phân tích tĩnh (Static Failure Analysis):**
   - Khi GitHub Actions báo `backend / verify` bị fail sau 2 phút, AI có dấu hiệu khởi chạy các tiến trình ngầm để biên dịch lại mã nguồn, gây treo máy cục bộ do xung đột byte-buddy trên môi trường sandbox.
   - Sinh viên đã dứt khoát chỉ đạo dừng việc chạy ngầm, buộc AI phải trích xuất tệp cấu hình `.github/workflows/backend.yml` và phân tích trực tiếp từ cây lệnh `mvn -B verify` để khoanh vùng chính xác lỗi nằm ở pha `maven-failsafe-plugin` (Integration Test).

2. **Chẩn đoán xung đột kiến trúc bảo mật giữa Module 070 và Module 010 (Auth):**
   - *Bản chất lỗi:* Nhánh `main` vừa tích hợp PR #89 (SCRUM-74 Auth) đưa vào `SecurityConfig` với quy tắc mặc định chặn toàn bộ `/api/**` nếu không nằm trong `PublicRoutes.java`. Trong khi đó, Feature 070 quy định hai endpoint công khai cho khách xem không cần đăng nhập (`GET /api/v1/media/cooksnaps/**` và `GET /api/v1/recipes/*/reviews`).
   - *Can thiệp của sinh viên:* Chỉ đạo bổ sung chính xác 2 route này vào `PublicRoutes.java` (đưa ảnh vào `BEARER_IGNORED` và danh sách đánh giá vào `OPTIONALLY_SIGNED_IN`), bảo đảm đúng tiêu chuẩn OpenAPI `security: []`.

3. **Bóc tách lỗi tiềm ẩn tinh vi trong `ReviewCurrentUserId.java`:**
   - *Bản chất lỗi:* Mặc dù route đã mở trong `PublicRoutes`, các bài test `RouteProtectionIT` và `ReviewControllerIT` vẫn bị trả về mã lỗi 401 khi khách vãng lai gọi `GET /recipes/1/reviews`.
   - *Phát hiện đột phá của sinh viên:* Sinh viên soi vào hợp đồng của interface `CurrentUser`: phương thức `user.id()` sẽ chủ động ném ngoại lệ `UnauthenticatedException` khi người dùng chưa đăng nhập. Trong lớp `ReviewCurrentUserId`, phương thức `getOptionalId()` chỉ kiểm tra `user != null` rồi gọi thẳng `user.id()`. Khi khách vãng lai truy cập, ngoại lệ này bị quăng ra Controller và kích hoạt bộ lọc bảo mật trả về 401.
   - *Giải pháp triệt để:* Sinh viên yêu cầu bọc khối `try-catch (Exception ex)` bên trong `getOptionalId()`, trả về `null` một cách an toàn cho khách vãng lai, đưa bài test chuyển ngay sang mã **200 OK**.

4. **Sửa lỗi vi phạm Persistence Context của Hibernate (`TransientObjectException`):**
   - Trong bài test `ReviewRepositoryIT.testCascadeDeleteCooksnaps`, đối tượng `Cooksnap` trỏ đến `savedReview` nhưng danh sách con `savedReview.getCooksnaps()` chưa được cập nhật trong bộ nhớ. Khi lệnh xóa review được kích hoạt, Hibernate phát hiện thực thể con trỏ vào một instance đang bị xóa dở và ném ngoại lệ. Sinh viên trực tiếp chỉ đạo thiết lập quan hệ hai chiều hoàn chỉnh trước khi gọi `delete()`.

5. **Phát hiện và xử lý xung đột phân nhánh trên PR #98 của Frontend:**
   - Với vai trò QA Lead thẩm định PR #98 của Duy, khi thấy GitHub báo conflict, sinh viên đã dùng lệnh `git merge-tree` bóc tách từng dòng xung đột:
     - Tại `routes.tsx`: Xung đột giữa route xem review `recipes/:id` (Feature 070) và route xem hồ sơ cá nhân `profiles/:id` (Feature 011). Sinh viên đưa ra quyết định kiến trúc: **giữ lại cả hai route**.
     - Tại `MyAccountPage.tsx`: Nhánh `main` dùng inline style cũ, trong khi PR #98 đã nâng cấp lên Tailwind responsive. Sinh viên chỉ đạo **chọn phiên bản class Tailwind**.
   - Hướng dẫn tác giả Duy thực hiện merge đúng chuẩn, đưa PR #98 đạt trạng thái xanh ("Able to merge") và tiến hành phê duyệt.

#### Đánh giá kết quả & Tuân thủ Liêm chính học thuật
- **Kết quả nghiệm thu:** 
  - Toàn bộ 430 integration test trên GitHub Actions chạy thành công 100% (`BUILD SUCCESS`).
  - PR #93 (Backend SCRUM-77) và PR #98 (Frontend SCRUM-78) đã merge an toàn vào nhánh `main`.
- **Vai trò con người (Human-in-the-Loop):** Khẳng định sinh viên nắm quyền chủ động 100%, trực tiếp đưa ra các quyết định kiến trúc cốt lõi, bắt lỗi công cụ AI, bảo vệ nghiêm ngặt tính toàn vẹn của mã nguồn và hoàn thành đúng hạn các cam kết của Sprint 04.

---

## Mục 15: Thẩm định kiểm thử toàn hệ thống (mvn verify), chuẩn hóa JaCoCo, hoàn tất báo cáo và sản xuất video demo YouTube (`SCRUM-79`)

### 15.1. Thông tin phiên làm việc (Session Metadata)

| Trường | Nội dung |
|---|---|
| **Mục** | **Mục 15** \| **Ngày thực hiện:** 2026-11-20 – 2026-11-21 (14:00 ngày 20/11 – 22:30 ngày 21/11) |
| **Người thực hiện** | Trần Nguyễn Công Chung (`itzchugnn` - MSSV: 24120272) — QA Lead, Compliance Auditor & Member E |
| **Nhiệm vụ / Task Jira** | `SCRUM-79` — Verify Spec Kit Tests & Record YouTube Video Demo |
| **Tài liệu liên quan** | `docs/test/scrum-79-spec-kit-test-report.md`, `README.md`, `src/backend/wikicook-app/pom.xml`, `jacoco.csv`, storyboard 7 phân cảnh, YouTube video: `https://www.youtube.com/watch?v=PS3sbgVm_jI` |
| **Công cụ AI & Model** | Google Antigravity (Gemini 2.5 Pro / Flash), Windows Shell / Git CLI |
| **Mục đích làm việc** | Chỉ đạo công cụ AI cấu trúc và hoàn thiện báo cáo kiểm thử hệ thống Part E (`scrum-79-spec-kit-test-report.md`), tổng hợp dữ liệu JaCoCo và kết quả đo kiểm `mvn verify` thực tế từ PM Hưng (ngày 21/11), chỉnh sửa các sai lệch trong kịch bản storyboard, đồng bộ tài liệu `README.md` và sản xuất video minh chứng End-to-End đăng tải lên YouTube. |
| **Nhánh Git** | `docs/SCRUM-79-verification-demo` (PR #108) |
| **Minh chứng (Evidence)** | Commits `e7fd8ae`, `2fc6943`, `49915b6`, `2325bbc`, `21a4000`; PR #108 được Mai Văn Hiển (`@MaiHien3507`) duyệt (Approve) và merge (`64b23bf`); video YouTube 1080p có thuyết minh tại `https://www.youtube.com/watch?v=PS3sbgVm_jI`. |

#### Dòng tóm tắt theo mẫu AI Usage Notes
**Google Antigravity.** *Gemini 2.5 Pro / Flash, Google DeepMind*, truy cập các ngày 20/11 đến 21/11/2026; prompt: yêu cầu cấu trúc báo cáo kiểm thử 5 phần theo chuẩn RUP, trích xuất dữ liệu độ phủ JaCoCo theo số dòng thực tế, phát hiện và sửa các điểm sai lệch trong storyboard API (endpoint xác thực email, trường `household_size` trong DB), đối soát kết quả `mvn verify` (1.410 tests tổng gồm unit + integration, trong đó `mvn test` tại `wikicook-app` đạt 654 unit tests) đo kiểm ngày 21/11, và tích hợp link video demo YouTube vào `README.md`; AI tổng hợp số liệu và sinh nội dung báo cáo; sinh viên trực tiếp phát hiện và loại bỏ số liệu ước lượng mơ hồ ("about 97%"), sửa lại đường dẫn endpoint, kiểm chứng 7 phân cảnh demo, biên tập lồng tiếng video thực tế và ký duyệt biên bản kiểm thử.

---

### 15.2. Chuỗi câu lệnh / Prompt đã sử dụng (Prompts Used)

```text
Prompt 1 (Thiết lập cấu trúc báo cáo kiểm thử Part E & Kịch bản Storyboard 7 phân cảnh):
"Tôi là Chung (@itzchugnn), QA Lead của WikiCook. Để phục vụ đánh giá nghiệm thu Part E (30 điểm) cho hai nhóm tính năng F-01 (010+011) và F-07 (070), hãy khởi tạo cấu trúc báo cáo docs/test/scrum-79-spec-kit-test-report.md. Báo cáo phải bao gồm:
1. Tổng quan phạm vi nghiệm thu đối chiếu với Spec Kit và OpenAPI contracts.
2. Ma trận kết quả kiểm thử tự động (Unit Test, Integration Test, Testcontainers, Vitest, MSW).
3. Phân tích độ phủ mã nguồn JaCoCo chi tiết cho 3 service cốt lõi: auth.service, profile.service, và review.service.
4. Kịch bản Storyboard chi tiết từng bước cho video demo (7 phân cảnh, 4 phút 30 giây) từ Đăng ký, Xác thực email, Đăng nhập, Hồ sơ ăn uống & Dị ứng đến Viết review, Upload Cooksnap bóc tách EXIF/GPS, Lightbox, Sửa và Xóa đánh giá.
5. Biên bản ký duyệt nghiệm thu chất lượng (Quality Gate Sign-off).
Yêu cầu: Không dùng số liệu giả lập vô căn cứ; mọi API endpoint và thuộc tính DB phải đối chiếu chính xác với mã nguồn trên nhánh main."

Prompt 2 (Phản hồi review của PM: Hiệu chỉnh số liệu JaCoCo thực tế, chuẩn hóa endpoint xác thực và thuộc tính DB):
"PM Lê Quốc Hưng vừa hoàn thành review PR #108 và yêu cầu hiệu chỉnh 5 điểm kỹ thuật sau:
1. Số liệu kiểm thử: Ghi nhận rõ số liệu mvn verify chạy ngày 21/11 đạt tổng 1.410 tests (BUILD SUCCESS, 2 skipped: wikicook-app gồm 654 unit + 669 integration; wikicook-ai gồm 60 unit + 27 integration) trên main và 654 unit tests khi chạy mvn test trực tiếp, cùng 918 component tests Frontend (120 files).
2. Độ phủ JaCoCo: Tuyệt đối không dùng ước lượng 'about 97%' hay 'about 93%'. Bắt buộc đưa ra số dòng chính xác (covered/total lines) đo kiểm trên main: auth.service đạt 852/871 dòng (97.82%), profile.service đạt 353/379 dòng (93.14%), review.service đạt 358/464 dòng (77.16%).
3. Sửa Storyboard: Endpoint xác thực email phải là 'POST /api/v1/auth/verification/confirm' (không dùng confirm-email); trường quy mô hộ gia đình trong bảng users là 'household_size' dạng INTEGER (không phải servings).
4. Đồng bộ README.md: Bổ sung liên kết video YouTube demo 'https://www.youtube.com/watch?v=PS3sbgVm_jI' và tiêu đề chuẩn 'WikiCook - Part E System Verification & End-to-End Demo - Group 05' vào Section 4 của README.md.
5. Cập nhật dòng ký duyệt của PM: 'watched the demo video and approved on 21/11/2026'.
Hãy áp dụng toàn bộ các chỉnh sửa trên vào scrum-79-spec-kit-test-report.md và README.md."
```

---

### 15.3. Nội dung do AI tạo ra (Which Content Was Generated by AI)
- Bộ khung tài liệu `docs/test/scrum-79-spec-kit-test-report.md` gồm 5 phần chi tiết, bảng ma trận ánh xạ 7 phân cảnh storyboard với mã nguồn API và giao diện React.
- Bảng thống kê độ phủ kiểm thử JaCoCo và bảng tổng hợp kết quả chạy test multi-module Maven.
- Đoạn Markdown nhúng liên kết và metadata của video demo YouTube trong `README.md`.

---

### 15.4. Sự can thiệp chuyên môn & Kiểm chứng của sinh viên (Human Verification & Editing)

#### Bảng quyết định kỹ thuật của sinh viên (Sử dụng / Hiệu chỉnh / Bác bỏ)

| Thành phần tài liệu & Dữ liệu | Quyết định của sinh viên | Phân tích lý do kỹ thuật & Can thiệp chuyên môn |
|---|---|---|
| **Dữ liệu độ phủ mã nguồn JaCoCo** | **Bác bỏ** việc dùng ước lượng phần trăm xấp xỉ; **Bắt buộc** lấy số dòng thực tế | AI ban đầu đưa ra nhận xét chung chung như "khoảng 97%" hay "khoảng 93%". Trong thẩm định QA, việc ước lượng làm mất đi tính tin cậy học thuật. Sinh viên đối chiếu kết quả đo kiểm JaCoCo trên `main`, đưa vào tỷ lệ chính xác từng dòng: `auth.service` (852/871 dòng = 97.82%), `profile.service` (353/379 dòng = 93.14%), và `review.service` (358/464 dòng = 77.16%; unit test độc lập đạt 353/464 dòng = 76.08%), khẳng định cả 3 module đều vượt xa rào chắn $\ge 70\%$. |
| **Endpoint xác thực Email trong Storyboard** | **Hiệu chỉnh bắt buộc** sang URI chuẩn | Bản thảo storyboard của AI ghi nhầm URI thành `/api/v1/auth/confirm-email`. Sinh viên soi vào controller `AuthVerificationController.java` và OpenAPI spec, phát hiện endpoint thật sự là `POST /api/v1/auth/verification/confirm`. Sinh viên chỉ đạo sửa lại ngay để kịch bản demo khớp 100% với backend. |
| **Thuộc tính quy mô hộ gia đình** | **Sửa lỗi sai lệch Schema CSDL** | AI mô tả trong phân cảnh hồ sơ người dùng là cập nhật "servings". Sinh viên đối chiếu với schema bảng `users` tại migration Flyway `V201`, phát hiện tên cột chính xác là `household_size` kiểu `INTEGER`. Sinh viên yêu cầu chuẩn hóa đúng danh pháp kỹ thuật. |
| **Phân định kết quả `mvn test` và `mvn verify`** | **Phân tách rạch ròi** ngữ cảnh đo kiểm | Nhận diện môi trường terminal cục bộ không đủ tài nguyên chạy toàn bộ Testcontainers nặng của cả dự án cùng lúc, sinh viên ghi rõ: bài kiểm tra cục bộ chạy `mvn test` đạt 654 unit tests, trong khi kết quả toàn diện `mvn verify` đạt tổng 1.410 tests (`wikicook-app`: 654 unit + 669 integration; `wikicook-ai`: 60 unit + 27 integration; 2 skipped) được đo kiểm độc lập bởi PM Hưng trên môi trường chuẩn Docker ngày 21/11/2026. |
| **Sản xuất video demo thực tế** | **Tự thực hiện 100% bằng video người thật** | Sinh viên tự dàn dựng môi trường, nạp dữ liệu hạt giống (seed data), thực hiện ghi hình 1080p toàn bộ 7 phân cảnh trong 4 phút 30 giây (từ đăng ký tài khoản, nhận mail kích hoạt, cập nhật hồ sơ dị ứng, viết review, bóc tách EXIF ảnh Cooksnap đến mở Lightbox, sửa và xóa đánh giá), lồng tiếng giải thích chuyên sâu và tải lên YouTube dưới chế độ Unlisted. |

---

## Mục 16: Kiểm toán đối chiếu Use-Case Spec v2.1/v2.1.1 (`SCRUM-71`), chuẩn hóa quan hệ UC-100-02 và thanh tra AI Log toàn đội (`SCRUM-85`)

### 16.1. Thông tin phiên làm việc (Session Metadata)

| Trường | Nội dung |
|---|---|
| **Mục** | **Mục 16** \| **Ngày thực hiện:** 2026-11-20 – 2026-11-25 (09:00 ngày 20/11 – 20:30 ngày 25/11) |
| **Người thực hiện** | Trần Nguyễn Công Chung (`itzchugnn` - MSSV: 24120272) — QA Lead, Compliance Auditor & Member E |
| **Nhiệm vụ / Task Jira** | `SCRUM-71` (Use-Case Spec v2.0 Review & Realignment), `SCRUM-85` (Five-Member AI Usage Report) & `SCRUM-86` (Full QA Deliverables Cross-Audit) |
| **Tài liệu liên quan** | `docs/requirements/use-case-specification.md`, `docs/requirements/use-case-model.md`, `docs/analysis-and-design/architecture.md`, `docs/ai_log/` (5 tệp log thành viên), `docs/management/reports/sprint-04/sprint-04-report.md` (Section 6 & 7) |
| **Công cụ AI & Model** | Google Antigravity (Gemini 2.5 Pro / Flash), Git CLI |
| **Mục đích làm việc** | Thực hiện kiểm toán toàn diện Use-Case Specification v2.0 cho các phần 9, 10, 12 đối chiếu với OpenAPI và mã nguồn trên `main`; giải quyết dứt điểm mâu thuẫn kiến trúc tại `UC-100-02` bằng quan hệ «include» `UC-070-08`; nâng tài liệu lên `v2.1` (PR #109); thẩm định bản ghi kiểm toán của PM Hưng tại `v2.1.1` (PR #116); đồng thời thanh tra toàn bộ nhật ký sử dụng AI của 5 thành viên (`SCRUM-85`) và kiểm toán chéo chất lượng bàn giao PA4 (`SCRUM-86`, PR #117). |
| **Nhánh Git** | `docs/SCRUM-71-uc-spec-review-confirmation` (PR #109), `docs/SCRUM-71-uc-spec-review-record` (PR #116), `docs/SCRUM-85-86-ai-and-qa-audit` (PR #117) |
| **Minh chứng (Evidence)** | PR #109 merged (commit `8b2008b`), PR #116 merged (commit `47aca2b`), PR #117 cập nhật Section 6 & Section 7 của `sprint-04-report.md` đồng bộ với `main` (`00539b4`). |

#### Dòng tóm tắt theo mẫu AI Usage Notes
**Google Antigravity.** *Gemini 2.5 Pro / Flash, Google DeepMind*, truy cập các ngày 20/11 đến 22/11/2026; prompt: đối chiếu toàn bộ các luồng của Mục 9, 10, 12 với OpenAPI và Spring Boot controller trên main, tái cấu trúc use case UC-100-02 để delegate việc tính toán rating sang use case UC-070-08 phù hợp với Use-Case Model v2.0, bóc tách 4 điểm kỹ thuật cốt lõi cho bản ghi review (sliding window 15m email rate limit, multipart profile update, không tăng recipe version khi aggregate rating), và quét toàn bộ 5 file `ai_log_<member>.md` để tổng hợp ma trận tuân thủ AI 5 thành viên; AI phân tích diff và đề xuất nội dung chỉnh sửa; sinh viên trực tiếp kiểm chứng mã nguồn thực tế, bắt lỗi bộ đếm 60s sai lệch, xác nhận UC-100-02 tuân thủ kiến trúc phân rã, phê duyệt PR #109 và #116, và thiết lập bảng kiểm toán AI 5 thành viên.

---

### 16.2. Chuỗi câu lệnh / Prompt đã sử dụng (Prompts Used)

```text
Prompt 1 (Đối soát Use-Case Spec v2.0 & Tái cấu trúc quan hệ UC-100-02 delegate sang UC-070-08):
"Tôi là Chung (@itzchugnn), QA Lead thực hiện nhiệm vụ SCRUM-71: Rà soát và đóng dấu thẩm định Use-Case Specification v2.0.
Hãy đối chiếu chi tiết 3 phần trong docs/requirements/use-case-specification.md:
- Mục 9 (Spec 010 Auth): UC-010-01 đến UC-010-05 so với contracts/openapi.yaml và package com.wikicook.auth.
- Mục 10 (Spec 011 Profile): UC-011-01 đến UC-011-06 so với contracts/openapi.yaml và package com.wikicook.profile.
- Mục 12 (Spec 070 Review): UC-070-01 đến UC-070-08 so với contracts/openapi.yaml, Flyway V501 và package com.wikicook.review.
Đặc biệt tại Mục 13 (UC-100-02 Process Content Reports): Bản v2.0 đang để Admin Service tự gọi recalculate trực tiếp. Điều này phá vỡ tính bao đóng của module Review. Hãy sửa đổi UC-100-02 để include 'UC-070-08 Update Recipe Rating Summary' đúng như thiết kế trong Use-Case Model v2.0 (§4.7 & §4.10). Sau đó xóa toàn bộ nhãn 'v2.0 review pending' tại các mục 9, 10, 12, nâng version header lên 2.1 và cập nhật Revision History."

Prompt 2 (Thẩm định bản ghi review của PM trong PR #116 & Kiểm chứng 4 điểm kỹ thuật cốt lõi):
"PM Hưng đã mở PR #116 trên nhánh docs/SCRUM-71-uc-spec-review-record để ghi nhận chi tiết biên bản rà soát của tôi dưới dạng khối ghi chú [!NOTE] Review record (v2.1.1).
Hãy chạy git diff đối chiếu nhánh của PR #116 với main tại 84844a6 và kiểm chứng 4 điểm hiệu chỉnh kỹ thuật:
1. Giới hạn gửi email: Kiểm tra EmailRequestLimiter trong com.wikicook.auth, xác nhận cơ chế 3 email / 15 phút (FR-017) và xác nhận không có bộ đếm cứng 60s.
2. Endpoint hồ sơ: Xác nhận MyProfileController dùng POST /api/v1/me/profile (multipart/form-data) và bảng user_allergens (V210). Xác nhận luồng reauth chưa cài đặt (OQ-011-09).
3. Thẩm định Review: Xác nhận rating 1-5, comment 10-2000 ký tự, max 3 ảnh Cooksnap và kiểm tra ReviewRepositoryIT (T015) xác nhận việc aggregate rating không làm tăng recipes.version.
4. Mô phỏng 3-way merge (git merge-tree): Xác nhận PR #116 không gây xung đột với main và không ảnh hưởng đến bất kỳ file nào khác."

Prompt 3 (Thanh tra nhật ký AI toàn nhóm & Thiết lập ma trận tuân thủ 5 thành viên - SCRUM-85):
"Thực hiện kiểm toán tuân thủ liêm chính học thuật cho nhiệm vụ SCRUM-85 (Compile Five-Member AI Usage Report):
Hãy quét toàn bộ thư mục docs/ai_log/ gồm 5 tệp: ai_log_hung.md, ai_log_duy.md, ai_log_hien.md, ai_log_chung.md, ai_log_thanhdu.md.
1. Kiểm tra 7 tiêu chí bắt buộc theo Mục 5 AI Usage Guidelines.pdf đối với các mục ghi nhận trong Sprint 04.
2. Tổng hợp ma trận số lượng mục đúng phạm vi Sprint 04 / PA4: Hưng (Mục 25–37), Duy (Mục 22–26), Hiển (Mục 26–27, 31–49), Chung (Mục 13–16), Dự (Mục 27–30 sau khi merge PR #121).
3. Trích xuất 10 phiên tương tác tiêu biểu đại diện cho 100% các phần A, B, C, D, E, F của đồ án PA4 và lập báo cáo QA Cross-Audit 9 chiều (SCRUM-86) với trạng thái Conditional Sign-Off chờ hoàn tất SCRUM-88 và SCRUM-90."
```

---

### 16.3. Nội dung do AI tạo ra (Which Content Was Generated by AI)
- Bản cập nhật sửa đổi `UC-100-02` trong `use-case-specification.md` liên kết sang `UC-070-08`.
- Báo cáo phân tích tĩnh đối chiếu Use Case với mã nguồn Spring Boot thực tế trên `main`.
- Bảng tổng hợp đối soát 4 điểm kỹ thuật và kết quả mô phỏng 3-way merge cho PR #116.
- Bảng ma trận thống kê trạng thái AI Log của 5 thành viên phục vụ task `SCRUM-85`.

---

### 16.4. Sự can thiệp chuyên môn & Kiểm chứng của sinh viên (Human Verification & Editing)

#### Bảng quyết định kỹ thuật của sinh viên (Sử dụng / Hiệu chỉnh / Bác bỏ)

| Vấn đề kiến trúc & Kiểm toán | Quyết định của sinh viên | Phân tích lý do kỹ thuật & Can thiệp chuyên môn |
|---|---|---|
| **Ranh giới tính toán Rating trong `UC-100-02`** | **Bác bỏ** việc gọi trực tiếp `recalculate()`; **Bắt buộc** áp dụng quan hệ «include» `UC-070-08` | Trong bản thảo v2.0, use case xử lý vi phạm của Admin tự ý can thiệp vào nghiệp vụ đánh giá. Sinh viên chỉ ra điều này vi phạm nguyên tắc Đóng gói (Encapsulation) và lệch với mô hình Use-Case Model v2.0 (§4.7, §4.10). Sinh viên yêu cầu sửa đổi luồng để `UC-100-02` include use case `UC-070-08 Update Recipe Rating Summary`. |
| **Xác thực cơ chế Rate Limit gửi email** | **Bác bỏ** giả định cooldown 60 giây; **Chuẩn hóa** theo cửa sổ trượt 15 phút | Khi xem xét bản nháp có đề cập đến "cooldown 60 giây", sinh viên trực tiếp soi mã nguồn lớp `EmailRequestLimiter.java` trong `com.wikicook.auth`. Backend chỉ cài đặt `MAX_REQUESTS = 3` trong khoảng thời gian trượt `WINDOW = 15 phút` (FR-017). Sinh viên chỉnh lý thông tin chính xác vào biên bản review. |
| **Bảo toàn Optimistic Locking của công thức món ăn** | **Xác nhận & Bảo vệ** quy tắc kiến trúc lõi | Sinh viên kiểm chứng lại bài test `ReviewRepositoryIT` (T015), khẳng định rằng `UC-070-08` khi đồng bộ `rating_avg` và `review_count` bắt buộc dùng Native Query cập nhật thẳng vào CSDL mà không tăng trường `recipes.version`, bảo vệ tác giả món ăn không bị văng lỗi 409 sai khi đang sửa công thức. |
| **Phương thức tải hồ sơ người dùng** | **Xác định chính xác** multipart vs JSON | Đối chiếu với `MyProfileController`, sinh viên xác nhận endpoint cập nhật hồ sơ cá nhân là `POST /api/v1/me/profile` nhận `multipart/form-data` (để hỗ trợ upload avatar), trong khi cập nhật chế độ ăn kiêng mới là `PUT /api/v1/me/dietary-profile` dạng JSON. |

#### Đánh giá kết quả & Tuân thủ Liêm chính học thuật
- **Kết quả nghiệm thu:** 
  - `use-case-specification.md` đạt chuẩn phiên bản `v2.1.1`, đồng bộ 100% với Use-Case Model v2.0 và mã nguồn trên `main`.
  - Hai PR `#109` và `#116` đã merge an toàn, đầy đủ bằng chứng kiểm toán được lưu vết công khai trên Git.
  - Bảng kiểm toán AI Log 5 thành viên được thiết lập sẵn sàng cho Báo cáo Sprint 04 (`SCRUM-85`).
- **Vai trò con người (Human-in-the-Loop):** Khẳng định sinh viên nắm vững toàn bộ kiến trúc hệ thống, trực tiếp đối chiếu mã nguồn, phát hiện và sửa chữa các sai sót kỹ thuật tinh vi của AI, không ỷ lại và luôn tuân thủ nghiêm ngặt quy chế học thuật của môn học.


