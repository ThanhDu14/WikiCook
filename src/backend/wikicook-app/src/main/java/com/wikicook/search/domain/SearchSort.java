package com.wikicook.search.domain;

/** 020 FR-014; RELEVANCE needs a keyword and falls back to NEWEST without one (research R7). */
public enum SearchSort {
    RELEVANCE,
    NEWEST,
    TOP_RATED,
    QUICKEST
}
