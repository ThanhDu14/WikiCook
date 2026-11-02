package com.wikicook.mealplan.dto;

import jakarta.validation.constraints.NotNull;

/** The slot as the client saw it (research R4); a {@code null} state in a request means "the slot is empty". */
public record ExpectedSlotState(@NotNull Long itemId, @NotNull Integer version) {
}
