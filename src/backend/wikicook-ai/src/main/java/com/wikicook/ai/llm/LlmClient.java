package com.wikicook.ai.llm;

import com.wikicook.ai.AiUnavailableException;
import java.time.Duration;

/** Text generation, shared by the AI meal planner (031) and nutrition analysis (090). */
public interface LlmClient {

    /**
     * @throws AiUnavailableException when the provider cannot answer within {@code timeout},
     *                                the call is cancelled, or the client runs in offline mode
     */
    LlmResult complete(LlmRequest request, Duration timeout, CancellationToken token);
}
