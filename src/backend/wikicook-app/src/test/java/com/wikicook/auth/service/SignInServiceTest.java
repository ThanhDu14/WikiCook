package com.wikicook.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.wikicook.auth.api.AuditWriter;
import com.wikicook.auth.domain.SanctionKind;
import com.wikicook.auth.domain.UserAccount;
import com.wikicook.auth.domain.UserSession;
import com.wikicook.auth.domain.UserSuspension;
import com.wikicook.auth.repository.UserSuspensionRepository;
import com.wikicook.auth.security.BcryptBulkhead;
import com.wikicook.auth.service.SessionService.NewSession;
import com.wikicook.auth.service.SignInService.SignInCommand;
import com.wikicook.auth.service.SignInService.SignInResult;
import com.wikicook.auth.service.SignInThrottle.Attempt;
import com.wikicook.auth.service.SignInThrottle.Failure;
import com.wikicook.common.security.AccountStatus;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** FR-007, FR-008, FR-012, FR-013, NFR-003: the sign-in decision with its collaborators mocked. */
class SignInServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-13T03:00:00Z");
    private static final String DUMMY = "dummy-hash";

    private final SignInThrottle throttle = mock(SignInThrottle.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final SessionService sessions = mock(SessionService.class);
    private final AccessTokenService tokens = mock(AccessTokenService.class);
    private final AuditWriter audit = mock(AuditWriter.class);
    private final UserSuspensionRepository suspensions = mock(UserSuspensionRepository.class);
    private final ClosureTokenService closureTokens = mock(ClosureTokenService.class);
    private final EmailHasher hasher = new EmailHasher("unit-test-hash-key-0123456789");
    private SignInService service;

    @BeforeEach
    void setUp() {
        when(encoder.encode(anyString())).thenReturn(DUMMY);
        service = new SignInService(throttle, encoder, new BcryptBulkhead(2), sessions, tokens, audit, hasher,
                suspensions, closureTokens);
    }

    private static SignInCommand command(String email, String password) {
        return new SignInCommand(email, password, false, null, "JUnit");
    }

    private static UserAccount account(String passwordHash, AccountStatus status) {
        UserAccount user = new UserAccount("an@mail.com", passwordHash, "An", NOW);
        ReflectionTestUtils.setField(user, "id", 7L);
        user.setStatus(status);
        user.setEmailVerifiedAt(NOW);
        return user;
    }

    private Attempt attemptFor(UserAccount user) {
        Attempt attempt = new Attempt(1L, user);
        when(throttle.begin(anyString(), anyString(), any())).thenReturn(attempt);
        return attempt;
    }

    @Test
    void unknownEmailComparesAgainstTheDummyHashExactlyOnceAndFailsGenerically() {
        Attempt attempt = attemptFor(null);
        when(throttle.recordFailure(eq(attempt), anyString())).thenReturn(new Failure(null, false));

        assertThatThrownBy(() -> service.signIn(command("Nobody@Mail.com", "pw")))
                .isInstanceOfSatisfying(AuthException.class, e -> assertThat(e.code())
                        .isEqualTo(AuthErrorCode.INVALID_CREDENTIALS));

        verify(encoder, times(1)).matches("pw", DUMMY);
        verify(encoder, times(1)).matches(anyString(), anyString());
    }

    @Test
    void accountWithoutPasswordComparesAgainstTheDummyHashExactlyOnceAndNeverSucceeds() {
        Attempt attempt = attemptFor(account(null, AccountStatus.ACTIVE));
        when(encoder.matches(anyString(), eq(DUMMY))).thenReturn(true); // even a "match" must not sign in
        when(throttle.recordFailure(eq(attempt), anyString())).thenReturn(new Failure(null, false));

        assertThatThrownBy(() -> service.signIn(command("an@mail.com", "pw")))
                .isInstanceOfSatisfying(AuthException.class, e -> assertThat(e.code())
                        .isEqualTo(AuthErrorCode.INVALID_CREDENTIALS));

        verify(encoder, times(1)).matches(anyString(), anyString());
        verify(throttle, never()).recordCorrectPassword(any(), anyBoolean());
    }

    @Test
    @SuppressWarnings("unchecked")
    void failedSignInOfAnUnknownEmailIsAuditedWithoutTargetAndWithTheEmailHash() {
        Attempt attempt = attemptFor(null);
        when(throttle.recordFailure(eq(attempt), anyString())).thenReturn(new Failure(null, false));

        assertThatThrownBy(() -> service.signIn(command(" Nobody@Mail.com ", "pw"))).isInstanceOf(AuthException.class);

        ArgumentCaptor<Map<String, Object>> details = ArgumentCaptor.forClass(Map.class);
        verify(audit).write(eq("SIGN_IN_FAILED"), isNull(), eq("USER"), isNull(), details.capture());
        assertThat(details.getValue()).containsEntry("reason", "BAD_CREDENTIALS")
                .containsEntry("emailHash", hasher.hash("nobody@mail.com"));
        assertThat(details.getValue().values()).doesNotContain("nobody@mail.com");
    }

    @Test
    @SuppressWarnings("unchecked")
    void failedSignInOfARegisteredAccountTargetsTheUserAndOmitsTheEmailHash() {
        Attempt attempt = attemptFor(account("hash", AccountStatus.ACTIVE));
        when(throttle.recordFailure(eq(attempt), anyString())).thenReturn(new Failure(null, false));

        assertThatThrownBy(() -> service.signIn(command("an@mail.com", "wrong"))).isInstanceOf(AuthException.class);

        ArgumentCaptor<Map<String, Object>> details = ArgumentCaptor.forClass(Map.class);
        verify(audit).write(eq("SIGN_IN_FAILED"), isNull(), eq("USER"), eq(7L), details.capture());
        assertThat(details.getValue()).containsEntry("reason", "BAD_CREDENTIALS").doesNotContainKey("emailHash");
    }

    @Test
    void accountStatusIsNotRevealedWhenThePasswordIsWrong() {
        Attempt attempt = attemptFor(account("hash", AccountStatus.SUSPENDED));
        when(encoder.matches("wrong", "hash")).thenReturn(false);
        when(throttle.recordFailure(eq(attempt), anyString())).thenReturn(new Failure(null, false));

        assertThatThrownBy(() -> service.signIn(command("an@mail.com", "wrong")))
                .isInstanceOfSatisfying(AuthException.class, e -> assertThat(e.code())
                        .isEqualTo(AuthErrorCode.INVALID_CREDENTIALS));

        verifyNoInteractions(suspensions);
    }

    @Test
    void suspendedAccountWithTheRightPasswordIsRefusedWithReasonAndEndDate() {
        Attempt attempt = attemptFor(account("hash", AccountStatus.SUSPENDED));
        when(encoder.matches("right", "hash")).thenReturn(true);
        Instant endsAt = NOW.plusSeconds(3600);
        when(suspensions.findRunning(7L)).thenReturn(Optional.of(
                new UserSuspension(7L, SanctionKind.SUSPENSION, "Spam", NOW, endsAt, 1L)));

        assertThatThrownBy(() -> service.signIn(command("an@mail.com", "right")))
                .isInstanceOfSatisfying(AuthException.class, e -> {
                    assertThat(e.code()).isEqualTo(AuthErrorCode.ACCOUNT_SUSPENDED);
                    assertThat(e.properties()).containsEntry("reason", "Spam").containsEntry("endsAt", endsAt);
                });

        verify(throttle).recordCorrectPassword(attempt, false);
        verify(audit).write(eq("SIGN_IN_FAILED"), isNull(), eq("USER"), eq(7L),
                org.mockito.ArgumentMatchers.argThat(m -> "SUSPENDED".equals(m.get("reason"))));
        verifyNoInteractions(sessions);
    }

    @Test
    void bannedAccountWithTheRightPasswordIsRefusedWithoutAnEndDate() {
        attemptFor(account("hash", AccountStatus.BANNED));
        when(encoder.matches("right", "hash")).thenReturn(true);
        when(suspensions.findRunning(7L)).thenReturn(Optional.of(
                new UserSuspension(7L, SanctionKind.BAN, "Fraud", NOW, null, 1L)));
        when(closureTokens.issue(7L)).thenReturn("closure-token");

        assertThatThrownBy(() -> service.signIn(command("an@mail.com", "right")))
                .isInstanceOfSatisfying(AuthException.class, e -> {
                    assertThat(e.code()).isEqualTo(AuthErrorCode.ACCOUNT_BANNED);
                    assertThat(e.properties()).containsEntry("reason", "Fraud").containsEntry("closureToken", "closure-token")
                            .doesNotContainKey("endsAt");
                });
    }

    @Test
    void theFifthWrongPasswordIsAnAccountLockedWithRetryAtAndIsAudited() {
        Attempt attempt = attemptFor(account("hash", AccountStatus.ACTIVE));
        when(throttle.recordFailure(eq(attempt), anyString())).thenReturn(new Failure(NOW.plusSeconds(900), true));

        assertThatThrownBy(() -> service.signIn(command("an@mail.com", "wrong")))
                .isInstanceOfSatisfying(AuthException.class, e -> {
                    assertThat(e.code()).isEqualTo(AuthErrorCode.ACCOUNT_LOCKED);
                    assertThat(e.properties()).containsEntry("retryAt", NOW.plusSeconds(900));
                });

        verify(audit).write(eq("ACCOUNT_LOCKED"), isNull(), eq("USER"), eq(7L), anyMap());
    }

    @Test
    void aRejectionByTheLockIsAuditedAsLockedAndNothingIsCompared() {
        when(throttle.begin(anyString(), anyString(), any()))
                .thenThrow(AuthException.accountLocked(NOW.plusSeconds(600)));

        assertThatThrownBy(() -> service.signIn(command("an@mail.com", "pw")))
                .isInstanceOfSatisfying(AuthException.class, e -> assertThat(e.code())
                        .isEqualTo(AuthErrorCode.ACCOUNT_LOCKED));

        verifyNoInteractions(sessions);
        verify(encoder, never()).matches(anyString(), anyString());
        verify(audit).write(eq("SIGN_IN_FAILED"), isNull(), eq("USER"), isNull(),
                org.mockito.ArgumentMatchers.argThat(m -> "LOCKED".equals(m.get("reason"))));
    }

    @Test
    void theIpLimitIsNotAudited() {
        when(throttle.begin(anyString(), anyString(), any())).thenThrow(AuthException.tooManyRequests(60));

        assertThatThrownBy(() -> service.signIn(command("an@mail.com", "pw")))
                .isInstanceOfSatisfying(AuthException.class, e -> assertThat(e.code())
                        .isEqualTo(AuthErrorCode.TOO_MANY_REQUESTS));

        verifyNoInteractions(audit);
    }

    @Test
    void rightPasswordOfAnActiveAccountCreatesASessionAndAnAccessToken() {
        UserAccount user = account("hash", AccountStatus.ACTIVE);
        Attempt attempt = attemptFor(user);
        when(encoder.matches("right", "hash")).thenReturn(true);
        UserSession session = new UserSession(7L, "h", true, NOW, NOW.plusSeconds(60));
        ReflectionTestUtils.setField(session, "id", 55L);
        when(sessions.create(7L, true, "JUnit")).thenReturn(new NewSession(session, "refresh-token"));
        when(tokens.issue(7L, 55L)).thenReturn(new AccessTokenService.IssuedToken("jwt", 900));

        SignInResult result = service.signIn(new SignInCommand("an@mail.com", "right", true, null, "JUnit"));

        assertThat(result.account()).isSameAs(user);
        assertThat(result.session().refreshToken()).isEqualTo("refresh-token");
        assertThat(result.accessToken().value()).isEqualTo("jwt");
        verify(throttle).recordCorrectPassword(attempt, true);
        verify(audit).write(eq("SIGN_IN_SUCCEEDED"), eq(7L), eq("USER"), eq(7L), anyMap());
        verify(throttle, never()).recordFailure(any(), anyString());
        verify(audit, never()).write(eq("SIGN_IN_FAILED"), any(), anyString(), anyLong(), anyMap());
    }

    @Test
    void theEmailIsNormalizedBeforeItIsHashedAndLookedUp() {
        Attempt attempt = attemptFor(null);
        when(throttle.recordFailure(eq(attempt), anyString())).thenReturn(new Failure(null, false));

        assertThatThrownBy(() -> service.signIn(command("  An@Mail.COM ", "pw"))).isInstanceOf(AuthException.class);

        verify(throttle).begin(eq(hasher.hash("an@mail.com")), eq("an@mail.com"), any());
    }
}
