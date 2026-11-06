package com.wikicook.ai.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wikicook.ai.AiUnavailableException;
import com.wikicook.ai.embedding.EmbeddingTask;
import com.wikicook.ai.llm.CancellationToken;
import com.wikicook.ai.llm.LlmRequest;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class FakeClientsTest {

    private static final LlmRequest REQUEST = new LlmRequest("sys", "user", null, 0.4);
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    @Test
    void llmPlaysTheScriptInOrderAndRecordsPrompts() {
        FakeLlmClient llm = new FakeLlmClient().thenReply("{\"a\":1}").thenFail().thenReply("{}");

        assertThat(llm.complete(REQUEST, TIMEOUT, CancellationToken.none()).text()).isEqualTo("{\"a\":1}");
        assertThatThrownBy(() -> llm.complete(REQUEST, TIMEOUT, CancellationToken.none()))
                .isInstanceOf(AiUnavailableException.class);
        assertThat(llm.complete(REQUEST, TIMEOUT, CancellationToken.none()).text()).isEqualTo("{}");
        assertThat(llm.requests()).hasSize(3).allSatisfy(r -> assertThat(r.userPrompt()).isEqualTo("user"));
        assertThatThrownBy(() -> llm.complete(REQUEST, TIMEOUT, CancellationToken.none()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("script");
    }

    @Test
    void llmSleepEndsOnCancel() {
        FakeLlmClient llm = new FakeLlmClient().thenSleep(Duration.ofSeconds(30), "{}");
        CancellationToken token = CancellationToken.none();
        CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS).execute(token::cancel);

        long start = System.nanoTime();
        assertThatThrownBy(() -> llm.complete(REQUEST, TIMEOUT, token)).isInstanceOf(AiUnavailableException.class);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(5));
        assertThat(llm.wasCancelled()).isTrue();
    }

    @Test
    void llmSleepLongerThanTimeoutTimesOut() {
        FakeLlmClient llm = new FakeLlmClient().thenSleep(Duration.ofSeconds(30), "{}");

        assertThatThrownBy(() -> llm.complete(REQUEST, Duration.ofMillis(50), CancellationToken.none()))
                .isInstanceOf(AiUnavailableException.class).hasMessageContaining("time");
    }

    @Test
    void embeddingUsesFixedVectorsAndCanFail() {
        FakeEmbeddingClient embedding = new FakeEmbeddingClient(3).vector("phở", 1, 0, 0);

        assertThat(embedding.embed("phở", EmbeddingTask.RETRIEVAL_QUERY)).containsExactly(1, 0, 0);
        assertThat(embedding.embed("khác", EmbeddingTask.RETRIEVAL_QUERY)).hasSize(3);
        assertThat(embedding.embedBatch(List.of("phở"), EmbeddingTask.RETRIEVAL_DOCUMENT).getFirst())
                .containsExactly(1, 0, 0);
        assertThat(embedding.texts()).containsExactly("phở", "khác", "phở");

        embedding.failing(true);
        assertThatThrownBy(() -> embedding.embed("phở", EmbeddingTask.RETRIEVAL_QUERY))
                .isInstanceOf(AiUnavailableException.class);
    }
}
