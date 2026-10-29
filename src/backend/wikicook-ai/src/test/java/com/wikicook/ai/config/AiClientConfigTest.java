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
