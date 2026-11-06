package com.wikicook.aiplanner.dto;

import com.wikicook.mealplan.domain.SlotKey;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Answer of {@code GET …/options} (openapi {@code GenerationOptions}, research R12).
 *
 * @param selectableDays   empty for a past week, from today for the current week
 * @param emptySlots       slots generation may still fill: no past day, no meal of today past its cutoff
 * @param nextAvailableAt  set when {@code remaining} is 0
 * @param aiAvailableToday {@code false} when the platform's daily cap is used up (the draft will be AUTO)
 */
public record GenerationOptionsResponse(
        LocalDate weekStart,
        List<DayOfWeek> selectableDays,
        List<SlotKey> emptySlots,
        GenerationConstraints defaults,
        boolean profileIncomplete,
        int remaining,
        Instant nextAvailableAt,
        boolean aiAvailableToday) {
}
