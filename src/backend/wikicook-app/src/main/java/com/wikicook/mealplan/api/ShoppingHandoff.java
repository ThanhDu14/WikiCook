package com.wikicook.mealplan.api;

import java.time.LocalDate;
import java.util.List;

/**
 * The week's available dishes in week order, and how many unavailable ones were left out (FR-024, FR-025). Also
 * the body of {@code GET …/shopping-handoff} (openapi {@code ShoppingHandoff}).
 */
public record ShoppingHandoff(LocalDate weekStart, List<HandoffItem> items, int skippedUnavailable) {
}
