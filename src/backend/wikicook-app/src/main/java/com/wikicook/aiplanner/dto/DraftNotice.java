package com.wikicook.aiplanner.dto;

/** Why a draft is {@link DraftMode#AUTO}: the AI failed, or today's platform cap is used up (FR-025a). */
public enum DraftNotice {
    AUTO_NO_AI,
    AI_DAILY_CAP
}
