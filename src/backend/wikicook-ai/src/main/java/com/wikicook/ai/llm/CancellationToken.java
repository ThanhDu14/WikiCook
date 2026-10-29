package com.wikicook.ai.llm;

import java.util.ArrayList;
import java.util.List;

/** Lets the caller abort a running LLM call, e.g. when the user closes the generation dialog. */
public final class CancellationToken {

    private final List<Runnable> listeners = new ArrayList<>();
    private boolean cancelled;

    public static CancellationToken none() {
        return new CancellationToken();
    }

    public synchronized void cancel() {
        if (cancelled) {
            return;
        }
        cancelled = true;
        listeners.forEach(Runnable::run);
        listeners.clear();
    }

    public synchronized boolean isCancelled() {
        return cancelled;
    }

    /** Runs {@code listener} on cancel, or right away if the token is already cancelled. */
    public synchronized void onCancel(Runnable listener) {
        if (cancelled) {
            listener.run();
        } else {
            listeners.add(listener);
        }
    }
}
