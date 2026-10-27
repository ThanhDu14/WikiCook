# Kế hoạch tuần 6 – 10 (02/11 – 06/12/2026): hoàn thành Sprint 3, 4, 5

*Soạn bởi: Nguyễn Thành Dự (ThanhDu14) | Review: _(chờ PM Lê Quốc Hưng)_ | Chỉnh sửa: _(chờ)_*

Môn học kết thúc ở **tuần 10** (tuần 1 bắt đầu 28/09). Sau [tuần 5](w5-sprint03-assignment.md) chỉ còn 5 tuần, nên lịch trong [project plan](project-plan.md) mục 4.1.2 (Sprint 4 đến 04/12, Sprint 5 đến 25/12) không còn đúng. Kế hoạch này theo nhịp **2 tuần một sprint, một PA**, giống PA1 (nộp tuần 2) và PA2 (nộp tuần 4):

| Tuần | Ngày | Sprint | Mục tiêu tuần | Mốc |
|---|---|---|---|---|
| **5** | 26/10 – 01/11 | Sprint 3 | Use case, domain model, code khung *(file tuần 5)* | — |
| **6** | 02/11 – 08/11 | Sprint 3 | Wireframe, navigation map; nối các tầng; tag `v0.5.0` | **Nộp PA3** – T7 07/11 *(dự kiến)* |
| **7** | 09/11 – 15/11 | Sprint 4 | Toàn bộ schema; API và giao diện F-01 – F-04, F-06 cơ bản | — |
| **8** | 16/11 – 22/11 | Sprint 4 | AI planner + RAG, test, tag `v0.8.0-beta.1`; Architecture Document | **Nộp PA4** – T7 21/11 *(dự kiến)* |
| **9** | 23/11 – 29/11 | Sprint 5 | F-05 – F-10 còn lại; test E2E | — |
| **10** | 30/11 – 06/12 | Sprint 5 | Freeze, kiểm thử hồi quy, 2 lần chạy thử demo, tag `v1.0.0-rc.1` | **Nộp PA5 + demo** *(theo lịch lớp)* |

> **Hạn nộp PA3 – PA5 đang là dự kiến** theo nhịp 2 tuần. D xác nhận lại với giảng viên và sửa bảng này. Khi đã chốt, D cập nhật project plan mục 4.1.2, 4.1.3, 4.4.2 theo lịch mới.

## Thu hẹp phạm vi

5 tuần không đủ để làm hết 10 tính năng với đầy đủ chi tiết như trong spec. Đề xuất dưới đây chia việc thành **bắt buộc cho demo** và **làm nếu còn thời gian**. B (PO) chốt danh sách trong buổi Sprint 4 Planning (CN 08/11) và ghi vào mục *Không bao gồm* của spec tương ứng.

| Tính năng | Bắt buộc cho demo | Làm nếu còn thời gian |
|---|---|---|
| F-01 Tài khoản (010, 011) | Đăng ký, đăng nhập email/mật khẩu, JWT, 4 vai trò, dietary profile | Đăng nhập Google, xác minh email, khóa tài khoản |
| F-02 Tìm kiếm (020) | Full-text có/không dấu, lọc thời gian nấu, ẩm thực, dị ứng | Lọc calo, sắp xếp nâng cao |
| F-03 AI Meal Planner (030, 031) | Planner thủ công; AI tạo 7 ngày × 3 bữa qua RAG; báo lỗi khi LLM lỗi; giới hạn 5 lần/giờ | Kéo-thả, dùng offline |
| F-04 Shopping list (040) | Tạo từ công thức và từ meal plan, gộp nguyên liệu, nhóm theo quầy, tích chọn | Thêm món tự nhập |
| F-05 Chế độ nấu (050) | Toàn màn hình, nút lớn, nhiều đồng hồ song song, giữ màn hình sáng | Điều khiển bằng giọng nói |
| F-06 Đăng công thức (060) | Wizard, lưu nháp, gửi duyệt, ảnh | Link YouTube/TikTok, sửa khi bị yêu cầu chỉnh |
| F-07 Đánh giá + Cooksnap (070) | Chấm sao, bình luận, ảnh Cooksnap | Mẹo thay thế nguyên liệu, báo cáo đánh giá |
| F-08 Bộ sưu tập (080) | Lưu công thức, tạo/xóa bộ sưu tập | Đổi tên, sắp xếp |
| F-09 Dinh dưỡng (090) | LLM ước tính calo và macro khi công thức `PUBLISHED`, nhãn "do AI ước tính" | Chạy lại khi sửa công thức |
| F-10 Admin (100) | Hàng đợi duyệt, đình chỉ người dùng, CRUD danh mục | Thống kê, thao tác hàng loạt, xử lý báo cáo |

Cổng nghiệm thu của `v0.8` và `v1.0` (project plan mục 4.4.2) giữ nguyên các ngưỡng đo, nhưng chỉ áp dụng cho phần **bắt buộc**.

## Thành viên và phạm vi code

Ai viết spec thì code luôn tính năng đó, cả backend lẫn frontend, và tự sửa lỗi S1/S2 trong module của mình (project plan mục 4.4.3).

| Ký hiệu | Thành viên | Module (spec) | Migration (`full-schema.md` mục 7) | Vai trò trong build |
|---|---|---|---|---|
| **A** | Mai Văn Hiển | 010, 011, 080 | `V010` (giữ file chung), `V101`, `V210`, `V502` | Integration Owner; nghiệm thu kỹ thuật `v0.5` |
| **B** | Nguyễn Đức Duy | 020, 060 | `V201`, `V202`, `V203` | PO; nghiệm thu chức năng `v0.8`, `v1.0` |
| **C** | Nguyễn Thành Dự | 030, 031, 090 | `V001`, `V301`, `V302` | Release Owner (tag, đóng gói, release notes) |
| **D** | Lê Quốc Hưng | 040, 050 | `V401` | PM; nghiệm thu cuối; báo cáo sprint |
| **E** | Trần Nguyễn Công Chung | 070, 100 | `V501`, `V503` | Test Owner (kế hoạch và báo cáo kiểm thử) |

**Họp cố định:** CN cuối sprint là Review + Retrospective + Planning sprint sau; T4 standup 1; T7 standup 2 (tuần không nộp bài) hoặc CN review (tuần nộp bài). D ghi biên bản vào báo cáo sprint.

---

## Tuần 6 · 02/11 – 08/11: tag `v0.5.0` và nộp PA3

Wireframe vẽ trên canvas **wiki-screens** (mobile 390×844 trước, mỗi spec một trang, đủ 4 trạng thái: đang tải, rỗng, lỗi, có dữ liệu). Spec đã có trang trên canvas thì chỉ bổ sung trạng thái còn thiếu. Chỉ vẽ màn hình của phần **bắt buộc**.

| Người | Wireframe | Code / Spec Kit / nộp bài |
|---|---|---|
| **A** | Đăng ký, đăng nhập, hồ sơ + dietary profile, bộ sưu tập | Backend đọc DB/Redis từ biến môi trường, Actuator có `db` + `redis`; `/speckit-tasks` 010, 011, 080; nghiệm thu kỹ thuật `v0.5` |
| **B** | Trang chủ, tìm kiếm + bộ lọc, **trang chi tiết công thức (khung chung, ghép phần của mọi người)**, wizard | `/speckit-tasks` 020, 060; rà use case khớp wireframe; chốt câu hỏi #5, #6 ở `full-schema.md` mục 8 |
| **C** | Bổ sung trang 030, 031; khối dinh dưỡng trên trang chi tiết | CI xanh trên `main`; chốt câu hỏi #7 (mô hình embedding, số chiều vector); `CHANGELOG.md`, tag `v0.5.0`, xuất PDF, đóng gói ZIP PA3 |
| **D** | Shopping list, chế độ nấu, màn hình kết thúc nấu; **navigation map** | `/speckit-tasks` 040, 050; báo cáo Sprint 3; nghiệm thu cuối; Sprint 4 Planning |
| **E** | Khối đánh giá + Cooksnap, admin (hàng đợi duyệt, người dùng, danh mục) | `/speckit-tasks` 070, 100; route theo navigation map, trang health; chạy kế hoạch kiểm thử `v0.5` → `docs/test/v0.5-test-report.md`; QA tài liệu PA3 |

| Hạn | Việc |
|---|---|
| **T2 02/11** | D gửi navigation map bản đầu; mọi người chạy `/speckit-tasks` |
| **T3 03/11** | FE ↔ BE ↔ PostgreSQL ↔ Redis chạy được trong Compose; **freeze code `v0.5`** |
| **T4 04/11** (standup 1) | E chạy kiểm thử `v0.5` từ clone mới, báo lỗi lên Jira; wireframe bản đầu |
| **T5 05/11** | Sửa xong lỗi S1/S2; A nghiệm thu kỹ thuật, D nghiệm thu cuối; C tag `v0.5.0`; B ghép trang chi tiết công thức |
| **T6 06/11** | Wireframe hoàn chỉnh; C xuất PDF và đóng gói; E QA bản đóng gói |
| **T7 07/11** | **Nộp PA3** |
| **CN 08/11** | Sprint 3 Review & Retrospective; Sprint 4 Planning: B chốt phạm vi bắt buộc, D tạo task Jira |

---

## Tuần 7 · 09/11 – 15/11: schema đầy đủ, API và giao diện lõi

Migration chạy theo đúng thứ tự ở `full-schema.md` mục 7. **B đẩy `V201`, `V202` trước T2 09/11** (làm trong cuối tuần 6) vì gần như mọi bảng khác có FK tới `recipes` và `ingredients`.

| Người | Migration | Backend (`/api/v1`) | Frontend |
|---|---|---|---|
| **A** | `V010`, `V101` (đủ bảng), `V210` | F-01: đăng ký, đăng nhập, JWT/BCrypt, **RBAC 4 vai trò** (Spring Security), dietary profile | Đăng ký, đăng nhập, hồ sơ; lưu JWT, chặn route theo vai trò |
| **B** | `V201`, `V202`, `V203` | F-06 cơ bản: tạo/lưu nháp/gửi duyệt; F-02: full-text có/không dấu + bộ lọc + lọc dị ứng | Trang chủ, tìm kiếm, trang chi tiết công thức, wizard |
| **C** | `V301`, `V302` | Planner thủ công (030); pipeline embedding khi công thức `PUBLISHED` (cùng A), top-k qua `pgvector`; đếm lượt AI trên Redis | Lịch meal planner tuần |
| **D** | `V401` | F-04: shopping list từ công thức, gộp nguyên liệu, quy đổi đơn vị trong code (câu hỏi #8) | Shopping list (nhóm theo quầy, tích chọn) |
| **E** | — | Testcontainers, JaCoCo trong CI, **test ma trận phân quyền**; API duyệt cơ bản (`PENDING → PUBLISHED / REJECTED`) để công thức xuất bản được | — |
| **C** | — | **Importer dữ liệu cào** (`wikicook-ai`): nạp `tools/angingon-scraper/data/recipes.jsonl` vào `recipes`, tạo embedding; B duyệt ánh xạ danh mục | — |

| Hạn | Việc |
|---|---|
| **T2 09/11** | A gộp `V010__shared_lookups.sql` (A, B, D gửi bảng tra cứu); B đã đẩy `V201`, `V202` |
| **T3 10/11** | Toàn bộ migration `V001`…`V401` chạy không lỗi trên DB trống |
| **T4 11/11** (standup 1) | API của từng người chạy được qua Swagger |
| **T6 13/11** | Dữ liệu cào đã nạp, công thức `PUBLISHED`, đủ embedding |
| **T7 14/11** (standup 2) | Giao diện F-01, F-02, F-04, F-06 nối API thật |
| **CN 15/11** | Chạy thử luồng: đăng nhập → tìm món → xem chi tiết → tạo shopping list |

---

## Tuần 8 · 16/11 – 22/11: AI planner, kiểm thử, tag `v0.8.0-beta.1` và nộp PA4

Cổng nghiệm thu `v0.8` (project plan mục 4.4.2): coverage dòng và nhánh ≥ 70% cho package `service` của các module đã làm; P95 < 500 ms cho API không dùng AI ở 20 người dùng ảo trong 5 phút; bản nháp AI hiện trong ≤ 30 s với P90 (giới hạn cứng 60 s); tắt LLM thì 100% thao tác planner thủ công vẫn chạy; ma trận phân quyền đạt 100%; không còn lỗi S1.

| Người | Việc |
|---|---|
| **A** | **Software Architecture Document** (kiến trúc phân tầng, dựa trên `architecture.md` và sơ đồ kiến trúc); test + coverage 010/011 |
| **B** | Nghiệm thu chức năng (PO); test + coverage 020/060; ERD và DDL cho PA4 (xuất từ Flyway, đối chiếu `full-schema.md`) |
| **C** | F-03: ghép prompt từ RAG, gọi LLM giới hạn 60 s, kiểm tra kết quả, ô bị loại để trống, báo lỗi khi LLM lỗi; hộp thoại "Tạo bằng AI"; Mock LLM cho CI; xác nhận plan → shopping list (cùng D). Sau đó: release notes, tag `v0.8.0-beta.1`, đóng gói PA4 |
| **D** | Shopping list từ meal plan, nhân theo khẩu phần; F-05 cơ bản (xem từng bước); báo cáo Sprint 4; nghiệm thu cuối |
| **E** | Test tích hợp F-01 – F-04, test `LLM_MODE=offline`, k6; chạy kế hoạch kiểm thử `v0.8` → `docs/test/v0.8-test-report.md`; QA tài liệu PA4 |

| Hạn | Việc |
|---|---|
| **T2 16/11** | C: AI planner tạo được thực đơn 7 ngày × 3 bữa từ dữ liệu mẫu |
| **T3 17/11** | **Freeze tính năng `v0.8`**; chỉ nhận PR sửa lỗi và thêm test |
| **T4 18/11** (standup 1) | E chạy kiểm thử + k6, báo lỗi lên Jira |
| **T5 19/11** | Sửa xong lỗi S1; B nghiệm thu chức năng, D nghiệm thu cuối; C tag `v0.8.0-beta.1` |
| **T6 20/11** | A xong Architecture Document; C đóng gói; E QA bản đóng gói |
| **T7 21/11** | **Nộp PA4** |
| **CN 22/11** | Sprint 4 Review & Retrospective; Sprint 5 Planning |

---

## Tuần 9 · 23/11 – 29/11: các tính năng còn lại

| Người | Tính năng | Kết quả cuối tuần |
|---|---|---|
| **A** | F-08 Bộ sưu tập (`V502`): lưu công thức, tạo/xóa bộ sưu tập, hộp thoại "Lưu vào bộ sưu tập" | Lưu được công thức từ trang chi tiết |
| **B** | F-06 đầy đủ: ảnh từng bước, "Công thức của tôi" theo trạng thái | Đăng công thức có ảnh → admin duyệt → hiện trong tìm kiếm |
| **C** | F-09 Dinh dưỡng: LLM ước tính khi `PUBLISHED`, khối dinh dưỡng trên trang chi tiết; `LLM_MODE=live/mock/offline` cho demo | Khối dinh dưỡng hiện trên trang chi tiết; demo chạy được khi tắt mạng |
| **D** | F-05 đầy đủ: chế độ nấu toàn màn hình, nhiều đồng hồ có chuông, Screen Wake Lock, tiếp tục khi mất mạng; màn hình kết thúc nấu → mở form đánh giá | Nấu hết một công thức với 2 đồng hồ chạy cùng lúc |
| **E** | F-07 Đánh giá + Cooksnap (`V501`); F-10 Admin (`V503`): hàng đợi duyệt, đình chỉ người dùng, CRUD danh mục | Đánh giá + Cooksnap chạy được; admin duyệt trên giao diện |
| **E** | Kịch bản E2E Playwright cho kịch bản demo D1 – D10 (project plan mục 4.4.5) | D1 – D10 viết xong, chạy được ít nhất D1 – D6 |

| Hạn | Việc |
|---|---|
| **T3 24/11** | Mọi tính năng còn lại có API chạy được |
| **T4 25/11** (standup 1) | Có giao diện cho mọi tính năng còn lại |
| **T6 27/11** | Mọi tính năng bắt buộc đã merge vào `main` |
| **T7 28/11** (standup 2) | Chạy thử 10 kịch bản demo lần đầu; chốt danh sách lỗi |
| **CN 29/11** | Sửa lỗi S1/S2 tìm được khi chạy thử |

---

## Tuần 10 · 30/11 – 06/12: release candidate và demo

Cổng nghiệm thu `v1.0` (project plan mục 4.4.2, áp dụng cho phần bắt buộc): đạt 10 kịch bản demo trong **2 lần chạy thử liên tiếp**; coverage ≥ 70% cho package `service`; P95 < 500 ms ở 100 người dùng đồng thời; không còn lỗi S1/S2; chế độ offline chạy được.

| Người | Việc |
|---|---|
| **A** | Sửa lỗi; cập nhật Architecture Document nếu kiến trúc thay đổi; chuẩn bị phần trình bày kiến trúc + AI/RAG |
| **B** | Nghiệm thu đủ tính năng (PO); user guide; dẫn demo luồng người dùng |
| **C** | Ảnh chụp dữ liệu demo (seed snapshot); release notes, tag `v1.0.0-rc.1` và image Docker; đóng gói PA5; máy demo chạy sẵn bản Docker |
| **D** | Báo cáo Sprint 5 + tổng kết; nghiệm thu cuối (Go/No-Go); điều phối 2 lần chạy thử demo |
| **E** | Kiểm thử hồi quy, E2E D1 – D10, k6 100 người dùng; báo cáo kiểm thử cuối `docs/test/v1.0-test-report.md`; QA tài liệu PA5 |

| Hạn | Việc |
|---|---|
| **T2 30/11** | **Code freeze**; chỉ nhận PR sửa lỗi S1/S2 |
| **T3 01/12** | E chạy hồi quy + E2E + k6; **chạy thử demo lần 1** |
| **T4 02/12** | Sửa lỗi; **chạy thử demo lần 2**; D quyết định Go/No-Go; C tag `v1.0.0-rc.1` |
| **T5 03/12** | C đóng gói PA5; E QA bản đóng gói; cả nhóm tập trình bày |
| **T6 04/12 → tuần 10** | **Nộp PA5 và demo** theo lịch lớp; sau khi demo được chấp nhận, C tag lại commit đã demo thành `v1.0.0` |

> **Dự phòng:** nếu lần chạy thử thứ 2 không đạt, demo bằng tag `v0.8.0-beta.1` cộng các tính năng đã qua kiểm thử, và D báo trước cho giảng viên (project plan mục 4.4.7).

---

## Review chéo

Mỗi PR cần ít nhất 1 người review. PR chạm vào điểm giao giữa hai module (mục 2 của [phân công tuần 12/10](w1-spec-design-assignment.md)) cần thêm người của module kia.

| Người viết | Review code | Review tài liệu |
|---|---|---|
| A | C | B |
| B | E | C |
| C | A | D |
| D | B | E |
| E | A | A |

**Khi review code**, kiểm tra:

- CI xanh; có test cho logic mới trong `service`.
- Migration mới không sửa file đã merge, chỉ thêm file mới.
- Không gọi LLM từ frontend; không commit khóa API hay `.env`.
- Có xử lý lỗi theo spec: không có quyền, dữ liệu sai, mất mạng, LLM lỗi.
