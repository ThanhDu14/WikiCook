package com.wikicook.auth.api;

import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import java.time.Instant;

/**
 * An account as the admin operations return it. {@code email} is null unless the caller is an Administrator
 * (NFR-009: a Moderator who suspends an account must not learn its email).
 */
public record AccountAdminView(long id, String displayName, String email, Role role, AccountStatus status,
        boolean emailVerified, ActiveSanction activeSanction) {

    /** The suspension or ban that is running; {@code kind} is {@code SUSPENSION} or {@code BAN}. */
    public record ActiveSanction(String kind, String reason, Instant startsAt, Instant endsAt) {
    }
}
