package com.wikicook.auth.dto;

import java.time.Instant;

/** {@code POST /auth/password/reset/validate}: whose password the link changes and until when it works (AU06). */
public record ResetTokenInfo(String email, Instant expiresAt) {
}
