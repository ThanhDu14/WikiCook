package com.wikicook.aiplanner.domain;

/** How to loosen the constraints when too few recipes are eligible (FR-024). */
public enum RelaxSuggestion {
    INCREASE_MAX_COOK_TIME,
    REMOVE_PREFERRED_CUISINES,
    SELECT_FEWER_SLOTS
}
