package com.wikicook.ai.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.ai.embedding.EmbeddingTask;
import com.wikicook.ai.embedding.GeminiEmbeddingClient;
import com.wikicook.ai.llm.CancellationToken;
import com.wikicook.ai.llm.GeminiLlmClient;
import com.wikicook.ai.llm.LlmRequest;
import com.wikicook.ai.llm.LlmResult;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Calls the real Gemini API: checks that the request and reply shapes assumed by the clients still hold.
 * Runs only when {@code WIKICOOK_AI_API_KEY} is set (never on CI); four requests per run.
 */
@EnabledIfEnvironmentVariable(named = "WIKICOOK_AI_API_KEY", matches = ".+")
class GeminiLiveIT {

    private static final String BASE_URL = env("WIKICOOK_AI_BASE_URL", "https://generativelanguage.googleapis.com/v1beta");
    private static final String SCHEMA = """
            {"type":"OBJECT","properties":{
              "dish":{"type":"STRING"},
              "minutes":{"type":"INTEGER"}},
             "required":["dish","minutes"]}
            """;

    private static GeminiLlmClient llm;
    private static GeminiEmbeddingClient embedding;

    @BeforeAll
    static void connect() {
        String key = System.getenv("WIKICOOK_AI_API_KEY");
        llm = new GeminiLlmClient(BASE_URL, key, env("WIKICOOK_AI_LLM_MODEL", "gemini-2.5-flash"));
        embedding = new GeminiEmbeddingClient(BASE_URL, key, env("WIKICOOK_AI_EMBEDDING_MODEL", "gemini-embedding-001"),
                768, Duration.ofSeconds(30));
    }

    @AfterAll
    static void close() {
        if (llm != null) {
            llm.close();
            embedding.close();
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    @Test
    void llmFollowsTheResponseSchema() throws Exception {
        LlmRequest request = new LlmRequest("Bạn là đầu bếp Việt Nam. Trả lời ngắn.",
                "Gợi ý một món ăn sáng Việt Nam và số phút nấu.", SCHEMA, 0.4);

        LlmResult result = llm.complete(request, Duration.ofSeconds(60), CancellationToken.none());

        JsonNode reply = new ObjectMapper().readTree(result.text());
        assertThat(reply.path("dish").isTextual()).isTrue();
        assertThat(reply.path("dish").asText()).isNotBlank();
        assertThat(reply.path("minutes").canConvertToInt()).isTrue();
        assertThat(result.model()).startsWith("gemini");
    }

    @Test
    void embeddingsHaveConfiguredSizeAndCloseDishesAreCloser() {
        float[] query = embedding.embed("món phở", EmbeddingTask.RETRIEVAL_QUERY);
        List<float[]> docs = embedding.embedBatch(List.of("Phở bò", "Phở gà", "Chè đậu xanh"),
                EmbeddingTask.RETRIEVAL_DOCUMENT);

        assertThat(query).hasSize(768);
        assertThat(norm(query)).isCloseTo(1.0, within(1e-3));
        assertThat(docs).hasSize(3).allSatisfy(v -> {
            assertThat(v).hasSize(768);
            assertThat(norm(v)).isCloseTo(1.0, within(1e-3));
        });
        assertThat(cosine(docs.get(0), docs.get(1))).isGreaterThan(cosine(docs.get(0), docs.get(2)));
        assertThat(cosine(query, docs.get(0))).isGreaterThan(cosine(query, docs.get(2)));
    }

    private static double norm(float[] v) {
        return Math.sqrt(cosine(v, v));
    }

    /** Dot product; equals cosine similarity for unit vectors. */
    private static double cosine(float[] a, float[] b) {
        double dot = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }
}
