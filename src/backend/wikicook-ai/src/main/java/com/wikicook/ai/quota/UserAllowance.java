package com.wikicook.ai.quota;

import java.time.Instant;

/**
 * Meal planner generations a user has left in the rolling hour.
 *
 * @param nextAvailableAt when the oldest counted generation leaves the window; set only when {@code remaining} is 0
 */
public record UserAllowance(int remaining, Instant nextAvailableAt) {
}
