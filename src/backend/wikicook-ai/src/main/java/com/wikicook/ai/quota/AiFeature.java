package com.wikicook.ai.quota;

/** {@code ai_requests.feature}: which spec made the call; each has its own share of the daily cap. */
public enum AiFeature {
    /** Spec 031, a weekly draft or slot suggestions. */
    MEAL_PLANNER,
    /** Spec 090, nutrition analysis of one recipe. */
    NUTRITION
}
