package com.wikicook.auth.dto;

import jakarta.validation.constraints.Size;

public record ReinstateRequest(@Size(max = 500) String reason) {
}
