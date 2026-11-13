package com.wikicook.auth.dto;

import com.wikicook.auth.domain.SanctionKind;
import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import java.time.Instant;

/**
 * What the one query per request returns (research R1): the session joined with its user and the sanction that is
 * running now, if any. Internal; never sent to a client.
 */
public record SessionSnapshot(long sessionId, long userId, Role role, AccountStatus status, boolean emailVerified,
        Instant revokedAt, Instant expiresAt, Instant lastActivityAt, Role pendingRoleNoticeFrom,
        SanctionKind sanctionKind, String sanctionReason, Instant sanctionEndsAt) {
}
