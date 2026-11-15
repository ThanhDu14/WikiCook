package com.wikicook.auth.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** A suspension: the reason (1-500 characters, shown to the user) and when it ends, in the future (FR-013, FR-024a). */
public record SuspendRequest(@NotBlank @Size(max = 500) String reason, @NotNull @Future Instant endsAt) {
}
