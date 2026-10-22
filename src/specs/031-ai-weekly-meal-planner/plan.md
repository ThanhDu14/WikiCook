# Implementation Plan: AI Weekly Meal Planner

**Branch**: `feature/SCRUM-xx-ai-weekly-meal-planner` *(TODO: mã Jira)* | **Date**: 2026-10-18 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/031-ai-weekly-meal-planner/spec.md`

## Summary

Từ lịch của spec 030, người dùng bấm "Tạo bằng AI" để điền các ô trống của tuần đang xem bằng công
thức PUBLISHED của WikiCook. Kết quả hiện thành bản nháp trên cùng lịch. Người dùng sửa, tạo lại,
bỏ, hoặc xác nhận; xác nhận thì lưu vào kế hoạch và mở flow shopping list. Ngoài ra picker của 030
có danh sách "Gợi ý cho bạn" cho từng ô.

Cách làm: module `com.wikicook.aiplanner` cộng package dùng chung `com.wikicook.ai` (LLM, embedding,
hạn mức – 090 dùng lại). Pipeline RAG cố định gồm 5 bước:
1. Lọc hard rule bằng SQL (dị ứng, loại trừ, kiểu ăn suy từ `ingredients.kind`, thời gian, PUBLISHED,
   không phải món ăn vặt, hợp bữa).
2. Truy hồi top-K bằng pgvector trên `recipe_embeddings`.
3. LLM (Gemini Flash, JSON mode) xếp các ô.
4. Kiểm tra lại từng pick; sai số ô hoặc JSON hỏng thì retry một lần.
5. Fallback greedy tất định ≤ 2 s khi AI lỗi, quá 60 s hoặc hết hạn mức ngày.

Giới hạn 5 lượt/giờ và hạn mức ngày đếm trên bảng `ai_requests` (cũng là log FR-032). Draft chỉ ở
client; confirm ghi batch qua `MealPlanWriteFacade` của 030. Gợi ý một ô chỉ dùng truy hồi, không
gọi LLM. Embedding được cập nhật theo event của module công thức, cộng job đối soát hằng đêm.

## Technical Context

**Language/Version**: Java 21 (backend, virtual threads), TypeScript 5 + React 18 (frontend)

**Primary Dependencies**: Spring Boot 3.x (Web, Data JPA, Security, Validation, Scheduling, Cache),
springdoc-openapi, Flyway, `com.pgvector:pgvector` (kiểu `PGvector` cho JDBC); `RestClient` của
Spring gọi Gemini REST (không SDK); frontend dùng lại component của 030

**Storage**: PostgreSQL 16 + pgvector; bảng `recipe_embeddings`, `ai_requests`
(`V302__ai_requests_embeddings.sql`); ghi `meal_plan_items` qua facade của 030

**Testing**: JUnit 5 + Mockito, Testcontainers `pgvector/pgvector:pg16`, `FakeLlmClient`,
`FakeEmbeddingClient`, WireMock (chỉ cho test adapter Gemini), JaCoCo; Vitest + RTL + msw

**Target Platform**: Linux server (Docker Compose); trình duyệt desktop/mobile ≥ 360 px

**Project Type**: Web application (backend REST + SPA)

**Performance Goals**: 90% bản nháp AI ≤ 30 s, tối đa 60 s (NFR-001); bản nháp dự phòng ≤ 2 s
(SC-002a); gợi ý ≤ 3 s cho 90% lần mở picker (SC-008)

**Constraints**: Chỉ backend gọi AI, key không tới trình duyệt (NFR-002); không gửi dữ liệu cá nhân
(NFR-003); 0 món vi phạm hard rule (NFR-004); AI lỗi không ảnh hưởng tính năng khác (NFR-006); $0
(free tier); 5 lượt/người/giờ + hạn mức ngày cấu hình được

**Scale/Scope**: 1.000–10.000 công thức PUBLISHED, tối đa 21 ô/lượt, candidate ≤ 90/lượt; vài chục
lượt tạo/ngày (đồ án môn học)

Không còn mục NEEDS CLARIFICATION – xem [research.md](research.md) R1–R18. Provider và model
embedding là **đề xuất**, chờ nhóm xác nhận (research mục cuối).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Ghi chú |
|---|---|---|
| I. Spec trước, code sau | ⚠️ Có điều kiện | Spec còn **Draft**, chưa có mã Jira (giống 030). Không implement trước khi spec được duyệt và branch có mã Jira. |
| II. Test-First | ✅ | Fake LLM/embedding cho mọi nhánh lỗi; Testcontainers pgvector (không H2); JaCoCo ≥ 70% cho `aiplanner.service`, `ai`; mỗi acceptance scenario có test (SC-009). |
| III. Kiến trúc phân lớp | ✅ | Controller → Service → Repository; DTO record; ghi meal plan chỉ qua `MealPlanWriteFacade`; thư viện mới ghi ở Complexity Tracking. |
| IV. Bảo mật & dữ liệu | ✅ | `WIKICOOK_AI_API_KEY` từ env, không log, `.env.example` chỉ có tên biến; chỉ server gọi AI; prompt không có tên/email/id người dùng; output AI coi là không tin cậy và luôn qua `DraftValidator`; SQL tham số hóa; ProblemDetail không lộ chi tiết lỗi provider. |
| V. Trải nghiệm nhất quán | ✅ | Disclaimer trong dialog, trên draft và dạng ngắn trong gợi ý (FR-028, FR-035); dùng lại lưới/picker/dialog của 030; tiếng Việt trong string store; 360 px. |
| Ràng buộc kỹ thuật | ✅ | Java 21, Spring Boot 3, PostgreSQL 16 + Flyway, `/api/v1`, OpenAPI, LLM gọi từ backend. |

**Kết quả gate**: PASS (điều kiện Nguyên tắc I phải xong trước `/speckit-implement`).

**Re-check sau Phase 1**: PASS – không thêm bảng ngoài full-schema; tách migration V302/V303 (R18)
cần cập nhật full-schema mục 7; contracts chỉ dùng `/api/v1/ai-planner/**`; ranh giới module giữ qua
facade/port/event.

## Project Structure

### Documentation (this feature)

```text
specs/031-ai-weekly-meal-planner/
├── plan.md              # File này
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/
│   ├── openapi.yaml     # REST /api/v1/ai-planner
│   └── integration.md   # Package ai, event 060, facade 030, frontend
└── tasks.md             # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
src/backend/
└── src/
    ├── main/java/com/wikicook/
    │   ├── ai/                       # dùng chung 031 + 090
    │   │   ├── llm/        LlmClient, LlmRequest, LlmResult, GeminiLlmClient, MockLlmClient,
    │   │   │               OfflineLlmClient, CancellationToken
    │   │   ├── embedding/  EmbeddingClient, GeminiEmbeddingClient, MockEmbeddingClient
    │   │   ├── quota/      AiQuotaService, AiRequest (entity), AiRequestRepository,
    │   │   │               AiFeature, AiTrigger, AiOutcome
    │   │   └── config/     AiProperties, AiClientConfig (chọn bean theo wikicook.ai.mode)
    │   ├── common/diet/    DietRuleSql
    │   └── aiplanner/
    │       ├── controller/ AiPlannerController, AiPlannerExceptionHandler
    │       ├── service/    GenerationService (điều phối 5 bước), GenerationOptionsService,
    │       │               CandidateService, PromptBuilder, DraftValidator, BackupPlanner,
    │       │               ConfirmService, SuggestionService, InFlightGenerations
    │       ├── embedding/  RecipeEmbeddingSync, EmbeddingReconcileJob, EmbeddingTextBuilder
    │       ├── repository/ RecipeEmbeddingRepository, EligibleRecipeQuery, CandidateQuery
    │       ├── domain/     RecipeEmbedding
    │       └── dto/        GenerateRequest, GenerationConstraints, DraftResponse, ConfirmRequest, ...
    ├── main/resources/
    │   ├── db/migration/V302__ai_requests_embeddings.sql
    │   └── prompts/meal-plan.v1.txt
    └── test/java/com/wikicook/{ai,aiplanner}/   unit, Testcontainers IT, FakeLlmClient

src/frontend/src/
├── api/aiPlannerApi.ts
└── features/ai-planner/
    ├── components/  AiGenerateButton, AiGenerateDialog, GenerationProgress, DraftBar, SlotSuggestions,
    │                NotEnoughRecipesMessage, AiDisclaimer
    ├── hooks/       useAiDraft, useLeaveGuard
    ├── register.ts  đăng ký vào aiPlannerEntry + suggestionsSlot của 030
    ├── strings.vi.ts
    └── __tests__/
```

**Structure Decision**: Web application `src/backend` + `src/frontend` (đã chốt 2026-10-18). Phần gọi
AI tách ra `com.wikicook.ai` vì 090 dùng chung; nghiệp vụ AI planner ở `com.wikicook.aiplanner`;
frontend là feature folder riêng, gắn vào trang của 030 qua registry và props đã định ở 030
contracts/integration.md.

## Phases (tóm tắt để /speckit-tasks chia việc)

| Thứ tự | Nội dung | User story |
|---|---|---|
| 1 | Package `ai` (LLM/embedding client 3 chế độ, `AiQuotaService`), migration V302, `DietRuleSql` | nền tảng |
| 2 | Options + generate: lọc → truy hồi → prompt → validate → draft; dialog, progress, draft trên lịch | US1 (P1) |
| 3 | Sửa draft, xác nhận, bỏ, tạo lại, chặn rời trang | US2 (P1) |
| 4 | Loại pick sai, ô "Chọn món", retry một lần | US3 (P1) |
| 5 | NOT_ENOUGH_RECIPES + gợi ý nới ràng buộc | US4 (P2) |
| 6 | 5 lượt/giờ, hạn mức ngày, hiển thị lượt còn lại | US5 (P2) |
| 7 | BackupPlanner, timeout 60 s, hủy | US6 (P2) |
| 8 | Gợi ý một ô | US7 (P2) |
| 9 | Đồng bộ embedding theo event + job đêm | US8 (P3) |
| 10 | Đánh giá với LLM thật, bảo mật, hiệu năng | polish |

## Complexity Tracking

| Bổ sung | Lý do cần | Phương án đơn giản hơn bị loại vì |
|---|---|---|
| Package dùng chung `com.wikicook.ai` (ngoài cấu trúc 1 package/tính năng) | 031 và 090 dùng chung client, hạn mức ngày và bảng `ai_requests` | Mỗi spec tự gọi AI – trùng code, không kiểm soát được hạn mức chung (R1) |
| `com.pgvector:pgvector` (backend) | Đọc/ghi `VECTOR(768)` qua JDBC | Tự chuyển `float[]` ↔ chuỗi `'[…]'` – dễ sai, không có kiểu cho tham số |
| WireMock (test dependency) | Test adapter `GeminiLlmClient`/`GeminiEmbeddingClient` map HTTP, timeout, lỗi 429/5xx | Gọi API thật trong CI – cần key và mạng; chỉ dùng fake – không kiểm tra phần HTTP (R17) |
| Map `InFlightGenerations` in-memory | Hủy lượt đang chạy (FR-023) và chặn 2 lượt cùng lúc | Job queue trong DB – thừa khi draft chỉ ở client và chỉ có 1 instance (R9) |
| Pipeline 5 bước (filter → retrieve → LLM → validate → fallback) | Bắt buộc bởi spec (FR-008a, FR-013, FR-027) và proposal 4.4 | Gọi LLM thẳng với toàn bộ catalogue – vượt context, không bảo đảm hard rule |
