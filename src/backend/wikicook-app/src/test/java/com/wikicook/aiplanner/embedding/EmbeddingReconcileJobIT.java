package com.wikicook.aiplanner.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.ai.AiUnavailableException;
import com.wikicook.ai.config.AiProperties;
import com.wikicook.ai.embedding.EmbeddingClient;
import com.wikicook.ai.embedding.EmbeddingTask;
import com.wikicook.ai.embedding.MockEmbeddingClient;
import com.wikicook.ai.quota.AiRequestRepository;
import com.wikicook.aiplanner.repository.RecipeEmbeddingRepository;
import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.support.AbstractPostgresIT;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class EmbeddingReconcileJobIT extends AbstractPostgresIT {

    private static final Instant NOW = Instant.parse("2026-11-02T20:00:00Z"); // 03:00 Vietnam time

    @Autowired
    RecipeEmbeddingRepository embeddings;

    @Autowired
    AiRequestRepository aiRequests;

    @Autowired
    JdbcTemplate jdbc;

    private RecipeFixtures recipes;
    private final CountingClient client = new CountingClient("model-a");

    @BeforeEach
    void seed() {
        RecipeFixtures.clear(jdbc);
        jdbc.update("DELETE FROM ai_requests");
        recipes = new RecipeFixtures(jdbc);
    }

    private EmbeddingReconcileJob job(EmbeddingClient embeddingClient, int batchSize, int limit, boolean onStartup) {
        var settings = new AiProperties.Embedding(embeddingClient.model(), 768, Duration.ofSeconds(5), onStartup,
                batchSize, Duration.ZERO, limit);
        return new EmbeddingReconcileJob(embeddings, new EmbeddingTextBuilder(), embeddingClient, aiRequests, settings,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private int rows() {
        return jdbc.queryForObject("SELECT count(*) FROM recipe_embeddings", Integer.class);
    }

    @Test
    void embedsMissingRecipesOnceAndSkipsUnchangedOnes() {
        long pho = recipes.publishedRecipe("Phở bò tái", "Bánh phở", "Thịt bò");
        recipes.publishedRecipe("Cơm tấm sườn", "Gạo tấm", "Sườn heo");
        recipes.recipe("Bản nháp chưa xong", "DRAFT");

        EmbeddingReconcileJob.Result first = job(client, 50, 0, false).run();
        EmbeddingReconcileJob.Result second = job(client, 50, 0, false).run();

        assertThat(first.embedded()).isEqualTo(2);
        assertThat(first.failed()).isFalse();
        assertThat(second.embedded()).isZero();
        assertThat(client.calls).isEqualTo(1);
        assertThat(rows()).isEqualTo(2);
        var stored = embeddings.findByRecipeId(pho).orElseThrow();
        assertThat(stored.model()).isEqualTo("model-a");
        assertThat(stored.contentHash()).hasSize(64);
    }

    @Test
    void reembedsChangedContentAndChangedModel() {
        long pho = recipes.publishedRecipe("Phở bò tái", "Bánh phở", "Thịt bò");
        recipes.publishedRecipe("Cơm tấm sườn", "Gạo tấm");
        job(client, 50, 0, false).run();
        String oldHash = embeddings.findByRecipeId(pho).orElseThrow().contentHash();

        recipes.renameFirstIngredient(pho, "Bún");
        assertThat(job(client, 50, 0, false).run().embedded()).isEqualTo(1);
        assertThat(embeddings.findByRecipeId(pho).orElseThrow().contentHash()).isNotEqualTo(oldHash);

        CountingClient newModel = new CountingClient("model-b");
        assertThat(job(newModel, 50, 0, false).run().embedded()).isEqualTo(2);
        assertThat(embeddings.findByRecipeId(pho).orElseThrow().model()).isEqualTo("model-b");
    }

    @Test
    void removesRowsOfRecipesNoLongerPublished() {
        long kept = recipes.publishedRecipe("Phở bò tái");
        long archived = recipes.publishedRecipe("Bún chả Hà Nội");
        job(client, 50, 0, false).run();
        recipes.archive(archived);

        EmbeddingReconcileJob.Result result = job(client, 50, 0, false).run();

        assertThat(result.removed()).isEqualTo(1);
        assertThat(embeddings.findByRecipeId(kept)).isPresent();
        assertThat(embeddings.findByRecipeId(archived)).isEmpty();
    }

    @Test
    void worksInBatchesAndHonoursTheLimit() {
        for (int i = 0; i < 12; i++) {
            recipes.publishedRecipe("Món thử số " + i);
        }

        assertThat(job(client, 5, 7, false).run().embedded()).isEqualTo(7);
        assertThat(client.batchSizes).containsExactly(5, 2);

        assertThat(job(client, 5, 0, false).run().embedded()).isEqualTo(5);
        assertThat(client.batchSizes).containsExactly(5, 2, 3, 2);
        assertThat(rows()).isEqualTo(12);
    }

    @Test
    void providerErrorStopsTheRunAndTheNextRunContinues() {
        for (int i = 0; i < 6; i++) {
            recipes.publishedRecipe("Món thử số " + i);
        }
        client.failOnCall = 2;

        EmbeddingReconcileJob.Result broken = job(client, 3, 0, false).run();

        assertThat(broken.failed()).isTrue();
        assertThat(broken.embedded()).isEqualTo(3);
        assertThat(rows()).isEqualTo(3);

        client.failOnCall = 0;
        assertThat(job(client, 3, 0, false).run().embedded()).isEqualTo(3);
        assertThat(rows()).isEqualTo(6);
    }

    @Test
    void purgesMealPlannerRequestsOlderThan90Days() {
        long userId = jdbc.queryForObject("INSERT INTO users (email, display_name) VALUES (?, 'T') RETURNING id",
                Long.class, "job-" + UUID.randomUUID() + "@wikicook.test");
        for (Duration age : List.of(Duration.ofDays(91), Duration.ofDays(30))) {
            jdbc.update("""
                    INSERT INTO ai_requests (feature, trigger, outcome, called_llm, counted, user_id, week_start, created_at)
                    VALUES ('MEAL_PLANNER', 'USER', 'FAILED', true, false, ?, DATE '2026-11-02', ?)
                    """, userId, Timestamp.from(NOW.minus(age)));
        }

        assertThat(job(client, 50, 0, false).run().purgedRequests()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ai_requests", Integer.class)).isEqualTo(1);
    }

    @Test
    void startupRunOnlyWhenEnabled() {
        recipes.publishedRecipe("Phở bò tái");

        job(client, 50, 0, false).onStartup();
        assertThat(client.calls).isZero();

        job(client, 50, 0, true).onStartup();
        assertThat(client.calls).isEqualTo(1);
    }

    /** Hash-based vectors from {@link MockEmbeddingClient}, counting batches; can fail one call. */
    static final class CountingClient implements EmbeddingClient {

        private final MockEmbeddingClient vectors = new MockEmbeddingClient(768);
        private final String model;
        final List<Integer> batchSizes = new java.util.ArrayList<>();
        int calls;
        int failOnCall;

        CountingClient(String model) {
            this.model = model;
        }

        @Override
        public int dimensions() {
            return 768;
        }

        @Override
        public String model() {
            return model;
        }

        @Override
        public float[] embed(String text, EmbeddingTask task) {
            return vectors.embed(text, task);
        }

        @Override
        public List<float[]> embedBatch(List<String> texts, EmbeddingTask task) {
            calls++;
            if (calls == failOnCall) {
                throw new AiUnavailableException("Gemini returned HTTP 503");
            }
            assertThat(task).isEqualTo(EmbeddingTask.RETRIEVAL_DOCUMENT);
            batchSizes.add(texts.size());
            return vectors.embedBatch(texts, task);
        }
    }
}
