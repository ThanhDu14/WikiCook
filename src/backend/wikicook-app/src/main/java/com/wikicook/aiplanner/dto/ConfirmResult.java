package com.wikicook.aiplanner.dto;

/** Response of {@code POST …/confirm} (openapi {@code ConfirmResult}, FR-019). */
public record ConfirmResult(int saved, int skippedOccupied, int skippedUnavailable) {
}
