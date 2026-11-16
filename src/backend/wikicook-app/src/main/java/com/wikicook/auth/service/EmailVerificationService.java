package com.wikicook.auth.service;

import com.wikicook.auth.domain.TokenPurpose;
import com.wikicook.auth.domain.UserAccount;
import com.wikicook.auth.dto.ResendVerificationRequest;
import com.wikicook.auth.dto.VerificationResult;
import com.wikicook.auth.dto.VerificationResult.Status;
import com.wikicook.auth.mail.LinkMailer;
import com.wikicook.auth.repository.UserAccountRepository;
import com.wikicook.auth.service.OneTimeTokenService.Consumption;
import com.wikicook.auth.service.OneTimeTokenService.Issued;
import com.wikicook.common.security.AccountStatus;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Email verification (FR-005, FR-017; research R6). Confirming marks the account verified in the same transaction that
 * consumes the token, and creates no session. Resending issues a new link, which replaces the older ones, and never
 * touches {@code created_at}, so the 7 days are not extended. It answers the same whether or not the address is
 * registered or already verified; only the email limit can refuse it, and that counts unknown addresses too.
 */
@Service
public class EmailVerificationService {

    private final OneTimeTokenService tokens;
    private final UserAccountRepository users;
    private final EmailRequestLimiter limiter;
    private final EmailHasher hasher;
    private final LinkMailer mailer;
    private final Clock clock;

    public EmailVerificationService(OneTimeTokenService tokens, UserAccountRepository users,
            EmailRequestLimiter limiter, EmailHasher hasher, LinkMailer mailer, Clock clock) {
        this.tokens = tokens;
        this.users = users;
        this.limiter = limiter;
        this.hasher = hasher;
        this.mailer = mailer;
        this.clock = clock;
    }

    /** @throws AuthException 410 {@code TOKEN_EXPIRED} or {@code TOKEN_INVALID} */
    @Transactional(noRollbackFor = AuthException.class)
    public VerificationResult confirm(String token) {
        Consumption consumption = tokens.consume(token, TokenPurpose.EMAIL_VERIFICATION);
        switch (consumption.status()) {
            case CONSUMED -> {
                UserAccount user = users.findById(consumption.userId()).orElseThrow(AuthException::tokenInvalid);
                if (user.isEmailVerified()) {
                    return new VerificationResult(Status.ALREADY_VERIFIED);
                }
                user.setEmailVerifiedAt(clock.instant());
                users.save(user);
                return new VerificationResult(Status.VERIFIED);
            }
            case USED -> {
                boolean verified = users.findById(consumption.userId()).map(UserAccount::isEmailVerified)
                        .orElse(false);
                if (verified) {
                    return new VerificationResult(Status.ALREADY_VERIFIED);
                }
                throw AuthException.tokenInvalid();
            }
            case EXPIRED -> throw AuthException.tokenExpired();
            default -> throw AuthException.tokenInvalid();
        }
    }

    /**
     * Sends a new link to an unverified, active account. Identified by {@code email} (AU03) or by the token of an
     * expired link (AU04).
     *
     * @throws AuthException 429 {@code EMAIL_RATE_LIMITED}
     */
    @Transactional
    public void resend(ResendVerificationRequest request) {
        Optional<UserAccount> user;
        if (request.email() != null) {
            limiter.requireSlot(hasher.hash(request.email()), TokenPurpose.EMAIL_VERIFICATION);
            user = users.findByEmail(request.email());
        } else {
            user = tokens.userOf(request.token(), TokenPurpose.EMAIL_VERIFICATION).flatMap(users::findById);
            if (user.isPresent()) {
                limiter.requireSlot(hasher.hash(user.get().getEmail()), TokenPurpose.EMAIL_VERIFICATION);
            }
        }
        user.filter(account -> !account.isEmailVerified() && account.getStatus() == AccountStatus.ACTIVE)
                .ifPresent(account -> {
                    Issued link = tokens.issue(account.getId(), TokenPurpose.EMAIL_VERIFICATION,
                            RegistrationService.VERIFICATION_TTL);
                    mailer.sendVerification(account.getEmail(), account.getDisplayName(), link.token());
                });
    }
}
