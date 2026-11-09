package com.wikicook.aiplanner.service;

import com.wikicook.ai.llm.CancellationToken;
import com.wikicook.ai.quota.AiOutcome;
import com.wikicook.aiplanner.controller.GenerationInProgressException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * Generations of the meal planner held in memory (research R9); a restart forgets them.
 * <ul>
 *   <li>Running: at most one per user, with the token that cancels its LLM call.</li>
 *   <li>Just finished: for {@value #KEEP_MINUTES} minutes after a generation that wrote an {@code ai_requests} row,
 *       its row, outcome and whether its draft was confirmed. The cancel endpoint (US6, T082) gives back the
 *       generation of a draft the client never saw; confirming it first makes that impossible.</li>
 * </ul>
 */
@Component
public class InFlightGenerations {

    private static final int KEEP_MINUTES = 10;
    static final Duration KEEP_FINISHED = Duration.ofMinutes(KEEP_MINUTES);

    /**
     * A generation that ended after writing its row.
     *
     * @param confirmed whether {@code POST …/confirm} came with its request id
     */
    public record Finished(long userId, long requestRowId, AiOutcome outcome, Instant finishedAt, boolean confirmed) {

        Finished confirm() {
            return new Finished(userId, requestRowId, outcome, finishedAt, true);
        }
    }

    private record Result(long requestRowId, AiOutcome outcome) {
    }

    private final Clock clock;
    private final Map<Long, Running> running = new ConcurrentHashMap<>();
    private final Map<UUID, Finished> finished = new ConcurrentHashMap<>();

    public InFlightGenerations(Clock clock) {
        this.clock = clock;
    }

    /** A running generation; {@link #close()} ends it. */
    public final class Running implements AutoCloseable {

        private final long userId;
        private final UUID requestId;
        private final CancellationToken token = new CancellationToken();
        private final AtomicReference<Result> result = new AtomicReference<>();

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

        /** Records how the generation ended; kept in the "just finished" list once it is {@link #close() closed}. */
        public void finished(long requestRowId, AiOutcome outcome) {
            result.set(new Result(requestRowId, outcome));
        }

        @Override
        public void close() {
            if (!running.remove(userId, this)) {
                return;
            }
            Result end = result.get();
            if (end != null) {
                finished.put(requestId, new Finished(userId, end.requestRowId(), end.outcome(), clock.instant(), false));
            }
        }
    }

    /** @throws GenerationInProgressException when the user already has one running */
    public Running begin(long userId, UUID requestId) {
        purge();
        Running generation = new Running(userId, requestId);
        if (running.putIfAbsent(userId, generation) != null) {
            throw new GenerationInProgressException();
        }
        return generation;
    }

    public boolean isRunning(long userId) {
        return running.containsKey(userId);
    }

    /** The user's generation that finished in the last ten minutes, if any. */
    public Optional<Finished> finished(long userId, UUID requestId) {
        purge();
        return Optional.ofNullable(finished.get(requestId)).filter(f -> f.userId() == userId);
    }

    /** Called when the user saves the draft of {@code requestId}: its generation stays counted (research R9). */
    public void markConfirmed(long userId, UUID requestId) {
        finished.computeIfPresent(requestId, (id, f) -> f.userId() == userId ? f.confirm() : f);
    }

    /**
     * Takes the user's AI draft generation that finished in the last ten minutes and was not confirmed, so the cancel
     * endpoint can give it back once; empty for anything else.
     *
     * @return its {@code ai_requests} row
     */
    public Optional<Long> takeRefundable(long userId, UUID requestId) {
        purge();
        Finished taken = finished.get(requestId);
        boolean refundable = taken != null && taken.userId() == userId && taken.outcome() == AiOutcome.AI_DRAFT
                && !taken.confirmed();
        return refundable && finished.remove(requestId, taken) ? Optional.of(taken.requestRowId()) : Optional.empty();
    }

    int finishedCount() {
        purge();
        return finished.size();
    }

    private void purge() {
        Instant oldest = clock.instant().minus(KEEP_FINISHED);
        finished.values().removeIf(f -> f.finishedAt().isBefore(oldest));
    }
}
