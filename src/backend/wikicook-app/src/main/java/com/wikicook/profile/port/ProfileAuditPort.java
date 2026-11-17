package com.wikicook.profile.port;

import java.util.Map;

/**
 * Where the profile module records what a person did (FR-042, research R14). The default does nothing, so US1 and US2
 * run without the audit log of spec 010; task T116 adds the adapter over {@code AuditWriter}. {@code details} never
 * holds an email, a display name, a password or a dietary value, and the dietary save sends none at all.
 */
public interface ProfileAuditPort {

    void record(long userId, String action, Map<String, ?> details);
}
