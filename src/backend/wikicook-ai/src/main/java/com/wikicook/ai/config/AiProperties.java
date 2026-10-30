package com.wikicook.ai.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * {@code wikicook.ai.*}. The API key comes from the environment ({@code WIKICOOK_AI_API_KEY}) and is never logged.
 */
@Validated
@ConfigurationProperties("wikicook.ai")
public record AiProperties(
        @NotNull @DefaultValue("mock") AiMode mode,
        String apiKey,
        @NotBlank @DefaultValue("https://generativelanguage.googleapis.com/v1beta") String baseUrl,
        @Valid @NotNull @DefaultValue Llm llm,
        @Valid @NotNull @DefaultValue Embedding embedding,
        @Valid @NotNull @DefaultValue DailyCap dailyCap) {

    public record Llm(
            @NotBlank @DefaultValue("gemini-2.5-flash") String model,
            @NotNull @DefaultValue("60s") Duration timeout) {

        @AssertTrue(message = "llm.timeout must be positive")
        public boolean isTimeoutPositive() {
            return timeout == null || timeout.isPositive();
        }
    }

    /**
     * @param reconcileOnStartup run the reconcile job once when the application starts
     * @param batchSize          recipes per page of the reconcile job, one provider request per page at most
     * @param batchPause         wait between provider requests, to stay under the free tier's requests per minute
     * @param reconcileLimit     most recipes embedded per run, 0 for no limit
     */
    public record Embedding(
            @NotBlank @DefaultValue("gemini-embedding-001") String model,
            @Positive @DefaultValue("768") int dimensions,
            @NotNull @DefaultValue("30s") Duration timeout,
            @DefaultValue("false") boolean reconcileOnStartup,
            @Min(1) @Max(100) @DefaultValue("50") int batchSize,
            @NotNull @DefaultValue("30s") Duration batchPause,
            @PositiveOrZero @DefaultValue("0") int reconcileLimit) {

        @AssertTrue(message = "embedding.timeout must be positive")
        public boolean isTimeoutPositive() {
            return timeout == null || timeout.isPositive();
        }

        @AssertTrue(message = "embedding.batch-pause must not be negative")
        public boolean isBatchPauseValid() {
            return batchPause == null || !batchPause.isNegative();
        }
    }

    /**
     * Rows with {@code called_llm} per day (from 00:00 Vietnam time) shared by 031 and 090 (research R11).
     * One row can make two provider calls (retry), so {@code total} leaves room below the free tier.
     */
    public record DailyCap(
            @PositiveOrZero @DefaultValue("100") int total,
            @DecimalMin("0.0") @DecimalMax("1.0") @DefaultValue("0.7") double mealPlannerShare,
            @DecimalMin("0.0") @DecimalMax("1.0") @DefaultValue("0.3") double nutritionShare) {

        @AssertTrue(message = "daily-cap shares must add up to at most 1")
        public boolean isSharesWithinTotal() {
            return mealPlannerShare + nutritionShare <= 1.0 + 1e-9;
        }

        public int mealPlanner() {
            return share(mealPlannerShare);
        }

        public int nutrition() {
            return share(nutritionShare);
        }

        private int share(double share) {
            return (int) Math.floor(total * share + 1e-9);
        }
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Keeps the key out of logs and error messages. */
    @Override
    public String toString() {
        return "AiProperties[mode=" + mode + ", apiKey=" + (hasApiKey() ? "***" : "<unset>") + ", baseUrl=" + baseUrl
                + ", llm=" + llm + ", embedding=" + embedding + ", dailyCap=" + dailyCap + "]";
    }
}
