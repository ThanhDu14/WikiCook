package com.wikicook.auth.service;

import com.wikicook.auth.api.AccountAdminFacade;
import com.wikicook.auth.api.AccountAdminView;
import com.wikicook.auth.api.AccountAdminView.ActiveSanction;
import com.wikicook.auth.api.AuditWriter;
import com.wikicook.auth.api.ReauthService;
import com.wikicook.auth.domain.EmailOutboxMessage;
import com.wikicook.auth.domain.LiftReason;
import com.wikicook.auth.domain.MailTemplate;
import com.wikicook.auth.domain.SanctionKind;
import com.wikicook.auth.domain.UserAccount;
import com.wikicook.auth.domain.UserSuspension;
import com.wikicook.auth.job.SuspensionExpiryJob;
import com.wikicook.auth.repository.EmailOutboxRepository;
import com.wikicook.auth.repository.UserAccountRepository;
import com.wikicook.auth.repository.UserSessionRepository;
import com.wikicook.auth.repository.UserSuspensionRepository;
import com.wikicook.auth.service.AccountActionPolicy.Action;
import com.wikicook.auth.service.AccountActionPolicy.Party;
import com.wikicook.auth.service.AccountActionPolicy.Request;
import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.CurrentAccount;
import com.wikicook.common.security.Role;
import com.wikicook.common.security.UnauthenticatedException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Role changes, suspensions, bans and reinstatements (FR-022 - FR-024a; research R9, R10). One transaction per
 * operation changes the account, the sanction rows, the sessions, the outbox and the audit trail together.
 *
 * <ul>
 * <li>Every operation first calls {@link SuspensionExpiryJob#finishExpired}, which runs in its own committed
 *     transaction, so a suspension that ended but was not yet closed is closed (and its email queued) before the status
 *     is read, and stays closed even if the operation is then refused with a 409.
 * <li>An operation that touches an Administrator, or changes any role, first locks the set of active Administrators
 *     ({@code SELECT … FOR UPDATE ORDER BY id}) and only then reads the actor and the target. Two Administrators
 *     demoting each other at once therefore queue up, the second sees it is no longer an Administrator, and at least
 *     one stays (FR-023).
 * <li>The actor's role is re-read inside the lock, so a role lowered a moment ago is already effective.
 * <li>A role change sends no email: it sets {@code pending_role_notice_from} (the role before the first unseen change;
 *     going back to it clears the notice) and the app shows a notice at the next request (FR-024a).
 * </ul>
 */
@Service
public class AccountAdminService implements AccountAdminFacade {

    static final Duration REAUTH_WINDOW = Duration.ofMinutes(5);
    private static final String TARGET_TYPE = "USER";

    private final UserAccountRepository users;
    private final UserSuspensionRepository suspensions;
    private final UserSessionRepository sessions;
    private final EmailOutboxRepository outbox;
    private final AuditWriter audit;
    private final ReauthService reauth;
    private final CurrentAccount current;
    private final AccountActionPolicy policy;
    private final SuspensionExpiryJob expiry;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public AccountAdminService(UserAccountRepository users, UserSuspensionRepository suspensions,
            UserSessionRepository sessions, EmailOutboxRepository outbox, AuditWriter audit, ReauthService reauth,
            CurrentAccount current, AccountActionPolicy policy, SuspensionExpiryJob expiry, JdbcTemplate jdbc,
            Clock clock) {
        this.users = users;
        this.suspensions = suspensions;
        this.sessions = sessions;
        this.outbox = outbox;
        this.audit = audit;
        this.reauth = reauth;
        this.current = current;
        this.policy = policy;
        this.expiry = expiry;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AccountAdminView view(long actorId, long targetId) {
        expiry.finishExpired(targetId);
        UserAccount target = users.findById(targetId).orElseThrow(AuthException::userNotFound);
        UserAccount actor = users.findById(actorId).orElseThrow(AuthException::unauthenticated);
        return viewOf(target, actor);
    }

    @Override
    @Transactional
    public AccountAdminView changeRole(long actorId, long targetId, Role newRole) {
        Context context = begin(actorId, targetId, new Request(Action.CHANGE_ROLE, newRole, null));
        UserAccount target = context.target();
        Role old = target.getRole();
        if (old == newRole) {
            return viewOf(target, context.actor());
        }
        if (newRole != Role.ADMINISTRATOR && target.getStatus() == AccountStatus.ACTIVE) {
            assertNotLastAdministrator(context);
        }
        Role noticeFrom = target.getPendingRoleNoticeFrom() != null ? target.getPendingRoleNoticeFrom() : old;
        target.setPendingRoleNoticeFrom(noticeFrom == newRole ? null : noticeFrom);
        target.setRole(newRole);
        users.save(target);
        audit.write("ROLE_CHANGED", actorId, TARGET_TYPE, targetId, Map.of("from", old.name(), "to", newRole.name()));
        return viewOf(target, context.actor());
    }

    @Override
    @Transactional
    public AccountAdminView suspend(long actorId, long targetId, String reason, Instant endsAt) {
        Context context = begin(actorId, targetId, new Request(Action.SUSPEND, null, endsAt));
        UserAccount target = context.target();
        if (target.getStatus() != AccountStatus.ACTIVE) {
            throw AuthException.accountNotActive();
        }
        assertNotLastAdministrator(context);
        Instant now = clock.instant();
        UserSuspension sanction = suspensions.save(
                new UserSuspension(targetId, SanctionKind.SUSPENSION, reason, now, endsAt, actorId));
        target.setStatus(AccountStatus.SUSPENDED);
        users.save(target);
        sessions.revokeAllOf(targetId, now);
        outbox.save(new EmailOutboxMessage(targetId, MailTemplate.ACCOUNT_SUSPENDED,
                Map.of("reason", reason, "endsAt", endsAt.toString()), now, now));
        audit.write("ACCOUNT_SUSPENDED", actorId, TARGET_TYPE, targetId,
                Map.of("reason", reason, "endsAt", endsAt.toString()));
        return viewOf(target, context.actor(), sanction);
    }

    @Override
    @Transactional
    public AccountAdminView ban(long actorId, long targetId, String reason) {
        Context context = begin(actorId, targetId, new Request(Action.BAN, null, null));
        UserAccount target = context.target();
        if (target.getStatus() == AccountStatus.BANNED) {
            throw AuthException.accountNotActive();
        }
        assertNotLastAdministrator(context);
        Instant now = clock.instant();
        Optional<UserSuspension> running = suspensions.findRunning(targetId);
        if (running.isPresent()) {
            running.get().lift(now, actorId, LiftReason.REPLACED_BY_BAN);
            // flush now: at most one running sanction per user, and Hibernate would insert the ban before updating
            suspensions.saveAndFlush(running.get());
        }
        UserSuspension ban = suspensions.save(new UserSuspension(targetId, SanctionKind.BAN, reason, now, null, actorId));
        target.setStatus(AccountStatus.BANNED);
        users.save(target);
        sessions.revokeAllOf(targetId, now);
        outbox.save(new EmailOutboxMessage(targetId, MailTemplate.ACCOUNT_BANNED, Map.of("reason", reason), now, now));
        audit.write("ACCOUNT_BANNED", actorId, TARGET_TYPE, targetId, Map.of("reason", reason));
        return viewOf(target, context.actor(), ban);
    }

    @Override
    @Transactional
    public AccountAdminView reinstate(long actorId, long targetId, String reason) {
        Context context = begin(actorId, targetId, new Request(Action.REINSTATE, null, null));
        UserAccount target = context.target();
        if (target.getStatus() == AccountStatus.ACTIVE) {
            throw AuthException.accountNotSanctioned();
        }
        Instant now = clock.instant();
        Optional<UserSuspension> running = suspensions.findRunning(targetId);
        running.ifPresent(sanction -> {
            sanction.lift(now, actorId, LiftReason.LIFTED);
            suspensions.save(sanction);
            MailTemplate mail = sanction.getKind() == SanctionKind.BAN ? MailTemplate.BAN_LIFTED
                    : MailTemplate.SUSPENSION_ENDED;
            outbox.save(new EmailOutboxMessage(targetId, mail, Map.of("lifted", "early"), now, now));
        });
        target.setStatus(AccountStatus.ACTIVE);
        users.save(target);
        audit.write("ACCOUNT_REINSTATED", actorId, TARGET_TYPE, targetId,
                reason == null || reason.isBlank() ? Map.of() : Map.of("reason", reason));
        return viewOf(target, context.actor());
    }

    // ---- shared steps ----

    private record Context(UserAccount actor, UserAccount target, List<Long> activeAdministrators) {
    }

    private Context begin(long actorId, long targetId, Request request) {
        expiry.finishExpired(targetId);
        String targetRole = jdbc.query("SELECT role FROM users WHERE id = ?", rs -> rs.next() ? rs.getString(1) : null,
                targetId);
        if (targetRole == null) {
            throw AuthException.userNotFound();
        }
        List<Long> administrators = Role.ADMINISTRATOR.name().equals(targetRole) || request.action() == Action.CHANGE_ROLE
                ? lockActiveAdministrators() : List.of();
        UserAccount target = users.lockById(targetId).orElseThrow(AuthException::userNotFound);
        UserAccount actor = users.findById(actorId).orElseThrow(AuthException::unauthenticated);
        policy.check(party(actor), party(target), request, clock.instant(), reauthenticated());
        return new Context(actor, target, administrators);
    }

    /** All active Administrators, locked in id order so concurrent operations cannot deadlock (FR-023). */
    private List<Long> lockActiveAdministrators() {
        return jdbc.queryForList("SELECT id FROM users WHERE role = 'ADMINISTRATOR' AND status = 'ACTIVE' "
                + "ORDER BY id FOR UPDATE", Long.class);
    }

    private static void assertNotLastAdministrator(Context context) {
        UserAccount target = context.target();
        boolean activeAdministrator = target.getRole() == Role.ADMINISTRATOR && target.getStatus() == AccountStatus.ACTIVE;
        if (activeAdministrator && context.activeAdministrators().size() == 1
                && context.activeAdministrators().contains(target.getId())) {
            throw AuthException.lastAdministrator();
        }
    }

    private boolean reauthenticated() {
        try {
            return reauth.isRecent(current.sessionId(), REAUTH_WINDOW);
        } catch (UnauthenticatedException e) {
            return false; // not inside a request: nothing to re-authenticate
        }
    }

    private static Party party(UserAccount user) {
        return new Party(user.getId(), user.getRole(), user.isSystem());
    }

    private AccountAdminView viewOf(UserAccount target, UserAccount actor) {
        return viewOf(target, actor, suspensions.findRunning(target.getId()).orElse(null));
    }

    private AccountAdminView viewOf(UserAccount target, UserAccount actor, UserSuspension sanction) {
        ActiveSanction active = sanction == null || sanction.getLiftedAt() != null ? null
                : new ActiveSanction(sanction.getKind().name(), sanction.getReason(), sanction.getStartsAt(),
                        sanction.getEndsAt());
        String email = actor.getRole() == Role.ADMINISTRATOR ? target.getEmail() : null;
        return new AccountAdminView(target.getId(), target.getDisplayName(), email, target.getRole(),
                target.getStatus(), target.isEmailVerified(), active);
    }
}
