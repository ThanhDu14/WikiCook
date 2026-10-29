package com.wikicook.ai.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.ai.embedding.EmbeddingClient;
import com.wikicook.ai.embedding.MockEmbeddingClient;
import com.wikicook.ai.embedding.OfflineEmbeddingClient;
import com.wikicook.ai.llm.LlmClient;
import com.wikicook.ai.llm.MockLlmClient;
import com.wikicook.ai.llm.OfflineLlmClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AiClientConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(AiClientConfig.class);

    @Test
    void defaultsToMockWithResearchSettings() {
        runner.run(context -> {
            assertThat(context.getBean(LlmClient.class)).isInstanceOf(MockLlmClient.class);
            assertThat(context.getBean(EmbeddingClient.class)).isInstanceOf(MockEmbeddingClient.class);
            AiProperties properties = context.getBean(AiProperties.class);
            assertThat(properties.mode()).isEqualTo(AiMode.MOCK);
            assertThat(properties.llm().timeout()).isEqualTo(Duration.ofSeconds(60));
            assertThat(properties.embedding().model()).isEqualTo("gemini-embedding-001");
            assertThat(properties.embedding().dimensions()).isEqualTo(768);
        });
    }

    @Test
    void offlineModeUsesOfflineClients() {
        runner.withPropertyValues("wikicook.ai.mode=offline").run(context -> {
            assertThat(context.getBean(LlmClient.class)).isInstanceOf(OfflineLlmClient.class);
            assertThat(context.getBean(EmbeddingClient.class)).isInstanceOf(OfflineEmbeddingClient.class);
        });
    }

    @Test
    void liveModeWithoutKeyFailsWithoutLeakingAnything() {
        runner.withPropertyValues("wikicook.ai.mode=live").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("WIKICOOK_AI_API_KEY");
        });
    }

    @Test
    void liveModeWithKeyFailsUntilGeminiClientsExist() {
        runner.withPropertyValues("wikicook.ai.mode=live", "wikicook.ai.api-key=secret-123").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause()
                    .hasMessageContaining("not implemented")
                    .hasMessageNotContaining("secret-123");
        });
    }

    @Test
    void rejectsInvalidDimensions() {
        runner.withPropertyValues("wikicook.ai.embedding.dimensions=0").run(context -> assertThat(context).hasFailed());
    }

    @Test
    void toStringHidesApiKey() {
        runner.withPropertyValues("wikicook.ai.api-key=secret-123").run(context ->
                assertThat(context.getBean(AiProperties.class).toString())
                        .doesNotContain("secret-123")
                        .contains("apiKey=***"));
        runner.run(context -> assertThat(context.getBean(AiProperties.class).toString()).contains("apiKey=<unset>"));
    }
}
