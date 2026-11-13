package com.wikicook.auth.api;

import java.util.Map;

/**
 * Writes one {@code audit_logs} row in the caller's transaction (FR-025). Offered to 011 and 100 so they record their
 * actions in the same table. {@code details} must not contain an email or a display name (NFR-009); the implementation
 * rejects them.
 */
public interface AuditWriter {

    void write(String action, Long actorId, String targetType, Long targetId, Map<String, Object> details);
}
