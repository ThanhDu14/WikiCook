# WikiCook Frontend (React SPA) — PRD

> Phạm vi: **chỉ frontend**. Backend, database, LLM là phụ thuộc bên ngoài; trong MVP frontend chạy trên **mock API (MSW)**.
> Nguồn: `docs/requirements/project-proposal.md`, `docs/requirements/existing-app-survey.md`.

## Problem Statement

Người nấu ăn bận rộn (20–45 tuổi) mất 15–30 phút mỗi ngày để quyết định "hôm nay ăn gì", phải lướt qua các blog công thức nhiều quảng cáo, không tự scale khẩu phần, không nối được sang danh sách đi chợ, khó kiểm tra dị ứng, và phải chạm màn hình bằng tay ướt/dính khi nấu. Hệ quả: bỏ cuộc nấu ở nhà, mua trùng/thiếu nguyên liệu, nấu hỏng món, rủi ro dị ứng.

## Evidence

- Proposal §1.1 nêu 4 pain point (decision fatigue, tài nguyên phân mảnh, khó tuân thủ chế độ ăn, không thân thiện khi nấu) — **Assumption – chưa có phỏng vấn người dùng thật**, cần xác nhận bằng usability test ở cuối Phase 4.
- Khảo sát angingon.com (`existing-app-survey.md`): không có Cook Mode, không cho người dùng đóng góp, không có review/Cooksnap, không có dinh dưỡng/dị ứng.
- Mealime và Paprika (đối thủ quốc tế) đều coi "meal plan → shopping list tự gộp" và "cook mode có timer" là tính năng cốt lõi → mẫu hình đã được thị trường chứng minh.

## Proposed Solution

Một Single-Page Application responsive (mobile-first, chạy tốt trên desktop) bằng React, song ngữ Việt/Anh, gồm luồng cốt lõi **Tìm & lọc → Chi tiết công thức → Cook Mode có timer**, cùng luồng lập kế hoạch **Meal Planner → Shopping List**, và các tính năng cộng đồng mức cơ bản (Collections, đăng công thức, review). Frontend phát triển độc lập với backend nhờ mock API dựa trên một hợp đồng API được viết ra từ sớm, để khi backend xong chỉ cần đổi base URL. Cách này được chọn thay vì chờ backend vì nhóm frontend hiện chỉ có 1 người và thời gian 8 tuần.

## Key Hypothesis

We believe **luồng Tìm & lọc theo chế độ ăn → Chi tiết → Cook Mode có timer bấm-để-chạy** will **giảm thời gian quyết định món và giúp nấu xong mà không phải thao tác nhiều trên màn hình** for **người nấu ăn bận rộn**.
We'll know we're right when **≥ 4/5 người thử trong usability test tìm được món phù hợp trong < 60 giây và hoàn thành Cook Mode của một công thức mà không cần trợ giúp**.

## What We're NOT Building (MVP)

- **Admin Dashboard & Moderation (3.10)** — để sau MVP; công thức người dùng đăng trong MVP được giả lập trạng thái "Pending review".
- **Kết nối LLM thật** (AI search, AI menu, phân tích dinh dưỡng) — chỉ UI + mock response; phụ thuộc backend.
- **OAuth thật (Google/Apple)** — chỉ có nút UI, xử lý bằng mock.
- **Offline PWA đầy đủ (Service Worker cache toàn site)** — chỉ lưu phiên Cook Mode vào localStorage để không mất khi reload/rớt mạng.
- **Ứng dụng native mobile**, thanh toán, thương mại điện tử, tư vấn dinh dưỡng y khoa.
- **Điều khiển bằng giọng nói** trong Cook Mode — "hands-free" trong MVP nghĩa là nút lớn + màn hình luôn sáng + ít thao tác.

## Success Metrics

| Metric | Target | How Measured |
|--------|--------|--------------|
| Thời gian tìm được công thức phù hợp | < 60 giây | Usability test 5 người, bấm giờ |
| Hoàn thành luồng Tìm → Chi tiết → Cook Mode | ≥ 4/5 người, 0 lỗi chặn | Usability test + E2E Playwright pass |
| Lighthouse mobile – Accessibility | ≥ 90 | Lighthouse trên build production |
| Lighthouse mobile – Performance | ≥ 90 | Lighthouse trên build production |
| Độ phủ i18n | 100% chuỗi UI qua translation key | Lint/kiểm tra không có chuỗi cứng trong JSX |
| Test coverage logic (hooks/utils) | ≥ 80% | Vitest coverage |

## Open Questions

- [ ] Công nghệ backend và định dạng API (REST/GraphQL, auth bằng JWT hay session)? Ảnh hưởng tầng API client.
- [ ] Ai viết và duyệt API contract — frontend đề xuất hay backend? Hạn chốt?
- [ ] Nguồn dữ liệu mẫu: tự soạn bao nhiêu công thức (đề xuất 30–50, cả món Việt và quốc tế, 2 ngôn ngữ)?
- [ ] Nội dung công thức có song ngữ không, hay chỉ UI song ngữ còn công thức giữ ngôn ngữ gốc?
- [ ] Quy tắc gộp đơn vị trong Shopping List (g/kg, muỗng/ml, "1 nắm") — cần bảng quy đổi chung với backend.
- [ ] Khi thêm thành viên frontend: tiếp nhận phase nào (đề xuất Phase 5 hoặc 7)?
- [ ] Tiêu chí chấm điểm môn học có yêu cầu bắt buộc phần Admin không? Nếu có cần đưa 3.10 trở lại MVP.

---

## Users & Context

**Primary User — Busy Home Cook**
- **Who**: Người đi làm / sinh viên sống riêng, 20–45 tuổi, nấu cho 1–4 người.
- **Current behavior**: Lướt Google/Facebook/YouTube tìm món, đọc blog dài, chụp màn hình nguyên liệu, tự nhớ thời gian nấu.
- **Trigger**: 17–18h trên đường về (điện thoại) cần quyết định bữa tối; tối Chủ nhật (laptop) lên kế hoạch cả tuần.
- **Success state**: Chọn được món hợp chế độ ăn trong vòng 1 phút, có danh sách đi chợ đầy đủ, nấu theo từng bước với timer mà không phải lướt.

**Secondary Actors**
| Actor | Nhu cầu chính trên frontend |
|---|---|
| Recipe Contributor | Đăng công thức qua wizard có cấu trúc, nhận review và Cooksnap |
| Dietary-Conscious User | Lọc theo dị ứng/calo, thấy badge cảnh báo rõ ràng trên card và trang chi tiết |
| Guest (chưa đăng nhập) | Xem/tìm công thức, mở Cook Mode; các tính năng cá nhân hoá yêu cầu đăng nhập |
| Admin | *Ngoài MVP* |

**Job to Be Done**
When tôi về nhà mệt và chỉ còn vài nguyên liệu, I want to tìm nhanh một món < 30 phút hợp với chế độ ăn của mình, so I can nấu xong bữa tối mà không phải nghĩ nhiều hay lục lại công thức.

**Non-Users**
Đầu bếp chuyên nghiệp/nhà hàng; người cần tư vấn dinh dưỡng y khoa (dinh dưỡng chỉ là ước tính AI tham khảo); người bán hàng/thương mại.

---

## Solution Detail

### Core Capabilities (MoSCoW)

| Priority | Capability | Rationale |
|----------|------------|-----------|
| Must | App shell, routing, i18n Việt/Anh, mock API, design tokens | Nền móng cho mọi màn hình |
| Must | Auth (email) + Onboarding chế độ ăn/dị ứng/khẩu phần (3.1) | Cá nhân hoá lọc & cảnh báo dị ứng |
| Must | Home, Tìm & lọc đa tiêu chí, Chi tiết công thức có scale khẩu phần, badge dinh dưỡng/dị ứng (3.2, 3.9 hiển thị) | Đầu luồng giá trị cốt lõi |
| Must | Cook Mode fullscreen + nhiều timer song song + âm báo + Wake Lock (3.5) | Điểm khác biệt lớn nhất so với angingon.com |
| Must | Weekly Meal Planner kéo-thả + Shopping List tự gộp theo quầy (3.3, 3.4) | Luồng giá trị thứ hai; được người dùng chọn là bắt buộc |
| Should | Bookmarks & Collections (3.8) | Nhẹ, tăng quay lại |
| Should | Recipe Creation Wizard (3.6) | Mô hình 2 chiều, khác biệt với blog 1 chiều |
| Should | Reviews, rating 5 sao, Cooksnaps (3.7) | Niềm tin cộng đồng |
| Should | UI AI Smart Chef: ô AI search ngôn ngữ tự nhiên + nút "AI tạo thực đơn" với mock response (§4) | Giữ chỗ UI để nối LLM sau |
| Could | Lưu phiên Cook Mode vào localStorage, khôi phục khi reload | Tăng độ bền khi rớt Wi-Fi (proposal §2.2) |
| Could | Dark mode / high-contrast toggle trong Cook Mode | Đọc dễ trong bếp |
| Won't | Admin dashboard & moderation (3.10) | Để sau MVP — 1 người FE, 8 tuần |
| Won't | LLM thật, OAuth thật, offline PWA đầy đủ, voice control | Phụ thuộc backend / ngoài thời gian |

### Actors × Screens (Screen Inventory)

| # | Màn hình | Route (đề xuất) | Actor | Auth | Module |
|---|---|---|---|---|---|
| S1 | Home (hero + AI search + nổi bật/mới nhất) | `/` | Guest, User | – | 3.2, §4 |
| S2 | Đăng nhập / Đăng ký | `/login`, `/register` | Guest | – | 3.1 |
| S3 | Onboarding chế độ ăn (nhiều bước) | `/onboarding` | User mới | ✔ | 3.1 |
| S4 | Hồ sơ & cài đặt (avatar, chế độ ăn, ngôn ngữ) | `/profile` | User | ✔ | 3.1 |
| S5 | Tìm kiếm & lọc | `/recipes?q=&filters` | Guest, User | – | 3.2 |
| S6 | Chi tiết công thức (scale khẩu phần, dinh dưỡng, review) | `/recipes/:id` | Guest, User | – | 3.2, 3.7, 3.9 |
| S7 | Cook Mode | `/recipes/:id/cook` | Guest, User | – | 3.5 |
| S8 | Meal Planner tuần | `/planner` | User | ✔ | 3.3, §4 |
| S9 | Shopping List | `/shopping-list` | User | ✔ | 3.4 |
| S10 | Collections (danh sách + chi tiết) | `/collections`, `/collections/:id` | User | ✔ | 3.8 |
| S11 | Recipe Wizard (tạo/sửa) | `/recipes/new`, `/recipes/:id/edit` | Contributor | ✔ | 3.6 |
| S12 | Công thức của tôi (trạng thái Draft/Pending/Published) | `/me/recipes` | Contributor | ✔ | 3.6 |
| S13 | 404 / lỗi / offline | `*` | All | – | – |

### User Stories (MVP)

**3.1 Auth & Onboarding**
- US-01: Là khách, tôi muốn đăng ký bằng email/mật khẩu để lưu thực đơn và bộ sưu tập. *AC: validate email/mật khẩu inline; lỗi hiển thị bằng ngôn ngữ đang chọn.*
- US-02: Là người dùng mới, tôi muốn khai báo dị ứng, chế độ ăn và số người trong nhà ngay sau đăng ký để kết quả được lọc sẵn. *AC: có thể bỏ qua; sửa lại được ở `/profile`.*
- US-03: Là người dùng, tôi muốn đổi ngôn ngữ Việt/Anh bất kỳ lúc nào và được ghi nhớ.

**3.2 / 3.9 Khám phá & Chi tiết**
- US-04: Là người nấu bận rộn, tôi muốn lọc theo thời gian, ẩm thực, độ khó, phương pháp nấu, calo để tìm món < 30 phút. *AC: bộ lọc phản ánh lên URL (chia sẻ/back được); có trạng thái rỗng & loading.*
- US-05: Là người ăn kiêng, tôi muốn công thức chứa chất tôi dị ứng bị cảnh báo rõ (hoặc ẩn) dựa trên hồ sơ.
- US-06: Là người dùng, tôi muốn đổi số khẩu phần và thấy lượng nguyên liệu tự tính lại.
- US-07: Là người dùng, tôi muốn xem calo/macro mỗi khẩu phần kèm dòng miễn trừ "ước tính bởi AI".
- US-08: Là người dùng, tôi muốn gõ câu tự nhiên ("món chay dưới 20 phút có đậu phụ") vào ô AI search và nhận gợi ý. *(mock)*

**3.5 Cook Mode**
- US-09: Là người đang nấu, tôi muốn xem từng bước chữ lớn, nút Tiếp/Lùi to, màn hình không tự tắt.
- US-10: Là người đang nấu, tôi muốn bấm vào thời lượng trong bước ("15 phút") để bật timer, chạy nhiều timer cùng lúc, có âm báo khi hết giờ.
- US-11: Là người đang nấu, tôi muốn reload/rớt mạng mà không mất bước hiện tại và các timer. *(Could)*

**3.3 / 3.4 Planner & Shopping**
- US-12: Là người dùng, tôi muốn kéo công thức vào ô Sáng/Trưa/Tối của từng ngày trong tuần (và có thể thêm bằng nút cho người dùng bàn phím/mobile).
- US-13: Là người dùng, tôi muốn bấm "AI tạo thực đơn" để điền gợi ý cho tuần. *(mock)*
- US-14: Là người dùng, tôi muốn tạo danh sách đi chợ từ các ngày đã chọn, nguyên liệu trùng được gộp và nhóm theo quầy, tick được khi mua.

**3.8 Collections**
- US-15: Là người dùng, tôi muốn bookmark công thức và sắp vào bộ sưu tập tự đặt tên.

**3.6 / 3.7 Cộng đồng**
- US-16: Là contributor, tôi muốn tạo công thức qua wizard nhiều bước (thông tin → nguyên liệu → bước + ảnh + timer → xem trước) và lưu nháp.
- US-17: Là người đã nấu, tôi muốn đánh giá 1–5 sao, viết nhận xét/biến tấu và đăng ảnh Cooksnap.

### MVP Scope

Tối thiểu để kiểm chứng giả thuyết: **Phase 1 → 4** (nền móng, auth/onboarding, khám phá/chi tiết, Cook Mode). Phase 5–7 là phạm vi MVP người dùng đã chọn nhưng **cắt từ dưới lên** nếu trễ tiến độ (thứ tự cắt: 7 → 5 → phần AI mock ở 6).

### User Flow (critical path)

```
Home ──(tìm/AI search)──► Kết quả lọc ──► Chi tiết (chỉnh khẩu phần, xem badge)
                                             │
                     ┌───────────────────────┴──────────────┐
                     ▼                                      ▼
              Cook Mode (bước + timer)            Thêm vào Planner ─► Shopping List
                     │                                                     │
                     ▼                                                     ▼
             Review / Cooksnap ─► Lưu vào Collection          (đi chợ xong) ─► Cook Mode
```

---

## Technical Approach

**Feasibility**: MEDIUM–HIGH — SPA dữ liệu mock là mô hình quen thuộc; độ phức tạp tập trung ở Cook Mode (timer/âm thanh/Wake Lock), Planner (kéo-thả), Shopping List (gộp đơn vị), Wizard (form nhiều bước + upload ảnh).

**Architecture Notes** *(đề xuất — chốt bằng ADR ở bước Kiến trúc)*
- Vite + React + TypeScript; cấu trúc theo feature (`src/features/{auth,recipes,cook-mode,planner,shopping,collections,contribute}`).
- React Router cho routing; route bảo vệ cho các màn hình cần đăng nhập.
- TanStack Query cho server state; Zustand cho client state (timers, phiên Cook Mode, ngôn ngữ).
- Tailwind CSS + design tokens; react-i18next cho song ngữ (không chuỗi cứng trong JSX).
- MSW + bộ seed data JSON làm mock API; API client một lớp duy nhất để đổi sang backend thật bằng biến môi trường.
- react-hook-form + zod cho Onboarding và Wizard; dnd-kit cho kéo-thả Planner (hỗ trợ bàn phím).
- Screen Wake Lock API + Web Audio cho Cook Mode (fallback khi trình duyệt không hỗ trợ).
- Vitest + React Testing Library (unit/component), Playwright (E2E luồng cốt lõi).
- Mã nguồn đặt trong `src/` của repo (hiện đang trống).

**Technical Risks**

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| 1 người FE, phạm vi 8 module trong 8 tuần → trễ | H | Ưu tiên theo phase; cắt từ Phase 7 trở lên; giữ Phase 1–4 chắc chắn |
| Mock lệch với API thật → sửa nhiều khi tích hợp | M | Viết API contract (types + OpenAPI) trước Phase 2, dùng chung cho MSW |
| Timer song song lệch giờ khi tab chạy nền | M | Tính theo timestamp kết thúc thay vì đếm interval; test với fake timers |
| Trình duyệt chặn âm thanh tự phát / không hỗ trợ Wake Lock | M | Mở khoá audio bằng thao tác người dùng khi vào Cook Mode; hiển thị gợi ý nếu không hỗ trợ |
| Gộp đơn vị nguyên liệu sai | M | Bảng quy đổi đơn vị chuẩn + unit test; đơn vị không quy đổi được thì liệt kê riêng |
| Kéo-thả không dùng được trên mobile/bàn phím | M | dnd-kit có keyboard sensor + nút "Thêm" thay thế |
| Song ngữ làm chậm tiến độ | L | Setup i18n ở Phase 1, mọi chuỗi qua key ngay từ đầu |

---

## Implementation Phases

<!--
  STATUS: pending | in-progress | complete
  PARALLEL: phases that can run concurrently (e.g., "with 3" or "-")
  DEPENDS: phases that must complete first (e.g., "1, 2" or "-")
  PRP: link to generated plan file once created
-->

| # | Phase | Description | Status | Parallel | Depends | PRP Plan |
|---|-------|-------------|--------|----------|---------|----------|
| 1 | Foundation | Scaffold Vite/React/TS, router, Tailwind + tokens, i18n, MSW + seed data, API client, app shell, test setup | pending | - | - | - |
| 2 | Auth & Onboarding | Login/Register (mock), route bảo vệ, onboarding chế độ ăn, profile & đổi ngôn ngữ | pending | with 3 | 1 | - |
| 3 | Discovery & Recipe Detail | Home + AI search (mock), tìm & lọc đồng bộ URL, card, chi tiết + scale khẩu phần + badge dinh dưỡng/dị ứng | pending | with 2 | 1 | - |
| 4 | Cook Mode | Fullscreen từng bước, timer song song, âm báo, Wake Lock, lưu phiên | pending | with 5 | 3 | - |
| 5 | Collections | Bookmark trên card/chi tiết, CRUD bộ sưu tập | pending | with 4 | 2, 3 | - |
| 6 | Planner & Shopping List | Lịch tuần kéo-thả, AI tạo thực đơn (mock), sinh shopping list gộp theo quầy, checklist | pending | - | 2, 3 | - |
| 7 | Contribute & Reviews | Recipe Wizard nhiều bước + upload ảnh + lưu nháp, "Công thức của tôi", rating/review/Cooksnap | pending | - | 2, 3 | - |
| 8 | Hardening & QA | A11y audit, Lighthouse ≥ 90, E2E luồng cốt lõi, rà i18n, usability test 5 người | pending | - | 4, 6 | - |

### Phase Details

**Phase 1: Foundation** *(tuần 1)*
- **Goal**: Mọi màn hình sau chỉ việc thêm vào, không phải sửa nền.
- **Scope**: Project trong `src/`; routing + layout (header, nav mobile, footer); design tokens; i18n vi/en; MSW với seed 30–50 công thức; API client + types theo contract; Vitest/RTL/Playwright chạy được; ESLint/Prettier.
- **Success signal**: `npm run dev`, `npm test`, `npm run build` đều xanh; đổi ngôn ngữ trên app shell hoạt động; gọi được `GET /recipes` mock.

**Phase 2: Auth & Onboarding** *(tuần 2)*
- **Goal**: Người dùng có hồ sơ chế độ ăn để cá nhân hoá.
- **Scope**: S2, S3, S4; lưu phiên đăng nhập mock; guard route.
- **Success signal**: US-01..03 pass test; route cần đăng nhập redirect đúng.

**Phase 3: Discovery & Recipe Detail** *(tuần 2–3)*
- **Goal**: Hoàn thành nửa đầu luồng cốt lõi.
- **Scope**: S1, S5, S6 (phần chi tiết, chưa có review); scale khẩu phần; badge/cảnh báo dị ứng theo hồ sơ.
- **Success signal**: US-04..08 pass; tìm món < 60s khi tự thử.

**Phase 4: Cook Mode** *(tuần 4)*
- **Goal**: Điểm khác biệt chính của sản phẩm.
- **Scope**: S7; timer store dựa timestamp; âm báo; Wake Lock; lưu/khôi phục phiên.
- **Success signal**: US-09..11 pass; 3 timer song song chính xác ±1s sau 10 phút chạy nền; usability test sớm với 2–3 người.

**Phase 5: Collections** *(tuần 5)*
- **Goal**: Người dùng lưu & tổ chức công thức.
- **Scope**: S10 + nút bookmark trên card/chi tiết.
- **Success signal**: US-15 pass.

**Phase 6: Planner & Shopping List** *(tuần 5–6)*
- **Goal**: Luồng giá trị thứ hai: lên thực đơn → đi chợ.
- **Scope**: S8, S9; dnd-kit; logic gộp/quy đổi đơn vị; nhóm theo quầy; AI menu mock.
- **Success signal**: US-12..14 pass; unit test gộp đơn vị ≥ 90% coverage.

**Phase 7: Contribute & Reviews** *(tuần 7)*
- **Goal**: Mô hình cộng đồng 2 chiều.
- **Scope**: S11, S12; review/rating/Cooksnap trên S6.
- **Success signal**: US-16, US-17 pass; công thức mới hiển thị trạng thái Pending.

**Phase 8: Hardening & QA** *(tuần 8)*
- **Goal**: Đạt Success Metrics.
- **Scope**: Sửa lỗi a11y/perf, E2E luồng cốt lõi, usability test 5 người, cập nhật `docs/test/`.
- **Success signal**: Tất cả metric trong bảng Success Metrics đạt.

### Parallelism Notes

Hiện chỉ có 1 người frontend nên các phase thực tế chạy tuần tự theo số thứ tự. Cột Parallel cho biết phần nào **có thể giao cho thành viên mới** mà không đụng nhau: Phase 2 ‖ 3 (khác feature folder, chỉ chung API client), Phase 4 ‖ 5. Phase 6 và 7 cùng phụ thuộc 2+3 và có thể chia cho 2 người nếu nhóm mở rộng. Phase 8 luôn ở cuối.

---

## Decisions Log

| Decision | Choice | Alternatives | Rationale |
|----------|--------|--------------|-----------|
| Nguồn dữ liệu | Mock API (MSW) + contract | Chờ backend; gọi API thật | Backend chưa có; FE 1 người cần làm song song |
| Ngôn ngữ UI | Song ngữ vi/en (i18n) | Chỉ vi; chỉ en | Người dùng chọn; setup từ đầu rẻ hơn thêm sau |
| Phạm vi MVP | 3.1–3.9 + UI AI mock | Chỉ luồng cốt lõi 3 module | Người dùng chọn; giảm rủi ro bằng thứ tự phase + thứ tự cắt |
| Admin (3.10) | Ngoài MVP | Moderation queue tối giản | Không đủ nhân lực; Open Question về yêu cầu chấm điểm |
| AI Smart Chef | UI + mock response | Bỏ hẳn; LLM thật | Giữ trải nghiệm demo, không phụ thuộc backend |
| Hands-free | Nút lớn + Wake Lock + timer bấm-để-chạy | Voice control | Voice ngoài thời gian và kém ổn định đa trình duyệt |

---

## Research Summary

**Market Context**
- angingon.com: mạnh ở AI search trên hero, bộ lọc sidebar, meal planner 3 bữa + AI menu; thiếu Cook Mode, đóng góp cộng đồng, review, dinh dưỡng.
- Mealime: shopping list tự gộp & nhóm theo quầy; onboarding chế độ ăn/dị ứng từ đầu.
- Paprika: bấm thời lượng trong bước để bật timer; giữ màn hình sáng; scale khẩu phần; planner nối shopping list.

**Technical Context**
- `src/` hiện trống — không có pattern có sẵn; convention sẽ được đặt ở Phase 1.
- `docs/analysis-and-design/architecture.md` chưa có nội dung — cần điền + ADR trước Phase 1.
- Screen Wake Lock API được hỗ trợ trên Chrome/Edge và Safari 16.4+; cần fallback.

---

*Generated: 2026-10-02*
*Status: DRAFT - needs validation*
