package com.wikicook.auth.service;

import com.wikicook.auth.api.AuditWriter;
import com.wikicook.auth.domain.TokenPurpose;
import com.wikicook.auth.domain.UserAccount;
import com.wikicook.auth.dto.ResetTokenInfo;
import com.wikicook.auth.mail.LinkMailer;
import com.wikicook.auth.repository.UserAccountRepository;
import com.wikicook.auth.security.BcryptBulkhead;
import com.wikicook.auth.service.OneTimeTokenService.Consumption;
import com.wikicook.auth.service.OneTimeTokenService.Inspection;
import com.wikicook.auth.service.OneTimeTokenService.Issued;
import com.wikicook.common.security.AccountStatus;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Password reset (US4, FR-014 to FR-017; research R4, R6, R8).
 *
 * <ul>
 * <li><b>Asking for a link</b> answers the same, in the same time, whether or not the address is registered (NFR-003):
 *     the request only takes a slot of the email limit, which counts every address alike (FR-017), and hands the rest
 *     (looking the account up, issuing the token, mailing it) to a background task on a virtual thread. A Google-only,
 *     suspended or banned account gets no link, with the same answer (FR-014). A locked account still gets one: a reset
 *     does not lift the lock (FR-012).
 * <li><b>Checking a link</b> shows whose password it changes and when it expires, and does not use it up (AU06).
 * <li><b>Setting the password</b> hashes first, outside any transaction, then in one transaction changes the hash, uses
 *     the token, ends every session and writes the audit row (FR-016). A password that breaks FR-004 never reaches the
 *     token: the form rules are checked before the controller runs and the comparison with the email here, before the
 *     token is consumed (AF 3.5). It does not mark the email as verified (R6).
 * </ul>
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    /** FR-015. */
    static final Duration RESET_TTL = Duration.ofMinutes(30);

    private final OneTimeTokenService tokens;
    private final UserAccountRepository users;
    private final EmailRequestLimiter limiter;
    private final EmailHasher hasher;
    private final LinkMailer mailer;
    private final PasswordEncoder encoder;
    private final BcryptBulkhead bulkhead;
    private final SessionService sessions;
    private final AuditWriter audit;
    private final TransactionTemplate transaction;
    private final Executor background;

    @Autowired
    public PasswordResetService(OneTimeTokenService tokens, UserAccountRepository users, EmailRequestLimiter limiter,
            EmailHasher hasher, LinkMailer mailer, PasswordEncoder encoder, BcryptBulkhead bulkhead,
            SessionService sessions, AuditWriter audit, TransactionTemplate transaction) {
        this(tokens, users, limiter, hasher, mailer, encoder, bulkhead, sessions, audit, transaction,
                Executors.newVirtualThreadPerTaskExecutor());
    }

    PasswordResetService(OneTimeTokenService tokens, UserAccountRepository users, EmailRequestLimiter limiter,
            EmailHasher hasher, LinkMailer mailer, PasswordEncoder encoder, BcryptBulkhead bulkhead,
            SessionService sessions, AuditWriter audit, TransactionTemplate transaction, Executor background) {
        this.tokens = tokens;
        this.users = users;
        this.limiter = limiter;
        this.hasher = hasher;
        this.mailer = mailer;
        this.encoder = encoder;
        this.bulkhead = bulkhead;
        this.sessions = sessions;
        this.audit = audit;
        this.transaction = transaction;
        this.background = background;
    }

    /**
     * Asks for a reset link. {@code email} is already normalized (FR-002).
     *
     * @throws AuthException 429 {@code EMAIL_RATE_LIMITED}
     */
    public void requestReset(String email) {
        limiter.requireSlot(hasher.hash(email), TokenPurpose.PASSWORD_RESET);
        background.execute(() -> issueLinkQuietly(email));
    }

    private void issueLinkQuietly(String email) {
        try {
            transaction.executeWithoutResult(status -> users.findByEmail(email).filter(PasswordResetService::eligible)
                    .ifPresent(user -> {
                        Issued link = tokens.issue(user.getId(), TokenPurpose.PASSWORD_RESET, RESET_TTL);
                        mailer.sendPasswordReset(user.getEmail(), user.getDisplayName(), link.token());
                    }));
        } catch (RuntimeException e) {
            // the caller already has its 202; the person asks again (Edge Cases, FR-017)
            log.error("Could not issue a password reset link: {}", e.toString());
        }
    }

    /** FR-014: only an active account that has a password gets a link. */
    private static boolean eligible(UserAccount user) {
        return user.getPasswordHash() != null && user.getStatus() == AccountStatus.ACTIVE;
    }

    /**
     * Checks a link without using it.
     *
     * @throws AuthException 410 {@code TOKEN_EXPIRED} or {@code TOKEN_INVALID}
     */
    public ResetTokenInfo validate(String token) {
        Inspection inspection = tokens.inspect(token, TokenPurpose.PASSWORD_RESET);
        UserAccount user = resettable(inspection);
        return new ResetTokenInfo(user.getEmail(), inspection.expiresAt());
    }

    /**
     * Sets the new password. The caller has checked the rules of FR-004 that need no account.
     *
     * @throws AuthException 410 {@code TOKEN_EXPIRED} or {@code TOKEN_INVALID}; 400 {@code VALIDATION_ERROR} when the
     *     password is the account's email
     */
    public void reset(String token, String newPassword) {
        UserAccount user = resettable(tokens.inspect(token, TokenPurpose.PASSWORD_RESET));
        if (EmailNormalizer.normalize(newPassword).equals(EmailNormalizer.normalize(user.getEmail()))) {
            throw AuthException.passwordEqualsEmail();
        }
        String passwordHash = bulkhead.run(() -> encoder.encode(newPassword));

        transaction.executeWithoutResult(status -> {
            Consumption used = tokens.consume(token, TokenPurpose.PASSWORD_RESET);
            switch (used.status()) {
                case CONSUMED -> {
                }
                case EXPIRED -> throw AuthException.tokenExpired();
                default -> throw AuthException.tokenInvalid(); // used by another request since the check
            }
            UserAccount account = users.findById(used.userId()).filter(PasswordResetService::notSanctioned)
                    .orElseThrow(AuthException::tokenInvalid);
            account.setPasswordHash(passwordHash);
            users.save(account);
            int revoked = sessions.revokeAll(account.getId());
            audit.write("PASSWORD_RESET", account.getId(), "USER", account.getId(), Map.of("sessionsRevoked", revoked));
        });
    }

    /** The account behind a live link; any other state of the link, or of the account, is a 410. */
    private UserAccount resettable(Inspection inspection) {
        switch (inspection.status()) {
            case VALID -> {
                return users.findById(inspection.userId()).filter(PasswordResetService::notSanctioned)
                        .orElseThrow(AuthException::tokenInvalid);
            }
            case EXPIRED -> throw AuthException.tokenExpired();
            default -> throw AuthException.tokenInvalid();
        }
    }

    /** FR-014: a link issued before a suspension or ban does not work after it. */
    private static boolean notSanctioned(UserAccount user) {
        return user.getStatus() == AccountStatus.ACTIVE;
    }
}
