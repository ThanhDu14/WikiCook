package com.wikicook.ai.quota;

/** {@code ai_requests.outcome}. Only {@code AI_DRAFT} of the meal planner counts toward the hourly limit. */
public enum AiOutcome {
    AI_DRAFT,
    AUTO_DRAFT,
    NOT_ENOUGH,
    SUCCESS,
    /** Also the provisional outcome written by {@link AiQuotaService#reserve}. */
    FAILED,
    REJECTED,
    TIMEOUT,
    CANCELLED,
    QUOTA_CAP
}
