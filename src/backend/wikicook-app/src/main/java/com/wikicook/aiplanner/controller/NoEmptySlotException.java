package com.wikicook.aiplanner.controller;

import org.springframework.http.HttpStatus;

/** Every selected slot is already planned. */
public class NoEmptySlotException extends AiPlannerException {

    public NoEmptySlotException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "NO_EMPTY_SLOT", "The week has no empty slot to fill", null);
    }
}
