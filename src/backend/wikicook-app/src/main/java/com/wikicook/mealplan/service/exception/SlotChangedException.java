package com.wikicook.mealplan.service.exception;

import org.springframework.http.HttpStatus;

/** The dish is not in the state the client saw: changed, moved or removed elsewhere (research R4, edge case "two tabs"). */
public class SlotChangedException extends MealPlanException {

    public SlotChangedException() {
        super(HttpStatus.CONFLICT, "SLOT_CHANGED", "The dish changed elsewhere; reload the week");
    }
}
