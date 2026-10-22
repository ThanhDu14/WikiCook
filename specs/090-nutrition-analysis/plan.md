# Implementation Plan: Nutrition & Allergen Analysis

**Branch**: `feature/SCRUM-xx-nutrition-analysis` *(TODO: mã Jira)* | **Date**: 2026-10-18 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/090-nutrition-analysis/spec.md`

## Summary

Khi một công thức được duyệt (lần đầu, bản sửa đổi nguyên liệu/khẩu phần, hoặc seed import), hệ
thống ước tính kcal, protein, carb, fat mỗi khẩu phần và gợi ý nhãn chế độ ăn bằng LLM **một lần**,
kiểm tra hợp lý rồi lưu vào `recipe_nutrition`. Đọc công thức, tìm kiếm, planner chỉ đọc giá trị đã
lưu. Cảnh báo dị ứng lấy từ `ingredient_allergens`, không bao giờ từ AI, và hiển thị tách khỏi nhãn AI.
Admin xem danh sách, bấm "Recalculate" (cách duy nhất để gọi lại AI), ẩn/hiện ước tính sai.

Cách làm: module `com.wikicook.nutrition` dùng lại `com.wikicook.ai` của 031 (`LlmClient` timeout
60 s, `AiQuotaService`, bảng `ai_requests`).
- **Trigger**: `@TransactionalEventListener(AFTER_COMMIT)` → `NutritionQueue` trong bộ nhớ (ưu tiên,
  1 worker, giãn cách) – research R3.
- **Fingerprint** SHA-256 của nguyên liệu + định lượng + đơn vị + cờ tùy chọn + khẩu phần: cùng
  fingerprint thì không gọi lại; kết quả về với fingerprint cũ thì bỏ (R5, R6).
- **`NutritionValidator`** thuần: cấu trúc, không âm, ≤ 3.000 kcal / 300 g, ±20% so với 4P+4C+9F,
  whitelist 6 nhãn, bỏ nhãn mâu thuẫn allergen/kind (R8).
- Cung cấp `DayNutritionPort` cho 030 và `CalorieFilterSql` cho 020 (R13).

## Technical Context

**Language/Version**: Java 21 (virtual threads), TypeScript 5 + React 18

**Primary Dependencies**: Spring Boot 3.x (Web, Data JPA, Security, Validation), springdoc-openapi,
Flyway, Jackson; package `com.wikicook.ai` của 031. Không thêm thư viện mới.

**Storage**: PostgreSQL 16; bảng `recipe_nutrition` (`V303__recipe_nutrition.sql`); ghi `ai_requests`
(V302, 031); đọc `recipes`, `recipe_ingredients`, `ingredients`, `units`, `ingredient_allergens`,
`allergens`, `user_allergens`; ghi `audit_logs` qua port 010

**Testing**: JUnit 5 + Mockito, Testcontainers `pgvector/pgvector:pg16`, `FakeLlmClient` (031) + bộ
response mẫu, JaCoCo ≥ 70% cho `nutrition.service`; Vitest + RTL + msw

**Target Platform**: Linux server (Docker Compose); trình duyệt ≥ 360 px

**Project Type**: Web application (backend REST + SPA)

**Performance Goals**: 95% công thức mới có số trong 5 phút (NFR-001); GET nutrition/allergen là đọc
DB thuần (p95 < 200 ms); thời gian publish không đổi khi AI tắt (SC-005)

**Constraints**: Gọi AI một lần mỗi lần duyệt, không retry, không job quét (Clarifications); chỉ server
gọi AI (FR-003); không dữ liệu cá nhân trong prompt (FR-004); output AI không tin cậy (NFR-005); chung
hạn mức ngày với 031 (FR-020); $0 free tier

**Scale/Scope**: ~1.000 công thức seed + vài công thức mới/ngày; 1 instance

Không còn NEEDS CLARIFICATION – xem [research.md](research.md) R1–R19. Prompt lập plan dùng tên bảng của
bản nháp cũ (`ai_usage`, `nutrition_attempts`, `recipe_dietary_tags`, status `HIDDEN`); plan theo
full-schema (research R1).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Ghi chú |
|---|---|---|
| I. Spec trước, code sau | ⚠️ Có điều kiện | Spec còn **Draft**, chưa có mã Jira. Không implement trước khi spec được duyệt và branch có mã Jira. |
| II. Test-First | ✅ | `NutritionValidatorTest` từng luật; response LLM mẫu; Testcontainers (không H2); JaCoCo ≥ 70%; mỗi acceptance scenario có test (SC-008). |
| III. Kiến trúc phân lớp | ✅ | Controller → Service → Repository; DTO record; module khác dùng qua port/facade/event; không thư viện mới. |
| IV. Bảo mật & dữ liệu | ✅ | Key từ env (031); prompt không có dữ liệu cá nhân; output AI qua validator, render dạng text; `/api/v1/admin/**` cần `ROLE_ADMIN`; SQL tham số hóa; ProblemDetail không lộ lỗi provider; `hidden_reason` validate độ dài. |
| V. Trải nghiệm nhất quán | ✅ | Disclaimer cố định trong mọi trạng thái và cạnh mọi số calo (FR-016); 360 px; chuỗi trong string store; danh sách admin phân trang. |
| Ràng buộc kỹ thuật | ✅ | Java 21, Spring Boot 3, PostgreSQL 16 + Flyway, `/api/v1`, OpenAPI, LLM gọi từ backend. |

**Kết quả gate**: PASS (điều kiện Nguyên tắc I phải xong trước `/speckit-implement`).

**Re-check sau Phase 1**: PASS – không thêm bảng ngoài full-schema (dùng `diet_labels`, `is_hidden`,
`ai_requests`); migration V303 theo cách tách của 031 R18; API public chỉ đọc; công cụ admin chỉ đưa
việc vào hàng đợi.

## Project Structure

### Documentation (this feature)

```text
specs/090-nutrition-analysis/
├── plan.md              # File này
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/
│   ├── openapi.yaml     # REST /api/v1/recipes/{id}/nutrition|allergens, /api/v1/admin/nutrition
│   └── integration.md   # ai (031), event 060, DayNutritionPort (030), CalorieFilterSql (020), component
└── tasks.md             # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
src/backend/
└── src/
    ├── main/java/com/wikicook/
    │   ├── ai/quota/        AiRequestRetentionJob (dọn ai_requests, research R16)
    │   └── nutrition/
    │       ├── api/         CalorieFilterSql, NutritionVisibilitySql (cho 020)
    │       ├── controller/  RecipeNutritionController, NutritionAdminController,
    │       │                NutritionExceptionHandler
    │       ├── service/     NutritionTriggerListener, NutritionQueue, NutritionWorker,
    │       │                EstimationService, EstimationInputBuilder, Fingerprint,
    │       │                NutritionPromptBuilder, NutritionValidator, NutritionReadService,
    │       │                AllergenService, NutritionAdminService, NutritionDayTotals
    │       │                (cài DayNutritionPort), InterruptedEstimatesCleaner
    │       ├── port/        AuditLogPort + JdbcAuditLogWriter (mặc định)
    │       ├── repository/  RecipeNutritionRepository, EstimationInputQuery, AllergenQuery,
    │       │                AdminEstimateQuery
    │       ├── domain/      RecipeNutrition, NutritionStatus, DietLabel, FailureReason
    │       └── dto/         RecipeNutritionDto, RecipeAllergensDto, EstimateRow, AttemptDto, HideRequest
    ├── main/resources/
    │   ├── db/migration/V303__recipe_nutrition.sql
    │   └── prompts/nutrition.v1.txt
    └── test/
        ├── java/com/wikicook/nutrition/   unit, Testcontainers IT, ReferenceAccuracyEvaluation
        └── resources/nutrition/           llm-responses/*.json, reference-dishes.csv

src/frontend/src/
├── api/nutritionApi.ts
└── features/nutrition/
    ├── components/  NutritionBlock, NutritionDisclaimer, AllergenArea, AiDietLabels, CalorieValue
    ├── admin/       NutritionAdminPage, HideDialog, AttemptHistory
    ├── strings.vi.ts
    └── __tests__/
```

**Structure Decision**: Web application `src/backend` + `src/frontend` (đã chốt 2026-10-18). Logic dinh
dưỡng ở `com.wikicook.nutrition`; gọi AI và hạn mức dùng `com.wikicook.ai` của 031. Frontend là feature
folder riêng; trang 060, 020, 030 và khu admin 100 nhúng component theo contracts/integration.md.

## Phases (tóm tắt để /speckit-tasks chia việc)

| Thứ tự | Nội dung | User story |
|---|---|---|
| 1 | V303, entity/repository, `Fingerprint`, `EstimationInputBuilder`, string store, kiểm tra package `ai` | nền tảng |
| 2 | `NutritionValidator` + response mẫu | US3 (P1) |
| 3 | Listener AFTER_COMMIT, `NutritionQueue`/worker, `EstimationService`, quota, restart → INTERRUPTED | US2 (P1) |
| 4 | `GET …/nutrition`, `NutritionBlock` 4 trạng thái + disclaimer | US1 (P1) |
| 5 | `GET …/allergens`, `AllergenArea`, `AiDietLabels`, highlight | US4 (P1) |
| 6 | Fingerprint đổi / không đổi, STALE, dedupe | US5 (P2) |
| 7 | `DayNutritionPort`, `CalorieFilterSql`, `CalorieValue` | US6 (P2) |
| 8 | API + trang admin, recalculate, hide/unhide, audit, 403 | US7 (P2) |
| 9 | SC-006 với LLM thật, bảo mật, 360 px/a11y, retention job | polish |

Thứ tự US3 → US2 → US1: validator là điều kiện để lưu bất kỳ kết quả nào; US1 cần dữ liệu do US2 tạo.

## Complexity Tracking

| Bổ sung | Lý do cần | Phương án đơn giản hơn bị loại vì |
|---|---|---|
| `NutritionQueue` (`PriorityBlockingQueue` + 1 worker + giãn cách) thay vì `@Async` thuần | Seed import 1.000 món không được chặn publish thường (NFR-001) và không vượt RPM free tier | `@Async` + thread pool – không ưu tiên, không giãn cách, dễ đụng 429; job queue trong DB – thêm bảng và vòng poll (R3) |
| `AiRequestRetentionJob` trong package `ai` | NFR-009 (≥ 30 ngày) + full-schema (dọn 30/90 ngày) cho cả 031 và 090 | Mỗi spec một job – trùng; không dọn – bảng lớn dần (R16) |
| `JdbcAuditLogWriter` mặc định cho `AuditLogPort` | FR-026 khi 010 chưa cài port | Chờ 010 – chặn US7 |
