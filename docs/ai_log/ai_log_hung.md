# Nhật ký sử dụng AI – Lê Quốc Hưng

File này ghi lại toàn bộ lịch sử sử dụng công cụ AI của **Lê Quốc Hưng** (mã **A**, PM & Scrum Master, GitHub: `@LqHung06`) trong suốt quá trình phát triển dự án WikiCook, tuân thủ nghiêm ngặt quy chế học thuật của môn học tại `AI Usage Guidelines.pdf`. Mỗi lần sử dụng AI là một mục mới, mục mới nhất ở cuối.

| # | Ngày | Nội dung thực hiện | Module / Spec / Tài liệu |
|---|---|---|---|
| 1 | 2026-10-15 | Khởi tạo khung Software Development Plan (`SCRUM-28`) và Sprint 02 Report (`SCRUM-27`) | `docs/management/project-plan.md`, `reports/sprint-02/` |
| 2 | 2026-10-16 | Hợp nhất baseline tài liệu Project Plan v1.0, giải quyết xung đột nhánh và đồng bộ Jira Board | `docs/management/project-plan.md` |
| 3 | 2026-10-16 | Tạo đặc tả chức năng (Feature Specification) cho Chế độ nấu ăn rảnh tay & Đếm giờ song song bằng `/speckit-specify` | `specs/050-cooking-mode/` |
| 4 | 2026-10-16 | Làm rõ yêu cầu kỹ thuật và hành vi biên (Interactive Clarification) bằng `/speckit-clarify` | `specs/050-cooking-mode/` |
| 5 | 2026-10-16 | Tạo đặc tả chức năng (Feature Specification) cho Danh sách mua sắm thông minh & Gộp nguyên liệu (`040`) bằng `/speckit-specify` | `specs/040-shopping-list/` |
| 6 | 2026-10-16 | Làm rõ yêu cầu kỹ thuật và hành vi biên (Interactive Clarification) bằng `/speckit-clarify` | `specs/040-shopping-list/` |

---

## Mục 1: Khởi tạo khung Project Plan và Sprint 02 Report

| Trường | Nội dung |
|---|---|
| **Ngày** | 2026-10-15 |
| **Người thực hiện** | Lê Quốc Hưng (`LqHung06`) |
| **Tài liệu liên quan** | `docs/management/project-plan.md`, `docs/management/reports/sprint-02/sprint-02-report.md` |
| **Công cụ AI** | Google Antigravity (Gemini 3.8 Flash) |
| **Mục đích** | Thiết lập khung cấu trúc tài liệu quản lý dự án chuyển thể từ template RUP sang quy trình Scrum 5 Sprints, đồng bộ 18 tasks Jira từ `SCRUM-27` đến `SCRUM-45`. |
| **Nhánh Git** | `Hung` |

### Tóm tắt kết quả & Hiệu chỉnh của sinh viên:
- AI hỗ trợ sinh khung phân rã 18 tasks khớp với bảng Jira thực tế (bỏ qua `SCRUM-31`).
- Sinh viên rà soát, viết lại chi tiết phần Definition of Done (DoD 6 cổng), quy chế họp Scrum và biên bản cuộc họp Sprint 2 Planning ngày 14/10/2026.

---

## Mục 2: Hợp nhất baseline tài liệu Project Plan v1.0

| Trường | Nội dung |
|---|---|
| **Ngày** | 2026-10-16 |
| **Người thực hiện** | Lê Quốc Hưng (`LqHung06`) |
| **Tài liệu liên quan** | `docs/management/project-plan.md` |
| **Công cụ AI** | Google Antigravity (Gemini 3.8 Flash) |
| **Mục đích** | Phát hiện và xử lý lỗi nối đuôi trùng lặp giữa bản thảo của Hưng (`SCRUM-28`) và Hiển (`SCRUM-29`), bổ sung Risk Matrix (`SCRUM-30`) và Build Plan (`SCRUM-45`). |
| **Nhánh Git** | `Hung` |

### Tóm tắt kết quả & Hiệu chỉnh của sinh viên:
- AI rà soát toàn bộ 700 dòng của file cũ, loại bỏ phần tiêu đề lặp, tích hợp sơ đồ Mermaid Gantt và Critical Path.
- Sinh viên kiểm tra đối soát ma trận RACI, xác nhận lộ trình 5 bản build từ `v0.1` đến `v1.0 RC` phục vụ bài demo cuối kỳ ở PA5.

---

## Mục 3: Viết spec 050 (Hands-free Cooking Mode & Concurrent Timers) bằng `/speckit-specify`

| Trường | Nội dung |
|---|---|
| **Ngày** | 2026-10-16 |
| **Người thực hiện** | Lê Quốc Hưng (`LqHung06`) |
| **Spec liên quan** | 050 – Chế độ nấu ăn rảnh tay và Đếm giờ song song (`specs/050-cooking-mode/`) |
| **Công cụ AI** | Google Antigravity (Gemini 3.8 Flash), lệnh Spec Kit `/speckit-specify` |
| **Mục đích** | Soạn thảo bản đặc tả yêu cầu người dùng (`spec.md`) và checklist chất lượng (`checklists/requirements.md`) giải quyết trọn vẹn 8 rủi ro kỹ thuật nền tảng web. |
| **Nhánh Git** | `Hung` (chuẩn bị cho branch `feature/050-cooking-mode`) |

### Quá trình phân tích & Đánh giá 8 rủi ro kỹ thuật trước khi sinh spec:
Trước khi yêu cầu AI sinh file đặc tả, sinh viên đã tiến hành phản biện và yêu cầu AI xử lý 8 vấn đề kỹ thuật cốt lõi:
1. **Mâu thuẫn Giọng nói vs Chế độ Offline:** Web Speech API cần kết nối mạng để nhận diện tiếng Việt; do đó giọng nói được định nghĩa là tính năng tăng cường (Progressive Enhancement - P3), khi offline hệ thống tự tắt mic nhưng giữ 100% tính năng xem bước và timer.
2. **Tách biệt tiêu chí thành công của MVP và Tính năng nâng cao:** Không gộp "chuyển bước không chạm tay" vào tiêu chí chung để tránh việc MVP (P1+P2) bị đánh rớt; tách thành SC-001 (chạm tối giản với nút $\ge 64\text{px}$) và SC-003 (khi bật giọng nói).
3. **Thừa nhận thực tế các giới hạn nền tảng Web:** Nhận diện việc iOS Safari không hỗ trợ `navigator.vibrate`, Screen Wake Lock tự nhả khi ẩn tab, và chính sách Autoplay Policy của trình duyệt (giải quyết bằng việc coi nút "Bắt đầu nấu" là cử chỉ người dùng hợp lệ để kích hoạt AudioContext).
4. **Định lượng rõ ràng các tiêu chí đo lường:** Sai số timer $< 1.0\text{s}$ khi tab mở; độ chính xác giọng nói $\ge 85\%$ trong môi trường $\le 60\text{ dB}$; bảo toàn phiên gồm đúng `current_step_index` và `remaining_seconds`.
5. **Bảo vệ quyền riêng tư & Tránh nhận nhầm lệnh:** Sử dụng Push-to-Talk (bấm mic để nghe) thay vì Always-listening; nếu độ tin cậy nhận diện $< 70\%$ thì bỏ qua và nháy nhẹ viền vàng, tuyệt đối không nhảy bước bậy.
6. **Quy tắc phục hồi phiên (Session Recovery):** Thiết lập thời gian sống của phiên là 4 tiếng (TTL); lưu `localStorage` theo từng món ăn; timer hết giờ trong lúc đóng tab được hiển thị trạng thái "Đã quá hạn X phút" màu đỏ.
7. **Nhận diện thời gian trong văn bản:** Parse các mẫu chuẩn (`X phút`, `X giờ`, `X-Y phút`), lấy cận trên cho khoảng thời gian và luôn cung cấp nút "+ Thêm hẹn giờ" thủ công.
8. **Xử lý phụ thuộc nguyên liệu theo bước:** Nếu công thức chưa có dữ liệu nguyên liệu chia theo từng bước, hệ thống tự động fallback hiển thị nút mở ngăn kéo "Xem nguyên liệu toàn bài".

### Prompt đã sử dụng:

```text
/speckit-specify Tạo đặc tả yêu cầu chức năng (Feature Specification) cho tính năng "050-cooking-mode: Hands-free Cooking Mode & Concurrent Timers (Chế độ nấu ăn rảnh tay và Đếm giờ song song)" của dự án WikiCook theo đúng cấu trúc spec-template.md.

Mô tả chi tiết bài toán và nghiệp vụ:

1. Bối cảnh & Nỗi đau người dùng:
Khi nấu ăn trong bếp thực tế, tay người nấu thường ướt, dính dầu mỡ hoặc bận cầm dụng cụ, khó thao tác trên màn hình cảm ứng. Màn hình điện thoại dễ bị khóa/tắt khi đang đọc dở công thức; chữ thông thường quá nhỏ khi nhìn từ xa; các món ăn nhiều công đoạn dễ bị cháy khét nếu thiếu bộ đếm giờ riêng biệt cho từng bước.

2. Phân cấp tính năng & Ưu tiên (User Stories & Priorities):
- Story 1 (P1 - MVP Cốt lõi): Chế độ xem toàn màn hình & Giữ sáng màn hình tối đa.
  + Người dùng bấm "Bắt đầu nấu" (đóng vai trò User Gesture kích hoạt âm thanh) để mở giao diện toàn màn hình, chữ to vượt trội, hiển thị duy nhất 1 bước/màn hình kèm chế độ tương phản cao (High-contrast).
  + Hệ thống cố gắng tối đa (Best-effort) giữ màn hình sáng liên tục qua Screen Wake Lock API. Nếu thiết bị/trình duyệt không hỗ trợ, hiển thị thanh nhắc nhở người dùng chỉnh cài đặt màn hình máy.
- Story 2 (P1 - MVP Cốt lõi): Điều hướng bước chạm cực lớn & Phím tắt.
  + Hỗ trợ nút chuyển bước (Next/Previous) kích thước tối thiểu 64x64px ở đáy màn hình, dễ chạm bằng cổ tay hoặc ngón tay dính bẩn.
  + Hỗ trợ phím tắt bàn phím (Spacebar: Next, Mũi tên trái: Previous) cho máy tính bảng/laptop trong bếp.
- Story 3 (P2): Quản lý nhiều bộ đếm giờ song song (Parallel Timers).
  + Tự động nhận diện thời gian từ nội dung bước theo mẫu chuẩn ("X phút", "X giờ", "X-Y phút" -> lấy Y phút). Luôn có nút "+ Thêm hẹn giờ" để người dùng nhập tay nếu văn bản không nhận diện được.
  + Cho phép chạy nhiều timer song song. Hiển thị đếm ngược to rõ.
  + Khi chạm 00:00, phát âm thanh chuông báo lặp lại; trên thiết bị hỗ trợ thì rung kèm theo. Người dùng có thể bấm bất kỳ đâu trên màn hình hoặc nhấn phím Space để tắt chuông.
- Story 4 (P3 - Tính năng nâng cao): Điều khiển rảnh tay bằng giọng nói tiếng Việt (Online Feature).
  + Tính năng bổ trợ, KHÔNG bắt buộc: Chỉ hoạt động khi có kết nối Internet và người dùng chủ động bấm icon Micro (Push-to-Talk).
  + Tập lệnh chuẩn tối giản: "Tiếp theo", "Quay lại", "Đọc lại", "Hẹn giờ", "Tắt chuông".
  + Xử lý tạp âm bếp: Nếu độ tin cậy nhận diện dưới 70%, hệ thống bỏ qua và nháy nhẹ viền vàng, TUYỆT ĐỐI KHÔNG tự ý chuyển bước.
- Story 5 (P3): Khôi phục phiên nấu ăn (Session Recovery).
  + Lưu tiến trình nấu cục bộ trên thiết bị theo recipe_id với thời hạn hết hạn (TTL) là 4 tiếng.
  + Khi tải lại trang, nếu có phiên dở dang, hiện hộp thoại hỏi tiếp tục. Nếu timer đã hết giờ trong lúc đóng tab, hiển thị trạng thái "Đã quá hạn X phút" màu đỏ.

3. Ràng buộc nền tảng & Giả định kỹ thuật (Assumptions & Dependencies):
- Giả định dữ liệu: Phụ thuộc vào dữ liệu công thức. Nếu một bước có danh sách nguyên liệu riêng thì hiển thị ngay trong bước; nếu công thức chưa có dữ liệu chi tiết theo bước thì hiển thị nút bật bảng "Nguyên liệu chung".
- Hạn chế nền tảng Web: 
  + Tính năng rung (Vibration) chỉ áp dụng cho thiết bị Android/trình duyệt có hỗ trợ; iOS Safari tự động bỏ qua tính năng này mà không báo lỗi.
  + Giọng nói bị vô hiệu hóa khi mất mạng Internet; chế độ xem bước và timer cục bộ vẫn hoạt động 100% khi offline.

4. Tiêu chí thành công định lượng & Không phụ thuộc công nghệ (Measurable Success Criteria):
- SC-001 (Chạm tối giản - MVP): Người dùng có thể hoàn thành việc chuyển các bước nấu với diện tích vùng chạm cảm ứng lớn hơn ít nhất 300% so với giao diện web thông thường.
- SC-002 (Rảnh tay - Khi bật Giọng nói): Khi tính năng giọng nói được kích hoạt trong môi trường âm thanh bếp thông thường (< 60 dB), tỷ lệ nhận diện đúng 5 khẩu lệnh chuẩn đạt tối thiểu 85%.
- SC-003 (Độ chính xác timer): Bộ đếm giờ hiển thị và kích hoạt chuông báo với độ trễ sai số không quá 1.0 giây so với mốc thời gian thực khi tab đang mở.
- SC-004 (Bảo toàn phiên): 100% trạng thái số thứ tự bước nấu và thời gian còn lại của các timer đang chạy được khôi phục chính xác nếu trang bị tải lại trong vòng 4 tiếng.

Yêu cầu đầu ra:
Tạo file specs/050-cooking-mode/spec.md theo đúng spec-template.md với User Stories (Given-When-Then), Edge Cases, Functional Requirements, Key Entities, Success Criteria và Assumptions. Kèm theo checklist tại checklists/requirements.md.
```

### Kết quả do AI sinh ra & Kiểm chứng của sinh viên:
- File đặc tả đã tạo: `specs/050-cooking-mode/spec.md`.
- File checklist chất lượng: `specs/050-cooking-mode/checklists/requirements.md`.
- Sinh viên kiểm tra:
  + Cấu trúc tiếng Anh học thuật đạt chuẩn.
  + 5 User Stories đều có kịch bản kiểm thử hành vi Given - When - Then chi tiết.
  + Không rò rỉ chi tiết code/implementation vào file spec theo đúng quy định của Spec Kit.
  + Sẵn sàng cho bước `/speckit-plan`.

---

## Mục 4: Làm rõ yêu cầu kỹ thuật và hành vi biên bằng `/speckit-clarify`

| Trường | Nội dung |
|---|---|
| **Ngày** | 2026-10-16 |
| **Người thực hiện** | Lê Quốc Hưng (`LqHung06`) |
| **Spec liên quan** | 050 – Chế độ nấu ăn rảnh tay và Đếm giờ song song (`specs/050-cooking-mode/`) |
| **Công cụ AI** | Google Antigravity (Gemini 3.8 Flash), lệnh Spec Kit `/speckit-clarify` |
| **Mục đích** | Rà soát các điểm mập mờ trong tài liệu đặc tả, tiến hành vòng lặp hỏi đáp tương tác (interactive clarification loop) tối đa 5 câu hỏi để chuẩn hóa hành vi biên của hệ thống. |
| **Nhánh Git** | `Hung` (chuẩn bị cho branch `feature/050-cooking-mode`) |

### Quá trình làm rõ & Quyết định kỹ thuật:
- **Câu hỏi 1 (Thời lượng chuông báo đếm giờ):** Làm rõ hành vi khi timer chạm 00:00 mà người dùng đang bận tay không tắt được.
  + *Quyết định chọn:* Phương án A – Chuông reo tối đa 60 giây rồi tự động ngắt âm thanh để chống hao pin/chói tai, chuyển sang hiển thị nhấp nháy đỏ "Đã quá hạn X phút" (Overdue) cho đến khi người dùng tắt thủ công.
  + *Cập nhật vào Spec:* Bổ sung mục `## Clarifications`, đồng bộ Acceptance Scenario 5 của User Story 3, cập nhật `FR-013` và `Edge Case 6`.
- **Câu hỏi 2 (Quản lý đa phiên nấu ăn khi mở nhiều món):** Làm rõ hành vi khi người dùng mở nấu một món mới trong khi món cũ vẫn đang chạy trong thời hạn 4 tiếng.
  + *Quyết định chọn:* Phương án A – Lưu phiên độc lập theo từng `recipe_id` trong `localStorage` (`cooking_session_{recipe_id}`), cho phép mở nhiều tab nấu song song nhiều món mà không xung đột hay ghi đè timer.
  + *Cập nhật vào Spec:* Bổ sung mục `## Clarifications`, thêm Acceptance Scenario 7 vào User Story 5, cập nhật `FR-017` và hoàn thiện `Edge Case 3`.
- **Câu hỏi 3 (Hành vi khẩu lệnh "Đọc lại"):** Làm rõ việc hệ thống phản hồi bằng giọng đọc Text-to-Speech hay chỉ bằng tín hiệu hình ảnh/âm thanh.
  + *Quyết định chọn:* Phương án A – Đọc to toàn bộ nội dung hướng dẫn của bước bằng Web Speech Synthesis tiếng Việt (`vi-VN`); nếu thiết bị không hỗ trợ thì fallback an toàn bằng chớp viền bước và phát earcon chime mà không gây crash ứng dụng.
  + *Cập nhật vào Spec:* Bổ sung mục `## Clarifications`, hoàn thiện kịch bản 2 và 6 trong User Story 4, cập nhật `FR-015`, `Edge Case 7` và `Assumptions`.
- **Câu hỏi 4 (Gia hạn thời gian nhanh cho timer đang chạy):** Làm rõ cách người dùng kéo dài thời gian khi món ăn cần nấu thêm vài phút.
  + *Quyết định chọn:* Phương án A – Cung cấp nút chip bấm nhanh "+1 phút", "+5 phút" trực tiếp trên thẻ timer đang chạy để gia hạn tức thì chỉ với 1 chạm.
  + *Cập nhật vào Spec:* Bổ sung mục `## Clarifications`, thêm Acceptance Scenario 8 vào User Story 3, cập nhật `FR-011`.
- **Câu hỏi 5 (Xử lý timer đang chạy khi thoát hoặc hoàn thành phiên):** Làm rõ hành vi khi người dùng hoàn thành bước cuối hoặc bấm thoát trong lúc vẫn còn timer đang đếm ngược.
  + *Quyết định chọn:* Phương án A – Hiển thị hộp thoại cảnh báo có xác nhận kèm số lượng timer đang chạy, cho phép chọn tiếp tục chạy timer trong widget nền hoặc xác nhận hủy tất cả để thoát.
  + *Cập nhật vào Spec:* Bổ sung mục `## Clarifications`, cập nhật User Story 1 (Scenario 5), User Story 5 (Scenario 6), `FR-019` và `Edge Case 8`.

### Kết quả phiên làm rõ & Đánh giá chất lượng:
- **Số câu hỏi đã hoàn thành:** 5 / 5 câu theo đúng ngân sách của Spec Kit.
- **File đặc tả đã cập nhật:** `specs/050-cooking-mode/spec.md`.
- **Checklist chất lượng yêu cầu:** `specs/050-cooking-mode/checklists/requirements.md` đạt 16/16 tiêu chí (100% PASSED).
- **Trạng thái sẵn sàng:** Tài liệu đặc tả đã loại bỏ toàn bộ các điểm mù và hành vi biên mơ hồ. Sẵn sàng 100% để chuyển sang giai đoạn lập kế hoạch kiến trúc bằng lệnh `/speckit-plan`.

---

## Mục 5: Viết spec 040 (Automated Smart Shopping List & Ingredient Aggregation) bằng `/speckit-specify`

| Trường | Nội dung |
|---|---|
| **Ngày** | 2026-10-16 |
| **Người thực hiện** | Lê Quốc Hưng (`LqHung06`) |
| **Spec liên quan** | 040 – Danh sách mua sắm thông minh và Gộp nguyên liệu tự động (`specs/040-shopping-list/`) |
| **Công cụ AI** | Google Antigravity (Gemini 3.8 Flash), lệnh Spec Kit `/speckit-specify` |
| **Mục đích** | Soạn thảo bản đặc tả yêu cầu chức năng (`spec.md`) và checklist chất lượng (`checklists/requirements.md`) theo phân công tại `w1-spec-design-assignment.md` (Module 3.4). |
| **Nhánh Git** | `feature/040-shopping-list` |

### Quá trình phân tích & Đánh giá nghiệp vụ trước khi sinh spec:
Sinh viên rà soát toàn diện các điểm giao thoa kỹ thuật và nghiệp vụ thực tế khi đi chợ:
1. **Chuẩn hóa ngôn ngữ tài liệu:** Chuyển đổi toàn bộ nội dung sang tiếng Anh chuyên ngành (Formal Technical English) để đảm bảo tính đồng bộ 100% với 6 spec còn lại trong repository (`010`, `020`, `030`, `031`, `050`, `060`).
2. **Quy tắc gộp nguyên liệu & Đổi đơn vị hai chiều:** Định lượng được scale theo tỷ lệ khẩu phần ($S_{\text{target}} / S_{\text{base}}$). Chỉ cộng dồn các đơn vị cùng hệ chuẩn hóa metric ($g \leftrightarrow kg$, $ml \leftrightarrow l$); các đơn vị định tính/rời rạc ("quả", "nhánh", "thìa canh") được giữ thành các dòng con riêng biệt, không cộng ép gây sai lệch.
3. **Phân nhóm quầy siêu thị (Aisle Categorization):** Tự động phân loại nguyên liệu theo 6 quầy chuẩn (Produce, Meat & Seafood, Dairy & Eggs, Pantry & Spices, Frozen, General) giúp người đi chợ di chuyển tuần tự theo sơ đồ tầng.
4. **Khả năng hoạt động Ngoại tuyến (In-Store Offline Resilience):** Lưu trữ cục bộ (Local-first / IndexedDB) đảm bảo khi mất sóng 3G/4G trong tầng hầm siêu thị, người dùng vẫn tick chọn, sửa số lượng bình thường; tự động đồng bộ ngầm (Background Sync) trong 3 giây khi có mạng lại.
5. **Đưa sẵn 8 Clarifications (Q1–Q8):** Khóa chặt các quyết định biên (đơn vị thìa, snapshot công thức, gộp từ meal plan vs công thức lẻ, ưu tiên giữ trạng thái đã mua khi đồng bộ conflict).

### Kết quả do AI sinh ra & Kiểm chứng của sinh viên:
- File đặc tả đã tạo: `specs/040-shopping-list/spec.md`.
- File checklist chất lượng: `specs/040-shopping-list/checklists/requirements.md`.
- Sinh viên kiểm tra:
  + Cấu trúc tiếng Anh học thuật đạt chuẩn.
  + Đầy đủ 5 User Stories (P1 đến P3) với kịch bản kiểm thử Given - When - Then chi tiết.
  + Checklist đạt 16/16 tiêu chí (100% PASSED).
  + Sẵn sàng cho bước `/speckit-plan`.

---

## Mục 6: Làm rõ yêu cầu kỹ thuật và hành vi biên bằng `/speckit-clarify`

| Trường | Nội dung |
|---|---|
| **Ngày** | 2026-10-16 |
| **Người thực hiện** | Lê Quốc Hưng (`LqHung06`) |
| **Spec liên quan** | 040 – Danh sách mua sắm thông minh và Gộp nguyên liệu tự động (`specs/040-shopping-list/`) |
| **Công cụ AI** | Google Antigravity (Gemini 3.8 Flash), lệnh Spec Kit `/speckit-clarify` |
| **Mục đích** | Rà soát các điểm mập mờ trong tài liệu đặc tả, tiến hành vòng lặp hỏi đáp tương tác (interactive clarification loop) tối đa 5 câu hỏi để chuẩn hóa hành vi biên của hệ thống. |
| **Nhánh Git** | `feature/040-shopping-list` |

### Quá trình làm rõ & Quyết định kỹ thuật:
- **Câu hỏi 1 (Làm tròn số lượng lẻ cho đơn vị đếm được / rời rạc):** Làm rõ cách xử lý khi nhân tỷ lệ khẩu phần sinh ra số lượng lẻ thập phân cho các đơn vị rời rạc (quả, củ, bắp, lon, gói, quả trứng).
  + *Quyết định chọn:* Phương án A – Tự động làm tròn lên (Ceiling $\lceil x \rceil$) thành số nguyên gần nhất (ví dụ: 1 quả trứng, 2 bắp cải) trên checklist mua hàng nhằm tránh thiếu hụt nguyên liệu khi nấu; giữ nguyên giá trị tính toán thập phân chính xác trong chi tiết nguồn gốc (Item Provenance); các đơn vị đo lường liên tục ($g, kg, ml, l$) giữ độ chính xác tối đa 2 chữ số thập phân.
  + *Cập nhật vào Spec:* Bổ sung Q9 vào mục `## Clarifications`, thêm Acceptance Scenario 7 vào User Story 2, cập nhật `FR-003` và bổ sung `Discrete Unit Ceiling Rule` vào `Assumptions`.
- **Câu hỏi 2 (Lọc gia vị và nguyên liệu sẵn có tại nhà trước khi gộp):** Làm rõ việc người dùng có được xem trước và bỏ chọn các gia vị cơ bản (muối, đường, dầu ăn, nước mắm...) trước khi gộp vào danh sách mua sắm hay không.
  + *Quyết định chọn:* Phương án A – Hiển thị Bottom Sheet/Modal xem trước danh sách nguyên liệu kèm checkbox chọn lọc trước khi gộp; cung cấp nút tiện ích 1 chạm *"Bỏ chọn gia vị cơ bản"* để nhanh chóng loại trừ các món thường có sẵn tại nhà, giúp danh sách mua sắm không bị rác.
  + *Cập nhật vào Spec:* Bổ sung Q10 vào mục `## Clarifications`, thêm Acceptance Scenario 8 vào User Story 2, cập nhật `FR-002` và `FR-004`.
- **Câu hỏi 3 (Cơ chế xử lý dữ liệu khi bấm "Xóa các món đã mua"):** Làm rõ việc các món đã mua sau khi hết 5 giây hoàn tác sẽ bị xóa vĩnh viễn (Hard delete) hay ẩn tạm thời nhưng giữ trong lịch sử (Soft-clear).
  + *Quyết định chọn:* Phương án A – Cơ chế Soft-clear (`is_cleared = true`), ẩn khỏi giao diện checklist mua sắm để giảm tải thị giác khi đi chợ, có nút mở xem lại theo nhu cầu; đồng thời bảo lưu 100% dữ liệu các món này khi danh sách chuyển sang trạng thái lưu trữ (Archived) để phục vụ đối soát hóa đơn và thống kê.
  + *Cập nhật vào Spec:* Bổ sung Q11 vào mục `## Clarifications`, cập nhật Acceptance Scenario 5 của User Story 3, cập nhật `FR-018` và bổ sung thuộc tính `isCleared`, `clearedAt` vào thực thể `ShoppingListItem`.
- **Câu hỏi 4 (Phạm vi ghi nhớ khi người dùng thủ công chuyển quầy hàng):** Làm rõ việc người dùng reassign quầy hàng (Aisle Re-categorization) cho một nguyên liệu thì hệ thống chỉ nhớ trên danh sách hiện tại hay lưu thành thói quen lâu dài.
  + *Quyết định chọn:* Phương án A – Áp dụng ngay cho danh sách hiện tại; tại hộp thoại chuyển quầy cung cấp checkbox tùy chọn *"Ghi nhớ quầy này cho các lần mua sau"*. Nếu người dùng tích chọn, hệ thống lưu vào hồ sơ sở thích cá nhân (`UserAislePreference`) để các lần import công thức sau tự động phân vào đúng quầy quen thuộc.
  + *Cập nhật vào Spec:* Bổ sung Q12 vào mục `## Clarifications`, cập nhật Acceptance Scenario 6 của User Story 3, cập nhật `FR-011` và bổ sung thực thể `UserAislePreference` vào Key Entities.
- **Câu hỏi 5 (Trạng thái các món khi khôi phục danh sách cũ từ Lịch sử):** Làm rõ trạng thái mua sắm của các món (`isChecked`, `isCleared`) khi một danh sách đã lưu trữ được khôi phục làm danh sách hiện hành.
  + *Quyết định chọn:* Phương án A – Tự động đặt lại toàn bộ trạng thái về "Chưa mua" (`isChecked = false`, `isCleared = false`), giữ nguyên tên món, định lượng đã scale và phân nhóm quầy để người dùng có ngay một checklist mới sẵn sàng cho chuyến đi chợ tiếp theo mà không phải bấm bỏ tick từng món.
  + *Cập nhật vào Spec:* Bổ sung Q13 vào mục `## Clarifications`, cập nhật Acceptance Scenario 5 của User Story 1, và cập nhật `FR-022`.

### Kết quả phiên làm rõ & Đánh giá chất lượng:
- **Số câu hỏi đã hoàn thành:** 5 / 5 câu theo đúng ngân sách tối đa của Spec Kit.
- **File đặc tả đã cập nhật:** `specs/040-shopping-list/spec.md` (bao gồm Q1–Q13 tại mục Clarifications).
- **Checklist chất lượng yêu cầu:** `specs/040-shopping-list/checklists/requirements.md` đạt 16/16 tiêu chí (100% PASSED).
- **Trạng thái sẵn sàng:** Toàn bộ các góc khuất nghiệp vụ, tính toán định lượng, tương tác ngoại tuyến và vòng đời dữ liệu đã được khóa chặt chẽ. Sẵn sàng 100% cho bước lập kế hoạch kiến trúc bằng `/speckit-plan`.
