package com.wikicook.auth.service;

import com.wikicook.common.security.Role;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Who may do what to whom (FR-022, FR-022a; research R10). A pure rule set, so every combination can be tested without
 * a database; {@link AccountAdminService} supplies the facts and applies the result. Checked in this order, so a caller
 * who is not allowed never learns anything else (for example that re-authenticating would help):
 *
 * <ol>
 * <li>nobody acts on themselves: {@code SELF_ACTION_FORBIDDEN};
 * <li>a system account is never affected, and only a higher role acts on a lower one (an Administrator may also act
 *     on another Administrator): {@code ACCESS_DENIED};
 * <li>suspending needs at least Moderator, everything else (role, ban, reinstatement) needs Administrator;
 * <li>every suspension ends in the future and lasts at most 30 days, whoever orders it:
 *     {@code SUSPENSION_LIMIT_EXCEEDED};
 * <li>any action on another Administrator, and any promotion to Administrator, needs a re-authentication in the last
 *     5 minutes: {@code REAUTH_REQUIRED}.
 * </ol>
 */
@Component
public class AccountActionPolicy {

    public static final Duration MAX_SUSPENSION = Duration.ofDays(30);

    public enum Action { CHANGE_ROLE, SUSPEND, BAN, REINSTATE }

    /** An account taking part in an action. */
    public record Party(long id, Role role, boolean system) {
    }

    /** What is asked: {@code newRole} for CHANGE_ROLE, {@code endsAt} for SUSPEND, null otherwise. */
    public record Request(Action action, Role newRole, Instant endsAt) {
    }

    /** @throws AuthException the first rule that is broken; returns normally when the action is allowed */
    public void check(Party actor, Party target, Request request, Instant now, boolean reauthenticated) {
        if (actor.id() == target.id()) {
            throw AuthException.selfActionForbidden();
        }
        if (target.system() || !mayAct(actor.role(), target.role(), request.action())) {
            throw AuthException.accessDenied();
        }
        if (request.action() == Action.SUSPEND) {
            checkSuspensionEnd(request.endsAt(), now);
        }
        if (needsReauthentication(target, request) && !reauthenticated) {
            throw AuthException.reauthRequired();
        }
    }

    private static boolean mayAct(Role actor, Role target, Action action) {
        Role needed = action == Action.SUSPEND ? Role.MODERATOR : Role.ADMINISTRATOR;
        boolean enoughRole = actor == needed || actor.outranks(needed);
        boolean higherThanTarget = actor.outranks(target) || (actor == Role.ADMINISTRATOR && target == Role.ADMINISTRATOR);
        return enoughRole && higherThanTarget;
    }

    private static void checkSuspensionEnd(Instant endsAt, Instant now) {
        if (endsAt == null || !endsAt.isAfter(now)) {
            throw AuthException.validation("A suspension must end in the future");
        }
        if (endsAt.isAfter(now.plus(MAX_SUSPENSION))) {
            throw AuthException.suspensionLimitExceeded();
        }
    }

    private static boolean needsReauthentication(Party target, Request request) {
        boolean onAnotherAdministrator = target.role() == Role.ADMINISTRATOR;
        boolean promotion = request.action() == Action.CHANGE_ROLE && request.newRole() == Role.ADMINISTRATOR
                && target.role() != Role.ADMINISTRATOR;
        return onAnotherAdministrator || promotion;
    }
}
