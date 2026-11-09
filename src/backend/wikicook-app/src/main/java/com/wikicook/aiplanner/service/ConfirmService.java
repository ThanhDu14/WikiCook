package com.wikicook.aiplanner.service;

import com.wikicook.aiplanner.dto.ConfirmRequest;
import com.wikicook.aiplanner.dto.ConfirmResult;
import com.wikicook.mealplan.api.BatchSaveResult;
import com.wikicook.mealplan.api.DraftItem;
import com.wikicook.mealplan.api.MealPlanWriteFacade;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * Saves a confirmed draft into the user's week (FR-018, FR-019). 030 does it in one transaction and skips meals
 * filled meanwhile (another device or tab) and recipes no longer PUBLISHED. No AI call and no quota: the
 * generation was already counted, and stays counted once its draft is saved.
 */
@Service
public class ConfirmService {

    private final MealPlanWriteFacade mealPlans;
    private final InFlightGenerations inFlight;

    public ConfirmService(MealPlanWriteFacade mealPlans, InFlightGenerations inFlight) {
        this.mealPlans = mealPlans;
        this.inFlight = inFlight;
    }

    public ConfirmResult confirm(long userId, LocalDate weekStart, ConfirmRequest request) {
        BatchSaveResult result = mealPlans.saveDraft(userId, weekStart, request.items().stream()
                .map(item -> new DraftItem(item.day(), item.meal(), item.recipeId(), item.servings(), item.source()))
                .toList());
        // saved: cancelling this generation later no longer gives it back (research R9)
        inFlight.markConfirmed(userId, request.requestId());
        return new ConfirmResult(result.saved(), result.skippedOccupied(), result.skippedUnavailable());
    }
}
