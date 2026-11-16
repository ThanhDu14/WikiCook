package com.wikicook.auth.service;

import com.wikicook.auth.api.BlockedEmails;
import com.wikicook.auth.domain.TokenPurpose;
import com.wikicook.auth.dto.RegisterRequest;
import com.wikicook.auth.dto.RegisterResponse;
import com.wikicook.auth.mail.LinkMailer;
import com.wikicook.auth.security.BcryptBulkhead;
import com.wikicook.auth.service.EmailRequestLimiter.Slot;
import com.wikicook.auth.service.OneTimeTokenService.Issued;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Creates an unverified Member (FR-001, FR-003, FR-005). Neither registering nor verifying signs the user in.
 *
 * <ul>
 * <li>The password is hashed first, through the BCrypt bulkhead and outside any transaction.
 * <li>{@code INSERT … ON CONFLICT (email) DO NOTHING}: of two simultaneous requests (a double click) exactly one
 *     inserts, the other gets 409 {@code EMAIL_TAKEN}, and only the winner sends an email (FR-002).
 * <li>An email on the Blocked Email list is refused with a neutral 422 (FR-026).
 * <li>The email limit (FR-017) never refuses a registration: above it the account is created without a token and
 *     without an email, and the response says how long to wait.
 * <li>The link is mailed after the commit by {@link LinkMailer}.
 * </ul>
 */
@Service
public class RegistrationService {

    static final Duration VERIFICATION_TTL = Duration.ofHours(24);

    private final BlockedEmails blockedEmails;
    private final PasswordEncoder encoder;
    private final BcryptBulkhead bulkhead;
    private final TransactionTemplate transaction;
    private final JdbcTemplate jdbc;
    private final EmailHasher hasher;
    private final EmailRequestLimiter limiter;
    private final OneTimeTokenService tokens;
    private final LinkMailer mailer;
    private final Clock clock;

    public RegistrationService(BlockedEmails blockedEmails, PasswordEncoder encoder, BcryptBulkhead bulkhead,
            TransactionTemplate transaction, JdbcTemplate jdbc, EmailHasher hasher, EmailRequestLimiter limiter,
            OneTimeTokenService tokens, LinkMailer mailer, Clock clock) {
        this.blockedEmails = blockedEmails;
        this.encoder = encoder;
        this.bulkhead = bulkhead;
        this.transaction = transaction;
        this.jdbc = jdbc;
        this.hasher = hasher;
        this.limiter = limiter;
        this.tokens = tokens;
        this.mailer = mailer;
        this.clock = clock;
    }

    /** @throws AuthException 422 {@code REGISTRATION_REFUSED}, 409 {@code EMAIL_TAKEN} */
    public RegisterResponse register(RegisterRequest request) {
        String email = request.email();
        if (blockedEmails.isBlocked(email)) {
            throw AuthException.registrationRefused();
        }
        String passwordHash = bulkhead.run(() -> encoder.encode(request.password()));

        return transaction.execute(status -> {
            Instant now = clock.instant();
            List<Long> inserted = jdbc.queryForList("""
                    INSERT INTO users (email, password_hash, display_name, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT (email) DO NOTHING
                    RETURNING id
                    """, Long.class, email, passwordHash, request.displayName(), Timestamp.from(now),
                    Timestamp.from(now));
            if (inserted.isEmpty()) {
                throw AuthException.emailTaken();
            }
            long userId = inserted.get(0);

            Slot slot = limiter.tryAcquire(hasher.hash(email), TokenPurpose.EMAIL_VERIFICATION);
            if (!slot.allowed()) {
                return new RegisterResponse(email, false, null, slot.retryAfterSeconds());
            }
            Issued link = tokens.issue(userId, TokenPurpose.EMAIL_VERIFICATION, VERIFICATION_TTL);
            mailer.sendVerification(email, request.displayName(), link.token());
            return new RegisterResponse(email, true, link.expiresAt(), null);
        });
    }
}
