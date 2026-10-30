package com.wikicook.aiplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wikicook.aiplanner.controller.GenerationInProgressException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** T069 (part): one running generation per user (research R9). */
class InFlightGenerationsTest {

    private final InFlightGenerations inFlight = new InFlightGenerations();

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
        inFlight.begin(1, UUID.randomUUID()).close();

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
}
