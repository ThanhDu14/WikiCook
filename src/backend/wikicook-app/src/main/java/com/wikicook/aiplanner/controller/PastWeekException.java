package com.wikicook.aiplanner.controller;

import org.springframework.http.HttpStatus;

/** The chosen week has ended (edge case "tuần đã qua"). */
public class PastWeekException extends AiPlannerException {

    public PastWeekException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "PAST_WEEK", "The week is already over", null);
    }
}
