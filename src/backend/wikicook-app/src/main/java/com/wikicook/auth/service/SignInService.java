package com.wikicook.auth.service;

import com.wikicook.auth.api.AuditWriter;
import com.wikicook.auth.domain.UserAccount;
import com.wikicook.auth.domain.UserSuspension;
import com.wikicook.auth.repository.UserSuspensionRepository;
import com.wikicook.auth.security.BcryptBulkhead;
import com.wikicook.auth.service.SessionService.NewSession;
import com.wikicook.auth.service.SignInThrottle.Attempt;
import com.wikicook.auth.service.SignInThrottle.Failure;
import com.wikicook.common.security.AccountStatus;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Email and password sign-in (FR-007, FR-008, FR-012, FR-013; research R4). Deliberately <b>not</b> transactional: the
 * BCrypt comparison takes 100-300 ms and must not hold a database connection. Each database step is its own short
 * transaction in {@link SignInThrottle} and {@link SessionService}.
 *
 * <p>An email without an account and an account without a password are compared against {@code dummyHash}, made at
 * startup with the same cost, so the answer takes the same time as a real wrong password (NFR-003). The status of the
 * account (suspended, banned) is looked at only <em>after</em> the password was right, so it is not revealed to someone
 * who does not know it (FR-013). Google is never called here (NFR-005).
 */
@Service
public class SignInService {

    private final SignInThrottle throttle;
    private final PasswordEncoder encoder;
    private final BcryptBulkhead bulkhead;
    private final SessionService sessions;
    private final AccessTokenService tokens;
    private final AuditWriter audit;
    private final EmailHasher hasher;
    private final UserSuspensionRepository suspensions;
    private final String dummyHash;

    public SignInService(SignInThrottle throttle, PasswordEncoder encoder, BcryptBulkhead bulkhead,
            SessionService sessions, AccessTokenService tokens, AuditWriter audit, EmailHasher hasher,
            UserSuspensionRepository suspensions) {
        this.throttle = throttle;
        this.encoder = encoder;
        this.bulkhead = bulkhead;
        this.sessions = sessions;
        this.tokens = tokens;
        this.audit = audit;
        this.hasher = hasher;
        this.suspensions = suspensions;
        byte[] random = new byte[16];
        new SecureRandom().nextBytes(random);
        this.dummyHash = encoder.encode(HexFormat.of().formatHex(random));
    }

    public SignInResult signIn(SignInCommand command) {
        String email = EmailNormalizer.normalize(command.email());
        String emailHash = hasher.hash(email);

        Attempt attempt = begin(emailHash, email, command.ip());
        UserAccount user = attempt.user();
        String storedHash = user == null ? null : user.getPasswordHash();

        // one comparison on every path; a missing account or password compares against the dummy and never succeeds
        String compareWith = storedHash != null ? storedHash : dummyHash;
        boolean matches = bulkhead.run(() -> encoder.matches(command.password(), compareWith));
        if (!matches || storedHash == null) {
            throw failed(attempt, emailHash);
        }

        boolean active = user.getStatus() == AccountStatus.ACTIVE;
        throttle.recordCorrectPassword(attempt, active);
        if (!active) {
            audit.write("SIGN_IN_FAILED", null, "USER", user.getId(), Map.of("reason", user.getStatus().name()));
            throw restricted(user);
        }

        NewSession session = sessions.create(user.getId(), command.rememberMe(), command.userAgent());
        AccessTokenService.IssuedToken accessToken = tokens.issue(user.getId(), session.session().getId());
        audit.write("SIGN_IN_SUCCEEDED", user.getId(), "USER", user.getId(),
                Map.of("sessionId", session.session().getId()));
        return new SignInResult(user, session, accessToken);
    }

    private Attempt begin(String emailHash, String email, InetAddress ip) {
        try {
            return throttle.begin(emailHash, email, ip);
        } catch (AuthException e) {
            if (e.code() == AuthErrorCode.ACCOUNT_LOCKED) {
                audit.write("SIGN_IN_FAILED", null, "USER", null, failureDetails("LOCKED", emailHash));
            }
            throw e;
        }
    }

    /** Records the wrong password and returns the error to throw: 423 when this one locked the account, else 401. */
    private AuthException failed(Attempt attempt, String emailHash) {
        Failure failure = throttle.recordFailure(attempt, emailHash);
        UserAccount user = attempt.user();
        // an unknown email is audited by its hash only, never in readable form (FR-025)
        Map<String, Object> details = user == null ? failureDetails("BAD_CREDENTIALS", emailHash)
                : Map.of("reason", "BAD_CREDENTIALS");
        audit.write("SIGN_IN_FAILED", null, "USER", user == null ? null : user.getId(), details);
        if (failure.lockedUntil() == null) {
            return AuthException.invalidCredentials();
        }
        if (failure.newlyLocked()) {
            audit.write("ACCOUNT_LOCKED", null, "USER", user == null ? null : user.getId(),
                    user == null ? failureDetails("TOO_MANY_FAILURES", emailHash) : Map.of("reason", "TOO_MANY_FAILURES"));
        }
        return AuthException.accountLocked(failure.lockedUntil());
    }

    private AuthException restricted(UserAccount user) {
        UserSuspension sanction = suspensions.findRunning(user.getId()).orElse(null);
        String reason = sanction == null ? null : sanction.getReason();
        return user.getStatus() == AccountStatus.BANNED
                ? AuthException.accountBanned(reason, null)
                : AuthException.accountSuspended(reason, sanction == null ? null : sanction.getEndsAt());
    }

    private static Map<String, Object> failureDetails(String reason, String emailHash) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("reason", reason);
        details.put("emailHash", emailHash);
        return details;
    }

    public record SignInCommand(String email, String password, boolean rememberMe, InetAddress ip,
            String userAgent) {
    }

    public record SignInResult(UserAccount account, NewSession session, AccessTokenService.IssuedToken accessToken) {
    }
}
