package com.wikicook.aiplanner.controller;

import java.time.Instant;
import org.springframework.http.HttpStatus;

/** Five counted generations in the last 60 minutes (US5, FR-025). */
public class RateLimitedException extends AiPlannerException {

    private final Instant nextAvailableAt;

    public RateLimitedException(Instant nextAvailableAt) {
        super(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "No generations left this hour", null);
        this.nextAvailableAt = nextAvailableAt;
    }

    public Instant nextAvailableAt() {
        return nextAvailableAt;
    }
}
