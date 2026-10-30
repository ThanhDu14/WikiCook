package com.wikicook.aiplanner.controller;

import org.springframework.http.HttpStatus;

/** Slot suggestions could not be computed in time (FR-038). */
public class SuggestionsUnavailableException extends AiPlannerException {

    public SuggestionsUnavailableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "SUGGESTIONS_UNAVAILABLE", "Suggestions are unavailable", null);
    }
}
