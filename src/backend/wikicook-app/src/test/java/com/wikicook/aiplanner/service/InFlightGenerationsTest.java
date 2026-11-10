package com.wikicook.aiplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wikicook.ai.quota.AiOutcome;
import com.wikicook.ai.testing.MutableClock;
import com.wikicook.aiplanner.controller.GenerationInProgressException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** T069: one running generation per user, and the ten-minute "just finished" list (research R9). */
class InFlightGenerationsTest {

    private static final Instant NOW = Instant.parse("2026-11-09T02:00:00Z");

    private final MutableClock clock = new MutableClock(NOW);
    private final InFlightGenerations inFlight = new InFlightGenerations(clock);

    /** Runs one generation of {@code userId} to the end with the given row and outcome. */
    private UUID finish(long userId, long row, AiOutcome outcome) {
        UUID id = UUID.randomUUID();
        try (InFlightGenerations.Running running = inFlight.begin(userId, id)) {
            running.finished(row, outcome);
        }
        return id;
    }

    @Test
    void secondGenerationOfTheSameUserIsRejected() {
        try (InFlightGenerations.Running running = inFlight.begin(1, UUID.randomUUID())) {
            assertThat(running.token().isCancelled()).isFalse();
            assertThatThrownBy(() -> inFlight.begin(1, UUID.randomUUID()))
                    .isInstanceOf(GenerationInProgressException.class);
        }
    }

    @Test
    void otherUsersAreNotBlocked() {
        try (InFlightGenerations.Running first = inFlight.begin(1, UUID.randomUUID());
             InFlightGenerations.Running second = inFlight.begin(2, UUID.randomUUID())) {
            assertThat(first.requestId()).isNotEqualTo(second.requestId());
        }
    }

    @Test
    void finishedGenerationNoLongerBlocks() {
        finish(1, 10, AiOutcome.AI_DRAFT);

        try (InFlightGenerations.Running again = inFlight.begin(1, UUID.randomUUID())) {
            assertThat(inFlight.isRunning(1)).isTrue();
        }
        assertThat(inFlight.isRunning(1)).isFalse();
    }

    @Test
    void closingTwiceKeepsANewerGeneration() {
        InFlightGenerations.Running old = inFlight.begin(1, UUID.randomUUID());
        old.close();
        try (InFlightGenerations.Running current = inFlight.begin(1, UUID.randomUUID())) {
            old.close();
            assertThat(inFlight.isRunning(1)).isTrue();
        }
    }

    @Test
    void finishedGenerationsAreKeptTenMinutesWhateverTheOutcome() {
        UUID ai = finish(1, 10, AiOutcome.AI_DRAFT);
        UUID failed = finish(1, 11, AiOutcome.FAILED);
        UUID cancelled = finish(1, 12, AiOutcome.CANCELLED);

        assertThat(inFlight.finished(1, ai)).hasValueSatisfying(f -> {
            assertThat(f.requestRowId()).isEqualTo(10);
            assertThat(f.outcome()).isEqualTo(AiOutcome.AI_DRAFT);
            assertThat(f.finishedAt()).isEqualTo(NOW);
            assertThat(f.confirmed()).isFalse();
        });
        assertThat(inFlight.finished(1, failed)).hasValueSatisfying(f -> assertThat(f.requestRowId()).isEqualTo(11));
        assertThat(inFlight.finished(1, cancelled)).isPresent();

        clock.set(NOW.plus(InFlightGenerations.KEEP_FINISHED));
        assertThat(inFlight.finished(1, ai)).isPresent();
        clock.set(NOW.plus(InFlightGenerations.KEEP_FINISHED).plusSeconds(1));
        assertThat(inFlight.finished(1, ai)).isEmpty();
        assertThat(inFlight.finished(1, failed)).isEmpty();
        assertThat(inFlight.finishedCount()).isZero(); // dropped, not only hidden
        assertThat(InFlightGenerations.KEEP_FINISHED).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void aGenerationThatEndedBeforeItsRowIsNotKept() {
        UUID id = UUID.randomUUID();
        inFlight.begin(1, id).close(); // e.g. rate limited: no ai_requests row

        assertThat(inFlight.finished(1, id)).isEmpty();
    }

    @Test
    void stillRunningIsNotFinished() {
        UUID id = UUID.randomUUID();
        try (InFlightGenerations.Running running = inFlight.begin(1, id)) {
            running.finished(10, AiOutcome.AI_DRAFT);
            assertThat(inFlight.finished(1, id)).isEmpty();
        }
        assertThat(inFlight.finished(1, id)).isPresent();
    }

    @Test
    void anotherUserSeesNothing() {
        UUID id = finish(1, 10, AiOutcome.AI_DRAFT);

        assertThat(inFlight.finished(2, id)).isEmpty();
        inFlight.markConfirmed(2, id);
        assertThat(inFlight.takeRefundable(2, id)).isEmpty();
        assertThat(inFlight.finished(1, id)).hasValueSatisfying(f -> assertThat(f.confirmed()).isFalse());
    }

    @Test
    void markConfirmedFlagsTheGeneration() {
        UUID id = finish(1, 10, AiOutcome.AI_DRAFT);

        inFlight.markConfirmed(1, id);

        assertThat(inFlight.finished(1, id)).hasValueSatisfying(f -> assertThat(f.confirmed()).isTrue());
        assertThat(inFlight.takeRefundable(1, id)).isEmpty();
    }

    @Test
    void markConfirmedOfAnUnknownRequestDoesNothing() {
        inFlight.markConfirmed(1, UUID.randomUUID());

        assertThat(inFlight.finishedCount()).isZero();
    }

    @Test
    void onlyAnUnconfirmedAiDraftWithinTenMinutesIsRefundableAndOnlyOnce() {
        UUID ai = finish(1, 10, AiOutcome.AI_DRAFT);
        UUID auto = finish(1, 11, AiOutcome.AUTO_DRAFT);
        UUID late = finish(1, 12, AiOutcome.AI_DRAFT);

        assertThat(inFlight.takeRefundable(1, auto)).isEmpty();
        assertThat(inFlight.takeRefundable(1, ai)).contains(10L);
        assertThat(inFlight.takeRefundable(1, ai)).isEmpty();
        clock.set(NOW.plus(Duration.ofMinutes(11)));
        assertThat(inFlight.takeRefundable(1, late)).isEmpty();
    }
}
