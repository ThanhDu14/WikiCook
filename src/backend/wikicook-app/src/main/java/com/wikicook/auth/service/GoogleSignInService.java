package com.wikicook.auth.service;

import com.wikicook.auth.api.AccountResetEvent;
import com.wikicook.auth.api.AuditWriter;
import com.wikicook.auth.api.BlockedEmails;
import com.wikicook.auth.domain.UserAccount;
import com.wikicook.auth.domain.UserSuspension;
import com.wikicook.auth.job.SuspensionExpiryJob;
import com.wikicook.auth.repository.UserAccountRepository;
import com.wikicook.auth.repository.UserSuspensionRepository;
import com.wikicook.auth.service.SessionService.NewSession;
import com.wikicook.common.security.AccountStatus;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * What happens with a Google identity once Google has proved it (spec 010 US5, FR-018, FR-019, FR-026; research R7).
 * The table of R7 is read from the top, and the order matters: a suspended or banned account is refused before anything
 * can take it over, because taking over deletes the {@code users} row and with it the sanction.
 *
 * <ol>
 * <li>no email, or an email Google did not verify: refused;
 * <li>the account of this {@code google_sub}, or of this email, is suspended or banned (also when it is unverified):
 *     nothing changes and the reason goes back to the caller; a suspension that has ended is closed first;
 * <li>known {@code google_sub}: sign in;
 * <li>email of a <b>verified</b> account: link the {@code google_sub} and sign in, keeping its data and role;
 * <li>email of an <b>unverified</b> account: take it over, as one transaction (publish {@link AccountResetEvent},
 *     delete the row, insert it again with the Google name, verified and without a password, audit
 *     {@code ACCOUNT_RESET}); any failure rolls everything back;
 * <li>no account, email on the Blocked Email list: refused without a reason (FR-026);
 * <li>otherwise: a new verified Member.
 * </ol>
 *
 * The sign-in lock (FR-012) is not consulted: it does not stop Google. The session is the short one, as without
 * "Remember me" (FR-018). Not transactional as a whole: the decision is one transaction, the session another.
 */
@Service
public class GoogleSignInService {

    static final int NAME_MAX = 50;
    private static final String FALLBACK_NAME = "Thành viên";

    private final UserAccountRepository users;
    private final UserSuspensionRepository suspensions;
    private final ClosureTokenService closureTokens;
    private final SuspensionExpiryJob expiry;
    private final BlockedEmails blockedEmails;
    private final SessionService sessions;
    private final AuditWriter audit;
    private final EmailHasher hasher;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transaction;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public GoogleSignInService(UserAccountRepository users, UserSuspensionRepository suspensions,
            ClosureTokenService closureTokens, SuspensionExpiryJob expiry, BlockedEmails blockedEmails, SessionService sessions, AuditWriter audit,
            EmailHasher hasher, ApplicationEventPublisher events, TransactionTemplate transaction, JdbcTemplate jdbc,
            Clock clock) {
        this.users = users;
        this.suspensions = suspensions;
        this.closureTokens = closureTokens;
        this.expiry = expiry;
        this.blockedEmails = blockedEmails;
        this.sessions = sessions;
        this.audit = audit;
        this.hasher = hasher;
        this.events = events;
        this.transaction = transaction;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** The claims of the Google ID token that this service needs. {@code subject} is Google's stable id of the person. */
    public record GoogleIdentity(String subject, String email, boolean emailVerified, String name) {
    }

    public enum Kind {
        /** A session was created: set the cookie and go on. */
        SIGNED_IN,
        /** No email, or one Google did not verify (FR-018). */
        NO_VERIFIED_EMAIL,
        /** The email is on the Blocked Email list (FR-026); the answer says nothing more. */
        REGISTRATION_REFUSED,
        /** Suspended or banned: see {@link Outcome#restriction()} (FR-013, FR-019). */
        RESTRICTED,
        /** The email belongs to an account linked to another Google identity; nothing is changed. */
        IDENTITY_CONFLICT
    }

    public record Outcome(Kind kind, NewSession session, AuthException restriction) {

        static Outcome of(Kind kind) {
            return new Outcome(kind, null, null);
        }
    }

    private record Decision(Kind kind, long userId, AuthException restriction) {

        static Decision signedIn(long userId) {
            return new Decision(Kind.SIGNED_IN, userId, null);
        }

        static Decision of(Kind kind) {
            return new Decision(kind, 0, null);
        }
    }

    /** @throws RuntimeException when the database or a listener of {@link AccountResetEvent} fails; nothing changed */
    public Outcome signIn(GoogleIdentity identity, String userAgent) {
        String email = EmailNormalizer.normalize(identity.email());
        if (email == null || email.isEmpty() || !identity.emailVerified()) {
            audit.write("SIGN_IN_FAILED", null, "USER", null, Map.of("reason", "GOOGLE_NO_VERIFIED_EMAIL", "provider", "GOOGLE"));
            return Outcome.of(Kind.NO_VERIFIED_EMAIL);
        }
        closeEndedSuspension(identity.subject(), email);

        Decision decision = transaction.execute(status -> decide(identity, email));
        if (decision.kind() != Kind.SIGNED_IN) {
            return new Outcome(decision.kind(), null, decision.restriction());
        }
        NewSession session = sessions.create(decision.userId(), false, userAgent);
        jdbc.update("UPDATE users SET last_sign_in_at = ? WHERE id = ?", Timestamp.from(clock.instant()),
                decision.userId());
        audit.write("SIGN_IN_SUCCEEDED", decision.userId(), "USER", decision.userId(),
                Map.of("sessionId", session.session().getId(), "provider", "GOOGLE"));
        return new Outcome(Kind.SIGNED_IN, session, null);
    }

    /** A suspension past its end date is over even if the job has not run yet (R9), so the sign-in goes on (FR-013). */
    private void closeEndedSuspension(String subject, String email) {
        users.findByGoogleSub(subject).or(() -> users.findByEmail(email))
                .filter(user -> user.getStatus() == AccountStatus.SUSPENDED)
                .ifPresent(user -> expiry.finishExpired(user.getId()));
    }

    private Decision decide(GoogleIdentity identity, String email) {
        UserAccount account = users.findByGoogleSub(identity.subject()).orElse(null);
        boolean knownIdentity = account != null;
        if (account == null) {
            account = users.findByEmail(email).orElse(null);
        }
        if (account != null) {
            account = users.lockById(account.getId()).orElse(null);
        }
        if (account != null) {
            return decideKnown(account, knownIdentity, identity, email);
        }
        if (blockedEmails.isBlocked(email)) {
            audit.write("SIGN_IN_FAILED", null, "USER", null, failure("REGISTRATION_REFUSED", email));
            return Decision.of(Kind.REGISTRATION_REFUSED);
        }
        UserAccount created = create(identity, email);
        return Decision.signedIn(created.getId());
    }

    private Decision decideKnown(UserAccount account, boolean knownIdentity, GoogleIdentity identity, String email) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            audit.write("SIGN_IN_FAILED", null, "USER", account.getId(), Map.of("reason", account.getStatus().name(), "provider", "GOOGLE"));
            return new Decision(Kind.RESTRICTED, account.getId(), restriction(account));
        }
        if (knownIdentity) {
            return Decision.signedIn(account.getId());
        }
        if (account.getGoogleSub() != null) {
            // the same email, but another Google person: never overwrite who is linked
            audit.write("SIGN_IN_FAILED", null, "USER", account.getId(), Map.of("reason", "GOOGLE_IDENTITY_CONFLICT", "provider", "GOOGLE"));
            return Decision.of(Kind.IDENTITY_CONFLICT);
        }
        if (account.isEmailVerified()) {
            account.setGoogleSub(identity.subject());
            users.save(account);
            return Decision.signedIn(account.getId());
        }
        return Decision.signedIn(takeOver(account, identity, email));
    }

    /** FR-019: the unverified account becomes a new one, so whoever pre-loaded it with data or a password gains nothing. */
    private long takeOver(UserAccount old, GoogleIdentity identity, String email) {
        long oldId = old.getId();
        events.publishEvent(new AccountResetEvent(oldId));
        users.delete(old);
        users.flush(); // Hibernate runs inserts before deletes: the email must be free first
        UserAccount fresh = create(identity, email);
        audit.write("ACCOUNT_RESET", null, "USER", fresh.getId(),
                Map.of("previousUserId", oldId, "newUserId", fresh.getId(), "provider", "GOOGLE"));
        return fresh.getId();
    }

    private UserAccount create(GoogleIdentity identity, String email) {
        Instant now = clock.instant();
        UserAccount account = new UserAccount(email, null, displayName(identity.name(), email), now);
        account.setGoogleSub(identity.subject());
        account.setEmailVerifiedAt(now);
        return users.saveAndFlush(account);
    }

    private AuthException restriction(UserAccount account) {
        UserSuspension sanction = suspensions.findRunning(account.getId()).orElse(null);
        String reason = sanction == null ? null : sanction.getReason();
        // the person proved who they are with Google, so a banned one gets the closure token too (R16)
        return account.getStatus() == AccountStatus.BANNED
                ? AuthException.accountBanned(reason, closureTokens.issue(account.getId()))
                : AuthException.accountSuspended(reason, sanction == null ? null : sanction.getEndsAt());
    }

    private Map<String, Object> failure(String reason, String email) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("reason", reason);
        details.put("provider", "GOOGLE");
        details.put("emailHash", hasher.hash(email));
        return details;
    }

    /**
     * The name Google gives, trimmed and cut to 50 code points like a name typed at registration (FR-018); without one
     * the part of the email before the {@code @} (AF 4.8).
     */
    static String displayName(String googleName, String email) {
        String name = googleName == null ? "" : googleName.strip();
        if (name.isEmpty()) {
            int at = email.indexOf('@');
            name = (at > 0 ? email.substring(0, at) : email).strip();
        }
        if (name.codePointCount(0, name.length()) > NAME_MAX) {
            name = name.substring(0, name.offsetByCodePoints(0, NAME_MAX)).strip();
        }
        return name.isEmpty() ? FALLBACK_NAME : name;
    }
}
