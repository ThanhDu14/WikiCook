package com.wikicook.ai.llm;

import com.wikicook.ai.AiUnavailableException;
import java.time.Duration;

/** Always unavailable; used to test and demo the non-AI fallback. */
public class OfflineLlmClient implements LlmClient {

    @Override
    public LlmResult complete(LlmRequest request, Duration timeout, CancellationToken token) {
        throw new AiUnavailableException("AI is in offline mode");
    }
}
