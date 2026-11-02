package com.wikicook.mealplan.service.exception;

import org.springframework.http.HttpStatus;

/** The slot is not in the state the client saw: it changed elsewhere (research R4, edge case "two tabs"). */
public class SlotChangedException extends MealPlanException {

    public SlotChangedException() {
        super(HttpStatus.CONFLICT, "SLOT_CHANGED", "The slot changed elsewhere; reload the week");
    }
}
