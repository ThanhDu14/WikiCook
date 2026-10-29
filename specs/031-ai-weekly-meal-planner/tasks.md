---

description: "Task list for 031 AI Weekly Meal Planner"
---

# Tasks: AI Weekly Meal Planner

**Input**: Design documents from `specs/031-ai-weekly-meal-planner/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/openapi.yaml](contracts/openapi.yaml),
[contracts/integration.md](contracts/integration.md), [quickstart.md](quickstart.md)

**Tests**: BẮT BUỘC (constitution Principle II, spec SC-009). Trong mỗi phase, task test đứng
trước task code và **phải chạy FAIL trước** khi viết code. Mỗi task test ghi mã acceptance scenario
(`US1-3`…) hoặc edge case mà nó phủ.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- **⛓ 030 / B / D / Nhóm**: task chạm điểm nối với spec 030, với B (060 – event công thức), D (040 –
  route import) hoặc quyết định chung của nhóm (provider/model). Làm trước bằng fake/mock
  (`wikicook.ai.mode=mock`) để không bị chặn.

## Path Conventions

Backend là Maven multi-module (`src/backend/pom.xml`, cập nhật 2026-10-28). Đường dẫn
`src/backend/src/...` ghi trong từng task đọc theo bảng sau:

| Ghi trong task | Đường dẫn thật |
|---|---|
| `src/backend/src/{main,test}/java/com/wikicook/ai/**` | `src/backend/wikicook-ai/src/{main,test}/java/com/wikicook/ai/**` |
| `src/backend/src/{main,test}/java/com/wikicook/{aiplanner,common/diet}/**` | `src/backend/wikicook-app/src/{main,test}/java/com/wikicook/...` (module chưa có – Tech Lead thêm cùng skeleton v0.5) |
| `src/backend/src/main/resources/db/migration/` | `src/backend/wikicook-db/src/main/resources/db/migration/` (module chỉ chứa migration; `wikicook-ai` dùng ở scope test) |
| `src/backend/src/main/resources/application.yml` | `src/backend/wikicook-app/src/main/resources/application.yml` |
| `src/backend/pom.xml` (dependency) | pom của module chứa code đó; version đặt ở `dependencyManagement` của `src/backend/pom.xml` |
| `./mvnw verify` | `cd src/backend && mvn verify` (chưa có Maven wrapper) |

- `wikicook-ai` không được phụ thuộc `wikicook-app`, và hiện chỉ có `spring-boot-starter-jdbc` (không JPA):
  code trong `ai/quota` dùng `JdbcTemplate` thay cho entity/`JpaRepository` nếu không thêm JPA vào module.
- Frontend: `src/frontend/src/features/ai-planner/` (test trong `__tests__/`)
- Tiền điều kiện: skeleton Build v0.5 và spec 030 tới hết Phase 5 (MVP 030) + các task 030 nêu ở
  mục Dependencies.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Dependency, cấu hình, khung thư mục, cổng chất lượng

- [ ] T001 *(một phần: đã có `ai/llm`, `ai/embedding`, `ai/config` trong wikicook-ai; còn `ai/quota` và các package của wikicook-app)* Tạo các package rỗng `ai/llm`, `ai/embedding`, `ai/quota`, `ai/config`, `common/diet`, `aiplanner/{controller,service,embedding,repository,domain,dto}` trong src/backend/src/main/java/com/wikicook/ và các package test tương ứng trong src/backend/src/test/java/com/wikicook/
- [x] T002 [P] *(xong: `pgvector` (compile) và `wiremock-standalone` 3.13.2 (test) trong wikicook-ai, version ở `dependencyManagement`)* Thêm `com.pgvector:pgvector` (compile) và `org.wiremock:wiremock-standalone` (test) vào src/backend/pom.xml – lý do ở plan.md Complexity Tracking
- [ ] T003 [P] *(một phần: `AiProperties` đã có mode, api-key, llm, embedding với giá trị mặc định; còn `daily-cap.*`, `.env.example`, `application.yml` của wikicook-app)* Thêm `WIKICOOK_AI_MODE=mock`, `WIKICOOK_AI_API_KEY=` (để trống, chỉ tên biến) và `WIKICOOK_AI_EMBEDDING_RECONCILE_ON_STARTUP=false` vào .env.example; thêm khối `wikicook.ai` (mode, llm.model, llm.timeout 60s, embedding.model `gemini-embedding-001`, embedding.dimensions 768, daily-cap.total, daily-cap.meal-planner-share 0.7, daily-cap.nutrition-share 0.3, đọc từ env) và `spring.threads.virtual.enabled: true` vào src/backend/src/main/resources/application.yml
- [x] T004 [P] *(xong cho wikicook-ai: JaCoCo `check` ≥ 70% dòng và nhánh trên cả module; phần `aiplanner.service` làm khi có wikicook-app)* Thêm rule JaCoCo `check` (LINE COVEREDRATIO ≥ 0.70) cho package `com.wikicook.aiplanner.service` và `com.wikicook.ai`, gắn vào phase `verify`, trong src/backend/pom.xml – để CI chặn ngay từ đầu
- [ ] T005 [P] Tạo thư mục src/frontend/src/features/ai-planner/{components,hooks,__tests__}
- [ ] T006 [P] Bật `@EnableAsync`, `@EnableScheduling`, `@EnableCaching` trong src/backend/src/main/java/com/wikicook/ai/config/AiAsyncConfig.java (executor riêng `aiTaskExecutor` dùng virtual thread)

**Checkpoint**: `./mvnw verify` và `npm test` chạy được; rule JaCoCo đã có hiệu lực.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema AI, package `ai` dùng chung, luật hard rule, fake cho test, xử lý lỗi, API client

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Tests cho Foundational (viết trước, phải FAIL)

- [ ] T007 [P] Integration test `AiSchemaIT` (extends `AbstractPostgresIT` của 030) – V302 chạy được; bị từ chối khi: `feature` ngoài `('MEAL_PLANNER','NUTRITION')`, `trigger` ngoài `('USER','APPROVAL','REVISION','IMPORT','ADMIN_RECALC')`, `outcome` ngoài `('AI_DRAFT','AUTO_DRAFT','NOT_ENOUGH','SUCCESS','FAILED','REJECTED','TIMEOUT','CANCELLED','QUOTA_CAP')`, vi phạm `CHECK (counted = (feature = 'MEAL_PLANNER' AND outcome = 'AI_DRAFT'))`, `MEAL_PLANNER` thiếu `user_id` hoặc `week_start`, `NUTRITION` thiếu `recipe_id`; `recipe_embeddings.embedding` sai số chiều (≠ 768) bị từ chối; xóa recipe → dòng embedding bị xóa (`CASCADE`); xóa user → dòng `ai_requests` của user bị xóa trong src/backend/src/test/java/com/wikicook/ai/quota/AiSchemaIT.java
- [ ] T008 [P] Integration test `AiQuotaServiceIT` – `reserve` ghi dòng `outcome = 'FAILED'`, `counted = false`; `finish` cập nhật outcome/called_llm/removed_picks/reason; hạn mức ngày tính từ 00:00 giờ VN theo phần cấu hình của feature; 2 thread `reserve` đồng thời khi còn đúng 1 chỗ → chỉ 1 `llmAllowed = true` (advisory lock); `userAllowance`: chỉ đếm dòng `counted` trong 60 phút trượt (dòng 61 phút trước không tính), `nextAvailableAt` = dòng cũ nhất + 60 phút trong src/backend/src/test/java/com/wikicook/ai/quota/AiQuotaServiceIT.java
- [ ] T009 [P] Integration test `EligibleRecipeQueryIT` với seed – loại công thức có nguyên liệu gắn dị ứng (`ingredient_allergens`), có nguyên liệu bị loại trừ; VEGAN loại DAIRY/EGG/HONEY; VEGETARIAN loại món có nước mắm (`kind = 'SEAFOOD'`); PESCATARIAN loại MEAT nhưng giữ SEAFOOD; `total_time_minutes <= maxCookTime`; chỉ `status = 'PUBLISHED'`; loại `is_snack`; lọc theo `for_breakfast/for_lunch/for_dinner`; công thức có `recipe_nutrition.diet_labels = {VEGAN}` nhưng chứa DAIRY vẫn bị loại (FR-009a); trả số hợp lệ theo từng bữa trong src/backend/src/test/java/com/wikicook/common/diet/EligibleRecipeQueryIT.java
- [ ] T010 [P] Test `GeminiLlmClientTest` (WireMock) – body có `responseMimeType: application/json` và `responseSchema`; key gửi ở header, không xuất hiện trong log (bắt log bằng `OutputCaptureExtension`); quá timeout → `AiUnavailableException`; HTTP 429/5xx → `AiUnavailableException`; `CancellationToken.cancel()` đóng request đang chờ trong src/backend/src/test/java/com/wikicook/ai/llm/GeminiLlmClientTest.java ⛓ Nhóm
- [ ] T011 [P] Test `GeminiEmbeddingClientTest` (WireMock) – gửi `outputDimensionality: 768` và task type `RETRIEVAL_DOCUMENT`/`RETRIEVAL_QUERY`; `embedBatch` gom request; vector trả về đúng 768 phần tử; lỗi → `AiUnavailableException` trong src/backend/src/test/java/com/wikicook/ai/embedding/GeminiEmbeddingClientTest.java ⛓ Nhóm
- [ ] T012 [P] *(một phần: `AiClientConfigTest` đã phủ mock/offline và thiếu key ở live; còn live chọn đúng bean Gemini)* Test `AiClientConfigTest` – `wikicook.ai.mode=live|mock|offline` chọn đúng bean; `offline` luôn ném `AiUnavailableException`; thiếu key ở chế độ `live` → ứng dụng không khởi động, thông báo không chứa giá trị key trong src/backend/src/test/java/com/wikicook/ai/config/AiClientConfigTest.java
- [ ] T013 [P] Web slice test `AiPlannerExceptionHandlerTest` – map đúng `status` + `code` của research R16: 400 `VALIDATION_ERROR`, 422 `PAST_WEEK`/`NO_EMPTY_SLOT`/`NOT_ENOUGH_RECIPES` (kèm `eligibleCount`, `requestedSlots`, `suggestions`), 409 `GENERATION_IN_PROGRESS`, 429 `RATE_LIMITED` (kèm `nextAvailableAt` và header `Retry-After`), 503 `BACKUP_UNAVAILABLE`/`SUGGESTIONS_UNAVAILABLE`; không lộ thông điệp lỗi của provider trong src/backend/src/test/java/com/wikicook/aiplanner/controller/AiPlannerExceptionHandlerTest.java
- [ ] T014 [P] Test `aiPlannerApi.test.ts` (msw) – mỗi hàm gọi đúng method/URL của contracts/openapi.yaml; `generate` truyền `AbortSignal`; `cancel` gọi `DELETE /api/v1/ai-planner/generations/{requestId}`; `getSuggestions` hết 3 s hoặc nhận 503 → trả `null` không ném lỗi trong src/frontend/src/features/ai-planner/__tests__/aiPlannerApi.test.ts

### Implementation cho Foundational

- [x] T015 *(xong: `wikicook-db/.../V302__ai_requests_embeddings.sql`, `ExtensionsMigrationIT` chạy hết V001–V303; T007 vẫn phải viết)* Viết migration src/backend/src/main/resources/db/migration/V302__ai_requests_embeddings.sql đúng data-model.md mục 1: `recipe_embeddings(recipe_id BIGINT PK REFERENCES recipes ON DELETE CASCADE, embedding VECTOR(768) NOT NULL, model VARCHAR(50) NOT NULL, content_hash CHAR(64) NOT NULL, created_at, updated_at)` + `CREATE INDEX … USING hnsw (embedding vector_cosine_ops)`; `ai_requests(id BIGINT GENERATED ALWAYS AS IDENTITY PK, feature VARCHAR(15) NOT NULL CHECK IN ('MEAL_PLANNER','NUTRITION'), user_id BIGINT NULL REFERENCES users ON DELETE CASCADE, recipe_id BIGINT NULL REFERENCES recipes ON DELETE CASCADE, actor_id BIGINT NULL REFERENCES users ON DELETE SET NULL, trigger VARCHAR(15) NOT NULL CHECK IN (…), outcome VARCHAR(15) NOT NULL CHECK IN (…), called_llm BOOLEAN NOT NULL, counted BOOLEAN NOT NULL, week_start DATE NULL, requested_slots SMALLINT NULL, removed_picks SMALLINT NULL, constraints JSONB NULL, reason VARCHAR(100) NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now())` + 3 CHECK của data-model.md + index `(user_id, created_at DESC) WHERE counted`, `(feature, created_at) WHERE called_llm`, `(recipe_id, created_at DESC)` (làm T007 xanh)
- [ ] T016 [P] Tạo enum `AiFeature`, `AiTrigger`, `AiOutcome`, entity `AiRequest` (chỉ `created_at`, `constraints` map JSONB bằng `@JdbcTypeCode(SqlTypes.JSON)`) và `AiRequestRepository` trong src/backend/src/main/java/com/wikicook/ai/quota/
- [ ] T017 *(một phần: `AiProperties` + `AiClientConfig` có rồi; còn validate `daily-cap`/share và nhánh `live` tạo bean Gemini)* Tạo `AiProperties` (`@ConfigurationProperties("wikicook.ai")`, validate model/timeout/share) và `AiClientConfig` chọn bean theo `mode` trong src/backend/src/main/java/com/wikicook/ai/config/ (làm T012 xanh)
- [ ] T018 [P] *(một phần: interface, `MockLlmClient`, `OfflineLlmClient` có rồi; còn `GeminiLlmClient`)* Tạo `LlmClient`, `LlmRequest`, `LlmResult`, `CancellationToken`, `AiUnavailableException`, và cài `GeminiLlmClient` (`RestClient`, read timeout = thời gian còn lại, JSON mode), `MockLlmClient` (trả plan hợp lệ từ candidate trong prompt), `OfflineLlmClient` trong src/backend/src/main/java/com/wikicook/ai/llm/ (làm T010 xanh) ⛓ Nhóm
- [ ] T019 [P] *(một phần: interface, `MockEmbeddingClient`, `OfflineEmbeddingClient` có rồi; còn `GeminiEmbeddingClient`)* Tạo `EmbeddingClient`, `EmbeddingTask` và cài `GeminiEmbeddingClient`, `MockEmbeddingClient` (vector tất định từ hash), `OfflineEmbeddingClient` trong src/backend/src/main/java/com/wikicook/ai/embedding/ (làm T011 xanh) ⛓ Nhóm
- [ ] T020 Cài `AiQuotaService` (`reserve` trong transaction có `pg_advisory_xact_lock(hashtext(feature))`, `finish`, `userAllowance`, ngày tính theo `Asia/Ho_Chi_Minh`) trong src/backend/src/main/java/com/wikicook/ai/quota/AiQuotaService.java (làm T008 xanh)
- [ ] T021 Tạo `DietRuleSql` (mệnh đề `NOT EXISTS` cho dị ứng qua `ingredient_allergens`, nguyên liệu loại trừ, và `ingredients.kind` cấm theo kiểu ăn: VEGETARIAN → MEAT, SEAFOOD; VEGAN → MEAT, SEAFOOD, EGG, DAIRY, HONEY; PESCATARIAN → MEAT) trong src/backend/src/main/java/com/wikicook/common/diet/DietRuleSql.java ⛓ 030 (030 T041 nên dùng lại lớp này)
- [ ] T022 Cài `EligibleRecipeQuery` (`NamedParameterJdbcTemplate`, `status = 'PUBLISHED' AND NOT is_snack`, lọc bữa, thời gian, `DietRuleSql`; trả id hợp lệ theo bữa) trong src/backend/src/main/java/com/wikicook/aiplanner/repository/EligibleRecipeQuery.java (làm T009 xanh)
- [ ] T023 [P] Tạo entity `RecipeEmbedding` và `RecipeEmbeddingRepository` (upsert native với `PGvector`, xóa theo recipe, tìm dòng lệch hash/model, tìm công thức PUBLISHED chưa có embedding) trong src/backend/src/main/java/com/wikicook/aiplanner/domain/RecipeEmbedding.java và repository/RecipeEmbeddingRepository.java
- [ ] T024 [P] Tạo test fixture `FakeLlmClient` (hàng đợi phản hồi theo kịch bản: JSON hợp lệ, id lạ, id ARCHIVED, id có dị ứng, thiếu ô, JSON hỏng, ngủ N giây, ném lỗi; ghi lại prompt đã nhận và cờ hủy), `FakeEmbeddingClient` và `RecipeSeed` (dựng ≥ 60 công thức đủ bữa, ẩm thực, dị ứng, `is_snack`, 1 ARCHIVED) trong src/backend/src/test/java/com/wikicook/ai/testing/ và src/backend/src/test/java/com/wikicook/aiplanner/support/RecipeSeed.java
- [ ] T025 Tạo exception (`PastWeekException`, `NoEmptySlotException`, `NotEnoughRecipesException`, `RateLimitedException`, `GenerationInProgressException`, `BackupUnavailableException`, `SuggestionsUnavailableException`) và `AiPlannerExceptionHandler` (ProblemDetail + `code`, header `Retry-After`) trong src/backend/src/main/java/com/wikicook/aiplanner/controller/ (làm T013 xanh)
- [ ] T026 [P] Tạo type TS khớp contracts/openapi.yaml (`GenerationOptions`, `GenerationConstraints`, `GenerateRequest`, `DraftResponse`, `DraftItem`, `ConfirmRequest`, `ConfirmResult`, `Suggestion`) trong src/frontend/src/features/ai-planner/types.ts và `aiPlannerApi.ts` dùng client chung trong src/frontend/src/api/aiPlannerApi.ts (làm T014 xanh)
- [ ] T027 [P] Tạo string store tiếng Việt: disclaimer nguyên văn "Thực đơn do AI tạo dựa trên hồ sơ của bạn, hãy tự kiểm tra dị ứng và cân bằng dinh dưỡng" và dạng ngắn, "Tạo tự động, không dùng AI", "AI đã hết lượt hôm nay", "Chọn món", gợi ý nới ràng buộc, thông báo theo `code` trong src/frontend/src/features/ai-planner/strings.vi.ts

**Checkpoint**: V302 chạy trên Testcontainers; T007–T014 xanh; chưa có endpoint nghiệp vụ.

---

## Phase 3: User Story 1 - Generate a week of meals from my profile (Priority: P1) 🎯 MVP

**Goal**: Dialog ràng buộc điền sẵn từ hồ sơ; bấm "Tạo" chạy lọc → truy hồi → LLM và hiện bản nháp nhãn "AI" chỉ ở các ô trống được chọn.

**Independent Test**: Tài khoản dị ứng đậu phộng, 4 người; ≥ 60 công thức; tuần sau rỗng → "Tạo bằng AI" → chấp nhận mặc định → trong 60 s có bản nháp 21 ô, không đậu phộng, không trùng, mỗi món 4 khẩu phần (quickstart A1–A4).

### Tests for User Story 1 ⚠️ (viết trước, phải FAIL)

- [ ] T028 [P] [US1] Unit test `GenerationOptionsServiceTest` (`Clock` cố định) – giá trị mặc định lấy từ `DietaryProfilePort` (US1-1); household null → `servings = null` ("Theo công thức"); tuần hiện tại chỉ chọn được từ hôm nay, tuần sau chọn được cả 7 ngày, tuần đã qua → `selectableDays` rỗng (US1-2, edge case tuần đã qua); `emptySlots` lấy từ `MealPlanWriteFacade` và bỏ các bữa của hôm nay đã qua giờ (BREAKFAST sau 10:00, LUNCH sau 14:00, DINNER sau 20:00 giờ VN, cấu hình `wikicook.ai-planner.meal-cutoffs`) – Chủ nhật 21:00 → không còn ô (edge case); `profileIncomplete` khi hồ sơ không có dị ứng/loại trừ/kiểu ăn (US1-8); `remaining`, `aiAvailableToday` từ `AiQuotaService` trong src/backend/src/test/java/com/wikicook/aiplanner/service/GenerationOptionsServiceTest.java
- [ ] T029 [P] [US1] Integration test `CandidateQueryIT` (`FakeEmbeddingClient`) – mỗi bữa lấy `k = max(3 × số ô của bữa, 20)` theo khoảng cách cosine, tổng ≤ 90; công thức hợp lệ chưa có embedding được thêm cuối; `EmbeddingClient` lỗi → xếp bằng SQL (ẩm thực ưa thích → `rating_avg` → `id`) và không ném lỗi trong src/backend/src/test/java/com/wikicook/aiplanner/repository/CandidateQueryIT.java
- [ ] T030 [P] [US1] Unit test `PromptBuilderTest` – prompt chứa danh sách ô và candidate dạng `id | tên | bữa | phút | ẩm thực | nguyên liệu chính`; tên dài cắt còn 80 ký tự, bỏ xuống dòng; có JSON schema `{"slots":[{"day","meal","recipeId"}]}`; **không** chứa tên, email hay id người dùng (NFR-003) trong src/backend/src/test/java/com/wikicook/aiplanner/service/PromptBuilderTest.java
- [ ] T031 [P] [US1] Unit test `GenerationServiceTest` (đường AI thành công, Mockito) – gọi theo đúng thứ tự lọc → truy hồi → LLM → kiểm tra; chỉ điền ô trống được chọn, món người dùng đã xếp (kể cả món có dị ứng) giữ nguyên (US1-5, FR-007, edge case); món có `source = AI` và servings theo ràng buộc (US1-5); không trùng công thức, món sáng ở bữa sáng, ≤ thời gian tối đa (US1-6); ràng buộc sửa trong dialog không ghi vào hồ sơ (US1-3, FR-004 – `DietaryProfilePort` chỉ được đọc); `finish(AI_DRAFT, calledLlm = true)` trong src/backend/src/test/java/com/wikicook/aiplanner/service/GenerationServiceTest.java
- [ ] T032 [P] [US1] Integration test `AiPlannerControllerUS1IT` (MockMvc + Testcontainers + `FakeLlmClient`) – `GET …/options` 200 đúng schema; `POST …/generations` 200 `mode = AI`; 400 khi `days` rỗng, `meals` rỗng, `servings` 0 hoặc 21, `maxCookTimeMinutes` 5 (edge case); tuần đã qua → 422 `PAST_WEEK`; tuần đầy → 422 `NO_EMPTY_SLOT` (edge case); không token → 401; `ai_requests.constraints` không có email/tên (NFR-003) trong src/backend/src/test/java/com/wikicook/aiplanner/controller/AiPlannerControllerUS1IT.java
- [ ] T033 [P] [US1] Test `AiGenerateDialog.test.tsx` – điền sẵn dị ứng, loại trừ, kiểu ăn, khẩu phần, thời gian, ẩm thực (US1-1); mặc định chọn mọi ngày còn lại và cả 3 bữa, bỏ chọn được (US1-2); bỏ chọn hết ngày hoặc hết bữa → "Tạo" bị tắt (edge case); khẩu phần có lựa chọn "Theo công thức"; hồ sơ trống → nhắc hoàn thiện hồ sơ kèm link (US1-8); có disclaimer (US1-7); hiện số lượt còn lại trong src/frontend/src/features/ai-planner/__tests__/AiGenerateDialog.test.tsx
- [ ] T034 [P] [US1] Test `GenerationProgress.test.tsx` – hiện trạng thái đang tạo dễ hiểu (không có chữ kỹ thuật) cho tới khi có bản nháp; nút hủy gọi `onCancel` (US1-4) trong src/frontend/src/features/ai-planner/__tests__/GenerationProgress.test.tsx
- [ ] T035 [P] [US1] Test `useAiDraft.test.ts` – tạo draft từ `DraftResponse`; chuyển thành `draftItems` cho `WeekGrid` với nhãn "AI"; không ghi `localStorage`/`sessionStorage` trong src/frontend/src/features/ai-planner/__tests__/useAiDraft.test.ts
- [ ] T036 [P] [US1] Test `DraftBar.test.tsx` – disclaimer luôn hiện trên bản nháp (US1-7, FR-028); hiện nhãn chế độ AI trong src/frontend/src/features/ai-planner/__tests__/DraftBar.test.tsx
- [ ] T037 [P] [US1] Test `AiGenerateButton.test.tsx` (msw cho `GET …/options`, `Clock` cố định) – tuần có 2 món và còn ô trống → nút bật, bấm mở `AiGenerateDialog` với `weekStart` (FR-001); `emptySlots` rỗng → nút tắt kèm gợi ý "Tuần này không còn ô trống" (edge case tuần đầy); tuần đã qua (`selectableDays` rỗng) → không render nút (edge case tuần đã qua); Chủ nhật 21:00 tuần hiện tại, `emptySlots` rỗng → nút tắt kèm gợi ý hết bữa hôm nay (edge case); `GET …/options` lỗi 5xx hoặc mất mạng → không render nút, trang 030 vẫn dùng được (NFR-006) trong src/frontend/src/features/ai-planner/__tests__/AiGenerateButton.test.tsx

### Implementation for User Story 1

- [ ] T038 [P] [US1] Tạo DTO record theo data-model.md mục 3: `GenerationConstraints` (`@Min(1) @Max(20) Integer servings` nullable, `@Min(10) Integer maxCookTimeMinutes` nullable, list id không null), `GenerateRequest` (`@NotNull UUID requestId`, `@NotEmpty @Size(max = 7) Set<DayOfWeek> days`, `@NotEmpty @Size(max = 3) Set<Meal> meals`, `@Valid @NotNull constraints`), `DraftItemDto`, `DraftResponse`, `GenerationOptions`, `SlotKeyDto` trong src/backend/src/main/java/com/wikicook/aiplanner/dto/
- [ ] T039 [US1] Cài `GenerationOptionsService` (dùng `DietaryProfilePort`, `MealPlanWriteFacade.emptySlots`, `WeekCalendar`, meal cut-off, `AiQuotaService`) trong src/backend/src/main/java/com/wikicook/aiplanner/service/GenerationOptionsService.java (làm T028 xanh) ⛓ 030 (T020, T100)
- [ ] T040 [US1] Cài `CandidateQuery` (pgvector `ORDER BY embedding <=> :q LIMIT :k` trong tập hợp lệ của bữa, phần đuôi cho món chưa có embedding, nhánh SQL khi không có vector) và `CandidateService` (dựng câu truy vấn theo bữa, không chứa dữ liệu cá nhân, embed với `QUERY`) trong src/backend/src/main/java/com/wikicook/aiplanner/repository/CandidateQuery.java và service/CandidateService.java (làm T029 xanh)
- [ ] T041 [US1] Cài `PromptBuilder` và mẫu prompt src/backend/src/main/resources/prompts/meal-plan.v1.txt (vai trò, luật cứng, ưu tiên hợp bữa/đa dạng/dùng lại nguyên liệu/ẩm thực, JSON schema) trong src/backend/src/main/java/com/wikicook/aiplanner/service/PromptBuilder.java (làm T030 xanh)
- [ ] T042 [US1] Cài `GenerationService.generate` đường AI: kiểm tra tuần/ô (`PastWeekException`, `NoEmptySlotException`) → `AiQuotaService.reserve` → `EligibleRecipeQuery` → `CandidateService` → `LlmClient` (timeout 60 s) → parse pick → `DraftResponse` → `finish(AI_DRAFT)`; lỗi AI tạm thời ném `BackupUnavailableException` (US6 thay bằng fallback) trong src/backend/src/main/java/com/wikicook/aiplanner/service/GenerationService.java (làm T031 xanh)
- [ ] T043 [US1] Cài `AiPlannerController` (`GET /api/v1/ai-planner/weeks/{weekStart}/options`, `POST …/generations`, `@MondayDate` của 030, `@Valid`, OpenAPI annotation) trong src/backend/src/main/java/com/wikicook/aiplanner/controller/AiPlannerController.java (làm T032 xanh)
- [ ] T044 [US1] Cài hook `useAiDraft(weekStart)` (state draft, map sang `draftItems`) trong src/frontend/src/features/ai-planner/hooks/useAiDraft.ts (làm T035 xanh)
- [ ] T045 [P] [US1] Cài `AiGenerateButton`, `AiGenerateDialog`, `GenerationProgress`, `AiDisclaimer`, `DraftBar` (bản đầu: disclaimer + nhãn chế độ) trong src/frontend/src/features/ai-planner/components/ (làm T033, T034, T036, T037 xanh)
- [ ] T046 [US1] Tạo `register.ts`: đăng ký `AiGenerateButton` vào `aiPlannerEntry` và truyền `draftItems` của `useAiDraft` vào `WeekGrid`/`DayView` của trang Meal Planner trong src/frontend/src/features/ai-planner/register.ts ⛓ 030 (T075, T102)

**Checkpoint**: Independent Test ở trên đạt với `mode=mock`; quickstart A1–A4.

---

## Phase 4: User Story 2 - Review, adjust and confirm the draft (Priority: P1)

**Goal**: Sửa bản nháp bằng công cụ của 030, Xác nhận (lưu + mở shopping list), Bỏ, Tạo lại, hỏi trước khi rời trang.

**Independent Test**: Tạo bản nháp, chuyển một món, thay một món, xóa một món → "Xác nhận" → lịch đúng các thay đổi sau khi tải lại, flow shopping list của tuần đó mở ra (quickstart A10–A14).

### Tests for User Story 2 ⚠️ (viết trước, phải FAIL)

- [ ] T047 [P] [US2] Unit test `ConfirmServiceTest` – gọi `MealPlanWriteFacade.saveDraft` với đúng item, giữ `source` AI/AUTO/MANUAL và servings (US2-2); trả `saved`, `skippedOccupied` (US2-6), `skippedUnavailable` (edge case công thức bị archive giữa lúc tạo và lúc xác nhận); không gọi `LlmClient` và không ghi `ai_requests` trong src/backend/src/test/java/com/wikicook/aiplanner/service/ConfirmServiceTest.java
- [ ] T048 [P] [US2] Integration test `AiPlannerControllerUS2IT` – `POST …/confirm` 200 và `GET /api/v1/meal-plans/{weekStart}` của 030 thấy món với `source = AI`; thiết bị khác điền một ô trước → `skippedOccupied = 1` và ô giữ món của thiết bị kia (US2-6); hai tab xác nhận lần lượt → tab sau bị bỏ qua các ô đã có (edge case draft ở 2 tab); cùng công thức ở ô tự xếp và ô AI được phép (edge case); `items` rỗng hoặc > 21 → 400 trong src/backend/src/test/java/com/wikicook/aiplanner/controller/AiPlannerControllerUS2IT.java ⛓ 030 (T100)
- [ ] T049 [P] [US2] Test `useAiDraft.edit.test.ts` – move/swap/thay/xóa trong draft chỉ đổi state, không gọi API meal plan (US2-1, FR-017); món thay bằng picker có `source = MANUAL`; "Bỏ" trả tuần về đúng như trước (US2-3); "Tạo lại" mở dialog với ràng buộc lần trước, kết quả mới thay draft, món tự xếp không đổi (US2-5) trong src/frontend/src/features/ai-planner/__tests__/useAiDraft.edit.test.ts
- [ ] T050 [P] [US2] Test `useLeaveGuard.test.tsx` – có draft thì đăng ký `beforeunload`; đổi route hoặc đổi tuần → hỏi xác nhận qua `useBlocker`, Hủy thì ở lại (US2-4, FR-022); 401 khi có draft → draft mất, chuyển `/login` giữ tuần (edge case phiên hết hạn); mất mạng → draft mất, hiện thông báo offline của 030 (edge case) trong src/frontend/src/features/ai-planner/__tests__/useLeaveGuard.test.tsx
- [ ] T051 [P] [US2] Test `DraftBar.confirm.test.tsx` – "Xác nhận" gọi confirm, báo "{n} món bị bỏ qua" khi có, tải lại tuần, điều hướng `/shopping-list/import?weekStart=…` (US2-2, US2-6) trong src/frontend/src/features/ai-planner/__tests__/DraftBar.confirm.test.tsx

### Implementation for User Story 2

- [ ] T052 [US2] Tạo `ConfirmRequest` (`@NotNull UUID requestId`, `@NotEmpty @Size(max = 21) List<@Valid Item> items`, mỗi item `servings` `@Min(1) @Max(20)` nullable), `ConfirmResult`, cài `ConfirmService` và endpoint `POST /api/v1/ai-planner/weeks/{weekStart}/confirm` trong src/backend/src/main/java/com/wikicook/aiplanner/service/ConfirmService.java, dto/ và controller/AiPlannerController.java (làm T047, T048 xanh) ⛓ 030 (T100)
- [ ] T053 [US2] Thêm move/swap/thay/xóa/bỏ/tạo lại vào `useAiDraft` (nối `onDraftChange` của 030, mở `RecipePicker` cho thay thế) trong src/frontend/src/features/ai-planner/hooks/useAiDraft.ts (làm T049 xanh)
- [ ] T054 [US2] Cài `useLeaveGuard` (`beforeunload` + `useBlocker`, xử lý 401/offline) trong src/frontend/src/features/ai-planner/hooks/useLeaveGuard.ts (làm T050 xanh)
- [ ] T055 [US2] Thêm nút Xác nhận / Tạo lại / Bỏ vào `DraftBar` và điều hướng sang flow import sau khi xác nhận trong src/frontend/src/features/ai-planner/components/DraftBar.tsx (làm T051 xanh) ⛓ D (route `/shopping-list/import`)

**Checkpoint**: US1 + US2 chạy độc lập; quickstart A10–A14.

---

## Phase 5: User Story 3 - Never show unsafe or non-existent dishes (Priority: P1)

**Goal**: Mọi pick sai bị loại trước khi người dùng thấy, ô tương ứng thành "Chọn món"; kết quả sai số ô hoặc JSON hỏng thì retry một lần.

**Independent Test**: AI trả 1 công thức không tồn tại, 1 ARCHIVED, 1 chứa dị ứng → 3 ô trống "Chọn món", 3 công thức đó không xuất hiện ở đâu (quickstart A5).

### Tests for User Story 3 ⚠️ (viết trước, phải FAIL)

- [ ] T056 [P] [US3] Unit test `DraftValidatorTest` – loại id không thuộc candidate, id không tồn tại (US3-1), id không còn PUBLISHED (US3-1), id vi phạm dị ứng/loại trừ/kiểu ăn khi kiểm tra lại (US3-2); trùng công thức giữ lần đầu theo ngày → bữa (FR-011); thiếu ô, thừa ô, ô không được yêu cầu, JSON hỏng → `RETRY`; đếm `removedPicks` đúng trong src/backend/src/test/java/com/wikicook/aiplanner/service/DraftValidatorTest.java
- [ ] T057 [P] [US3] Unit test `GenerationServiceRetryTest` – lần 1 `RETRY` → gọi LLM lần 2 kèm tin nhắn sửa lỗi; lần 2 hợp lệ → `AI_DRAFT`; chỉ retry khi còn ≥ 15 s; `chooseSlots` và `removedPicks` có trong response (US3-1, US3-2, US3-4) trong src/backend/src/test/java/com/wikicook/aiplanner/service/GenerationServiceRetryTest.java
- [ ] T058 [P] [US3] Integration test `DraftSafetyIT` (Testcontainers + `FakeLlmClient`) – chèn 3 pick xấu → 3 ô trong `chooseSlots`, không công thức nào trong 3 công thức đó có trong `items` (SC-003, NFR-004); `ai_requests.removed_picks = 3` (FR-032) trong src/backend/src/test/java/com/wikicook/aiplanner/service/DraftSafetyIT.java
- [ ] T059 [P] [US3] Test `ChooseSlot.test.tsx` – ô "Chọn món" hiện nhãn; bấm mở `RecipePicker` của 030 cho đúng ô (US3-3); `DraftBar` báo "{n} ô cần bạn tự chọn" (US3-4) trong src/frontend/src/features/ai-planner/__tests__/ChooseSlot.test.tsx

### Implementation for User Story 3

- [ ] T060 [US3] Cài `DraftValidator` (parse JSON, so tập ô, kiểm tra candidate, đọc lại trạng thái + `DietRuleSql`, khử trùng) trong src/backend/src/main/java/com/wikicook/aiplanner/service/DraftValidator.java (làm T056 xanh)
- [ ] T061 [US3] Gắn `DraftValidator` và retry một lần (ngân sách thời gian) vào `GenerationService` trong src/backend/src/main/java/com/wikicook/aiplanner/service/GenerationService.java (làm T057, T058 xanh)
- [ ] T062 [US3] Hiển thị ô "Chọn món" qua `draftItems` của 030 và thông báo số ô trong `DraftBar`; chọn món cho ô đó cập nhật draft trong src/frontend/src/features/ai-planner/components/DraftBar.tsx và hooks/useAiDraft.ts (làm T059 xanh) ⛓ 030 (T047, T102)

**Checkpoint**: MVP (US1 + US2 + US3) hoàn chỉnh: tạo → kiểm tra → xác nhận an toàn; quickstart A1–A5, A10–A14.

---

## Phase 6: User Story 4 - Not enough suitable recipes (Priority: P2)

**Goal**: Khi số công thức hợp lệ của một bữa ít hơn số ô của bữa đó, không tạo bản nháp, báo rõ và gợi ý nới ràng buộc; không tính lượt.

**Independent Test**: Ràng buộc chỉ khớp ít công thức hơn số ô → thông báo kèm gợi ý, lượt còn lại không đổi (quickstart A15).

### Tests for User Story 4 ⚠️ (viết trước, phải FAIL)

- [ ] T063 [P] [US4] Unit test `GenerationServiceNotEnoughTest` – bữa nào có số hợp lệ < số ô của bữa đó → `NotEnoughRecipesException` (US4-1), kể cả khi số hợp lệ > 0 (edge case); `suggestions`: `INCREASE_MAX_COOK_TIME` khi có thời gian tối đa, `REMOVE_PREFERRED_CUISINES` khi có ẩm thực ưa thích, `SELECT_FEWER_SLOTS` luôn có (US4-2, FR-024); không gọi LLM, `finish(NOT_ENOUGH, calledLlm = false)`, không tính lượt (US4-3) trong src/backend/src/test/java/com/wikicook/aiplanner/service/GenerationServiceNotEnoughTest.java
- [ ] T064 [P] [US4] Integration test `NotEnoughRecipesIT` – 422 `NOT_ENOUGH_RECIPES` với `eligibleCount`, `requestedSlots`, `suggestions`; `remaining` không đổi trong src/backend/src/test/java/com/wikicook/aiplanner/controller/NotEnoughRecipesIT.java
- [ ] T065 [P] [US4] Test `NotEnoughRecipesMessage.test.tsx` – hiện lý do và từng gợi ý bằng tiếng Việt, nút quay lại dialog giữ ràng buộc trong src/frontend/src/features/ai-planner/__tests__/NotEnoughRecipesMessage.test.tsx

### Implementation for User Story 4

- [ ] T066 [US4] Thêm kiểm tra số hợp lệ theo bữa (sau bước lọc, trước truy hồi) và dựng `suggestions` trong src/backend/src/main/java/com/wikicook/aiplanner/service/GenerationService.java (làm T063, T064 xanh)
- [ ] T067 [US4] Cài `NotEnoughRecipesMessage` và hiển thị trong `AiGenerateDialog` khi nhận 422 trong src/frontend/src/features/ai-planner/components/NotEnoughRecipesMessage.tsx (làm T065 xanh)

**Checkpoint**: US4 chạy độc lập; quickstart A15.

---

## Phase 7: User Story 5 - Fair use limit (Priority: P2)

**Goal**: 5 lượt được tính trong 60 phút trượt, hiện lượt còn lại và thời điểm có lượt mới; chỉ bản nháp AI được tính; một người chỉ chạy một lượt cùng lúc.

**Independent Test**: 5 lượt thành công trong một giờ → lượt 6 bị từ chối kèm thời điểm; lỗi AI không trừ lượt (quickstart A16).

### Tests for User Story 5 ⚠️ (viết trước, phải FAIL)

- [ ] T068 [P] [US5] Integration test `RateLimitIT` (`FakeLlmClient`, `Clock` điều khiển được) – `GET …/options` trả `remaining` (US5-1); 5 lượt `AI_DRAFT` (kể cả "Tạo lại") → lượt 6 trả 429 `RATE_LIMITED` với `nextAvailableAt` và `Retry-After` (US5-2, US5-3, SC-007); lượt lỗi, quá giờ, bị hủy, `NOT_ENOUGH`, bản nháp `AUTO` không làm giảm `remaining` (US5-4, FR-026); sau 60 phút từ lượt cũ nhất lại có lượt trong src/backend/src/test/java/com/wikicook/aiplanner/controller/RateLimitIT.java
- [ ] T069 [P] [US5] Unit test `InFlightGenerationsTest` – người dùng đã có một lượt đang chạy → lượt thứ hai ném `GenerationInProgressException`; lượt kết thúc (thành công/lỗi/hủy) thì không còn chặn lượt mới, nhưng được giữ 10 phút trong danh sách "vừa xong" (`requestId`, dòng `ai_requests`, cờ đã confirm) rồi tự xóa trong src/backend/src/test/java/com/wikicook/aiplanner/service/InFlightGenerationsTest.java
- [ ] T070 [P] [US5] Test `AiGenerateDialog.limit.test.tsx` – `remaining = 0` → "Tạo" bị tắt, hiện giờ có lượt mới (US5-2); nhận 429 khi bấm → cập nhật cùng trạng thái đó trong src/frontend/src/features/ai-planner/__tests__/AiGenerateDialog.limit.test.tsx

### Implementation for User Story 5

- [ ] T071 [US5] Kiểm tra `AiQuotaService.userAllowance` trước khi `reserve` và ném `RateLimitedException` (kèm `nextAvailableAt`); trả `remaining`/`nextAvailableAt` trong `DraftResponse` trong src/backend/src/main/java/com/wikicook/aiplanner/service/GenerationService.java (làm T068 xanh)
- [ ] T072 [US5] Cài `InFlightGenerations` (`ConcurrentHashMap<userId, InFlight(requestId, Future, CancellationToken)>` + danh sách "vừa xong" giữ 10 phút theo research R9, `markConfirmed(requestId)` gọi từ `ConfirmService`) và dùng trong `GenerationService` trong src/backend/src/main/java/com/wikicook/aiplanner/service/InFlightGenerations.java (làm T069 xanh)
- [ ] T073 [US5] Hiển thị lượt còn lại / giờ có lượt mới và xử lý 429 trong `AiGenerateDialog` trong src/frontend/src/features/ai-planner/components/AiGenerateDialog.tsx (làm T070 xanh)

**Checkpoint**: US5 chạy độc lập; quickstart A16.

---

## Phase 8: User Story 6 - AI unavailable: backup draft without AI (Priority: P2)

**Goal**: AI lỗi, quá 60 s, trả kết quả không dùng được hai lần hoặc hết hạn mức ngày → bản nháp tự động ≤ 2 s, nhãn "Auto", không tính lượt; người dùng hủy được khi đang chờ.

**Independent Test**: Tắt AI (`mode=offline`) → trong 2 s có bản nháp "Tạo tự động, không dùng AI", mọi món qua hard rule, không trùng, lượt còn lại không đổi (quickstart A6–A9, A17).

### Tests for User Story 6 ⚠️ (viết trước, phải FAIL)

- [ ] T074 [P] [US6] Unit test `BackupPlannerTest` – kết quả tất định với cùng đầu vào; chỉ chọn món hợp bữa; không trùng công thức; ẩm thực ưa thích được ưu tiên; món dùng lại nguyên liệu được cộng điểm; phạt cùng danh mục với ngày trước; hết candidate của bữa → ô vào `chooseSlots` (US6-5); 21 ô × 90 candidate chạy < 50 ms trong src/backend/src/test/java/com/wikicook/aiplanner/service/BackupPlannerTest.java
- [ ] T075 [P] [US6] Unit test `GenerationServiceFallbackTest` – LLM ném lỗi → `AUTO_DRAFT`, `notice = AUTO_NO_AI`, món `source = AUTO` (US6-1, US6-3); hai lần `RETRY` → fallback (US3-5, FR-014); quá ngân sách 60 s → `TIMEOUT` + bản nháp AUTO (US6-2, FR-023); hết hạn mức ngày → không gọi LLM, `QUOTA_CAP`, `notice = AI_DAILY_CAP` (FR-025a, edge case); không lượt nào trong số này được tính (US6-4); bước lọc cũng lỗi → `BackupUnavailableException` (US6-6) trong src/backend/src/test/java/com/wikicook/aiplanner/service/GenerationServiceFallbackTest.java
- [ ] T076 [P] [US6] Integration test `OfflineModeIT` (SC-002a) – `wikicook.ai.mode=offline`, tuần trống 21 ô: thời gian response ≤ 2 s, 0 món vi phạm hard rule (đối chiếu `EligibleRecipeQuery`), 0 công thức trùng, `remaining` không đổi trong src/backend/src/test/java/com/wikicook/aiplanner/controller/OfflineModeIT.java
- [ ] T077 [P] [US6] Integration test `CancelGenerationIT` – `FakeLlmClient` ngủ 30 s; `DELETE /api/v1/ai-planner/generations/{requestId}` → request generate kết thúc, `CancellationToken` của LLM đã bị hủy, `outcome = CANCELLED`, không tính lượt (US1-4, US5-4); lượt đã xong với `AI_DRAFT`, chưa confirm, trong 10 phút → `DELETE` đổi thành `CANCELLED`, `counted = false`, `called_llm` vẫn `true`, `remaining` tăng lại (edge case mất mạng); đã confirm hoặc quá 10 phút → 204 nhưng vẫn tính lượt; `requestId` của người khác → không đổi gì; `DELETE` id không tồn tại → 204 trong src/backend/src/test/java/com/wikicook/aiplanner/controller/CancelGenerationIT.java
- [ ] T078 [P] [US6] Test `GenerationProgress.cancel.test.tsx` – bấm hủy → `AbortController.abort()` + gọi `cancel`, quay lại dialog; mất mạng khi đang tạo → hiện thông báo offline của 030, giữ `requestId` trong bộ nhớ; khi có `online` event → gọi `cancel(requestId)` đúng một lần, không ghi `localStorage`/`sessionStorage` (edge case "không tính lượt") trong src/frontend/src/features/ai-planner/__tests__/GenerationProgress.cancel.test.tsx
- [ ] T079 [P] [US6] Test `DraftBar.auto.test.tsx` – `mode = AUTO` hiện "Tạo tự động, không dùng AI" và nhãn "Auto" trên món (US6-3); `AI_DAILY_CAP` hiện "AI đã hết lượt hôm nay"; 503 `BACKUP_UNAVAILABLE` → thông báo gợi ý thử lại sau hoặc tự lên kế hoạch, tuần không đổi (US6-6) trong src/frontend/src/features/ai-planner/__tests__/DraftBar.auto.test.tsx

### Implementation for User Story 6

- [ ] T080 [US6] Cài `BackupPlanner` (greedy có điểm theo research R8) trong src/backend/src/main/java/com/wikicook/aiplanner/service/BackupPlanner.java (làm T074 xanh)
- [ ] T081 [US6] Thay nhánh lỗi tạm của T042 bằng fallback: ngân sách 60 s cho cả lượt, hết hạn mức ngày → fallback không gọi LLM, ghi đúng `outcome`/`called_llm` trong src/backend/src/main/java/com/wikicook/aiplanner/service/GenerationService.java (làm T075, T076 xanh)
- [ ] T082 [US6] Thêm endpoint `DELETE /api/v1/ai-planner/generations/{requestId}` gọi `InFlightGenerations.cancel` (chỉ hủy lượt của chính người gọi; lượt "vừa xong" `AI_DRAFT` chưa confirm → `AiQuotaService.finish(…, CANCELLED, …, "CLIENT_LOST")`) trong src/backend/src/main/java/com/wikicook/aiplanner/controller/AiPlannerController.java (làm T077 xanh)
- [ ] T083 [US6] Nối hủy (`AbortController` + `cancel`) vào `GenerationProgress`, gọi `cancel` khi có mạng lại sau khi mất mạng lúc đang tạo, hiển thị notice AUTO/AI_DAILY_CAP và lỗi `BACKUP_UNAVAILABLE` trong `DraftBar` trong src/frontend/src/features/ai-planner/components/GenerationProgress.tsx và DraftBar.tsx (làm T078, T079 xanh)

**Checkpoint**: US6 chạy độc lập; quickstart A6–A9, A17.

---

## Phase 9: User Story 7 - Suggestions for one slot (Priority: P2)

**Goal**: Picker của 030 hiện "Gợi ý cho bạn" (tối đa 5 món hợp bữa, theo hồ sơ) chỉ bằng truy hồi, không gọi LLM, không tính lượt.

**Independent Test**: Hồ sơ dị ứng tôm, ưa món Việt; thứ Hai sáng đã có "Phở bò"; mở picker từ thứ Ba sáng → ≤ 3 s hiện ≤ 5 món sáng, không tôm, không "Phở bò"; mở 10 lần không tốn lượt (quickstart A18–A20).

### Tests for User Story 7 ⚠️ (viết trước, phải FAIL)

- [ ] T084 [P] [US7] Unit test `SuggestionServiceTest` – tối đa 5 món, đều hợp bữa của ô (US7-1); không vi phạm hard rule, không trùng món đã có trong tuần (mock `MealPlanWriteFacade.plannedRecipeIds`) hay trong `exclude` của draft (US7-2, edge case gợi ý khi có draft); xếp ẩm thực ưa thích → độ gần vector → phạt cùng danh mục; `reason` dạng "Món sáng · Việt Nam · 20 phút" (US7-3); ít hơn 5 → trả đủ số có, không có → danh sách rỗng (US7-6); `verifyNoInteractions(llmClient)` và không ghi `ai_requests` (FR-033, FR-036) trong src/backend/src/test/java/com/wikicook/aiplanner/service/SuggestionServiceTest.java
- [ ] T085 [P] [US7] Integration test `SuggestionsIT` – 200 đúng schema; embedding lỗi hoặc chậm > 2,5 s → 503 `SUGGESTIONS_UNAVAILABLE` (US7-7); `wikicook.ai.mode` LLM offline nhưng embedding chạy → vẫn có gợi ý (FR-037); lần gọi thứ hai cùng câu truy vấn không gọi `EmbeddingClient` (cache); người dùng đã hết 5 lượt/giờ vẫn nhận gợi ý (US7-5, edge case) trong src/backend/src/test/java/com/wikicook/aiplanner/controller/SuggestionsIT.java
- [ ] T086 [P] [US7] Test `SlotSuggestions.test.tsx` – render phía trên kết quả của picker (US7-1); mỗi món có lý do ngắn và danh sách có disclaimer dạng ngắn (US7-3); chọn món → thêm như lựa chọn picker với `source = AI` (US7-4, FR-038); quá 3 s hoặc 503 → ẩn, không báo lỗi (US7-7); rỗng → "Chưa có gợi ý phù hợp" (US7-6); truyền `exclude` là id món trong draft trong src/frontend/src/features/ai-planner/__tests__/SlotSuggestions.test.tsx

### Implementation for User Story 7

- [ ] T087 [US7] Cài `SuggestionService` (tập hợp lệ của bữa trừ `MealPlanWriteFacade.plannedRecipeIds` và `exclude`, xếp hạng, `reason`, cache embedding câu truy vấn bằng `@Cacheable("plannerQueryEmbeddings")`, timeout 2,5 s) trong src/backend/src/main/java/com/wikicook/aiplanner/service/SuggestionService.java (làm T084 xanh) ⛓ 030 (T100)
- [ ] T088 [US7] Thêm endpoint `GET /api/v1/ai-planner/weeks/{weekStart}/suggestions` (`day`, `meal`, `exclude` tối đa 21 id) trong src/backend/src/main/java/com/wikicook/aiplanner/controller/AiPlannerController.java (làm T085 xanh)
- [ ] T089 [US7] Cài `SlotSuggestions` và đăng ký vào `suggestionsSlot` của `RecipePicker` trong `register.ts` trong src/frontend/src/features/ai-planner/components/SlotSuggestions.tsx và register.ts (làm T086 xanh) ⛓ 030 (T047)

**Checkpoint**: US7 chạy độc lập; quickstart A18–A20.

---

## Phase 10: User Story 8 - Keep the AI's recipe choices up to date (Priority: P3)

**Goal**: Công thức mới/sửa được AI dùng trong ≤ 24 h; công thức bị archive không bao giờ xuất hiện; lỗi embedding không chặn publish.

**Independent Test**: Publish công thức mới, chạy job đối soát, tạo với ràng buộc chỉ món đó hợp → được chọn; archive → không bao giờ được chọn nữa (quickstart A21, A22).

### Tests for User Story 8 ⚠️ (viết trước, phải FAIL)

- [ ] T090 [P] [US8] Unit test `EmbeddingTextBuilderTest` – văn bản gồm tên, danh mục, ẩm thực, bữa, tổng thời gian, tên nguyên liệu; `content_hash` (SHA-256, 64 ký tự hex) ổn định với cùng nội dung, đổi khi nguyên liệu đổi trong src/backend/src/test/java/com/wikicook/aiplanner/embedding/EmbeddingTextBuilderTest.java
- [ ] T091 [P] [US8] Integration test `RecipeEmbeddingSyncIT` – sau commit `RecipePublishedEvent` → có dòng embedding (US8-1); cùng hash và model → không gọi `EmbeddingClient`; `RecipeRevisionApprovedEvent` đổi nội dung → cập nhật (US8-2); `RecipeArchivedEvent` → xóa dòng; `EmbeddingClient` lỗi → transaction publish vẫn commit, không có dòng embedding (US8-4, FR-031) trong src/backend/src/test/java/com/wikicook/aiplanner/embedding/RecipeEmbeddingSyncIT.java ⛓ B
- [ ] T092 [P] [US8] Integration test `EmbeddingReconcileJobIT` – tạo embedding cho công thức PUBLISHED thiếu dòng, lệch `content_hash` hoặc lệch `model`; xóa dòng của công thức không còn PUBLISHED; xử lý theo lô 50; xóa dòng `ai_requests` `MEAL_PLANNER` cũ hơn 90 ngày trong src/backend/src/test/java/com/wikicook/aiplanner/embedding/EmbeddingReconcileJobIT.java
- [ ] T093 [P] [US8] Integration test `ArchivedRecipeNeverChosenIT` – công thức đã ARCHIVED nhưng còn dòng embedding không bao giờ có trong draft, kể cả khi `FakeLlmClient` chọn nó (US8-3, FR-030); công thức mới PUBLISHED chưa có embedding vẫn được chọn qua phần đuôi candidate (US8-1) trong src/backend/src/test/java/com/wikicook/aiplanner/service/ArchivedRecipeNeverChosenIT.java

### Implementation for User Story 8

- [ ] T094 [P] [US8] Cài `EmbeddingTextBuilder` trong src/backend/src/main/java/com/wikicook/aiplanner/embedding/EmbeddingTextBuilder.java (làm T090 xanh)
- [ ] T095 [US8] Cài `RecipeEmbeddingSync` nghe `RecipePublishedEvent`, `RecipeRevisionApprovedEvent`, `RecipeArchivedEvent`, `RecipesImportedEvent` bằng `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async("aiTaskExecutor")`, nuốt và log lỗi (không có key/nội dung) trong src/backend/src/main/java/com/wikicook/aiplanner/embedding/RecipeEmbeddingSync.java (làm T091 xanh) ⛓ B (B publish 4 event theo contracts/integration.md mục 2; trước đó test dùng event tự publish)
- [ ] T096 [US8] Cài `EmbeddingReconcileJob` (`@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Ho_Chi_Minh")`, lô 50, nghỉ giữa lô, cờ `wikicook.ai.embedding.reconcile-on-startup`) và dọn `ai_requests` > 90 ngày trong src/backend/src/main/java/com/wikicook/aiplanner/embedding/EmbeddingReconcileJob.java (làm T092, T093 xanh)

**Checkpoint**: Mọi user story chạy độc lập; quickstart A21, A22.

---

## Phase 11: Polish & Cross-Cutting Concerns

**Purpose**: Chất lượng với LLM thật, bảo mật, hiệu năng, truy vết, tài liệu

- [ ] T097 [P] Đánh giá 20 lượt tạo với `WIKICOOK_AI_MODE=live` trên bộ seed đầy đủ: tỉ lệ ô được điền (SC-004 ≥ 90%), thời gian (NFR-001: 90% ≤ 30 s), số pick bị loại; ghi vào docs/test/031-ai-planner-evaluation.md ⛓ Nhóm (cần key và provider đã chốt)
- [ ] T098 [P] Kiểm tra bảo mật theo quickstart mục 5 (không có request trình duyệt → provider, không có key trong `src/frontend/dist`, `ai_requests.constraints` và log không có email/tên) và ghi vào docs/test/031-ai-planner-security.md (NFR-002, NFR-003)
- [ ] T099 [P] Viết script k6: `GET …/suggestions` p90 ≤ 3 s (SC-008) và `POST …/generations` ở `mode=mock` dưới 20 VU, trong src/backend/perf/ai-planner.k6.js
- [ ] T100 [P] So sánh OpenAPI do springdoc sinh với specs/031-ai-weekly-meal-planner/contracts/openapi.yaml, sửa chỗ lệch
- [ ] T101 Chạy quickstart A1–A24 (specs/031-ai-weekly-meal-planner/quickstart.md) và ghi kết quả vào docs/test/031-ai-planner-acceptance.md
- [ ] T102 [P] Lập ma trận truy vết acceptance scenario (US1-1 … US8-4) và edge case → test trong docs/test/031-ai-planner-traceability.md (SC-009)
- [ ] T103 [P] Cập nhật docs/analysis-and-design/database/full-schema.md mục 7 (tách `V302__ai_requests_embeddings.sql` / `V303__recipe_nutrition.sql`) sau khi nhóm đồng ý (research R18)
- [ ] T104 [P] Đồng bộ specs/031-ai-weekly-meal-planner sang src/specs/031-ai-weekly-meal-planner theo src/README.md mục 5

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: cần skeleton Build v0.5.
- **Foundational (Phase 2)**: sau Setup – CHẶN mọi story. T007 dùng `AbstractPostgresIT` (030 T004); T015 cần `V202` (recipes) và `V301` (meal plans).
- **US1 (Phase 3)**: sau Foundational và các điểm nối 030 bên dưới.
- **US2, US3 (Phase 4, 5)**: sau US1; độc lập với nhau (US2 thêm confirm/sửa draft, US3 thêm validator). Cùng sửa `GenerationService.java`/`useAiDraft.ts` nên làm tuần tự nếu một người.
- **US4, US5 (Phase 6, 7)**: sau US1; độc lập với nhau.
- **US6 (Phase 8)**: sau US3 (fallback thay nhánh "hai lần RETRY") và US5 (`InFlightGenerations` cho hủy).
- **US7 (Phase 9)**: chỉ cần Foundational + `CandidateService` (T040); có thể làm song song với US2–US6.
- **US8 (Phase 10)**: chỉ cần Foundational; có thể làm sớm để có embedding thật cho demo.
- **Polish (Phase 11)**: sau các story muốn giao.

### Phụ thuộc vào spec 030

| 031 task | Cần từ 030 | Ghi chú |
|---|---|---|
| T007, T009 … (mọi IT) | 030 T004 `AbstractPostgresIT` | |
| T028, T039 | 030 T020 `DietaryProfilePort`, 030 T019 `WeekCalendar` | |
| T039, T047, T048, T052, T087 | 030 T100 `MealPlanWriteFacade` | Có thể mock interface trong unit test trước khi 030 T100 xong |
| T046, T062 | 030 T102 `WeekGrid`/`DayView` `draftItems` + `onDraftChange` | |
| T046 | 030 T075 registry `aiPlannerEntry` | |
| T062, T089 | 030 T047 `RecipePicker` `suggestionsSlot` | |
| T021 | 030 T041 | 030 nên chuyển sang dùng `DietRuleSql` để hai bên một luật |

### Phụ thuộc người khác (⛓) – không chặn nhờ fake/mock

| Task | Người | Tạm thời | Khi bên kia xong |
|---|---|---|---|
| T010, T011, T018, T019, T097 | Nhóm (provider/model) | `wikicook.ai.mode=mock` | Điền model thật vào cấu hình, chạy T097 |
| T091, T095 | B (060) | Test tự publish event | B publish 4 event từ `RecipeService` |
| T055 | D (040) | Route placeholder `/shopping-list/import` | D cài flow import |
| T039, T046, T048, T052, T062, T087, T089 | C (030) | Mock interface / prop | Dùng cài đặt thật của 030 |

### Within Each User Story

- Test viết trước và FAIL → DTO → repository/query → service → controller → hook → component → đăng ký.

### Parallel Opportunities

- Setup: T002–T006 song song.
- Foundational: T007–T014 song song; T016, T018, T019, T023, T024, T026, T027 song song.
- Sau US1: US4, US5, US7, US8 có thể giao cho người khác nhau cùng lúc; backend và frontend của cùng story song song.

---

## Parallel Example: User Story 1

```bash
# Viết toàn bộ test US1 cùng lúc (phải FAIL):
Task: "T028 GenerationOptionsServiceTest in src/backend/src/test/java/com/wikicook/aiplanner/service/GenerationOptionsServiceTest.java"
Task: "T029 CandidateQueryIT in src/backend/src/test/java/com/wikicook/aiplanner/repository/CandidateQueryIT.java"
Task: "T030 PromptBuilderTest in src/backend/src/test/java/com/wikicook/aiplanner/service/PromptBuilderTest.java"
Task: "T033 AiGenerateDialog.test.tsx in src/frontend/src/features/ai-planner/__tests__/AiGenerateDialog.test.tsx"
Task: "T035 useAiDraft.test.ts in src/frontend/src/features/ai-planner/__tests__/useAiDraft.test.ts"
Task: "T037 AiGenerateButton.test.tsx in src/frontend/src/features/ai-planner/__tests__/AiGenerateButton.test.tsx"

# Sau đó code song song:
Task: "T038 DTO records in src/backend/src/main/java/com/wikicook/aiplanner/dto/"
Task: "T041 PromptBuilder in src/backend/src/main/java/com/wikicook/aiplanner/service/PromptBuilder.java"
Task: "T045 AiGenerateButton, AiGenerateDialog, GenerationProgress, AiDisclaimer, DraftBar in src/frontend/src/features/ai-planner/components/"
```

---

## Implementation Strategy

### MVP First (Setup + Foundational + US1 + US2 + US3)

1. Phase 1 Setup → Phase 2 Foundational (CHẶN mọi story).
2. Phase 3 US1 → **dừng và kiểm chứng** A1–A4 với `mode=mock`.
3. Phase 4 US2 → kiểm chứng A10–A14.
4. Phase 5 US3 → kiểm chứng A5. **MVP xong**: tạo → kiểm tra an toàn → xác nhận.
   Lưu ý: trong MVP, khi AI lỗi người dùng nhận 503 (nhánh tạm của T042); fallback có ở US6.

### Incremental Delivery

1. MVP → demo nội bộ với `mode=mock`.
2. US6 (fallback, hủy) ngay sau MVP – project-plan yêu cầu plan hợp lệ ≤ 2 s khi tắt LLM cho Build v0.8.
3. US5 (giới hạn lượt) + US4 (không đủ món).
4. US8 (embedding thật) + US7 (gợi ý một ô).
5. Polish trước Build v0.8: T097 với LLM thật, T098 bảo mật, T099 k6.

### Parallel Team Strategy

1. C làm Setup + Foundational (package `ai` sẽ được 090 dùng lại).
2. Sau US1: một người làm US2 + US3 (frontend draft + validator), một người làm US8 + US7 (embedding, gợi ý).
3. US6 sau US3 + US5.

---

## Notes

- [P] = file khác nhau, không phụ thuộc task chưa xong.
- Mã scenario (US1-3…) trong mô tả test dùng cho ma trận truy vết T102.
- Commit theo Conventional Commits kèm mã Jira, ví dụ `feat(aiplanner): add draft validator (SCRUM-xx)`.
- Không commit `WIKICOOK_AI_API_KEY`; CI chạy `mode=mock`.
