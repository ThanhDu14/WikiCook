# Nhật ký sử dụng AI – Nguyễn Đức Duy

Mỗi thành viên ghi log vào một file riêng; file này là của **Nguyễn Đức Duy** (mã **B**, Product Owner & Requirements Lead, phụ trách spec `020-recipe-search-filter` và `060-recipe-editor` theo [phân công tuần 12/10 – 18/10](../management/w1-spec-design-assignment.md), và Mục 1–6 của Vision Document theo Sprint 2 – `SCRUM-32` → `SCRUM-35` trong [project plan](../management/project-plan.md)). Mỗi lần dùng AI là một mục mới, mục mới nhất ở cuối.

| # | Ngày | Nội dung | Spec / Tài liệu |
|---|---|---|---|
| 1 | 2026-10-08 | Viết spec 020 (tìm kiếm và lọc công thức) bằng `/speckit-specify` | 020 |
| 2 | 2026-10-08 | Viết spec 060 (wizard tạo công thức, "Công thức của tôi") bằng `/speckit-specify` | 060 |
| 3 | 2026-10-08 | Soạn bản nháp schema nhóm bảng `recipes` / `ingredients` gửi cả nhóm | 020, 060 |
| 4 | 2026-10-08 | Làm rõ spec 020 bằng `/speckit-clarify` (5 câu hỏi) | 020 |
| 5 | 2026-10-08 | Làm rõ spec 060 bằng `/speckit-clarify` (5 câu hỏi) | 060 |
| 6 | 2026-10-17 | Soạn Vision Document Mục 1 (Introduction) và Mục 2 (Positioning) theo mẫu RUP (`SCRUM-32`) | Vision §1–2 |
| 7 | 2026-10-17 | Soạn Vision Document Mục 3 (Stakeholder & User Descriptions) và Mục 4 (Product Overview) (`SCRUM-33`) | Vision §3–4 |
| 8 | 2026-10-17 | Viết 10 tính năng cốt lõi dạng đoạn văn What/Why/Who ở Mục 5 (`SCRUM-34`) | Vision §5 |
| 9 | 2026-10-17 | Thiết kế 2 sơ đồ Mermaid luồng người dùng ở Mục 6 (`SCRUM-35`) | Vision §6 |

---

## Mục 1: Viết spec 020 (tìm kiếm và lọc công thức) bằng `/speckit-specify`

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-08 |
| Người thực hiện | Nguyễn Đức Duy |
| Spec liên quan | 020 – Tìm kiếm và lọc công thức (module 3.2, `specs/020-recipe-search-filter`) |
| Công cụ AI | Claude Code (model Claude Opus 5.5), lệnh Spec Kit `/speckit-specify` |
| Mục đích | Sinh bản đặc tả đầu tiên cho tìm kiếm full-text, bộ lọc, sắp xếp, phân trang và lọc theo dietary profile |
| Nhánh Git | `Duy` |

### Prompt đã dùng

Trước khi chạy, AI đã đọc constitution, proposal mục 2–3, vision document, khảo sát app (mục 4.1, 8.1), spec 010 và file phân công để viết mô tả dưới đây.

```
/speckit-specify SPECIFY_FEATURE_DIRECTORY=specs/020-recipe-search-filter
Khách (chưa đăng nhập) và người dùng đã đăng nhập tìm các công thức đã PUBLISHED trên WikiCook.
- Trang chủ có ô tìm kiếm nổi bật và các bộ lọc nhanh 1 chạm ("Dưới 30 phút", "Món chay", "Dễ làm")
  theo gợi ý ở docs/requirements/existing-app-survey.md mục 4.1 và 8.1.
- Tìm kiếm full-text theo từ khóa trên tên món, mô tả, nguyên liệu và thẻ; gõ có dấu hay không dấu,
  hoa hay thường đều ra cùng kết quả ("pho bo" tìm được "Phở bò"); khớp ở tên món được xếp trên khớp
  ở mô tả/nguyên liệu.
- Bộ lọc kết hợp được với nhau (AND giữa các nhóm, OR trong cùng một nhóm): tổng thời gian (chuẩn bị
  + nấu), ẩm thực, độ khó, cách nấu, danh mục, khoảng calo mỗi khẩu phần (số liệu do spec 090 ước
  tính; công thức chưa có calo bị loại khi bật lọc calo và có ghi chú rõ).
- Sắp xếp: liên quan nhất (mặc định khi có từ khóa), mới nhất (mặc định khi không có từ khóa), đánh
  giá cao nhất, nấu nhanh nhất. Kết quả phân trang, hiện tổng số kết quả; mỗi thẻ công thức hiện ảnh,
  tên, tổng thời gian, độ khó, điểm và số lượt đánh giá.
- Người dùng có dietary profile (spec 011): công thức chứa chất gây dị ứng hoặc nguyên liệu họ loại
  trừ tự động bị ẩn, có dòng thông báo "Đã ẩn N công thức theo hồ sơ ăn uống của bạn" và cho tắt tạm
  trong lần tìm đó. Khách không có lọc này. Quy tắc loại phải dùng chung được với AI planner (spec 031).
- Từ khóa, bộ lọc, sắp xếp và trang hiện tại nằm trên URL để chia sẻ link, tải lại trang và nút Back
  hoạt động đúng. Trên mobile (từ 360px) panel lọc mở dạng bottom sheet.
- Xử lý đủ 4 trạng thái màn hình: đang tải, không có kết quả (gợi ý bỏ bớt bộ lọc hoặc kiểm tra chính
  tả), lỗi mạng/máy chủ (giữ nguyên bộ lọc, có nút thử lại), có dữ liệu. Từ khóa rỗng, chỉ có khoảng
  trắng, ký tự đặc biệt hoặc quá dài phải được xử lý an toàn.
- Mục tiêu hiệu năng theo constitution: 95% lượt tìm trả kết quả dưới 1 giây với 10.000 công thức.
Không bao gồm: tìm theo nguyên liệu có sẵn trong tủ lạnh, tìm bằng AI/ngôn ngữ tự nhiên, gợi ý cá nhân
hóa, tạo/sửa công thức (spec 060), ước tính dinh dưỡng (spec 090), quản lý danh mục/ẩm thực/thẻ
(spec 100), nội dung trang chi tiết công thức.
Dữ liệu: recipes, recipe_ingredients, ingredients, categories, cuisines, tags, recipe_tags theo
docs/analysis-and-design/database/README.md (đang soạn, A tạo khung ngày 12/10).
Viết spec bằng tiếng Anh, cùng cấu trúc và văn phong với specs/user-authentication-roles/spec.md (có
Summary, In scope/Out of scope, Non-Functional Requirements). Không hỏi lại trong lúc specify: điểm nào
chưa rõ thì chọn mặc định hợp lý và ghi vào Assumptions để chốt ở /speckit-clarify; điểm giao với A, C,
E (danh mục nguyên liệu – dị ứng, rating_avg) ghi thành mục riêng để thống nhất với nhóm.
```

### Tóm tắt phản hồi của AI

- Tạo `specs/020-recipe-search-filter/spec.md` và `checklists/requirements.md`, ghi `.specify/feature.json`:
  - 4 user story: tìm theo từ khóa (P1), lọc kết hợp nhiều tiêu chí (P1), ẩn công thức xung đột dietary profile (P2), sắp xếp – phân trang – chia sẻ link (P2);
  - 26 yêu cầu chức năng (FR-001 → FR-026), 7 yêu cầu phi chức năng (NFR-001 → NFR-007), 13 trường hợp biên, 7 tiêu chí thành công (SC-001 → SC-007);
  - giá trị mặc định do AI chọn: 20 kết quả/trang đánh số trang (không cuộn vô hạn), mốc thời gian ≤ 15/30/60/> 60 phút, mốc calo < 300/300–500/500–800/> 800 kcal, từ khóa tối đa 100 ký tự, giới hạn 60 lượt tìm/phút;
  - AI tự bổ sung: thứ tự xếp hạng tên món > thẻ/nguyên liệu > mô tả; phá hòa bằng ngày xuất bản để phân trang không lặp/sót; **kiểu ăn (chay, keto) không tự áp dụng** mà chỉ dị ứng và nguyên liệu loại trừ mới là luật cứng; khi xem công thức bị ẩn thì mỗi món có cảnh báo nêu rõ chất gây dị ứng; phản hồi cũ không được ghi đè kết quả mới.
- Thêm bảng **Integration Points** gồm 6 điểm cần chốt với A, C, E: liên kết dị ứng – nguyên liệu là nguồn sự thật (nhãn do LLM của spec 090 chỉ để tham khảo), quy tắc loại dùng chung với spec 031, `rating_avg`, calo từ spec 090, danh mục do admin quản lý, các cột lõi của `recipes`.
- Checklist chất lượng đạt 16/16, không có `[NEEDS CLARIFICATION]`; 3 giá trị mặc định đánh dấu "to confirm at `/speckit-clarify`".

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| 4 user story và độ ưu tiên | Dùng | Bao đủ phạm vi spec 020 trong file phân công và module 3.2 của proposal |
| Bộ lọc nhanh trên trang chủ | Dùng | Theo bài học "Quick Presets" ở khảo sát app; B cũng phụ trách wireframe trang chủ |
| Chỉ dị ứng và nguyên liệu loại trừ là luật cứng, kiểu ăn không tự áp dụng | Dùng | Dị ứng là an toàn, kiểu ăn là sở thích; tránh ẩn quá nhiều kết quả |
| 20 kết quả/trang, mốc thời gian và calo cố định | Dùng tạm | Đã đánh dấu để chốt ở `/speckit-clarify` |
| Liên kết dị ứng – nguyên liệu là nguồn sự thật | Chờ thống nhất | Điểm giao A ↔ B ↔ C trong file phân công, chốt ở họp T4 14/10 |

### Cách kiểm chứng

- Đối chiếu constitution: nguyên tắc I (đánh dấu chờ mã Jira ở dòng Feature Branch), IV (NFR-004 – kiểm tra từ khóa phía server, hiển thị an toàn), V (NFR-001 – 95% < 1 giây với 10.000 công thức; FR-016 – phân trang; NFR-002 – từ 360px).
- Đối chiếu spec 010: khách được đọc công thức đã xuất bản (ma trận phân quyền); NFR-005 – không lộ công thức chưa xuất bản.
- Đối chiếu file phân công: đủ các ý của dòng `020` (có dấu/không dấu, lọc thời gian/ẩm thực/độ khó/cách nấu/calo, sắp xếp, phân trang, tự lọc dị ứng).
- `grep` trong spec: 0 `[NEEDS CLARIFICATION]`, 26 FR, 7 SC, không có tên công nghệ (Spring, React, PostgreSQL, SQL, API).

---

## Mục 2: Viết spec 060 (wizard tạo công thức và "Công thức của tôi") bằng `/speckit-specify`

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-08 |
| Người thực hiện | Nguyễn Đức Duy |
| Spec liên quan | 060 – Soạn công thức (module 3.6, `specs/060-recipe-editor`) |
| Công cụ AI | Claude Code (model Claude Opus 5.5), lệnh Spec Kit `/speckit-specify` |
| Mục đích | Sinh bản đặc tả đầu tiên cho wizard 6 bước, lưu nháp, máy trạng thái công thức và trang "Công thức của tôi" |
| Nhánh Git | `Duy` |

### Prompt đã dùng

Mô tả mở rộng từ ví dụ ở mục 6 của file phân công: thêm điều kiện xác minh email, chi tiết từng bước, rút lại bài đang chờ duyệt, lưu trữ/khôi phục, sửa bài đã xuất bản và chống mất dữ liệu.

```
/speckit-specify SPECIFY_FEATURE_DIRECTORY=specs/060-recipe-editor
Người dùng đã đăng nhập và đã xác minh email (Member trở lên, theo spec 010 FR-006) đăng công thức mới
qua wizard 6 bước; mỗi bước kiểm tra dữ liệu trước khi cho đi tiếp và có thanh tiến trình để quay lại
bước trước:
(1) Thông tin chung: tên, mô tả ngắn, ảnh bìa, ẩm thực, danh mục, cách nấu, thẻ, độ khó, thời gian
chuẩn bị và thời gian nấu (phút), số khẩu phần.
(2) Nguyên liệu: chọn từ danh mục nguyên liệu dùng chung (gợi ý khi gõ, không phân biệt dấu) hoặc đề
xuất nguyên liệu mới chưa có; định lượng, đơn vị, ghi chú ("thái hạt lựu"); đánh dấu "tùy chọn"; chia
nhóm ("Phần nước dùng"); kéo đổi thứ tự.
(3) Dụng cụ: chọn từ danh sách có sẵn (nồi chiên không dầu, lò nướng…).
(4) Các bước: nội dung, ảnh tùy chọn, hẹn giờ tùy chọn (chế độ nấu spec 050 dùng lại); thêm/xóa/kéo
đổi thứ tự.
(5) Video: link YouTube/TikTok tùy chọn, chỉ nhận link hợp lệ của hai nền tảng này, có xem trước.
(6) Xem lại: hiển thị như trang chi tiết công thức, chỉ ra mục còn thiếu, rồi gửi duyệt.
- Lưu nháp (DRAFT) ở bất kỳ bước nào, tự động lưu định kỳ; mở lại thì tiếp tục đúng bước đang dở. Mất
  mạng hoặc hết phiên đăng nhập khi đang soạn không được mất dữ liệu đã nhập; có cảnh báo khi rời trang
  mà chưa lưu.
- Máy trạng thái: gửi duyệt thì DRAFT → PENDING, khóa sửa cho tới khi có kết quả (tác giả được rút lại
  về DRAFT). Moderator (spec 100) quyết định PUBLISHED, REJECTED (kèm lý do) hoặc REVISION_REQUESTED
  (kèm ghi chú); REVISION_REQUESTED mở lại wizard để sửa và gửi lại. Tác giả lưu trữ (ARCHIVED) và
  khôi phục công thức của mình.
- Sửa công thức đã PUBLISHED: chọn phương án hợp lý (ví dụ bản sửa đi duyệt lại, bản cũ vẫn hiển thị
  trong lúc chờ) và ghi vào Assumptions để chốt với E ở /speckit-clarify.
- Trang "Công thức của tôi": tab theo trạng thái kèm số lượng, hiện lý do từ chối/ghi chú chỉnh sửa,
  chỉ hiện thao tác hợp lệ với từng trạng thái.
- Ảnh sai định dạng hoặc quá dung lượng bị chặn ngay trên form; tải ảnh lỗi thì cho thử lại mà không
  mất phần đã nhập; bấm gửi nhiều lần không tạo hai bản ghi.
- Chỉ tác giả xem và sửa được bản nháp của mình; mọi kiểm tra quyền và dữ liệu phải làm phía server
  (constitution nguyên tắc IV). Nội dung người dùng nhập phải hiển thị an toàn.
Không bao gồm: duyệt công thức (spec 100), ước tính dinh dưỡng (spec 090), tìm kiếm (spec 020), đánh
giá/Cooksnap (spec 070), nhập công thức từ link web, đồng tác giả, dịch công thức.
Dữ liệu: recipes, recipe_steps, recipe_ingredients, ingredients, units, recipe_media, equipment,
recipe_equipment theo docs/analysis-and-design/database/README.md (đang soạn).
Viết spec bằng tiếng Anh, cùng cấu trúc và văn phong với specs/user-authentication-roles/spec.md và
specs/020-recipe-search-filter/spec.md. Không hỏi lại trong lúc specify: điểm nào chưa rõ thì chọn mặc
định hợp lý, ghi vào Assumptions để chốt ở /speckit-clarify; các điểm giao với C, D, E (bước có hẹn
giờ, nhóm quầy siêu thị của nguyên liệu, kiểm duyệt, sự kiện PUBLISHED kích hoạt dinh dưỡng/embedding)
ghi thành bảng riêng.
```

### Tóm tắt phản hồi của AI

- Tạo `specs/060-recipe-editor/spec.md` và `checklists/requirements.md`, cập nhật `.specify/feature.json` sang spec 060:
  - 6 user story: tạo công thức qua wizard và gửi duyệt (P1), lưu nháp và không mất dữ liệu (P1), phản hồi quyết định của moderator (P2), trang "Công thức của tôi" (P2), sửa công thức đã xuất bản (P3), lưu trữ – khôi phục – xóa (P3);
  - 33 yêu cầu chức năng (FR-001 → FR-033), 7 yêu cầu phi chức năng, 16 trường hợp biên, 7 tiêu chí thành công;
  - máy trạng thái ghi rõ chuyển trạng thái nào do tác giả, chuyển nào chỉ moderator/admin được làm (FR-023, FR-024);
  - giá trị mặc định do AI chọn: 1–50 nguyên liệu, 1–30 bước, mỗi bước tối đa 1 ảnh và 1 hẹn giờ (10 giây – 24 giờ), ảnh JPEG/PNG/WebP ≤ 5 MB, tự lưu sau 30 giây, tối đa 10 lần gửi duyệt/ngày và 20 bản nháp;
  - AI tự bổ sung: sửa bài đã xuất bản tạo **một bản sửa (revision)** đi duyệt, bản cũ vẫn hiển thị; công thức đã xuất bản chỉ được lưu trữ, không được xóa (giữ đánh giá và bookmark của người khác); phát hiện bản nháp bị sửa trên thiết bị khác và cho chọn bản giữ lại; nhận định lượng dạng "1/2", "0,5"; đơn vị "vừa ăn" không cần định lượng; nút đổi thứ tự lên/xuống thay cho kéo-thả trên mobile.
- Thêm bảng **Integration Points** gồm 10 điểm cần chốt với A, C, D, E: các cột lõi của `recipes`, chuyển trạng thái kiểm duyệt, luồng sửa bài đã xuất bản, `rating_avg`, hẹn giờ mỗi bước, quầy siêu thị, sự kiện xuất bản kích hoạt dinh dưỡng/embedding (lỗi không chặn xuất bản), dị ứng của nguyên liệu mới, nơi lưu ảnh, công thức bị lưu trữ trong bộ sưu tập/meal plan.
- Checklist chất lượng đạt 16/16, không có `[NEEDS CLARIFICATION]`.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| 6 user story và độ ưu tiên | Dùng | MVP (US1 + US2) demo được luồng "soạn → gửi duyệt" mà không cần spec 100 |
| Bản sửa đi duyệt lại, bản đã xuất bản vẫn hiển thị | Dùng tạm | Điểm giao B ↔ E, chốt ở `/speckit-clarify` |
| Không cho xóa công thức đã xuất bản | Dùng | Giữ đánh giá, bookmark, meal plan của người khác; khớp điểm giao "Xóa tài khoản" và "Công thức bị gỡ" |
| Các giới hạn số lượng (50 nguyên liệu, 30 bước, 5 MB…) | Dùng tạm | Chốt ở `/speckit-clarify`; giới hạn ảnh chốt cùng E |
| Mọi bài gửi đều qua duyệt, kể cả Verified Contributor | Dùng | Khớp ma trận phân quyền của spec 010 |

### Cách kiểm chứng

- Đối chiếu spec 010: FR-006 (chưa xác minh email chỉ lưu nháp, không gửi duyệt) → US1 kịch bản 10, FR-001; trường hợp tài khoản bị đình chỉ → Edge Cases.
- Đối chiếu constitution: nguyên tắc IV (FR-003, NFR-003 – kiểm tra quyền, dữ liệu, loại file phía server; hiển thị văn bản thuần), V (NFR-005 – từ 360px; FR-033 – 4 trạng thái màn hình), II (SC-007 – mỗi kịch bản có test tự động).
- Đối chiếu file phân công: máy trạng thái `DRAFT → PENDING → PUBLISHED / REJECTED / REVISION_REQUESTED`, `ARCHIVED` khớp FR-023; điểm giao "Bước có hẹn giờ" (B ↔ D) và "Dinh dưỡng và embedding khi xuất bản" (C ↔ B ↔ E) có trong bảng Integration Points.
- `grep` trong spec: 0 `[NEEDS CLARIFICATION]`, 33 FR, 7 SC, không có tên công nghệ.
- Việc tiếp theo: gửi bản nháp schema `recipes`/`ingredients` cho nhóm trước T3 13/10; mang 2 bảng Integration Points ra họp T4 14/10; chạy `/speckit-clarify` cho 020 và 060 trước T5 15/10.

---

## Mục 3: Soạn bản nháp schema nhóm bảng `recipes` / `ingredients`

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-08 |
| Người thực hiện | Nguyễn Đức Duy |
| Spec liên quan | 020, 060 – phần CSDL của B (mục 2 file phân công) |
| Công cụ AI | Claude Code (model Claude Opus 5.5) |
| Mục đích | Có bản nháp schema lõi `recipes` + `ingredients` gửi cả nhóm trước T3 13/10, vì mọi spec khác đều phụ thuộc |
| Nhánh Git | `Duy` |

### Prompt đã dùng

```
Soạn bản nháp schema PostgreSQL 16 cho nhóm bảng mình phụ trách (B) theo mục 2 của
docs/management/w1-spec-design-assignment.md: recipes, recipe_steps, recipe_ingredients, ingredients,
units, recipe_media, categories, cuisines, tags, recipe_tags, equipment, recipe_equipment.
- Suy ra cột và ràng buộc từ FR và Key Entities của specs/020-recipe-search-filter/spec.md và
  specs/060-recipe-editor/spec.md; mỗi quyết định ghi rõ FR nào cần nó.
- Mỗi bảng: cột (kiểu, NULL, mặc định), khóa chính/khóa ngoại kèm ON DELETE, CHECK/UNIQUE, index cho
  các truy vấn chính (danh sách mới nhất, tìm full-text, lọc, "Công thức của tôi", hàng đợi duyệt).
- Máy trạng thái DRAFT → PENDING → PUBLISHED / REJECTED / REVISION_REQUESTED, ARCHIVED: vẽ sơ đồ,
  ghi chuyển nào do tác giả, chuyển nào do moderator. Bản nháp được thiếu trường nhưng rời DRAFT thì
  phải đủ (dùng CHECK theo status).
- Tìm kiếm full-text tiếng Việt không dấu: cột tsvector, index GIN, unaccent; giải thích cách xử lý việc
  unaccent không phải hàm IMMUTABLE và dữ liệu tìm kiếm nằm ở nhiều bảng.
- Quy ước chung (khóa chính, kiểu enum, timestamp, soft delete, tên file Flyway) do A chốt: đề xuất một
  phương án kèm lý do và đánh dấu "chờ A chốt". Bảng cần thêm ngoài danh sách được giao (nhóm nguyên
  liệu, cách nấu, bản sửa, lịch sử trạng thái, liên kết nguyên liệu – dị ứng) đánh dấu là đề xuất.
- Liệt kê câu hỏi cho từng điểm giao với A, C, D, E kèm đề xuất của mình để mang ra họp T4 14/10.
- Kèm sơ đồ Mermaid erDiagram và danh sách file migration Flyway dự kiến.
Viết tiếng Việt, đặt ở docs/analysis-and-design/database/draft-recipes-ingredients.md (file riêng, không
đụng README.md mà A sẽ tạo).
```

### Tóm tắt phản hồi của AI

- Tạo `docs/analysis-and-design/database/draft-recipes-ingredients.md` gồm 6 mục: quy ước giả định, ERD Mermaid, sơ đồ máy trạng thái, đặc tả 12 bảng được giao + 6 bảng đề xuất, migration Flyway dự kiến, 10 câu hỏi cho họp T4.
- Quy ước đề xuất (chờ A chốt): khóa chính `BIGINT IDENTITY`, enum `VARCHAR + CHECK`, công thức không soft delete mà dùng `ARCHIVED`, danh mục dùng `is_active`, migration nhóm B dùng dải `V2xx`.
- `recipes`: các cột nội dung cho phép `NULL` để lưu nháp; ràng buộc `chk_recipes_complete` buộc đủ trường khi `status <> 'DRAFT'`; `total_time_minutes` là cột generated cho lọc/sắp xếp; `version` cho optimistic lock (spec 060 FR-021); 6 index theo từng truy vấn chính, dùng partial index `WHERE status = 'PUBLISHED'`.
- Tìm kiếm: hàm bọc `f_unaccent` khai báo `IMMUTABLE`; `search_vector` do Service ghi khi xuất bản vì lấy dữ liệu từ nhiều bảng, trọng số tên (A) > thẻ + nguyên liệu (B) > mô tả (C), cấu hình `simple`, từ khóa tham số hóa.
- 6 bảng đề xuất thêm: `ingredient_allergens` (nguồn sự thật để lọc dị ứng), `ingredient_groups`, `cooking_methods` + `recipe_cooking_methods`, `recipe_revisions` (bản sửa lưu JSONB, tối đa một bản đang mở), `recipe_status_history` (lý do từ chối, ghi chú sửa, bắt buộc `message` khi từ chối).
- Ràng buộc đáng chú ý: một ảnh bìa và một ảnh mỗi bước bằng unique partial index; ảnh ≤ 5 MB; `UNIQUE (recipe_id, position) DEFERRABLE` để đổi thứ tự trong một transaction; tên nguyên liệu đã duyệt không trùng khi bỏ dấu.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Cột nội dung `NULL` + `CHECK` theo `status` | Dùng | Lưu nháp thiếu trường (spec 060 FR-015) mà vẫn đảm bảo bài gửi duyệt đủ dữ liệu ngay ở CSDL |
| `search_vector` do Service cập nhật thay vì trigger | Dùng | Theo constitution III: logic ở Service, dễ test; chỉ cần cập nhật khi xuất bản |
| Bản sửa lưu JSONB thay vì nhân đôi 6 bảng con | Dùng tạm | Đơn giản cho đồ án; chốt cùng E ở họp T4 |
| `ingredient_allergens` do B giữ | Chờ thống nhất | Điểm giao A ↔ B ↔ C, câu hỏi số 3 |
| Quy ước khóa chính, enum, dải số migration | Chờ A chốt | Thuộc phần quy ước chung của A |

### Cách kiểm chứng

- Đối chiếu từng bảng với danh sách của B ở mục 2 file phân công: đủ 12 bảng; 6 bảng thêm được đánh dấu 🔵 và có lý do.
- Đối chiếu với spec: giới hạn trong `CHECK` khớp FR (thời gian 0–1440 – 060 FR-005; khẩu phần 1–50; hẹn giờ 10 giây – 24 giờ – FR-010; ảnh JPEG/PNG/WebP ≤ 5 MB – FR-014); index khớp truy vấn của 020 FR-014/FR-015 và 060 FR-031.
- Đối chiếu constitution: PostgreSQL 16 + Flyway, không `ddl-auto`; truy vấn tìm kiếm tham số hóa (nguyên tắc IV).
- Chưa chạy thử SQL vì máy chưa có PostgreSQL/Docker; sẽ chạy migration khi C dựng xong `docker-compose.yml` (PostgreSQL 16 + `unaccent`).
- Việc tiếp theo: gửi link file vào nhóm Zalo trước T3 13/10; cập nhật theo kết quả họp T4 rồi chuyển vào `database/README.md`.

---

## Mục 4: Làm rõ spec 020 bằng `/speckit-clarify`

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-08 |
| Người thực hiện | Nguyễn Đức Duy |
| Spec liên quan | 020 – Tìm kiếm và lọc công thức (`specs/020-recipe-search-filter`) |
| Công cụ AI | Claude Code (model Claude Opus 5.5), lệnh Spec Kit `/speckit-clarify` |
| Mục đích | Chốt các giá trị mặc định AI tự chọn ở bước specify và quy tắc an toàn cho người bị dị ứng, trước khi lập kế hoạch kỹ thuật |
| Nhánh Git | `Duy` |

### Prompt đã dùng

```
/speckit-clarify
Rà soát spec 020 (specs/020-recipe-search-filter/spec.md) và hỏi mình lần lượt từng câu, tối đa 5 câu,
về những điểm còn mơ hồ có ảnh hưởng lớn tới dữ liệu, độ an toàn với người bị dị ứng và trải nghiệm tìm
kiếm. Ưu tiên: (1) 3 giá trị mặc định đang đánh dấu "to confirm at /speckit-clarify" trong Assumptions
(20 kết quả/trang đánh số trang, mốc thời gian và calo cố định, tắt lọc dị ứng chỉ trong một lần tìm);
(2) các điểm giao trong bảng Integration Points ảnh hưởng tới schema ở
docs/analysis-and-design/database/draft-recipes-ingredients.md, đặc biệt nguồn sự thật của nhãn dị ứng
và cách tính điểm đánh giá trên thẻ công thức. Mỗi câu kèm phương án đề xuất, lý do và hệ quả của từng
phương án. Sau khi mình trả lời, ghi vào mục Clarifications và sửa đồng bộ mọi FR, User Story, Edge
Case, Success Criteria, Assumptions liên quan; không để lại giá trị cũ mâu thuẫn.
```

Câu 1 mình tự chọn sau khi đọc hệ quả từng phương án; từ câu 2, mình đồng ý dùng phương án AI đề xuất cho các câu còn lại:

| # | Câu hỏi của AI | AI đề xuất | Mình chọn |
|---|---|---|---|
| 1 | Hệ thống dựa vào đâu để biết công thức chứa chất gây dị ứng của người dùng? | A – chỉ liên kết nguyên liệu → chất dị ứng trong danh mục; nhãn LLM (spec 090) chỉ để hiển thị | A – đồng ý |
| 2 | "Hiện các công thức đã bị ẩn" có hiệu lực trong bao lâu? | A – chỉ trong lần tìm hiện tại | Theo đề xuất |
| 3 | Kết quả chia trang đánh số hay "Xem thêm"/cuộn vô hạn? | A – trang đánh số, 20 công thức/trang | Theo đề xuất |
| 4 | Lọc thời gian và calo dùng mốc cố định hay thanh trượt tự chọn? | A – mốc cố định | Theo đề xuất |
| 5 | "Đánh giá cao nhất" xử lý công thức có rất ít đánh giá thế nào? | A – cần ít nhất 3 đánh giá mới được xếp hạng, ít hơn thì xếp sau | Theo đề xuất |

### Tóm tắt phản hồi của AI

- Thêm mục **Clarifications / Session 2026-10-08** với 5 câu hỏi – trả lời.
- FR-018: ghi rõ nguồn duy nhất để xác định "nguyên liệu chứa chất dị ứng" là liên kết trong danh mục nguyên liệu; nhãn dị ứng do AI ước tính (spec 090) không được dùng để ẩn/hiện công thức. Bảng Integration Points đổi thành "đã quyết định cho spec này", A và C chỉ còn xác nhận ai giữ bảng liên kết.
- FR-020: thêm điều kiện "đổi từ khóa hoặc rời trang kết quả thì tự bật lại lọc dị ứng".
- FR-016: ghi rõ trang đánh số 20 công thức, không cuộn vô hạn hay "Xem thêm".
- Thêm FR-014a: thứ tự "đánh giá cao nhất" (≥ 3 đánh giá → < 3 đánh giá → chưa có đánh giá); Edge Cases thêm trường hợp công thức có 1–2 đánh giá; bảng Integration Points nhắc E dùng chung số lượt đánh giá cho ngưỡng này.
- Assumptions: bỏ 3 dòng "to confirm", thay bằng một dòng dẫn tới Clarifications.
- Checklist chất lượng vẫn 16/16.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Chỉ dùng liên kết nguyên liệu – chất dị ứng để lọc | Dùng | Dữ liệu do người duyệt nhập, kiểm chứng được; nhãn LLM có thể sai và chưa có ngay khi xuất bản |
| Dùng cả hai nguồn để lọc (phương án C câu 1) | **Bỏ** | Ẩn cả công thức do LLM báo nhầm; spec 020 phụ thuộc tiến độ spec 090 |
| Tắt lọc dị ứng trong cả phiên (phương án B câu 2) | **Bỏ** | Người bị dị ứng dễ quên đang tắt lọc |
| Ngưỡng 3 đánh giá cho "đánh giá cao nhất" | Dùng (bổ sung mới) | Tránh món chỉ có 1 đánh giá 5 sao đứng đầu danh sách |
| Trang đánh số, mốc lọc cố định | Dùng | Giữ link chia sẻ và nút Back đúng (SC-005); dễ kiểm thử, gọn trên mobile |

### Cách kiểm chứng

- Mục Clarifications có đúng 5 dòng; mỗi câu trả lời khớp với FR-014a, FR-016, FR-018, FR-020, Edge Cases và Assumptions.
- Tìm "to confirm" trong spec chỉ còn dòng Integration Points về việc A và C xác nhận người giữ bảng, không còn giá trị mặc định chưa chốt.
- Đối chiếu schema nháp mục 4.4: bảng `ingredient_allergens` là nguồn lọc, khớp câu 1.
- Việc tiếp theo: báo E về ngưỡng 3 đánh giá dùng `review_count`; báo A, C về quyết định nguồn dị ứng ở họp T4 14/10.

---

## Mục 5: Làm rõ spec 060 bằng `/speckit-clarify`

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-08 |
| Người thực hiện | Nguyễn Đức Duy |
| Spec liên quan | 060 – Soạn công thức (`specs/060-recipe-editor`) |
| Công cụ AI | Claude Code (model Claude Opus 5.5), lệnh Spec Kit `/speckit-clarify` |
| Mục đích | Chốt vòng đời công thức sau khi xuất bản, nguyên liệu mới, các giới hạn số lượng và cách xử lý sửa trên nhiều thiết bị |
| Nhánh Git | `Duy` |

### Prompt đã dùng

```
/speckit-clarify
Rà soát spec 060 (specs/060-recipe-editor/spec.md) và hỏi mình lần lượt từng câu, tối đa 5 câu, ưu tiên
những điểm ảnh hưởng tới máy trạng thái công thức, dữ liệu và việc không mất nội dung người dùng đã nhập:
(1) sửa công thức đã PUBLISHED thì người đọc thấy gì trong lúc chờ duyệt (điểm giao với E – spec 100);
(2) khôi phục công thức ARCHIVED có phải duyệt lại không; (3) tác giả có được dùng nguyên liệu chưa có
trong danh mục không và ai duyệt; (4) các giới hạn số lượng đang đặt tạm (số nguyên liệu, số bước, dung
lượng ảnh, số lần gửi duyệt/ngày, số bản nháp, chu kỳ tự lưu); (5) cùng một bản nháp bị sửa trên hai
thiết bị. Mỗi câu kèm phương án đề xuất, lý do và hệ quả, đối chiếu với schema nháp ở
docs/analysis-and-design/database/draft-recipes-ingredients.md. Sau khi mình trả lời từng câu, ghi vào
mục Clarifications và sửa đồng bộ FR, User Story, Edge Cases, Assumptions và bảng Integration Points;
không để lại giá trị cũ mâu thuẫn.
```

| # | Câu hỏi của AI | AI đề xuất | Mình chọn |
|---|---|---|---|
| 1 | Sửa công thức đã xuất bản thì người đọc thấy gì trong lúc chờ duyệt? | A – bản sửa đi duyệt riêng, người đọc vẫn thấy bản đang xuất bản | Theo đề xuất |
| 2 | Khôi phục công thức đã lưu trữ có phải duyệt lại không? | A – không, vì công thức đã lưu trữ không sửa được | Theo đề xuất |
| 3 | Tác giả có được dùng nguyên liệu chưa có trong danh mục không? | A – được đề xuất, moderator duyệt cùng công thức | Theo đề xuất |
| 4 | Các giới hạn mặc định có chấp nhận được không? | A – giữ nguyên toàn bộ | Theo đề xuất |
| 5 | Bản nháp bị sửa trên hai thiết bị thì khi lưu xử lý thế nào? | A – phát hiện xung đột, hỏi người dùng giữ bản nào | Theo đề xuất |

### Tóm tắt phản hồi của AI

- Thêm mục **Clarifications / Session 2026-10-08** với 5 câu hỏi – trả lời.
- User Story 6 kịch bản 2: bỏ điều kiện "không bị sửa trong lúc lưu trữ", vì nay công thức đã lưu trữ không sửa được; khôi phục luôn trả về `PUBLISHED` với nội dung cũ.
- FR-026: thêm `ARCHIVED` vào danh sách trạng thái tác giả không được sửa, phải khôi phục trước rồi mới sửa (để câu 2 không có kẽ hở).
- Bảng Integration Points: luồng sửa công thức đã xuất bản chuyển thành "đã quyết định cho spec này", E chỉ còn xác nhận cách bản sửa hiện trong hàng đợi duyệt.
- Assumptions: bỏ 3 dòng "to confirm", thay bằng một dòng dẫn tới Clarifications và nhắc việc thống nhất với E trước `/speckit-plan`.
- Checklist chất lượng vẫn 16/16.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Bản sửa đi duyệt riêng, bản cũ vẫn hiển thị | Dùng | Công thức không biến mất khỏi tìm kiếm khi tác giả sửa lỗi nhỏ; khớp bảng `recipe_revisions` trong schema nháp |
| Khôi phục không cần duyệt lại | Dùng | Nội dung giống hệt lúc đã được duyệt; đi kèm việc cấm sửa khi đang lưu trữ |
| "Lưu sau cùng thắng" khi sửa trên hai thiết bị | **Bỏ** | Mất nội dung âm thầm, trái NFR-001 (không mất ký tự nào) |
| Giới hạn 50 nguyên liệu, 30 bước, ảnh 5 MB, 10 lần gửi/ngày, 20 bản nháp | Dùng | Đủ cho công thức gia đình, chặn spam; giới hạn ảnh vẫn chờ E xác nhận nơi lưu |

### Cách kiểm chứng

- Mục Clarifications có đúng 5 dòng; mỗi câu trả lời khớp với FR-021, FR-026, FR-027, User Story 6 và Assumptions.
- Tìm "to confirm" và "not changed" trong spec không còn kết quả cũ mâu thuẫn.
- Đối chiếu schema nháp: `recipe_revisions` (tối đa một bản sửa đang mở), cột `version` cho optimistic lock, `ingredients.status = 'PROPOSED'`, các `CHECK` giới hạn — đều khớp câu trả lời.
- Việc tiếp theo: mang luồng sửa công thức đã xuất bản ra họp T4 14/10 để E xác nhận; sau khi thống nhất các điểm giao thì chạy `/speckit-plan` cho 020 và 060 (hạn T6 16/10).

---

## Mục 6: Soạn Vision Document Mục 1 (Introduction) và Mục 2 (Positioning)

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-17 |
| Người thực hiện | Nguyễn Đức Duy |
| Tài liệu liên quan | `docs/requirements/vision-document.md` – Mục 1, 2 (Jira `SCRUM-32`) |
| Công cụ AI | Claude Code (model Claude Opus 5.5) |
| Mục đích | Đưa Vision Document từ khung PA1 sang đúng cấu trúc mẫu RUP Vision; chuẩn hóa Problem Statement và Product Position Statement |
| Nhánh Git | `Duy` |

### Prompt đã dùng

```
Soạn Mục 1 (Introduction) và Mục 2 (Positioning) của docs/requirements/vision-document.md theo đúng cấu
trúc mẫu RUP Vision cho dự án nhỏ (rup_vision_sp.docx), task Jira SCRUM-32.
Bối cảnh: file hiện có là bản PA1 (positioning, persona, use case) chưa theo mẫu RUP. Nguồn dữ liệu:
docs/requirements/project-proposal.md (mục 1–3), existing-app-survey.md, comparative-analysis.md,
docs/management/project-plan.md (vai trò, task SCRUM-32 → SCRUM-36, ràng buộc CON/ASM) và các spec 010,
020, 030, 031, 060.
Yêu cầu:
1. Tái cấu trúc toàn bộ file theo khung RUP: 1 Introduction, 2 Positioning, 3 Stakeholder and User
   Descriptions, 4 Product Overview, 5 Product Features, 6 User Workflow Diagrams, 7 Non-Functional
   Requirements. Mục 7 do Mai Văn Hiển viết ở SCRUM-36: chỉ giữ tiêu đề và metadata, không viết nội dung.
2. Metadata 3 cấp giống project-plan.md: bảng Responsibility Matrix (Performed by / Reviewed by / Edited
   by / Jira Task) và dòng metadata in nghiêng dưới mỗi mục. Reviewer lấy đúng theo bảng task Sprint 2
   (Mục 1–2: Lê Quốc Hưng; 3–4: Trần Nguyễn Công Chung; 5–6: Mai Văn Hiển), editor là Lê Quốc Hưng.
   Thêm Revision History.
3. Mục 1: 1.1 Purpose (nêu rõ đối tượng đọc và cách họ dùng tài liệu), 1.2 Scope (in-scope là 10 tính
   năng, out-of-scope rõ ràng), 1.3 Definitions (Cooksnap, RAG, pgvector, các vai trò của spec 010…),
   1.4 References (IEEE Std 830-1998, mẫu RUP, PA1/PA2, proposal, survey, project plan, architecture, các
   spec), 1.5 Overview.
4. Mục 2: 2.1 Business Opportunity dựa trên khoảng trống thị trường rút ra từ khảo sát PA1; 2.2 Problem
   Statement đúng bảng 4 dòng "The problem of / affects / the impact of which is / a successful solution
   would be"; 2.3 Product Position Statement đúng bảng "For / Who / WikiCook (is) / That / Unlike / Our
   product".
Ràng buộc: tiếng Anh chuẩn văn phong tài liệu kỹ thuật; mọi con số phải có nguồn trong repo (không bịa số
liệu khảo sát người dùng); thuật ngữ và quyết định phải khớp các spec đã clarify (ví dụ: nguồn dữ liệu dị
ứng là liên kết nguyên liệu – chất dị ứng, không phải nhãn do LLM sinh). Chỉ sửa vision-document.md.
```

### Tóm tắt phản hồi của AI

- Viết lại toàn bộ `vision-document.md` theo 7 mục của mẫu RUP; thêm phần đầu tài liệu (môn học, nhóm, phiên bản `0.9`, ngày), bảng Responsibility Matrix 7 dòng có cột Jira và bảng Revision History (`v0.1` là bản PA1 merge ở PR #12 ngày 08/10, `v0.9` là bản này).
- Mục 1: Purpose cho 3 nhóm người đọc (giảng viên/trợ giảng, PO, nhóm phát triển); Scope nêu 10 tính năng và 5 hạng mục ngoài phạm vi (app native, đặt hàng/thanh toán, IoT, AI tự tạo công thức, tư vấn y khoa); 10 thuật ngữ; 8 tài liệu tham chiếu, có ghi chú IEEE 830 đã được thay bằng ISO/IEC/IEEE 29148.
- Mục 2: Business Opportunity chỉ ra mỗi đối thủ chỉ phủ một phần chuỗi "chọn món → lên thực đơn → đi chợ → nấu → chia sẻ"; Problem Statement và Position Statement dùng đúng nhãn của mẫu RUP, phần "Unlike" nêu tên và điểm yếu cụ thể của MyFridgeFood và Ăn Gì Ngon.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Tái cấu trúc cả file theo mẫu RUP | Dùng | Đề PA2 yêu cầu theo `rup_vision_sp.docx`; khung 3 mục của PA1 không có Introduction, Stakeholder, Product Overview |
| Sơ đồ use case cấp cao của PA1 | **Bỏ** | Mô hình hóa use case là việc của Sprint 3 (PA3); persona và phạm vi của PA1 được chuyển vào Mục 3.2 và 1.2 |
| Số liệu "15–30 phút/ngày" | Dùng, ghi rõ nguồn | Chỉ có trong proposal PA1, nhóm chưa khảo sát người dùng thật nên ghi "PA1 proposal estimate" |
| Mục 7 chỉ để tiêu đề và metadata | Dùng | Thuộc `SCRUM-36` của Hiển; tránh conflict khi merge |

### Cách kiểm chứng

- Đối chiếu nhãn bảng 2.2 và 2.3 với mẫu RUP: đủ 4 dòng và 6 dòng, đúng thứ tự.
- Đối chiếu reviewer/editor trong bảng metadata với bảng task Sprint 2 ở `project-plan.md` mục 4.2.2: khớp cả 7 dòng.
- Kiểm tra các nhánh khác trên remote: không nhánh nào sửa `vision-document.md`, nên tái cấu trúc không gây conflict.
- Xem lịch sử git của file để ghi đúng ngày `v0.1` (commit `af3a9f3`, 08/10/2026).

---

## Mục 7: Soạn Vision Document Mục 3 (Stakeholder & User Descriptions) và Mục 4 (Product Overview)

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-17 |
| Người thực hiện | Nguyễn Đức Duy |
| Tài liệu liên quan | `docs/requirements/vision-document.md` – Mục 3, 4 (Jira `SCRUM-33`) |
| Công cụ AI | Claude Code (model Claude Opus 5.5) |
| Mục đích | Mô tả stakeholder, người dùng, môi trường sử dụng, nhu cầu cốt lõi, đối thủ và bối cảnh sản phẩm, có dẫn chứng từ khảo sát PA1 |
| Nhánh Git | `Duy` |

### Prompt đã dùng

```
Tiếp tục docs/requirements/vision-document.md: viết Mục 3 (Stakeholder and User Descriptions) và Mục 4
(Product Overview) theo mẫu RUP — task SCRUM-33. Tiêu chí nghiệm thu: phân tích sâu, dẫn chứng thuyết
phục từ kết quả khảo sát PA1.
Mục 3:
- 3.1 Stakeholder Summary (Name / Description / Responsibilities): giảng viên – trợ giảng, PO, PM/SM, nhóm
  phát triển, nhà cung cấp dịch vụ bên ngoài (LLM, Google OAuth, email).
- 3.2 User Summary (Name / Description / Responsibilities / Stakeholder) cho người nấu ăn bận rộn, người
  sáng tạo công thức, quản trị viên/moderator; giữ nhóm người dùng có chế độ ăn đặc biệt của PA1 như một
  nhóm con; thêm cột ánh xạ vai trò hệ thống theo spec 010 (Guest/Member/Contributor/Moderator/Admin).
- 3.3 User Environment theo từng bối cảnh: bếp (tay ướt/dính dầu mỡ, hơi nước, tiếng ồn máy hút mùi,
  thiết bị đặt cách 0,5–1 m, màn hình tự tắt sau 30–60 giây), lên thực đơn hằng tuần, siêu thị sóng yếu,
  soạn công thức/kiểm duyệt; nền tảng và trình duyệt hỗ trợ; ứng dụng liên quan (Zalo, YouTube/TikTok).
- 3.4 Summary of Key Needs: bảng Need / Priority / Concerns / Current Solution / Proposed Solution, đánh
  mã N1…N8 để Mục 5 truy vết ngược.
- 3.5 Alternatives and Competition: phân tích MyFridgeFood và Ăn Gì Ngon (điểm mạnh, điểm yếu, WikiCook
  học được gì), trích dẫn đúng Figure và hàng trong ma trận của existing-app-survey.md; nêu các giải pháp
  thay thế khác (blog/YouTube, nhóm Facebook, ghi chú giấy); kết thúc bằng bảng so sánh năng lực 3 sản
  phẩm và kết luận về điểm khác biệt.
Mục 4:
- 4.1 Product Perspective: vị trí sản phẩm, kiến trúc khái quát theo architecture.md, kèm sơ đồ Mermaid
  ngữ cảnh hệ thống (người dùng, thành phần WikiCook, hệ thống bên ngoài).
- 4.2 Assumptions and Dependencies: bảng ID / Type / Statement / Impact if it does not hold, gồm giả định
  (mạng, đủ công thức cho AI, dữ liệu dị ứng do moderator duyệt, hạn mức free tier, API trình duyệt) và
  phụ thuộc (LLM, embedding + pgvector, Google OAuth, email, thứ tự giữa các spec, ràng buộc dự án).
Ràng buộc: chỉ dùng số liệu có trong proposal, survey, spec; không bịa thống kê; tiếng Anh; giữ metadata 3 cấp.
```

### Tóm tắt phản hồi của AI

- 3.1: 5 nhóm stakeholder; 3.2: 4 nhóm người dùng kèm vai trò hệ thống và ghi chú persona (mục tiêu, nỗi đau).
- 3.3: 6 bối cảnh sử dụng, mỗi bối cảnh suy ra yêu cầu giao diện (chữ lớn đọc từ xa, nút lớn, giữ màn hình sáng, dùng một tay, chịu được mất mạng).
- 3.4: 8 nhu cầu N1–N8 (5 ưu tiên High, 3 Medium).
- 3.5: phân tích 2 đối thủ có dẫn chứng (Figure 1, 2, 5–7, 10, 11 và các hàng trong ma trận Mục 2 và 6 của survey), 3 nhóm giải pháp thay thế, bảng so sánh 11 năng lực; kết luận khác biệt nằm ở **chuỗi liền mạch** cộng 3 năng lực mà cả hai đối thủ đều không có.
- 4.1: sơ đồ ngữ cảnh Mermaid gồm 3 nhóm người dùng, 4 thành phần nội bộ và 6 hệ thống bên ngoài; 4.2: 5 giả định (ASM-01 → 05) và 6 phụ thuộc (DEP-01 → 06), mỗi dòng có hệ quả nếu không đúng.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Người dùng có chế độ ăn đặc biệt là nhóm con | Dùng | Đề Jira nêu 3 nhóm, nhưng đây là persona của PA1 và là lý do tồn tại của Feature 1 và 9 |
| Chỉ so sánh với 2 app đã khảo sát ở PA1 | Dùng | Mọi nhận định đều kiểm chứng được bằng screenshot trong repo; không thêm đối thủ chưa khảo sát |
| ASM-03: liên kết nguyên liệu – chất dị ứng là nguồn sự thật | Dùng | Khớp quyết định ở `/speckit-clarify` spec 020 (Mục 4 của log này) |
| Giới hạn 5 lần sinh thực đơn/người/giờ trong ASM-04 | Dùng | Lấy từ spec 031 và proposal mục 4.8 |

### Cách kiểm chứng

- Đối chiếu các ô 🟢/🟡/❌ của MyFridgeFood và Ăn Gì Ngon trong bảng 3.5.4 với ma trận Mục 2 và 6 của `existing-app-survey.md`: khớp; riêng dòng "Search by on-hand ingredients" không có trong ma trận nên lấy theo mô tả tính năng ở Mục 1 và 5 của survey.
- Kiểm tra các Figure được trích dẫn đều có trong survey (Figure 1–11).
- Sơ đồ ngữ cảnh 4.1 parse hợp lệ bằng Mermaid 11.4.1 (xem Mục 9).

---

## Mục 8: Viết 10 tính năng cốt lõi ở Vision Document Mục 5

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-17 |
| Người thực hiện | Nguyễn Đức Duy |
| Tài liệu liên quan | `docs/requirements/vision-document.md` – Mục 5 (Jira `SCRUM-34`) |
| Công cụ AI | Claude Code (model Claude Opus 5.5) |
| Mục đích | Mô tả 10 tính năng cốt lõi thành đoạn văn 3–5 câu theo quy tắc bắt buộc của đề PA2 |
| Nhánh Git | `Duy` |

### Prompt đã dùng

```
Viết Mục 5 (Product Features) của docs/requirements/vision-document.md — task SCRUM-34. Áp dụng quy tắc bắt
buộc của đề PA2: mỗi tính năng là MỘT đoạn văn hoàn chỉnh 3–5 câu, tuyệt đối không dùng gạch đầu dòng.
Mỗi đoạn phải trả lời đủ 3 ý:
- What: tính năng làm gì, vận hành thế nào;
- Why: giải quyết nỗi đau nào của người dùng, dẫn chứng khoảng trống của MyFridgeFood / Ăn Gì Ngon từ
  khảo sát PA1;
- Who: nhóm người dùng nào (theo User Summary ở Mục 3.2) hưởng lợi trực tiếp.
10 tính năng theo đúng thứ tự: User Auth & Dietary Profile, Smart Recipe Discovery & Filters, AI Weekly
Meal Planner, Smart Shopping List, Hands-free Cooking Mode & Timers, Community Recipe Contribution, Social
Cooksnaps & Reviews, Custom Recipe Collections, Nutrition & Allergen Analyzer, Admin Content Moderation.
Nội dung phải khớp các quyết định đã chốt trong spec: 010 (4 vai trò, xác minh email); 020 (tìm không dấu,
lọc kết hợp, ẩn món xung đột hồ sơ ăn uống, ngưỡng 3 đánh giá); 030/031 (lịch 7 ngày × 3 bữa, RAG, ràng
buộc cứng/mềm, kiểm tra từng món AI chọn, AI không tạo món mới); 040 và 050 trên nhánh feature; 060
(wizard 6 bước, vòng đời trạng thái). Nhãn dị ứng phải lấy từ liên kết nguyên liệu – chất dị ứng, LLM chỉ
ước tính dinh dưỡng. Cuối mục thêm bảng truy vết Feature → User Need (N1–N8) → module PA1 → spec.
Tiếng Anh chuyên nghiệp; tự đếm số câu của từng đoạn trước khi trả kết quả.
```

### Tóm tắt phản hồi của AI

- 10 đoạn văn (5.1 → 5.10), mỗi đoạn 5 câu theo thứ tự: cơ chế (What) → chi tiết vận hành → giá trị khi kết nối với tính năng khác → nỗi đau và dẫn chứng đối thủ (Why) → người hưởng lợi (Who).
- Mục 5.11: bảng truy vết 10 tính năng → nhu cầu N1–N8 → module 3.1–3.10 của proposal → spec.
- Các chi tiết lấy từ spec: "pho bo" tìm ra "Phở bò" (020), ngưỡng 3 đánh giá cho "Top rated" (020 clarify), vòng đời Draft → Pending → Published/Rejected/Revision Requested → Archived (060), thử lại LLM và để trống món sai (031), giữ màn hình sáng và lệnh giọng nói tiếng Việt (050), gộp đơn vị g ↔ kg và dùng offline (040).

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Tìm theo nguyên liệu có sẵn (pantry matching) nằm trong Feature 2 | Dùng, có ghi chú | Đề Jira và Workflow 2 yêu cầu, nhưng spec 020 đã loại khỏi phạm vi; bảng 5.11 ghi "pantry matching in a later increment" — **cần PO chốt** có viết spec riêng hay không |
| Nhãn dị ứng lấy từ liên kết nguyên liệu, không từ LLM | Dùng (khác proposal 3.9) | Proposal PA1 nói LLM gắn nhãn dị ứng; đã đổi theo quyết định clarify spec 020 vì nhãn LLM có thể sai |
| Viết đúng 5 câu mỗi đoạn | Dùng | Mức trên của khung 3–5 câu, đủ chỗ cho cả What, Why, Who |

### Cách kiểm chứng

- Đếm câu bằng script `awk` trên từng đoạn 5.1–5.10: cả 10 đoạn đều 5 câu; không đoạn nào dùng gạch đầu dòng.
- Đối chiếu con số với spec: 4 vai trò (010 FR-003 và ma trận phân quyền), 7 × 3 = 21 slot (030 FR-002, 031), ≥ 3 đánh giá (020 FR-014a), wizard 6 bước (060).
- Kiểm tra mỗi đoạn đều nhắc ít nhất một nhóm người dùng có trong bảng 3.2.

---

## Mục 9: Thiết kế 2 sơ đồ Mermaid luồng người dùng ở Vision Document Mục 6

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-17 |
| Người thực hiện | Nguyễn Đức Duy |
| Tài liệu liên quan | `docs/requirements/vision-document.md` – Mục 6 (Jira `SCRUM-35`) |
| Công cụ AI | Claude Code (model Claude Opus 5.5) |
| Mục đích | Minh họa 2 hành trình đầu-cuối nối các tính năng ở Mục 5 bằng sơ đồ Mermaid có chú thích |
| Nhánh Git | `Duy` |

### Prompt đã dùng

```
Thiết kế Mục 6 (User Workflow Diagrams) của docs/requirements/vision-document.md — task SCRUM-35: 2 sơ đồ
Mermaid flowchart, mỗi sơ đồ kèm bảng chú thích từng bước (Step / Actor / Description / Feature ở Mục 5).
- Workflow 1 – AI Meal Planning & Grocery: thiết lập khẩu phần và ràng buộc từ dietary profile → RAG truy
  vấn vector database (pgvector) → lọc cứng dị ứng/nguyên liệu loại trừ → LLM sinh thực đơn tuần → kiểm
  tra từng món → xem/swap món → xác nhận → tự động xuất danh sách đi chợ đã gộp và nhóm theo quầy siêu thị.
- Workflow 2 – Pantry Search & Cooking Mode: chọn nguyên liệu có sẵn trong tủ lạnh → tìm công thức phù hợp
  → bật chế độ nấu rảnh tay → kích hoạt nhiều bộ hẹn giờ đếm ngược song song → đăng ảnh Cooksnap.
- Thể hiện cả nhánh ngoại lệ: ràng buộc không hợp lệ, không đủ công thức, LLM lỗi hoặc quá thời gian (quay
  về planner thủ công), không tìm thấy món phù hợp; hẹn giờ vẫn chạy nền khi người dùng sang bước khác.
- Phân biệt tác nhân bằng classDef màu (người dùng / hệ thống / dịch vụ AI / điểm quyết định) và ghi chú
  giải màu ngay trước sơ đồ.
- Khớp spec 031 (thử lại LLM 1 lần; món sai bị loại và để trống cho người dùng chọn) và spec 050.
Tiêu chí nghiệm thu: cú pháp Mermaid hợp lệ, render được trên GitHub; tự kiểm tra bằng trình parse của
Mermaid trước khi báo xong.
```

### Tóm tắt phản hồi của AI

- Workflow 1: 13 bước, 4 điểm quyết định (ràng buộc hợp lệ, đủ ứng viên, LLM phản hồi kịp, hài lòng với bản nháp) với các nhánh sửa ràng buộc, nới ràng buộc, quay về planner thủ công, swap/sinh lại; tô màu riêng các bước của dịch vụ AI (embedding, truy vấn pgvector, LLM).
- Workflow 2: 13 bước, vòng lặp theo từng bước nấu; hẹn giờ nối bằng mũi tên nét đứt "runs in background" tới chuông báo để thể hiện chạy song song.
- Mỗi sơ đồ có bảng chú thích 6–7 dòng gom các bước, ghi tác nhân và tính năng tương ứng ở Mục 5.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Phân biệt tác nhân bằng màu thay vì swimlane `subgraph` | Dùng | Swimlane có nhiều mũi tên qua lại giữa các làn nên sơ đồ rối khi render |
| Cooksnap hiện ngay, nội dung xấu xử lý qua báo cáo | Dùng tạm | Chưa có spec 070; **cần thống nhất** với người viết spec 070 và 100 xem Cooksnap có phải duyệt trước không |
| Bước LLM trả về JSON có cấu trúc | Dùng | Khớp proposal mục 4.4 và spec 031 (kiểm tra schema) |

### Cách kiểm chứng

- Parse cả 3 sơ đồ Mermaid trong file (ngữ cảnh 4.1 và 2 workflow) bằng `mermaid@11.4.1` chạy trên Node + jsdom: **3/3 hợp lệ** (`flowchart-v2`); một sơ đồ cố tình viết sai cú pháp thì parser báo lỗi, chứng tỏ bước kiểm tra có tác dụng.
- Đối chiếu nhánh lỗi với spec 031: thử lại một lần khi LLM lỗi hoặc sai định dạng; món không hợp lệ không hiển thị và slot được đánh dấu để người dùng chọn tay.
- Chưa xem render thực tế trên GitHub vì chưa push; cần mở file trên GitHub sau khi push để kiểm tra lần cuối.
