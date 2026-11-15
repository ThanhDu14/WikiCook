package com.wikicook.auth.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** One audit entry as the API shows it (FR-025); {@code details} never holds an email or a display name. */
public record AuditLogDto(long id, Long actorId, String action, String targetType, Long targetId,
        Map<String, Object> details, String ipAddress, Instant createdAt) {

    /** A page, newest first; {@code hasNext} says whether another page follows. */
    public record Page(List<AuditLogDto> content, int page, int size, boolean hasNext) {
    }
}
