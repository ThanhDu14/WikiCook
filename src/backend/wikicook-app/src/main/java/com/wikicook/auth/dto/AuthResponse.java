package com.wikicook.auth.dto;

/** Sign-in and refresh result; the refresh token travels only in the HttpOnly cookie, never in the body (R2). */
public record AuthResponse(String accessToken, long expiresIn, CurrentAccountDto account) {
}
