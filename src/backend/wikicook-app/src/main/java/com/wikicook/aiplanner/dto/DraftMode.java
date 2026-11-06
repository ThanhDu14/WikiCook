package com.wikicook.aiplanner.dto;

/** Who made the draft: the LLM, or the backup planner without AI (FR-015, FR-027). */
public enum DraftMode {
    AI,
    AUTO
}
