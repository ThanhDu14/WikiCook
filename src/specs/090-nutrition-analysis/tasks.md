---

description: "Task list for 090 Nutrition & Allergen Analysis"
---

# Tasks: Nutrition & Allergen Analysis

**Input**: Design documents from `specs/090-nutrition-analysis/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/openapi.yaml](contracts/openapi.yaml),
[contracts/integration.md](contracts/integration.md), [quickstart.md](quickstart.md)

**Tests**: BẮT BUỘC (constitution Principle II, spec SC-008). Trong mỗi phase, task test đứng
trước task code và **phải chạy FAIL trước** khi viết code. Mỗi task test ghi mã acceptance scenario
(`US3-2`…), FR hoặc edge case mà nó phủ.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- **⛓ 031 / 030 / A / B / E / Nhóm**: task chạm điểm nối với spec khác hoặc quyết định chung. Cách
  làm tạm khi bên kia chưa xong ghi ở mục Dependencies.

## Path Conventions

- Backend main: `src/backend/src/main/java/com/wikicook/nutrition/` (+ `ai/quota/` cho job dọn log)
- Backend test: `src/backend/src/test/java/com/wikicook/nutrition/`, resource `src/backend/src/test/resources/nutrition/`
- Migration: `src/backend/src/main/resources/db/migration/`
- Frontend: `src/frontend/src/features/nutrition/` (test trong `__tests__/`)
- Tiền điều kiện: skeleton Build v0.5; package `com.wikicook.ai` + V302 của 031 (Foundational 031) và
  `AbstractPostgresIT` của 030 (030 T004).

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Khung thư mục, cấu hình, cổng chất lượng, kiểm tra phần dùng chung của 031

- [ ] T001 Tạo các package `nutrition/{api,controller,service,port,repository,domain,dto}` trong src/backend/src/main/java/com/wikicook/, package test tương ứng trong src/backend/src/test/java/com/wikicook/nutrition/ và thư mục src/backend/src/test/resources/nutrition/llm-responses/
- [ ] T002 [P] Thêm khối `wikicook.ai.nutrition` (`queue-capacity: 5000`, `min-interval: 4s`, `prompt-version: v1`) vào src/backend/src/main/resources/application.yml; không thêm biến môi trường mới (key vẫn là `WIKICOOK_AI_API_KEY`, phần hạn mức là `wikicook.ai.daily-cap.nutrition-share` của 031)
- [ ] T003 [P] Thêm rule JaCoCo `check` (LINE COVEREDRATIO ≥ 0.70) cho package `com.wikicook.nutrition.service`, gắn vào phase `verify`, trong src/backend/pom.xml
- [ ] T004 [P] Tạo thư mục src/frontend/src/features/nutrition/{components,admin,__tests__}
- [ ] T005 Kiểm tra đã có `LlmClient`, `AiQuotaService`, `AiFeature.NUTRITION`, `AiTrigger` (`APPROVAL`, `REVISION`, `IMPORT`, `ADMIN_RECALC`), `FakeLlmClient` và `V302__ai_requests_embeddings.sql` trong src/backend/src/main/java/com/wikicook/ai/ và src/backend/src/main/resources/db/migration/; nếu chưa có thì làm đúng các task 031 T015–T018, T020, T024 theo specs/031-ai-weekly-meal-planner/contracts/integration.md mục 1 (một bản duy nhất, không viết bản riêng cho 090) ⛓ 031

**Checkpoint**: `./mvnw verify` và `npm test` chạy được; package `ai` và V302 có sẵn.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema, entity, fingerprint, đầu vào ước tính, luật "hiển thị được", audit, xử lý lỗi, fixture, string store, API client

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Tests cho Foundational (viết trước, phải FAIL)

- [ ] T006 [P] Integration test `NutritionSchemaIT` (extends `AbstractPostgresIT` của 030) – V303 chạy được; bị từ chối khi: `status` ngoài `('CALCULATING','AVAILABLE','FAILED')`, `kcal` ngoài `BETWEEN 0 AND 3000`, `protein_g`/`carbs_g`/`fat_g` ngoài `BETWEEN 0 AND 300`, `diet_labels` có phần tử ngoài `ARRAY['GLUTEN_FREE','DAIRY_FREE','NUT_FREE','LOW_SODIUM','VEGETARIAN','VEGAN']`, `status = 'AVAILABLE'` mà thiếu một trong 4 số, `is_hidden = true` mà `hidden_reason IS NULL`, `hidden_reason` dài 301 ký tự, dòng thứ hai cùng `recipe_id` (PK); xóa recipe → dòng bị xóa (`CASCADE`); xóa user `hidden_by` → `NULL` (`SET NULL`); mặc định `diet_labels = '{}'`, `low_confidence = false`, `is_hidden = false` trong src/backend/src/test/java/com/wikicook/nutrition/repository/NutritionSchemaIT.java
- [ ] T007 [P] Unit test `FingerprintTest` – kết quả 64 ký tự hex; cùng đầu vào → cùng hash; đổi `quantity`, `unit_id`, `ingredient_id`, `is_optional`, `servings` hoặc thứ tự `position` → hash đổi; `1.500` và `1.5` cho cùng hash; `quantity` NULL mã hóa `-`; đổi tên nguyên liệu (cùng id) không đổi hash (research R5, FR-005) trong src/backend/src/test/java/com/wikicook/nutrition/service/FingerprintTest.java
- [ ] T008 [P] Integration test `EstimationInputBuilderIT` – công thức B: "Muối" (`units.requires_quantity = false`) và nguyên liệu có `quantity IS NULL` vào `toTaste`, "Ngò" (`is_optional`) vào `optional`, chỉ nguyên liệu có định lượng và không tùy chọn vào `included` (FR-012, FR-013); `CatalogueFacts` gồm mã allergen và `ingredients.kind` của **mọi** nguyên liệu, kể cả tùy chọn và "vừa ăn" (research R8); `servings` NULL được giữ nguyên NULL; đầu vào không chứa tiêu đề, tác giả, id người dùng (FR-004) trong src/backend/src/test/java/com/wikicook/nutrition/service/EstimationInputBuilderIT.java
- [ ] T009 [P] Integration test `NutritionVisibilitySqlIT` – chỉ trả công thức có `recipes.status = 'PUBLISHED' AND n.status = 'AVAILABLE' AND NOT n.is_hidden`; loại `CALCULATING`, `FAILED`, ẩn, không có dòng, công thức ARCHIVED có dòng `AVAILABLE` (research R12, edge case lưu trữ khi đang tính) trong src/backend/src/test/java/com/wikicook/nutrition/api/NutritionVisibilitySqlIT.java
- [ ] T010 [P] Integration test `JdbcAuditLogWriterIT` – ghi một dòng `audit_logs` với `actor_id`, `action`, `target_type = 'RECIPE'`, `target_id`, `details` JSONB; chạy trong transaction của người gọi (rollback → không có dòng) (FR-026) trong src/backend/src/test/java/com/wikicook/nutrition/port/JdbcAuditLogWriterIT.java
- [ ] T011 [P] Unit test `NutritionExceptionHandlerTest` – ProblemDetail có `code`: 404 `RECIPE_NOT_FOUND`, 422 `RECIPE_NOT_PUBLISHED`, 404 `NO_ESTIMATE`, 409 `ALREADY_QUEUED`, 400 `VALIDATION_ERROR`; body không có stack trace hay chữ lỗi của provider (constitution IV) trong src/backend/src/test/java/com/wikicook/nutrition/controller/NutritionExceptionHandlerTest.java
- [ ] T012 [P] Test `nutritionApi.test.ts` (msw) – mỗi hàm gọi đúng method/URL của contracts/openapi.yaml (`getNutrition`, `getAllergens`, `listEstimates`, `listAttempts`, `recalculate`, `hide`, `unhide`); `getAllergens` gửi JWT khi đã đăng nhập; không hàm nào gọi domain của provider AI (FR-003) trong src/frontend/src/features/nutrition/__tests__/nutritionApi.test.ts

### Implementation cho Foundational

- [ ] T013 Viết migration src/backend/src/main/resources/db/migration/V303__recipe_nutrition.sql đúng data-model.md mục 1: `recipe_nutrition(recipe_id BIGINT PK REFERENCES recipes ON DELETE CASCADE, status VARCHAR(12) NOT NULL CHECK (status IN ('CALCULATING','AVAILABLE','FAILED')), kcal SMALLINT NULL CHECK (kcal BETWEEN 0 AND 3000), protein_g/carbs_g/fat_g SMALLINT NULL CHECK (… BETWEEN 0 AND 300), diet_labels VARCHAR(15)[] NOT NULL DEFAULT '{}' CHECK (diet_labels <@ ARRAY['GLUTEN_FREE','DAIRY_FREE','NUT_FREE','LOW_SODIUM','VEGETARIAN','VEGAN']::VARCHAR[]), low_confidence BOOLEAN NOT NULL DEFAULT false, excluded_ingredients JSONB NULL, input_fingerprint CHAR(64) NOT NULL, last_error VARCHAR(30) NULL, is_hidden BOOLEAN NOT NULL DEFAULT false, hidden_reason VARCHAR(300) NULL, hidden_by BIGINT NULL REFERENCES users ON DELETE SET NULL, computed_at TIMESTAMPTZ NULL)` + `CHECK (NOT is_hidden OR hidden_reason IS NOT NULL)` + `CHECK (status <> 'AVAILABLE' OR (kcal IS NOT NULL AND protein_g IS NOT NULL AND carbs_g IS NOT NULL AND fat_g IS NOT NULL))` + index `(kcal) WHERE status = 'AVAILABLE' AND NOT is_hidden` và index `(status)` (làm T006 xanh)
- [ ] T014 [P] Tạo enum `NutritionStatus` (`CALCULATING`, `AVAILABLE`, `FAILED`), `DietLabel` (6 nhãn), `FailureReason` (`AI_ERROR`, `TIMEOUT`, `MALFORMED`, `NEGATIVE`, `OUT_OF_RANGE`, `MACRO_MISMATCH`, `USAGE_CAP`, `NOTHING_TO_ESTIMATE`, `NO_SERVINGS`, `INTERRUPTED`, `QUEUE_FULL`, `STALE`), entity `RecipeNutrition` và `RecipeNutritionRepository` (native: `upsertCalculating(recipeId, fingerprint)` dùng `INSERT … ON CONFLICT (recipe_id) DO UPDATE` xóa số liệu/nhãn nhưng giữ `is_hidden`; `writeResultIfFingerprint(…)` và `markFailedIfFingerprint(…)` có điều kiện `WHERE recipe_id = :id AND input_fingerprint = :fp`, trả số dòng; `markAllCalculatingInterrupted()`) trong src/backend/src/main/java/com/wikicook/nutrition/domain/ và repository/RecipeNutritionRepository.java
- [ ] T015 [P] Cài `Fingerprint.of(servings, List<IngredientLine>)` (SHA-256 hex theo research R5) trong src/backend/src/main/java/com/wikicook/nutrition/service/Fingerprint.java (làm T007 xanh)
- [ ] T016 Cài `EstimationInputQuery` (`NamedParameterJdbcTemplate`, join `recipe_ingredients`/`ingredients`/`units`/`ingredient_allergens`/`allergens`) và `EstimationInputBuilder` trả `EstimationInput(servings, included, toTaste, optional, CatalogueFacts, fingerprint)` trong src/backend/src/main/java/com/wikicook/nutrition/repository/EstimationInputQuery.java và service/EstimationInputBuilder.java (làm T008 xanh)
- [ ] T017 [P] Cài `NutritionVisibilitySql.visible(recipeAlias, nutritionAlias)` trong src/backend/src/main/java/com/wikicook/nutrition/api/NutritionVisibilitySql.java (làm T009 xanh)
- [ ] T018 [P] Tạo `AuditLogPort.record(actorId, action, targetType, targetId, details)` và bean mặc định `JdbcAuditLogWriter` (`@ConditionalOnMissingBean`) trong src/backend/src/main/java/com/wikicook/nutrition/port/ (làm T010 xanh) ⛓ A (010)
- [ ] T019 Tạo `RecipeNotFoundException`, `RecipeNotPublishedException`, `NoEstimateException`, `AlreadyQueuedException` và `NutritionExceptionHandler` (ProblemDetail + `code`) trong src/backend/src/main/java/com/wikicook/nutrition/controller/ (làm T011 xanh)
- [ ] T020 [P] Tạo response LLM mẫu `valid.json`, `malformed.txt`, `missing-fat.json`, `negative.json`, `over-3000.json`, `macro-off-25pct.json`, `unknown-label.json`, `dairy-free-with-milk.json`, `html-label.json` trong src/backend/src/test/resources/nutrition/llm-responses/ và `NutritionSeed` (công thức A–E như quickstart mục 1, allergen `GLUTEN`, `MILK`, `PEANUT`, `TREE_NUT`, unit `to_taste`, tài khoản admin/mod/allergy) trong src/backend/src/test/java/com/wikicook/nutrition/NutritionSeed.java ⛓ 031 (`FakeLlmClient`) ⛓ A (mã allergen)
- [ ] T021 [P] Tạo string store tiếng Việt: disclaimer nguyên văn "Số liệu dinh dưỡng do AI ước tính, chỉ mang tính tham khảo", "Đang tính…", "Chưa có số liệu dinh dưỡng", "Độ tin cậy thấp", "Không tính: nguyên liệu tùy chọn", "Không có chất gây dị ứng nào được ghi trong danh mục nguyên liệu", "Gợi ý của AI", "Có trong hồ sơ dị ứng của bạn", tên 6 nhãn, tên trạng thái và lý do lỗi cho admin, "{n} món chưa có số liệu" trong src/frontend/src/features/nutrition/strings.vi.ts (NFR-007)
- [ ] T022 [P] Tạo type TS khớp contracts/openapi.yaml (`RecipeNutrition`, `RecipeAllergens`, `EstimateRow`, `EstimatePage`, `Attempt`, `HideRequest`, các enum) trong src/frontend/src/features/nutrition/types.ts và `nutritionApi.ts` dùng client chung trong src/frontend/src/api/nutritionApi.ts (làm T012 xanh)

**Checkpoint**: V303 chạy trên Testcontainers; test Foundational xanh; chưa có trigger hay endpoint.

---

## Phase 3: User Story 3 - Implausible AI output never reaches users (Priority: P1)

**Goal**: Mọi output AI sai định dạng, âm, quá ngưỡng, lệch macro bị từ chối; nhãn lạ hoặc mâu thuẫn catalogue bị bỏ.

**Independent Test**: Chạy bộ response mẫu qua validator: mọi response sai bị từ chối đúng lý do, response hợp lệ được nhận (SC-003, SC-004).

### Tests for User Story 3 (viết trước, phải FAIL)

- [ ] T023 [P] [US3] Unit test `NutritionValidatorTest` (class thuần, không Spring) với `@Nested` cho từng luật của research R8: **Structure** – không phải JSON, thiếu `fat_g`, số dạng chuỗi, `NaN`/`Infinity` → `MALFORMED` (US3-1); **Negative** – một giá trị < 0 → `NEGATIVE` (US3-2); **Range** – tổng 12.000 kcal / 4 khẩu phần = 3.000 nhận, 12.004 / 4 từ chối `OUT_OF_RANGE`, macro 301 g/khẩu phần từ chối, 50 khẩu phần vẫn kiểm theo khẩu phần (US3-2, edge case); **Macro** – lệch đúng 20% nhận, 20,1% từ chối `MACRO_MISMATCH`, `kcal` và `4P+4C+9F` cùng < 20 thì nhận (US3-3); **Order** – nhiều luật sai thì trả lý do của luật đầu tiên; **Labels** – "Keto" bị bỏ, phần còn lại vẫn được đánh giá (US3-4), "Gluten-Free"/"gluten free" chuẩn hóa thành `GLUTEN_FREE`; **Contradiction** – từng dòng bảng R8 (GLUTEN_FREE↔`GLUTEN`; DAIRY_FREE↔`MILK` hoặc kind `DAIRY`; NUT_FREE↔`PEANUT`/`TREE_NUT`; VEGETARIAN↔`MEAT`/`SEAFOOD`; VEGAN↔`MEAT`/`SEAFOOD`/`EGG`/`DAIRY`/`HONEY`), `LOW_SODIUM` không bao giờ bị bỏ vì catalogue (US3-5, FR-011); **Rounding** – kiểm tra trên số chưa làm tròn, kết quả trả số nguyên đã làm tròn (FR-015) trong src/backend/src/test/java/com/wikicook/nutrition/service/NutritionValidatorTest.java
- [ ] T024 [P] [US3] Parameterized test `LlmResponseFixturesTest` – mỗi file trong src/test/resources/nutrition/llm-responses/ cho đúng kết quả kỳ vọng (`valid` nhận; `malformed`, `missing-fat`, `negative`, `over-3000`, `macro-off-25pct` từ chối đúng lý do; `unknown-label`, `html-label`, `dairy-free-with-milk` nhận nhưng bỏ nhãn) – 100% response sai bị từ chối (SC-003, NFR-005) trong src/backend/src/test/java/com/wikicook/nutrition/service/LlmResponseFixturesTest.java
- [ ] T025 [P] [US3] Unit test `NutritionPromptBuilderTest` – prompt có `servings` và từng nguyên liệu `included` dạng `tên | định lượng | đơn vị`; **không** có nguyên liệu "vừa ăn"/tùy chọn, tiêu đề, tên tác giả, email, id (FR-004, NFR-004); yêu cầu **tổng của cả công thức**; có JSON schema `{"kcal","protein_g","carbs_g","fat_g","diet_labels"}` và danh sách 6 nhãn; `temperature = 0` trong src/backend/src/test/java/com/wikicook/nutrition/service/NutritionPromptBuilderTest.java

### Implementation for User Story 3

- [ ] T026 [US3] Cài `NutritionValidator.validate(rawJson, servings, CatalogueFacts)` trả `ValidationResult` (`Accepted(kcal, proteinG, carbsG, fatG, labels, droppedLabels)` / `Rejected(FailureReason)`) và record `CatalogueFacts(Set<String> allergenCodes, Set<IngredientKind> kinds)` trong src/backend/src/main/java/com/wikicook/nutrition/service/NutritionValidator.java (làm T023, T024 xanh)
- [ ] T027 [US3] Cài `NutritionPromptBuilder` (dựng `LlmRequest`) và mẫu prompt src/backend/src/main/resources/prompts/nutrition.v1.txt (vai trò, chỉ dùng nguyên liệu được liệt kê, cỡ thông thường cho đơn vị đếm, trả tổng cả công thức, 6 nhãn, JSON schema) trong src/backend/src/main/java/com/wikicook/nutrition/service/NutritionPromptBuilder.java (làm T025 xanh)

**Checkpoint**: Validator và prompt hoàn chỉnh, không phụ thuộc DB hay AI thật.

---

## Phase 4: User Story 2 - Estimated once on approval, stored, never blocking publication (Priority: P1)

**Goal**: Mỗi lần duyệt/import có đúng một việc ước tính chạy nền; kết quả hợp lệ được lưu; lỗi thành `FAILED` không tự thử lại; publish không bao giờ chờ AI.

**Independent Test**: Tắt AI (`mode=offline`) → duyệt công thức: publish xong ngay, ước tính `FAILED`, không gọi lại; bật `mode=mock` → duyệt công thức khác: có số liệu được lưu (quickstart N1, N2, N5–N6, N16–N19).

### Tests for User Story 2 (viết trước, phải FAIL)

- [ ] T028 [P] [US2] Unit test `NutritionQueueTest` (`Clock` + sleeper giả) – `ADMIN_RECALC`, `APPROVAL`, `REVISION` ra trước `IMPORT`, cùng ưu tiên thì FIFO (NFR-001); đầy `queue-capacity` → `offer` trả `false`; worker gọi xử lý lần lượt, giữ khoảng cách ≥ `min-interval` giữa hai lần gọi LLM; việc lỗi không được đưa lại vào hàng đợi (FR-006); `isQueued(recipeId)` đúng khi việc đang chờ hoặc đang chạy trong src/backend/src/test/java/com/wikicook/nutrition/service/NutritionQueueTest.java
- [ ] T029 [P] [US2] Unit test `EstimationServiceTest` (Mockito) – thành công: `AiQuotaService.reserve(NUTRITION, …)` → `LlmClient.complete(…, Duration.ofSeconds(60), …)` → validator → `writeResultIfFingerprint` với `AVAILABLE`, nhãn, `low_confidence = true` khi `toTaste` khác rỗng, `excluded_ingredients` `{"toTaste":[…],"optional":[…]}`, `computed_at`; `finish(SUCCESS, calledLlm = true)` (US2-2, FR-012); LLM lỗi → `FAILED`/`AI_ERROR`, `finish(FAILED, true)`; quá 60 s → `TIMEOUT` (edge case); validator từ chối → `FAILED` với lý do, `finish(REJECTED, true, reason)` (US3-6); `llmAllowed = false` → không gọi LLM, `FAILED`/`USAGE_CAP`, `finish(QUOTA_CAP, false)` (FR-020); không có nguyên liệu `included` → không gọi LLM, `NOTHING_TO_ESTIMATE` (edge case); `servings` NULL → không gọi LLM, `NO_SERVINGS`; không có lần gọi thứ hai sau lỗi (US2-4, FR-006) trong src/backend/src/test/java/com/wikicook/nutrition/service/EstimationServiceTest.java
- [ ] T030 [P] [US2] Integration test `NutritionTriggerListenerIT` – transaction publish commit → có dòng `CALCULATING` và một việc `APPROVAL` (US2-1, FR-001); transaction rollback → không có dòng, không có việc (FR-002); hàng đợi đầy hoặc listener ném lỗi → publish vẫn commit, dòng thành `FAILED`/`QUEUE_FULL` (FR-002); `RecipesImportedEvent` 3 id → 3 việc `IMPORT` (FR-001) trong src/backend/src/test/java/com/wikicook/nutrition/service/NutritionTriggerListenerIT.java
- [ ] T031 [P] [US2] Integration test `EstimationFlowIT` (Testcontainers + `FakeLlmClient`) – publish A → chờ (Awaitility) → `AVAILABLE`, `ai_requests` có đúng 1 dòng `feature = 'NUTRITION'`, `trigger = 'APPROVAL'`, `counted = false` (US2-1/2, quickstart N1–N2); D → `NOTHING_TO_ESTIMATE` không gọi LLM (N5); E → `NO_SERVINGS` (N6); `mode=offline` → `FAILED`/`AI_ERROR`, publish trả về trong cùng thời gian (US2-4, N16); `FakeLlmClient` ngủ 70 s → `TIMEOUT` (N17); `daily-cap.total=1` và import 3 công thức → 1 `AVAILABLE`, 2 `USAGE_CAP` (edge case seed import, N18) trong src/backend/src/test/java/com/wikicook/nutrition/service/EstimationFlowIT.java
- [ ] T032 [P] [US2] Integration test `InterruptedEstimatesCleanerIT` – các dòng `CALCULATING` lúc `ApplicationReadyEvent` thành `FAILED`/`INTERRUPTED`, không gọi `LlmClient`, không đưa vào hàng đợi (research R4, N19) trong src/backend/src/test/java/com/wikicook/nutrition/service/InterruptedEstimatesCleanerIT.java

### Implementation for User Story 2

- [ ] T033 [US2] Cài `NutritionTask(recipeId, fingerprint, trigger, actorId, enqueuedAt)`, `NutritionQueue` (`PriorityBlockingQueue`, sức chứa từ cấu hình, tập `queued`) và `NutritionWorker` (1 virtual thread, khởi động ở `ApplicationReadyEvent`, dừng khi shutdown, giãn cách `min-interval`) trong src/backend/src/main/java/com/wikicook/nutrition/service/NutritionQueue.java và NutritionWorker.java (làm T028 xanh)
- [ ] T034 [US2] Cài `EstimationService.process(NutritionTask)` theo research R7–R9 (dùng `EstimationInputBuilder`, `NutritionPromptBuilder`, `LlmClient`, `NutritionValidator`, `AiQuotaService`, `RecipeNutritionRepository`) trong src/backend/src/main/java/com/wikicook/nutrition/service/EstimationService.java (làm T029 xanh) ⛓ 031
- [ ] T035 [US2] Cài `NutritionTriggerListener` (`@TransactionalEventListener(phase = AFTER_COMMIT)` cho `RecipePublishedEvent`, `RecipesImportedEvent`; ghi dòng bằng transaction `REQUIRES_NEW`; bắt mọi exception, log không kèm dữ liệu nhạy cảm) trong src/backend/src/main/java/com/wikicook/nutrition/service/NutritionTriggerListener.java (làm T030 xanh) ⛓ B (060 event)
- [ ] T036 [US2] Cài `InterruptedEstimatesCleaner` (`@EventListener(ApplicationReadyEvent)`, chạy trước khi worker nhận việc) trong src/backend/src/main/java/com/wikicook/nutrition/service/InterruptedEstimatesCleaner.java (làm T032 xanh, sau đó T031 xanh)

**Checkpoint**: Duyệt công thức tạo ước tính chạy nền; lỗi không tự thử lại; publish không phụ thuộc AI.

---

## Phase 5: User Story 1 - See nutrition per serving on a recipe (Priority: P1)

**Goal**: Trang công thức hiện khối dinh dưỡng mỗi khẩu phần với 4 trạng thái và disclaimer cố định.

**Independent Test**: Publish công thức đủ định lượng, chờ ước tính, mở bằng Guest → 4 số mỗi khẩu phần + disclaimer; công thức vừa publish → "Đang tính…" (quickstart N2, N4).

### Tests for User Story 1 (viết trước, phải FAIL)

- [ ] T037 [P] [US1] Unit test `NutritionReadServiceTest` – ánh xạ đúng bảng data-model mục 4: không có dòng → `NO_DATA`; `CALCULATING` → `CALCULATING` không số; `AVAILABLE` → 4 số + `servings`; `low_confidence` → `lowConfidence = true` + `excluded.toTaste`; `FAILED` → `NO_DATA` không có lý do (US1-4); ẩn → `NO_DATA`, `dietLabels` rỗng (US7-4); `excluded` rỗng khi không `AVAILABLE` trong src/backend/src/test/java/com/wikicook/nutrition/service/NutritionReadServiceTest.java
- [ ] T038 [P] [US1] Integration test `RecipeNutritionControllerIT` – Guest `GET /api/v1/recipes/{id}/nutrition` 200 đúng schema `RecipeNutrition`, header `Cache-Control: public, max-age=60` (US1-1); công thức không tồn tại hoặc không PUBLISHED → 404 `RECIPE_NOT_FOUND`; JSON không bao giờ có `lastError`, `hiddenReason` (US1-4) trong src/backend/src/test/java/com/wikicook/nutrition/controller/RecipeNutritionControllerIT.java
- [ ] T039 [P] [US1] Test `NutritionBlock.test.tsx` (msw, fake timers) – `AVAILABLE`: kcal, protein, carb, fat số nguyên kèm đơn vị và số khẩu phần (US1-1); disclaimer hiện ở **mọi** trạng thái, không cần mở rộng (US1-2, FR-016); `CALCULATING`: "Đang tính…", hỏi lại mỗi 15 s, dừng sau 5 phút hoặc khi unmount (US1-3); `NO_DATA`: "Chưa có số liệu dinh dưỡng", không số, không lỗi (US1-4); `lowConfidence`: "Độ tin cậy thấp" + tên nguyên liệu "vừa ăn" (US1-5); có `excluded.optional` → "Không tính: nguyên liệu tùy chọn" (FR-013); component không nhận prop khẩu phần nên đổi khẩu phần trên trang không đổi số (US1-6); vùng trạng thái có `aria-live="polite"` (NFR-008); mất mạng / request lỗi (edge case "offline / từ cache"): lần tải đầu lỗi → không hiện số, hiện trạng thái `NO_DATA` kèm disclaimer; đã có dữ liệu mà lần hỏi lại lỗi → **giữ nguyên** trạng thái và số đã tải kèm disclaimer, không xóa, không hiện lỗi kỹ thuật; phần còn lại của trang không bị ảnh hưởng (US1-3); không có request nào tới domain provider AI (FR-003) trong src/frontend/src/features/nutrition/__tests__/NutritionBlock.test.tsx

### Implementation for User Story 1

- [ ] T040 [US1] Tạo `RecipeNutritionDto` và cài `NutritionReadService.getForRecipe(recipeId)` (kiểm tra PUBLISHED, đọc dòng, ánh xạ trạng thái) trong src/backend/src/main/java/com/wikicook/nutrition/service/NutritionReadService.java và dto/RecipeNutritionDto.java (làm T037 xanh)
- [ ] T041 [US1] Cài `RecipeNutritionController` `GET /api/v1/recipes/{recipeId}/nutrition` (public, `@Min(1)`, OpenAPI annotation, `Cache-Control`) và cho phép truy cập không đăng nhập trong cấu hình bảo mật trong src/backend/src/main/java/com/wikicook/nutrition/controller/RecipeNutritionController.java (làm T038 xanh) ⛓ A (SecurityConfig)
- [ ] T042 [P] [US1] Cài `NutritionDisclaimer` và `NutritionBlock` (4 trạng thái, polling, giữ dữ liệu đã tải khi request sau bị lỗi, chỉ render text, không `dangerouslySetInnerHTML`) trong src/frontend/src/features/nutrition/components/ (làm T039 xanh)
- [ ] T043 [US1] Export `NutritionBlock`, `AllergenArea`, `AiDietLabels`, `CalorieValue` từ src/frontend/src/features/nutrition/index.ts để trang chi tiết công thức nhúng theo contracts/integration.md mục 3.3; khi trang của 060 chưa có, thêm route xem thử `/dev/nutrition/:recipeId` chỉ bật ở môi trường dev ⛓ B (060)

**Checkpoint**: MVP hiển thị: Guest thấy số liệu hoặc trạng thái đúng trên trang công thức.

---

## Phase 6: User Story 4 - Allergen warnings and AI diet labels shown separately (Priority: P1)

**Goal**: Vùng cảnh báo dị ứng lấy từ catalogue, tách biệt và khác kiểu với nhãn AI; highlight dị ứng trong hồ sơ.

**Independent Test**: Công thức C có đậu phộng, AI gợi ý "Vegan" → vùng allergen ghi "Đậu phộng"; "Vegan" ở vùng riêng "Gợi ý của AI" (quickstart N8–N11).

### Tests for User Story 4 (viết trước, phải FAIL)

- [ ] T044 [P] [US4] Integration test `RecipeAllergensControllerIT` – `GET /api/v1/recipes/{id}/allergens` trả mỗi allergen một lần từ mọi nguyên liệu, kể cả tùy chọn (US4-1); không phụ thuộc trạng thái ước tính (không có dòng `recipe_nutrition` vẫn trả đủ); Guest không có `inMyProfile`; có JWT của `allergy@` → `PEANUT.inMyProfile = true` (US4-4, FR-018); thêm `ingredient_allergens` cho một nguyên liệu → GET kế tiếp có allergen mới, `ai_requests` không thêm dòng (US4-5); có JWT → `Cache-Control: no-store`; không PUBLISHED → 404 trong src/backend/src/test/java/com/wikicook/nutrition/controller/RecipeAllergensControllerIT.java
- [ ] T045 [P] [US4] Test `AllergenArea.test.tsx` – liệt kê "Đậu phộng, Sữa" mỗi tên một lần (US4-1); rỗng → đúng chuỗi "Không có chất gây dị ứng nào được ghi trong danh mục nguyên liệu", không bao giờ "không chứa chất gây dị ứng" (US4-2); `inMyProfile` → icon + chữ "Có trong hồ sơ dị ứng của bạn", không chỉ dựa vào màu (US4-4, NFR-008); vùng allergen là phần tử riêng, không nằm trong `NutritionBlock` (FR-017) trong src/frontend/src/features/nutrition/__tests__/AllergenArea.test.tsx
- [ ] T046 [P] [US4] Test `AiDietLabels.test.tsx` – có tiêu đề "Gợi ý của AI" và disclaimer ngắn, kiểu chip khác cảnh báo dị ứng (role/class khác) (US4-3); không có nhãn hoặc `NO_DATA`/ẩn → không render; nhãn hiển thị bằng tên tiếng Việt từ string store, không render chuỗi thô từ API (NFR-005) trong src/frontend/src/features/nutrition/__tests__/AiDietLabels.test.tsx

### Implementation for User Story 4

- [ ] T047 [US4] Cài `AllergenQuery` (allergen không trùng của công thức) và `AllergenService.getForRecipe(recipeId, Optional<userId>)` (đánh dấu `inMyProfile` từ `DietaryProfilePort`) trong src/backend/src/main/java/com/wikicook/nutrition/repository/AllergenQuery.java và service/AllergenService.java ⛓ 030 (T020 `DietaryProfilePort`) ⛓ A (011)
- [ ] T048 [US4] Thêm `GET /api/v1/recipes/{recipeId}/allergens` (xác thực tùy chọn, `RecipeAllergensDto`) vào src/backend/src/main/java/com/wikicook/nutrition/controller/RecipeNutritionController.java (làm T044 xanh)
- [ ] T049 [P] [US4] Cài `AllergenArea` và `AiDietLabels` trong src/frontend/src/features/nutrition/components/ (làm T045, T046 xanh)

**Checkpoint**: Toàn bộ P1 (MVP) xong: ước tính chạy nền, được kiểm tra, hiển thị; cảnh báo dị ứng tách khỏi AI.

---

## Phase 7: User Story 5 - Estimates follow recipe changes (Priority: P2)

**Goal**: Bản sửa đổi nguyên liệu/khẩu phần làm số cũ thôi hiển thị và tính lại một lần; sửa chữ hoặc khôi phục không đổi thì giữ nguyên; kết quả cũ về muộn bị bỏ; trigger trùng chỉ tính một lần.

**Independent Test**: Duyệt bản sửa đổi định lượng → "Đang tính…" rồi số mới; duyệt bản sửa chỉ mô tả → giữ số, không gọi AI (quickstart N12–N15).

### Tests for User Story 5 (viết trước, phải FAIL)

- [ ] T050 [P] [US5] Integration test `FingerprintTriggerIT` – `RecipeRevisionApprovedEvent` với fingerprint khác → dòng `CALCULATING`, 4 số và `diet_labels` bị xóa, `is_hidden` giữ nguyên, một việc `REVISION` (US5-1); bản sửa chỉ đổi mô tả (cờ `false`, hoặc cờ `true` nhưng fingerprint không đổi) → dòng không đổi, không có việc, không có dòng `ai_requests` (US5-2, FR-008); bản sửa đang chờ duyệt → không có event, khối vẫn trả ước tính của bản đã publish (US5-3); archive rồi `RecipePublishedEvent` với cùng fingerprint → dùng lại ước tính, kể cả khi dòng đang `FAILED` (US5-4, FR-006) trong src/backend/src/test/java/com/wikicook/nutrition/service/FingerprintTriggerIT.java
- [ ] T051 [P] [US5] Integration test `StaleResultIT` – `FakeLlmClient` ngủ 5 s; duyệt bản sửa thứ hai (đổi định lượng) khi lượt thứ nhất đang chạy → kết quả lượt thứ nhất bị bỏ, `ai_requests.reason = 'STALE'`; dòng cuối cùng mang số của lượt thứ hai (edge case, FR-005, N14) trong src/backend/src/test/java/com/wikicook/nutrition/service/StaleResultIT.java
- [ ] T052 [P] [US5] Integration test `DuplicateTriggerIT` – hai việc cùng `(recipeId, fingerprint)` (event lặp, hoặc admin recalculate khi việc duyệt còn chờ) → `LlmClient` được gọi đúng 1 lần (edge case hai trigger); công thức bị archive khi đang tính → kết quả vẫn ghi `AVAILABLE`, nhưng `GET …/nutrition` trả 404 và `NutritionVisibilitySql` loại nó (edge case) trong src/backend/src/test/java/com/wikicook/nutrition/service/DuplicateTriggerIT.java

### Implementation for User Story 5

- [ ] T053 [US5] Mở rộng `NutritionTriggerListener`: nghe `RecipeRevisionApprovedEvent`; với mọi trigger trừ `ADMIN_RECALC`, so fingerprint hiện tại với dòng đã có – cùng thì bỏ qua, khác thì `upsertCalculating` rồi đưa việc vào hàng đợi (research R5) trong src/backend/src/main/java/com/wikicook/nutrition/service/NutritionTriggerListener.java (làm T050 xanh) ⛓ B (060 cờ `ingredientsOrServingsChanged`)
- [ ] T054 [US5] Thêm khóa `(recipeId, fingerprint)` vào `NutritionQueue` (việc trùng bị bỏ, log debug) và nhánh `STALE` trong `EstimationService` khi `writeResultIfFingerprint`/`markFailedIfFingerprint` trả 0 dòng (`finish(…, reason = STALE)`) trong src/backend/src/main/java/com/wikicook/nutrition/service/NutritionQueue.java và EstimationService.java (làm T051, T052 xanh)

**Checkpoint**: Số liệu luôn khớp phiên bản công thức đang hiển thị.

---

## Phase 8: User Story 6 - Calories for search and the meal planner (Priority: P2)

**Goal**: 030 có tổng calo/người/ngày và số món chưa có số liệu; 020 lọc theo calo chỉ với ước tính hiển thị được; mọi con số calo kèm disclaimer.

**Independent Test**: Lọc "300–500 kcal" chỉ ra công thức đúng khoảng; ngày planner có 2 món có số + 1 món chưa → tổng 2 món, "1 món chưa có số liệu" (quickstart N26–N27).

### Tests for User Story 6 (viết trước, phải FAIL)

- [ ] T055 [P] [US6] Integration test `NutritionDayTotalsIT` – `totals({MONDAY:[A,B,X]})` với A, B hiển thị được, X `FAILED` → `kcalPerPerson = kcal(A) + kcal(B)`, `includedDishes = 2`, `missingDishes = 1` (US6-3/4); món trùng trong ngày tính mỗi lần xuất hiện; ngày không món nào có số → `includedDishes = 0` (030 hiện "Chưa có số liệu dinh dưỡng", US6-5); món bị ẩn tính là thiếu (US7-4); bean này thay bean mặc định của 030 (`@ConditionalOnMissingBean`) trong src/backend/src/test/java/com/wikicook/nutrition/service/NutritionDayTotalsIT.java
- [ ] T056 [P] [US6] Integration test `CalorieFilterSqlIT` – ghép `CalorieFilterSql.clause("r")` vào truy vấn công thức: khoảng 300–500 gồm cả hai biên (US6-1); `CALCULATING`, `FAILED`, ẩn, không có dòng bị loại khi lọc (US6-2); `kcalMin`/`kcalMax` NULL = không giới hạn một phía; mệnh đề của `CalorieFilterSql` và `NutritionVisibilitySql` không tham chiếu `diet_labels` (US4-6, FR-019) trong src/backend/src/test/java/com/wikicook/nutrition/api/CalorieFilterSqlIT.java
- [ ] T057 [P] [US6] Test `CalorieValue.test.tsx` – hiện "520 kcal" kèm disclaimer ngắn ở cả bản thường và `compact` (FR-016); không render khi `kcal` null trong src/frontend/src/features/nutrition/__tests__/CalorieValue.test.tsx

### Implementation for User Story 6

- [ ] T058 [US6] Cài `NutritionDayTotals implements DayNutritionPort` (một truy vấn `WHERE recipe_id = ANY(:ids)` cho cả tuần, dùng `NutritionVisibilitySql`) trong src/backend/src/main/java/com/wikicook/nutrition/service/NutritionDayTotals.java (làm T055 xanh) ⛓ 030 (T020 port, T097 gắn vào `getWeek`)
- [ ] T059 [P] [US6] Cài `CalorieFilterSql.clause(recipeAlias)` và `params(kcalMin, kcalMax)` trong src/backend/src/main/java/com/wikicook/nutrition/api/CalorieFilterSql.java (làm T056 xanh) ⛓ B (020 ghép vào truy vấn tìm kiếm)
- [ ] T060 [P] [US6] Cài `CalorieValue` trong src/frontend/src/features/nutrition/components/CalorieValue.tsx (làm T057 xanh) ⛓ 030 (T098 `DayNutritionBadge` dùng component này) ⛓ B (020 thẻ kết quả)

**Checkpoint**: Search và planner đọc calo đã lưu, không gọi AI.

---

## Phase 9: User Story 7 - Administrator recalculates and hides estimates (Priority: P2)

**Goal**: Admin xem danh sách theo trạng thái, tính lại (cách duy nhất để gọi lại AI), ẩn/hiện; mọi thao tác có audit; vai trò khác bị từ chối.

**Independent Test**: Admin lọc "failed", Recalculate một công thức → số mới; Hide một công thức → trang "Chưa có số liệu", mất khỏi lọc calo (quickstart N20–N25).

### Tests for User Story 7 (viết trước, phải FAIL)

- [ ] T061 [P] [US7] Unit test `NutritionAdminServiceTest` – `recalculate`: công thức không PUBLISHED → `RecipeNotPublishedException`; đưa việc `ADMIN_RECALC` kèm `actorId`; việc trùng đang chờ → `AlreadyQueuedException`; chưa có dòng → tạo `CALCULATING`; có dòng → dòng **không đổi** (US7-2); ghi audit `NUTRITION_RECALC_REQUESTED` (US7-6); `hide`: `reason` bắt buộc, 1–300 ký tự, đặt `is_hidden`, `hidden_reason`, `hidden_by`, audit `NUTRITION_HIDDEN`; chưa có dòng → `NoEstimateException`; `unhide` xóa cả ba, audit `NUTRITION_UNHIDDEN`; hide/unhide gọi lại là idempotent (FR-025, FR-026) trong src/backend/src/test/java/com/wikicook/nutrition/service/NutritionAdminServiceTest.java
- [ ] T062 [P] [US7] Integration test `AdminRecalculateFlowIT` (`FakeLlmClient`) – tính lại công thức đang `AVAILABLE`: số cũ vẫn được đọc cho tới khi có kết quả, rồi bị thay (US7-2); kết quả hỏng → số cũ giữ nguyên, lượt `REJECTED`/`MALFORMED` có `actor_id` (US7-3, N22); tính lại công thức đang ẩn thành công → vẫn `is_hidden` (FR-025, N24); dòng `FAILED` cùng fingerprint vẫn được gọi lại (FR-006); hết hạn mức → dòng `AVAILABLE` giữ nguyên, lượt `QUOTA_CAP` trong src/backend/src/test/java/com/wikicook/nutrition/service/AdminRecalculateFlowIT.java
- [ ] T063 [P] [US7] Integration test `NutritionAdminControllerIT` – `GET /api/v1/admin/nutrition` trả `status` suy ra (`NO_DATA`, `CALCULATING`, `AVAILABLE`, `LOW_CONFIDENCE`, `FAILED`, `HIDDEN` ưu tiên khi ẩn), `lastAttemptAt`/`lastOutcome`/`lastReason` từ lượt `ai_requests` mới nhất, `queued` (US7-1); lọc `status=FAILED`; `q=pho` tìm được "Phở bò" (không dấu); mặc định 20 dòng, `size` > 50 → 400; `GET …/{id}/attempts` mới nhất trước, chỉ 30 ngày, có `actorName`; `POST …/recalculate` 202, `…/hide` 204 (reason 301 ký tự → 400), `…/unhide` 204; mỗi thao tác có đúng một dòng `audit_logs` (US7-6); MEMBER, CONTRIBUTOR, MODERATOR → 403, Guest → 401 (US7-5, FR-027) trong src/backend/src/test/java/com/wikicook/nutrition/controller/NutritionAdminControllerIT.java
- [ ] T064 [P] [US7] Test `NutritionAdminPage.test.tsx` – bảng có tên, trạng thái, lần thử gần nhất, lý do tiếng Việt; lọc trạng thái và tìm tên (debounce 300 ms) đổi query; phân trang; "Tính lại" → 202 → dòng hiện "Đang trong hàng đợi"; "Ẩn" mở `HideDialog`, nút xác nhận tắt khi lý do trống hoặc > 300 ký tự; "Bỏ ẩn"; `AttemptHistory` hiện lịch sử; 403 → thông báo không có quyền trong src/frontend/src/features/nutrition/__tests__/NutritionAdminPage.test.tsx

### Implementation for User Story 7

- [ ] T065 [P] [US7] Cài `AdminEstimateQuery` (`recipes` PUBLISHED `LEFT JOIN recipe_nutrition`, `LEFT JOIN LATERAL` lượt `ai_requests` mới nhất theo `recipe_id`, lọc trạng thái suy ra, `f_unaccent(lower(title)) LIKE`, phân trang) và truy vấn lịch sử 30 ngày trong src/backend/src/main/java/com/wikicook/nutrition/repository/AdminEstimateQuery.java
- [ ] T066 [US7] Cài `NutritionAdminService` (`list`, `attempts`, `recalculate`, `hide`, `unhide`; audit trong cùng transaction) và cho `EstimationService` nhận `ADMIN_RECALC`: bỏ qua so fingerprint, lỗi thì giữ nguyên dòng đang có số (research R10) trong src/backend/src/main/java/com/wikicook/nutrition/service/NutritionAdminService.java và EstimationService.java (làm T061, T062 xanh)
- [ ] T067 [US7] Cài `NutritionAdminController` (5 endpoint của contracts/openapi.yaml tag `nutrition-admin`, `@Valid HideRequest`, `@Max(50) size`) và yêu cầu `ROLE_ADMIN` cho `/api/v1/admin/nutrition/**` trong src/backend/src/main/java/com/wikicook/nutrition/controller/NutritionAdminController.java (làm T063 xanh) ⛓ A (010 SecurityConfig, role)
- [ ] T068 [US7] Cài `NutritionAdminPage`, `HideDialog`, `AttemptHistory` và route `/admin/nutrition` (chỉ ADMIN) trong src/frontend/src/features/nutrition/admin/ (làm T064 xanh) ⛓ E (100 vị trí trong khu admin)

**Checkpoint**: Mọi user story hoạt động độc lập.

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Không gọi AI khi đọc, độ chính xác, dọn log, bảo mật, a11y, đối chiếu tài liệu

- [ ] T069 [P] Integration test `NoAiCallOnReadIT` – gọi 100 lần `GET …/nutrition`, `GET …/allergens`, `GET /api/v1/admin/nutrition`, `DayNutritionPort.totals`, truy vấn có `CalorieFilterSql` → `FakeLlmClient` nhận 0 lần gọi (US2-3, FR-007, SC-005); thời gian publish với `mode=offline` và `mode=mock` chênh không quá 10% (SC-005, NFR-002); `mode=offline` thì `GET` tuần của 030 vẫn 200 (US2-5) trong src/backend/src/test/java/com/wikicook/nutrition/NoAiCallOnReadIT.java
- [ ] T070 [P] Integration test `AiRequestRetentionJobIT` – xóa dòng `NUTRITION` cũ hơn 30 ngày và `MEAL_PLANNER` cũ hơn 90 ngày, giữ dòng mới hơn (NFR-009, research R16) trong src/backend/src/test/java/com/wikicook/ai/quota/AiRequestRetentionJobIT.java
- [ ] T071 Cài `AiRequestRetentionJob` (`@Scheduled(cron = "0 0 4 * * *", zone = "Asia/Ho_Chi_Minh")`, chỉ xóa log, không gọi AI) trong src/backend/src/main/java/com/wikicook/ai/quota/AiRequestRetentionJob.java (làm T070 xanh) ⛓ 031 (bỏ phần dọn `MEAL_PLANNER` khỏi job đối soát embedding, sửa 031 data-model mục 1)
- [ ] T072 [P] Soạn src/backend/src/test/resources/nutrition/reference-dishes.csv (20 món Việt thông dụng: tên, nguyên liệu + định lượng, khẩu phần, kcal/khẩu phần theo Bảng thành phần thực phẩm Việt Nam – Viện Dinh dưỡng) và test `ReferenceAccuracyEvaluation` (chỉ chạy khi `-Dnutrition.eval=true` và `mode=live`, đạt khi ≥ 16/20 món trong ±25%, ghi kết quả vào docs/test/090-nutrition-accuracy.md) trong src/backend/src/test/java/com/wikicook/nutrition/ReferenceAccuracyEvaluation.java (SC-006) ⛓ C soạn, D duyệt ⛓ Nhóm (provider)
- [ ] T073 [P] Integration test `NutritionPrivacyIT` – prompt ghi bởi `FakeLlmClient` cho mọi công thức seed không chứa email, tên tác giả, tiêu đề, id (FR-004, NFR-004); log ở mức DEBUG không chứa giá trị `WIKICOOK_AI_API_KEY` (NFR-003); response `html-label.json` không để lại chuỗi HTML nào trong `diet_labels` (NFR-005) trong src/backend/src/test/java/com/wikicook/nutrition/NutritionPrivacyIT.java
- [ ] T074 [P] Chạy kiểm tra bảo mật thủ công theo quickstart mục 5 (Network tab, `grep` bundle) và ghi kết quả vào docs/test/090-nutrition-security.md
- [ ] T075 [P] Kiểm tra thủ công `NutritionBlock`, `AllergenArea`, `AiDietLabels`, trang admin ở 360 px (không cuộn ngang), chỉ bàn phím, NVDA/VoiceOver đọc trạng thái khối và cảnh báo dị ứng (NFR-006, NFR-008); ghi vào docs/test/090-nutrition-a11y.md
- [ ] T076 [P] So sánh OpenAPI do springdoc sinh (`/v3/api-docs`) với specs/090-nutrition-analysis/contracts/openapi.yaml, sửa chỗ lệch trong controller/DTO hoặc trong contract
- [ ] T077 Chạy `./mvnw verify` và bảo đảm JaCoCo ≥ 70% line coverage cho `com.wikicook.nutrition.service` (rule của T003 trong src/backend/pom.xml)
- [ ] T078 Chạy quickstart N1–N29 (specs/090-nutrition-analysis/quickstart.md) và ghi kết quả vào docs/test/090-nutrition-acceptance.md
- [ ] T079 [P] Lập ma trận truy vết acceptance scenario (US1-1 … US7-6) và edge case → test tương ứng trong docs/test/090-nutrition-traceability.md (SC-008)
- [ ] T080 [P] Chuẩn bị và chạy usability test (≥ 5 người: nhận ra số liệu là ước tính, phân biệt cảnh báo dị ứng với nhãn AI), ghi tỉ lệ đạt so với SC-007 (≥ 90%) vào docs/test/090-nutrition-usability.md
- [ ] T081 [P] Cập nhật docs/analysis-and-design/database/full-schema.md mục 7 (tách `V302__ai_requests_embeddings.sql` / `V303__recipe_nutrition.sql`) và mục 4.7 (job dọn `ai_requests`) sau khi nhóm đồng ý – bỏ qua nếu 031 T103 đã làm phần mục 7 ⛓ Nhóm
- [ ] T082 [P] Đồng bộ specs/090-nutrition-analysis sang src/specs/090-nutrition-analysis theo src/README.md mục 5

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: bắt đầu ngay; T005 phụ thuộc Foundational của 031 (hoặc tự làm các task đó).
- **Foundational (Phase 2)**: sau Setup – CHẶN mọi story. Test Foundational cần `AbstractPostgresIT` (030 T004).
- **US3 (Phase 3)**: chỉ cần Foundational; không cần DB cho validator.
- **US2 (Phase 4)**: cần US3 (validator, prompt).
- **US1 (Phase 5)**: cần Foundational; dữ liệu thật đến từ US2 (test có thể seed thẳng `recipe_nutrition`).
- **US4 (Phase 6)**: chỉ cần Foundational (allergen không phụ thuộc AI); phần `AiDietLabels` dùng dữ liệu của US2. Có thể làm song song với US3/US2.
- **US5 (Phase 7)**: cần US2.
- **US6 (Phase 8)**: cần Foundational (`NutritionVisibilitySql`); có thể làm song song với US1–US5.
- **US7 (Phase 9)**: cần US2 (hàng đợi, `EstimationService`).
- **Polish (Phase 10)**: sau các story cần thiết; T069 cần US1, US4, US6, US7.

### Within Each User Story

- Test viết trước và FAIL → entity/query → service → controller → frontend.
- Task "(làm Txxx xanh)" chỉ xong khi test đó xanh.

### Phụ thuộc với spec khác

| 090 task | Cần từ | Cách làm tạm khi chưa có | Khi bên kia xong |
|---|---|---|---|
| T005, T034, T020 | 031 T015–T018, T020, T024 (`ai`, V302, `FakeLlmClient`) | Tự làm đúng các task đó theo contract 031 (một bản) | – |
| T006 và mọi IT | 030 T004 `AbstractPostgresIT` | Tạo base class giống 030 T004 trong cùng package test chung | Dùng bản của 030 |
| T047 | 030 T020 `DietaryProfilePort`, A (011) cài port | Bean mặc định của 030 (hồ sơ rỗng → không highlight) | Highlight thật |
| T058 | 030 T020 (port), T097 (gắn vào `getWeek`), T098 (`DayNutritionBadge`) | Test port độc lập | 030 hiện tổng ngày |
| T035, T053 | B (060): `RecipePublishedEvent`, `RecipeRevisionApprovedEvent`, `RecipesImportedEvent` | Test tự publish event | B publish trong `RecipeService` |
| T043 | B (060): trang chi tiết công thức | Route `/dev/nutrition/:recipeId` ở dev | B nhúng component |
| T059, T060 | B (020): truy vấn tìm kiếm, thẻ kết quả | Test với truy vấn mẫu | 020 ghép mệnh đề |
| T018 | A (010): `AuditLogPort` | `JdbcAuditLogWriter` mặc định | Bean của 010 thay thế |
| T041, T067 | A (010): SecurityConfig, role ADMIN | Cấu hình tạm trong test slice | Dùng cấu hình chung |
| T020 | A (011): mã allergen trong seed | Seed riêng cho test | Đối chiếu mã thật |
| T068 | E (100): khu admin | Route `/admin/nutrition` độc lập | Đặt vào menu admin |
| T072 | Nhóm: provider + `daily-cap.total` | `mode=mock` | Chạy với LLM thật |

### Parallel Opportunities

- Setup: T002–T004 song song.
- Foundational: mọi task test (T006 … T012) song song; T014, T015, T017, T018, T020, T021, T022 song song.
- US3: ba task test song song; US4 và US6 có thể giao người khác ngay sau Foundational.
- US7 backend và frontend song song.

---

## Parallel Example: User Story 2

```bash
# Viết toàn bộ test US2 cùng lúc (phải FAIL):
Task: "T028 NutritionQueueTest in src/backend/src/test/java/com/wikicook/nutrition/service/NutritionQueueTest.java"
Task: "T029 EstimationServiceTest in src/backend/src/test/java/com/wikicook/nutrition/service/EstimationServiceTest.java"
Task: "T030 NutritionTriggerListenerIT in src/backend/src/test/java/com/wikicook/nutrition/service/NutritionTriggerListenerIT.java"
Task: "T031 EstimationFlowIT in src/backend/src/test/java/com/wikicook/nutrition/service/EstimationFlowIT.java"
Task: "T032 InterruptedEstimatesCleanerIT in src/backend/src/test/java/com/wikicook/nutrition/service/InterruptedEstimatesCleanerIT.java"

# Sau đó code (cùng package service nhưng file khác nhau; T034 cần T033 cho NutritionTask):
Task: "T033 NutritionQueue + NutritionWorker"
Task: "T034 EstimationService"
Task: "T035 NutritionTriggerListener"
Task: "T036 InterruptedEstimatesCleaner"
```

---

## Implementation Strategy

### MVP First (toàn bộ P1)

1. Phase 1 Setup + Phase 2 Foundational.
2. Phase 3 US3 (validator) → Phase 4 US2 (chạy nền) → Phase 5 US1 (khối dinh dưỡng).
3. Phase 6 US4 (allergen + nhãn AI) – cảnh báo dị ứng là lý do chính của module nên nằm trong MVP.
4. **DỪNG và kiểm tra**: quickstart N1–N11, N16–N19; demo trang công thức.

### Incremental Delivery

1. MVP (US3 + US2 + US1 + US4) → demo.
2. US5 (theo bản sửa) → số liệu luôn đúng phiên bản.
3. US6 (030, 020) → khi 030 Phase 10 và 020 sẵn sàng.
4. US7 (admin) → cách sửa ước tính lỗi; cần trước khi seed import 1.000 món.
5. Polish trước Build v0.8: T072 với LLM thật, T073, T075, T078.

### Chia việc (team)

- C (chủ spec): Foundational, US3, US2, US5, T072.
- Người thứ hai (sau Foundational): US4 + US1 frontend, rồi US7 frontend.
- US6 làm cùng lúc với C (030) và B (020); US7 backend sau US2.

---

## Notes

- [P] = file khác nhau, không phụ thuộc task chưa xong.
- Mỗi task test phải chạy FAIL trước; commit sau mỗi task hoặc nhóm task.
- Không tự retry, không job quét ước tính: chỉ admin "Recalculate" gọi lại AI (FR-006).
- Output AI luôn qua `NutritionValidator` và chỉ hiển thị dạng text (NFR-005).
- Mã scenario (US1-3…) trong mô tả test dùng cho ma trận truy vết T079.
