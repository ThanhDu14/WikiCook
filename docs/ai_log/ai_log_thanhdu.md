# Nhật ký sử dụng AI – Thành Dự (ThanhDu14)

File này ghi lại các lần mình dùng Spec Kit trong dự án WikiCook. Mỗi lần dùng AI là một mục mới, mục mới nhất ở cuối.

| # | Ngày | Nội dung | Nhóm |
|---|---|---|---|
| 1 | 2026-10-02 | Viết constitution của dự án bằng `/speckit-constitution` | Chung |
| 2 | 2026-10-02 | Viết spec đăng nhập và phân quyền bằng `/speckit-specify` | 01 – Tài khoản |
| 3 | 2026-10-08 | Viết spec 030 (meal planner thủ công) bằng `/speckit-specify` + `/speckit-clarify` | 03 – Meal Planner |
| 4 | 2026-10-08 | Viết spec 031 (AI Weekly Meal Planner) bằng `/speckit-specify`, bổ sung gợi ý và xếp hạng theo bữa | 03 – Meal Planner |

---

## Mục 1: Viết constitution của dự án bằng `/speckit-constitution`

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-02 |
| Người thực hiện | Thành Dự |
| Nhóm tính năng liên quan | Chung – áp dụng cho mọi tính năng (`.specify/memory/constitution.md`) |
| Công cụ AI | Claude Code (model Claude Opus 5.5), lệnh Spec Kit `/speckit-constitution` |
| Mục đích | Viết bộ nguyên tắc nền của dự án để các bước `/speckit-plan`, `/speckit-analyze` đối chiếu |
| Nhánh Git | `feature/speckit-auth-spec` (commit `2061701`, merge vào `main` ở PR #7) |

### Prompt đã dùng

Trước đó mình hỏi AI "1 file constitution tốt là gồm những gì?". AI trả lời các tiêu chí và đưa một bản phác thảo cho WikiCook. Mình lấy bản phác thảo đó dán vào prompt:

```
Hãy dùng /spec-kit constitution viết cho mình với tech mình sài là java spring-boot, reactjs,
postgresql , và thêm các phần chính như

### I. Spec trước, code sau
Mọi tính năng MUST có spec.md được duyệt trước khi lập plan và code.
Mỗi user story MUST gắn mã Jira (SCRUM-xx).
Rationale: đồ án chấm theo quy trình SE; truy vết yêu cầu → code → test.

### II. Test-First (bắt buộc)
Test MUST được viết và FAIL trước khi viết code (Red → Green → Refactor).
Coverage MUST ≥ [70]% cho tầng business logic.
Rationale: đảm bảo mỗi yêu cầu có bằng chứng kiểm thử cho docs/test.

### III. Kiến trúc phân lớp đơn giản
[Frontend] ↔ [API] ↔ [Service] ↔ [Repository/DB]. Không gọi DB trực tiếp từ route.
Không thêm thư viện/pattern mới khi chưa có lý do ghi trong plan.md.
Rationale: nhóm sinh viên, cần dễ đọc, dễ chia việc (YAGNI).

### IV. Bảo mật & dữ liệu người dùng
Mật khẩu MUST được hash; secret MUST nằm trong .env (không commit).
Mọi input từ người dùng (công thức, bình luận) MUST được validate phía server.

### V. Trải nghiệm nhất quán
UI MUST responsive (mobile ≥ 360px), tiếng Việt là ngôn ngữ chính.
Trang tìm kiếm công thức SHOULD phản hồi < [1]s với [N] công thức.

## Ràng buộc kỹ thuật
- Stack: [ví dụ Next.js + Node/Express + PostgreSQL] — chốt cụ thể phiên bản
- Hosting: [...]

## Quy trình phát triển
- Branch: feature/<SCRUM-xx>-<mô-tả>; commit theo Conventional Commits
- Mỗi PR MUST có ≥ 1 người review, CI xanh
- Definition of Done: spec ✓, test pass ✓, review ✓, docs cập nhật ✓

## Governance
- Constitution có hiệu lực cao nhất; plan vi phạm MUST ghi lý do trong "Complexity Tracking"
- Sửa đổi cần cả nhóm đồng ý; tăng version theo semver
```

AI chuyển prompt trên thành tham số cho lệnh:

```
/speckit-constitution Tech stack: Java Spring Boot (backend), ReactJS (frontend), PostgreSQL
(database). Include the principles, technical constraints, development workflow, and governance
sections provided by the user (Spec-first with Jira SCRUM-xx, Test-First with >=70% coverage on
business logic, simple layered architecture, security & user data, consistent UX in
Vietnamese/responsive ≥360px, search <1s). Write in Vietnamese.
```

### Tóm tắt phản hồi của AI

- Viết `.specify/memory/constitution.md` bản **v1.0.0**, phê chuẩn ngày 2026-10-02, không còn placeholder của template.
- Giữ nguyên ý 5 nguyên tắc mình đưa, cụ thể hóa cho Spring Boot + React + PostgreSQL:
  - **II. Test-First:** JUnit 5 + Mockito cho Service, Testcontainers chạy PostgreSQL thật (không dùng H2), Vitest + React Testing Library cho frontend, JaCoCo đo coverage ≥ 70%.
  - **III. Kiến trúc:** Controller → Service → Repository, API chỉ trả DTO, package chia theo tính năng.
  - **IV. Bảo mật:** Spring Security + BCrypt, `@Valid`, secret và LLM API key để trong `.env`, LLM chỉ gọi từ backend.
  - **V. Trải nghiệm:** tìm kiếm < 1 giây (p95) với 10.000 công thức; kết quả AI phải kèm disclaimer như proposal.
- Ràng buộc kỹ thuật AI tự chọn: Java 21 LTS, Spring Boot 3.x, Maven, React 18 + Vite, PostgreSQL 16, Flyway, OpenAPI, Docker Compose.
- Để lại `TODO(HOSTING)` vì nhóm chưa chốt nơi triển khai.
- Thêm comment "Sync Impact Report" ở đầu file và gợi ý xóa trước khi commit.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| 5 nguyên tắc cốt lõi (I → V) | Dùng | Giữ đúng ý mình đưa trong prompt |
| Tech stack Spring Boot + React + PostgreSQL | Dùng | Đúng công nghệ nhóm đã chọn |
| Phiên bản và công cụ AI tự chọn (Java 21, Spring Boot 3.x, React 18, PostgreSQL 16, Flyway, Testcontainers...) | Dùng, cần nhóm xác nhận | Prompt chỉ chốt 3 công nghệ chính; các chi tiết này AI tự chọn |
| Con số 10.000 công thức cho mục tiêu tìm kiếm < 1 giây | Dùng tạm | AI giả định thay cho `[N]` trong prompt; cần nhóm xác nhận |
| `TODO(HOSTING)` | Giữ | Nhóm chưa chốt nơi triển khai |
| Comment "Sync Impact Report" ở đầu file | Giữ | Chưa xóa theo gợi ý của AI, vẫn còn trong bản trên `main` |
| Chuyển constitution ra thư mục gốc `/WikiCook` | **Bỏ** | Mình hỏi thêm; AI giải thích các lệnh Spec Kit chỉ đọc file ở `.specify/memory/`, chuyển hoặc copy sẽ làm lệch hai bản |

### Cách kiểm chứng

- Đọc lại toàn bộ constitution, đối chiếu với 5 nguyên tắc trong prompt và tech stack trong proposal.
- Kiểm tra dòng version: `Version: 1.0.0 | Ratified: 2026-10-02 | Last Amended: 2026-10-02`.
- Kiểm tra không còn placeholder dạng `[...]` của template, chỉ còn `TODO(HOSTING)` có chủ đích.
- Việc cần làm tiếp: nhóm xác nhận các phiên bản và con số AI tự chọn, chốt hosting, xóa Sync Impact Report.

---

## Mục 2: Viết spec đăng nhập và phân quyền bằng `/speckit-specify`

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-02 |
| Người thực hiện | Thành Dự |
| Nhóm tính năng liên quan | 01 – Tài khoản (spec `specs/user-authentication-roles`) |
| Công cụ AI | Claude Code (model Claude Opus 5.5), lệnh Spec Kit `/speckit-specify` |
| Mục đích | Sinh bản đặc tả cho tính năng đăng nhập, đăng ký và phân quyền theo vai trò |
| Nhánh Git | `feature/speckit-auth-spec` (commit `2061701`, merge vào `main` ở PR #7) |

### Prompt đã dùng

```
/speckit-specify Hãy mô tả tính năng đăng nhập : kết quả tôi mong muốn là user story, tiêu chí
chấp nhận và các tình huống biên. Một đặc tả tốt tôi mong muốn là có tóm tắt + USER STORY + tiêu
chí chấp nhận+ Yêu cầu phi chức năng và yêu  cầu chức năng, hãy tạo 1 folder /specs trước và
/specs/user-authentication-roles và viết nội dung dung đặc tả vào đó . hãy viết bằng tiếng anh
```

### Tóm tắt phản hồi của AI

- Tạo thư mục `specs/user-authentication-roles/` gồm `spec.md` và `checklists/requirements.md`, và file `.specify/feature.json` trỏ tới thư mục này.
- `spec.md` (tiếng Anh) gồm:
  - **Summary:** mục đích và phạm vi, ghi rõ phần có làm và phần không làm.
  - **5 user story** có độ ưu tiên, cách test độc lập, kịch bản chấp nhận dạng Given/When/Then: đăng nhập email/mật khẩu (P1), đăng ký và xác thực email (P1), phân quyền theo vai trò Guest/Member/Contributor/Moderator/Admin (P1), quên mật khẩu (P2), đăng nhập Google (P3).
  - **13 tình huống biên:** khóa tài khoản sau 5 lần sai, tài khoản bị khóa/ban, Admin cuối cùng không được tự hạ quyền, bấm submit hai lần, link chuyển hướng ra trang ngoài, phiên hết hạn khi đang nhập form...
  - **25 yêu cầu chức năng** kèm bảng ma trận phân quyền, **9 yêu cầu phi chức năng** (bảo mật, hiệu năng, giao diện từ 360px, khả năng tiếp cận, audit), Key Entities, **7 tiêu chí thành công** đo được.
- AI tự kiểm tra spec theo checklist chất lượng: đạt 16/16 mục ngay lần đầu.
- AI tự chọn mặc định ở các điểm sau và đề nghị nhóm xem lại:
  - hoãn đăng nhập Apple vì cần tài khoản Apple Developer trả phí (proposal có ghi tính năng này);
  - tài khoản chưa xác thực email vẫn đăng nhập được nhưng không được đăng nội dung;
  - khóa 15 phút sau 5 lần sai, "remember me" 30 ngày, link đặt lại mật khẩu hết hạn sau 30 phút;
  - hồ sơ ăn uống và avatar tách ra spec riêng.
- AI nhắc: spec chưa có mã Jira (constitution nguyên tắc I bắt buộc), và không tạo branch mới vì chưa bật git extension của Spec Kit.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| Cấu trúc Summary + User Story + tiêu chí chấp nhận + FR + NFR | Dùng | Đúng cấu trúc mình yêu cầu trong prompt |
| 5 user story và độ ưu tiên | Dùng | Email/mật khẩu và phân quyền làm P1 vì mọi tính năng cá nhân hóa khác đều cần biết người dùng là ai |
| Ma trận phân quyền 5 vai trò | Dùng | Khớp các vai trò trong proposal |
| Hoãn đăng nhập Apple | Dùng | Cần tài khoản trả phí; ghi rõ là ngoài phạm vi |
| Mặc định khóa 15 phút / 5 lần sai, remember me 30 ngày, link reset 30 phút | Dùng | Theo thông lệ, có thể đổi khi chạy `/speckit-clarify` hoặc `/speckit-plan` |
| Tách hồ sơ ăn uống và avatar sang spec khác | Dùng | Giữ spec này gọn trong phạm vi đăng nhập và phân quyền |
| Dòng `Feature Branch` để `SCRUM-xx` (TODO) | **Sửa** | Sau đó nhóm gán mã `SCRUM-27`, đổi trạng thái thành "Ready for Implementation (Sprint 02)" và sửa chính tả kiểu Mỹ (`personalized`, `authorized`...) |

### Cách kiểm chứng

- Đọc lại toàn bộ `spec.md`, đối chiếu với:
  - module tài khoản trong `docs/requirements/project-proposal.md` (đăng ký, đăng nhập email/Google/Apple, vai trò);
  - constitution nguyên tắc I (spec gắn mã Jira) và IV (mật khẩu hash, validate input phía server).
- Kiểm tra checklist `specs/user-authentication-roles/checklists/requirements.md`: đạt 16/16 mục, không còn `[NEEDS CLARIFICATION]`.
- Spec được Hưng (LqHung06) review và approve ở PR #7 trước khi merge vào `main`.
- Việc cần làm tiếp: chạy `/speckit-plan` cho tính năng này ở Sprint 02.

---

## Mục 3: Viết spec 030 (meal planner thủ công) bằng `/speckit-specify` + `/speckit-clarify`

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-08 |
| Người thực hiện | Thành Dự |
| Nhóm tính năng liên quan | 03 – Meal Planner (spec `specs/030-meal-planner`) |
| Công cụ AI | Claude Code (model Claude Opus 5.5), lệnh Spec Kit `/speckit-specify` và `/speckit-clarify` |
| Mục đích | Sinh bản đặc tả cho lịch thực đơn tuần thủ công theo phân công tuần 12/10 – 18/10 |
| Nhánh Git | `docs/w1-spec-design-assignment` (chưa commit) |

### Prompt đã dùng

**Lần 1: tạo spec**

Mình nhờ AI soạn prompt theo mẫu ở mục 6 của `docs/management/w1-spec-design-assignment.md`, đọc lại rồi chạy:

```
/speckit-specify SPECIFY_FEATURE_DIRECTORY=specs/030-meal-planner
Người dùng đã đăng nhập lập thực đơn tuần thủ công trên trang Meal Planner. Lịch hiển thị 7 ngày
(thứ Hai → Chủ nhật) × 3 bữa (sáng, trưa, tối); mỗi ô chứa một công thức đã PUBLISHED, hiện ảnh,
tên và thời gian nấu, bấm vào thì mở trang chi tiết công thức. Thêm món vào ô bằng hai cách: bấm
ô trống để mở hộp chọn công thức (tìm theo tên, lọc nhanh từ bộ sưu tập đã lưu), hoặc bấm "Thêm
vào meal plan" trên trang chi tiết công thức rồi chọn ngày và bữa. Mỗi món có khẩu phần riêng, mặc
định lấy số người trong gia đình ở hồ sơ. Đổi chỗ món bằng kéo-thả giữa các ô trên desktop; trên
mobile lịch hiện theo từng ngày, dùng menu "Chuyển sang..." thay cho kéo-thả. Đổi món, xóa món,
xóa cả ngày. Chuyển sang tuần trước/tuần sau; tuần chưa có kế hoạch hiện trạng thái rỗng kèm nút
"Tạo bằng AI" (mở spec 031). Nút "Tạo shopping list" gửi các món của tuần đang xem sang spec 040.
Công thức trong lịch bị gỡ hoặc ARCHIVED thì ô vẫn giữ tên món, đánh dấu "Không còn khả dụng" và
gợi ý đổi món. Mỗi thay đổi được lưu ngay lên server. Mất mạng: vẫn xem được lịch tuần đã tải gần
nhất, các thao tác sửa bị khóa kèm thông báo, có mạng lại thì tải lại. Thao tác thất bại thì trả
ô về trạng thái cũ và báo lỗi. Khách chưa đăng nhập bấm "Thêm vào meal plan" thì được đưa tới
trang đăng nhập rồi quay lại.
Không bao gồm: tạo thực đơn bằng AI (spec 031), gộp nguyên liệu và shopping list (spec 040), dinh
dưỡng theo ngày/tuần (spec 090), tìm kiếm nâng cao (spec 020).
Dữ liệu: meal_plans, meal_plan_items theo docs/analysis-and-design/database/README.md; định dạng ô
khớp mục 4.6 của docs/requirements/project-proposal.md để spec 031 đổ kết quả vào cùng lịch.
```

**Lần 2: làm rõ yêu cầu**

```
/speckit-clarify g. Dùng trên điện thoại và khi mất mạng (P3) mất mạng thì không xem được .
```

### Tóm tắt phản hồi của AI

- Tạo `specs/030-meal-planner/spec.md` (tiếng Anh) và `checklists/requirements.md`, cập nhật `.specify/feature.json` trỏ tới thư mục này.
- `spec.md` gồm:
  - **Summary** với phạm vi có làm / không làm, nói rõ đây là lịch dùng chung mà spec 031 đổ kết quả vào.
  - **7 user story:** điền món vào ô trống (P1), thêm từ trang chi tiết công thức (P1), đổi chỗ / thay / xóa món (P1), chuyển tuần và tuần rỗng (P2), gửi sang shopping list (P2), công thức không còn khả dụng (P2), dùng trên điện thoại và khi mất mạng (P3). Mỗi story có cách test độc lập và kịch bản Given/When/Then.
  - **13 tình huống biên:** hai thiết bị sửa cùng tuần, ô bị đổi ở nơi khác trước khi lưu, khẩu phần ngoài 1–20, cùng một món ở nhiều ô, tuần vắt qua năm mới, phiên hết hạn khi đang sửa...
  - **31 yêu cầu chức năng**, **6 yêu cầu phi chức năng**, Key Entities (Meal Plan, Meal Plan Item), **7 tiêu chí thành công** đo được.
- AI tự kiểm tra spec theo checklist chất lượng: đạt 16/16 mục ngay lần đầu, không có `[NEEDS CLARIFICATION]`.
- AI tự chọn mặc định và ghi vào mục Assumptions: mỗi ô một món, khẩu phần 1–20, tuần bắt đầu thứ Hai theo giờ Việt Nam, offline chỉ đọc và không xếp hàng thao tác sửa, kéo-thả chỉ trên desktop, không chuyển món sang tuần khác, hồ sơ không có số người thì lấy khẩu phần gốc của công thức.
- Ở bước `/speckit-clarify`, AI cập nhật spec theo yêu cầu mới:
  - mất mạng thì không hiện lịch, thay bằng thông báo cần kết nối mạng kèm nút "Thử lại"; không lưu dữ liệu meal plan trên máy để xem offline;
  - đang mở planner mà mất mạng thì lịch cũng bị thay bằng thông báo đó; có mạng lại thì tự tải lại tuần đang xem;
  - sửa User Story 7, FR-027 → FR-029, SC-006, mục Assumptions, và ghi quyết định vào mục Clarifications.

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| 7 user story và độ ưu tiên | Dùng | Bám đúng phạm vi spec 030 trong bảng phân công |
| Mỗi ô một công thức | Dùng | Khớp định dạng ô ở mục 4.6 của proposal để spec 031 dùng chung lịch |
| Offline vẫn xem được tuần đã tải gần nhất (mặc định AI chọn ở lần 1) | **Bỏ** | Mình quyết định lại ở `/speckit-clarify`: mất mạng thì không xem được |
| Không xếp hàng thao tác sửa khi offline | Dùng | Proposal mục 2.2 yêu cầu có mạng để đồng bộ; cho sửa khi offline phải xử lý xung đột |
| Kéo-thả trên ô đã có món thì đổi chỗ hai món | Dùng | Không làm mất món nào khi kéo nhầm |
| Món không còn khả dụng bị bỏ khỏi shopping list và báo số món bị bỏ | Dùng, cần thống nhất với D | Là điểm giao C ↔ D (spec 040) |
| Dòng `Feature Branch` để `SCRUM-xx` | Giữ, chờ gán mã | Chưa có mã Jira cho spec này |

### Cách kiểm chứng

- Đối chiếu spec với:
  - phạm vi spec 030 trong `docs/management/w1-spec-design-assignment.md` mục 1;
  - proposal mục 3.3 (lịch kéo-thả), 4.2 bước 9 (sửa kế hoạch AI), 4.3 (khẩu phần 1–20), 4.6 (định dạng ô);
  - các điểm giao ở mục 2 của bảng phân công: meal plan → shopping list (C ↔ D), công thức bị gỡ trong meal plan (A ↔ C ↔ E).
- Kiểm tra checklist `specs/030-meal-planner/checklists/requirements.md`: đạt 16/16, không còn `[NEEDS CLARIFICATION]`.
- Sau `/speckit-clarify`, tìm từ "offline" trong spec để chắc không còn câu nào nói offline vẫn xem được.
- Việc cần làm tiếp: D review spec qua PR (theo bảng review chéo), gán mã Jira, rồi chạy `/speckit-plan`.

---

## Mục 4: Viết spec 031 (AI Weekly Meal Planner) bằng `/speckit-specify`

| Trường | Nội dung |
|---|---|
| Ngày | 2026-10-08 |
| Người thực hiện | Thành Dự |
| Nhóm tính năng liên quan | 03 – Meal Planner, tính năng AI đặc biệt ở mục 4 của proposal (spec `specs/031-ai-weekly-meal-planner`) |
| Công cụ AI | Claude Code (model Claude Opus 5.5), lệnh Spec Kit `/speckit-specify` |
| Mục đích | Sinh bản đặc tả cho tính năng AI xếp thực đơn tuần, dùng chung lịch với spec 030 |
| Nhánh Git | `docs/w1-spec-design-assignment` (chưa commit) |

### Prompt đã dùng

**Lần 1: tạo spec**

Mình nhờ AI soạn prompt dựa trên ví dụ ở mục 6 của bảng phân công, mục 4 của proposal và các quyết định đã chốt ở spec 030, đọc lại rồi chạy:

```
/speckit-specify SPECIFY_FEATURE_DIRECTORY=specs/031-ai-weekly-meal-planner
Người dùng đã đăng nhập bấm "Tạo bằng AI" trên trang Meal Planner (spec 030) để AI xếp thực đơn
cho tuần đang xem. Trước khi tạo, hệ thống hiện hộp thoại ràng buộc điền sẵn từ hồ sơ: dị ứng,
nguyên liệu loại trừ, kiểu ăn, khẩu phần (số người trong gia đình), thời gian nấu tối đa mỗi món,
ẩm thực ưa thích; người dùng chọn số ngày (1–7, mặc định cả tuần) và các bữa (sáng/trưa/tối, mặc
định cả 3). Chỉnh ràng buộc ở đây chỉ áp dụng cho lần tạo này, không sửa hồ sơ. Hồ sơ chưa có thông
tin ăn uống thì vẫn tạo được nhưng hiện nhắc hoàn thiện hồ sơ. AI chỉ điền vào ô trống, giữ nguyên
món người dùng đã xếp. AI chỉ chọn trong các công thức đã PUBLISHED của WikiCook, không tự bịa món,
hạn chế lặp món trong tuần, chọn món hợp với bữa (món sáng cho bữa sáng), ưu tiên dùng lại nguyên
liệu giữa các món. Trong lúc tạo hiện trạng thái đang xử lý, quá 60 giây thì báo lỗi. Kết quả hiện
trên lịch dạng bản nháp, món do AI chọn có nhãn "AI"; món AI trả về không tồn tại hoặc vi phạm dị
ứng/loại trừ bị bỏ, ô đó để trống và đánh dấu "Chọn món" để người dùng tự chọn bằng hộp chọn của
spec 030. Với bản nháp, người dùng được đổi chỗ, thay, xóa món như spec 030; "Tạo lại" với ràng buộc
khác; "Hủy" bỏ bản nháp; "Xác nhận" lưu vào lịch và tạo shopping list (spec 040). Rời trang khi bản
nháp chưa xác nhận thì hỏi lại trước khi bỏ. Không đủ công thức phù hợp thì báo và gợi ý nới ràng
buộc (bớt thời gian nấu, bỏ ẩm thực ưa thích). Giới hạn 5 lần tạo/người/giờ, hiện số lượt còn lại
và thời điểm được tạo tiếp; lần lỗi do hệ thống không bị tính. AI lỗi hoặc quá thời gian thì báo
lỗi, lịch thủ công vẫn dùng bình thường. Luôn hiện cảnh báo "Thực đơn do AI tạo dựa trên hồ sơ của
bạn, hãy tự kiểm tra dị ứng và cân bằng dinh dưỡng". Công thức được tìm kiếm ngữ nghĩa khi
PUBLISHED, cập nhật khi sửa, bỏ khi ARCHIVED; gọi AI chỉ ở backend (constitution).
Không bao gồm: lịch và thao tác thủ công (spec 030), gộp nguyên liệu và shopping list (spec 040),
phân tích dinh dưỡng (spec 090), quản lý hồ sơ ăn uống (spec 011), AI tạo công thức mới.
Dữ liệu và quy tắc: mục 4 của docs/requirements/project-proposal.md (luồng 4.2, đầu vào 4.3, kết quả
4.6, xử lý lỗi 4.7); meal_plans, meal_plan_items, recipe_embeddings, ai_usage theo
docs/analysis-and-design/database/README.md.
```

**Lần 2: bổ sung gợi ý và xếp hạng theo bữa**

```
AI này tôi muốn recommend và khi mà người dùng tìm kiếm thì cũng phải ranking cho người dùng (ví dụ
buổi sáng là món ăn sáng thay vì là món cơm trưa hay chiều)
```

AI hỏi lại 2 câu, mình chọn: áp dụng ở **hộp chọn món của planner** (không sửa trang tìm kiếm chung của spec 020), và xếp hạng theo **bữa + hồ sơ**.

### Tóm tắt phản hồi của AI

- Tạo `specs/031-ai-weekly-meal-planner/spec.md` (tiếng Anh) và `checklists/requirements.md`, cập nhật `.specify/feature.json` trỏ tới thư mục này.
- `spec.md` gồm:
  - **Summary** với phạm vi có làm / không làm; AI chỉ điền ô trống trên lịch của spec 030, kết quả là bản nháp.
  - **7 user story:** tạo thực đơn từ hồ sơ (P1), xem lại / chỉnh / xác nhận bản nháp (P1), không bao giờ hiện món không tồn tại hoặc vi phạm dị ứng (P1), không đủ công thức phù hợp (P2), giới hạn 5 lần/giờ (P2), AI lỗi (P2), giữ dữ liệu công thức cho AI luôn mới (P3).
  - **12 tình huống biên:** tuần đã đủ món, tuần đã qua, tối Chủ nhật không còn bữa nào, bản nháp mở ở hai tab, mất mạng khi đang tạo, công thức bị gỡ giữa lúc nháp và lúc xác nhận...
  - **32 yêu cầu chức năng**, **6 yêu cầu phi chức năng** (hiệu năng, chỉ gọi AI từ server, không gửi tên/email cho dịch vụ AI, 0 món vi phạm dị ứng), Key Entities, **8 tiêu chí thành công** đo được.
- AI tự kiểm tra spec theo checklist chất lượng: đạt 16/16 mục ngay lần đầu, không có `[NEEDS CLARIFICATION]`.
- AI tự chọn mặc định và ghi vào mục Assumptions: chọn từng ngày trong tuần thay cho "số ngày", khẩu phần mặc định 2 khi hồ sơ trống, bản nháp chỉ sống trong trang đang mở, "không đủ công thức" khi số công thức phù hợp ít hơn số ô cần điền, lần tạo bị hủy / lỗi / không đủ công thức không bị tính, dữ liệu công thức cho AI cập nhật trong vòng 24 giờ.

- Ở lần 2, AI cập nhật hai spec và ghi quyết định vào mục Clarifications của cả hai:
  - **Spec 030:** hộp chọn món mở từ một ô xếp kết quả theo bữa của ô trước (ô bữa sáng thì món sáng lên đầu), rồi tới ẩm thực ưa thích và thời gian nấu; món vi phạm dị ứng / loại trừ / kiểu ăn xuống cuối kèm nhãn cảnh báo (FR-007a → FR-007c, SC-007).
  - **Spec 031:** thêm User Story 7 "Suggestions for one slot" (P2): mục "Gợi ý cho bạn" trong hộp chọn món hiện tối đa 5 món hợp bữa và hồ sơ, không trùng món đã có trong tuần, có lý do ngắn cho từng món, không tính vào giới hạn 5 lần/giờ, quá 3 giây hoặc AI lỗi thì ẩn đi (FR-033 → FR-038, SC-008).

### Phần đã sử dụng / chỉnh sửa / bỏ

| Phần | Quyết định | Lý do |
|---|---|---|
| 7 user story và độ ưu tiên | Dùng | Bám luồng 4.2 và xử lý lỗi 4.7 của proposal |
| Gợi ý từng ô và xếp hạng theo bữa trong hộp chọn món | Dùng (thêm ở lần 2) | Mình muốn AI gợi ý cả khi tìm món cho từng ô, không chỉ khi tạo cả tuần |
| Xếp hạng trên trang tìm kiếm chung (spec 020) | **Bỏ** | Chỉ áp dụng trong planner, không đụng spec của Duy |
| Công thức không có thông tin bữa thì tính là món trưa/tối | Dùng, cần thống nhất với B | AI tự chọn; phụ thuộc spec 060 |
| Dị ứng, loại trừ, kiểu ăn, thời gian nấu là luật cứng; ẩm thực, hợp bữa, đa dạng, dùng lại nguyên liệu là ưu tiên | Dùng | Khớp mục 4.1.1 và 4.4 của proposal |
| AI chỉ điền ô trống, kết quả là bản nháp phải xác nhận | Dùng | Theo prompt; giữ món người dùng đã xếp |
| Chọn từng ngày thay cho "số ngày 1–7" | Dùng, cần xem lại | AI tự diễn giải để không xếp món cho ngày đã qua của tuần hiện tại |
| Khẩu phần mặc định 2 khi hồ sơ trống | Dùng, cần xem lại | AI tự chọn; spec 030 lấy khẩu phần gốc của công thức |
| Thông tin "món hợp bữa nào" trên công thức | Dùng, cần thống nhất với B | Phụ thuộc spec 060 |
| Dòng `Feature Branch` để `SCRUM-xx` | Giữ, chờ gán mã | Chưa có mã Jira cho spec này |

### Cách kiểm chứng

- Đối chiếu spec với:
  - phạm vi spec 031 và ghi chú "Về spec 031" trong `docs/management/w1-spec-design-assignment.md`;
  - proposal mục 4.2 (luồng), 4.3 (đầu vào), 4.6 (kết quả), 4.7 (xử lý lỗi), 4.8 (giới hạn 5 lần/giờ), 4.9 (hạn chế);
  - spec 030 (lịch, hộp chọn món, hành vi khi mất mạng) để không mâu thuẫn;
  - constitution: chỉ gọi AI từ backend, kết quả AI phải có cảnh báo.
- Kiểm tra checklist `specs/031-ai-weekly-meal-planner/checklists/requirements.md`: đạt 16/16, không còn `[NEEDS CLARIFICATION]`.
- Việc cần làm tiếp: D review spec qua PR (theo bảng review chéo), thống nhất các điểm giao với A, B, D, chạy `/speckit-clarify` cho các mặc định ở trên, gán mã Jira.
