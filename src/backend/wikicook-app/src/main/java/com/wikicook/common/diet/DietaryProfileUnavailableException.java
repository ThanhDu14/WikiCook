package com.wikicook.common.diet;

/**
 * The dietary profile could not be read (FR-026a, research R8). Unchecked and never turned into an empty profile: a
 * planner that treated "can not read" as "no allergies" could pick a dish that harms the person. {@code
 * GlobalExceptionHandler} answers 503 {@code PROFILE_UNAVAILABLE}, so the caller picks nothing and says why. The message
 * and cause are for the log only; they never reach the client.
 */
public class DietaryProfileUnavailableException extends RuntimeException {

    public DietaryProfileUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
