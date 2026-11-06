# wikicook-ai

Module Maven dùng chung cho AI Weekly Meal Planner (spec 031) và phân tích dinh dưỡng (spec 090): gọi LLM, tạo embedding, truy hồi công thức bằng `pgvector`, nạp dữ liệu cào. Thiết kế theo [research.md của 031](../../../specs/031-ai-weekly-meal-planner/research.md) (R1 – R4).

Module này **không phụ thuộc** `wikicook-app`; `wikicook-app` phụ thuộc module này. Các lớp nằm trong `com.wikicook.ai`, nên ứng dụng Spring Boot ở `com.wikicook` tự quét được.

## Cấu trúc

| Package | Nội dung |
|---|---|
| `com.wikicook.ai` | `AiUnavailableException`: AI không dùng được, phía gọi chuyển sang đường không dùng AI |
| `ai.llm` | `LlmClient`, `LlmRequest`, `LlmResult`, `CancellationToken`; `GeminiLlmClient` (JSON mode + `responseSchema`), `MockLlmClient`, `OfflineLlmClient` |
| `ai.embedding` | `EmbeddingClient`, `EmbeddingTask`; `GeminiEmbeddingClient` (lô ≤ 100, chuẩn hóa độ dài 1), `MockEmbeddingClient` (vector cố định theo hash), `OfflineEmbeddingClient` |
| `ai.gemini` | `GeminiHttp`: POST tới Gemini REST, key ở header `x-goog-api-key`, timeout và hủy theo từng lời gọi (virtual thread); lỗi chỉ báo mã HTTP, không chép nội dung lỗi của provider |
| `ai.quota` | `AiQuotaService` (`reserve` / `finish` / `userAllowance`), `AiRequestRepository` (JDBC trên `ai_requests`), enum `AiFeature` / `AiTrigger` / `AiOutcome` |
| `ai.config` | `AiProperties` (`wikicook.ai.*`), `AiClientConfig` chọn client theo `wikicook.ai.mode`, `AiAsyncConfig` (bật async/scheduling/caching, executor `aiTaskExecutor` chạy virtual thread) |
| (test) `wikicook-db` | Migration Flyway dùng chung (`V001__extensions.sql` … `V303`); test `*IT` kế thừa `ai.db.AbstractPostgresIT` (một container cho cả module) |

`wikicook.ai.mode=live` cần `WIKICOOK_AI_API_KEY`; thiếu key thì ứng dụng dừng khi khởi động, thông báo không chứa giá trị key.

Hạn mức (research R11): mỗi ngày, tính từ 00:00 giờ Việt Nam, mỗi feature được `daily-cap.total × share` dòng `called_llm`. `reserve` kiểm tra hạn mức rồi ghi dòng trong cùng transaction, có `pg_advisory_xact_lock`. Mỗi người dùng được 5 lượt meal planner `counted` trong 60 phút trượt.

Embedding công thức nằm ở `wikicook-app` (`com.wikicook.aiplanner.embedding`). `EmbeddingReconcileJob` chạy lúc 03:00 giờ Việt Nam: tạo lại embedding cho công thức PUBLISHED bị thiếu, lệch `content_hash` hoặc lệch model; xóa dòng của công thức không còn PUBLISHED; dọn `ai_requests` MEAL_PLANNER cũ hơn 90 ngày. Dữ liệu cào cũng được embed qua job này.

`GeminiLiveIT` gọi Gemini thật để kiểm tra dạng request/response. Test chỉ chạy khi có `WIKICOOK_AI_API_KEY` (`set -a; source ../.env; set +a` trước `mvn verify`), mỗi lần chạy gửi 4 request.

Pipeline trong `wikicook-app` (`com.wikicook.aiplanner`): lọc hard rule bằng SQL (`EligibleRecipeQuery` + `DietRuleSql`), truy hồi pgvector (`CandidateService`, lỗi embedding thì xếp bằng SQL), prompt `meal-plan.v1` (`PromptBuilder`), kiểm tra kết quả (`DraftValidator`) và bản nháp dự phòng không dùng AI (`BackupPlanner`). `GenerationService` nối các bước: kiểm tra tuần/ô → kiểm 5 lượt/giờ → `AiQuotaService.reserve` → lọc → kiểm đủ món theo bữa (`NOT_ENOUGH_RECIPES`) → truy hồi → LLM trong ngân sách 60 s, retry đúng một lần khi còn ≥ 15 s → kiểm tra → `finish`. LLM lỗi, quá giờ, retry vẫn hỏng hoặc hết hạn mức ngày thì trả bản nháp `AUTO`. Mỗi người chỉ có một lượt đang chạy (`409 GENERATION_IN_PROGRESS`). Log chỉ ghi số lượng và thời gian, không ghi prompt hay câu trả lời của LLM.

`AiPlannerController` có `GET /api/v1/ai-planner/weeks/{weekStart}/options` và `POST /api/v1/ai-planner/weeks/{weekStart}/generations`.

Chạy thử ngày 2026-10-30 trên DB local (1.559 món PUBLISHED): reconcile với `WIKICOOK_AI_EMBEDDING_RECONCILE_LIMIT=200` embed 200 món trong 104 giây, không lỗi. Cùng ngày, `POST …/generations` cho cả tuần 2026-11-02 (21 ô, `gemini-3.5-flash`, stub bật) trả `mode = AI`, 21 món, không trùng, 0 ô "Chọn món", 0 pick bị loại, 1 lời gọi LLM, 15,7 giây.

### Bản giả lập cho dev (`com.wikicook.devstub`)

Spec 010 (đăng nhập), 011 (hồ sơ) và 030 (lưu meal plan) chưa có, nên `wikicook.dev-stubs.enabled=true` (`WIKICOOK_DEV_STUBS_ENABLED`, mặc định `false`) bật bản giả lập. Đây không phải code chính; mỗi bean có `@ConditionalOnMissingBean` nên bean thật tự thay khi có.

| Bean | Bản giả lập | Thay bằng |
|---|---|---|
| `CurrentUser` | `StubCurrentUser`: id lấy từ header `X-Dev-User-Id`, không có thì dùng `wikicook.dev-stubs.user-id` (mặc định 1); id không phải số dương → 400 `VALIDATION_ERROR` | Spring Security của 010 |
| `DietaryProfilePort` | `StubDietaryProfile`: cùng một hồ sơ cho mọi người, từ `wikicook.dev-stubs.profile.*` (`household-size`, `diet-type`, `max-cook-time-minutes`, `allergen-ids`, `excluded-ingredient-ids`, `preferred-cuisine-ids`); mặc định rỗng, `NONE` | Module hồ sơ (011) |
| `MealPlanWriteFacade` | `InMemoryMealPlanFacade`: tuần lưu trong bộ nhớ, mất khi khởi động lại | Cài đặt của 030 trên `meal_plan_items` |
| (dòng `users`) | `DevUserProvisioner`: thêm user có id cần dùng nếu thiếu (`ai_requests.user_id` có khóa ngoại), rồi đẩy sequence `users.id` qua id đó | Không cần khi có đăng ký thật |

Khi stub tắt và chưa có 010, mọi lời gọi AI planner trả 401 `UNAUTHENTICATED`.

Chưa có: hủy lượt tạo (`DELETE /generations/{requestId}`, danh sách "vừa xong" 10 phút), confirm (US2), gợi ý một ô (US7), đồng bộ embedding theo event của 060 (`RecipeEmbeddingSync`), các IT `DraftSafetyIT`, `NotEnoughRecipesIT`, `RateLimitIT`, `OfflineModeIT`, và frontend. Interface của 030 (`DietaryProfilePort`, `MealPlanWriteFacade`, `WeekCalendar`, `@MondayDate`) đã tạo đúng chữ ký trong contract nhưng cài đặt thật vẫn chờ 030.

### Thử bằng curl

```bash
cd src/backend
WIKICOOK_AI_MODE=live WIKICOOK_DEV_STUBS_ENABLED=true docker compose up -d --build app   # key lấy từ .env

# Tùy chọn cho dialog của tuần sau (không gọi provider)
curl -s localhost:8080/api/v1/ai-planner/weeks/2026-11-02/options -H 'X-Dev-User-Id: 1'

# Tạo bản nháp cả tuần: 1 lời gọi LLM (2 nếu phải retry) và 1 lời gọi embedding cho mỗi bữa
curl -s -X POST localhost:8080/api/v1/ai-planner/weeks/2026-11-02/generations \
  -H 'X-Dev-User-Id: 1' -H 'Content-Type: application/json' \
  -d '{"requestId":"'"$(uuidgen)"'",
       "days":["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"],
       "meals":["BREAKFAST","LUNCH","DINNER"],
       "constraints":{"allergenIds":[],"excludedIngredientIds":[],"dietType":"NONE",
                      "servings":4,"maxCookTimeMinutes":null,"preferredCuisineIds":[]}}'

# Tuần đã qua → 422 PAST_WEEK (không gọi provider)
curl -s -X POST localhost:8080/api/v1/ai-planner/weeks/2026-10-19/generations \
  -H 'X-Dev-User-Id: 1' -H 'Content-Type: application/json' \
  -d '{"requestId":"'"$(uuidgen)"'","days":["MONDAY"],"meals":["LUNCH"],
       "constraints":{"allergenIds":[],"excludedIngredientIds":[],"dietType":"NONE","preferredCuisineIds":[]}}'
```

Đổi tuần trong URL theo ngày chạy (`weekStart` phải là thứ Hai, nếu không → 400 `INVALID_WEEK_START`). Mỗi user được 5 lượt `mode = AI` trong 60 phút; dùng `X-Dev-User-Id` khác để thử tiếp. Không để `WIKICOOK_DEV_STUBS_ENABLED=true` ở môi trường thật.

## Cấu hình

| Thuộc tính | Biến môi trường | Mặc định |
|---|---|---|
| `wikicook.ai.mode` | `WIKICOOK_AI_MODE` | `mock` (`live` / `mock` / `offline`) |
| `wikicook.ai.api-key` | `WIKICOOK_AI_API_KEY` | trống; không bao giờ ghi ra log |
| `wikicook.ai.llm.model` | `WIKICOOK_AI_LLM_MODEL` | `gemini-3.5-flash` (`gemini-2.5-flash` đã đóng với key mới; `gemini-3.8-flash` chạy được nhưng mất ~40 s mỗi lời gọi) |
| `wikicook.ai.llm.timeout` | `WIKICOOK_AI_LLM_TIMEOUT` | `60s` |
| `wikicook.ai.embedding.model` | `WIKICOOK_AI_EMBEDDING_MODEL` | `gemini-embedding-001` |
| `wikicook.ai.embedding.dimensions` | `WIKICOOK_AI_EMBEDDING_DIMENSIONS` | `768` (khớp `VECTOR(768)`) |
| `wikicook.ai.embedding.timeout` | `WIKICOOK_AI_EMBEDDING_TIMEOUT` | `30s` |
| `wikicook.ai.embedding.reconcile-on-startup` | `WIKICOOK_AI_EMBEDDING_RECONCILE_ON_STARTUP` | `false` |
| `wikicook.ai.embedding.reconcile-limit` | `WIKICOOK_AI_EMBEDDING_RECONCILE_LIMIT` | `0` (không giới hạn số món mỗi lần chạy) |
| `wikicook.ai.embedding.batch-size` | `WIKICOOK_AI_EMBEDDING_BATCH_SIZE` | `50` công thức/lô, tối đa 100 |
| `wikicook.ai.embedding.batch-pause` | `WIKICOOK_AI_EMBEDDING_BATCH_PAUSE` | `30s` giữa hai request tới provider |
| `wikicook.ai.base-url` | `WIKICOOK_AI_BASE_URL` | `https://generativelanguage.googleapis.com/v1beta` |
| `wikicook.ai.daily-cap.total` | `WIKICOOK_AI_DAILY_CAP_TOTAL` | `100` dòng/ngày (một dòng có thể gọi provider 2 lần khi retry) |
| `wikicook.ai.daily-cap.meal-planner-share` | `WIKICOOK_AI_DAILY_CAP_MEAL_PLANNER_SHARE` | `0.7` |
| `wikicook.ai.daily-cap.nutrition-share` | `WIKICOOK_AI_DAILY_CAP_NUTRITION_SHARE` | `0.3` (tổng hai share ≤ 1) |
| `wikicook.ai-planner.meal-cutoffs.{breakfast,lunch,dinner}` | – | `10:00` / `14:00` / `20:00` giờ VN; bữa của hôm nay qua giờ này thì không tạo được |
| `wikicook.dev-stubs.enabled` | `WIKICOOK_DEV_STUBS_ENABLED` | `false`; chỉ cho dev |
| `wikicook.dev-stubs.user-id` | `WIKICOOK_DEV_STUBS_USER_ID` | `1` |
| `wikicook.dev-stubs.profile.*` | – | hồ sơ rỗng, `diet-type: NONE` |

## Build và test

```bash
cd src/backend
mvn -pl wikicook-ai verify      # unit test + *IT (Testcontainers, cần Docker) + JaCoCo ≥ 70% dòng và nhánh
```

Test `*IT` dùng image `pgvector/pgvector:pg16`, giống `docker-compose.yml`.
