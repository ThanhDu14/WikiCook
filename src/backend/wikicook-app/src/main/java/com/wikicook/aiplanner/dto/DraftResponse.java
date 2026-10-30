package com.wikicook.aiplanner.dto;

import com.wikicook.mealplan.domain.SlotKey;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Answer of {@code POST …/generations} (openapi {@code DraftResponse}); the draft lives only on the client.
 *
 * @param notice      {@code null} for an AI draft
 * @param chooseSlots requested slots left for the user ("Chọn món")
 * @param remaining   counted generations left in the rolling hour, after this one
 */
public record DraftResponse(
        UUID requestId,
        LocalDate weekStart,
        DraftMode mode,
        DraftNotice notice,
        List<DraftItemDto> items,
        List<SlotKey> chooseSlots,
        int removedPicks,
        int remaining,
        Instant nextAvailableAt) {
}
