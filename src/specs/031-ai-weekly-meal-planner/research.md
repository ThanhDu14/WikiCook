# Research: AI Weekly Meal Planner – 031

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Ngày**: 2026-10-18

Mỗi mục ghi theo dạng **Decision / Rationale / Alternatives considered**. Bố cục mã nguồn
(`src/backend` – `com.wikicook`, `src/frontend`) đã chốt ngày 2026-10-18, dùng chung với spec 030.
Không còn mục NEEDS CLARIFICATION; những điểm phụ thuộc người khác nằm ở mục cuối.

> **Đối chiếu với prompt plan**: prompt nhắc bảng `ai_usage` / `ai_generation_requests`, nhưng
> [full-schema.md mục 4.7](../../../docs/analysis-and-design/database/full-schema.md#47-ai-planner-và-dinh-dưỡng-031-090--c)
> đã gộp chúng (và `nutrition_attempts` của 090) vào **một bảng `ai_requests`**. Plan này dùng
> `ai_requests`. Package dùng chung `ai` được định nghĩa ở đây vì 031 làm trước 090.

---

## R1. Package dùng chung `com.wikicook.ai`

- **Decision**: Tách phần gọi AI ra package `com.wikicook.ai` dùng chung cho 031 và 090:
  - `LlmClient` – `LlmResult complete(LlmRequest req, Duration timeout, CancellationToken token)`;
    `LlmRequest` gồm system prompt, user prompt, JSON schema của output, temperature.
  - `EmbeddingClient` – `float[] embed(String text, EmbeddingTask task)` và `embedBatch(...)`.
  - `AiQuotaService` – `QuotaDecision reserve(AiFeature feature)` kiểm tra hạn mức ngày và ghi
    dòng `ai_requests` trong cùng transaction có `pg_advisory_xact_lock(hashtext(feature))`
    (full-schema 4.7); `finish(id, outcome, ...)` cập nhật kết quả.
  - Cài đặt chọn bằng `wikicook.ai.mode = live | mock | offline` (khớp công tắc `LLM_MODE` của
    project-plan): `live` gọi provider thật, `mock` trả dữ liệu dựng sẵn (CI, demo), `offline` luôn
    ném `AiUnavailableException` (để test fallback, SC-002a).
  - API key đọc từ biến môi trường `WIKICOOK_AI_API_KEY`; không log request/response chứa key;
    không trả key ra client (NFR-002, constitution IV).
- **Rationale**: Hai spec cùng một người (C) phụ trách, dùng chung hạn mức ngày (031 FR-025a,
  090 FR-020) và cùng bảng `ai_requests`; một chỗ để đổi provider.
- **Alternatives considered**: Mỗi spec tự gọi API – trùng code, hạn mức chung không kiểm soát
  được; Spring AI – thêm framework lớn (milestone, nhiều auto-config) cho 2 endpoint, khó mock
  timeout/hủy chính xác.

## R2. Nhà cung cấp LLM và cách gọi

- **Decision**: Mặc định Google Gemini (dòng **Flash**, có free tier) qua REST API
  `generateContent`, gọi bằng `RestClient` của Spring 6 (không thêm SDK). Bật JSON mode
  (`responseMimeType: application/json` + `responseSchema`), `temperature` 0.4. Tên model nằm ở
  cấu hình `wikicook.ai.llm.model`, không hard-code.
- **Rationale**: Architecture doc liệt kê "Gemini Pro / GPT-4"; ràng buộc CON-03 của project-plan
  là $0 → Gemini Flash free tier. Dùng REST trực tiếp giữ được timeout và hủy request theo ý mình.
- **Alternatives considered**: OpenAI GPT-4o-mini – không có free tier lâu dài; Gemini Pro – free
  tier giới hạn request/phút thấp hơn, chậm hơn cho 21 ô. **Cần nhóm xác nhận** ở buổi chốt
  provider (spec Assumptions, họp 14/10).

## R3. Mô hình embedding

- **Decision**: `gemini-embedding-001` với `outputDimensionality = 768` (khớp
  `recipe_embeddings.embedding VECTOR(768)`), task type `RETRIEVAL_DOCUMENT` cho công thức và
  `RETRIEVAL_QUERY` cho câu truy vấn. Tên model + số chiều nằm trong cấu hình; đổi model → job
  đối soát (R10) tạo lại vì cột `model` khác.
- **Rationale**: Đa ngôn ngữ (có tiếng Việt), free tier, cùng provider và cùng API key với LLM;
  hỗ trợ giảm số chiều nên giữ được schema 768.
- **Alternatives considered**: `text-embedding-004` – 768 chiều nhưng yếu với tiếng Việt;
  OpenAI `text-embedding-3-small` – 1536 chiều, trả phí; mô hình chạy local (multilingual-e5) –
  cần thêm một service Python trong Docker Compose. **Cần nhóm xác nhận** (full-schema câu hỏi 7).

## R4. Bước 1 – lọc hard rule bằng SQL (FR-008a, FR-009, FR-009a)

- **Decision**: `EligibleRecipeQuery` (native SQL, `NamedParameterJdbcTemplate`, tham số hóa) trả
  về công thức hợp lệ cho từng bữa được chọn:
  - `status = 'PUBLISHED' AND NOT is_snack`;
  - hợp bữa: `for_breakfast` / `for_lunch` / `for_dinner` theo bữa;
  - `total_time_minutes <= :maxCookTime` khi có;
  - `NOT EXISTS` nguyên liệu thuộc dị ứng (`ingredient_allergens.allergen_id IN :allergenIds`), thuộc
    danh sách loại trừ (`ingredient_id IN :excludedIds`), hoặc có `ingredients.kind` cấm theo kiểu ăn
    (VEGETARIAN: MEAT, SEAFOOD; VEGAN: MEAT, SEAFOOD, EGG, DAIRY, HONEY; PESCATARIAN: MEAT);
  - **không** đọc `recipe_nutrition.diet_labels` (FR-009a).
  Mệnh đề hard rule đặt trong lớp dùng chung `com.wikicook.common.diet.DietRuleSql` để spec 030
  (cảnh báo trong picker), 031 (loại bỏ) và 020 (lọc dị ứng) dùng cùng một định nghĩa.
- **Rationale**: Clarification 2026-10-18 – lọc trước rồi mới truy hồi; lọc bằng SQL thì không có
  công thức vi phạm nào lọt tới LLM (NFR-004). Index `(ingredient_id, recipe_id)` và index bữa của
  full-schema 4.5 phục vụ trực tiếp truy vấn này.
- **Alternatives considered**: Lọc sau truy hồi vector (post-filter) – top-K có thể toàn món vi phạm,
  phải lấy K rất lớn; để LLM tự tránh dị ứng – không đảm bảo an toàn.

## R5. Bước 2 – truy hồi candidate bằng pgvector

- **Decision**:
  - Câu truy vấn cho mỗi bữa được chọn, dựng từ ràng buộc, không chứa thông tin cá nhân (NFR-003),
    ví dụ "Món ăn sáng, ẩm thực Việt Nam, Nhật Bản, nấu dưới 30 phút". Mỗi lượt tạo tối đa 3 lần
    embed (một lần cho mỗi bữa).
  - SQL: `SELECT r.id … FROM recipe_embeddings e JOIN recipes r … WHERE r.id IN (<tập hợp lệ của
    bữa đó>) ORDER BY e.embedding <=> :queryVec LIMIT :k`.
  - `k` của mỗi bữa = `max(3 × số ô của bữa đó, 20)`; tổng không quá 90 (proposal 4.4: 60–90).
  - Công thức hợp lệ mà chưa có embedding vẫn được thêm vào cuối danh sách, xếp theo ẩm thực ưa
    thích rồi `rating_avg`, để món mới publish không bị bỏ sót (FR-029).
  - Khi `EmbeddingClient` lỗi: bỏ bước vector, xếp candidate bằng SQL (ẩm thực ưa thích →
    `rating_avg` → `id`) rồi vẫn đi tiếp tới LLM hoặc fallback.
- **Rationale**: Với ~1.000–10.000 công thức, lọc rồi xếp theo khoảng cách chỉ quét vài nghìn vector
  (< 50 ms) nên không cần tối ưu HNSW + filter phức tạp; index HNSW vẫn có ích cho gợi ý một ô.
- **Alternatives considered**: Một câu truy vấn cho cả tuần – lệch về một bữa; ANN không lọc rồi
  post-filter – thiếu candidate khi ràng buộc chặt.

## R6. Bước 3 – prompt và schema output của LLM

- **Decision**:
  - System prompt (tiếng Việt): vai trò "người xếp thực đơn, chỉ chọn trong danh sách"; các luật
    cứng; ưu tiên hợp bữa, đa dạng, dùng lại nguyên liệu, ẩm thực ưa thích.
  - User prompt: danh sách ô cần điền (`day`, `meal`); danh sách candidate dạng gọn `id | tên ≤ 80 ký
    tự | bữa | phút | ẩm thực | 5 nguyên liệu chính`; ràng buộc (không có tên, email hay id người
    dùng).
  - Output schema: `{"slots":[{"day":"MONDAY","meal":"BREAKFAST","recipeId":123}]}`.
  - Tên công thức là dữ liệu do người dùng tạo, coi như không tin cậy: cắt độ dài, bỏ xuống dòng; mọi
    output đều qua bước 4 nên một prompt injection trong tên món không vượt qua được hard rule.
  - Mẫu prompt để trong `src/backend/src/main/resources/prompts/meal-plan.v1.txt`, có version.
- **Rationale**: Proposal 4.4 bước 3–4. Output chỉ chứa id nên dễ kiểm tra và tốn ít token.
- **Alternatives considered**: Để LLM trả cả tên/thời gian – thừa và dễ sai; tool calling – không cần
  cho một lần chọn.

## R7. Bước 4 – kiểm tra kết quả (FR-013, FR-014)

- **Decision**: `DraftValidator` thuần Java:
  1. Parse JSON theo schema; lỗi parse **hoặc** tập (day, meal) khác tập ô yêu cầu → `RETRY`.
  2. Với từng pick: `recipeId` phải thuộc tập candidate, và đọc lại từ DB vẫn PUBLISHED và qua
     `DietRuleSql` (kiểm tra lần hai, phòng công thức vừa bị archive/sửa); không đạt thì bỏ pick và
     đánh dấu ô "Chọn món".
  3. Trùng công thức giữa các ô AI: giữ lần xuất hiện đầu theo thứ tự ngày → bữa, các ô sau thành
     "Chọn món" (FR-011).
  4. Đếm `removedPicks` cho FR-032.
  Retry đúng **một lần** với tin nhắn sửa lỗi ("Thiếu ô X, Y" / "JSON không hợp lệ"); lần hai vẫn
  `RETRY` → fallback (R8).
- **Rationale**: NFR-004 và SC-003 (0 món vi phạm) phải đúng bất kể LLM trả gì.
- **Alternatives considered**: Thay pick sai bằng món khác tự động – spec yêu cầu để trống và đánh
  dấu "Chọn món" (US3).

## R8. Bước 5 – bản nháp dự phòng không dùng AI (FR-027, SC-002a)

- **Decision**: `BackupPlanner` greedy, tất định:
  - Duyệt ô theo ngày → bữa. Với mỗi ô, chọn candidate (của bữa đó, chưa dùng) có điểm cao nhất:
    `+3` nếu thuộc ẩm thực ưa thích, `+1` cho mỗi nguyên liệu trùng với các món đã chọn (tối đa
    +3, khuyến khích dùng lại nguyên liệu), `-2` nếu cùng `category` với món đã chọn của ngày trước
    (tăng đa dạng); hòa điểm thì xếp theo thứ hạng truy hồi rồi `id`.
  - Ô không còn candidate → "Chọn món" (US6-5).
  - `source = AUTO`; notice "Tạo tự động, không dùng AI", hoặc "AI đã hết lượt hôm nay" khi bị chặn
    bởi hạn mức ngày (FR-025a).
  - Dùng lại candidate đã có từ bước 1–2, nên không gọi thêm dịch vụ ngoài nào; độ phức tạp
    O(ô × candidate) chạy trong vài ms.
  - Nếu bước 1 cũng lỗi (DB không truy cập được) → `503 BACKUP_UNAVAILABLE` (US6-6).
- **Rationale**: Project-plan yêu cầu "fallback trả plan hợp lệ ≤ 2 s khi tắt LLM"; greedy theo điểm
  đơn giản, dễ test và không bao giờ vi phạm hard rule vì chỉ chọn trong tập đã lọc.
- **Alternatives considered**: Chọn ngẫu nhiên – kết quả không tất định, khó test; giải tối ưu
  (ILP) – quá mức cần.

## R9. Thời gian, đồng bộ hay bất đồng bộ, và hủy

- **Decision**:
  - `POST …/generations` là **đồng bộ**, tối đa 60 s, chạy trên virtual thread
    (`spring.threads.virtual.enabled=true`).
  - Ngân sách 60 s tính cho cả hai lần gọi LLM: lần retry chỉ chạy khi còn ≥ 15 s, nếu không thì
    chuyển sang fallback ngay. Read timeout của HTTP client = thời gian còn lại.
  - Client sinh `requestId` (UUID) và gửi trong body. Khi người dùng bấm hủy: `AbortController.abort()`
    và gọi `DELETE /api/v1/ai-planner/generations/{requestId}`. Server giữ
    `Map<requestId, Future>` (in-memory, theo userId), hủy future (interrupt → đóng kết nối HTTP tới
    provider) và ghi `outcome = CANCELLED`, `counted = false`.
  - Mỗi người dùng chỉ có tối đa 1 lượt tạo đang chạy; lượt thứ hai → `409 GENERATION_IN_PROGRESS`.
  - **Mất mạng khi đang tạo** (edge case "nếu chưa thấy bản nháp thì không tính lượt"): server vẫn
    chạy xong và ghi `AI_DRAFT`, nhưng response không tới client. Lúc đó client giữ `requestId` trong
    bộ nhớ (không lưu storage). Khi có mạng lại (`online` event), client gọi
    `DELETE /generations/{requestId}`.
    - Sau khi một lượt kết thúc, `InFlightGenerations` giữ `requestId → (dòng ai_requests, thời
      điểm kết thúc, đã confirm?)` trong 10 phút.
    - Nhận `DELETE` cho lượt đã xong, có `outcome = AI_DRAFT`, chưa confirm và còn trong 10 phút thì
      server gọi `AiQuotaService.finish(…, CANCELLED, calledLlm = true, …, reason = "CLIENT_LOST")`.
      Kết quả là `counted = false`, nhưng `called_llm` giữ nguyên nên lượt vẫn tính vào hạn mức
      ngày.
    - `POST …/confirm` với `requestId` đó đánh dấu "đã confirm"; sau đó `DELETE` không hoàn lượt.
  - Giới hạn đã chấp nhận:
    - Người dùng reload hoặc đóng tab trước khi có mạng lại thì client mất `requestId`, nên lượt
      vẫn bị tính.
    - Server khởi động lại thì map bị xóa, nên lượt cũng vẫn bị tính.
    - Client cố ý gọi `DELETE` sau khi đã nhận bản nháp chỉ né được giới hạn 5 lượt/giờ, không né
      được hạn mức ngày, nên chi phí provider vẫn có trần.
- **Rationale**: FR-023 (60 s, cho phép hủy); NFR-001 (90% ≤ 30 s). Servlet không phát hiện được
  client ngắt kết nối một cách tin cậy (ghi response vào kết nối đã chết thường vẫn thành công vì có
  buffer), nên cần lệnh hủy tường minh, và client phải tự báo khi mất mạng.
- **Alternatives considered**: Job bất đồng bộ + polling/SSE – thêm bảng job và endpoint trạng thái
  mà spec không cần (draft chỉ ở client); WebFlux – đổi mô hình lập trình cho cả dự án.

## R10. Đồng bộ embedding (FR-029 – FR-031, US8)

- **Decision**:
  - `RecipeEmbeddingSync` nghe event của module công thức (spec 060, B): `RecipePublishedEvent`,
    `RecipeRevisionApprovedEvent`, `RecipeArchivedEvent`, `RecipesImportedEvent` bằng
    `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`, nên lỗi không bao giờ chặn
    publish (FR-031).
  - Văn bản đem embed: tên, danh mục, ẩm thực, bữa, tổng thời gian, danh sách tên nguyên liệu.
    `content_hash` = SHA-256 của văn bản; trùng hash và trùng `model` thì bỏ qua (không tốn lượt).
  - Archive → xóa dòng `recipe_embeddings`. FR-030 vẫn đúng ngay cả trước khi xóa, nhờ bước 1 lọc
    `PUBLISHED` và bước 4 kiểm tra lại.
  - Job `@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Ho_Chi_Minh")` đối soát: tạo/sửa embedding cho
    công thức PUBLISHED thiếu dòng, lệch hash hoặc lệch model; xóa dòng của công thức không còn
    PUBLISHED. Chạy theo lô 50, nghỉ giữa các lô để giữ trong giới hạn request/phút của free tier.
    Dữ liệu cào nhập lần đầu cũng đi qua job này.
- **Rationale**: Proposal 4.8 ("embed lúc publish và re-index hằng đêm"); job đêm bảo đảm ≤ 24 h kể
  cả khi event bị lỡ (server restart, provider lỗi).
- **Alternatives considered**: Chỉ dùng event – lỡ event thì công thức không bao giờ được embed; chỉ
  dùng job – công thức mới phải chờ tới đêm. Chạy nhiều instance cần ShedLock, nhưng hiện chỉ có 1
  instance nên ghi lại để làm sau.

## R11. Giới hạn 5 lượt/giờ và hạn mức ngày: DB hay Redis (FR-025, FR-025a, FR-026, FR-032)

- **Decision**: Đếm trên bảng **`ai_requests`** trong PostgreSQL, không dùng Redis.
  - Lượt còn lại của người dùng = `5 − COUNT(*) WHERE feature = 'MEAL_PLANNER' AND user_id = ? AND
    counted AND created_at > now() − interval '60 minutes'` (index `(user_id, created_at DESC) WHERE
    counted`). `nextAvailableAt` = `created_at` cũ nhất trong cửa sổ + 60 phút.
  - Mọi lượt tạo đều ghi một dòng (FR-032). Ghi ngay lúc bắt đầu (`outcome = 'FAILED'` tạm thời,
    `called_llm` = có định gọi LLM hay không) để giữ chỗ trong hạn mức ngày, rồi cập nhật ở cuối:
    `AI_DRAFT` (`counted = true`), `AUTO_DRAFT`, `NOT_ENOUGH`, `FAILED`, `TIMEOUT`, `CANCELLED`,
    `QUOTA_CAP`. CHECK `counted = (feature = 'MEAL_PLANNER' AND outcome = 'AI_DRAFT')` của schema bảo
    đảm FR-026.
  - Hạn mức ngày đếm số **dòng** `called_llm` từ 00:00 giờ VN. Retry trong cùng một lượt vẫn là một
    dòng, nên trần cấu hình (`wikicook.ai.daily-cap.total`, `…meal-planner-share`,
    `…nutrition-share`) cần chừa biên cho retry (khoảng 2 lời gọi provider mỗi dòng).
- **Rationale**: Log FR-032 vốn phải lưu trong DB; đếm từ chính log thì không bao giờ lệch giữa log
  và giới hạn. Khối lượng rất nhỏ (vài nghìn dòng/ngày) nên `COUNT` có index là đủ nhanh. Advisory
  lock xử lý đúng trường hợp hai request đồng thời.
- **Alternatives considered**: Redis (`INCR` + TTL, hoặc sorted set cho cửa sổ trượt) – nhanh, nhưng
  phải giữ log ở DB song song, cần script Lua cho cửa sổ trượt nguyên tử, và test phải chạy thêm
  container Redis. Project-plan có nhắc "Redis plan cache / rate limit cho AI endpoint", nhưng spec
  031 không có cache plan (draft chỉ ở client); Redis để dành cho rate limit đăng nhập của spec 010.

## R12. Dữ liệu cho dialog ràng buộc (FR-002 – FR-006, FR-025)

- **Decision**: `GET /api/v1/ai-planner/weeks/{weekStart}/options` trả về:
  - ràng buộc mặc định lấy từ `DietaryProfilePort` của spec 030 (dị ứng, loại trừ, kiểu ăn,
    household size hoặc `null` = "Theo công thức", thời gian tối đa, ẩm thực ưa thích);
  - `profileIncomplete`; ngày chọn được (từ hôm nay với tuần hiện tại, cả tuần với tuần sau, rỗng với
    tuần đã qua); ô trống (`MealPlanWriteFacade.emptySlots`), bỏ các bữa của hôm nay đã qua giờ chốt
    (BREAKFAST 10:00, LUNCH 14:00, DINNER 20:00 giờ VN; cấu hình `wikicook.ai-planner.meal-cutoffs`)
    để xử lý edge case "Chủ nhật tối hết bữa"; `remaining`, `nextAvailableAt`;
  - `aiAvailableToday` (false khi hết hạn mức ngày; dialog vẫn cho tạo và báo trước sẽ ra bản tự động).
  Ràng buộc người dùng sửa trong dialog được gửi kèm request generate và chỉ dùng cho lượt đó (FR-004),
  không ghi vào hồ sơ.
- **Rationale**: Một request khi mở dialog; server là nơi duy nhất tính giờ VN và hạn mức.
- **Alternatives considered**: Frontend tự đọc hồ sơ và tự tính lượt còn lại – lặp logic, dễ sai lệch.

## R13. Draft ở client và chỉnh sửa trên lịch của 030

- **Decision**: Hook `useAiDraft(weekStart)` giữ draft trong state của trang (không lưu
  `localStorage`, không lưu server – spec Assumptions):
  `{ requestId, mode: AI|AUTO, notice, items: DraftItem[], chooseSlots: SlotKey[], constraints }`.
  - Truyền vào `WeekGrid`/`DayView` của 030 qua `draftItems` + `onDraftChange`; mọi move/swap/thay/xóa
    chỉ đổi state draft (031 FR-017).
  - Ô "Chọn món" mở `RecipePicker` của 030; món chọn tay trong draft có `source = MANUAL`, món chọn từ
    "Gợi ý cho bạn" có `source = AI` (FR-038).
  - Có draft chưa xác nhận: `beforeunload` (reload/đóng tab) + `useBlocker` của React Router (rời
    trang, đổi tuần) hỏi lại (FR-022).
- **Rationale**: Đúng ranh giới đã chốt ở contracts/integration.md của 030.
- **Alternatives considered**: Lưu draft ở server – spec loại trừ; lưu `localStorage` – trái FR-027
  của 030 (không giữ dữ liệu plan trên thiết bị).

## R14. Xác nhận draft (FR-018, FR-019)

- **Decision**: `POST /api/v1/ai-planner/weeks/{weekStart}/confirm` với
  `{ requestId, items: [{ day, meal, recipeId, servings|null, source }] }` → gọi
  `MealPlanWriteFacade.saveDraft` (một transaction; bỏ qua ô đã có món và công thức không còn
  PUBLISHED / là `is_snack`; `servings = null` thì dùng `ServingsPolicy` của 030) → trả
  `{ saved, skippedOccupied, skippedUnavailable }`. Frontend báo số món bị bỏ qua, xóa draft, tải lại
  tuần, rồi điều hướng `/shopping-list/import?weekStart=…` (route đã chốt ở 030 research R10).
- **Rationale**: Ghi qua facade của 030 giữ một nơi duy nhất ghi `meal_plan_items`; confirm không gọi
  AI và không tốn lượt.
- **Alternatives considered**: Confirm ghi trực tiếp vào repository của 030 – phá ranh giới module.

## R15. Gợi ý cho một ô (FR-033 – FR-038, US7)

- **Decision**: `GET /api/v1/ai-planner/weeks/{weekStart}/suggestions?day=&meal=&exclude=` trả tối đa
  5 món:
  - Tập hợp lệ = bước 1 (R4) cho bữa đó, trừ món đã có trong tuần (đọc qua `MealPlanWriteFacade.plannedRecipeIds`) và
    trừ `exclude` (id món trong draft đang mở).
  - Xếp hạng: ẩm thực ưa thích → độ gần vector với câu truy vấn của bữa → phạt món cùng `category`
    với các món khác trong tuần (đa dạng). Món hợp bữa là điều kiện lọc nên luôn đứng đầu (FR-034).
  - Embedding của câu truy vấn được cache bằng Spring Cache `ConcurrentMapCache` (khóa = hash câu
    truy vấn) nên lần mở picker thứ hai không gọi provider.
  - Timeout phía server 2,5 s: `EmbeddingClient` lỗi hoặc quá hạn → `503 SUGGESTIONS_UNAVAILABLE`.
    Frontend có timeout 3 s → ẩn danh sách, không hiện lỗi (FR-037).
  - Không bao giờ gọi `LlmClient` và không ghi `ai_requests` (FR-033, FR-036).
  - `reason` dựng từ dữ liệu (ví dụ "Món sáng · Việt Nam · 20 phút"), không do AI viết.
- **Rationale**: Clarification 2026-10-18 – gợi ý chỉ dùng bước truy hồi; LLM lỗi thì gợi ý vẫn chạy.
- **Alternatives considered**: Dùng vector "hồ sơ người dùng" tính sẵn – phải lưu thêm dữ liệu; gọi
  LLM để xếp hạng – trái clarification.

## R16. Mã lỗi

- **Decision**: `ProblemDetail` + `code`, chung kiểu với 030:
  - `VALIDATION_ERROR` 400 (FR-005);
  - `PAST_WEEK` 422, `NO_EMPTY_SLOT` 422;
  - `RATE_LIMITED` 429, kèm `nextAvailableAt` và header `Retry-After`;
  - `GENERATION_IN_PROGRESS` 409;
  - `NOT_ENOUGH_RECIPES` 422, kèm `eligibleCount`, `requestedSlots`, `suggestions[]` thuộc
    `INCREASE_MAX_COOK_TIME | REMOVE_PREFERRED_CUISINES | SELECT_FEWER_SLOTS` (FR-024);
  - `BACKUP_UNAVAILABLE` 503; `SUGGESTIONS_UNAVAILABLE` 503.
  Bản nháp AI và bản nháp tự động đều trả **200**, phân biệt bằng `mode`.
- **Rationale**: Bản nháp tự động là kết quả hợp lệ chứ không phải lỗi (US6).
- **Alternatives considered**: Trả 503 kèm bản nháp – frontend khó xử lý.

## R17. Kiểm thử

- **Decision**:
  - `FakeLlmClient` (bean test) điều khiển theo kịch bản: trả JSON hợp lệ, id không tồn tại, id
    ARCHIVED, id chứa dị ứng, thiếu ô, JSON hỏng, ngủ quá 60 s, ném lỗi, kiểm tra cờ hủy.
  - `FakeEmbeddingClient`: vector tất định từ hash của văn bản, để test retrieval ổn định.
  - Testcontainers `pgvector/pgvector:pg16` (dùng lại `AbstractPostgresIT` của 030).
  - SC-002a: `wikicook.ai.mode=offline`, đo thời gian generate → assert ≤ 2 s và 0 vi phạm.
  - Frontend: Vitest + RTL + msw (giống 030).
  - Đánh giá SC-004/SC-006 với LLM thật nằm ngoài CI (chạy tay, ghi kết quả vào `docs/test/`).
- **Rationale**: Constitution II; CI không được phụ thuộc mạng hay API key.
- **Alternatives considered**: WireMock giả lập Gemini – kiểm tra thêm được phần map HTTP, nên chỉ
  dùng cho một test adapter `GeminiLlmClientTest` (dev dependency, ghi trong Complexity Tracking).

## R18. Migration

- **Decision**: Tách `V302__ai.sql` của full-schema mục 7 thành `V302__ai_requests_embeddings.sql`
  (031: `ai_requests`, `recipe_embeddings` + index HNSW) và `V303__recipe_nutrition.sql` (090).
- **Rationale**: Hai spec làm ở hai thời điểm; mỗi migration thuộc đúng một spec, dễ review.
- **Alternatives considered**: Giữ một file V302 cho cả ba bảng – 031 sẽ phải tạo bảng của 090.
  **Cần cập nhật** full-schema mục 7 khi nhóm đồng ý.

---

## Điểm cần chốt với nhóm

| # | Với | Nội dung | Đề xuất |
|---|---|---|---|
| 1 | Cả nhóm | Provider LLM + model embedding (R2, R3) | Gemini Flash + `gemini-embedding-001` (768 chiều) |
| 2 | B (060) | Tên và payload các event `RecipePublishedEvent`, `RecipeRevisionApprovedEvent`, `RecipeArchivedEvent`, `RecipesImportedEvent` | Theo [contracts/integration.md](contracts/integration.md) mục 2 |
| 3 | B (060/020), C (030) | Lớp dùng chung `DietRuleSql` cho luật dị ứng / loại trừ / kiểu ăn | Đặt ở `com.wikicook.common.diet`; 030 T041 dùng lại |
| 4 | C (090) | Package `ai` (`LlmClient`, `AiQuotaService`) và tách migration V302/V303 | Như R1, R18 |
| 5 | D (040) | Route import sau confirm | `/shopping-list/import?weekStart=` (đã nêu ở 030) |
