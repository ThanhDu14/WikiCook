package com.wikicook.mealplan.dto;

import java.util.List;

/** One item when moved to an empty slot, two when swapped. */
public record MoveResult(Kind kind, List<PlacedItem> items) {

    public enum Kind {
        MOVED, SWAPPED
    }
}
