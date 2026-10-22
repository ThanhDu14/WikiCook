# Data Model: AI Weekly Meal Planner – 031

**Feature**: [spec.md](spec.md) | **Research**: [research.md](research.md)

Bảng gốc lấy từ [full-schema.md mục 4.7](../../../docs/analysis-and-design/database/full-schema.md#47-ai-planner-và-dinh-dưỡng-031-090--c).
Migration `V302__ai_requests_embeddings.sql` (research R18). Spec này **không thêm bảng** ngoài
schema; draft không được lưu.

## 1. Bảng sở hữu

### `recipe_embeddings` → entity `RecipeEmbedding` (package `aiplanner`)

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `recipe_id` | `BIGINT` | PK, FK → `recipes` `ON DELETE CASCADE` | 1–1 với công thức PUBLISHED |
| `embedding` | `VECTOR(768)` | NOT NULL | Map bằng `float[]`; đọc/ghi qua native SQL (`?::vector`) |
| `model` | `VARCHAR(50)` | NOT NULL | Ví dụ `gemini-embedding-001@768`; khác cấu hình → tạo lại |
| `content_hash` | `CHAR(64)` | NOT NULL | SHA-256 của văn bản đem embed (research R10) |
| `created_at`, `updated_at` | `TIMESTAMPTZ` | NOT NULL | `updated_at` = "last refreshed time" (Key Entities) |

Index `HNSW (embedding vector_cosine_ops)`. Truy vấn luôn `JOIN recipes … WHERE status = 'PUBLISHED'`.

### `ai_requests` → entity `AiRequest` (package `ai`, dùng chung với 090)

| Cột | Kiểu | Ràng buộc | Dùng trong 031 |
|---|---|---|---|
| `id` | `BIGINT` identity | PK | |
| `feature` | `VARCHAR(15)` | `CHECK IN ('MEAL_PLANNER','NUTRITION')` | `MEAL_PLANNER` |
| `user_id` | `BIGINT` | FK → `users` `CASCADE` | Người tạo; bắt buộc khi `MEAL_PLANNER` |
| `recipe_id`, `actor_id` | `BIGINT` | NULL | Không dùng (của 090) |
| `trigger` | `VARCHAR(15)` | `CHECK IN ('USER','APPROVAL','REVISION','IMPORT','ADMIN_RECALC')` | `USER` |
| `outcome` | `VARCHAR(15)` | `CHECK IN ('AI_DRAFT','AUTO_DRAFT','NOT_ENOUGH','SUCCESS','FAILED','REJECTED','TIMEOUT','CANCELLED','QUOTA_CAP')` | Xem mục 4 |
| `called_llm` | `BOOLEAN` | NOT NULL | true khi lượt này gọi LLM (tính vào hạn mức ngày) |
| `counted` | `BOOLEAN` | `CHECK (counted = (feature = 'MEAL_PLANNER' AND outcome = 'AI_DRAFT'))` | Tính vào 5 lượt/giờ (FR-026) |
| `week_start` | `DATE` | Bắt buộc khi `MEAL_PLANNER` | Tuần được tạo |
| `requested_slots`, `removed_picks` | `SMALLINT` | NULL | FR-032 |
| `constraints` | `JSONB` | NULL | Ràng buộc đã dùng (mục 3) |
| `reason` | `VARCHAR(100)` | NULL | Nhóm lý do lỗi: `LLM_ERROR`, `LLM_UNPARSEABLE`, `EMBEDDING_ERROR`, `DB_ERROR`… |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | Chỉ `created_at` |

Ràng buộc: `CHECK (feature <> 'MEAL_PLANNER' OR (user_id IS NOT NULL AND week_start IS NOT NULL))`.
Index: `(user_id, created_at DESC) WHERE counted`; `(feature, created_at) WHERE called_llm`.
Dọn dòng `MEAL_PLANNER` cũ hơn 90 ngày (job hằng ngày, chung với R10).

## 2. Dữ liệu dùng qua module khác

| Nguồn | Dùng cho | Cách đọc |
|---|---|---|
| `meal_plans`, `meal_plan_items` (030) | Ô trống, món đã có trong tuần, lưu khi confirm | `MealPlanWriteFacade` (030 contracts/integration.md 2.2) |
| `recipes`, `recipe_ingredients`, `ingredients.kind`, `ingredient_allergens`, `cuisines`, `categories` (060) | Lọc hard rule, candidate, văn bản embedding | `EligibleRecipeQuery`, `CandidateQuery` (native, chỉ đọc) + `DietRuleSql` |
| Hồ sơ ăn uống (011) | Giá trị mặc định của dialog | `DietaryProfilePort` (định nghĩa ở 030) |
| Event công thức (060) | Đồng bộ embedding | `RecipePublishedEvent` … (contracts/integration.md) |

## 3. Value object / DTO chính

| Tên | Trường | Ghi chú |
|---|---|---|
| `GenerationConstraints` | `allergenIds[]`, `excludedIngredientIds[]`, `dietType`, `servings` (1–20 hoặc `null` = "Theo công thức"), `maxCookTimeMinutes` (`null` hoặc ≥ 10), `preferredCuisineIds[]` | Lưu nguyên vào `ai_requests.constraints` |
| `GenerateRequest` | `requestId` (UUID), `days[]` (Day, 1–7 phần tử, không trùng, không ở quá khứ), `meals[]` (Meal, 1–3 phần tử), `constraints` | FR-003, FR-005 |
| `SlotKey` | `day`, `meal` | Dùng lại từ 030 |
| `Candidate` | `recipeId`, `title`, `meals`, `totalTimeMinutes`, `cuisineId`, `categoryId`, `mainIngredientIds[]`, `rank` | Chỉ tồn tại trong một lượt tạo |
| `DraftItemDto` | `day`, `meal`, `recipeId`, `recipeName`, `coverImageUrl`, `totalTimeMinutes`, `servings` (hoặc null), `source` (`AI`/`AUTO`), `dietType` | Định dạng ô như 030 `PlanItem` (proposal 4.6) |
| `DraftResponse` | `requestId`, `weekStart`, `mode` (`AI`/`AUTO`), `notice` (`null` / `AUTO_NO_AI` / `AI_DAILY_CAP`), `items[]`, `chooseSlots[]`, `removedPicks`, `remaining`, `nextAvailableAt` | FR-015, FR-016, FR-025 |
| `GenerationOptions` | `weekStart`, `selectableDays[]`, `emptySlots[]`, `defaults` (`GenerationConstraints`), `profileIncomplete`, `remaining`, `nextAvailableAt`, `aiAvailableToday` | research R12 |
| `ConfirmRequest` | `requestId`, `items[]` (`day`, `meal`, `recipeId`, `servings|null`, `source` ∈ MANUAL/AI/AUTO) | FR-018 |
| `ConfirmResult` | `saved`, `skippedOccupied`, `skippedUnavailable` | FR-019 |
| `SuggestionDto` | `recipeId`, `title`, `coverImageUrl`, `totalTimeMinutes`, `cuisineName`, `reason` | FR-035 |

**Draft ở client** (`useAiDraft`, research R13): `{ requestId, weekStart, mode, notice, items,
chooseSlots, constraints }` – chỉ nằm trong state của trang.

## 4. Vòng đời một lượt tạo (`ai_requests.outcome`)

```text
 POST generate ──▶ kiểm tra đầu vào ──(lỗi)──▶ 400 (không ghi dòng)
        │
        ├─ hết 5 lượt/giờ ──▶ 429 RATE_LIMITED (không ghi dòng)
        │
        ▼
 ghi dòng (outcome tạm FAILED) ──▶ Bước 1 lọc + Bước 2 truy hồi
        │                               │
        │                     ít hợp lệ hơn số ô ──▶ NOT_ENOUGH (422)
        ▼
 hết hạn mức ngày? ──có──▶ Bước 5 fallback ──▶ QUOTA_CAP + bản nháp AUTO (notice AI_DAILY_CAP)
        │ không
        ▼
 Bước 3–4 LLM + kiểm tra ──hợp lệ──▶ AI_DRAFT (counted = true)
        │ lần 2 vẫn RETRY / lỗi / quá 60 s
        ▼
 Bước 5 fallback ──▶ AUTO_DRAFT (hoặc TIMEOUT khi quá 60 s), bản nháp AUTO (notice AUTO_NO_AI)
        │ fallback cũng lỗi
        ▼
 FAILED + 503 BACKUP_UNAVAILABLE

 DELETE generations/{requestId} khi đang chạy ──▶ CANCELLED
 DELETE generations/{requestId} sau AI_DRAFT, chưa confirm, ≤ 10 phút (client mất mạng)
        ──▶ CANCELLED (counted = false, called_llm giữ nguyên, reason CLIENT_LOST)
```

Chỉ `AI_DRAFT` có `counted = true`. `called_llm = true` khi đã thực sự gửi ít nhất một request tới
LLM (kể cả khi lỗi hoặc hết giờ).

## 5. Quy tắc kiểm tra

| Quy tắc | Nơi kiểm tra | Nguồn |
|---|---|---|
| `weekStart` là thứ Hai, không phải tuần đã qua | `@MondayDate` (030) + service → `422 PAST_WEEK` | FR-001, Edge Cases |
| ≥ 1 ngày, ≥ 1 bữa; ngày không ở quá khứ | Bean Validation + service | FR-003, FR-005 |
| Servings 1–20 hoặc null; thời gian tối đa null hoặc ≥ 10 | `@Min/@Max` | FR-005, Edge Cases |
| Ô được chọn phải còn trống ít nhất 1 | service → `422 NO_EMPTY_SLOT` | Edge Cases |
| Pick của AI ∈ candidate, PUBLISHED, qua hard rule, không trùng | `DraftValidator` | FR-011, FR-013 |
| Số hợp lệ của mỗi bữa ≥ số ô của bữa đó | service → `422 NOT_ENOUGH_RECIPES` | FR-012, US4 |
| 5 lượt `counted` / 60 phút trượt | `AiQuotaService` (`ai_requests`) | FR-025, FR-026 |
| Hạn mức ngày theo phần của MEAL_PLANNER | `AiQuotaService` + advisory lock | FR-025a |
