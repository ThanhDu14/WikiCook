# Nhật ký sử dụng AI – Nguyễn Đức Duy

Mỗi thành viên ghi log vào một file riêng; file này là của **Nguyễn Đức Duy** (mã **B**, Product Owner & Requirements Lead, phụ trách spec `020-recipe-search-filter` và `060-recipe-editor` theo [phân công tuần 12/10 – 18/10](docs/management/w1-spec-design-assignment.md)). Mỗi lần dùng AI là một mục mới, mục mới nhất ở cuối.

| # | Ngày | Nội dung | Spec |
|---|---|---|---|
| 1 | 2026-10-08 | Viết spec 020 (tìm kiếm và lọc công thức) bằng `/speckit-specify` | 020 |
| 2 | 2026-10-08 | Viết spec 060 (wizard tạo công thức, "Công thức của tôi") bằng `/speckit-specify` | 060 |
| 3 | 2026-10-08 | Soạn bản nháp schema nhóm bảng `recipes` / `ingredients` gửi cả nhóm | 020, 060 |
| 4 | 2026-10-08 | Làm rõ spec 020 bằng `/speckit-clarify` (5 câu hỏi) | 020 |
| 5 | 2026-10-08 | Làm rõ spec 060 bằng `/speckit-clarify` (5 câu hỏi) | 060 |

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
docs/analysis-and-design/database/draft-recipes-ingredients.md. Mình đồng ý dùng phương án đề xuất cho
cả 5 câu; ghi vào Clarifications, sửa đồng bộ FR, User Story, Edge Cases, Assumptions và bảng
Integration Points, không để lại giá trị cũ mâu thuẫn.
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
