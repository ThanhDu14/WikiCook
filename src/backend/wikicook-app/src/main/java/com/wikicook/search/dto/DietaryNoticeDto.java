package com.wikicook.search.dto;

/**
 * Dietary-profile state of one search (020 FR-019, FR-020).
 *
 * @param profileAvailable false for guests
 * @param applied          the profile is non-empty and the override is off: conflicting recipes are removed
 * @param hiddenCount      conflicting recipes over all pages, whether removed or shown
 * @param showingHidden    the override is on and conflicting recipes are included with warnings
 */
public record DietaryNoticeDto(boolean profileAvailable, boolean applied, long hiddenCount, boolean showingHidden) {
}
