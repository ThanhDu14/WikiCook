package com.wikicook.shoppinglist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateCustomItemRequest(
    @NotNull UUID clientUuid,
    @NotBlank @Size(min = 1, max = 100) String customName,
    @Positive BigDecimal quantity,
    String unitName,
    Long aisleCategoryId
) {}
