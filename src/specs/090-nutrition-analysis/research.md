# Research: Nutrition & Allergen Analysis (090)

Phase 0 của [plan.md](plan.md). Mỗi mục có Decision / Rationale / Alternatives considered. Bảng và
cột theo `docs/analysis-and-design/database/full-schema.md` (mục 4.3–4.7, 4.10).

## R1. Đối chiếu yêu cầu của plan với full-schema

Prompt lập plan dùng tên của bản nháp CSDL cũ. Full-schema đã rút gọn, nên plan theo full-schema:

| Prompt | Full-schema (dùng) | Ghi chú |
|---|---|---|
| `recipe_nutrition.status` có `HIDDEN` | `status IN ('CALCULATING','AVAILABLE','FAILED')` + `is_hidden` | Ẩn là cờ riêng nên "tính lại thành công vẫn giữ ẩn" (FR-025) không cần nhớ trạng thái cũ |
| `fingerprint` | `input_fingerprint CHAR(64)` | |
| `left_out JSON` | `excluded_ingredients JSONB` | |
| bảng `recipe_dietary_tags` | cột `recipe_nutrition.diet_labels VARCHAR(15)[]` + `CHECK <@` 6 nhãn | Nhãn chỉ để hiển thị, không cần bảng để lọc |
| bảng `nutrition_attempts` | `ai_requests` với `feature = 'NUTRITION'` | Một bảng cho mọi lượt dùng AI (031 + 090) |
| bảng `ai_usage` | `ai_requests` + `AiQuotaService` của 031 | Hạn mức ngày = `COUNT(*) WHERE called_llm` |

- **Decision**: Theo full-schema như bảng trên. Lý do lỗi (`last_error VARCHAR(30)`) dùng mã ở R7.
- **Rationale**: Full-schema là bản cả nhóm dùng (commit c76a8c4); 031 đã tạo `ai_requests`.
- **Alternatives considered**: Tạo đúng các bảng của prompt – trùng với `ai_requests`, lệch
  schema chung.

## R2. Package `ai` dùng chung với 031

- **Decision**: 090 **dùng lại** `com.wikicook.ai` do 031 định nghĩa (031 contracts/integration.md
  mục 1): `LlmClient.complete(request, timeout, token)` (timeout 60 s, JSON mode), `AiQuotaService`
  (`reserve`/`finish`), chế độ `wikicook.ai.mode = live | mock | offline`, key `WIKICOOK_AI_API_KEY`
  chỉ từ env, không log. 090 không thêm client, không thêm thư viện.
  - Nếu 090 làm trước 031: task Foundational của 090 cài phần `ai` theo đúng contract của 031 (một
    lần, không làm hai bản).
- **Rationale**: Một hạn mức chung (FR-020 ↔ 031 FR-025a); không trùng code gọi provider.
- **Alternatives considered**: Client riêng trong `nutrition` – hai chỗ giữ key và hai cách đếm
  hạn mức.

## R3. Xử lý bất đồng bộ: `@Async` hay job queue trong DB

| Tiêu chí | A. Hàng đợi trong bộ nhớ (`@TransactionalEventListener(AFTER_COMMIT)` → `NutritionQueue`) | B. Job queue trong DB (bảng `nutrition_jobs`, worker poll `FOR UPDATE SKIP LOCKED`) |
|---|---|---|
| Publish không chờ AI (FR-002) | ✅ listener chỉ đưa vào hàng đợi | ✅ ghi một dòng job trong cùng transaction |
| Mất việc khi restart | ❌ việc đang chờ mất → xử lý ở R4 | ✅ còn trong bảng |
| Schema | Không thêm bảng | Thêm bảng ngoài full-schema |
| Ưu tiên publish thường hơn seed import | `PriorityBlockingQueue` | Cột `priority` + `ORDER BY` |
| Poll định kỳ | Không | Có (gần giống "job quét" spec đã bỏ) |
| Độ phức tạp | Thấp | Trung bình (lease, worker, dọn job) |

- **Decision**: **A**. Listener `@TransactionalEventListener(phase = AFTER_COMMIT)` ghi/đặt dòng
  `recipe_nutrition` (R5) rồi đưa `NutritionTask(recipeId, fingerprint, trigger, actorId)` vào
  `NutritionQueue`:
  - `PriorityBlockingQueue` – `APPROVAL`, `REVISION`, `ADMIN_RECALC` trước `IMPORT`, cùng ưu tiên thì
    vào trước ra trước. Publish thường không phải chờ 1.000 món seed (NFR-001).
  - **1 worker** (virtual thread) gọi LLM lần lượt, giữ khoảng cách tối thiểu giữa hai lần gọi
    `wikicook.ai.nutrition.min-interval` (mặc định 4 s) để không vượt giới hạn RPM của free tier.
  - Sức chứa `wikicook.ai.nutrition.queue-capacity` (mặc định 5.000); đầy → `FAILED`, `QUEUE_FULL`.
- **Rationale**: Spec chọn "gọi một lần, không retry, không quét đêm"; lượng gọi vài lần/ngày; một
  instance (giống 031 R9). Mất việc khi restart được xử lý tường minh (R4), không cần bảng mới.
- **Alternatives considered**: B – an toàn khi restart nhưng thêm bảng và vòng poll; `@Async` thuần
  với `ThreadPoolTaskExecutor` – không có ưu tiên và không giãn cách được, seed import sẽ đụng 429.

## R4. Restart khi còn việc chưa xong

- **Decision**: Khi ứng dụng khởi động (`ApplicationReadyEvent`), các dòng `status = 'CALCULATING'`
  được đổi thành `FAILED`, `last_error = 'INTERRUPTED'`, **không gọi AI**. Admin thấy trong danh sách
  (lọc "failed") và bấm "Recalculate".
- **Rationale**: Khối dinh dưỡng không bị kẹt ở "đang tính" mãi; giữ đúng FR-006 (không tự gọi lại).
- **Alternatives considered**: Tự đưa lại vào hàng đợi – là retry tự động, trái FR-006 / Clarifications.

## R5. Fingerprint và kết quả về muộn (FR-005, FR-008, US5)

- **Decision**:
  - `input_fingerprint` = SHA-256 (hex, 64 ký tự) của chuỗi chuẩn hóa:
    `servings` + với mỗi `recipe_ingredients` theo `position`:
    `ingredient_id|quantity (bỏ số 0 thừa, NULL → "-")|unit_id|is_optional`.
    Dùng `ingredient_id` (không dùng tên) nên sửa chính tả tên nguyên liệu không làm tính lại.
  - Mọi trigger trừ `ADMIN_RECALC`: nếu đã có dòng với **cùng fingerprint** (bất kể trạng thái,
    kể cả `FAILED`) → không làm gì (FR-008: sửa chỉ chữ, khôi phục ARCHIVED không đổi; FR-006: lượt
    lỗi chỉ được gọi lại qua admin). Cờ
    `ingredientsOrServingsChanged` của event chỉ để bỏ qua sớm, nguồn quyết định là fingerprint.
  - Fingerprint khác → cập nhật dòng: `status = CALCULATING`, `input_fingerprint` mới, xóa số liệu
    và nhãn cũ (giá trị cũ thôi hiển thị ngay, US5-1), giữ `is_hidden`.
  - Ghi kết quả bằng `UPDATE recipe_nutrition SET … WHERE recipe_id = :id AND input_fingerprint =
    :fpLúcBắtĐầu`. 0 dòng → kết quả cũ, bỏ; `ai_requests.reason = 'STALE'`.
- **Rationale**: Một điều kiện SQL thay cho so sánh trong code; đúng edge case "bản sửa được duyệt khi
  bản trước đang tính".
- **Alternatives considered**: So sánh `recipes.version` – đổi cả khi chỉ sửa chữ (sai FR-008).

## R6. Hai trigger cùng lúc (edge case)

- **Decision**:
  - `NutritionQueue` giữ tập `inFlight` theo khóa `(recipeId, fingerprint)` cho việc đang chờ và
    đang chạy. Việc mới trùng khóa → bỏ (log debug), không ghi `ai_requests` vì không gọi AI.
  - Fingerprint khác (bản sửa mới) → vẫn chạy; kết quả của việc cũ bị bỏ ở bước ghi (R5).
  - PK `recipe_nutrition.recipe_id` + `INSERT … ON CONFLICT (recipe_id) DO UPDATE` bảo đảm mỗi
    công thức chỉ một dòng hiện hành.
- **Rationale**: Một instance; dedupe trong bộ nhớ đủ và không giữ connection DB suốt 60 s.
- **Alternatives considered**: `pg_advisory_lock` session-level giữ trong lúc gọi LLM – chiếm một
  connection tới 60 s; cột "đang tính lại" – thêm cột ngoài schema.

## R7. Gọi AI và nhóm lý do lỗi

- **Decision**:
  - Gửi: `servings`, danh sách nguyên liệu **được tính** (`name`, `quantity`, `units.name_vi`).
    Nguyên liệu "vừa ăn" (`units.requires_quantity = false` hoặc `quantity IS NULL`) và
    `is_optional` **không gửi** – được ghi vào `excluded_ingredients` (FR-012, FR-013). Không có id,
    tên tác giả, tiêu đề công thức (FR-004, NFR-004).
  - Hỏi **tổng của cả công thức** (`kcal`, `protein_g`, `carbs_g`, `fat_g`, `diet_labels[]`);
    server chia cho `servings` rồi kiểm tra theo khẩu phần. LLM cộng theo nguyên liệu tự nhiên hơn,
    phép chia do code làm nên không sai số học.
  - Không còn nguyên liệu nào để tính → không gọi AI, `FAILED`, `NOTHING_TO_ESTIMATE`.
  - `recipes.servings` NULL (dữ liệu cào có thể thiếu) → không gọi AI, `FAILED`, `NO_SERVINGS`; không
    đoán số khẩu phần vì số liệu là "mỗi khẩu phần".
  - Prompt `prompts/nutrition.v1.txt`, `temperature = 0`, JSON schema cố định.
  - Mã lý do (`recipe_nutrition.last_error`, `ai_requests.reason`):
    `AI_ERROR`, `TIMEOUT`, `MALFORMED`, `NEGATIVE`, `OUT_OF_RANGE`, `MACRO_MISMATCH`, `USAGE_CAP`,
    `NOTHING_TO_ESTIMATE`, `NO_SERVINGS`, `INTERRUPTED`, `QUEUE_FULL`, `STALE`.
  - `ai_requests.outcome`: `SUCCESS`; `REJECTED` (validator); `TIMEOUT`; `QUOTA_CAP`; `FAILED` (còn lại).
- **Rationale**: Mỗi lượt có một dòng log với thời gian, kết quả, lý do (NFR-009).
- **Alternatives considered**: Gửi cả nguyên liệu tùy chọn với cờ `optional` – AI có thể cộng nhầm;
  hỏi trực tiếp theo khẩu phần – LLM hay quên chia.

## R8. Validator thuần (FR-009 – FR-011, US3)

- **Decision**: `NutritionValidator` là class Java thuần (không Spring, không DB), nhận
  `(rawJson, servings, CatalogueFacts)`, trả `Accepted(values, labels, droppedLabels)` hoặc
  `Rejected(reason)`. Luật chạy theo thứ tự, luật đầu tiên sai quyết định lý do:
  1. `MALFORMED` – không parse được, thiếu 1 trong 4 số, số không hữu hạn.
  2. `NEGATIVE` – có giá trị < 0.
  3. `OUT_OF_RANGE` – mỗi khẩu phần `kcal > 3000` hoặc một macro `> 300 g`.
  4. `MACRO_MISMATCH` – `E = 4P + 4C + 9F`; sai nếu `|kcal − E| > 0,2 × E`. Nếu `kcal < 20` và
     `E < 20` (ví dụ trà, nước dùng loãng) thì coi là khớp, tránh chia cho số rất nhỏ.
  5. Nhãn: chuẩn hóa (`"Gluten-Free"` → `GLUTEN_FREE`), bỏ nhãn ngoài 6 nhãn (FR-010), bỏ nhãn
     mâu thuẫn (FR-011) – không làm hỏng phần còn lại (US3-4).
  - Bảng mâu thuẫn (`CatalogueFacts` = mã allergen + `ingredients.kind` của **mọi** nguyên liệu, kể
    cả tùy chọn và "vừa ăn"):

    | Nhãn | Bị bỏ khi có |
    |---|---|
    | `GLUTEN_FREE` | allergen `GLUTEN` |
    | `DAIRY_FREE` | allergen `MILK` hoặc kind `DAIRY` |
    | `NUT_FREE` | allergen `PEANUT` hoặc `TREE_NUT` |
    | `VEGETARIAN` | kind `MEAT`, `SEAFOOD` |
    | `VEGAN` | kind `MEAT`, `SEAFOOD`, `EGG`, `DAIRY`, `HONEY` |
    | `LOW_SODIUM` | (không kiểm tra) |

  - Mã allergen phải khớp seed của 011 (A) – điểm cần chốt.
  - Giá trị lưu = làm tròn số nguyên sau khi chia (FR-015); kiểm tra dùng số chưa làm tròn.
- **Rationale**: Unit test từng luật không cần Spring (SC-003); output AI coi là không tin cậy.
- **Alternatives considered**: Bean Validation trên DTO – không diễn đạt được luật chéo (macro,
  catalogue).

## R9. Hạn mức (FR-020)

- **Decision**: Trước khi gọi LLM: `AiQuotaService.reserve(NUTRITION, trigger, null, recipeId, …)`
  (advisory lock theo feature, 031). `llmAllowed = false` → `QUOTA_CAP`, dòng thành `FAILED` với
  `USAGE_CAP` (với `ADMIN_RECALC`: giữ nguyên dòng, R10). Phần của nutrition:
  `wikicook.ai.daily-cap.nutrition-share` (đã có trong cấu hình 031, mặc định 0,3 × `total`).
- **Rationale**: Một trần chung trong free tier; không thêm bảng đếm.
- **Alternatives considered**: Đếm riêng trong `nutrition` – hai nguồn số liệu.

## R10. Admin "Recalculate" (FR-024, US7-2/3)

- **Decision**:
  - `POST /api/v1/admin/nutrition/{recipeId}/recalculate` → 202, đưa việc `ADMIN_RECALC` vào hàng
    đợi (ưu tiên cao). Chỉ cho công thức PUBLISHED; trùng việc đang chờ → 409 `ALREADY_QUEUED`.
  - Dòng hiện tại **không đổi** cho tới khi có kết quả hợp lệ (số cũ vẫn hiển thị, US7-2). Chưa có dòng
    → tạo dòng `CALCULATING`.
  - Thành công → ghi đè số, nhãn, `low_confidence`, `excluded_ingredients`, `computed_at`, xóa
    `last_error`; **giữ `is_hidden`** (FR-025).
  - Lỗi / bị từ chối → giữ nguyên số liệu; lý do xem ở lịch sử `ai_requests` (US7-3). Dòng chưa từng
    có số → `FAILED` với lý do.
  - Fingerprint dùng giá trị **hiện tại** của công thức (có thể khác dòng cũ, ví dụ dòng `FAILED`).
- **Rationale**: Đúng US7-2/3 mà không cần trạng thái thứ năm.

## R11. Hide / Unhide và audit (FR-025, FR-026)

- **Decision**: `POST …/hide {reason: 1–300 ký tự}` đặt `is_hidden = true`, `hidden_reason`,
  `hidden_by`; `POST …/unhide` xóa cả ba. Ghi `audit_logs` qua `AuditLogPort` của 010 (A) trong cùng
  transaction: `action` ∈ `NUTRITION_RECALC_REQUESTED`, `NUTRITION_HIDDEN`, `NUTRITION_UNHIDDEN`;
  `target_type = 'RECIPE'`, `target_id = recipeId`, `details = {reason}`. Port có bean mặc định ghi
  thẳng `audit_logs` bằng `JdbcTemplate` nếu 010 chưa có.
- **Rationale**: Audit cùng transaction → không có thao tác mà thiếu nhật ký.

## R12. "Hiển thị được" – một định nghĩa dùng chung

- **Decision**: Một ước tính **hiển thị được** khi
  `recipes.status = 'PUBLISHED' AND n.status = 'AVAILABLE' AND NOT n.is_hidden`.
  Dùng cho: khối dinh dưỡng, nhãn AI, `DayNutritionPort` (030), lọc calo (020). Đặt trong
  `NutritionVisibilitySql` (mệnh đề SQL + tham số) để mọi nơi cùng một luật (giống `DietRuleSql`).
  - Edge case "lưu trữ khi đang tính": kết quả vẫn ghi, nhưng không hiển thị vì không PUBLISHED.
- **Rationale**: FR-021, FR-022, FR-025 cùng nói "ẩn = không có số liệu ở mọi nơi".

## R13. Cung cấp cho 020 và 030

- **Decision**:
  - **030**: cài `DayNutritionPort.totals(recipeIdsByDay)` của 030 (contracts/integration.md của
    030): mỗi món đếm 1 khẩu phần/người; `includedDishes`/`missingDishes`; ngày không món nào có số
    → 030 hiện "Chưa có số liệu dinh dưỡng". Không cần endpoint REST riêng – 030 trả trong response
    tuần (030 research). Một truy vấn cho cả tuần (`WHERE recipe_id = ANY(:ids)`).
  - **020**: `CalorieFilterSql.clause(minKcal, maxKcal)` trả mệnh đề `EXISTS (… NutritionVisibilitySql
    … AND n.kcal BETWEEN …)` để 020 ghép vào truy vấn tìm kiếm; không có bộ lọc → 020 không ghép
    (US6-2). Index `(kcal) WHERE status = 'AVAILABLE' AND NOT is_hidden` đã có trong schema.
  - Thẻ kết quả tìm kiếm nếu có calo phải kèm disclaimer ngắn (FR-016) – 090 cung cấp component.
- **Rationale**: Đọc giá trị đã lưu, không gọi AI (FR-007, SC-005).
- **Alternatives considered**: Endpoint `GET /nutrition/day-totals` cho frontend 030 – thêm một
  request mỗi lần mở tuần, trong khi 030 đã thiết kế lấy qua port.

## R14. Endpoint public cho trang công thức

- **Decision**:
  - `GET /api/v1/recipes/{recipeId}/nutrition` (public): `state` ∈ `CALCULATING`, `NO_DATA`,
    `AVAILABLE`; `lowConfidence`; 4 số; `servings`; `dietLabels`; `excluded` (`toTaste[]`,
    `optional[]` – tên nguyên liệu). Không có lý do lỗi, không có `hidden_reason` (US1-4).
  - `GET /api/v1/recipes/{recipeId}/allergens` (public, xác thực tùy chọn): danh sách allergen
    không trùng từ `ingredient_allergens` (mọi nguyên liệu, kể cả tùy chọn); khi có token thì mỗi
    allergen có `inMyProfile` (đọc `user_allergens` qua `DietaryProfilePort`, FR-018). Không phụ thuộc
    trạng thái AI (US4-1, US4-5).
  - Hai endpoint tách nhau để allergen vẫn có khi phần AI lỗi, và cache được khác nhau
    (`Cache-Control: max-age=60` cho nutrition; allergen `no-store` khi có token).
  - Công thức không PUBLISHED hoặc không tồn tại → 404.
- **Rationale**: Tách dữ liệu an toàn (allergen) khỏi dữ liệu AI cả ở API, không chỉ ở UI (FR-017).

## R15. Danh sách admin (FR-023)

- **Decision**: `GET /api/v1/admin/nutrition?status=&q=&page=&size=` (role `ADMIN`):
  - `status` hiển thị suy ra: `NO_DATA` (không có dòng), `CALCULATING`, `AVAILABLE`, `LOW_CONFIDENCE`,
    `FAILED`, `HIDDEN` (ưu tiên `HIDDEN` khi `is_hidden`).
  - Cột: tên công thức, trạng thái, lần thử gần nhất (`LEFT JOIN LATERAL` lượt `ai_requests` mới nhất
    theo `recipe_id`), lý do lỗi, cờ "đang trong hàng đợi" (từ `NutritionQueue`).
  - `q` tìm theo tên không dấu (`f_unaccent(lower(title)) LIKE`), phân trang 20 dòng (constitution V).
  - `GET /api/v1/admin/nutrition/{recipeId}/attempts` – lịch sử 30 ngày (NFR-009).
  - Member/Contributor/Moderator → 403 (FR-027), cấu hình `/api/v1/admin/**` cần `ROLE_ADMIN`.
- **Rationale**: Đủ cho US7 mà không cần bảng phụ.

## R16. Giữ lịch sử ≥ 30 ngày (NFR-009)

- **Decision**: Không xóa sớm hơn 30 ngày. Dọn `ai_requests` gom về một job của package `ai`:
  `AiRequestRetentionJob` (04:00 giờ VN) xóa `NUTRITION` > 30 ngày và `MEAL_PLANNER` > 90 ngày.
  Job chỉ xóa log, không gọi AI (không trái "không có job quét đêm" – điều đó nói về ước tính).
  031 data-model đang để việc dọn `MEAL_PLANNER` chung với job đối soát embedding → đề xuất chuyển
  về job này (C quyết, cùng chủ).
- **Alternatives considered**: Không dọn – bảng lớn dần; mỗi spec một job – trùng.

## R17. Frontend

- **Decision**:
  - `features/nutrition/`:
    - `NutritionBlock` có 4 trạng thái theo `state` + `lowConfidence`.
    - `AllergenArea` là vùng riêng, có icon + chữ (không chỉ màu, NFR-008). Highlight dị ứng trong hồ sơ
      bằng nền + chữ "Có trong hồ sơ dị ứng của bạn".
    - `AiDietLabels` có tiêu đề "Gợi ý của AI" và kiểu chip khác cảnh báo.
    - `NutritionDisclaimer` dùng chuỗi cố định.
    - `CalorieValue` (số calo + disclaimer ngắn) cho 030/020.
  - `aria-live="polite"` cho trạng thái khối (NFR-008). Toàn bộ chuỗi trong `strings.vi.ts` (NFR-007).
  - Mất mạng / request lỗi (edge case "offline / từ cache"): giữ số và trạng thái đã tải lần trước
    kèm disclaimer; chưa tải được lần nào thì không hiện số (như `NO_DATA`).
  - Đang `CALCULATING`: hỏi lại mỗi 15 s, tối đa 5 phút (NFR-001), dừng khi rời trang; không gọi AI
    từ trình duyệt (FR-003).
  - Đổi số khẩu phần trên trang (nếu 060 có) không đổi số mỗi khẩu phần (US1-6) – component không
    nhận prop servings để tính lại.
  - Trang admin `NutritionAdminPage` (bảng, lọc, tìm, Recalculate, Hide/Unhide có hộp nhập lý do) ở
    route `/admin/nutrition`; vị trí trong khu admin do E (100) quyết.
  - Hiển thị output AI chỉ bằng text React (không `dangerouslySetInnerHTML`, NFR-005).
- **Rationale**: Component nhỏ để 060 (trang chi tiết), 030, 020 nhúng lại.

## R18. Test

- **Decision**:
  - `NutritionValidatorTest` – mỗi luật một nhóm test, cộng bảng mâu thuẫn nhãn.
  - `FakeLlmClient` (của 031) + bộ response mẫu trong `src/test/resources/nutrition/llm-responses/`:
    `valid.json`, `malformed.txt`, `missing-fat.json`, `negative.json`, `over-3000.json`,
    `macro-off-25pct.json`, `unknown-label.json`, `dairy-free-with-milk.json`, `slow-70s` (ngủ).
  - IT Testcontainers `pgvector/pgvector:pg16`: listener AFTER_COMMIT (rollback → không có việc),
    fingerprint/stale, dedupe, quota, restart → INTERRUPTED, admin + audit, 403.
  - SC-005: đo thời gian publish với `mode=offline` và `mode=mock`; đếm số lần gọi `LlmClient` khi
    GET công thức, tìm kiếm, tuần planner = 0.
  - SC-006: fixture `src/test/resources/nutrition/reference-dishes.csv` (20 món, kcal/khẩu phần theo
    Bảng thành phần thực phẩm Việt Nam – Viện Dinh dưỡng; C soạn, D duyệt). Test
    `ReferenceAccuracyEvaluation` chỉ chạy khi `-Dnutrition.eval=true` + `mode=live` (ngoài CI), ghi
    kết quả vào `docs/test/090-nutrition-accuracy.md`.
  - Frontend: Vitest + RTL + msw cho 4 trạng thái, disclaimer, allergen/AI tách vùng, highlight, 360 px.

## R19. Migration

- **Decision**: `V303__recipe_nutrition.sql` (tách từ `V302__ai.sql`, khớp 031 R18): bảng
  `recipe_nutrition` đúng full-schema 4.7 + CHECK + index `(kcal) WHERE status = 'AVAILABLE' AND NOT
  is_hidden`. `ai_requests` đã có từ V302 (031).

## Điểm cần chốt với nhóm

| # | Với | Nội dung | Đề xuất |
|---|---|---|---|
| 1 | B (060), E (100) | Event `RecipePublishedEvent`, `RecipeRevisionApprovedEvent(recipeId, ingredientsOrServingsChanged)`, `RecipesImportedEvent(ids)` publish trong transaction đổi trạng thái | Như 031 contracts mục 2 |
| 2 | A (011) | Mã allergen `GLUTEN`, `MILK`, `PEANUT`, `TREE_NUT` trong seed `allergens` | Như R8 |
| 3 | A (010) | `AuditLogPort.record(actorId, action, targetType, targetId, details)` | R11 |
| 4 | B (020) | Ghép `CalorieFilterSql`; thẻ kết quả có calo dùng `CalorieValue` | R13 |
| 5 | B (060) | Trang chi tiết công thức nhúng `NutritionBlock` + `AllergenArea` + `AiDietLabels` | R17 |
| 6 | E (100) | Vị trí trang `/admin/nutrition` trong khu admin | R15 |
| 7 | C (031) | Job dọn `ai_requests` chung; tách migration V302/V303 | R16, R19 |
| 8 | Nhóm | Provider + free tier quyết định `daily-cap.total` và `min-interval` | R3, R9 |
| 9 | Nhóm | Seed import 1.000 món vượt hạn mức ngày → nhiều `USAGE_CAP`; admin tính lại từng món. Có cần nút "tính lại tất cả món chưa có số liệu" không? (ngoài spec hiện tại) | Hỏi PO; nếu có → sửa spec trước |
