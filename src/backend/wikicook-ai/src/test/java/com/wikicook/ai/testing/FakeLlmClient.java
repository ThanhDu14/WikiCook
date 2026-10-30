package com.wikicook.ai.testing;

import com.wikicook.ai.AiUnavailableException;
import com.wikicook.ai.llm.CancellationToken;
import com.wikicook.ai.llm.LlmClient;
import com.wikicook.ai.llm.LlmRequest;
import com.wikicook.ai.llm.LlmResult;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Scripted LLM for tests: each call takes the next step (reply, failure, or a slow reply that honours timeout and
 * cancel). Records every request so tests can check the prompt.
 */
public class FakeLlmClient implements LlmClient {

    public static final String MODEL = "fake";

    private record Step(String reply, boolean fail, Duration sleep) {
    }

    private final Deque<Step> script = new ArrayDeque<>();
    private final List<LlmRequest> requests = new ArrayList<>();
    private volatile boolean cancelled;

    public synchronized FakeLlmClient thenReply(String json) {
        script.add(new Step(json, false, Duration.ZERO));
        return this;
    }

    public synchronized FakeLlmClient thenFail() {
        script.add(new Step(null, true, Duration.ZERO));
        return this;
    }

    /** Waits {@code sleep}, then replies; ends early with {@link AiUnavailableException} on timeout or cancel. */
    public synchronized FakeLlmClient thenSleep(Duration sleep, String json) {
        script.add(new Step(json, false, sleep));
        return this;
    }

    @Override
    public LlmResult complete(LlmRequest request, Duration timeout, CancellationToken token) {
        Step step;
        synchronized (this) {
            requests.add(request);
            step = script.poll();
        }
        if (step == null) {
            throw new IllegalStateException("FakeLlmClient: no scripted step left");
        }
        if (step.sleep().isPositive()) {
            CountDownLatch cancel = new CountDownLatch(1);
            token.onCancel(() -> {
                cancelled = true;
                cancel.countDown();
            });
            Duration wait = step.sleep().compareTo(timeout) < 0 ? step.sleep() : timeout;
            try {
                if (cancel.await(wait.toMillis(), TimeUnit.MILLISECONDS)) {
                    throw new AiUnavailableException("AI call cancelled");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AiUnavailableException("AI call interrupted");
            }
            if (step.sleep().compareTo(timeout) > 0) {
                throw new AiUnavailableException("Gemini did not answer in time");
            }
        }
        if (step.fail()) {
            throw new AiUnavailableException("Gemini returned HTTP 503");
        }
        return new LlmResult(step.reply(), MODEL);
    }

    public synchronized List<LlmRequest> requests() {
        return List.copyOf(requests);
    }

    public boolean wasCancelled() {
        return cancelled;
    }
}
