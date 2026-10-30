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
| 11 | 2026-10-30 | Task SCRUM-50 – Bước 1: đọc tài liệu nguồn và tạo nhánh `docs/scrum-50-uc-spec-010-011-080` | `use-case-specification.md`, 010, 011, 080 |
| 12 | 2026-10-30 | Task SCRUM-50 – Bước 2: viết Use-Case Specification cho spec 010 (`UC-010-01` – `UC-010-04`) | `use-case-specification.md` §9, 010 |
| 13 | 2026-10-30 | Task SCRUM-50 – Bước 2: viết Use-Case Specification cho spec 011 (`UC-011-01` – `UC-011-05`) | `use-case-specification.md` §10, 011 |
| 14 | 2026-10-30 | Task SCRUM-50 – Bước 2: viết Use-Case Specification cho spec 080 (`UC-080-01` – `UC-080-04`) | `use-case-specification.md` §11, 080 |
| 15 | 2026-10-30 | Task SCRUM-50 – Bước 4: map màn hình prototype `AU`, `PF`, `BM` vào use case và lập danh sách Asset Gaps cho SCRUM-58 | `use-case-specification.md` §9.2, §10.2, §11.2 |
| 16 | 2026-10-30 | Task SCRUM-50 – Bước 5: kiểm tra trước khi báo xong | `use-case-specification.md` §9–§11 |
| 17 | 2026-10-30 | Task SCRUM-50 – đối chiếu lại use case với spec gốc và prototype, sửa 3 chỗ lệch | `use-case-specification.md` §9–§11 |
| 18 | 2026-10-30 | Task SCRUM-50 – ghi quyết định cho 23 Open Questions, đổi thứ tự bước đăng nhập, thêm Gap 080-O | `use-case-specification.md` §9–§11 |
| 19 | 2026-10-30 | Task SCRUM-50 – chọn cách A cho `OQ-010-02`, viết lại `AF 2.2` và `AF 2.3`; giữ `AF 4.1` và `OQ-011-05` ở trạng thái mở | `use-case-specification.md` §9, §10 |
| 20 | 2026-10-30 | Task SCRUM-50 – sửa lỗi của AI: `AU01` có ở dạng `W-AU01-Login.dc.html`, mọi màn có bản web `W-` | `use-case-specification.md` §9.2 |
| 21 | 2026-10-30 | Task SCRUM-50 – AI tự kiểm tra prototype và alternative flow theo yêu cầu của Hiển | `use-case-specification.md` §9–§11 |
| 22 | 2026-10-30 | Task SCRUM-50 – sửa lỗi của AI: Decision đặt sai chỗ, `AF 2.3` và `UC-011-03` chưa nêu là "proposed" | `use-case-specification.md` §9–§11 |

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

---

## Mục 11: Task SCRUM-50 – Bước 1: đọc tài liệu nguồn và tạo nhánh cho Use-Case Specification 010, 011, 080

**Task: SCRUM-50**

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-30 |
| Giờ truy cập | 07:50 |
| Người thực hiện | Mai Văn Hiển |
| Spec / Tài liệu liên quan | `docs/requirements/use-case-specification.md`, `docs/requirements/use-case-model.md`, `specs/010-user-authentication-roles/spec.md`, `specs/011-user-profile-dietary/spec.md`, `specs/080-bookmark-collection/spec.md`, `.specify/memory/constitution.md`, `docs/ai_log/ai_log_hien.md`, các màn `AU*`, `PF*`, `BM*` trong `docs/analysis-and-design/ui-ux/screens/` |
| Công cụ AI | Claude Code (tiện ích mở rộng VS Code, Windows 11), model Sonnet 5.5 (`claude-sonnet-5-5`); phiên bản Claude Code: 2.1.312 |
| Mục đích | Đọc mẫu UC-040 và các tài liệu nguồn, kiểm tra nhánh mới không kéo theo commit ai_log hay file không liên quan, tạo nhánh `docs/scrum-50-uc-spec-010-011-080` |
| Nhánh Git | `docs/scrum-50-uc-spec-010-011-080` (file log này để chưa add/commit trên nhánh này, theo yêu cầu của prompt) |

### Dòng tóm tắt theo mẫu AI Usage Notes

**Claude Code.** *Sonnet 5.5 (claude-sonnet-5-5), Anthropic, Claude Code trong VS Code trên Windows 11*, truy cập 07:50 ngày 30/10/2026, prompt: "Bối cảnh: tôi là Mai Văn Hiển (@MaiHien3507), đang làm SCRUM-50 … Bước 1 – Nhánh …" (nguyên văn bên dưới), dùng để đọc tài liệu nguồn và tạo nhánh cho SCRUM-50; AI đọc tài liệu, chạy `git log`, `git diff origin/main --stat`, tạo nhánh; sinh viên: yêu cầu đối chiếu lại use case với spec gốc và prototype (kết quả ở Mục 17), tự chốt quyết định về `US3` của 010 và `US4`/`US5` của 080, chạy lại các lệnh kiểm tra; đọc duyệt từng use case: Hiển yêu cầu AI tự đối chiếu với spec gốc và prototype (Mục 17, Mục 21); Open Questions: Hiển đã quyết định (Mục 18, Mục 19); gửi Asset Gaps cho Chung: Hiển báo đã gửi cho Chung lúc 09:29 ngày 30/10/2026.

### Prompt đã dùng

Prompt của cả SCRUM-50 (các mục sau ghi "như trên" kèm số bước):

```
Bối cảnh: tôi là Mai Văn Hiển (@MaiHien3507), đang làm SCRUM-50 của dự án WikiCook (Group 05, môn CSC13002). Task: viết Use-Case Specification cho spec 010, 011, 080. Assignee: Hiển. Reviewer: Duy.

Đọc trước khi làm gì khác:
- docs/requirements/use-case-specification.md (đặc biệt UC-040: mẫu format 8 mục, cách đặt metadata, bảng truy vết, cách trình bày ảnh prototype)
- docs/requirements/use-case-model.md
- specs/010-user-authentication-roles/spec.md
- specs/011-user-profile-dietary/spec.md
- specs/080-bookmark-collection/spec.md
- docs/analysis-and-design/ui-ux/screens/ (các màn AU, PF, BM)
- constitution trong repo (spec của tôi không được trái constitution)
- docs/ai_log/ai_log_hien.md (để biết format log, xem Bước 6)

## Bước 1. Nhánh
- Tạo nhánh mới `docs/scrum-50-uc-spec-010-011-080`. KHÔNG làm trực tiếp trên nhánh Hien (nhánh đó chứa log AI của tôi).
- Trước khi tạo, chạy `git log` và `git diff origin/main --stat` để kiểm tra: nhánh mới phải chứa use-case-specification.md và use-case-model.md hiện có, nhưng PR vào main không được kéo theo commit ai_log hay file không liên quan. Nếu không tránh được, DỪNG và báo tôi, đừng tự quyết.
- Hai file chưa theo dõi `AI Usage Guidelines.pdf` và `wikicook-pa3-team-assignment.md`: tuyệt đối không `git add`. Không dùng `git add .` hoặc `-A`; add từng file theo tên.

## Bước 2. Viết 13 use case (tiếng Anh) ở cuối use-case-specification.md
Thêm 3 section mới, mỗi use case đủ 8 mục theo đúng format UC-040: tên + ID, actor, description, preconditions, basic flow từng bước, TẤT CẢ alternative flow, postconditions, special requirements.

- Section 9 (spec 010): UC-010-01 đến UC-010-04
- Section 10 (spec 011): UC-011-01 đến UC-011-05
- Section 11 (spec 080): UC-080-01 đến UC-080-04

Yêu cầu nội dung:
- Chia use case theo user story / functional requirement trong spec gốc. Nếu spec gốc không chia ra đúng số use case như trên (4/5/4), báo tôi kèm đề xuất cách chia trước khi viết, đừng tự ép cho đủ số.
- Alternative flow phải đầy đủ, lấy từ spec: lỗi validation, dữ liệu rỗng, không có quyền, phiên hết hạn, tài khoản bị khóa/đình chỉ, mất mạng, v.v. nếu spec nhắc tới. Không bịa tính năng hay luồng ngoài spec; nếu spec thiếu hoặc mâu thuẫn, ghi vào danh sách "Spec issues" ở báo cáo cuối.
- Ngay sau header mỗi section (9, 10, 11) có dòng: Performed by: Mai Văn Hiển, Reviewed by: Nguyễn Đức Duy, Edited by: Mai Văn Hiển (đúng cách ghi ở các section hiện có).
- Mỗi section có bảng truy vết: use case → FR/US trong spec gốc → feature trong Vision §5 → màn hình prototype.
- Không sửa các section đã có của người khác.
- Làm lần lượt 010 → 011 → 080, mỗi feature một commit (message dạng `docs(uc-spec): add UC-010 specs for SCRUM-50`).

## Bước 3. Revision History đầu file
- Thêm dòng v1.2 (bản đầu cho SCRUM-50), theo format các dòng hiện có.
- Ngày ghi là ngày làm thật (lấy bằng lệnh `date`; tôi sẽ chỉnh nếu cần).
- KHÔNG thêm dòng v1.3 bây giờ: v1.3 chỉ thêm sau khi Duy review xong ngày 03/11.

## Bước 4. Map prototype (phối hợp SCRUM-58 với Chung)
Prototype có sẵn: 010 → AU02–AU09 (AU01, màn đăng nhập, không thấy trong thư mục); 011 → PF01–PF08; 080 → BM01–BM06.
- Map từng màn hình vào đúng use case, cho cả basic flow và alternative flow, theo cách UC-040 đặt ảnh/tham chiếu màn hình.
- Chỗ nào use case/alternative flow không có màn hình tương ứng (kể cả AU01) thì ghi vào mục "Asset Gaps" theo format đã có trong file; nếu file chưa có mục này, tạo ở cuối tài liệu. Mỗi dòng gap: use case, flow (basic/alt), màn hình cần có, mô tả ngắn.
- Nếu một use case không thể suy ra UI mạch lạc từ spec, ghi vào Spec issues thay vì tự vẽ UI lệch spec.
- Trong báo cáo cuối, xuất danh sách Asset Gaps dạng bảng markdown ngắn gọn để tôi gửi Chung trước 31/10.

## Bước 5. Kiểm tra trước khi báo xong
- Mermaid (nếu có dùng) render được; đánh số use case liên tục, không trùng ID; tên/link màn hình tồn tại thật trong thư mục screens.
- `git status` không chứa file ngoài use-case-specification.md (và file khác thật sự cần sửa, nêu rõ). File ai_log_hien.md chưa add/commit là bình thường (xem Bước 6).
- Tài liệu hoàn toàn bằng tiếng Anh; câu văn dạng "The system ..." / "The user ...", nhất quán với UC-040.

## Bước 6. Ghi AI-log song song (SCRUM-63)
Làm tới đâu ghi `docs/ai_log/ai_log_hien.md` tới đó, mỗi mốc một entry: sau Bước 1, sau mỗi feature (010, 011, 080), sau Bước 4, và khi kiểm tra xong.
- File này là log cá nhân của tôi, dùng chung cho mọi SCRUM tôi làm trong sprint. Mỗi entry ghi rõ "Task: SCRUM-50" ở đầu entry. Thêm entry mới vào cuối file, không sửa hay xóa entry cũ.
- Đọc các entry đang có trước và theo đúng format, ngôn ngữ và cách đánh số của chúng.
- Mỗi entry đủ trường: tên tool + version + platform; thời gian truy cập (ngày + giờ, lấy bằng lệnh `date`, không đoán); prompt đã dùng (dán nguyên văn prompt này ở entry đầu, các entry sau ghi "như trên" kèm số bước); mục đích; phần AI sinh (liệt kê cụ thể UC, mục, file); phần tôi tự làm / sửa / kiểm chứng.
- Version model và platform: nếu không biết chắc thì ghi "Hiển điền", đừng đoán. Với "phần tôi tự làm / kiểm chứng", chỉ ghi những gì tôi đã nói là đã làm; nếu chưa biết thì ghi "Hiển điền sau khi review", không viết thay tôi.
- File ai_log có thể chỉ tồn tại trên nhánh Hien. Nếu nhánh SCRUM-50 chưa có file này thì tạo đúng đường dẫn trên.
- KHÔNG commit thay đổi của file này vào các commit của nhánh SCRUM-50 (để PR gửi Duy chỉ chứa UC spec). Để nó ở trạng thái chưa add, và nêu rõ trong báo cáo cuối để tôi tự chuyển sang nhánh Hien và commit ở đó.

## Không làm
- Không push, không mở PR, không merge. Dừng sau commit cuối và báo cáo.
- Không đụng vào nhánh Hien, không sửa entry cũ trong ai_log.

## Báo cáo cuối (tiếng Việt, ngắn gọn)
1. Tên nhánh + 3 commit.
2. Bảng tóm tắt 13 use case: ID, tên, số alternative flow, màn hình prototype map vào.
3. Danh sách Asset Gaps cho Chung.
4. Spec issues / chỗ cần tôi quyết định.
5. AI-log: đã ghi bao nhiêu entry, những trường nào còn "Hiển điền", và nhắc tôi chuyển file sang nhánh Hien để commit.
```

### Tóm tắt phản hồi của AI

- Đọc `use-case-specification.md` trên `origin/main` (mẫu UC-040, bảng truy vết Mục 3, bảng map màn hình Mục 4, Mục 6–8 của Duy), `use-case-model.md` (§2 actor, §4.1, §4.8, bảng ID), ba spec 010/011/080, constitution v1.0.0 và các màn `AU02`–`AU09`.
- Tạo nhánh `docs/scrum-50-uc-spec-010-011-080` từ nhánh `Hien` sau khi đã `git pull`. Kiểm tra: `git diff origin/main --stat` rỗng và `git log origin/main..HEAD` rỗng, nghĩa là nhánh trùng `origin/main` (commit `96662ca`), PR sẽ không kéo theo commit ai_log hay file không liên quan.
- `docs/ai_log/ai_log_hien.md` đã nằm trên nhánh (đến từ `origin/main`) nên khi sửa file này nó hiện là file đã sửa chưa add; theo prompt, không add vào commit nào của nhánh.
- Phát hiện khi đọc: file `use-case-specification.md` dùng đuôi dòng CRLF (index Git là LF, `core.autocrlf=true`); `AU01-Login` có trong `canvas.json` và `index.html` nhưng không có trong `screens/`.
- Chưa có use case nào được viết ở mục này.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Tạo nhánh và kết quả kiểm tra `git diff origin/main --stat` | Dùng; Hiển đã chạy lại các lệnh kiểm tra | Nhánh trùng `origin/main` nên PR không kéo theo commit ai_log hay file khác |

### Cách kiểm chứng

- Lệnh có thể chạy lại: `git diff origin/main --stat` (rỗng tại thời điểm tạo nhánh) và `git log --oneline origin/main..docs/scrum-50-uc-spec-010-011-080`.
- Phần Hiển tự làm / kiểm chứng: Hiển yêu cầu đối chiếu lại use case với spec gốc và prototype (Mục 17), tự chốt các quyết định về `US3` của 010 và `US4`/`US5` của 080, và đã chạy lại các lệnh kiểm tra; đọc duyệt từng use case: Hiển yêu cầu AI tự đối chiếu với spec gốc và prototype (Mục 17, Mục 21); Open Questions: Hiển đã quyết định (Mục 18, Mục 19); gửi Asset Gaps cho Chung: Hiển báo đã gửi cho Chung lúc 09:29 ngày 30/10/2026.

---

## Mục 12: Task SCRUM-50 – Bước 2: viết Use-Case Specification cho spec 010 (UC-010-01 đến UC-010-04)

**Task: SCRUM-50**

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-30 |
| Giờ truy cập | 07:55 (lệnh `date` trả về 07:55:43 sau khi commit) |
| Người thực hiện | Mai Văn Hiển |
| Spec / Tài liệu liên quan | `specs/010-user-authentication-roles/spec.md`, `docs/requirements/use-case-model.md` §4.1, `docs/requirements/vision-document.md` §5.1, `.specify/memory/constitution.md`; kết quả ghi vào `docs/requirements/use-case-specification.md` (Section 9) |
| Công cụ AI | Claude Code (tiện ích mở rộng VS Code, Windows 11), model Sonnet 5.5 (`claude-sonnet-5-5`); phiên bản Claude Code: 2.1.312 |
| Mục đích | Viết 4 use case của spec 010, bảng truy vết, bảng map màn hình `AU02`–`AU09` kèm danh sách Asset Gaps, danh sách câu hỏi mở; thêm hàng Section 9 vào bảng trách nhiệm và dòng `v1.2` vào Revision History |
| Nhánh Git | `docs/scrum-50-uc-spec-010-011-080` (commit `fa05da0`, thông điệp `docs(uc-spec): add UC-010 specs for SCRUM-50`) |

### Dòng tóm tắt theo mẫu AI Usage Notes

**Claude Code.** *Sonnet 5.5 (claude-sonnet-5-5), Anthropic, Claude Code trong VS Code trên Windows 11*, truy cập 07:55 ngày 30/10/2026, prompt: như trên (Mục 11, Bước 2, Bước 3 và Bước 4 cho 010), dùng để viết Section 9 của `use-case-specification.md`; AI tạo `UC-010-01` đến `UC-010-04` (tổng 50 alternative flow), bảng truy vết 9.1, bảng map màn hình 9.2 với 14 Asset Gaps (010-A đến 010-N), 10 câu hỏi mở 9.3 (`OQ-010-01` đến `OQ-010-10`), hàng Section 9 trong bảng trách nhiệm và dòng `v1.2`; sinh viên: yêu cầu đối chiếu lại use case với spec gốc và prototype (kết quả ở Mục 17), tự chốt quyết định về `US3` của 010 và `US4`/`US5` của 080, chạy lại các lệnh kiểm tra; đọc duyệt từng use case: Hiển yêu cầu AI tự đối chiếu với spec gốc và prototype (Mục 17, Mục 21); Open Questions: Hiển đã quyết định (Mục 18, Mục 19); gửi Asset Gaps cho Chung: Hiển báo đã gửi cho Chung lúc 09:29 ngày 30/10/2026.

### Prompt đã dùng

Như trên (Mục 11): Bước 2 (Section 9, spec 010), Bước 3 (Revision History `v1.2`) và Bước 4 (map prototype cho `AU02`–`AU09`).

### Tóm tắt phản hồi của AI

- Section 9 của `docs/requirements/use-case-specification.md` (549 dòng thêm, không xóa dòng nào):
  - `UC-010-01` Register Account & Verify Email: 11 bước basic flow, 15 alternative flow (email trùng, mật khẩu không đạt, mật khẩu trùng email, xác nhận không khớp, tên hiển thị không hợp lệ, email không hợp lệ, email trong danh sách chặn, gửi trùng, gửi lại email, vượt giới hạn 3 lần/15 phút, link hết hạn, link đã dùng, xóa sau 7 ngày chưa xác minh, đã đăng nhập, đăng ký bằng Google).
  - `UC-010-02` Sign In with Email & Password: 10 bước, 17 alternative flow (sai mật khẩu, email không tồn tại, khóa 15 phút, đình chỉ, cấm, đầu vào không hợp lệ, tài khoản chưa xác minh, đã đăng nhập, khách bấm hành động cần đăng nhập, đích chuyển hướng ngoài WikiCook, hết phiên không "Remember me", hết phiên khi đang điền form, đăng xuất, bị đình chỉ/cấm khi đang đăng nhập, bị hạ vai trò, không đủ quyền, Google không dùng được).
  - `UC-010-03` Reset Forgotten Password: 12 bước, 9 alternative flow.
  - `UC-010-04` Sign In with Google: 10 bước, 9 alternative flow (gồm khớp tài khoản đã xác minh và khớp tài khoản chưa xác minh).
  - 9.1 bảng truy vết (US, FR, NFR, SC, Vision §5.1); 9.2 bảng map màn hình; 9.3 câu hỏi mở.
- Theo mô hình use case (§4.1) và phân chia 4 use case cho spec 010, `US3` (phân quyền theo vai trò) không có use case riêng; AI viết phần Guest và phần quyền của `US3` thành `AF 2.9`, `AF 2.15`, `AF 2.16` của `UC-010-02`, và ghi quyết định này ở `OQ-010-01` để Hiển chốt.
- Các chỗ spec thiếu hoặc mâu thuẫn không được tự bịa luồng, chỉ ghi vào 9.3: thông báo khóa tài khoản có thể làm lộ email đã đăng ký (`NFR-003`), link xác minh cũ sau khi gửi lại, đường vào trang đóng tài khoản của người bị cấm, thông báo khi email bị chặn, giới hạn gửi lại link đặt mật khẩu, tính nguyên tử của việc reset tài khoản chưa xác minh khi dùng Google, tên hiển thị và thời hạn phiên khi đăng nhập Google, nút "Đổi địa chỉ email" của `AU03` không có trong spec.
- Nội dung viết bằng tiếng Anh, câu dạng "The system …" / "The user …", metadata `Performed by / Reviewed by / Edited by` ngay sau header Section 9 và mỗi use case.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| `UC-010-01` đến `UC-010-04` | Dùng, sau khi được đối chiếu lại với spec 010 và `AU02`–`AU09` (Mục 17); sửa 1 chỗ ở `AF 2.4` và bổ sung 1 ghi chú ở 9.1 | Hiển yêu cầu đối chiếu lại; chỗ lệch là một câu của prototype bị gán nhầm cho `FR-013` và email của `FR-024a` chưa nói rõ thuộc use case nào |
| Cách xử lý `US3` thành alternative flow của `UC-010-02` (`OQ-010-01`) | Dùng (quyết định của Hiển): không tạo `UC-010-05`; phần Guest và phân quyền là `AF 2.9`, `AF 2.15`, `AF 2.16` | `use-case-model.md` đã chốt 4 use case cho spec 010 |
| Các câu hỏi mở `OQ-010-01` đến `OQ-010-10` | Dùng, kèm quyết định của Hiển cho từng câu (ghi ở Mục 18): giữ 2, sửa spec 010 cho 5 câu, hoãn 2, chuyển Chung 1 | Hiển đã đọc spec 010 và chọn; việc sửa spec chờ làm và cần Duy duyệt |

### Cách kiểm chứng

- Đã kiểm tra bằng lệnh trong phiên: 8 tên file màn hình `AU02`–`AU09` được nhắc trong Section 9 đều tồn tại trong `docs/analysis-and-design/ui-ux/screens/`; số alternative flow đếm được 15, 17, 9, 9; `git show --stat HEAD` chỉ có `docs/requirements/use-case-specification.md`.
- Phần Hiển tự làm / kiểm chứng (đối chiếu với `spec.md`, kiểm tra từng alternative flow): Hiển yêu cầu đối chiếu lại use case với spec gốc và prototype (Mục 17), tự chốt các quyết định về `US3` của 010 và `US4`/`US5` của 080, và đã chạy lại các lệnh kiểm tra; đọc duyệt từng use case: Hiển yêu cầu AI tự đối chiếu với spec gốc và prototype (Mục 17, Mục 21); Open Questions: Hiển đã quyết định (Mục 18, Mục 19); gửi Asset Gaps cho Chung: Hiển báo đã gửi cho Chung lúc 09:29 ngày 30/10/2026.

---

## Mục 13: Task SCRUM-50 – Bước 2: viết Use-Case Specification cho spec 011 (UC-011-01 đến UC-011-05)

**Task: SCRUM-50**

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-30 |
| Giờ truy cập | 08:00 (lệnh `date` trả về 08:00:23 sau khi commit) |
| Người thực hiện | Mai Văn Hiển |
| Spec / Tài liệu liên quan | `specs/011-user-profile-dietary/spec.md`, `specs/010-user-authentication-roles/spec.md` (trạng thái tài khoản, danh sách Blocked Email), `docs/requirements/use-case-model.md` §4.1 và §5, `docs/requirements/vision-document.md` §5.1, `.specify/memory/constitution.md`; kết quả ghi vào `docs/requirements/use-case-specification.md` (Section 10) |
| Công cụ AI | Claude Code (tiện ích mở rộng VS Code, Windows 11), model Sonnet 5.5 (`claude-sonnet-5-5`); phiên bản Claude Code: 2.1.312 |
| Mục đích | Viết 5 use case của spec 011, bảng truy vết, bảng map màn hình `PF01`–`PF08` kèm danh sách Asset Gaps, danh sách câu hỏi mở; cập nhật hàng Section 10 trong bảng trách nhiệm và dòng `v1.2` |
| Nhánh Git | `docs/scrum-50-uc-spec-010-011-080` (commit `b69ed16`, thông điệp `docs(uc-spec): add UC-011 specs for SCRUM-50`) |

### Dòng tóm tắt theo mẫu AI Usage Notes

**Claude Code.** *Sonnet 5.5 (claude-sonnet-5-5), Anthropic, Claude Code trong VS Code trên Windows 11*, truy cập 08:00 ngày 30/10/2026, prompt: như trên (Mục 11, Bước 2 và Bước 4 cho 011), dùng để viết Section 10 của `use-case-specification.md`; AI tạo `UC-011-01` đến `UC-011-05` (tổng 66 alternative flow), bảng truy vết 10.1, bảng map màn hình 10.2 với 13 Asset Gaps (011-A đến 011-M), 8 câu hỏi mở 10.3 (`OQ-011-01` đến `OQ-011-08`); sinh viên: yêu cầu đối chiếu lại use case với spec gốc và prototype (kết quả ở Mục 17), tự chốt quyết định về `US3` của 010 và `US4`/`US5` của 080, chạy lại các lệnh kiểm tra; đọc duyệt từng use case: Hiển yêu cầu AI tự đối chiếu với spec gốc và prototype (Mục 17, Mục 21); Open Questions: Hiển đã quyết định (Mục 18, Mục 19); gửi Asset Gaps cho Chung: Hiển báo đã gửi cho Chung lúc 09:29 ngày 30/10/2026.

### Prompt đã dùng

Như trên (Mục 11): Bước 2 (Section 10, spec 011), Bước 3 (cập nhật dòng `v1.2`) và Bước 4 (map prototype cho `PF01`–`PF08`).

### Tóm tắt phản hồi của AI

- Section 10 của `docs/requirements/use-case-specification.md` (627 dòng thêm, 1 dòng sửa là dòng `v1.2` của chính task này):
  - `UC-011-01` Set Up Dietary Profile: 11 bước, 18 alternative flow (sửa hồ sơ có sẵn, xóa sạch, số người và thời gian không hợp lệ, giá trị ngoài danh mục, vượt 50 nguyên liệu, dữ liệu sai từ client, rời trang chưa lưu, mất mạng, hết phiên, hai tab lưu cùng lúc, bấm lặp, nguyên liệu bị gộp/xóa, không tải được, người không phải chủ sở hữu hoặc Administrator đọc hồ sơ, khách, tài khoản bị đình chỉ/cấm, vượt giới hạn lưu).
  - `UC-011-02` Edit Public Profile & Avatar: 10 bước, 16 alternative flow (tên hiển thị không hợp lệ, giới thiệu quá 300 ký tự, ảnh sai loại hoặc quá 5 MB, file không phải ảnh thật, xóa ảnh, xem hồ sơ công khai, người khác đòi dữ liệu riêng tư, Administrator xem hồ sơ, rời trang, mất mạng, hết phiên, bấm lặp, giới hạn tần suất, đình chỉ/cấm, không tải được, khách).
  - `UC-011-03` Change or Set Password: 8 bước, 12 alternative flow (sai mật khẩu hiện tại, sai lặp lại, mật khẩu mới sai quy tắc hoặc trùng mật khẩu cũ, xác nhận không khớp, tài khoản chỉ dùng Google đặt mật khẩu, xác thực lại Google thất bại, tab cũ, bấm lặp, mất mạng/hết phiên, đình chỉ/cấm, người khác đổi mật khẩu).
  - `UC-011-04` Delete Account: 8 bước, 11 alternative flow (sai thông tin hoặc sai chữ xác nhận, tài khoản Google, Administrator cuối cùng, đình chỉ có thời hạn, tài khoản bị cấm, tài khoản thường, xóa lỗi giữa chừng, phiên khác đang mở, bấm lặp, hủy, mất mạng).
  - `UC-011-05` Apply Dietary Hard Rules (use case được include, viết với "Trigger" thay cho actor chính theo `use-case-model.md` §6.2 vấn đề 5): 8 bước, 9 alternative flow.
  - 10.1 bảng truy vết (5 user story, 39 acceptance scenario, 44 FR, 8 NFR, `SC-001`–`SC-008`); 10.2 bảng map màn hình; 10.3 câu hỏi mở.
- Các chỗ spec thiếu hoặc không rõ, ghi ở 10.3 thay vì tự bịa luồng: hành vi khi không đọc được hồ sơ ăn uống (rủi ro an toàn dị ứng), trạng thái rỗng/lỗi của hồ sơ công khai, hồ sơ công khai của người bị đình chỉ/cấm, giới hạn thử mật khẩu khi đổi mật khẩu và khi xóa tài khoản, "Đặt mật khẩu" có đăng xuất phiên khác không, các điểm tích hợp với 060/070/030/040 còn chờ xác nhận, `PF07` chỉ có biến thể nhập mật khẩu.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| `UC-011-01` đến `UC-011-05` | Dùng, sau khi được đối chiếu lại với spec 011 và `PF01`–`PF08` (Mục 17); không phát hiện chỗ lệch | Hiển yêu cầu đối chiếu lại; danh sách dữ liệu xóa/giữ ở `PF07` khớp `FR-031` – `FR-036` |
| Các câu hỏi mở `OQ-011-01` đến `OQ-011-08` | Dùng, kèm quyết định của Hiển cho từng câu (Mục 18): sửa spec 011 cho 4 câu, Hiển quyết `OQ-011-03`, gộp `OQ-011-06` vào `OQ-010-04`, chuyển 4 người `OQ-011-07`, đóng `OQ-011-08` | Hiển đã đọc spec 011 và chọn; việc sửa spec chờ làm và cần Duy duyệt |

### Cách kiểm chứng

- Đã kiểm tra bằng lệnh trong phiên: 8 file màn hình `PF01`–`PF08` được nhắc trong Section 10 đều tồn tại trong `docs/analysis-and-design/ui-ux/screens/`; số alternative flow đếm được 18, 16, 12, 11, 9; `git show --stat HEAD` chỉ có `docs/requirements/use-case-specification.md`.
- Phần Hiển tự làm / kiểm chứng (đối chiếu với `spec.md`): Hiển yêu cầu đối chiếu lại use case với spec gốc và prototype (Mục 17), tự chốt các quyết định về `US3` của 010 và `US4`/`US5` của 080, và đã chạy lại các lệnh kiểm tra; đọc duyệt từng use case: Hiển yêu cầu AI tự đối chiếu với spec gốc và prototype (Mục 17, Mục 21); Open Questions: Hiển đã quyết định (Mục 18, Mục 19); gửi Asset Gaps cho Chung: Hiển báo đã gửi cho Chung lúc 09:29 ngày 30/10/2026.

---

## Mục 14: Task SCRUM-50 – Bước 2: viết Use-Case Specification cho spec 080 (UC-080-01 đến UC-080-04)

**Task: SCRUM-50**

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-30 |
| Giờ truy cập | 08:04 (giờ của commit `a626527`, lấy bằng `git log`) |
| Người thực hiện | Mai Văn Hiển |
| Spec / Tài liệu liên quan | `specs/080-bookmark-collection/spec.md`, `docs/requirements/use-case-model.md` §4.8 và §5, `docs/requirements/vision-document.md` §5.8, `.specify/memory/constitution.md`; kết quả ghi vào `docs/requirements/use-case-specification.md` (Section 11) |
| Công cụ AI | Claude Code (tiện ích mở rộng VS Code, Windows 11), model Sonnet 5.5 (`claude-sonnet-5-5`); phiên bản Claude Code: 2.1.312 |
| Mục đích | Viết 4 use case của spec 080, bảng truy vết, bảng map màn hình `BM01`–`BM06` kèm danh sách Asset Gaps, danh sách câu hỏi mở; cập nhật hàng Section 11 và dòng `v1.2` |
| Nhánh Git | `docs/scrum-50-uc-spec-010-011-080` (commit `a626527`, thông điệp `docs(uc-spec): add UC-080 specs for SCRUM-50`) |

### Dòng tóm tắt theo mẫu AI Usage Notes

**Claude Code.** *Sonnet 5.5 (claude-sonnet-5-5), Anthropic, Claude Code trong VS Code trên Windows 11*, truy cập 08:04 ngày 30/10/2026, prompt: như trên (Mục 11, Bước 2 và Bước 4 cho 080), dùng để viết Section 11 của `use-case-specification.md`; AI tạo `UC-080-01` đến `UC-080-04` (tổng 68 alternative flow), bảng truy vết 11.1, bảng map màn hình 11.2 với 14 Asset Gaps (080-A đến 080-N), 5 câu hỏi mở 11.3 (`OQ-080-01` đến `OQ-080-05`); sinh viên: yêu cầu đối chiếu lại use case với spec gốc và prototype (kết quả ở Mục 17), tự chốt quyết định về `US3` của 010 và `US4`/`US5` của 080, chạy lại các lệnh kiểm tra; đọc duyệt từng use case: Hiển yêu cầu AI tự đối chiếu với spec gốc và prototype (Mục 17, Mục 21); Open Questions: Hiển đã quyết định (Mục 18, Mục 19); gửi Asset Gaps cho Chung: Hiển báo đã gửi cho Chung lúc 09:29 ngày 30/10/2026.

### Prompt đã dùng

Như trên (Mục 11): Bước 2 (Section 11, spec 080), Bước 3 (cập nhật dòng `v1.2`) và Bước 4 (map prototype cho `BM01`–`BM06`).

### Tóm tắt phản hồi của AI

- Section 11 của `docs/requirements/use-case-specification.md`:
  - `UC-080-01` Save Recipe with One Tap (mở rộng `UC-020-06`): 7 bước, 16 alternative flow (lưu từ thẻ, bỏ lưu khi không có bộ sưu tập, bỏ lưu khi có bộ sưu tập kèm xác nhận, hoàn tác, hoàn tác vượt giới hạn, hết thời gian hoàn tác, khách bấm lưu, "Đã lưu" đầy, công thức không còn PUBLISHED, lỗi mạng/máy chủ, mất kết nối, bấm lặp, hết phiên, tài khoản bị đình chỉ/cấm, email chưa xác minh hoặc tác giả tự lưu, công thức đang trong meal plan).
  - `UC-080-02` Add Recipe to Collections (mở rộng `UC-080-01`): 7 bước, 17 alternative flow.
  - `UC-080-03` Create, Rename & Delete Collection (mở rộng `UC-080-02`): 8 bước, 13 alternative flow.
  - `UC-080-04` Browse Saved Recipes & Collections: 7 bước, 22 alternative flow, gồm cả `US4` và `US5` (công thức không còn khả dụng, thanh "Bỏ tất cả", công thức được khôi phục, bộ sưu tập của người khác trả về "không tìm thấy"); phần cung cấp dữ liệu cho meal planner (`FR-026`) ghi ở Postconditions.
  - 11.1 bảng truy vết (5 user story, 43 acceptance scenario, 26 FR, 7 NFR, `SC-001`–`SC-010`); 11.2 bảng map màn hình; 11.3 câu hỏi mở.
- Mô hình use case (§4.8) gộp `US4` và `US5` vào `UC-080-04`, nên spec 080 có 5 user story nhưng chỉ có 4 use case; điều này được ghi ở `OQ-080-01` kèm đề xuất tách `UC-080-05` nếu reviewer muốn.
- Các chỗ spec và prototype lệch nhau, ghi ở 11.3 thay vì tự bịa: chữ "Không tìm thấy trang" của `CM01` so với "Không tìm thấy bộ sưu tập" của spec, nút Lưu của Khách ở `EP-5` so với ma trận quyền, các điểm tích hợp còn mở với 030/031/100, menu thẻ công thức và hành động "Bỏ lưu" chưa có màn hình.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| `UC-080-01` đến `UC-080-04` | Dùng, sau khi được đối chiếu lại với spec 080 và `BM01`–`BM06` (Mục 17); sửa 1 chỗ ở `UC-080-01` Step 5 | Hiển yêu cầu đối chiếu lại; nút "Xem" của prototype `BM05` thuộc thông báo "đã đầy", không thuộc thông báo "Đã lưu công thức" |
| Gộp `US4` và `US5` vào `UC-080-04` (`OQ-080-01`) | Dùng (quyết định của Hiển): gộp trong `UC-080-04` với 22 alternative flow | `use-case-model.md` §4.8 coi `US5` là alternative flow của `UC-080-04`; muốn tách `UC-080-05` thì phải sửa model trước freeze 02/11 |
| Các câu hỏi mở `OQ-080-02` đến `OQ-080-05` | Dùng, kèm quyết định của Hiển (Mục 18): `OQ-080-02` chuyển Duy, `OQ-080-03` chuyển Chung, `OQ-080-04` chuyển Dự và Chung, `OQ-080-05` đóng thành Gap 080-O | Hiển đã đọc spec 080 và chọn |

### Cách kiểm chứng

- Đã kiểm tra bằng lệnh trong phiên: 9 file màn hình được nhắc trong Section 11 (`BM01`–`BM06`, `AU08`, `CM01`, `CM04`) đều tồn tại trong `docs/analysis-and-design/ui-ux/screens/`; số alternative flow đếm được 16, 17, 13, 22; commit chỉ chứa `docs/requirements/use-case-specification.md`.
- Phần Hiển tự làm / kiểm chứng (đối chiếu với `spec.md`): Hiển yêu cầu đối chiếu lại use case với spec gốc và prototype (Mục 17), tự chốt các quyết định về `US3` của 010 và `US4`/`US5` của 080, và đã chạy lại các lệnh kiểm tra; đọc duyệt từng use case: Hiển yêu cầu AI tự đối chiếu với spec gốc và prototype (Mục 17, Mục 21); Open Questions: Hiển đã quyết định (Mục 18, Mục 19); gửi Asset Gaps cho Chung: Hiển báo đã gửi cho Chung lúc 09:29 ngày 30/10/2026.

---

## Mục 15: Task SCRUM-50 – Bước 4: map màn hình prototype và lập danh sách Asset Gaps (SCRUM-58)

**Task: SCRUM-50**

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-30 |
| Giờ truy cập | 08:05 (lệnh `date` khi ghi mục này; việc map được làm xen kẽ trong Mục 12–14) |
| Người thực hiện | Mai Văn Hiển |
| Spec / Tài liệu liên quan | `docs/analysis-and-design/ui-ux/screens/` (`AU02`–`AU09`, `PF01`–`PF08`, `BM01`–`BM06`, `CM01`, `CM04`), `docs/requirements/use-case-specification.md` (§9.2, §10.2, §11.2) |
| Công cụ AI | Claude Code (tiện ích mở rộng VS Code, Windows 11), model Sonnet 5.5 (`claude-sonnet-5-5`); phiên bản Claude Code: 2.1.312 |
| Mục đích | Đọc chữ hiển thị của từng màn hình, map vào basic flow và alternative flow của 13 use case, ghi các chỗ chưa có màn hình thành Asset Gaps để gửi Chung (SCRUM-58) trước 31/10 |
| Nhánh Git | `docs/scrum-50-uc-spec-010-011-080` (nằm trong các commit `fa05da0`, `b69ed16`, `a626527`) |

### Dòng tóm tắt theo mẫu AI Usage Notes

**Claude Code.** *Sonnet 5.5 (claude-sonnet-5-5), Anthropic, Claude Code trong VS Code trên Windows 11*, truy cập 08:05 ngày 30/10/2026, prompt: như trên (Mục 11, Bước 4), dùng để map màn hình prototype vào use case; AI trích chữ hiển thị từ các file `.dc.html`, đối chiếu với từng bước và alternative flow, tạo bảng 9.2, 10.2, 11.2; sinh viên: yêu cầu đối chiếu lại use case với spec gốc và prototype (kết quả ở Mục 17), tự chốt quyết định về `US3` của 010 và `US4`/`US5` của 080, chạy lại các lệnh kiểm tra; đọc duyệt từng use case: Hiển yêu cầu AI tự đối chiếu với spec gốc và prototype (Mục 17, Mục 21); Open Questions: Hiển đã quyết định (Mục 18, Mục 19); gửi Asset Gaps cho Chung: Hiển báo đã gửi cho Chung lúc 09:29 ngày 30/10/2026.

### Prompt đã dùng

Như trên (Mục 11): Bước 4.

### Tóm tắt phản hồi của AI

- Đọc chữ hiển thị của 8 màn `AU02`–`AU09`, 8 màn `PF01`–`PF08`, 6 màn `BM01`–`BM06` và `CM01`, `CM04` (không mở trình duyệt, không tạo ảnh).
- Kết quả: 41 Asset Gaps (010: 14, 011: 13, 080: 14) và các màn đã có được ghi vào bảng 9.2, 10.2, 11.2 theo mẫu của Mục 4 và 8.3 trong file. Màn `AU01-Login` có trong `canvas.json` và `index.html` nhưng không có trong thư mục `screens/` (Gap 010-D).
- Không vẽ UI lệch spec: các chỗ prototype khác spec được ghi thành câu hỏi mở thay vì thêm luồng, gồm nút "Đổi địa chỉ email" của `AU03` (`OQ-010-09`), `PF07` chỉ có biến thể mật khẩu (`OQ-011-08`), chữ "Không tìm thấy trang" của `CM01` (`OQ-080-03`).

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Danh sách Asset Gaps (41 lúc ghi mục này, nay 40 sau khi rút Gap 010-D và 010-E ở Mục 20) | Dùng, sau khi AI đọc lại các file `.dc.html` để đối chiếu (Mục 17, Mục 21); đã gửi Chung: Hiển báo đã gửi cho Chung lúc 09:29 ngày 30/10/2026 | Cần gửi Chung (SCRUM-58) trước 31/10 |
| Phần ghi nhận prototype lệch spec | Dùng; chuyển Chung: `OQ-010-09` (gỡ "Đổi địa chỉ email" khỏi `AU03`) và `OQ-080-03` (chuỗi "Không tìm thấy bộ sưu tập" cho `CM01`); đã gửi Chung: Hiển báo đã gửi cho Chung lúc 09:29 ngày 30/10/2026 | Prototype không được dùng để thêm luồng ngoài spec |

### Cách kiểm chứng

- Mọi tên file màn hình trong ba bảng đều tồn tại trong thư mục `screens/` (đã kiểm tra bằng lệnh).
- Phần Hiển tự làm (gửi danh sách gap cho Chung, đối chiếu với prototype thực tế): Hiển yêu cầu đối chiếu lại use case với spec gốc và prototype (Mục 17), tự chốt các quyết định về `US3` của 010 và `US4`/`US5` của 080, và đã chạy lại các lệnh kiểm tra; đọc duyệt từng use case: Hiển yêu cầu AI tự đối chiếu với spec gốc và prototype (Mục 17, Mục 21); Open Questions: Hiển đã quyết định (Mục 18, Mục 19); gửi Asset Gaps cho Chung: Hiển báo đã gửi cho Chung lúc 09:29 ngày 30/10/2026.

---

## Mục 16: Task SCRUM-50 – Bước 5: kiểm tra trước khi báo xong

**Task: SCRUM-50**

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-30 |
| Giờ truy cập | 08:05 (lệnh `date`) |
| Người thực hiện | Mai Văn Hiển |
| Spec / Tài liệu liên quan | `docs/requirements/use-case-specification.md` (Section 9–11) |
| Công cụ AI | Claude Code (tiện ích mở rộng VS Code, Windows 11), model Sonnet 5.5 (`claude-sonnet-5-5`); phiên bản Claude Code: 2.1.312 |
| Mục đích | Kiểm tra ID liên tục không trùng, liên kết màn hình tồn tại, file nào bị thay đổi, ngôn ngữ tiếng Anh, không dùng Mermaid |
| Nhánh Git | `docs/scrum-50-uc-spec-010-011-080` |

### Dòng tóm tắt theo mẫu AI Usage Notes

**Claude Code.** *Sonnet 5.5 (claude-sonnet-5-5), Anthropic, Claude Code trong VS Code trên Windows 11*, truy cập 08:05 ngày 30/10/2026, prompt: như trên (Mục 11, Bước 5), dùng để kiểm tra Section 9–11; AI chạy các lệnh kiểm tra và báo kết quả; sinh viên: yêu cầu đối chiếu lại use case với spec gốc và prototype (kết quả ở Mục 17), tự chốt quyết định về `US3` của 010 và `US4`/`US5` của 080, chạy lại các lệnh kiểm tra; đọc duyệt từng use case: Hiển yêu cầu AI tự đối chiếu với spec gốc và prototype (Mục 17, Mục 21); Open Questions: Hiển đã quyết định (Mục 18, Mục 19); gửi Asset Gaps cho Chung: Hiển báo đã gửi cho Chung lúc 09:29 ngày 30/10/2026.

### Prompt đã dùng

Như trên (Mục 11): Bước 5.

### Tóm tắt phản hồi của AI

- 13 use case, ID `UC-010-01`–`04`, `UC-011-01`–`05`, `UC-080-01`–`04`, không trùng.
- Không dùng Mermaid trong Section 9–11 (bảng và danh sách thay cho sơ đồ).
- Mọi file màn hình được nhắc đều có thật trong `docs/analysis-and-design/ui-ux/screens/`.
- `git diff origin/main..HEAD --stat` chỉ gồm `docs/requirements/use-case-specification.md` (1.738 dòng thêm, 0 dòng xóa so với `origin/main`); `ai_log_hien.md` chưa add, `AI Usage Guidelines.pdf` và `wikicook-pa3-team-assignment.md` chưa theo dõi.
- Quét nội dung: các dòng có tiếng Việt ngoài dấu trích dẫn chỉ là tên người ở các dòng "Owner" và dòng metadata; chuỗi giao diện tiếng Việt đều đặt trong dấu ngoặc kép như các Section hiện có.
- Chưa chạy bước convert Markdown sang PDF và chưa render xem trên GitHub.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Kết quả các lệnh kiểm tra | Dùng; Hiển đã chạy lại các lệnh kiểm tra | Xác nhận PR chỉ chứa `use-case-specification.md` |

### Cách kiểm chứng

- Lệnh có thể chạy lại: `git diff origin/main..HEAD --stat`, `git status -s`, kiểm tra tên file màn hình trong `screens/`.
- Phần Hiển tự làm / kiểm chứng: Hiển yêu cầu đối chiếu lại use case với spec gốc và prototype (Mục 17), tự chốt các quyết định về `US3` của 010 và `US4`/`US5` của 080, và đã chạy lại các lệnh kiểm tra; đọc duyệt từng use case: Hiển yêu cầu AI tự đối chiếu với spec gốc và prototype (Mục 17, Mục 21); Open Questions: Hiển đã quyết định (Mục 18, Mục 19); gửi Asset Gaps cho Chung: Hiển báo đã gửi cho Chung lúc 09:29 ngày 30/10/2026.

---

## Mục 17: Task SCRUM-50 – đối chiếu lại use case với spec gốc và prototype, sửa 3 chỗ lệch

**Task: SCRUM-50**

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-30 |
| Giờ truy cập | 08:29 (lệnh `date`) |
| Người thực hiện | Mai Văn Hiển |
| Spec / Tài liệu liên quan | `specs/010-user-authentication-roles/spec.md`, `specs/011-user-profile-dietary/spec.md`, `specs/080-bookmark-collection/spec.md`, các file `AU02`–`AU09`, `PF01`–`PF08`, `BM01`–`BM06`, `CM01`, `CM04` trong `docs/analysis-and-design/ui-ux/screens/`, `docs/requirements/use-case-specification.md` (Section 9–11) |
| Công cụ AI | Claude Code (tiện ích mở rộng VS Code, Windows 11), model Sonnet 5.5 (`claude-sonnet-5-5`); phiên bản Claude Code: 2.1.312 |
| Mục đích | Đọc lại 13 use case, đối chiếu với spec gốc (FR, NFR, SC, Edge Cases, Acceptance Scenarios) và với nội dung các file prototype `.dc.html`; sửa chỗ lệch |
| Nhánh Git | `docs/scrum-50-uc-spec-010-011-080` (commit `639acc7`, thông điệp `docs(uc-spec): align UC-010 and UC-080 wording with spec and prototype for SCRUM-50`) |

### Dòng tóm tắt theo mẫu AI Usage Notes

**Claude Code.** *Sonnet 5.5 (claude-sonnet-5-5), Anthropic, Claude Code 2.1.312 trong VS Code trên Windows 11*, truy cập 08:29 ngày 30/10/2026, prompt: "hãy đọc lại usecase và đối chiếu với spec gốc … hãy mở prototype .dc.html để đối chiếu" (kèm quyết định của Hiển về `US3` của 010 và `US4`/`US5` của 080), dùng để kiểm tra Section 9–11; AI quét mã FR/NFR/SC, đối chiếu Edge Cases và Acceptance Scenarios, đọc lại mã HTML của các màn hình, rồi sửa 3 chỗ trong `use-case-specification.md`; sinh viên: yêu cầu kiểm tra, chốt các quyết định ghi trong Mục 12 và 14, và chạy lại các lệnh kiểm tra.

### Prompt đã dùng

```
hãy đọc lại usecase và đối chiếu với spec gốc
US3 của 010: không tạo UC-010-05. Phần Guest và phân quyền được viết thành AF 2.9, 2.15, 2.16 của UC-010-02, nêu tại OQ-010-01. Lý do nêu là use-case-model.md đã chốt 4/5/4.
US4 và US5 của 080: gộp trong UC-080-04 (22 AF), nêu tại OQ-080-01. Muốn tách UC-080-05 thì phải sửa model trước freeze 02/11.
hãy mở prototype .dc.html để đối chiếu
đã chạy
```

### Tóm tắt phản hồi của AI

- Kiểm tra bằng script: mọi mã `FR-xxx`, `NFR-xxx`, `SC-xxx` của ba spec đều xuất hiện trong Section tương ứng (0 mã thiếu).
- Đối chiếu thủ công Edge Cases và Acceptance Scenarios của ba spec với các alternative flow: không thiếu luồng; riêng quy tắc Administrator cuối cùng (`FR-023`) và email đình chỉ/cấm (`FR-024a`) của spec 010 thuộc `UC-100-03`.
- Đọc lại mã HTML của `BM05`, `BM01`, `AU03`, `AU07` (và chữ hiển thị của các màn còn lại). Phát hiện và sửa 3 chỗ:
  1. `UC-080-01` Step 5: bỏ liên kết "Xem" khỏi thông báo "Đã lưu công thức"; trong `BM05`, nút "Xem" nằm ở thông báo "Danh sách Đã lưu đã đầy (1.000)", còn thông báo "Đã lưu công thức" chỉ có nút "Thêm vào bộ sưu tập".
  2. `UC-010-02` `AF 2.4`: câu "nội dung đã đăng vẫn hiển thị" chỉ có ở prototype `AU07`, không có ở `FR-013`; đã chuyển thành "as the prototype adds".
  3. Mục 9.1: thêm câu nói rõ email của `FR-024a` (đình chỉ, cấm, hết đình chỉ, gỡ cấm) được gửi khi Administrator thao tác theo spec 100, còn `UC-010-02` chỉ phụ trách thông báo đổi vai trò trong ứng dụng.
- Không phát hiện chỗ lệch ở Section 10 (011) và các màn `PF01`–`PF08`.
- Các quyết định `US3` của 010 và `US4`/`US5` của 080 do Hiển cung cấp được ghi lại trong bảng quyết định của Mục 12 và Mục 14.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Sửa 3 chỗ lệch ở `UC-080-01`, `UC-010-02` và mục 9.1 | Dùng (commit `639acc7`) | Câu chữ phải bám spec gốc; chi tiết riêng của prototype phải được nêu là của prototype |
| Kết luận không có chỗ lệch ở Section 10 | Dùng; AI đã đối chiếu lại 8 màn `PF01`–`PF08` (và bản `W-`) ở Mục 21, không lệch; Hiển đã đọc cả ba spec (theo thông báo của Hiển ở Mục 18) | Chữ trong bảng 10.2 khớp chữ trong các file prototype |

### Cách kiểm chứng

- Lệnh có thể chạy lại: `git show 639acc7` (3 dòng bị sửa trong `use-case-specification.md`); so sánh mã `FR`/`NFR`/`SC` của spec với Section 9–11.
- Hiển đã chạy lại các lệnh kiểm tra (theo thông báo của Hiển).

---

## Mục 18: Task SCRUM-50 – ghi các quyết định về Open Questions và sửa thứ tự kiểm tra đăng nhập

**Task: SCRUM-50**

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-30 |
| Giờ truy cập | 08:56 (lệnh `date`) |
| Người thực hiện | Mai Văn Hiển |
| Spec / Tài liệu liên quan | `docs/requirements/use-case-specification.md` (§9.3, §10.3, §11.3, `UC-010-02`, `UC-011-03`, bảng 11.2) |
| Công cụ AI | Claude Code (tiện ích mở rộng VS Code, Windows 11), model Sonnet 5.5 (`claude-sonnet-5-5`); phiên bản Claude Code: 2.1.312 |
| Mục đích | Ghi lại quyết định của Hiển cho 23 Open Questions vào tài liệu; sửa thứ tự các bước kiểm tra ở `UC-010-02`; thay các câu "not specified" ở `UC-011-03` bằng quy tắc đã chốt; thêm Gap 080-O |
| Nhánh Git | `docs/scrum-50-uc-spec-010-011-080` (commit `f0930c4`, thông điệp `docs(uc-spec): record open-question decisions and reorder sign-in checks for SCRUM-50`) |

### Dòng tóm tắt theo mẫu AI Usage Notes

**Claude Code.** *Sonnet 5.5 (claude-sonnet-5-5), Anthropic, Claude Code 2.1.312 trong VS Code trên Windows 11*, truy cập 08:56 ngày 30/10/2026, prompt: bảng quyết định cho Open Questions của spec 010, 011, 080 và hai chỗ cần sửa trong use case do Hiển viết (nguyên văn bên dưới), dùng để cập nhật `use-case-specification.md`; AI chép từng quyết định vào dưới mục Open Question tương ứng, đổi thứ tự bước ở `UC-010-02`, cập nhật `UC-011-03` và thêm Gap 080-O; sinh viên: đọc cả ba spec, quyết định từng Open Question và nêu hai chỗ cần sửa trong use case.

### Prompt đã dùng

Tóm lược (nguyên văn nằm trong hội thoại) bảng quyết định của Hiển cho từng Open Question (spec 010: giữ 010-01, 010-08; sửa spec 010-02, 010-03, 010-05, 010-06, 010-07; hoãn 010-04, 010-10; chuyển Chung 010-09. Spec 011: sửa 011-01, 011-02, 011-04, 011-05; Hiển quyết 011-03; gộp 011-06 vào 010-04; chuyển 4 người 011-07; bỏ 011-08. Spec 080: giữ 080-01; chuyển Duy 080-02; chuyển Chung 080-03; chuyển Dự và Chung 080-04; bỏ 080-05 và thêm Gap 080-O), thứ tự làm (1. 011-01, 010-02, 011-05 trước 01/11; 2. gửi hôm nay các việc chuyển người; 3. sửa một dòng nếu kịp: 010-03, 010-05, 010-06, 011-02, 011-04; 4. hoãn 010-04, 010-10) và hai chỗ cần sửa trong use case:

```
- UC-010-02 Step 6 và 7 vẫn cần đổi thứ tự: khóa, rồi mật khẩu, rồi mới đình chỉ/cấm. Spec cũng ngầm hiểu vậy.
- AF 3.10 của UC-011-03 bỏ câu "not specified", thay bằng quy tắc đã chốt ở 011-04.
```

Bảng quyết định đầy đủ của Hiển nằm trong hội thoại; nội dung từng quyết định được chép vào dưới mỗi Open Question trong `use-case-specification.md` (dòng "Decision (30/10/2026, Mai Văn Hiển)").

### Tóm tắt phản hồi của AI

- Thêm 23 dòng "Decision (30/10/2026, Mai Văn Hiển)" dưới 23 Open Questions (10 của 010, 8 của 011, 5 của 080); `OQ-011-06` ghi "Closed — merged into `[OQ-010-04]`", `OQ-011-08` ghi "Closed — already tracked as Gap 011-K", `OQ-080-05` ghi "Closed — tracked as Gap 080-O". Thêm một câu đầu mỗi mục 9.3, 10.3, 11.3 nói các thay đổi spec 010/011 cần Duy duyệt qua PR.
- `UC-010-02`: đổi thứ tự thành khóa (Step 6), kiểm tra mật khẩu (Step 7), rồi mới kiểm tra đình chỉ/cấm khi thông tin đúng (Step 8); các bước sau dời thành Step 9–11 và các tham chiếu `AF 2.3`, `AF 2.4`, `AF 2.5`, `AF 2.7`, `AF 2.9`, `AF 2.10` cùng bảng 9.2 được cập nhật theo.
- `UC-011-03`: `AF 3.2` ghi bộ đếm riêng và không khóa đăng nhập; `AF 3.6` thêm bước đăng xuất các phiên khác khi đặt mật khẩu; `AF 3.10` ghi giữ giá trị đã nhập, riêng ô mật khẩu bị xóa. Ba chỗ này ghi rõ là quy tắc đã chốt ngày 30/10/2026 cho spec 011, chưa nằm trong file spec.
- Thêm dòng Gap 080-O (menu thẻ công thức với "Lưu vào bộ sưu tập" và "Bỏ lưu") vào bảng 11.2, nâng tổng số Asset Gaps từ 41 lên 42.
- Không sửa file spec nào (`specs/010-...`, `specs/011-...`, `specs/080-...`); các việc "Sửa 010", "Sửa 011" trong bảng quyết định vẫn chờ làm và cần Duy duyệt vì spec 010 đang ở trạng thái "Ready for Implementation".
- Ghi chú về hai chỗ chưa xong: `UC-010-02` `AF 2.3` chưa viết lại theo cách A hay cách B của `OQ-010-02` vì spec chưa đổi; `UC-011-04` `AF 4.1` chưa ghi giới hạn thử mật khẩu của `OQ-011-05` vì spec chưa đổi.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Quyết định cho 23 Open Questions | Dùng (quyết định của Hiển, chép nguyên ý vào tài liệu) | Hiển đọc cả ba spec và chọn cho từng câu |
| Đổi thứ tự `UC-010-02` (khóa, mật khẩu, đình chỉ/cấm) | Dùng (yêu cầu của Hiển) | Thông báo đình chỉ/cấm chỉ hiện khi thông tin đăng nhập đúng; khớp trigger của `AF 2.4` và `AF 2.5` |
| Thay "not specified" ở `AF 3.2`, `AF 3.10` và thêm bước ở `AF 3.6` của `UC-011-03` | Dùng; `AF 3.2` và `AF 3.6` do AI mở rộng thêm từ yêu cầu về `AF 3.10` | Cùng một Open Question `OQ-011-04`, để các luồng không mâu thuẫn nhau |
| Gap 080-O | Dùng (yêu cầu của Hiển) | Thay cho `OQ-080-05` |
| Việc sửa spec 010/011 | Chưa làm | Cần nhánh riêng và Duy duyệt qua PR |

### Cách kiểm chứng

- `git show f0930c4`: `use-case-specification.md` thay đổi 62 dòng (thêm 23 dòng Decision, sửa các bước ở `UC-010-02`, `UC-011-03`, thêm Gap 080-O); không có file spec nào trong `specs/` bị đụng.
- Đã kiểm tra bằng lệnh: đếm được 23 dòng "Decision (30/10/2026" và các tham chiếu "Step 10" còn lại đều thuộc use case khác, không còn trỏ vào `UC-010-02`.
- Phần Hiển đối chiếu lại từng Decision với bảng của mình: Hiển báo đã đối chiếu và nội dung khớp bảng của mình; nhưng việc đối chiếu chỉ ra các Decision bị đặt sai chỗ trong tài liệu (xem Mục 22), nên mục này không đúng như mô tả "đặt dưới mỗi Open Question" cho đến khi sửa ở Mục 22

---

## Mục 19: Task SCRUM-50 – chọn cách A cho OQ-010-02, giữ OQ-011-05 ở trạng thái mở

**Task: SCRUM-50**

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-30 |
| Giờ truy cập | 09:12 (lệnh `date`) |
| Người thực hiện | Mai Văn Hiển |
| Spec / Tài liệu liên quan | `docs/requirements/use-case-specification.md` (`UC-010-02` `AF 2.1`–`AF 2.3`, Step 6, `OQ-010-02`, `OQ-011-05`) |
| Công cụ AI | Claude Code (tiện ích mở rộng VS Code, Windows 11), model Sonnet 5.5 (`claude-sonnet-5-5`); phiên bản Claude Code: 2.1.312 |
| Mục đích | Viết lại `AF 2.2` và `AF 2.3` theo cách A của `OQ-010-02`; ghi "Proposed resolution: A, pending clarification of spec 010"; ghi `OQ-011-05` ở trạng thái mở kèm đề xuất; giữ `AF 4.1` như cũ |
| Nhánh Git | `docs/scrum-50-uc-spec-010-011-080` (commit `cb7467a`, thông điệp `docs(uc-spec): write sign-in lockout flows for option A of OQ-010-02 (SCRUM-50)`) |

### Dòng tóm tắt theo mẫu AI Usage Notes

**Claude Code.** *Sonnet 5.5 (claude-sonnet-5-5), Anthropic, Claude Code 2.1.312 trong VS Code trên Windows 11*, truy cập 09:12 ngày 30/10/2026, prompt: quyết định chọn cách A cho `OQ-010-02` kèm nguyên văn `AF 2.2`, `AF 2.3` và quyết định giữ `AF 4.1` (nguyên văn bên dưới), dùng để cập nhật `use-case-specification.md`; AI thay `AF 2.2`, `AF 2.3` bằng văn bản của Hiển, cập nhật dòng Decision của `OQ-010-02` và `OQ-011-05`, và chỉnh hai câu liên quan (Step 6, `AF 2.1`); sinh viên: chọn cách A, viết nội dung `AF 2.2`/`AF 2.3`, bản clarification đề xuất cho spec 011 và quyết định không thêm `AF 4.12` khi chưa có clarification.

### Prompt đã dùng

Tóm lược (nguyên văn nằm trong hội thoại): (1) `OQ-010-02` chọn cách A, lý do cách A giữ nguyên hành vi spec đã mô tả và chỉ mở rộng phạm vi đếm sang email chưa đăng ký; hai lưu ý cho bước plan (bộ đếm email lạ tự hết hạn sau 15 phút và giới hạn theo IP; audit entry không lưu email lạ ở dạng đọc được); kèm văn bản `AF 2.2`, `AF 2.3` và dòng "Proposed resolution: A, pending clarification of spec 010"; "đặt lại mật khẩu xóa khóa" để ngoài AF vì spec chưa nói; sửa thứ tự Step 6 và 7 của `UC-010-02`. (2) `AF 4.1` của `UC-011-04` giữ nguyên (chỉ từ chối); khi clarification của spec 011 được duyệt thì thêm `AF 4.12` và cập nhật `OQ-011-05`; trong lúc chờ, `OQ-011-05` giữ trạng thái Open; kèm bản clarification đề xuất cho spec 011 và văn bản `AF 4.12`.

### Tóm tắt phản hồi của AI

- `UC-010-02`: thay `AF 2.2` và `AF 2.3` bằng văn bản Hiển cung cấp (so sánh băm giả để thời gian phản hồi tương đương, bộ đếm tạm 15 phút theo email đã chuẩn hóa, audit entry không lưu email dạng đọc được, khóa theo email, email `FR-012` chỉ gửi khi có tài khoản thật, cùng một màn "Không thể đăng nhập" cho mọi email).
- Chỉnh hai câu cho nhất quán với bộ đếm theo email: Step 6 nay kiểm tra "sign-in for the normalized email is not locked" và `AF 2.1` đếm "for the normalized email". Thứ tự khóa, mật khẩu, rồi đình chỉ/cấm đã được sửa ở Mục 18.
- `OQ-010-02`: dòng Decision ghi cách A, "Proposed resolution: A, pending clarification of spec 010", hai lưu ý cho `/speckit-plan`, và nêu "đặt lại mật khẩu xóa khóa" chưa thuộc các luồng vì spec chưa nói.
- `OQ-011-05`: dòng Decision ghi trạng thái mở, đề xuất bộ đếm chung cho đổi mật khẩu và xóa tài khoản (5 lần sai trong 15 phút, khóa 15 phút, tách khỏi bộ đếm đăng nhập), `AF 4.1` giữ nguyên, `AF 4.12` sẽ thêm sau khi clarification được duyệt.
- Không thêm `AF 4.12` và không sửa file spec nào trong `specs/`.
- Mục 18 có một câu nay đã cũ ("`AF 2.3` chưa viết lại theo cách A hay B"); mục này thay thế câu đó và Mục 18 không bị sửa.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Cách A cho `OQ-010-02` và văn bản `AF 2.2`, `AF 2.3` | Dùng nguyên văn (quyết định của Hiển) | Cách A giữ hành vi spec đã mô tả (giờ thử lại, email `FR-012`), cách B đi ngược Edge Case |
| Chỉnh Step 6 và `AF 2.1` sang đếm theo email | Dùng; do AI thêm để nhất quán với `AF 2.3` | Bộ đếm theo email phải thống nhất ở cả ba chỗ |
| "Đặt lại mật khẩu xóa khóa" | Không đưa vào luồng (quyết định của Hiển) | Spec chưa nói; phải thêm vào clarification trước |
| Giữ `AF 4.1`, không thêm `AF 4.12` | Dùng (quyết định của Hiển) | Quy tắc chưa vào spec 011 thì chưa ghi thành hành vi chắc chắn |

### Cách kiểm chứng

- `git show cb7467a`: `use-case-specification.md` đổi 28 dòng (15 thêm, 13 xóa), không có file nào trong `specs/` bị đụng.
- Phần Hiển đọc lại `AF 2.2`, `AF 2.3` trong tài liệu và đối chiếu với văn bản của mình: Hiển báo đã đọc lại và phát hiện `AF 2.3` còn sai hai chỗ (trigger gộp "đang khóa" với "lần sai thứ 5"; câu "email đã được gửi" làm lộ khác biệt giữa email có và không có tài khoản); đã sửa ở Mục 22

---

## Mục 20: Task SCRUM-50 – sửa lỗi của AI về màn hình đăng nhập `AU01` và các bản web `W-`

**Task: SCRUM-50**

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-30 |
| Giờ truy cập | 09:25 (lệnh `date`) |
| Người thực hiện | Mai Văn Hiển |
| Spec / Tài liệu liên quan | `docs/analysis-and-design/ui-ux/index.html`, `docs/analysis-and-design/ui-ux/screens/W-AU01-Login.dc.html`, `docs/requirements/use-case-specification.md` (`UC-010-02`, §9.2) |
| Công cụ AI | Claude Code (tiện ích mở rộng VS Code, Windows 11), model Sonnet 5.5 (`claude-sonnet-5-5`); phiên bản Claude Code: 2.1.312 |
| Mục đích | Hiển hỏi cách làm chi tiết Bước 1 (mở prototype và đối chiếu); khi đọc `index.html` AI phát hiện kết luận trước đó "không có `AU01`" là sai và sửa tài liệu |
| Nhánh Git | `docs/scrum-50-uc-spec-010-011-080` (commit `2e65aea`, thông điệp `docs(uc-spec): map UC-010-02 sign-in to W-AU01-Login and note web variants (SCRUM-50)`) |

### Dòng tóm tắt theo mẫu AI Usage Notes

**Claude Code.** *Sonnet 5.5 (claude-sonnet-5-5), Anthropic, Claude Code 2.1.312 trong VS Code trên Windows 11*, truy cập 09:25 ngày 30/10/2026, prompt: "hãy nói chi tiết bước 1 hơn", dùng để hướng dẫn mở prototype; AI đọc `index.html` và thư mục `screens/`, phát hiện lỗi của chính mình ở Mục 11, 12, 15 và sửa `use-case-specification.md`; sinh viên: hỏi cách làm chi tiết Bước 1

### Prompt đã dùng

```
hãy nói chi tiết bước 1 hơn
```

### Tóm tắt phản hồi của AI

- Lỗi: ở Mục 11, 12 và 15 AI ghi "`AU01-Login` có trong `canvas.json` và `index.html` nhưng không có trong `screens/`" (Gap 010-D) và chỉ map các file `AU02`–`AU09`, `PF01`–`PF08`, `BM01`–`BM06`. Thực tế `index.html` nhúng `screens/W-AU01-Login.dc.html`, file này có trong thư mục, và mọi màn hình đều có thêm bản web tên `W-<mã>` (25 file `W-` trong nhóm `AU`, `PF`, `BM`, `CM01`, `CM04`). AI chỉ kiểm tra tên file `AU*` mà không tìm thêm tiền tố `W-`.
- Sửa trong `use-case-specification.md`: `UC-010-02` trỏ tới `W-AU01-Login.dc.html` (màn này có thông báo lỗi chung "Email hoặc mật khẩu không đúng", cảnh báo "Sai 5 lần liên tiếp, tài khoản sẽ tạm khóa 15 phút", "Ghi nhớ (30 ngày)", nút Google và ngữ cảnh khách bấm lưu); rút Gap 010-D và 010-E (còn 14 - 2 = 12 gap của 010, tổng số gap từ 42 xuống 40); thêm ghi chú về bản `W-` dưới ba bảng 9.2, 10.2, 11.2.
- Các câu trong Mục 11, 12, 15 và 17 còn nói `AU01` thiếu hoặc 41/42 gap là câu cũ; mục này thay thế các câu đó và các mục cũ không bị sửa.
- Chưa đối chiếu nội dung của 25 file `W-` với từng hàng của ba bảng; việc đó nằm trong Bước 1 của Hiển.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Rút Gap 010-D, 010-E và trỏ `UC-010-02` tới `W-AU01-Login.dc.html` | Dùng | Màn hình tồn tại và khớp `AF 2.1`, `AF 2.3`, `AF 2.9` |
| Ghi chú bản `W-` dưới ba bảng map | Dùng | Mọi màn có hai bản; các bảng chỉ liệt kê bản di động |
| Việc báo Chung rằng danh sách gap thay đổi (Gap 010-D, 010-E rút) | Đã làm | — |

### Cách kiểm chứng

- `git show 2e65aea`; `ls docs/analysis-and-design/ui-ux/screens | grep W-AU01` cho `W-AU01-Login.dc.html`.
- Nội dung `W-AU01-Login.dc.html` được AI đọc lại ở Mục 21 và khớp với `UC-010-02`; Hiển báo đã tự mở các file prototype trong trình duyệt (ngày 30/10/2026); Hiển không nêu chỗ lệch nào cần sửa

---

## Mục 21: Task SCRUM-50 – AI tự kiểm tra prototype và alternative flow theo yêu cầu của Hiển

**Task: SCRUM-50**

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-30 |
| Giờ truy cập | 09:30 (lệnh `date` chạy ngay trước mục này trả về 09:29 sau commit `dd0f70b`) |
| Người thực hiện | Mai Văn Hiển |
| Spec / Tài liệu liên quan | `docs/requirements/use-case-specification.md` (§9.2, §10.2, §11.2 và các mục "UI Screen References"), các file `.dc.html` trong `docs/analysis-and-design/ui-ux/screens/` (bản di động và bản `W-`), ba spec 010, 011, 080 |
| Công cụ AI | Claude Code (tiện ích mở rộng VS Code, Windows 11), model Sonnet 5.5 (`claude-sonnet-5-5`); phiên bản Claude Code: 2.1.312 |
| Mục đích | Thay cho Bước 1 và Bước 2 của danh sách việc Hiển: đối chiếu chữ trong tài liệu với chữ trong prototype, đối chiếu một số alternative flow với FR trong spec, rồi điền kết quả vào log |
| Nhánh Git | `docs/scrum-50-uc-spec-010-011-080` (commit `dd0f70b`, thông điệp `docs(uc-spec): note NU04 next to Gap 080-C (SCRUM-50)`) |

### Dòng tóm tắt theo mẫu AI Usage Notes

**Claude Code.** *Sonnet 5.5 (claude-sonnet-5-5), Anthropic, Claude Code 2.1.312 trong VS Code trên Windows 11*, truy cập 09:30 ngày 30/10/2026, prompt: "hãy tự kiểm tra và điền vào các phần cần điền trong Ai log" (nguyên văn bên dưới), dùng để kiểm tra Section 9–11; AI viết script đọc chữ trong các file `.dc.html`, đối chiếu với chuỗi trong tài liệu, đọc lại 5 alternative flow so với FR, sửa 1 dòng bảng 11.2 và điền các chỗ trống có thể điền trong log; sinh viên: yêu cầu AI tự kiểm tra và điền log.

### Prompt đã dùng

```
hãy tự kiểm tra và điền vào các phần cần điền trong Ai log
```

### Tóm tắt phản hồi của AI

- Đối chiếu prototype (đọc nội dung HTML, không mở trình duyệt, không kiểm tra bố cục, màu hay hành vi tương tác): 58 dòng trong Section 9–11 có nhắc tên file màn hình, 61 chuỗi tiếng Việt trong dấu ngoặc kép được tìm trong nội dung file di động hoặc file `W-` tương ứng. Kết quả: mọi tên file tồn tại; 59/61 chuỗi khớp; 2 chuỗi còn lại là do cách viết của tài liệu (gộp "Đăng nhập / Tạo tài khoản / Để sau" thành một chuỗi và nối "Bò lúc lắc — Không còn khả dụng" bằng dấu gạch), không phải lệch thật.
- Đọc thêm `NU04-Unavailable.dc.html`: đã là trang "Công thức này không còn khả dụng", nên Gap 080-C chỉ còn là thông báo khi bấm lưu; dòng bảng 11.2 được ghi chú như vậy (commit `dd0f70b`). Mục Gap 020-E trong phần của Duy cũng đang nói thiếu màn này; AI không sửa phần của Duy, chỉ báo.
- Đối chiếu alternative flow với FR trong spec (đọc lại, không phải chạy test):
  - `UC-010-02` `AF 2.3` với `FR-012` và Edge Case "Brute force": khớp (khóa 15 phút sau 5 lần sai, có giờ thử lại, có email); phần đếm cả email chưa đăng ký là mở rộng theo cách A, ghi rõ chờ clarification.
  - `UC-010-04` `AF 4.2` với `FR-019` và Integration Points: khớp (xóa dữ liệu tạo trước khi xác minh, vô hiệu mật khẩu, kết thúc phiên cũ, tên từ Google, đánh dấu đã xác minh, gửi sự kiện cho 011/060/080).
  - `UC-011-04` `AF 4.3` với `FR-037`, `US4` AS8 và `FR-023` của spec 010: khớp (Administrator hoạt động cuối cùng bị từ chối, nội dung nêu cần chỉ định Administrator khác trước).
  - `UC-011-01` `AF 1.6` với `FR-011` và Edge Case: khớp (mục thứ 51 bị từ chối, nêu giới hạn 50).
  - `UC-080-01` `AF 1.8` với `FR-011` và `US1` AS8: khớp (không lưu, biểu tượng giữ trạng thái chưa lưu, thông báo "Danh sách Đã lưu đã đầy", gợi ý bỏ món không còn khả dụng).
- Điền vào log các chỗ AI xác minh được: kết quả đối chiếu prototype và alternative flow; các chỗ chỉ Hiển biết (đã gửi Chung hay chưa, Hiển tự đối chiếu bảng Decision, Hiển tự mở file trên trình duyệt) vẫn để "Hiển điền".

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Kết quả kiểm tra tự động: tên file đều tồn tại, 59/61 chuỗi khớp, 2 chuỗi do cách viết | Dùng; Hiển báo đã mở tất cả các file prototype trong trình duyệt sau khi AI kiểm tra, và không nêu chỗ lệch nào | Kết quả tự động chỉ đọc chữ trong HTML nên cần mắt người xem bố cục |
| Ghi chú `NU04` ở Gap 080-C | Dùng (commit `dd0f70b`) | Tránh bảng 11.2 ngụ ý chưa có trang "công thức không còn khả dụng" |
| Gap 020-E trong phần của Duy đã cũ | Chuyển Duy | Không sửa phần của người khác |

### Cách kiểm chứng

- Chạy lại bằng cách mở `docs/analysis-and-design/ui-ux/index.html` và so từng hàng của bảng 9.2, 10.2, 11.2 (Hiển báo đã mở tất cả các file prototype trong trình duyệt ngày 30/10/2026).
- `git show dd0f70b` chỉ đổi một dòng bảng 11.2.

---

## Mục 22: Task SCRUM-50 – sửa lỗi của AI: Decision đặt sai chỗ, `AF 2.3` và `UC-011-03` chưa nêu là "proposed"

**Task: SCRUM-50**

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-30 |
| Giờ truy cập | 09:58 (lệnh `date` sau khi commit `925f74d`) |
| Người thực hiện | Mai Văn Hiển |
| Spec / Tài liệu liên quan | `docs/requirements/use-case-specification.md` (`UC-010-02` `AF 2.3`, `UC-011-03` `AF 3.2`/`AF 3.6`/`AF 3.10`, §9.3, §10.3, §11.3) |
| Công cụ AI | Claude Code (tiện ích mở rộng VS Code, Windows 11), model Sonnet 5.5 (`claude-sonnet-5-5`); phiên bản Claude Code: 2.1.312 |
| Mục đích | Sửa các lỗi Hiển phát hiện khi đối chiếu: Decision bị dồn sai chỗ ở cả ba khối Open Questions, `AF 2.3` còn hai chỗ sai, `OQ-010-02` mâu thuẫn với `AF 2.3`, và `UC-011-03` ghi quy tắc chưa có trong spec 011 như đã chốt |
| Nhánh Git | `docs/scrum-50-uc-spec-010-011-080` (commit `925f74d`, thông điệp `docs(uc-spec): place open-question decisions under their own items and fix AF 2.3 (SCRUM-50)`) |

### Dòng tóm tắt theo mẫu AI Usage Notes

**Claude Code.** *Sonnet 5.5 (claude-sonnet-5-5), Anthropic, Claude Code 2.1.312 trong VS Code trên Windows 11*, truy cập 09:58 ngày 30/10/2026, prompt: danh sách lỗi và dòng thay thế do Hiển cung cấp (nguyên văn bên dưới), dùng để sửa `use-case-specification.md`; AI sửa `AF 2.3`, `OQ-010-02`, `UC-011-03` và đặt lại 23 dòng Decision; sinh viên: đối chiếu tài liệu, tìm ra bốn lỗi và viết nội dung thay thế cho từng Decision.

### Prompt đã dùng

Tóm lược (nguyên văn nằm trong hội thoại): (1) `AF 2.3` của `UC-010-02` sửa hai chỗ: trigger tách "At Step 6, sign-in for the normalized email is locked" khỏi "lần sai thứ 5", và bước 3 thay "the note that an email was sent" bằng ghi chú trung tính "Nếu email này đã đăng ký, chúng tôi đã gửi thông báo". (2) `OQ-010-02` ghi "Closed, merged into OQ-010-04" là sai, `AF 2.3` lại trỏ về mục này; `Current Stance` còn nói dùng thông báo trung tính của `AF 2.2`. (3) Decision bị dồn sai chỗ ở cả ba khối (lần thứ hai); kèm dòng thay thế cho từng Open Question của spec 010, 011, 080. (4) `UC-011-03` viết hành vi chưa có trong spec 011 (`FR-028` chỉ nói đổi mật khẩu); phải ghi "proposed" ngay trong `AF 3.2`, `AF 3.6`, `AF 3.10` hoặc chắc chắn clarification đi trước khi Duy review.

### Tóm tắt phản hồi của AI

- Nguyên nhân lỗi Decision đặt sai chỗ: script ở Mục 18 tìm vị trí của chuỗi `[OQ-xxx]` bằng lần xuất hiện đầu tiên trong tài liệu, và chuỗi này đã xuất hiện sớm hơn ở phần thân các use case (ví dụ "see `[OQ-010-05]`"), nên dòng Decision được chèn vào Open Question đầu tiên của từng khối (010-01, 011-01, 080-01) thay vì đúng mục của nó. Lần kiểm tra ở Mục 18 chỉ đếm tổng số 23 dòng nên không phát hiện.
- Sửa: xóa toàn bộ dòng Decision cũ (kể cả bản trùng), rồi chèn lại 23 dòng, mỗi dòng ngay sau dòng Owner của chính Open Question đó, tìm theo tiêu đề dạng `**`[OQ-xxx]`**`; chạy lại kiểm tra từng cặp (Open Question, Decision) và thấy khớp một-một.
- Nội dung dùng đúng các dòng thay thế Hiển cung cấp cho `OQ-010-01` đến `OQ-010-08`, `OQ-011-01` đến `OQ-011-04`, `OQ-011-06`, `OQ-080-01`, `OQ-080-03`, `OQ-080-05`; các Open Question còn lại giữ nội dung đã viết ở Mục 18 và 19 (`OQ-010-09`, `OQ-010-10`, `OQ-011-05`, `OQ-011-07`, `OQ-011-08`, `OQ-080-02`, `OQ-080-04`).
- `AF 2.3`: trigger và bước 3 sửa đúng như Hiển yêu cầu. `OQ-010-02`: Decision nay là cách A, trạng thái Open chờ clarification; `Current Stance` đổi thành "Unknown emails are counted and get the same lock screen".
- `UC-011-03`: `AF 3.2`, bước 6 của `AF 3.6` và `AF 3.10` nay mở đầu bằng "Proposed, pending clarification of spec 011 (`[OQ-011-04]`)", và `OQ-011-04` ghi các AF này là quy tắc đề xuất. Spec 011 chưa đổi, nên các AF này chỉ đúng khi clarification được duyệt.
- Sai sót của AI cần ghi nhận: Mục 18 viết "đặt 23 dòng Decision dưới mỗi Open Question" nhưng thực tế đặt sai chỗ; Mục 19 viết "`OQ-010-02` ghi Decision cách A" nhưng dòng đó cũng nằm sai mục; hai mục đó không bị sửa và mục này thay thế các câu đó.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Bốn lỗi do Hiển nêu và các dòng Decision thay thế | Dùng (do Hiển viết) | Hiển đối chiếu tài liệu với bảng quyết định của mình |
| Đánh dấu "Proposed" trong `AF 3.2`, `AF 3.6`, `AF 3.10` | Dùng (chọn phương án ghi "proposed" ngay trong AF thay vì chờ clarification) | Giữ tiêu chí "không trái spec gốc"; clarification của spec 011 chưa được duyệt |
| Giữ 7 dòng Decision của Mục 18 và 19 cho `OQ-010-09`, `OQ-010-10`, `OQ-011-05`, `OQ-011-07`, `OQ-011-08`, `OQ-080-02`, `OQ-080-04` | Giữ nguyên | Hiển không nêu thay thế cho các mục này |

### Cách kiểm chứng

- `git show 925f74d`; trong §9.3, §10.3, §11.3 mỗi Open Question có đúng một dòng "Decision (30/10/2026, Mai Văn Hiển)" ngay dưới dòng Owner của nó (23 dòng, 23 Open Question).
- Phần Hiển đọc lại bản sau sửa: Hiển báo đã đọc lại 23 dòng Decision, `AF 2.3`, `OQ-010-02` và `AF 3.2`/`AF 3.6`/`AF 3.10` của `UC-011-03` trong tài liệu sau sửa và không phát hiện thêm lỗi
