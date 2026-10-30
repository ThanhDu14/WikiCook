package com.wikicook.aiplanner.controller;

import com.wikicook.aiplanner.domain.RelaxSuggestion;
import java.util.List;
import org.springframework.http.HttpStatus;

/** A meal has fewer eligible recipes than slots (US4, FR-012, FR-024). */
public class NotEnoughRecipesException extends AiPlannerException {

    private final int eligibleCount;
    private final int requestedSlots;
    private final List<RelaxSuggestion> suggestions;

    public NotEnoughRecipesException(int eligibleCount, int requestedSlots, List<RelaxSuggestion> suggestions) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "NOT_ENOUGH_RECIPES", "Not enough recipes match the constraints", null);
        this.eligibleCount = eligibleCount;
        this.requestedSlots = requestedSlots;
        this.suggestions = List.copyOf(suggestions);
    }

    public int eligibleCount() {
        return eligibleCount;
    }

    public int requestedSlots() {
        return requestedSlots;
    }

    public List<RelaxSuggestion> suggestions() {
        return suggestions;
    }
}
