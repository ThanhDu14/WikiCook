package com.wikicook.shoppinglist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTitleRequest(
    @NotBlank @Size(min = 1, max = 60) String title
) {}
