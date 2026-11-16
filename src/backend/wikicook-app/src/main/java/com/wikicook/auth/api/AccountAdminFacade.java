package com.wikicook.auth.api;

import com.wikicook.common.security.Role;
import java.time.Instant;

/**
 * The account operations of spec 010 for other modules, chiefly the moderation screens of spec 100 (contracts/
 * integration.md 2.3). The REST endpoints under {@code /api/v1/admin/users} call exactly these methods. The hierarchy,
 * self-action, last-Administrator, re-authentication and audit rules are inside, so callers do not repeat them. All
 * operations of one account are serialized, and each first closes a suspension whose end date has passed (research R9).
 *
 * @throws com.wikicook.auth.service.AuthException for every refused action, with the codes of the contract
 */
public interface AccountAdminFacade {

    AccountAdminView view(long actorId, long targetId);

    /** Takes effect on the target's next request; no email, a notice in the app instead (FR-024a). */
    AccountAdminView changeRole(long actorId, long targetId, Role newRole);

    /** Moderator or above; every suspension is at most 30 days; a Moderator suspends only Member or Contributor. */
    AccountAdminView suspend(long actorId, long targetId, String reason, Instant endsAt);

    AccountAdminView ban(long actorId, long targetId, String reason);

    /** Early lift of a suspension or ban; a suspension that ends by itself is closed by 010's own job. */
    AccountAdminView reinstate(long actorId, long targetId, String reason);
}
