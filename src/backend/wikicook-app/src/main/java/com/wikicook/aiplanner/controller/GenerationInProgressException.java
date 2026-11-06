package com.wikicook.aiplanner.controller;

import org.springframework.http.HttpStatus;

/** The user already has a generation running (research R9). */
public class GenerationInProgressException extends AiPlannerException {

    public GenerationInProgressException() {
        super(HttpStatus.CONFLICT, "GENERATION_IN_PROGRESS", "A generation is already running", null);
    }
}
