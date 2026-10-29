# wikicook-ai

Module Maven dùng chung cho AI Weekly Meal Planner (spec 031) và phân tích dinh dưỡng (spec 090): gọi LLM, tạo embedding, truy hồi công thức bằng `pgvector`, nạp dữ liệu cào. Thiết kế theo [research.md của 031](../../../specs/031-ai-weekly-meal-planner/research.md) (R1 – R4).

Module này **không phụ thuộc** `wikicook-app`; `wikicook-app` phụ thuộc module này. Các lớp nằm trong `com.wikicook.ai`, nên ứng dụng Spring Boot ở `com.wikicook` tự quét được.

## Cấu trúc

| Package | Nội dung |
|---|---|
| `com.wikicook.ai` | `AiUnavailableException`: AI không dùng được, phía gọi chuyển sang đường không dùng AI |
| `ai.llm` | `LlmClient`, `LlmRequest`, `LlmResult`, `CancellationToken`; `MockLlmClient`, `OfflineLlmClient` |
| `ai.embedding` | `EmbeddingClient`, `EmbeddingTask`; `MockEmbeddingClient` (vector cố định theo hash), `OfflineEmbeddingClient` |
| `ai.config` | `AiProperties` (`wikicook.ai.*`), `AiClientConfig` chọn client theo `wikicook.ai.mode` |
| (test) `wikicook-db` | Migration Flyway dùng chung (`V001__extensions.sql` … `V303`); test `*IT` chạy toàn bộ trên Testcontainers |

Chưa có: `GeminiLlmClient`, `GeminiEmbeddingClient` (031 T018, T019), truy hồi RAG và importer dữ liệu cào. Hiện `wikicook.ai.mode=live` làm ứng dụng dừng khi khởi động, kèm thông báo rõ ràng.

## Cấu hình

| Thuộc tính | Biến môi trường | Mặc định |
|---|---|---|
| `wikicook.ai.mode` | `WIKICOOK_AI_MODE` | `mock` (`live` / `mock` / `offline`) |
| `wikicook.ai.api-key` | `WIKICOOK_AI_API_KEY` | trống; không bao giờ ghi ra log |
| `wikicook.ai.llm.model` | `WIKICOOK_AI_LLM_MODEL` | `gemini-2.5-flash` |
| `wikicook.ai.llm.timeout` | `WIKICOOK_AI_LLM_TIMEOUT` | `60s` |
| `wikicook.ai.embedding.model` | `WIKICOOK_AI_EMBEDDING_MODEL` | `gemini-embedding-001` |
| `wikicook.ai.embedding.dimensions` | `WIKICOOK_AI_EMBEDDING_DIMENSIONS` | `768` (khớp `VECTOR(768)`) |

## Build và test

```bash
cd src/backend
mvn -pl wikicook-ai verify      # unit test + *IT (Testcontainers, cần Docker) + JaCoCo ≥ 70% dòng và nhánh
```

Test `*IT` dùng image `pgvector/pgvector:pg16`, giống `docker-compose.yml`.
