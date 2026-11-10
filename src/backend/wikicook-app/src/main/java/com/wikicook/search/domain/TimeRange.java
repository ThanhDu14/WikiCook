package com.wikicook.search.domain;

/** Fixed total-time ranges of 020 FR-007 (prep + cook minutes); one at a time. */
public enum TimeRange {
    LE15(15),
    LE30(30),
    LE60(60),
    /** More than 60 minutes. */
    GT60(null);

    private final Integer maxMinutes;

    TimeRange(Integer maxMinutes) {
        this.maxMinutes = maxMinutes;
    }

    /** Upper bound in minutes, or {@code null} for {@link #GT60}. */
    public Integer maxMinutes() {
        return maxMinutes;
    }
}
