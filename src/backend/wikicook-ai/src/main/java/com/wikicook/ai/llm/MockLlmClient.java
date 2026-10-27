package com.wikicook.ai.llm;

import com.wikicook.ai.AiUnavailableException;
import java.time.Duration;
import java.util.function.Function;

/** Answers without calling a provider; used in CI and for demos without an API key. */
public class MockLlmClient implements LlmClient {

    public static final String MODEL = "mock";

    private final Function<LlmRequest, String> responder;

    public MockLlmClient() {
        this(request -> "{}");
    }

    public MockLlmClient(Function<LlmRequest, String> responder) {
        this.responder = responder;
    }

    @Override
    public LlmResult complete(LlmRequest request, Duration timeout, CancellationToken token) {
        if (token.isCancelled()) {
            throw new AiUnavailableException("LLM call cancelled");
        }
        return new LlmResult(responder.apply(request), MODEL);
    }
}
