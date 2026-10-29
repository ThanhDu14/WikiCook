# Phân công tuần 5 (26/10 – 01/11/2026, tuần 1 của Sprint 3): use case, domain model và walking skeleton `v0.5`

*Soạn bởi: Nguyễn Thành Dự (ThanhDu14) | Review: _(chờ PM Lê Quốc Hưng)_ | Chỉnh sửa: _(chờ)_*

Sprint 3 (26/10 – 07/11/2026, PA3; môn học kết thúc ở tuần 10 nên mỗi sprint chỉ còn 2 tuần) phải ra: đặc tả use case US-01 – US-07, domain model, wireframe + navigation map và **`Build v0.5`** (xem [project plan](project-plan.md) mục 4.3.3 và 4.4.2). Tuần này là tuần đầu trong 2 tuần của sprint, gồm 3 việc:

1. **Use case:** mỗi người viết use case cho các module mình đã viết spec ở tuần 12/10 (giữ nguyên người phụ trách để không phải đọc lại spec của người khác).
2. **Spec Kit:** các spec mới có `spec.md` chạy tiếp `/speckit-plan` để có `plan.md`, `data-model.md`, `contracts/`. Spec 030, 031, 090 đã có đủ, chỉ cần đối chiếu lại với [full-schema.md](../analysis-and-design/database/full-schema.md).
3. **Walking skeleton `v0.5`:** dựng khung backend, frontend, Docker Compose và CI. Đây là đường găng của Sprint 3: `v0.5` phải tag trong tuần 6, nên tuần này phải xong phần khung.

Thư mục code theo quyết định ngày 18/10: backend Maven ở `src/backend/` (package gốc `com.wikicook`, mỗi tính năng một package), frontend React 18 + TypeScript + Vite ở `src/frontend/`.

## Thành viên

Giữ ký hiệu như tuần 12/10:

| Ký hiệu | Thành viên | Vai trò |
|---|---|---|
| **A** | Mai Văn Hiển (MaiHien3507) | AI Architect & Technical Lead |
| **B** | Nguyễn Đức Duy (DucDuyNguyen15-IT) | Product Owner & Requirements Lead |
| **C** | Nguyễn Thành Dự (ThanhDu14) | DevOps Lead |
| **D** | Lê Quốc Hưng (LqHung06) | Project Manager & Scrum Master |
| **E** | Trần Nguyễn Công Chung (itzchugnn) | QA Lead |

---

## 1. Use case và Spec Kit

Use case lưu tại `docs/analysis-and-design/use-cases/UC-xx-<ten>.md`. B tạo template chung (3 mức **Brief**, **Casual**, **Fully Dressed**) trước **T4 28/10**. Mỗi user story US-01 – US-07 có ít nhất một use case Fully Dressed; các use case phụ viết Brief hoặc Casual. Mỗi use case ghi rõ spec nguồn và mã Jira.

| Người | User story / use case | `/speckit-plan` cần chạy | Việc chung |
|---|---|---|---|
| **A** | US-01 Đăng ký, đăng nhập, khởi tạo dietary profile; use case phụ: bộ sưu tập | 010, 011, 080 | Khung backend (mục 2) |
| **B** | US-02 Tìm công thức theo nguyên liệu có sẵn; US-05 Đăng công thức | 020, 060 | **Template use case + sơ đồ use case tổng** (actor: Guest, Member, Contributor, Moderator, Administrator, LLM service) |
| **C** | US-03 AI lập thực đơn tuần; use case phụ: planner thủ công, phân tích dinh dưỡng | Đối chiếu 030, 031, 090 với `full-schema.md` | Docker Compose + CI (mục 2) |
| **D** | US-04 Shopping list gộp nguyên liệu; US-06 Chế độ nấu rảnh tay | 040, 050 | Sprint 3 Planning, tạo task Jira; **ghép domain model tổng**; navigation map |
| **E** | US-07 Duyệt công thức và quản lý danh mục; use case phụ: đánh giá, Cooksnap | 070, 100 | Khung frontend (mục 2); kế hoạch kiểm thử `v0.5` |

**Domain model** (sơ đồ lớp khái niệm, Mermaid `classDiagram`) lưu tại `docs/analysis-and-design/domain-model.md`. Mỗi người vẽ phần lớp của module mình theo đúng nhóm bảng ở `full-schema.md` mục 3.2 – 3.6, gửi D trước **T6 30/10**. D ghép bản tổng trong ngày T7 31/10.

---

## 2. Walking skeleton `v0.5`

Tiêu chí chấp nhận lấy từ project plan mục 4.4.2: clone mới, chạy `docker compose up` thì cả 4 container `healthy` trong ≤ 5 phút; `GET /actuator/health` trả `UP` có cả `db` và `redis`; SPA hiển thị trạng thái health; Flyway `V1` chạy không lỗi; CI xanh trên `main`.

Tuần này làm xong phần khung, tuần sau nối và kiểm thử.

| Người | Việc | Kết quả cuối tuần |
|---|---|---|
| **A** | `src/backend/`: Spring Boot 3 / Java 21, các package theo tầng (`controller`, `service`, `repository`, `domain`), Actuator health, springdoc OpenAPI stub, Flyway `V101__users_auth.sql` (tuần này chỉ bảng `users`, khớp `full-schema.md` mục 4.1 và thứ tự migration ở mục 7) | `mvn verify` chạy được ở local, có 1 test kiểm tra context load |
| **C** | `V001__extensions.sql` (`unaccent`, `pg_trgm`, `vector`, hàm `f_unaccent`); `docker-compose.yml` ở gốc repo: `postgres` (image `pgvector/pgvector:pg16`), `redis:7`, `backend`, `frontend`; health check cho từng container; Dockerfile cho backend và frontend; `.env.example`. GitHub Actions: build + test backend, build + lint frontend, build Docker image trên mỗi PR | Compose chạy được với code khung của A và E; workflow CI chạy trên PR |
| **E** | `src/frontend/`: Vite + React 18 + TypeScript, React Router với các route rỗng theo navigation map, API client gọi `/api/v1` và `/actuator/health`, trang hiển thị trạng thái health | `npm run build` và `npm run lint` không lỗi |
| **E** | Kế hoạch kiểm thử `v0.5`: checklist từ tiêu chí chấp nhận ở trên, các bước kiểm tra từ clone mới | File `docs/test/v0.5-test-plan.md` |

> **Backend là Maven multi-module (chốt 27/10):** `src/backend/pom.xml` (parent) + `wikicook-app` (ứng dụng Spring Boot, A dựng) + `wikicook-ai` (package `com.wikicook.ai`: LLM, embedding, truy hồi RAG, importer dữ liệu cào; C dựng). `wikicook-ai` không phụ thuộc `wikicook-app`, build và test độc lập; `wikicook-app` phụ thuộc `wikicook-ai`. A dựng khung theo cấu trúc này ngay từ đầu.

> **Thứ tự phụ thuộc:** A và E đẩy khung lên nhánh trước **T5 29/10** để C viết Dockerfile và CI. C dựng sẵn container `postgres` và `redis` từ T3 để A thử Flyway ngay.

---

## 3. Lịch làm việc

| Hạn | Việc | A | B | C | D | E |
|---|---|---|---|---|---|---|
| **T3 27/10** | D: Sprint 3 Planning, tạo task Jira một người một task; mọi người đọc lại spec của mình | Khung backend | Template use case | Compose `postgres` + `redis` | Planning + Jira | Khung frontend |
| **T4 28/10** (standup 1) | B gửi template + sơ đồ use case tổng; báo tiến độ khung | ✔ | ✔ template | ✔ | ✔ | ✔ |
| **T5 29/10** | Use case Fully Dressed bản đầu; A và E đẩy khung lên nhánh | US-01 + đẩy khung | US-02, US-05 | US-03 | US-04, US-06 | US-07 + đẩy khung |
| **T6 30/10** | `/speckit-plan` xong; gửi phần domain model cho D; C ghép Dockerfile + CI | 010, 011, 080 | 020, 060 | Đối chiếu 030/031/090 + CI | 040, 050 | 070, 100 |
| **T7 31/10** (standup 2) | Use case phụ (Brief/Casual); D ghép domain model + navigation map; E viết kế hoạch kiểm thử; mở PR | ✔ | ✔ | ✔ | ✔ domain model | ✔ test plan |
| **CN 01/11** (họp cuối tuần) | Thử `docker compose up` từ clone mới; duyệt domain model và sơ đồ use case; review chéo PR xong; cập nhật Jira | ✔ | ✔ | ✔ | ✔ | ✔ |

Các tuần tiếp theo xem [kế hoạch tuần 6 – 10](w6-w10-weekly-plan.md).

---

## 4. Review chéo

Giữ cặp review của tuần 12/10 cho tài liệu, thêm cặp review cho code:

| Người viết | Review use case | Review `plan.md` / domain model | Review code khung |
|---|---|---|---|
| A | B | E | C (backend có chạy trong Docker không) |
| B | C | D | — |
| C | D | B | A (Compose, CI) |
| D | E | C | — |
| E | A | B | A (API client, cấu trúc frontend) |

**Khi review use case**, kiểm tra:

- Có actor chính, tiền điều kiện, hậu điều kiện, luồng chính và luồng thay thế/ngoại lệ.
- Khớp user story và tiêu chí chấp nhận trong `spec.md`; không thêm tính năng ngoài spec.
- Có luồng lỗi: mất mạng, không có quyền, LLM lỗi (US-03).

**Khi review code khung**, kiểm tra:

- Đúng thư mục `src/backend/`, `src/frontend/` và package `com.wikicook`.
- Không commit `.env` hay khóa API; chỉ có `.env.example`.
- CI chạy được trên PR và fail khi test fail.
