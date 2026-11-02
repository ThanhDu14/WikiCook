package com.wikicook.mealplan.service.exception;

import com.wikicook.mealplan.dto.PlanItemDto;
import org.springframework.http.HttpStatus;

/** The client expected an empty slot but it has a dish; the dialog names it and asks to replace (US2-3). */
public class SlotOccupiedException extends MealPlanException {

    private final PlanItemDto currentItem;

    public SlotOccupiedException(PlanItemDto currentItem) {
        super(HttpStatus.CONFLICT, "SLOT_OCCUPIED", "The slot already has a dish");
        this.currentItem = currentItem;
    }

    public PlanItemDto currentItem() {
        return currentItem;
    }
}
