package com.wikicook.auth.service;

import com.wikicook.auth.domain.UserAccount;
import com.wikicook.auth.dto.CurrentAccountDto;
import com.wikicook.auth.dto.SessionSnapshot;
import com.wikicook.auth.repository.UserAccountRepository;
import com.wikicook.auth.repository.UserSessionRepository;
import com.wikicook.auth.service.SessionService.RefreshResult;
import com.wikicook.auth.service.SessionService.Status;
import com.wikicook.common.security.AccountStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What the endpoints around a session need besides sign-in: the refresh with its account and token, and the current
 * account (FR-009, FR-024, FR-024a). Not transactional itself: {@link SessionService#refresh} commits first, so a
 * reuse that revoked a session stays revoked even though the answer is a 401.
 */
@Service
public class AccountSessionService {

    private final SessionService sessions;
    private final UserSessionRepository sessionRepository;
    private final UserAccountRepository users;
    private final AccessTokenService tokens;

    public AccountSessionService(SessionService sessions, UserSessionRepository sessionRepository,
            UserAccountRepository users, AccessTokenService tokens) {
        this.sessions = sessions;
        this.sessionRepository = sessionRepository;
        this.users = users;
        this.tokens = tokens;
    }

    /**
     * @throws AuthException 401 {@code UNAUTHENTICATED} for a missing, unknown, expired, revoked or reused cookie; 403
     *     {@code ACCOUNT_SUSPENDED} or {@code ACCOUNT_BANNED} when the account may no longer use the API
     */
    public Refreshed refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw AuthException.unauthenticated();
        }
        RefreshResult result = sessions.refresh(refreshToken);
        if (result.status() == Status.REJECTED) {
            throw AuthException.unauthenticated();
        }
        long sessionId = result.session().getId();
        SessionSnapshot snapshot = sessionRepository.findSnapshot(sessionId)
                .orElseThrow(AuthException::unauthenticated);
        if (snapshot.status() != AccountStatus.ACTIVE) {
            throw AuthException.restricted(snapshot);
        }
        UserAccount user = users.findById(snapshot.userId()).orElseThrow(AuthException::unauthenticated);
        return new Refreshed(user, result, tokens.issue(user.getId(), sessionId));
    }

    /** Clears the pending role-change notice of the user (FR-024a); nothing happens when there is none. */
    @Transactional
    public void acknowledgeRoleNotice(long userId) {
        users.findById(userId).filter(user -> user.getPendingRoleNoticeFrom() != null).ifPresent(user -> {
            user.setPendingRoleNoticeFrom(null);
            users.save(user);
        });
    }

    public CurrentAccountDto currentAccount(long userId) {
        return users.findById(userId).map(CurrentAccountDto::of).orElseThrow(AuthException::unauthenticated);
    }

    public record Refreshed(UserAccount account, RefreshResult result, AccessTokenService.IssuedToken accessToken) {
    }
}
