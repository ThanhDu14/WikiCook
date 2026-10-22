# Implementation Plan: Weekly Meal Planner (Manual)

**Branch**: `feature/SCRUM-xx-meal-planner` *(TODO: mã Jira)* | **Date**: 2026-10-18 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/030-meal-planner/spec.md`

## Summary

Lịch thực đơn tuần 7 ngày × 3 bữa cho người dùng đã đăng nhập: thêm món từ ô trống hoặc từ trang
công thức, khẩu phần riêng từng món, chuyển/đổi chỗ (kéo-thả trên desktop, menu "Chuyển sang…"
ở mọi nơi), thay, xóa, xóa cả ngày, chuyển tuần, bàn giao sang Shopping List, xử lý công thức
không còn khả dụng và trạng thái mất mạng.

Cách làm: module `com.wikicook.mealplan` (controller → service → repository) trên hai bảng
`meal_plans`, `meal_plan_items` của full-schema mục 4.6. Ô định danh bằng
`(weekStart, day, meal)` của người đang đăng nhập; mỗi lệnh ghi gửi kèm trạng thái ô mong đợi
để phát hiện xung đột (409). Move/swap đổi nội dung hai dòng trong một transaction có khóa dòng.
Hộp chọn món là native query tham số hóa, xếp hạng theo bữa → ẩm thực → thời gian → độ khớp tên,
món vi phạm hồ sơ xuống cuối. Dữ liệu của module khác (hồ sơ, bộ sưu tập, công thức, dinh dưỡng)
đọc qua port. Frontend React: lưới desktop + xem từng ngày trên mobile, optimistic update có
rollback, `@dnd-kit` cho kéo-thả, không lưu dữ liệu offline.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5 + React 18 (frontend)

**Primary Dependencies**: Spring Boot 3.x (Web, Data JPA, Security, Validation), springdoc-openapi,
Flyway; Vite, Tailwind CSS 3.4, `@dnd-kit/core` (research R13)

**Storage**: PostgreSQL 16 (+ `unaccent`, `pg_trgm`); bảng `meal_plans`, `meal_plan_items`
(migration `V301__meal_plans.sql`)

**Testing**: JUnit 5 + Mockito, `@DataJpaTest`/`@SpringBootTest` + Testcontainers
(`pgvector/pgvector:pg16`), JaCoCo; Vitest + React Testing Library + `msw`

**Target Platform**: Linux server (Docker Compose); trình duyệt hiện đại desktop và mobile từ 360 px

**Project Type**: Web application (backend REST + SPA)

**Performance Goals**: p95 xem tuần ≤ 2 s, p95 xác nhận lưu ≤ 1 s, 200 người dùng đồng thời (NFR-001)

**Constraints**: Không xem offline (FR-027); move/swap nguyên tử (NFR-005); chỉ chủ sở hữu thấy
kế hoạch (NFR-004); tuần theo giờ Asia/Ho_Chi_Minh (FR-002)

**Scale/Scope**: ~10.000 công thức, 21 ô/tuần/người, 1 trang chính + 2 dialog + 1 nút nhúng ở
trang công thức

Không còn mục NEEDS CLARIFICATION – xem [research.md](research.md) (R1–R17).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Ghi chú |
|---|---|---|
| I. Spec trước, code sau | ⚠️ Có điều kiện | Spec còn **Draft**, chưa có mã Jira. Plan được lập để nhóm duyệt cùng; **không bắt đầu implement** trước khi spec được duyệt và branch `feature/SCRUM-xx-meal-planner` được tạo. |
| II. Test-First | ✅ | Mỗi acceptance scenario có test (SC-008); tasks sẽ đặt test trước code; JaCoCo ≥ 70% cho `mealplan.service`; Testcontainers PostgreSQL thật, không H2 (R17). |
| III. Kiến trúc phân lớp | ✅ | Controller → Service → Repository; DTO record, không trả entity; package theo tính năng `mealplan`; module khác qua port (R7). Thư viện mới ghi ở Complexity Tracking. |
| IV. Bảo mật & dữ liệu | ✅ | Spring Security, mọi endpoint yêu cầu đăng nhập; `userId` lấy từ `SecurityContext`, không có id người khác trên URL; Bean Validation; query tham số hóa (`NamedParameterJdbcTemplate`); ProblemDetail không lộ stack trace; tên công thức hiển thị qua React (tự escape). |
| V. Trải nghiệm nhất quán | ✅ | Responsive từ 360 px, chuỗi tiếng Việt ở string store; dùng chung `RecipeCard`, `Button`, `Dialog`, `Toast`; picker phân trang 20/trang; tổng calo của 090 luôn kèm disclaimer. |
| Ràng buộc kỹ thuật | ✅ | Java 21, Spring Boot 3, PostgreSQL 16 + Flyway, React 18 + Vite, `/api/v1`, OpenAPI. |

**Kết quả gate**: PASS (điều kiện của Nguyên tắc I phải xong trước `/speckit-implement`).

**Re-check sau Phase 1**: PASS – data model không thêm bảng; contracts chỉ dùng
`/api/v1/meal-plans/**`; port giữ ranh giới module; không có vi phạm mới.

## Project Structure

### Documentation (this feature)

```text
specs/030-meal-planner/
├── plan.md              # File này
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/
│   ├── openapi.yaml     # REST API /api/v1/meal-plans
│   └── integration.md   # Port và facade với 011, 031, 040, 060, 080, 090
└── tasks.md             # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
src/backend/
├── pom.xml
└── src/
    ├── main/java/com/wikicook/mealplan/
    │   ├── controller/   MealPlanController, PickerController, MealPlanExceptionHandler
    │   ├── service/      MealPlanService, SlotMoveService, ServingsPolicy, WeekCalendar,
    │   │                 PickerService, ShoppingHandoffService
    │   ├── repository/   MealPlanRepository, MealPlanItemRepository,
    │   │                 RecipePickerQueryRepository (native, chỉ đọc)
    │   ├── domain/       MealPlan, MealPlanItem, Meal, ItemSource, DietType
    │   ├── dto/          WeekPlanResponse, DayPlanDto, PlanItemDto, PutSlotRequest,
    │   │                 PatchServingsRequest, MoveRequest, MoveResult, PickerResultDto, ...
    │   ├── port/         DietaryProfilePort, CollectionRecipePort, RecipeSummaryPort,
    │   │                 DayNutritionPort + bean mặc định
    │   ├── api/          MealPlanShoppingFacade, MealPlanWriteFacade (cho 040, 031)
    │   └── config/       MealPlanClockConfig
    ├── main/resources/db/migration/V301__meal_plans.sql
    └── test/java/com/wikicook/mealplan/
        ├── service/      unit test (Mockito)
        ├── repository/   @DataJpaTest + Testcontainers
        └── controller/   @SpringBootTest + MockMvc

src/frontend/
└── src/
    ├── api/mealPlanApi.ts
    ├── features/meal-planner/
    │   ├── pages/MealPlannerPage.tsx
    │   ├── components/   WeekHeader, WeekGrid, DayView, SlotCard, EmptySlot, SlotMenu,
    │   │                 MoveToMenu, ServingsInput, RecipePicker, AddToPlanDialog,
    │   │                 AddToPlanButton, EmptyWeekState, OfflineNotice, DayNutritionBadge
    │   ├── hooks/        useWeekPlan, useOnlineStatus, useIsDesktop
    │   ├── lib/          week.ts (tính tuần theo UTC+7)
    │   └── strings.vi.ts
    └── features/meal-planner/__tests__/   Vitest + RTL + msw
```

**Structure Decision**: Web application với backend `src/backend` và frontend `src/frontend`
theo deliverable Build v0.5 của `docs/management/project-plan.md` (research R1, đã chốt
2026-10-18). Module `mealplan` tự chứa đủ 4 lớp; điểm nối với module khác nằm trong `port/` và
`api/`.

## Phases (tóm tắt để /speckit-tasks chia việc)

| Thứ tự | Nội dung | User story |
|---|---|---|
| 1 | Migration `V301`, entity, repository, `WeekCalendar` (giờ VN), bean port mặc định | nền tảng |
| 2 | GET tuần, PUT ô, PATCH servings, picker + xếp hạng; UI lưới/ngày, `RecipePicker` | US1 (P1) |
| 3 | `AddToPlanButton`/`AddToPlanDialog`, 409 `SLOT_OCCUPIED`, redirect khách | US2 (P1) |
| 4 | Move/swap, thay, xóa, xóa ngày, rollback; dnd-kit + `MoveToMenu` | US3 (P1) |
| 5 | Chuyển tuần, empty state, nút "Tạo bằng AI" (ẩn khi chưa có 031) | US4 (P2) |
| 6 | Shopping handoff + `MealPlanShoppingFacade` | US5 (P2) |
| 7 | Hiển thị món không khả dụng | US6 (P2) |
| 8 | Mobile day view, `OfflineNotice`, tải lại khi có mạng | US7 (P3) |
| 9 | `DayNutritionBadge` (khi 090 có `DayNutritionPort`), `MealPlanWriteFacade` cho 031, k6 | tích hợp |

## Complexity Tracking

| Bổ sung | Lý do cần | Phương án đơn giản hơn bị loại vì |
|---|---|---|
| `@dnd-kit/core`, `@dnd-kit/utilities` (frontend) | Kéo-thả giữa 21 ô có hỗ trợ bàn phím và thông báo screen reader (FR-014, NFR-003) | HTML5 DnD gốc không có bàn phím, lỗi trên Safari/iOS, khó test; `react-beautiful-dnd` ngừng bảo trì (R13) |
| `msw` (dev dependency frontend) | Mock API ở tầng network cho test optimistic update/rollback và offline | Mock `fetch` thủ công dài và dễ sai khi test chuỗi request (R17) |
| Port interface tới 011/060/080/090 | Làm song song khi module kia chưa có; giữ ranh giới module | Đọc thẳng bảng của module khác – phá Nguyên tắc III và khóa 030 vào tiến độ người khác (R7) |
| Native query cho picker | Sắp xếp nhiều tiêu chí + phân trang trên ~10.000 công thức | JPA Criteria khó viết `similarity()`/`f_unaccent`; xếp trong Java không phân trang được (R6) |
