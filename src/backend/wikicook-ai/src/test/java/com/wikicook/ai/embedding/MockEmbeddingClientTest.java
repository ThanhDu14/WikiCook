package com.wikicook.ai.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.junit.jupiter.api.Test;

class MockEmbeddingClientTest {

    private final MockEmbeddingClient client = new MockEmbeddingClient(768);

    @Test
    void sameTextGivesSameVector() {
        assertThat(client.embed("Cháo gà", EmbeddingTask.RETRIEVAL_DOCUMENT))
                .containsExactly(client.embed("Cháo gà", EmbeddingTask.RETRIEVAL_DOCUMENT));
    }

    @Test
    void differentTextOrTaskGivesDifferentVector() {
        float[] doc = client.embed("Cháo gà", EmbeddingTask.RETRIEVAL_DOCUMENT);
        assertThat(client.embed("Cháo vịt", EmbeddingTask.RETRIEVAL_DOCUMENT)).isNotEqualTo(doc);
        assertThat(client.embed("Cháo gà", EmbeddingTask.RETRIEVAL_QUERY)).isNotEqualTo(doc);
    }

    @Test
    void vectorsHaveConfiguredSizeAndUnitLength() {
        float[] vector = client.embed("Canh chua cá lóc", EmbeddingTask.RETRIEVAL_DOCUMENT);
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        assertThat(vector).hasSize(768);
        assertThat(Math.sqrt(norm)).isCloseTo(1.0, within(1e-4));
        assertThat(client.dimensions()).isEqualTo(768);
        assertThat(client.model()).isEqualTo(MockEmbeddingClient.MODEL);
    }

    @Test
    void batchKeepsInputOrder() {
        List<float[]> batch = client.embedBatch(List.of("a", "b"), EmbeddingTask.RETRIEVAL_DOCUMENT);
        assertThat(batch.get(0)).containsExactly(client.embed("a", EmbeddingTask.RETRIEVAL_DOCUMENT));
        assertThat(batch.get(1)).containsExactly(client.embed("b", EmbeddingTask.RETRIEVAL_DOCUMENT));
    }

    @Test
    void rejectsNonPositiveDimensions() {
        assertThatThrownBy(() -> new MockEmbeddingClient(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
