package com.wikicook.ai.quota;

/** {@code ai_requests.trigger}: what started the call. */
public enum AiTrigger {
    USER,
    APPROVAL,
    REVISION,
    IMPORT,
    ADMIN_RECALC
}
