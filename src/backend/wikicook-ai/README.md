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
| `ai.config` | `AiProperties` (`wikicook.ai.*`), `AiClientConfig` chọn client theo `wikicook.ai.mode` |
| (test) `wikicook-db` | Migration Flyway dùng chung (`V001__extensions.sql` … `V303`); test `*IT` kế thừa `ai.db.AbstractPostgresIT` (một container cho cả module) |

`wikicook.ai.mode=live` cần `WIKICOOK_AI_API_KEY`; thiếu key thì ứng dụng dừng khi khởi động, thông báo không chứa giá trị key.

Hạn mức (research R11): mỗi ngày, tính từ 00:00 giờ Việt Nam, mỗi feature được `daily-cap.total × share` dòng `called_llm`. `reserve` kiểm tra hạn mức rồi ghi dòng trong cùng transaction, có `pg_advisory_xact_lock`. Mỗi người dùng được 5 lượt meal planner `counted` trong 60 phút trượt.

Chưa có: truy hồi RAG (`CandidateQuery`), đồng bộ embedding (`RecipeEmbeddingSync`, job đối soát) và importer dữ liệu cào.

## Cấu hình

| Thuộc tính | Biến môi trường | Mặc định |
|---|---|---|
| `wikicook.ai.mode` | `WIKICOOK_AI_MODE` | `mock` (`live` / `mock` / `offline`) |
| `wikicook.ai.api-key` | `WIKICOOK_AI_API_KEY` | trống; không bao giờ ghi ra log |
| `wikicook.ai.llm.model` | `WIKICOOK_AI_LLM_MODEL` | `gemini-2.5-flash` |
| `wikicook.ai.llm.timeout` | `WIKICOOK_AI_LLM_TIMEOUT` | `60s` |
| `wikicook.ai.embedding.model` | `WIKICOOK_AI_EMBEDDING_MODEL` | `gemini-embedding-001` |
| `wikicook.ai.embedding.dimensions` | `WIKICOOK_AI_EMBEDDING_DIMENSIONS` | `768` (khớp `VECTOR(768)`) |
| `wikicook.ai.embedding.timeout` | `WIKICOOK_AI_EMBEDDING_TIMEOUT` | `30s` |
| `wikicook.ai.base-url` | `WIKICOOK_AI_BASE_URL` | `https://generativelanguage.googleapis.com/v1beta` |
| `wikicook.ai.daily-cap.total` | `WIKICOOK_AI_DAILY_CAP_TOTAL` | `100` dòng/ngày (một dòng có thể gọi provider 2 lần khi retry) |
| `wikicook.ai.daily-cap.meal-planner-share` | `WIKICOOK_AI_DAILY_CAP_MEAL_PLANNER_SHARE` | `0.7` |
| `wikicook.ai.daily-cap.nutrition-share` | `WIKICOOK_AI_DAILY_CAP_NUTRITION_SHARE` | `0.3` (tổng hai share ≤ 1) |

## Build và test

```bash
cd src/backend
mvn -pl wikicook-ai verify      # unit test + *IT (Testcontainers, cần Docker) + JaCoCo ≥ 70% dòng và nhánh
```

Test `*IT` dùng image `pgvector/pgvector:pg16`, giống `docker-compose.yml`.
