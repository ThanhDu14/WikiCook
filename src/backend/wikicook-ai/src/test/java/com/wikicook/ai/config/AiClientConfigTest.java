package com.wikicook.ai.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.ai.embedding.EmbeddingClient;
import com.wikicook.ai.embedding.GeminiEmbeddingClient;
import com.wikicook.ai.embedding.MockEmbeddingClient;
import com.wikicook.ai.embedding.OfflineEmbeddingClient;
import com.wikicook.ai.llm.GeminiLlmClient;
import com.wikicook.ai.llm.LlmClient;
import com.wikicook.ai.llm.MockLlmClient;
import com.wikicook.ai.llm.OfflineLlmClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
            assertThat(properties.baseUrl()).isEqualTo("https://generativelanguage.googleapis.com/v1beta");
            assertThat(properties.dailyCap().total()).isEqualTo(100);
            assertThat(properties.dailyCap().mealPlannerShare()).isEqualTo(0.7);
            assertThat(properties.dailyCap().nutritionShare()).isEqualTo(0.3);
            assertThat(properties.embedding().reconcileOnStartup()).isFalse();
            assertThat(properties.embedding().batchSize()).isEqualTo(50);
            assertThat(properties.embedding().batchPause()).isEqualTo(Duration.ofSeconds(30));
            assertThat(properties.embedding().reconcileLimit()).isZero();
        });
    }

    @Test
    void dailyCapSplitsTheTotalBetweenFeatures() {
        runner.withPropertyValues("wikicook.ai.daily-cap.total=101").run(context -> {
            AiProperties.DailyCap cap = context.getBean(AiProperties.class).dailyCap();
            assertThat(cap.mealPlanner()).isEqualTo(70);
            assertThat(cap.nutrition()).isEqualTo(30);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "wikicook.ai.daily-cap.total=-1",
            "wikicook.ai.daily-cap.meal-planner-share=1.2",
            "wikicook.ai.daily-cap.nutrition-share=-0.1",
            "wikicook.ai.daily-cap.meal-planner-share=0.8", // 0.8 + 0.3 > 1
            "wikicook.ai.llm.timeout=0s",
            "wikicook.ai.embedding.timeout=-1s",
            "wikicook.ai.base-url=",
            "wikicook.ai.embedding.batch-size=0",
            "wikicook.ai.embedding.batch-size=101",
            "wikicook.ai.embedding.batch-pause=-1s",
            "wikicook.ai.embedding.reconcile-limit=-1",
    })
    void rejectsInvalidSettings(String setting) {
        runner.withPropertyValues(setting).run(context -> assertThat(context).hasFailed());
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
    void liveModeWithKeyUsesGeminiClients() {
        runner.withPropertyValues("wikicook.ai.mode=live", "wikicook.ai.api-key=secret-123",
                "wikicook.ai.llm.model=gemini-test", "wikicook.ai.embedding.dimensions=256").run(context -> {
            assertThat(context).hasNotFailed();
            LlmClient llm = context.getBean(LlmClient.class);
            EmbeddingClient embedding = context.getBean(EmbeddingClient.class);
            assertThat(llm).isInstanceOf(GeminiLlmClient.class);
            assertThat(embedding).isInstanceOf(GeminiEmbeddingClient.class);
            assertThat(embedding.dimensions()).isEqualTo(256);
            assertThat(embedding.model()).isEqualTo("gemini-embedding-001");
            assertThat(llm.toString()).contains("gemini-test").doesNotContain("secret-123");
            assertThat(embedding.toString()).doesNotContain("secret-123");
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
