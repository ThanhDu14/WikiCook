package com.wikicook.auth.security;

import com.wikicook.common.security.Role;

/**
 * The principal of a request with a valid session: who it is and what the database says about them right now
 * (research R1). {@code pendingRoleNoticeFrom} is the role before an unseen role change, or null (FR-024a).
 */
public record AuthenticatedAccount(long userId, Role role, boolean emailVerified, long sessionId,
        Role pendingRoleNoticeFrom) {
}
