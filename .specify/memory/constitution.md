<!--
Sync Impact Report
==================
Version change: (template, chưa phê chuẩn) → 1.0.0
Modified principles (placeholder → tên mới):
  - [PRINCIPLE_1_NAME] → I. Spec trước, code sau
  - [PRINCIPLE_2_NAME] → II. Test-First (KHÔNG THƯƠNG LƯỢNG)
  - [PRINCIPLE_3_NAME] → III. Kiến trúc phân lớp đơn giản
  - [PRINCIPLE_4_NAME] → IV. Bảo mật & dữ liệu người dùng
  - [PRINCIPLE_5_NAME] → V. Trải nghiệm nhất quán
Added sections:
  - Ràng buộc kỹ thuật (thay [SECTION_2_NAME])
  - Quy trình phát triển & Quality Gates (thay [SECTION_3_NAME])
  - Governance (điền đầy đủ)
Removed sections: không có
Templates requiring updates:
  - .specify/templates/plan-template.md ✅ không cần sửa (mục "Constitution Check" đọc file này lúc chạy)
  - .specify/templates/spec-template.md ✅ không cần sửa
  - .specify/templates/tasks-template.md ✅ không cần sửa (Principle II yêu cầu task test đứng trước task code)
Follow-up TODOs:
  - TODO(HOSTING): nhóm chưa chốt nơi triển khai (VD: Render, Railway, VPS + Docker).
  - Ngưỡng hiệu năng tìm kiếm giả định quy mô 10.000 công thức — nhóm xác nhận lại.
-->

# WikiCook Constitution

## Core Principles

### I. Spec trước, code sau

- Mọi tính năng MUST có `spec.md` (tạo bằng `/speckit-specify`) được nhóm duyệt trước khi lập
  plan và viết code.
- Mỗi user story MUST gắn mã Jira (`SCRUM-xx`); mã này MUST xuất hiện trong tên branch,
  commit message và tiêu đề PR.
- Thay đổi yêu cầu MUST được cập nhật vào `spec.md` trước, sau đó mới sửa plan/tasks/code.

**Rationale**: Đồ án được chấm theo quy trình Công nghệ Phần mềm; cần truy vết được
yêu cầu → thiết kế → code → test.

### II. Test-First (KHÔNG THƯƠNG LƯỢNG)

- Test MUST được viết và chạy FAIL trước khi viết code hiện thực (Red → Green → Refactor).
- Backend: unit test bằng JUnit 5 + Mockito cho tầng Service; integration test cho
  Controller/Repository bằng `@SpringBootTest`/`@DataJpaTest` với Testcontainers (PostgreSQL thật,
  không dùng H2 thay thế).
- Frontend: component test bằng Vitest + React Testing Library cho các luồng chính.
- Coverage MUST ≥ 70% (đo bằng JaCoCo) cho tầng business logic (`service` package);
  build MUST fail nếu dưới ngưỡng.
- Mỗi acceptance scenario trong `spec.md` MUST có ít nhất một test tương ứng.

**Rationale**: Đảm bảo mỗi yêu cầu có bằng chứng kiểm thử, phục vụ trực tiếp `docs/test/`.

### III. Kiến trúc phân lớp đơn giản

- Luồng bắt buộc: React (UI) ↔ REST API (`@RestController`) ↔ Service ↔ Repository
  (Spring Data JPA) ↔ PostgreSQL.
- Controller MUST NOT truy cập Repository trực tiếp; business logic MUST nằm ở Service.
- API MUST trao đổi bằng DTO; Entity JPA MUST NOT được trả thẳng ra client.
- Backend tổ chức package theo tính năng (VD: `recipe`, `user`, `mealplan`), mỗi tính năng
  có các lớp `controller`/`service`/`repository`/`dto`.
- Không thêm thư viện, framework hay design pattern mới khi chưa ghi lý do trong `plan.md`
  (mục Complexity Tracking).

**Rationale**: Nhóm sinh viên cần code dễ đọc, dễ chia việc và dễ review (YAGNI).

### IV. Bảo mật & dữ liệu người dùng

- Xác thực/phân quyền MUST dùng Spring Security; mật khẩu MUST được hash bằng BCrypt.
- Secret (DB password, JWT secret, OAuth client secret, LLM API key) MUST nằm trong biến môi
  trường / file `.env`, MUST NOT được commit; repo chỉ chứa `.env.example`.
- Mọi input người dùng (công thức, bình luận, đánh giá, hồ sơ) MUST được validate phía server
  bằng Bean Validation (`@Valid`), không chỉ dựa vào validate ở frontend.
- Truy vấn DB MUST dùng JPA/tham số hóa; MUST NOT nối chuỗi SQL từ input người dùng.
- Nội dung do người dùng tạo MUST được escape khi hiển thị (không dùng
  `dangerouslySetInnerHTML` với dữ liệu chưa sanitize).
- Lỗi trả về client MUST NOT lộ stack trace hay thông tin nội bộ.

**Rationale**: WikiCook lưu tài khoản, hồ sơ ăn uống/dị ứng và nội dung cộng đồng; rò rỉ hoặc
tấn công injection gây hại trực tiếp cho người dùng.

### V. Trải nghiệm nhất quán

- UI MUST responsive, hiển thị đúng từ chiều rộng 360px (mobile) trở lên.
- Tiếng Việt là ngôn ngữ chính của giao diện; chuỗi hiển thị SHOULD tập trung một chỗ để
  dễ bổ sung ngôn ngữ khác sau này.
- Các component dùng chung (button, form, card công thức, thông báo lỗi) MUST được tái sử dụng
  thay vì viết lại ở từng trang.
- API tìm kiếm công thức SHOULD phản hồi < 1 giây (p95) với quy mô 10.000 công thức; danh sách
  MUST phân trang.
- Kết quả do AI sinh ra (dinh dưỡng, gợi ý) MUST hiển thị kèm disclaimer theo proposal.

**Rationale**: Người dùng chính là người nấu ăn bận rộn, thường dùng điện thoại trong bếp;
giao diện phải nhanh, quen thuộc và dễ thao tác.

## Ràng buộc kỹ thuật

- **Backend**: Java 21 (LTS), Spring Boot 3.x, Spring Web, Spring Data JPA, Spring Security,
  Bean Validation; build bằng Maven.
- **Frontend**: ReactJS 18+ với Vite; gọi API qua một lớp client dùng chung.
- **Database**: PostgreSQL 16; schema MUST được quản lý bằng migration (Flyway), MUST NOT dùng
  `ddl-auto=update` ở môi trường triển khai.
- **API**: RESTful, JSON, tiền tố `/api/v1`; tài liệu hóa bằng OpenAPI (springdoc).
- **AI/LLM**: gọi LLM API từ backend (không gọi trực tiếp từ frontend để tránh lộ API key).
- **Môi trường local**: MUST chạy được bằng Docker Compose (PostgreSQL + backend + frontend).
- **Hosting**: TODO(HOSTING): nhóm chưa chốt nền tảng triển khai.

## Quy trình phát triển & Quality Gates

- **Quy trình SDD**: `/speckit-specify` → `/speckit-clarify` (nếu cần) → `/speckit-plan` →
  `/speckit-tasks` → `/speckit-analyze` → `/speckit-implement`.
- **Branch**: `feature/SCRUM-xx-<mo-ta-ngan>`; MUST NOT commit trực tiếp lên `main`.
- **Commit**: theo Conventional Commits, kèm mã Jira (VD: `feat(recipe): add search API (SCRUM-12)`).
- **Pull Request**: MUST có ≥ 1 thành viên khác review và CI xanh trước khi merge.
- **Quality gates (CI)** MUST pass:
  - Backend: `mvn verify` (build, test, JaCoCo ≥ 70% cho service).
  - Frontend: lint (ESLint), test (Vitest) và `vite build`.
- **Definition of Done**: spec được duyệt ✓, test viết trước và pass ✓, review ✓,
  tài liệu liên quan trong `docs/` được cập nhật ✓.

## Governance

- Constitution này có hiệu lực cao nhất, ưu tiên hơn mọi quy ước khác của dự án.
- Mọi `plan.md` MUST qua mục "Constitution Check"; vi phạm nào cũng MUST được ghi lý do và
  phương án đơn giản hơn đã bị loại trong mục "Complexity Tracking".
- Reviewer MUST kiểm tra PR tuân thủ các nguyên tắc trên.
- Sửa đổi constitution MUST được cả nhóm đồng ý (qua PR có đủ thành viên approve) và ghi
  lại trong Sync Impact Report.
- Versioning theo semver: MAJOR khi bỏ/định nghĩa lại nguyên tắc; MINOR khi thêm nguyên tắc
  hoặc mở rộng đáng kể; PATCH khi sửa câu chữ, làm rõ.

**Version**: 1.0.0 | **Ratified**: 2026-10-02 | **Last Amended**: 2026-10-02
