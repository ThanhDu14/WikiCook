package com.wikicook.aiplanner.controller;

import org.springframework.http.HttpStatus;

/** An AI planner error with its HTTP status and stable {@code code} (research R16). */
public abstract class AiPlannerException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected AiPlannerException(HttpStatus status, String code, String detail, Throwable cause) {
        super(detail, cause);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
