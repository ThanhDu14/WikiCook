# Nhật ký sử dụng AI – Mai Văn Hiển

Mỗi thành viên ghi log vào một file riêng; file này là của **Mai Văn Hiển** (mã **A**, AI Architect & Technical Lead, phụ trách spec `010-user-authentication-roles` (làm rõ bằng `/speckit-clarify`), `011-user-profile-dietary` và `080-bookmark-collection` theo [phân công tuần 12/10 – 18/10](../management/w1-spec-design-assignment.md), và `SCRUM-29`, `SCRUM-36`, `SCRUM-45` của Sprint 2 trong [project plan](../management/project-plan.md)). Mỗi lần dùng AI là một mục mới, mục mới nhất ở cuối.

| # | Ngày | Nội dung | Spec / Tài liệu |
|---|---|---|---|
| 1 | 2026-10-12 → 2026-10-18 | Dùng Claude (claude.ai): dịch/giải thích đề PA2 và bài học Spec Kit, hướng dẫn cài Spec Kit với Antigravity, soạn nháp prompt, góp ý phương án `/speckit-clarify` | Đề PA2, 010, 011, 080 |
| 2 | 2026-10-18 | Rà soát các spec hiện có về dietary profile, khẩu phần, bộ sưu tập, xóa tài khoản, avatar (chưa sửa file nào) | 010, 020, 030, 031, 040, 050, 060 |
| 3 | 2026-10-18 | Viết spec 080 (bookmark và bộ sưu tập) bằng `/speckit-specify` | 080 |
| 4 | 2026-10-18 | Viết spec 011 (hồ sơ cá nhân và dietary profile) bằng `/speckit-specify` | 011 |
| 5 | 2026-10-18 | Làm rõ spec 011 bằng `/speckit-clarify` (6 câu hỏi) | 011 |
| 6 | 2026-10-18 | Làm rõ spec 080 bằng `/speckit-clarify` (5 câu hỏi) | 080 |
| 7 | 2026-10-18 | Làm rõ spec 010 bằng `/speckit-clarify` (4 chỉ thị + 5 câu hỏi) | 010 |
| 8 | 2026-10-14 | Tạo Project Plan tổng thể theo mô hình Scrum 5 Sprint, chi tiết Sprint 2 | `project-plan.md` |
| 9 | 2026-10-19 (ngày commit) | Viết Mục 7 (Non-Functional Requirements) của Vision Document (`SCRUM-36`) | Vision §7 |
| 10 | 2026-10-19 (ngày commit) | Sửa Mục 4.4 của Project Plan cho khớp spec 031 và Vision §7 (`SCRUM-45`) | `project-plan.md` §4.4 |

---

## Mục 1: Dùng Claude (claude.ai) hỗ trợ đọc đề, cài đặt và soạn nháp prompt

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-12 , 2026-10-18 |
| Giờ truy cập | 19:31 – 19:53 |
| Người thực hiện | Mai Văn Hiển |
| Spec / Tài liệu liên quan | Đề PA2; các bài học Spec Kit trên Microsoft Learn; spec 010, 011, 080 |
| Công cụ AI | Claude (claude.ai), model Claude Opus 5.5 |
| Mục đích | (1) Dịch và giải thích đề PA2 và các bài học Spec Kit trên Microsoft Learn; (2) hướng dẫn cài đặt Spec Kit với Antigravity; (3) soạn nháp các prompt dùng ở Mục 2–7; (4) góp ý phương án khi trả lời `/speckit-clarify` |
| Nhánh Git | Không áp dụng (làm việc trên claude.ai, không sửa file trong repo; các prompt sau đó được chạy ở Mục 2–7 trên nhánh `Hien`) |

### Dòng tóm tắt theo mẫu AI Usage Notes

**Claude.** *Claude Opus 5.5, Anthropic, claude.ai*, truy cập 19:31 đến 19:53 trong 2 ngày 12/10 và 18/10/2026, prompt: xem lịch sử hội thoại bên dưới, dùng để hiểu đề PA2 và các bài học Spec Kit, cài Spec Kit với Antigravity, soạn nháp prompt cho Mục 2–7 và cân nhắc phương án khi trả lời `/speckit-clarify`; AI tạo bản dịch, giải thích, hướng dẫn cài và bản nháp prompt; sinh viên tự viết lại phần tóm tắt training, tự chạy thử cài đặt ở `speckit_test`, chỉnh prompt trước khi chạy và tự chọn phương án ở từng câu clarify.

### Prompt đã dùng

Xem lịch sử hội thoại claude.ai (ảnh chụp lưu tại `docs/assets/jira/ai_prompts/`).

### Tóm tắt phản hồi của AI

- **Hiểu đề và bài học:** dịch và giải thích đề PA2-2026 (4 phần A–D, hướng dẫn nộp, tiêu chí chấm) và 5 bài
  của module Microsoft Learn về SDD và GitHub Spec Kit (4 giai đoạn Specify → Plan → Tasks → Implement, so sánh
  với Waterfall/Scrum/TDD/BDD, các lệnh `/speckit-*`). Mỗi bài có gợi ý tóm tắt để viết bằng chứng training.
- **Cài đặt:** đối chiếu với README và trang Integrations chính thức của Spec Kit thì phát hiện bài Microsoft
  Learn đã cũ (nay dùng `--integration <key>`, lệnh dạng `/speckit-specify`). Xác nhận Antigravity được hỗ trợ
  với key `agy`. Hướng dẫn xử lý từng lỗi khi cài: lệnh `specify` không có trong PATH, lỗi "agy not found" (dùng
  `--ignore-agent-tools`), mở nhầm thư mục cha nên Antigravity không thấy skill.
- **Rà soát nhóm:** đọc file phân công tuần, `jira_tasks.md` và 3 AI log của nhóm. Chỉ ra việc viết spec chưa có
  task trên Jira, spec 010 đang gán trùng `SCRUM-27`, log của Hưng ghi sai mã thành viên.
- **Soạn prompt:** soạn nháp Prompt 0 (rà soát), prompt specify 011 và 080, prompt clarify 010/011/080 và
  prompt tạo log này. Sau khi đọc kết quả Prompt 0, đề xuất 10 phương án cho các mâu thuẫn giữa spec 020, 030,
  031, 060 liên quan tới hồ sơ ăn uống.
- **Clarify:** với mỗi câu hỏi của `/speckit-clarify`, phân tích ưu nhược điểm các phương án và gợi ý bổ sung
  (ví dụ chống né lệnh cấm khi xóa tài khoản, xóa dữ liệu tạo trước khi xác minh khi liên kết Google).

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Bản dịch và giải thích đề PA2, bài học Spec Kit | Dùng để tự học; phần tóm tắt training tự viết lại | Giúp mình hiểu đề nhanh hơn. Bằng chứng training phải thể hiện điều mình học được nên không chép nguyên gợi ý. |
| Hướng dẫn cài Spec Kit với Antigravity | Dùng, sau khi tự chạy thử ở thư mục `speckit_test` | Hướng dẫn ban đầu có chỗ phải sửa khi chạy thực tế (thiếu `--ignore-agent-tools`, mở sai thư mục workspace). Mình làm từng bước và kiểm tra kết quả bằng `Get-ChildItem` trước khi làm tiếp. |
| Bản nháp prompt cho Mục 2–7 | Dùng làm bản nháp; prompt chạy thực tế được chép ở từng mục | Mình chỉnh trước khi chạy, ví dụ thêm quy ước danh sách "Đã lưu" vào prompt 080. Prompt clarify 080 ban đầu mình chép nhầm phần ưu tiên của 011, phát hiện và thay bằng bản đúng. |
| Góp ý phương án khi trả lời `/speckit-clarify` | Tham khảo; quyết định cuối do mình chọn | Mình tự chọn phương án ở từng câu và chịu trách nhiệm về quyết định. Một số câu mình chọn khác đề xuất ban đầu của agent trong Antigravity: nguyên liệu loại trừ tối đa 50 thay vì 200, Admin không xem hồ sơ ăn uống, "Đã lưu" tối đa 1.000 món, thêm ngoại lệ chống né lệnh cấm. |

### Cách kiểm chứng

- Ảnh chụp hội thoại claude.ai cần lưu ở `docs/assets/jira/ai_prompts/` (thư mục này hiện chưa có trong repo; tạo và thêm ảnh trước khi mở PR).

---

## Mục 2: Rà soát các spec hiện có trước khi viết spec 011 và 080

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-18 |
| Giờ truy cập | 20:31 |
| Người thực hiện | Mai Văn Hiển |
| Spec / Tài liệu liên quan | 010, 020, 030, 031, 040, 050, 060, `draft-recipes-ingredients.md`, các file `ai_log_*.md` |
| Công cụ AI | Claude Code trong Google Antigravity, model Claude Opus 5.5 |
| Mục đích | Liệt kê giả định của các spec khác về dietary profile, khẩu phần, bộ sưu tập, xóa tài khoản, avatar; tìm mâu thuẫn giữa các spec; xem spec 010 còn sót phạm vi của 011 không (prompt 0, chưa sửa file nào) |
| Nhánh Git | `Hien` |

### Dòng tóm tắt theo mẫu AI Usage Notes

**Claude Code.** *Claude Opus 5.5, Anthropic, Claude Code trong Google Antigravity*, truy cập 20:31 ngày 18/10/2026, prompt: "Chưa sửa file nào. … Đọc … Báo cáo bằng tiếng Việt, dạng bảng" (nguyên văn bên dưới), dùng để rà soát giả định và mâu thuẫn giữa các spec trước khi viết 011 và 080; AI tạo báo cáo 4 phần trong hội thoại (không tạo file); sinh viên đối chiếu số FR/Assumption với các spec và dùng kết quả làm cơ sở cho prompt Mục 4 và Mục 7.

### Prompt đã dùng

```
Chưa sửa file nào. Mình là Mai Văn Hiển (mã A), phụ trách spec 010 (clarify), 011-user-profile-dietary
và 080-bookmark-collection theo docs/management/w1-spec-design-assignment.md.
Đọc: .specify/memory/constitution.md; docs/requirements/project-proposal.md mục 3.1 và 3.8;
specs/user-authentication-roles/spec.md; specs/020-recipe-search-filter/spec.md;
specs/030-meal-planner/spec.md; specs/031-ai-weekly-meal-planner/spec.md;
specs/060-recipe-editor/spec.md; specs/040-shopping-list/spec.md; specs/050-cooking-mode/spec.md;
docs/analysis-and-design/database/draft-recipes-ingredients.md (và database/README.md nếu có);
các file ai_log_*.md của nhóm (tìm trong repo).
Báo cáo bằng tiếng Việt, dạng bảng:
1. Mọi chỗ các spec khác đang giả định về dietary profile, số người trong gia đình, thời gian nấu
   tối đa, dị ứng, nguyên liệu loại trừ, bộ sưu tập/bookmark, xóa tài khoản, avatar. Ghi rõ spec,
   số FR/Assumption và giả định đó là gì.
2. Các chỗ mâu thuẫn giữa các spec (ví dụ kiểu ăn là luật cứng ở 031 nhưng không ở 020; khẩu phần
   mặc định khi hồ sơ trống ở 030 và 031 khác nhau).
3. Spec 010 còn sót nội dung nào thuộc phạm vi 011 (hồ sơ, avatar, đổi mật khẩu, xóa tài khoản)
   hay không, và mã Jira đang gán cho 010.
4. Đường dẫn thư mục chứa các file ai_log_*.md.
```

### Tóm tắt phản hồi của AI

- Prompt này chỉ yêu cầu báo cáo dạng bảng trong hội thoại, không tạo file nào nên kết quả rà soát không có trong repo.
- Dấu vết của kết quả nằm ở các spec viết sau: bảng Integration Points của spec 011 nêu các điểm phải sửa ở các spec khác (030 FR-012 – khẩu phần mặc định; 031 FR-009 – kiểu ăn và thời gian nấu; 030 Clarifications – thứ tự gợi ý món; 010 NFR-009 và Out of scope), và spec 010 được cập nhật Out of scope cùng dòng Feature Branch (xem Mục 7).
- Nội dung 4 phần của báo cáo:
  - **Bảng 1 – giả định của các spec khác** (nhóm theo chủ đề): dị ứng/loại trừ/kiểu ăn (020 FR-018 → FR-022, 030 FR-007b, 031 FR-002, FR-009, FR-034, 060 Key Entities, bảng `ingredient_allergens` trong bản nháp CSDL); khẩu phần (030 FR-011 → FR-013, 031 FR-002, 040 bước 0,5, 060 FR-005 khẩu phần gốc 1–50); thời gian nấu tối đa (030 FR-007a chỉ xếp hạng, 031 FR-009 luật cứng, 020 FR-007 dùng tổng thời gian); ẩm thực ưa thích (030, 031 giả định có trong hồ sơ dù phạm vi 011 không có); bookmark/bộ sưu tập (010 ma trận quyền, 030 FR-007, 060 US5-2, US6-4); xóa tài khoản (030 Edge Cases, `recipes.author_id ON DELETE SET NULL` chờ A chốt); avatar (chỉ 010 Out of scope nhắc tới). 040 và 050 không có giả định trực tiếp.
  - **Bảng 2 – 12 mâu thuẫn**, quan trọng nhất: kiểu ăn là sở thích ở 020, luật mềm ở 030, luật cứng ở 031; cách xử lý món vi phạm dị ứng khác nhau giữa 020/030/031; khẩu phần mặc định khi hồ sơ trống là khẩu phần gốc của công thức (030) nhưng là 2 (031); miền khẩu phần 1–20 / bước 0,5 / 1–50 không khớp; thời gian nấu tối đa chưa rõ là thời gian nấu hay tổng thời gian; 030 gộp "bookmark" và "bộ sưu tập" làm một; chưa chốt ai giữ bảng liên kết dị ứng; 010 NFR-009 chỉ cho chủ tài khoản và Admin thấy tên hiển thị trong khi tên tác giả hiển thị công khai.
  - **Bảng 3 – spec 010**: phần hồ sơ, avatar, dietary profile đã cắt; còn thiếu đổi mật khẩu, xóa tài khoản, sửa tên hiển thị trong Out of scope; chưa có mục Clarifications; dòng Feature Branch gán `SCRUM-27` nhưng Jira dùng mã này cho task Sprint Planning của Hưng, và Assumption cuối vẫn ghi "chưa gán mã Jira".
  - **Phần 4**: các file log nằm ở `docs/ai_log/` (lúc đó có `ai_log_duy.md`, `ai_log_hung.md`, `ai_log_thanhdu.md`); `ai_log_hung.md` ghi Hưng là mã A thay vì D; `database/README.md` chưa có; thư mục spec 010 đã đổi tên thành `specs/010-user-authentication-roles` nên đường dẫn trong prompt đã cũ.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Danh sách giả định và mâu thuẫn giữa các spec (mục 1–2 của báo cáo) | Dùng làm cơ sở cho các điểm giao trong prompt viết spec 011 (030 FR-012, 031 FR-009, 010 NFR-009) | Báo cáo dẫn đúng số FR/Assumption nên mở lại spec để kiểm tra được. Hai mâu thuẫn lớn nhất (kiểu ăn, khẩu phần mặc định) cũng là chỗ Dự đã ghi "cần xem lại" trong `ai_log_thanhdu.md`. Spec 011 là nguồn sự thật của dietary profile nên phải giải quyết các mâu thuẫn này ngay trong 011. |
| Phát hiện 010 còn sót phạm vi 011 và mã Jira gán cho 010 (mục 3) | Dùng làm cơ sở cho prompt làm rõ spec 010 (Mục 7) | File phân công yêu cầu tuần này spec 010 chỉ cần clarify, gắn mã Jira và cắt phần hồ sơ sang 011. Mã `SCRUM-27` đối chiếu được trong `project-plan.md` và `jira_tasks.md` là task Sprint Planning, nên giữ lại sẽ sai truy vết theo constitution nguyên tắc I. |

### Cách kiểm chứng

- Đối chiếu với kết quả cuối trong repo: spec 011 có các dòng Integration Points tương ứng và spec 010 đã cập nhật Out of scope, NFR-009, Feature Branch.

---

## Mục 3: Viết spec 080 (bookmark và bộ sưu tập) bằng `/speckit-specify`

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-18 |
| Giờ truy cập | 20:44 |
| Người thực hiện | Mai Văn Hiển |
| Spec / Tài liệu liên quan | 080 – Bookmark và bộ sưu tập (module 3.8, `specs/080-bookmark-collection`) |
| Công cụ AI | Claude Code trong Google Antigravity, model Claude Opus 5.5, lệnh Spec Kit `/speckit-specify` |
| Mục đích | Sinh bản đặc tả đầu tiên cho lưu một chạm, bộ sưu tập, công thức "Không còn khả dụng" và danh sách bộ sưu tập cho hộp chọn món của meal planner |
| Nhánh Git | `Hien` |

### Dòng tóm tắt theo mẫu AI Usage Notes

**Claude Code.** *Claude Opus 5.5, Anthropic, Claude Code trong Google Antigravity (Spec Kit `/speckit-specify`)*, truy cập 20:44 ngày 18/10/2026, prompt: "/speckit-specify SPECIFY_FEATURE_DIRECTORY=specs/080-bookmark-collection …" (nguyên văn bên dưới), dùng để viết bản đặc tả đầu tiên của spec 080; AI tạo `spec.md` (5 user story, 26 FR, 7 NFR, Integration Points) và checklist; sinh viên xác định phạm vi trong prompt, đổi "Yêu thích" thành "Đã lưu" và chốt các điểm còn mở ở bước clarify (Mục 6).

### Prompt đã dùng

```
/speckit-specify SPECIFY_FEATURE_DIRECTORY=specs/080-bookmark-collection
Người dùng đã đăng nhập (Member trở lên) lưu công thức và sắp xếp vào bộ sưu tập.
Phạm vi:
(1) Lưu/bỏ lưu công thức bằng một chạm trên trang chi tiết và thẻ công thức, có phản hồi ngay; khách
chưa đăng nhập bấm lưu thì được đưa tới đăng nhập rồi quay lại (giống spec 030).
(2) Tạo, đổi tên, xóa bộ sưu tập; xóa bộ sưu tập không xóa công thức.
(3) Hộp thoại "Lưu vào bộ sưu tập": thêm một công thức vào một hoặc nhiều bộ sưu tập, bỏ ra.
(4) Trang danh sách bộ sưu tập và trang chi tiết bộ sưu tập.
(5) Công thức bị ARCHIVED hoặc bị gỡ vẫn nằm trong bộ sưu tập dưới dạng ô "Không còn khả dụng"
(dùng cùng cách hiển thị với spec 030), người dùng tự bỏ ra được; spec 060 đã giữ bookmark khi tác
giả lưu trữ công thức.
(6) Cung cấp danh sách bộ sưu tập cho hộp chọn món của meal planner (spec 030 lọc nhanh "từ bộ sưu
tập đã lưu"): ghi vào Integration Points.
Để /speckit-clarify chốt (ghi vào Assumptions kèm mặc định đề xuất): "Yêu thích" là một bộ sưu tập
mặc định hay danh sách riêng; bộ sưu tập riêng tư hay chia sẻ được; giới hạn số bộ sưu tập và số
công thức mỗi bộ.
Đủ 4 trạng thái màn hình (chưa có bộ sưu tập, bộ sưu tập rỗng, lỗi mạng / trùng tên / công thức không
còn, có dữ liệu). Mọi kiểm tra quyền phía server; chỉ chủ sở hữu xem và sửa bộ sưu tập của mình.
Không bao gồm: tìm kiếm (020), meal planner (030/031), shopping list (040), đăng và kiểm duyệt công
thức (060/100).
Dữ liệu: collections, collection_recipes theo docs/analysis-and-design/database/README.md.
Viết spec bằng tiếng Anh, cùng cấu trúc với specs/020-recipe-search-filter/spec.md. Dòng Feature
Branch để chờ mã Jira. Không hỏi lại trong lúc specify; điểm nào chưa rõ ghi vào Assumptions.
```

### Tóm tắt phản hồi của AI

Số liệu dưới đây đếm trên file spec hiện tại (đã qua `/speckit-clarify` ở Mục 6).

- Tạo `specs/080-bookmark-collection/spec.md` và `checklists/requirements.md`:
  - 5 user story: lưu công thức bằng một chạm (P1), đưa một công thức vào nhiều bộ sưu tập (P1), tạo – đổi tên – xóa bộ sưu tập (P2), xem "Đã lưu" và các bộ sưu tập (P2), công thức không còn khả dụng (P2);
  - 26 yêu cầu chức năng (FR-001 → FR-026), 7 yêu cầu phi chức năng, 23 trường hợp biên, 10 tiêu chí thành công;
  - dòng Feature Branch là `feature/SCRUM-xx-…` chờ mã Jira.
- Bảng **Integration Points** gồm 8 điểm: hộp chọn món "From my collections" của spec 030, cách hiển thị "Không còn khả dụng", công thức ARCHIVED/bị gỡ (060, 100), thông báo mất mạng, thẻ công thức và biểu tượng lưu (020, 060, design system), xóa tài khoản (011), tín hiệu ưa thích cho AI planner (031), các bảng `collections`, `collection_recipes`.
- Theo ghi chú trong checklist: không dùng `[NEEDS CLARIFICATION]`; ba điểm chưa rõ ("Yêu thích" là bộ sưu tập mặc định hay danh sách riêng, riêng tư hay chia sẻ, giới hạn số lượng) được ghi vào Assumptions với mặc định đề xuất để chốt ở `/speckit-clarify`. Checklist đạt 16/16.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| 5 user story và độ ưu tiên | Dùng | Khớp đủ các mục (1)–(5) trong prompt. Hai story P1 (lưu một chạm, đưa vào nhiều bộ sưu tập) là đúng hai ý của module 3.8 "save favorite recipes and organize them into themed cookbooks"; các story còn lại dựa trên hai story này. |
| Danh sách mặc định tên "Yêu thích" (theo prompt) | Chỉnh sửa: đổi thành "Đã lưu" (Saved) ở bước clarify, xem Mục 6 | Danh sách này chứa mọi công thức đã lưu và các bộ sưu tập là tập con của nó, nên "Yêu thích" dễ bị hiểu là chỉ món thích nhất. "Đã lưu" cũng khớp cách 030 FR-007 gọi "recipes the user has saved". |
| Giới hạn số lượng, bộ sưu tập riêng tư, hiển thị "Không còn khả dụng" | Đã chốt ở `/speckit-clarify`, xem Mục 6 | Prompt specify cố ý không cho AI hỏi lại, để các điểm này vào Assumptions kèm mặc định. Mình muốn xem phương án và hệ quả với 030, 060 ở bước clarify trước khi quyết. |

### Cách kiểm chứng

- Đếm trong spec: 0 `[NEEDS CLARIFICATION]`; 26 FR, 7 NFR, 10 SC; checklist 16/16 mục đã đánh dấu.
- Lưu ý trong checklist: phần Notes vẫn ghi ba quyết định còn mở và dòng "Yêu thích" (viết trước khi clarify), chưa cập nhật theo kết quả Mục 6.

---

## Mục 4: Viết spec 011 (hồ sơ cá nhân và dietary profile) bằng `/speckit-specify`

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-18 |
| Giờ truy cập | 20:49 |
| Người thực hiện | Mai Văn Hiển |
| Spec / Tài liệu liên quan | 011 – Hồ sơ cá nhân và dietary profile (module 3.1, `specs/011-user-profile-dietary`) |
| Công cụ AI | Claude Code trong Google Antigravity, model Sonnet 5.5, lệnh Spec Kit `/speckit-specify` |
| Mục đích | Sinh bản đặc tả đầu tiên cho hồ sơ, avatar, dietary profile (nguồn sự thật cho 020, 030, 031), đổi/đặt mật khẩu và xóa tài khoản |
| Nhánh Git | `Hien` |

### Dòng tóm tắt theo mẫu AI Usage Notes

**Claude Code.** *Sonnet 5.5, Anthropic, Claude Code trong Google Antigravity (Spec Kit `/speckit-specify`)*, truy cập 20:49 ngày 18/10/2026, prompt: "/speckit-specify SPECIFY_FEATURE_DIRECTORY=specs/011-user-profile-dietary …" (nguyên văn bên dưới), dùng để viết bản đặc tả đầu tiên của spec 011; AI tạo `spec.md` (5 user story, 44 FR, 8 NFR, Integration Points 17 dòng) và checklist; sinh viên xác định phạm vi, định nghĩa luật cứng/ưu tiên trong prompt, rồi sửa giới hạn nguyên liệu loại trừ từ 200 xuống 50 và quyền xem của Admin ở bước clarify (Mục 5).

### Prompt đã dùng

```
/speckit-specify SPECIFY_FEATURE_DIRECTORY=specs/011-user-profile-dietary
Người dùng đã đăng nhập (Member trở lên, theo specs/010-user-authentication-roles) quản lý hồ sơ cá
nhân và hồ sơ ăn uống. Spec này là NGUỒN SỰ THẬT của dietary profile mà 020, 030, 031 đang dùng.
Phạm vi:
(1) Hồ sơ: sửa tên hiển thị (tối đa 50 ký tự như 010 FR-001), giới thiệu, avatar (JPEG/PNG/WebP
≤ 5 MB như spec 060, chặn ngay trên form). Phần công khai: tên hiển thị, avatar, giới thiệu, công
thức đã xuất bản. Phần riêng tư (chỉ chủ tài khoản và Admin): email, toàn bộ hồ sơ ăn uống, số người
trong gia đình.
(2) Hồ sơ ăn uống, mọi trường đều không bắt buộc:
- Chất gây dị ứng: chọn từ danh sách cố định (bảng allergens do spec này giữ). Liên kết nguyên liệu →
  chất dị ứng (ingredient_allergens) do B giữ, moderator/admin nhập, là nguồn lọc duy nhất; nhãn do
  LLM của 090 không dùng để lọc (theo 020 Clarifications Q1).
- Nguyên liệu loại trừ: chọn từ danh mục nguyên liệu dùng chung, gợi ý khi gõ, không phân biệt dấu.
- Kiểu ăn (chay, thuần chay, keto…), chọn một.
- Ẩm thực ưa thích, chọn nhiều.
- Số người trong gia đình: số nguyên 1–20.
- Thời gian nấu tối đa: TỔNG thời gian chuẩn bị + nấu, để trống hoặc 10–240 phút.
(3) Định nghĩa thống nhất cho các spec khác:
- LUẬT CỨNG = dị ứng và nguyên liệu loại trừ: hệ thống không bao giờ tự chọn công thức vi phạm; nếu
  công thức vi phạm vẫn hiện ra thì phải kèm cảnh báo nêu tên chất/nguyên liệu xung đột. Cách hiển thị
  cụ thể do từng spec quyết định (020 ẩn và cho xem lại, 030 xếp cuối kèm xác nhận, 031 loại hẳn).
- ƯU TIÊN = kiểu ăn, thời gian nấu tối đa, ẩm thực ưa thích: dùng để xếp hạng hoặc điền sẵn. Riêng
  hộp thoại AI planner (031), người dùng có thể xác nhận kiểu ăn và thời gian nấu thành luật cứng cho
  lần tạo đó.
- Khẩu phần mặc định = số người trong gia đình; trống thì mọi spec dùng 2.
- Sửa hồ sơ áp dụng từ thao tác kế tiếp, không thay đổi dữ liệu đã tạo (meal plan đã xếp, shopping
  list đã có).
(4) Đổi mật khẩu: cần mật khẩu hiện tại, xong thì đăng xuất các phiên khác. Tài khoản chỉ đăng nhập
bằng Google thì hiện "Đặt mật khẩu" thay vì "Đổi mật khẩu".
(5) Xóa tài khoản: xác thực lại (mật khẩu hoặc Google) và xác nhận, xóa ngay. Công thức đã xuất bản
giữ lại, tác giả hiện "Người dùng đã xóa"; bản nháp và bài đang chờ duyệt bị xóa; đánh giá giữ ở dạng
ẩn danh; Cooksnap bị xóa; hồ sơ ăn uống, meal plan, shopping list, bộ sưu tập bị xóa. Admin cuối cùng
không được tự xóa tài khoản. Đánh giá và Cooksnap ghi vào Integration Points để E xác nhận.
Đủ 4 trạng thái màn hình (đang tải; rỗng: chưa có hồ sơ ăn uống, có lời mời hoàn thiện; lỗi: mất
mạng / hết phiên / dữ liệu không hợp lệ, không mất dữ liệu đã nhập; có dữ liệu). Mọi kiểm tra quyền và
dữ liệu làm phía server (constitution nguyên tắc IV). Responsive từ 360px.
Không bao gồm: đăng ký, đăng nhập, xác minh email, quên mật khẩu, vai trò, đình chỉ/cấm tài khoản
(010); logic lọc và xếp hạng của 020, 030, 031; dữ liệu kiểu ăn của công thức (ghi vào Integration
Points cho B và C); ước tính dinh dưỡng (090).
Dữ liệu: users, dietary_profiles, diet_types, allergens, user_allergens, user_excluded_ingredients,
cùng bảng ẩm thực ưa thích nếu cần (đánh dấu là đề xuất). database/README.md chưa có; tham khảo
docs/analysis-and-design/database/draft-recipes-ingredients.md cho phần nguyên liệu và dị ứng.
Viết spec bằng tiếng Anh, cùng cấu trúc và văn phong với specs/020-recipe-search-filter/spec.md
(Summary, In scope/Out of scope, User Stories có độ ưu tiên và Given/When/Then, Edge Cases, FR, NFR,
Key Entities, Success Criteria, Assumptions, bảng Integration Points ghi rõ spec nào phải sửa gì:
030 FR-012, 031 FR-009, 010 NFR-009 và Out of scope). Dòng Feature Branch để chờ mã Jira. Không hỏi
lại trong lúc specify; điểm nào chưa rõ thì chọn mặc định hợp lý và ghi vào Assumptions.
```

### Tóm tắt phản hồi của AI

Số liệu dưới đây đếm trên file spec hiện tại (đã qua `/speckit-clarify` ở Mục 5).

- Tạo `specs/011-user-profile-dietary/spec.md` và `checklists/requirements.md`:
  - 5 user story: thiết lập dietary profile (P1), sửa hồ sơ công khai và avatar (P1), đổi/đặt mật khẩu (P2), xóa tài khoản (P2), định nghĩa thống nhất luật cứng – ưu tiên cho các spec khác (P2);
  - 44 yêu cầu chức năng (FR-001 → FR-042, gồm các mã có hậu tố chữ cái), 8 yêu cầu phi chức năng, 17 trường hợp biên, 8 tiêu chí thành công;
  - dòng Feature Branch là `feature/SCRUM-xx-user-profile-dietary` chờ mã Jira.
- Bảng **Integration Points** gồm 17 điểm cần thống nhất với A, B, C, E: định nghĩa luật cứng/ưu tiên (020, 030, 031), khẩu phần mặc định (030 FR-012), luật cứng ở 031 FR-009, thứ tự gợi ý món của 030, 010 NFR-009 và Out of scope, đăng xuất các phiên khi đổi mật khẩu, Administrator cuối cùng, tài khoản bị cấm/đình chỉ khi xóa, liên kết dị ứng – nguyên liệu, gộp/xóa nguyên liệu, kiểu ăn trên công thức, đánh giá và Cooksnap khi xóa tài khoản, meal plan/shopping list/bộ sưu tập, công thức đã xuất bản sau khi xóa, ẩm thực ưa thích.
- Theo phần Assumptions và Notes của checklist, các mặc định do AI chọn khi specify: giới thiệu tối đa 300 ký tự, nguyên liệu loại trừ tối đa 200 (về sau đổi thành 50 ở `/speckit-clarify`), không đổi email, không có thời gian ân hạn khi xóa, danh sách dị ứng và kiểu ăn cố định chỉ đổi bằng migration, đánh giá giữ ẩn danh chờ E xác nhận, bảng `user_preferred_cuisines` đề xuất thêm. Checklist đạt 16/16, không có `[NEEDS CLARIFICATION]`.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| 5 user story và độ ưu tiên | Dùng | Dietary profile để P1 vì đây là nguồn sự thật mà 020, 030, 031 đang dùng (ý chính của prompt). Story "định nghĩa thống nhất luật cứng – ưu tiên" giải quyết các mâu thuẫn tìm thấy ở Mục 2. |
| Nguyên liệu loại trừ tối đa 200 (mặc định của AI) | Chỉnh sửa: đổi thành 50 ở `/speckit-clarify`, xem Mục 5 | Danh sách quá dài làm 020 và 031 gần như không ra kết quả. |
| Admin xem được toàn bộ hồ sơ ăn uống (theo prompt: "chỉ chủ tài khoản và Admin") | Chỉnh sửa: Admin chỉ xem email và thông tin tài khoản, xem Mục 5 | Prompt của mình lúc đầu viết chưa chặt. Dị ứng và kiểu ăn là thông tin sức khỏe, nên theo nguyên tắc quyền tối thiểu Admin không cần xem. |
| Bảng `user_preferred_cuisines` đề xuất thêm | Dùng tạm (Assumptions ghi cần thống nhất trong nhóm) | 030 FR-007a và 031 FR-002 đã dùng "ẩm thực ưa thích" nhưng file phân công chưa có bảng này. Prompt yêu cầu đánh dấu là đề xuất, chờ nhóm duyệt khi chốt đặc tả CSDL. |

### Cách kiểm chứng

- Đếm trong spec: 0 `[NEEDS CLARIFICATION]`; 44 FR, 8 NFR, 8 SC; checklist 16/16 mục đã đánh dấu.
- Lưu ý trong checklist: dòng Notes vẫn ghi "200 exclusions" trong khi spec hiện tại là 50 (viết trước khi clarify), chưa cập nhật.

---

## Mục 5: Làm rõ spec 011 bằng `/speckit-clarify`

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-18 |
| Giờ truy cập | 21:05 |
| Người thực hiện | Mai Văn Hiển |
| Spec / Tài liệu liên quan | 011 – Hồ sơ cá nhân và dietary profile (`specs/011-user-profile-dietary`) |
| Công cụ AI | Claude Code trong Google Antigravity, model Sonnet 5.5, lệnh Spec Kit `/speckit-clarify` |
| Mục đích | Chốt trường nào là luật cứng, khẩu phần mặc định, hậu quả khi xóa tài khoản, các giá trị mặc định ở Assumptions, gộp/xóa nguyên liệu và tài khoản bị đình chỉ/cấm |
| Nhánh Git | `Hien` |

### Dòng tóm tắt theo mẫu AI Usage Notes

**Claude Code.** *Sonnet 5.5, Anthropic, Claude Code trong Google Antigravity (Spec Kit `/speckit-clarify`)*, truy cập 21:05 ngày 18/10/2026, prompt: "/speckit-clarify Rà soát specs/011-user-profile-dietary/spec.md và hỏi mình lần lượt từng câu, tối đa 5 câu …" (nguyên văn bên dưới), dùng để làm rõ spec 011; AI đặt câu hỏi kèm phương án và ghi 6 mục Clarifications, sửa đồng bộ spec; sinh viên tự chọn đáp án từng câu và đổi 3 điểm so với đề xuất (50 nguyên liệu loại trừ, Admin không xem hồ sơ ăn uống, thêm bước gõ `DELETE`).

### Prompt đã dùng

```
/speckit-clarify
Rà soát specs/011-user-profile-dietary/spec.md và hỏi mình lần lượt từng câu, tối đa 5 câu, ưu tiên:
(1) trường nào của dietary profile là luật cứng cho tìm kiếm (020) và AI planner (031);
(2) khẩu phần mặc định khi chưa nhập số người;
(3) xóa tài khoản thì công thức, đánh giá, Cooksnap, bộ sưu tập ra sao;
(4) các giá trị mặc định đang ghi trong Assumptions.
Mỗi câu kèm phương án đề xuất, lý do và hệ quả với các spec 020, 030, 031, 060. Sau khi mình trả lời,
ghi vào Clarifications và sửa đồng bộ FR, User Story, Edge Cases, Assumptions, Integration Points;
không để lại giá trị cũ mâu thuẫn.
```

Các câu trả lời mình gửi lần lượt cho AI (nguyên văn):

```
A
Số người trong gia đình là số nguyên 1–20, không bắt buộc. Khi trống, mọi spec dùng 2.
Xác thực lại (mật khẩu hoặc Google), rồi xóa ngay. Công thức đã xuất bản giữ lại, tác giả hiện “Người dùng đã xóa”. Bản nháp và bài đang chờ duyệt xóa. Đánh giá giữ, ẩn danh (giữ rating_avg ổn định). Cooksnap xóa (ảnh cá nhân). Hồ sơ, meal plan, shopping list, bộ sưu tập xóa. Admin cuối cùng không được tự xóa.
Giữ nguyên, trừ 2 điểm:
- Nguyên liệu loại trừ tối đa 50 thay vì 200 (danh sách quá dài làm 020 và 031 gần như không ra kết quả).
- Admin chỉ xem được email và thông tin tài khoản, KHÔNG xem hồ sơ ăn uống (dị ứng, kiểu ăn là thông tin
  sức khỏe, theo nguyên tắc quyền tối thiểu); Admin không sửa được hồ sơ, hồ sơ ăn uống hay mật khẩu của
  người khác.
Thêm: xóa tài khoản phải xác thực lại VÀ gõ chữ xác nhận (DELETE), màn xác nhận liệt kê dữ liệu bị xóa
và dữ liệu được giữ ở dạng ẩn danh.
Cập nhật đồng bộ FR, NFR, Edge Cases và Integration Points (010 NFR-009 cũng phải sửa theo điểm Admin).
Chọn A. Bổ sung:
- Khi gộp, nếu người dùng đã loại trừ sẵn nguyên liệu đích thì chỉ giữ một lựa chọn, không tạo bản trùng.
- Ghi vào Integration Points: nguyên liệu chỉ được xóa hẳn khi không còn công thức nào dùng nó (quy tắc của
  danh mục nguyên liệu – B/060 và màn quản trị – E/100), nên việc bỏ khỏi hồ sơ không làm lọt công thức
  nguy hiểm; thao tác gộp nguyên liệu ở spec 100 phải kích hoạt việc chuyển lựa chọn trong hồ sơ của 011.
  cập nhật quy tắc xóa tài khoản theo "Editing the display name, changing/setting a password, deleting the account and the dietary profile are owned by spec 011; Out of scope updated"; “tài khoản đang bị đình chỉ không được tự xóa; tài khoản bị cấm khi xóa thì giữ mã băm email trong danh sách chặn
```

Spec ghi 6 câu hỏi – trả lời (câu 6 phát sinh từ yêu cầu cập nhật quy tắc xóa tài khoản ở cuối lượt trả lời trên):

| # | Câu hỏi của AI | AI đề xuất | Mình chọn |
|---|---|---|---|
| 1 | Ngoài dị ứng và nguyên liệu loại trừ, kiểu ăn và thời gian nấu tối đa có là luật cứng cho tìm kiếm (020) và AI planner (031) không? | Đề xuất A (chỉ dị ứng và nguyên liệu loại trừ là luật cứng; kiểu ăn, thời gian, ẩm thực là ưu tiên; riêng hộp thoại 031 cho xác nhận thành luật cứng một lần). Lý do: dị ứng là an toàn, còn kiểu ăn dễ ẩn nhầm công thức chưa gắn nhãn. Phương án khác: B (031 luôn coi kiểu ăn và thời gian là luật cứng), C (kiểu ăn cũng là luật cứng ở 020 và 031) | A – Không: chỉ dị ứng và nguyên liệu loại trừ là luật cứng; kiểu ăn, thời gian nấu tối đa, ẩm thực ưa thích là ưu tiên (xếp hạng/điền sẵn). Trong hộp thoại 031, người dùng có thể xác nhận kiểu ăn và thời gian nấu thành luật cứng cho đúng một lần tạo |
| 2 | Khẩu phần mặc định khi người dùng chưa nhập số người trong gia đình là bao nhiêu? | Đề xuất A (mặc định 2 ở mọi spec; sửa 030 FR-012). Lý do: một con số cố định, dễ kiểm thử, khớp 031. Phương án khác: B (dùng khẩu phần của công thức như 030 cũ), C (bắt nhập số người ở lần đầu) | Là 2 ở mọi spec; số người trong gia đình vẫn không bắt buộc (số nguyên 1–20); khi trống, 030, 031, 040 đều dùng 2, không dùng khẩu phần của công thức |
| 3 | Khi xóa tài khoản, công thức, đánh giá, Cooksnap và dữ liệu cá nhân ra sao? | Đề xuất A (giữ công thức PUBLISHED với tác giả "Người dùng đã xóa"; xóa DRAFT, PENDING, REJECTED, REVISION_REQUESTED và ARCHIVED). Lý do: công thức đã xuất bản là nội dung cộng đồng có thể nằm trong kế hoạch của người khác. Phương án khác: B (giữ cả ARCHIVED), C (xóa luôn công thức đã xuất bản), D (cho người dùng chọn) | Xác thực lại (mật khẩu hoặc Google) rồi xóa ngay. Công thức đã xuất bản giữ lại, tác giả hiện "Người dùng đã xóa"; bản nháp, bài đang chờ duyệt (và các công thức chưa xuất bản khác) bị xóa; đánh giá giữ ẩn danh, `rating_avg` và số đánh giá không đổi; Cooksnap xóa; hồ sơ ăn uống, meal plan, shopping list, bộ sưu tập xóa; Admin cuối cùng không được tự xóa |
| 4 | Các giá trị mặc định ở Assumptions (giới thiệu 300 ký tự, nguyên liệu loại trừ, không đổi email, danh sách cố định) có giữ không, và ai được xem gì? | Đề xuất A (giữ nguyên: giới thiệu 300 ký tự, 200 nguyên liệu loại trừ, không đổi email, danh sách dị ứng và kiểu ăn chỉ đổi bằng migration). Phương án khác: B (Admin sửa danh sách trên màn hình), C (đổi giới hạn), D (cho đổi email trong 011) | Giữ nguyên trừ 2 điểm: nguyên liệu loại trừ tối đa **50** (thay vì 200); Admin chỉ xem email và thông tin tài khoản, **không** xem hồ sơ ăn uống và số người trong gia đình, không sửa hồ sơ/mật khẩu của người khác. Thêm: xóa tài khoản phải xác thực lại **và** gõ `DELETE`, màn xác nhận liệt kê dữ liệu bị xóa và dữ liệu giữ ẩn danh |
| 5 | Khi nguyên liệu bị loại trừ được gộp vào nguyên liệu khác hoặc bị xóa khỏi danh mục thì sao? | Đề xuất A (gộp thì chuyển sang nguyên liệu đích, xóa thì bỏ khỏi hồ sơ, đều báo ở lần mở hồ sơ kế tiếp). Lý do: giữ ý định của người dùng mà không để luật cứng mất âm thầm. Phương án khác: B (chặn xóa nguyên liệu đang có trong hồ sơ), C (giữ và đánh dấu "không còn tồn tại") | Chọn A, bổ sung: khi gộp, lựa chọn chuyển sang nguyên liệu đích và chỉ giữ một mục nếu đã loại trừ sẵn nguyên liệu đích; khi xóa, bỏ khỏi hồ sơ (danh mục chỉ cho xóa nguyên liệu không còn công thức nào dùng); chủ tài khoản được báo ở lần ghé kế tiếp; thao tác gộp của spec 100 kích hoạt việc chuyển |
| 6 | Tài khoản bị đình chỉ/cấm xử lý thế nào khi xóa, và ai sở hữu các quy tắc tài khoản? | Không áp dụng (yêu cầu cập nhật của mình ở cuối lượt trả lời trước, không phải câu hỏi của AI) | Theo yêu cầu cập nhật của mình: spec 011 sở hữu việc sửa tên hiển thị, đổi/đặt mật khẩu, xóa tài khoản và dietary profile (khớp Clarifications của 010). Tài khoản đình chỉ có thời hạn không tự xóa cho tới khi hết đình chỉ; tài khoản bị cấm vẫn được tự xóa và chỉ giữ mã băm một chiều của email trong danh sách chặn; tài khoản khác bị xóa thì không giữ gì và email dùng lại được ngay |

### Tóm tắt phản hồi của AI

- Thêm mục **Clarifications / Session 2026-10-18** với 6 câu hỏi – trả lời; câu 3 còn ghi chú rằng việc xóa cả công thức đã lưu trữ là cách AI hiểu "chưa xuất bản", cần báo lại nếu muốn giữ.
- Spec hiện tại có 44 FR, 8 NFR, 17 trường hợp biên, 8 SC và bảng Integration Points 17 dòng, trong đó có các dòng khớp câu trả lời: khẩu phần mặc định 2 (030 FR-012), luật cứng/ưu tiên (031 FR-009), 010 NFR-009 hai mức hiển thị, Administrator cuối cùng, tài khoản bị cấm/đình chỉ khi xóa, gộp/xóa nguyên liệu.
- Assumptions: giới hạn 50 nguyên liệu loại trừ ghi là do nhóm đặt ra; số người trong gia đình được coi là một phần của dietary profile nên cũng ẩn với Admin; xóa tài khoản không có thời gian ân hạn.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Nguyên liệu loại trừ tối đa 200 (giá trị mặc định ở Assumptions) | Chỉnh sửa → 50 | Danh sách quá dài làm 020 và 031 gần như không ra kết quả. |
| Admin xem hồ sơ ăn uống | Chỉnh sửa → Admin chỉ xem email và thông tin tài khoản | Dị ứng và kiểu ăn là thông tin sức khỏe; theo nguyên tắc quyền tối thiểu, Admin không xem và không sửa hồ sơ, hồ sơ ăn uống hay mật khẩu của người khác. |
| Xóa tài khoản chỉ cần xác thực lại | Chỉnh sửa → thêm bước gõ `DELETE` và màn liệt kê dữ liệu bị xóa/giữ ẩn danh | Tài khoản bị xóa ngay, không có thời gian ân hạn và không khôi phục được, nên cần thêm một bước chủ động để tránh bấm nhầm. Màn liệt kê giúp người dùng biết trước dữ liệu nào mất và dữ liệu nào giữ ẩn danh. |
| Chỉ kiểu ăn và thời gian nấu là ưu tiên, không phải luật cứng | Dùng | Giải quyết mâu thuẫn ở Mục 2 (kiểu ăn là luật cứng ở 031 nhưng không ở 020). Dị ứng và nguyên liệu loại trừ là vấn đề an toàn, kiểu ăn là sở thích. Người dùng vẫn có thể bật thành luật cứng cho một lần tạo ở 031. |
| Khẩu phần mặc định 2 khi trống | Dùng | Giải quyết mâu thuẫn 030 (khẩu phần gốc của công thức) và 031 (2) ở Mục 2. Khẩu phần gốc có thể tới 50, vượt miền 1–20 của 030, nên dùng một giá trị chung cho mọi spec là đơn giản nhất. |
| Quy tắc gộp nguyên liệu (không tạo bản trùng) | Dùng, có bổ sung của mình | Gộp không được làm mất lựa chọn loại trừ của người dùng, cũng không tạo bản trùng. Danh mục chỉ cho xóa nguyên liệu khi không còn công thức nào dùng, nên bỏ nguyên liệu khỏi hồ sơ không làm lọt món nguy hiểm. |

### Cách kiểm chứng

- Mục Clarifications của spec có đúng 6 dòng; các giá trị 50, `DELETE`, khẩu phần mặc định 2 xuất hiện thống nhất trong Edge Cases, Assumptions và Integration Points (Edge Cases ghi "up to 50 ingredients", Assumptions ghi 50).
- Đối chiếu chéo với spec 010: Clarifications 2026-10-18 của 010 cũng ghi 011 sở hữu việc sửa tên hiển thị, đổi/đặt mật khẩu, xóa tài khoản và dietary profile, và mô tả cùng quy tắc tài khoản bị cấm/đình chỉ khi xóa.
- Lưu ý còn tồn tại: phần Notes của `checklists/requirements.md` vẫn ghi "200 exclusions" (xem Mục 4).
- Lưu ý: dòng cuối của lượt trả lời trên (cập nhật quy tắc xóa tài khoản) nằm cùng đoạn với câu trả lời câu 5 trong bản nháp prompt, nên được chép nguyên văn như trên.

---

## Mục 6: Làm rõ spec 080 bằng `/speckit-clarify`

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-18 |
| Giờ truy cập | 21:27 |
| Người thực hiện | Mai Văn Hiển |
| Spec / Tài liệu liên quan | 080 – Bookmark và bộ sưu tập (`specs/080-bookmark-collection`) |
| Công cụ AI | Claude Code trong Google Antigravity, model Claude Opus 5.5, lệnh Spec Kit `/speckit-clarify` |
| Mục đích | Chốt quyền riêng tư của bộ sưu tập, giới hạn số lượng, cách hiển thị "Không còn khả dụng", hành vi khi mất mạng và việc hoàn tác khi bỏ lưu |
| Nhánh Git | `Hien` |

### Dòng tóm tắt theo mẫu AI Usage Notes

**Claude Code.** *Claude Opus 5.5, Anthropic, Claude Code trong Google Antigravity (Spec Kit `/speckit-clarify`)*, truy cập 21:27 ngày 18/10/2026, prompt: "/speckit-clarify Rà soát specs/080-bookmark-collection/spec.md và hỏi mình lần lượt từng câu, tối đa 5 câu …" (nguyên văn bên dưới), dùng để làm rõ spec 080; AI đặt 5 câu hỏi kèm phương án và ghi mục Clarifications, sửa đồng bộ spec; sinh viên tự chọn đáp án, đổi "Yêu thích" thành "Đã lưu", đặt giới hạn 1.000 / 50 / 500 và thêm thao tác "Bỏ tất cả món không còn khả dụng".

### Prompt đã dùng

```
/speckit-clarify
Rà soát specs/080-bookmark-collection/spec.md và hỏi mình lần lượt từng câu, tối đa 5 câu, ưu tiên:
(1) bộ sưu tập riêng tư hay chia sẻ được;
(2) giới hạn số bộ sưu tập và số công thức mỗi bộ;
(3) cách hiển thị và xử lý công thức "Không còn khả dụng" (ARCHIVED hoặc bị gỡ), khớp với spec 030;
(4) các giá trị mặc định đang ghi trong Assumptions.
Không hỏi lại về danh sách "Đã lưu" mặc định (đã chốt trong lúc specify). Mỗi câu kèm phương án đề
xuất, lý do và hệ quả với các spec 030 và 060. Sau khi mình trả lời, ghi vào Clarifications và sửa đồng
bộ FR, User Story, Edge Cases, Assumptions, Integration Points; không để lại giá trị cũ mâu thuẫn.
```

Các câu trả lời mình gửi lần lượt cho AI (nguyên văn):

```
Chọn A. Bộ sưu tập chỉ riêng tư, không ai khác thấy kể cả Admin (khớp nguyên tắc quyền tối thiểu đã chốt ở
spec 011). Chia sẻ bằng link hoặc công khai ghi vào Out of scope là để bản sau. Ghi vào Integration Points:
hộp chọn món của 030 chỉ đọc bộ sưu tập của chính người dùng.
Chọn A, chỉnh như sau:
- Danh sách mặc định tên là "Đã lưu" (Saved), không phải "Yêu thích"; kiểm tra và thống nhất tên này trong
  toàn bộ spec.
- 50 bộ sưu tập mỗi người, KHÔNG tính "Đã lưu"; 500 công thức mỗi bộ sưu tập.
- "Đã lưu" tối đa 1.000 công thức (vì mọi công thức trong bộ sưu tập đều phải nằm trong "Đã lưu", nên đây
  là trần tổng số món đã lưu). Khi đầy, nút lưu một chạm báo "Danh sách Đã lưu đã đầy" kèm gợi ý bỏ món
  không còn khả dụng.
- Thêm thao tác "Bỏ tất cả món không còn khả dụng" trong mỗi bộ sưu tập và trong "Đã lưu".
- Ghi vào Integration Points: hộp chọn món của 030 cần phân trang hoặc tìm theo tên trong một bộ sưu tập.
Cập nhật đồng bộ FR-011, Edge Cases và Success Criteria.
Chọn A. Bổ sung:
- Mục "Không còn khả dụng" chỉ hiện tên món đã lưu, không hiện ảnh, không mở được trang chi tiết; bấm vào thì báo công thức không còn khả dụng và có nút bỏ khỏi danh sách. Công thức được khôi phục thì tự hiện lại bình thường.
- Thanh số lượng và nút "Bỏ tất cả món không còn khả dụng" chỉ hiện khi có ít nhất 1 món như vậy; bấm "Bỏ tất cả" phải xác nhận trước.
- Ghi vào Integration Points: dùng chung cách hiển thị "Không còn khả dụng" với ô lịch của 030; công thức bị moderator gỡ (100) xử lý giống ARCHIVED.
Chọn A. Bổ sung: khi mất mạng, nút lưu/bỏ lưu một chạm trên trang chi tiết và thẻ công thức báo lỗi ngay và trả biểu tượng về trạng thái cũ, không xếp hàng gửi sau. Dùng chung thông báo mất mạng với 030 (FR-027, FR-028). Ghi vào Assumptions lý do: nấu offline đã do 050 đảm nhận, đi chợ offline do 040.Chọn A. Undo khôi phục đúng trạng thái trước khi bỏ lưu: món quay lại "Đã lưu" và mọi bộ sưu tập cũ, đúng vị trí; nếu công thức đã bị ARCHIVED hoặc gỡ trong lúc đó thì hiện là "Không còn khả dụng". Ghi rõ đây là ngoại lệ của FR-009 (Undo là hoàn tác, không phải lưu mới). Ghi vào Edge Cases: bỏ lưu không ảnh hưởng món đã xếp trong meal plan (030), chỉ làm món đó không còn hiện trong "From my collections".
```

| # | Câu hỏi của AI | AI đề xuất | Mình chọn |
|---|---|---|---|
| 1 | Bộ sưu tập là riêng tư hay có thể chia sẻ với người khác? | Đề xuất **A** (chỉ riêng tư) vì proposal 3.8 ghi "private culinary repertoire", không cần kiểm duyệt tên, không lộ món người khác đã lưu. Các phương án khác: B – riêng tư nhưng bật được link chỉ-xem; C – mỗi bộ chọn công khai/riêng tư | A – Chỉ riêng tư: không ai ngoài chủ sở hữu thấy tên hay nội dung, kể cả Moderator và Administrator (quyền tối thiểu như spec 011). Chia sẻ bằng link và công khai để bản sau. Hộp chọn món của 030 chỉ đọc bộ sưu tập của chính người dùng |
| 2 | Mỗi người tạo được bao nhiêu bộ sưu tập, mỗi bộ chứa bao nhiêu công thức? | Đề xuất **A**: 50 bộ sưu tập (tính cả danh sách mặc định), 500 công thức mỗi bộ, vượt xa nhu cầu thực tế nhưng chặn được lạm dụng. Các phương án khác: B – 20/200; C – 100/1.000; D – không giới hạn số bộ, 500 công thức mỗi bộ | A, chỉnh: 50 bộ sưu tập mỗi người không tính danh sách mặc định (đổi tên thành "Đã lưu"/Saved thay vì "Yêu thích"); 500 công thức mỗi bộ; "Đã lưu" tối đa 1.000 công thức (là trần tổng số món đã lưu). Khi đầy, nút lưu báo "Danh sách Đã lưu đã đầy" kèm gợi ý bỏ món không còn khả dụng; thêm thao tác "Bỏ tất cả món không còn khả dụng"; hộp chọn món của 030 phải phân trang hoặc tìm theo tên |
| 3 | Công thức đã lưu bị lưu trữ hoặc gỡ thì mục "Không còn khả dụng" hiện ở đâu và có tự dọn không? | Đề xuất **A** (giữ nguyên vị trí, không bao giờ tự dọn, đầu trang có số lượng và nút "Bỏ tất cả") vì khớp ô lịch của 030 và không tự xóa dữ liệu của người dùng (060 đã cam kết giữ bookmark). Các phương án khác: B – gom vào mục riêng ở cuối danh sách; C – tự bỏ sau 30 ngày | A, bổ sung: mục giữ nguyên vị trí, không tự xóa; chỉ hiện tên món đã lưu, không ảnh, không mở trang chi tiết (bấm vào thì báo không còn khả dụng và có nút bỏ khỏi danh sách); công thức được khôi phục thì tự hiện lại. Thanh số lượng và nút "Bỏ tất cả món không còn khả dụng" chỉ hiện khi có ít nhất 1 mục như vậy, bấm "Bỏ tất cả" phải xác nhận. Dùng chung cách hiển thị với ô lịch của 030; công thức bị moderator gỡ (100) xử lý như ARCHIVED |
| 4 | Xem "Đã lưu" và bộ sưu tập khi không có kết nối mạng được không? | Đề xuất **A** (cần mạng, không lưu gì trên thiết bị) vì nhất quán với planner 030 và tránh phải đồng bộ tới 1.000 món; nấu khi mất mạng đã có 050. Các phương án khác: B – chỉ xem offline danh sách đã tải; C – xem và sửa offline, đồng bộ last-write-wins như 040 | A, bổ sung: không; các trang bộ sưu tập cần kết nối, offline hiện thông báo như planner (030 FR-027, FR-028) kèm "Thử lại", mọi thao tác bị khóa. Nút lưu/bỏ lưu một chạm báo lỗi ngay, trả biểu tượng về trạng thái cũ, không xếp hàng gửi sau. Lý do ghi vào Assumptions: nấu offline do 050, đi chợ offline do 040 |
| 5 | Khi bỏ lưu công thức đang nằm trong bộ sưu tập thì xác nhận trước, chỉ cho Undo, hay cả hai? | Đề xuất **A** (chỉ xác nhận khi công thức nằm trong ít nhất một bộ, mọi lần bỏ lưu đều có Undo 5 giây) vì chạm nhầm trong bếp là thường gặp, còn bỏ lưu món không nằm trong bộ nào vẫn chỉ một chạm. Các phương án khác: B – không xác nhận, Undo 10 giây; C – luôn xác nhận, không có Undo | A, bổ sung: cả hai như đã đặc tả – xác nhận nêu số bộ sưu tập chỉ hiện khi công thức nằm trong ít nhất một bộ, và mọi lần bỏ lưu đều có Undo 5 giây. Undo khôi phục đúng trạng thái trước đó (về "Đã lưu" và mọi bộ cũ, đúng vị trí; nếu công thức đã bị lưu trữ/gỡ thì hiện là "Không còn khả dụng"); đây là ngoại lệ của FR-009. Bỏ lưu không ảnh hưởng món đã xếp trong meal plan (030), chỉ làm món đó không còn hiện trong "From my collections" |

### Tóm tắt phản hồi của AI

- Thêm mục **Clarifications / Session 2026-10-18** với 5 câu hỏi – trả lời.
- Tên danh sách mặc định thống nhất là "Đã lưu" (Saved) trong toàn bộ spec (Assumptions ghi "Đã lưu" là danh sách dựng sẵn chứa mọi công thức đã lưu, các bộ sưu tập là tập con của nó).
- Spec hiện tại có 26 FR, 7 NFR, 23 trường hợp biên, 10 SC và bảng Integration Points 8 dòng; các dòng "Unavailable presentation", "Archived and removed recipes" và "Offline message" được ghi là **Decided** (Clarifications 2026-10-18).
- Assumptions cập nhật theo câu trả lời: bộ sưu tập chỉ riêng tư (FR-002, NFR-002), các giới hạn 1.000 / 50 / 500 (FR-011), tên bộ sưu tập 1–50 ký tự và không trùng tên (không phân biệt hoa thường) trong cùng một người dùng, xác nhận + Undo 5 giây khi bỏ lưu (FR-006, FR-008), cần kết nối mạng (FR-024, FR-025).

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Bộ sưu tập chỉ riêng tư, chia sẻ để bản sau | Dùng | Khớp nguyên tắc quyền tối thiểu đã chốt ở spec 011 (Admin không xem dữ liệu cá nhân). Chia sẻ sẽ kéo theo kiểm duyệt tên bộ sưu tập (100) và hồ sơ công khai, vượt phạm vi Sprint. |
| Tên danh sách mặc định "Yêu thích" | Chỉnh sửa → "Đã lưu" (Saved) | Danh sách này chứa mọi công thức đã lưu, không chỉ món thích nhất; tên "Đã lưu" khớp 030 FR-007 ("recipes the user has saved"). |
| Giới hạn 50 bộ sưu tập (không tính "Đã lưu"), 500 công thức/bộ, 1.000 công thức trong "Đã lưu" | Chỉnh sửa theo số mình đưa ra | Mọi công thức trong bộ sưu tập đều phải nằm trong "Đã lưu", nên cần một trần riêng cho "Đã lưu" làm trần tổng số món đã lưu. Không tính "Đã lưu" vào 50 để người dùng không mất một chỗ cho danh sách mặc định. |
| Thao tác "Bỏ tất cả món không còn khả dụng" | Dùng (bổ sung mới của mình) | Món không còn khả dụng vẫn tính vào giới hạn, nên khi "Đã lưu" đầy người dùng cần cách dọn nhanh thay vì bỏ từng món. |
| Mục "Không còn khả dụng" chỉ hiện tên, không ảnh, không mở trang chi tiết | Dùng, có bổ sung của mình | Dùng chung cách hiển thị với ô lịch của 030 để người dùng thấy giống nhau ở cả hai nơi. Công thức bị moderator gỡ (100) xử lý như ARCHIVED cho đơn giản. Công thức được khôi phục thì tự hiện lại, khớp cam kết giữ bookmark của 060. |
| Hành vi khi mất mạng: báo lỗi ngay, không xếp hàng gửi sau | Dùng, có bổ sung của mình | Nấu khi mất mạng đã do 050 đảm nhận, đi chợ khi mất mạng do 040; bộ sưu tập không cần offline. Dùng chung thông báo mất mạng với 030 (FR-027, FR-028). |
| Undo khôi phục đúng trạng thái cũ, là ngoại lệ của FR-009 | Dùng, có bổ sung của mình | Undo là hoàn tác chứ không phải lưu mới, nên phải khôi phục cả khi công thức vừa bị lưu trữ. Ghi rõ là ngoại lệ để FR-009 (chỉ lưu công thức PUBLISHED) không mâu thuẫn. |

### Cách kiểm chứng

- Mục Clarifications có đúng 5 mục; Assumptions của spec nêu lại cùng các con số (1.000 / 50 / 500, Undo 5 giây) và dẫn về Clarifications 2026-10-18.
- Spec không còn `[NEEDS CLARIFICATION]`; checklist 16/16 mục đã đánh dấu.
- Lưu ý còn tồn tại: phần Notes của `checklists/requirements.md` vẫn ghi ba quyết định chờ `/speckit-clarify` và tên "Yêu thích" (viết trước clarify), chưa cập nhật (xem Mục 3).
- Lưu ý: ở bản nháp prompt, hai câu trả lời cho câu 4 và câu 5 nằm liền nhau trên cùng một dòng ("…do 040.Chọn A. Undo…"), nên được chép nguyên văn như trên.

---

## Mục 7: Làm rõ spec 010 bằng `/speckit-clarify`

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-18 |
| Giờ truy cập | 21:05 |
| Người thực hiện | Mai Văn Hiển |
| Spec / Tài liệu liên quan | 010 – Đăng ký, đăng nhập và vai trò (`specs/010-user-authentication-roles`) |
| Công cụ AI | Claude Code trong Google Antigravity, model Sonnet 5.5, lệnh Spec Kit `/speckit-clarify` |
| Mục đích | Cắt phần hồ sơ sang spec 011, sửa NFR-009 và Jira key, chốt quy tắc xóa tài khoản, liên kết Google với tài khoản chưa xác minh, thời hạn xác minh email, thông báo và quyền tác động lên Administrator |
| Nhánh Git | `Hien` |

### Dòng tóm tắt theo mẫu AI Usage Notes

**Claude Code.** *Sonnet 5.5, Anthropic, Claude Code trong Google Antigravity (Spec Kit `/speckit-clarify`)*, truy cập 21:05 ngày 18/10/2026, prompt: "Rà soát specs/010-user-authentication-roles/spec.md … Ngoài các câu hỏi thông thường (tối đa 5), cập nhật đồng bộ …" (nguyên văn bên dưới), dùng để làm rõ spec 010 và cắt phạm vi sang 011; AI đặt 5 câu hỏi, ghi 9 mục Clarifications và sửa spec (+112 / −22 dòng); sinh viên đưa 4 chỉ thị trong prompt, tự chọn đáp án và thêm các ngoại lệ (danh sách chặn mã băm email, quy tắc Administrator, xóa dữ liệu khi liên kết Google).

### Prompt đã dùng

```
Rà soát specs/010-user-authentication-roles/spec.md (spec này nay do mình, Mai Văn Hiển, phụ trách).
Ngoài các câu hỏi thông thường (tối đa 5), cập nhật đồng bộ:
- Out of scope: ghi rõ sửa tên hiển thị, đổi/đặt mật khẩu, xóa tài khoản và hồ sơ ăn uống thuộc
  specs/011-user-profile-dietary.
- NFR-009: email chỉ chủ tài khoản và Admin thấy; tên hiển thị và avatar là công khai (theo spec 011).
- Key Entities: thêm trạng thái tài khoản đã xóa nếu spec 011 cần.
- Dòng Feature Branch: bỏ SCRUM-27 (đó là task Sprint Planning trên Jira), để chờ mã Jira mới; sửa
  Assumption về Jira cho khớp.
Ghi các thay đổi vào mục Clarifications.
```

Các câu trả lời mình gửi lần lượt cho AI (nguyên văn):

```
A, kèm ngoại lệ chống né lệnh cấm:
- Tài khoản đang bị cấm (banned) vẫn được tự xóa, nhưng hệ thống giữ mã băm của email trong danh sách chặn (không giữ email gốc); đăng ký lại bằng email đó bị từ chối.
- Tài khoản đang bị đình chỉ có thời hạn (suspended) không được tự xóa cho tới khi hết đình chỉ.
- Nhật ký audit giữ đủ 90 ngày theo FR-025, ở dạng ẩn danh.
Ghi vào Integration Points với spec 011 (luồng xóa tài khoản) và spec 100 (cấm/đình chỉ).
A, bổ sung: vì tài khoản chưa xác minh vẫn lưu được dữ liệu (tên hiển thị, hồ sơ ăn uống của 011, bản nháp của 060, bộ sưu tập của 080), khi liên kết Google thì xóa toàn bộ dữ liệu tạo ra trước khi xác minh và coi như tài khoản mới; tên hiển thị lấy từ tài khoản Google. Lý do: tránh chủ email thật nhận hồ sơ dị ứng hoặc nội dung do người khác đặt sẵn. Ghi vào Edge Cases và Integration Points (011, 060, 080).
B (7 ngày), tính từ lúc đăng ký; xin link xác minh mới không gia hạn thời gian này. Tự xóa là xóa hẳn tài khoản và dữ liệu, email được dùng đăng ký lại ngay, không đưa vào danh sách chặn.
B. Bổ sung: email đình chỉ/cấm ghi rõ lý do, thời hạn (với đình chỉ) và cách khiếu nại; khi hết đình chỉ hoặc được gỡ cấm cũng gửi email báo. Đổi vai trò không gửi email, nhưng lần đăng nhập kế tiếp hiện thông báo trong ứng dụng nêu vai trò mới và quyền đã thay đổi (đặc biệt khi bị hạ quyền). Ghi vào Integration Points với spec 100 (thao tác đình chỉ/cấm và khiếu nại).
B. Bổ sung:
- "Administrator đang hoạt động" chỉ tính tài khoản không bị đình chỉ, cấm hoặc xóa; dùng chung quy tắc này với spec 011 (Administrator đang hoạt động cuối cùng không được tự xóa tài khoản).
- Moderator không được đình chỉ/cấm Moderator khác hoặc Administrator; chỉ vai trò cao hơn được thao tác lên vai trò thấp hơn, riêng Administrator được thao tác lên Administrator khác.
- Thao tác lên một Administrator khác yêu cầu xác thực lại và luôn được ghi audit.
Ghi vào Integration Points với spec 011 và spec 100.
```

Spec ghi 9 mục trong Clarifications: 4 chỉ thị của mình (đánh dấu "Maintainer instruction", không phải câu hỏi của AI) và 5 câu hỏi của AI:

| # | Câu hỏi của AI | AI đề xuất | Mình chọn |
|---|---|---|---|
| 1 | *(Chỉ thị của mình)* Ranh giới phạm vi với spec 011? | Không áp dụng (chỉ thị trong prompt, không phải câu hỏi của AI) | Sửa tên hiển thị, đổi/đặt mật khẩu, xóa tài khoản và dietary profile thuộc spec 011; Out of scope cập nhật |
| 2 | *(Chỉ thị của mình)* Ai được xem dữ liệu cá nhân (NFR-009)? | Không áp dụng (chỉ thị trong prompt) | Email chỉ chủ tài khoản và Administrator thấy; tên hiển thị và avatar là công khai (theo spec 011) |
| 3 | *(Chỉ thị của mình)* Spec 011 có cần trạng thái tài khoản "đã xóa" không? | Không áp dụng (chỉ thị trong prompt) | Không: spec 011 xóa hẳn tài khoản nên không thêm trạng thái "deleted" vào User Account; thay bằng thực thể Blocked Email (mã băm email) trong Key Entities |
| 4 | *(Chỉ thị của mình)* Mã Jira ở dòng Feature Branch? | Không áp dụng (chỉ thị trong prompt) | SCRUM-27 là task Sprint Planning, không phải feature này; dòng branch đổi thành `feature/SCRUM-xx-…` chờ mã Jira mới, Assumption sửa theo |
| 5 | Sau khi tài khoản bị xóa (spec 011), email đó có đăng ký lại ngay được không? | Phương án A: được đăng ký lại ngay như tài khoản độc lập; audit cũ giữ lại nhưng ẩn danh. Lý do: spec 011 xóa hẳn tài khoản và dữ liệu cá nhân nên giải phóng email ngay (B: chờ một thời gian; C: không bao giờ được đăng ký lại) | A, kèm ngoại lệ chống né lệnh cấm: được ngay như tài khoản mới (FR-002, FR-026); tài khoản bị cấm vẫn tự xóa được nhưng mã băm email vào danh sách chặn và đăng ký lại bị từ chối; tài khoản bị đình chỉ có thời hạn không tự xóa cho tới khi hết đình chỉ; audit giữ đủ 90 ngày (FR-025) ở dạng ẩn danh |
| 6 | Đăng nhập Google bằng email trùng tài khoản email/mật khẩu chưa xác minh thì xử lý thế nào? | Phương án A: liên kết với Google, đánh dấu đã xác minh, vô hiệu hóa mật khẩu cũ và đăng xuất các phiên cũ. Lý do: Google đã xác minh email, cách này chặn kiểu chiếm quyền bằng mật khẩu do người chưa xác minh đặt (B: giữ mật khẩu cũ, rủi ro chiếm quyền; C: từ chối đăng nhập Google) | A, bổ sung: coi như tài khoản mới – xóa mọi dữ liệu tạo trước khi xác minh (tên hiển thị, hồ sơ ăn uống 011, bản nháp 060, bộ sưu tập 080), vô hiệu mật khẩu cũ, kết thúc các phiên cũ, tên hiển thị lấy từ Google, đánh dấu đã xác minh (FR-019). Lý do: chủ email thật không thừa hưởng hồ sơ dị ứng hay nội dung do người khác đặt sẵn |
| 7 | Tài khoản không bao giờ xác minh email được giữ bao lâu? | Phương án B: 7 ngày. Lý do: đủ dài để người dùng quay lại xác minh, đủ ngắn để giải phóng email (A: 24 giờ; C: 30 ngày; D: không tự xóa) | B – 7 ngày tính từ lúc đăng ký, link xác minh mới không gia hạn; sau đó xóa hẳn tài khoản và dữ liệu, email đăng ký lại ngay, không vào danh sách chặn (FR-005) |
| 8 | Người dùng có được thông báo khi bị đổi vai trò, đình chỉ hoặc cấm không? | Phương án B: gửi email khi đình chỉ/cấm; đổi vai trò chỉ ghi audit và hiện thông báo trong ứng dụng. Lý do: FR-013 đã hiển thị lý do khi đăng nhập nên email cho hai trường hợp khóa là đủ, không gây nhiễu (A: không gửi email nào; C: gửi cho cả đổi vai trò) | B, bổ sung: đình chỉ/cấm gửi email nêu lý do, thời hạn (với đình chỉ) và cách khiếu nại, và gửi email khi hết đình chỉ hoặc gỡ cấm; đổi vai trò không gửi email nhưng lần đăng nhập kế tiếp có thông báo trong ứng dụng nêu vai trò mới và quyền thay đổi. Khiếu nại thuộc spec 100 (FR-024a) |
| 9 | Administrator có thể bị Administrator khác đình chỉ, cấm hoặc hạ quyền không? | Phương án B: Administrator được thao tác lên Administrator khác nhưng không tự thao tác lên mình, và luôn còn ít nhất một Administrator đang hoạt động. Lý do: đơn giản và đảm bảo nền tảng luôn có người quản trị (A: không ai được đình chỉ/cấm Administrator; C: không giới hạn thêm) | B, bổ sung: có, nhưng không bao giờ tự làm lên chính mình và luôn còn ít nhất một Administrator hoạt động (không bị đình chỉ, cấm, xóa; cùng quy tắc với 011); Moderator không tác động lên Moderator/Administrator, chỉ vai trò cao hơn tác động lên vai trò thấp hơn; thao tác lên Administrator khác cần xác thực lại và luôn ghi audit (FR-022a, FR-023) |

### Tóm tắt phản hồi của AI

- `git diff` của `specs/010-user-authentication-roles/spec.md` so với bản đã commit: +112 / −22 dòng; `checklists/requirements.md` đổi một dòng.
- Thêm mục **Clarifications / Session 2026-10-18** với 9 mục (4 chỉ thị + 5 câu hỏi – trả lời).
- Các thay đổi trong spec: dòng Feature Branch `SCRUM-27` → `SCRUM-xx` (chờ mã Jira); Out of scope chuyển hồ sơ, đổi/đặt mật khẩu, xóa tài khoản sang 011; NFR-009 hai mức hiển thị; thêm thực thể Blocked Email và bổ sung Audit Entry (ẩn danh khi tài khoản bị xóa); User Story 5 (Google) tách trường hợp tài khoản **đã xác minh** (liên kết) và **chưa xác minh** (đặt lại như tài khoản mới); FR-005 (xóa tài khoản chưa xác minh sau 7 ngày), FR-019, FR-022a (mới), FR-023, FR-024a (mới), FR-025, FR-026 (quy tắc xóa tài khoản).
- Spec hiện tại: 5 user story, 28 FR (FR-001 → FR-026 cùng FR-022a, FR-024a), 9 NFR, 15 trường hợp biên, 7 SC, 4 điểm trong Integration Points: luồng xóa tài khoản với 011; đặt lại tài khoản chưa xác minh với 011, 060, 080; quy tắc Administrator đang hoạt động với 011, 100; cấm/đình chỉ và khiếu nại với 100.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Cắt hồ sơ, đổi/đặt mật khẩu, xóa tài khoản sang spec 011 | Dùng | Theo file phân công, tuần này 010 chỉ clarify, gắn mã Jira và cắt phần hồ sơ sang 011. Mục 2 cho thấy Out of scope của 010 chưa nhắc đổi mật khẩu và xóa tài khoản. |
| Dòng Feature Branch `SCRUM-27` | Chỉnh sửa → `SCRUM-xx` chờ mã Jira | `SCRUM-27` là task Sprint Planning của Hưng trong `project-plan.md` và `jira_tasks.md`; gán trùng làm sai truy vết theo constitution nguyên tắc I. |
| Không thêm trạng thái "deleted" cho User Account, thêm Blocked Email (mã băm) | Dùng | Spec 011 xóa hẳn tài khoản nên không cần trạng thái "đã xóa". Chỉ cần giữ mã băm email để chặn đăng ký lại, không giữ email gốc. |
| Tài khoản bị cấm vẫn tự xóa được nhưng giữ mã băm email trong danh sách chặn | Dùng (ngoại lệ do mình thêm) | Chống né lệnh cấm bằng cách xóa tài khoản rồi đăng ký lại cùng email. Chỉ giữ mã băm một chiều để không lưu dữ liệu cá nhân sau khi xóa. |
| Google sign-in với email trùng tài khoản chưa xác minh: coi như tài khoản mới | Dùng, có bổ sung của mình | Tài khoản chưa xác minh vẫn tạo được dữ liệu (hồ sơ ăn uống 011, bản nháp 060, bộ sưu tập 080). Xóa dữ liệu đó để chủ email thật không nhận hồ sơ dị ứng hay nội dung do người khác đặt sẵn. |
| Tự xóa tài khoản chưa xác minh sau 7 ngày | Dùng (phương án B) | Tránh tài khoản chưa xác minh chiếm email của người khác quá lâu. Tính từ lúc đăng ký và không gia hạn khi xin link mới; không đưa email vào danh sách chặn để chủ thật đăng ký lại được ngay. |
| Thông báo cho người dùng bị đình chỉ/cấm/đổi vai trò | Dùng (phương án B), có bổ sung của mình | Người bị đình chỉ/cấm cần biết lý do, thời hạn và cách khiếu nại (khiếu nại thuộc spec 100). Người bị hạ quyền cần được báo trong ứng dụng để không bất ngờ mất chức năng. |
| Quy tắc tác động lên Administrator khác | Dùng (phương án B), có bổ sung của mình | Luôn còn ít nhất một Administrator đang hoạt động (dùng chung quy tắc với 011); Moderator không được tác động lên vai trò ngang hoặc cao hơn; thao tác lên Administrator khác là thao tác nhạy cảm nên cần xác thực lại và ghi audit. |

### Cách kiểm chứng

- Đối chiếu bằng `git diff` và `git status`: spec 010 và checklist ở trạng thái đã sửa nhưng chưa commit; mục Clarifications có 9 mục khớp bảng trên.
- Đối chiếu chéo với spec 011: Clarifications 2026-10-18 của 011 (câu 6) và các dòng Integration Points (Last Administrator, Banned and suspended accounts on deletion) dẫn về FR-023, FR-026 của 010 cùng nội dung.
- Lưu ý còn tồn tại: `checklists/requirements.md` của 010 có mục "All functional requirements have clear acceptance criteria" đang bỏ đánh dấu (`[ ]`) trong thay đổi chưa commit; cần xem lại các FR mới (FR-022a, FR-024a, FR-026) đã có kịch bản chấp nhận chưa trước khi đánh dấu lại.

---

## Mục 8: Tạo Project Plan tổng thể theo mô hình Scrum

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-14 |
| Giờ truy cập | 20:27 |
| Người thực hiện | Mai Văn Hiển |
| Spec / Tài liệu liên quan | `docs/management/project-plan.md` (Jira `SCRUM-29`), nguồn: `jira_tasks.md`, `PA2_task.md`, `project-proposal.md` |
| Công cụ AI | Claude Code trong Google Antigravity, model Sonnet 5.5 |
| Mục đích | Soạn Project Plan tổng thể 5 Sprint (PA1 → PA5), phân rã chi tiết Sprint 2 (Mục 4.1–4.3) và kế hoạch sơ bộ Sprint 3–5 |
| Nhánh Git | `Hien` |

### Prompt đã dùng

```
# VAI TRÒ
Bạn là Scrum Master / Project Manager kiêm technical writer cho dự án phần mềm sinh viên "WikiCook" (AI Smart Chef Assistant) trong môn Nhập môn Công nghệ phần mềm. Hãy viết bằng tiếng Việt, văn phong học thuật, súc tích, định dạng Markdown.

# NHIỆM VỤ
Tạo file `WikiCook/docs/management/project-plan.md` là Project Plan tổng thể áp dụng mô hình Scrum qua 5 Sprints, đồng thời phân rã chi tiết Sprint 2 tại các Mục 4.1, 4.2, 4.3.

# BƯỚC 0 – ĐỌC NGUỒN DỮ LIỆU (BẮT BUỘC, LÀM TRƯỚC KHI VIẾT)
1. Đọc toàn bộ `WikiCook/jira_tasks.md`: đây là nguồn sự thật (source of truth) về các task trên Jira Board.
2. Đọc toàn bộ `WikiCook/PA2_task.md`: đây là yêu cầu/đề bài của PA2 (Vision Document, Project Plan, Spec Kit Init).
3. Đọc nhanh `docs/requirements/project-proposal.md` (nếu có) để lấy bối cảnh, phạm vi và tên thành viên nhóm.
4. KHÔNG tự bịa task, mã SCRUM, người phụ trách, story point hay ngày tháng. Thông tin nào không có trong file nguồn thì ghi `TBD` và liệt kê ở cuối câu trả lời.

# MỤC TIÊU ĐẦU RA
## 1. Lộ trình 5 Sprints (mỗi Sprint 2–3 tuần, tương ứng PA1 → PA5)
| Sprint | Thời gian | Mục tiêu / Sản phẩm chính | PA |
|---|---|---|---|
| Sprint 1 | 28/09 – 09/10 | Project Proposal, Existing App Survey & Git Setup | PA1 |
| Sprint 2 | 14/10 – 22/10 | Vision Document, Project Plan & Spec Kit Init | PA2 |
| Sprint 3 | (đề xuất) | Use Case Specifications, Domain Analysis & UI Wireframes | PA3 |
| Sprint 4 | (đề xuất) | Architecture Design, Database Schema & Core Feature Coding | PA4 |
| Sprint 5 | (đề xuất) | Testing, Integration, Feature Polish & Final Demo | PA5 |

- Với Sprint 3–5: tự đề xuất ngày bắt đầu/kết thúc hợp lý (liền sau Sprint trước, mỗi Sprint 2–3 tuần). Nếu `PA2_task.md` hoặc các file khác có deadline PA3–PA5 thì ưu tiên dùng deadline đó.
- Thêm bảng/danh sách Milestones: các mốc quan trọng (nộp PA1–PA5, Sprint Review/Demo, Release cuối...). Mỗi milestone có ngày và tiêu chí hoàn thành.

## 2. Mục 4.1, 4.2, 4.3 – Chi tiết Sprint 2
- **4.1 Sprint Goal & phạm vi Sprint 2**: mục tiêu, sản phẩm bàn giao (deliverables), Definition of Done, thời gian, các Scrum event của Sprint (Planning, Daily Scrum, Review, Retrospective).
- **4.2 Bảng phân rã 18 tasks (SCRUM-28 → SCRUM-45)**: bảng với cột: `Mã task | Tên task | Mô tả ngắn | Người phụ trách | Ưu tiên | Ước lượng (SP/giờ) | Hạn | Trạng thái | Phụ thuộc`. Bảng phải khớp 100% với `jira_tasks.md`: đủ 18 dòng, đúng mã, đúng tên, không thừa/thiếu. Nhóm các task theo hạng mục (Vision Document / Project Plan / Spec Kit Init...) nếu hợp lý nhưng vẫn giữ nguyên mã và tên gốc.
- **4.3 Rủi ro, phụ thuộc & lịch làm việc Sprint 2**: thứ tự thực hiện/phụ thuộc giữa các task, rủi ro chính và cách giảm thiểu, và (nếu phù hợp) timeline theo ngày hoặc sơ đồ Gantt bằng Mermaid.

## 3. Kế hoạch sơ bộ Sprint 3–5
Với mỗi Sprint, liệt kê:
- Danh mục User Stories dự kiến (dạng "Là một <vai trò>, tôi muốn <việc> để <lợi ích>"), bám vào các tính năng của WikiCook (đặc biệt AI Smart Chef Assistant).
- Danh sách task kỹ thuật dự kiến (nhóm theo: phân tích, thiết kế, lập trình, kiểm thử, tài liệu).
- Deliverables và tiêu chí hoàn thành của Sprint.
Ghi rõ đây là kế hoạch sơ bộ, sẽ tinh chỉnh ở Sprint Planning của từng Sprint.

# CẤU TRÚC FILE project-plan.md
1. Giới thiệu (mục đích, phạm vi, thành viên, tài liệu liên quan)
2. Mô hình Scrum áp dụng (vai trò, events, artifacts, Definition of Done, công cụ: Jira, GitHub)
3. Lộ trình tổng thể 5 Sprints và Milestones
4. Chi tiết Sprint 2
   - 4.1 Sprint Goal & phạm vi
   - 4.2 Bảng phân rã 18 tasks (SCRUM-28 → SCRUM-45)
   - 4.3 Rủi ro, phụ thuộc & lịch làm việc
5. Kế hoạch sơ bộ Sprint 3–5 (user stories + task kỹ thuật)
6. Quản lý rủi ro và thay đổi chung
7. Phụ lục (bảng đối chiếu task Sprint 2 với Jira, lịch sử thay đổi tài liệu)

# ACCEPTANCE CRITERIA (TỰ KIỂM TRA TRƯỚC KHI KẾT THÚC)
- [ ] Lộ trình 5 Sprints rõ ràng, đủ ngày tháng, đủ Milestones.
- [ ] Bảng Sprint 2 có đúng 18 task, từ SCRUM-28 đến SCRUM-45, khớp 100% với `jira_tasks.md` (mã, tên, người phụ trách, ước lượng).
- [ ] Mục 4.1, 4.2, 4.3 đúng đánh số như đề bài.
- [ ] Sprint 3–5 có danh mục user stories và task kỹ thuật.
- [ ] Mọi nội dung có yêu cầu trong `PA2_task.md` đều được phản ánh.
- [ ] Markdown hiển thị đúng (bảng, tiêu đề, Mermaid nếu dùng).

# QUY TRÌNH THỰC HIỆN
1. Đọc các file nguồn → tóm tắt ngắn gọn (3–5 dòng) những gì đã đọc và liệt kê 18 task đã tìm thấy.
2. Nếu thấy mâu thuẫn hoặc thiếu dữ liệu (ví dụ ít/nhiều hơn 18 task), DỪNG và hỏi tôi, không tự suy đoán.
3. Viết file `WikiCook/docs/management/project-plan.md` (tạo thư mục nếu chưa có).
4. Chạy lại checklist Acceptance Criteria, đối chiếu từng task SCRUM-28..45 với `jira_tasks.md`.
5. Báo cáo ngắn: file đã tạo, các mục `TBD`, và những giả định đã đặt ra.

# RÀNG BUỘC
- Không sửa `jira_tasks.md` và `PA2_task.md`.
- Không thêm task ngoài 18 task Sprint 2 vào bảng ở Mục 4.2.
- Giữ nguyên thuật ngữ Scrum bằng tiếng Anh (Sprint, Backlog, Daily Scrum, Definition of Done...).
```

### Tóm tắt phản hồi của AI

Số liệu dưới đây đếm trên `project-plan.md` hiện tại, file này đã được chỉnh sửa tiếp sau lần tạo đầu (xem `git log` của file), nên có thể khác bản AI sinh ra ban đầu.

- File hiện có các phần: Document Metadata & Responsibility Matrix (kèm Revision History), 1 Introduction, 2 Project Overview, 3 Project Organization & Risk Management (RACI, Risk Matrix), 4 Project Plan (4.1 lộ trình 5 Sprint, 4.2 phân rã Sprint 2, 4.3 lịch Sprint 2 và kế hoạch sơ bộ Sprint 3–5, 4.4 Build and Release Plan), 5 Monitoring/Control (gồm Definition of Done), Appendix A (đối chiếu task với Jira) và Appendix B (Master Revision History).
- Cấu trúc thực tế khác cấu trúc 7 mục trong prompt (ví dụ Kế hoạch sơ bộ Sprint 3–5 nằm trong 4.3, thêm 4.4); bảng 4.2 trong file hiện tại ghi dải `SCRUM-27` → `SCRUM-45` thay vì `SCRUM-28` → `SCRUM-45`.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Lộ trình 5 Sprint, Milestones, chi tiết Sprint 2 | Dùng (các phần 4.1, 4.2 và lộ trình 5 Sprint đều có trong file hiện tại) | Prompt yêu cầu lộ trình 5 Sprint khớp PA1–PA5 có Milestones kèm ngày và tiêu chí hoàn thành, và Sprint 2 chi tiết ở Mục 4.1–4.3 bám `jira_tasks.md` (nguồn sự thật về task) |
| Kế hoạch sơ bộ Sprint 3–5 (user story, task kỹ thuật) | Dùng (kế hoạch sơ bộ Sprint 3–5 nằm trong Mục 4.3 của file hiện tại) | Prompt yêu cầu mỗi Sprint 3–5 có user story, task kỹ thuật và deliverables; ghi rõ đây là kế hoạch sơ bộ, sẽ tinh chỉnh ở Sprint Planning của từng Sprint |
| Cấu trúc file khác với 7 mục trong prompt, dải task `SCRUM-27` → `SCRUM-45` | Chỉnh sửa so với prompt | Project Plan có thêm Mục 4.4 (Build and Release Plan, `SCRUM-45`) và bảng Sprint 2 bám theo Jira, trong đó `SCRUM-27` là task Sprint Planning, nên cấu trúc và dải task khác với prompt |

### Cách kiểm chứng

- Đối chiếu từng task SCRUM với `jira_tasks.md` và Appendix A (Cross-Check Audit against Live Jira Board) trong file.
---

## Mục 9: Viết Mục 7 (Non-Functional Requirements) của Vision Document

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-19 (ngày commit `a90486a`) |
| Người thực hiện | Mai Văn Hiển |
| Spec / Tài liệu liên quan | `docs/requirements/vision-document.md` – Mục 7 (Jira `SCRUM-36`) |
| Công cụ AI | Claude Code trong Google Antigravity, model Sonnet 5.5 |
| Mục đích | Viết các yêu cầu phi chức năng có ngưỡng số, đối chiếu mô tả Jira với số liệu của spec và constitution trước khi viết |
| Nhánh Git | `Hien` |

### Prompt đã dùng

```
Task Jira SCRUM-36: viết Mục 7 (Non-Functional Requirements) của docs/requirements/vision-document.md. Mục 7 đã có sẵn tiêu đề và dòng metadata do Duy chừa; chỉ viết nội dung trong Mục 7, giữ nguyên dòng metadata, KHÔNG sửa mục khác (trừ thêm một dòng vào Revision History).

Bước 1 – Thu thập, chưa viết: đọc .specify/memory/constitution.md, docs/requirements/project-proposal.md, Mục 1–6 của vision-document.md, và mục Non-Functional Requirements, Success Criteria, Assumptions, Clarifications của mọi spec trong specs/ (010, 011, 020, 030, 031, 040, 050, 060, 080 và các spec khác nếu có). Lập danh sách mọi con số đã chốt (thời gian phản hồi, giới hạn, thời hạn, dung lượng, tỷ lệ...) kèm nguồn (file + số FR/NFR/SC).

Bước 2 – Đối chiếu với mô tả Jira sau và báo cáo cho mình TRƯỚC KHI VIẾT, dạng bảng
"Chỉ số / Jira nói / Spec hoặc constitution nói / Đề xuất":
- Performance: page load < 2.0 s trên 4G; P95 API < 500 ms; ≥ 100 người dùng đồng thời, tỷ lệ lỗi < 0.1%. - Security: BCrypt cost 12; JWT hết hạn 24 h (30 ngày với Remember me); khóa 15 phút sau 5 lần đăng nhập sai; tối đa 3 email xác thực / 15 phút.
- Platform & Accessibility: Chrome 110+, Safari 16+, Firefox 110+; WCAG 2.1 Level AA.
- AI: xử lý RAG prompt < 5 s; dịch vụ sẵn sàng 99.5%. Quy tắc: số liệu trong spec/constitution được ưu tiên hơn Jira. Chỗ nào Jira mâu thuẫn spec (ví dụ RAG < 5 s so với timeout 60 s của 031; JWT 24 h so với bảng refresh_tokens) thì đề xuất cách viết khớp spec. Chỉ số không kiểm soát được (uptime của dịch vụ AI bên ngoài) thì đề xuất chuyển thành yêu cầu về hành vi khi lỗi. Dừng lại chờ mình xác nhận.

Bước 3 – Sau khi mình xác nhận, viết Mục 7 bằng tiếng Anh, văn phong tài liệu kỹ thuật:
- 7.1 Applicable Standards (WCAG 2.1 AA, OWASP Top 10, ISO/IEC 25010 làm khung phân loại chất lượng, các chuẩn khác nếu spec có nhắc).
- Các nhóm theo ISO/IEC 25010: Performance Efficiency, Security, Reliability & Availability, Usability & Accessibility, Compatibility / Platform, Maintainability (constitution: coverage ≥ 70%, kiến trúc phân lớp), AI-specific Constraints, Privacy.
- Mỗi nhóm là một bảng với cột: ID (NFR-PERF-01...), Requirement, Metric & Threshold (có số và đơn vị), Measurement Method (đo bằng gì, trong điều kiện nào), Source (constitution / spec + số FR/NFR/SC / proposal / PA2 brief).
- Không có yêu cầu nào thiếu ngưỡng số; không dùng từ mơ hồ ("fast", "secure", "user-friendly").
- Thêm cuối mục một đoạn ngắn 7.x Verification Approach: các chỉ số được kiểm chứng ở sprint nào (theo Build Plan trong docs/management/project-plan.md).
- Revision History: thêm một dòng cho bản cập nhật Mục 7.
Không bịa số liệu: chỉ số nào không có nguồn mà là đề xuất mới thì ghi Source là "Proposed (SCRUM-36)" để nhóm duyệt. Không thêm tên công nghệ vào Requirement khi không cần (để ở Measurement Method).
```

### Tóm tắt phản hồi của AI

- Commit `a90486a` (2026-10-19) thêm 164 dòng vào `vision-document.md`; Revision History có thêm dòng `v0.10` cho Mục 7 (`SCRUM-36`).
- Mục 7 có 11 tiểu mục: 7.1 Applicable Standards, 7.2 Conventions and Measurement Baseline, 7.3 Performance Efficiency, 7.4 Security, 7.5 Reliability & Availability, 7.6 Usability & Accessibility, 7.7 Compatibility & Platform, 7.8 Maintainability, 7.9 AI-specific Constraints, 7.10 Privacy, 7.11 Verification Approach.
- Tổng cộng 88 mã NFR duy nhất: PERF 16, SEC 16, AI 13, REL 11, PRV 10, USA 8, MNT 8, COM 6. File có 6 chỗ ghi "Proposed (SCRUM-36)" cho chỉ số đề xuất mới.
- 7.11 liệt kê từng NFR được kiểm chứng ở Build nào theo Plan 4.4.2 (ví dụ Build v0.2 ngày 22/10 chỉ review, Build v0.5 ngày 13/11, Build v0.8 Beta ngày 04/12).

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Số liệu của spec và constitution được ưu tiên hơn mô tả Jira (theo quy tắc trong prompt) | Dùng | Prompt đặt quy tắc số liệu trong spec và constitution được ưu tiên hơn mô tả Jira, để Mục 7 không mâu thuẫn với các spec đã làm rõ |
| Các chỉ số Jira mâu thuẫn spec (RAG < 5 s, JWT 24 h) và uptime dịch vụ AI | Dùng theo hướng prompt đề xuất: viết khớp spec; mức 99.5% uptime không xuất hiện trong Mục 7 | Prompt nêu RAG < 5 s mâu thuẫn timeout 60 s của spec 031 và JWT 24 h mâu thuẫn bảng `refresh_tokens`; uptime của dịch vụ AI bên ngoài không kiểm soát được nên được chuyển thành yêu cầu về hành vi khi lỗi |
| Các chỉ số đề xuất mới ghi "Proposed (SCRUM-36)" | Chờ nhóm duyệt | Prompt yêu cầu không bịa số liệu: chỉ số không có nguồn trong repo phải ghi rõ là đề xuất mới để nhóm duyệt |

### Cách kiểm chứng

- Đếm trong file: 88 mã NFR duy nhất ở 8 nhóm; `git show --stat a90486a` chỉ sửa `vision-document.md`.
- Mục 7 được dùng lại ở Mục 10 (đối chiếu Mục 4.4 của Project Plan với Vision §7).

---

## Mục 10: Sửa Mục 4.4 của Project Plan cho khớp spec 031 và Vision §7

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-19 (ngày commit `4866b9f`) |
| Người thực hiện | Mai Văn Hiển |
| Spec / Tài liệu liên quan | `docs/management/project-plan.md` – Mục 4.4 (Jira `SCRUM-45`), đối chiếu spec 031 và Vision §7 |
| Công cụ AI | Claude Code trong Google Antigravity, model Sonnet 5.5 |
| Mục đích | Bỏ "LLM timeout 5 s" và "fallback SQL ≤ 2 s", thay bằng chỉ số theo spec 031; đối chiếu Mục 4.4 với Vision §7 |
| Nhánh Git | `Hien` |

### Prompt đã dùng

```
Task SCRUM-45 (mình là tác giả Mục 4.4 của docs/management/project-plan.md). Sửa Mục 4.4.2 và 4.4.4 cho khớp specs/031-ai-weekly-meal-planner/spec.md và Mục 7 của docs/requirements/vision-document.md:
- Bỏ "LLM timeout 5 s" và "fallback SQL ≤ 2 s" (031 không có fallback SQL; khi AI lỗi chỉ báo lỗi và lịch thủ công vẫn dùng được).
- Thay bằng chỉ số theo 031: nháp đầy đủ P90 ≤ 30 s, cứng 60 s; gợi ý một ô ≤ 3 s cho 90% lượt; truy xuất RAG + dựng prompt (không tính lệnh gọi LLM) P95 ≤ 5 s; AI lỗi thì 100% thao tác lịch thủ công vẫn chạy.
- Đối chiếu toàn bộ Mục 4.4 với Vision §7 (số người dùng đồng thời 100, tỷ lệ lỗi < 1%, coverage line và branch ≥ 70%), báo những chỗ còn lệch và sửa cho khớp.
- Thêm một dòng Revision History nếu file có.
Chỉ sửa Mục 4.4 của project-plan.md. Liệt kê dòng cũ và dòng mới để mình kiểm tra.
```

### Tóm tắt phản hồi của AI

- Commit `4866b9f` (2026-10-19) chỉ sửa `docs/management/project-plan.md`: 10 dòng thêm, 8 dòng xóa.
- Các thay đổi nhìn thấy trong `git show`:
  - Bảng tích hợp Backend ↔ LLM / Embedding API: bỏ "5 s timeout; deterministic SQL fallback", thay bằng "RAG retrieval and prompt assembly", giới hạn cứng 60 s cho việc sinh thực đơn, AI lỗi thì báo lỗi còn lịch thủ công vẫn dùng được.
  - Dòng Backend ↔ Redis: ghi giới hạn 5 lượt tạo được tính cho mỗi người dùng trong mọi cửa sổ 60 phút (spec 031 FR-025), bỏ "meal-plan cache" và "cache warm-up script".
  - Sơ đồ Build v0.8 Beta: "RAG + fallback / Redis cache" đổi thành "RAG planner / AI-error isolation".
  - Gate D3 của F-03: bỏ "fallback plan still loads", thay bằng hiển thị thông báo lỗi AI và lịch thủ công vẫn dùng được; kịch bản demo khi LLM lỗi đổi từ cache Redis + planner SQL sang chạy bằng LLM thật (`LLM_MODE=live`), trình bày thông báo lỗi và lịch thủ công.
  - Mục tiêu coverage: ghi rõ cả line và branch ≥ 70% (cho v0.8 Beta chỉ tính module đã cài, cho v1.0 RC toàn bộ logic backend).
  - Thêm dòng `v1.1` (19/10/2026) vào Revision History: tác giả Mai Văn Hiển, reviewer Lê Quốc Hưng.


### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Bỏ LLM timeout 5 s và fallback SQL | Dùng | Spec 031 không có fallback SQL; khi AI lỗi chỉ báo lỗi và lịch thủ công vẫn dùng được, nên hai ý này phải bỏ khỏi Mục 4.4 cho khớp |
| Chỉ số mới theo 031 (P90 ≤ 30 s, giới hạn cứng 60 s, gợi ý một ô ≤ 3 s, RAG P95 ≤ 5 s) | Dùng một phần trong Mục 4.4: giới hạn cứng 60 s đã có; P90 ≤ 30 s nằm ở Vision NFR-AI-01 (Mục 7) | Để Mục 4.4 dùng cùng chỉ số với spec 031 và Vision §7 thay vì số cũ của Project Plan |
| Kịch bản demo dùng LLM thật thay cho cache Redis + planner SQL | Dùng | Vì 031 không có fallback SQL nên không còn dùng cache Redis và planner SQL; demo chạy bằng LLM thật và khi lỗi trình bày thông báo lỗi cùng lịch thủ công |
| Dòng Revision History `v1.1` | Dùng | Prompt yêu cầu thêm dòng Revision History nếu file có; file có bảng nên thêm `v1.1` ghi phạm vi sửa Mục 4.4 |

### Cách kiểm chứng

- `git show 4866b9f` chỉ chạm `project-plan.md`; dòng `v1.1` ghi đúng phạm vi sửa Mục 4.4.
- Ghi chú: trong phần diff đã xem, các con số P90 ≤ 30 s, RAG P95 ≤ 5 s, gợi ý một ô ≤ 3 s chưa thấy ở các dòng của Mục 4.4; cần xem lại 4.4.2 và 4.4.4 xem đã nêu đủ các chỉ số mà prompt yêu cầu chưa.