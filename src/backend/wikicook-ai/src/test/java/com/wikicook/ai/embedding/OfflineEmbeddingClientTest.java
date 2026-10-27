package com.wikicook.ai.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wikicook.ai.AiUnavailableException;
import org.junit.jupiter.api.Test;

class OfflineEmbeddingClientTest {

    @Test
    void reportsConfigurationButNeverEmbeds() {
        OfflineEmbeddingClient client = new OfflineEmbeddingClient(768, "gemini-embedding-001");

        assertThat(client.dimensions()).isEqualTo(768);
        assertThat(client.model()).isEqualTo("gemini-embedding-001");
        assertThatThrownBy(() -> client.embed("Phở bò", EmbeddingTask.RETRIEVAL_QUERY))
                .isInstanceOf(AiUnavailableException.class);
    }
}
