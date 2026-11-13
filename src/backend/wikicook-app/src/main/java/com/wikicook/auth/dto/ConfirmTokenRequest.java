package com.wikicook.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmTokenRequest(@NotBlank @Size(max = 128) String token) {
}
