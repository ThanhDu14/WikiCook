package com.wikicook.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BanRequest(@NotBlank @Size(max = 500) String reason) {
}
