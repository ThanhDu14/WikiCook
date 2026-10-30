package com.wikicook.aiplanner.service;

import com.wikicook.ai.llm.CancellationToken;
import com.wikicook.aiplanner.controller.GenerationInProgressException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Generations running right now, at most one per user (research R9), in memory: a restart forgets them. The
 * cancel endpoint and the ten-minute "just finished" list of US5/US6 (T072, T082) build on this.
 */
@Component
public class InFlightGenerations {

    private final Map<Long, Running> running = new ConcurrentHashMap<>();

    /** A running generation; {@link #close()} ends it. */
    public final class Running implements AutoCloseable {

        private final long userId;
        private final UUID requestId;
        private final CancellationToken token = new CancellationToken();

        private Running(long userId, UUID requestId) {
            this.userId = userId;
            this.requestId = requestId;
        }

        public UUID requestId() {
            return requestId;
        }

        public CancellationToken token() {
            return token;
        }

        @Override
        public void close() {
            running.remove(userId, this);
        }
    }

    /** @throws GenerationInProgressException when the user already has one running */
    public Running begin(long userId, UUID requestId) {
        Running generation = new Running(userId, requestId);
        if (running.putIfAbsent(userId, generation) != null) {
            throw new GenerationInProgressException();
        }
        return generation;
    }

    public boolean isRunning(long userId) {
        return running.containsKey(userId);
    }
}
