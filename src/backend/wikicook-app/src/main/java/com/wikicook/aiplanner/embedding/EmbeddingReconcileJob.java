package com.wikicook.aiplanner.embedding;

import com.wikicook.ai.AiUnavailableException;
import com.wikicook.ai.config.AiAsyncConfig;
import com.wikicook.ai.config.AiProperties;
import com.wikicook.ai.embedding.EmbeddingClient;
import com.wikicook.ai.embedding.EmbeddingTask;
import com.wikicook.ai.quota.AiFeature;
import com.wikicook.ai.quota.AiRequestRepository;
import com.wikicook.aiplanner.repository.RecipeEmbeddingRepository;
import com.wikicook.aiplanner.repository.RecipeEmbeddingRepository.PublishedRecipe;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nightly reconcile of {@code recipe_embeddings} (research R10): embeds published recipes that have no row, a
 * different content hash or a different model, removes rows of recipes no longer published, and drops meal
 * planner {@code ai_requests} older than 90 days. Scraped recipes get their first embedding here too.
 */
@Component
public class EmbeddingReconcileJob {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingReconcileJob.class);
    static final Duration KEEP_REQUESTS = Duration.ofDays(90);

    private final RecipeEmbeddingRepository embeddings;
    private final EmbeddingTextBuilder textBuilder;
    private final EmbeddingClient client;
    private final AiRequestRepository aiRequests;
    private final AiProperties.Embedding settings;
    private final Clock clock;
    private final AtomicBoolean running = new AtomicBoolean();

    @Autowired
    public EmbeddingReconcileJob(RecipeEmbeddingRepository embeddings, EmbeddingTextBuilder textBuilder,
                                 EmbeddingClient client, AiRequestRepository aiRequests, AiProperties properties,
                                 Clock clock) {
        this(embeddings, textBuilder, client, aiRequests, properties.embedding(), clock);
    }

    EmbeddingReconcileJob(RecipeEmbeddingRepository embeddings, EmbeddingTextBuilder textBuilder,
                          EmbeddingClient client, AiRequestRepository aiRequests, AiProperties.Embedding settings,
                          Clock clock) {
        this.embeddings = embeddings;
        this.textBuilder = textBuilder;
        this.client = client;
        this.aiRequests = aiRequests;
        this.settings = settings;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Ho_Chi_Minh")
    public void nightly() {
        run();
    }

    /** Off by default: a first run over scraped data spends provider quota for a while. */
    @Async(AiAsyncConfig.AI_TASK_EXECUTOR)
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (settings.reconcileOnStartup()) {
            run();
        }
    }

    /**
     * @param embedded       recipes embedded in this run
     * @param removed        rows deleted because the recipe is no longer published
     * @param purgedRequests {@code ai_requests} rows past retention
     * @param failed         the provider failed; the next run continues where this one stopped
     */
    public record Result(int embedded, int removed, int purgedRequests, boolean failed) {
    }

    public Result run() {
        if (!running.compareAndSet(false, true)) {
            log.info("Embedding reconcile already running, skipped");
            return new Result(0, 0, 0, false);
        }
        try {
            return reconcile();
        } finally {
            running.set(false);
        }
    }

    private Result reconcile() {
        long start = System.nanoTime();
        int purged = aiRequests.deleteOlderThan(AiFeature.MEAL_PLANNER, clock.instant().minus(KEEP_REQUESTS));
        int removed = embeddings.deleteNotPublished();
        int limit = settings.reconcileLimit();
        int embedded = 0;
        boolean failed = false;
        boolean calledProvider = false;
        long afterId = 0;
        while (limit == 0 || embedded < limit) {
            List<PublishedRecipe> page = embeddings.findPublishedAfter(afterId, settings.batchSize());
            if (page.isEmpty()) {
                break;
            }
            afterId = page.getLast().source().recipeId();
            List<Stale> stale = stale(page, limit == 0 ? Integer.MAX_VALUE : limit - embedded);
            if (stale.isEmpty()) {
                continue;
            }
            if (calledProvider && !pause()) {
                break;
            }
            calledProvider = true;
            List<float[]> vectors;
            try {
                vectors = client.embedBatch(stale.stream().map(s -> s.text().text()).toList(),
                        EmbeddingTask.RETRIEVAL_DOCUMENT);
            } catch (AiUnavailableException e) {
                // Our own message: status or timeout only, never the key or the recipe text.
                log.warn("Embedding reconcile stopped after {} recipes: {}", embedded, e.getMessage());
                failed = true;
                break;
            }
            for (int i = 0; i < stale.size(); i++) {
                embeddings.upsert(stale.get(i).recipeId(), vectors.get(i), client.model(),
                        stale.get(i).text().contentHash());
            }
            embedded += stale.size();
        }
        log.info("Embedding reconcile: {} embedded, {} removed, {} old requests purged in {} ms{}", embedded, removed,
                purged, Duration.ofNanos(System.nanoTime() - start).toMillis(), failed ? " (stopped on error)" : "");
        return new Result(embedded, removed, purged, failed);
    }

    private record Stale(long recipeId, EmbeddingText text) {
    }

    private List<Stale> stale(List<PublishedRecipe> page, int max) {
        List<Stale> stale = new ArrayList<>();
        for (PublishedRecipe recipe : page) {
            if (stale.size() == max) {
                break;
            }
            EmbeddingText text = textBuilder.build(recipe.source());
            if (!text.contentHash().equals(recipe.contentHash()) || !client.model().equals(recipe.model())) {
                stale.add(new Stale(recipe.source().recipeId(), text));
            }
        }
        return stale;
    }

    /** @return {@code false} when interrupted (shutdown) */
    private boolean pause() {
        try {
            Thread.sleep(settings.batchPause());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
