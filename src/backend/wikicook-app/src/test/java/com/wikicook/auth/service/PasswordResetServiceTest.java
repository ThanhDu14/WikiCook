package com.wikicook.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import com.wikicook.auth.service.OneTimeTokenService.Status;
import com.wikicook.common.security.AccountStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FR-014, FR-015, FR-016, FR-004, NFR-003: the password reset decisions with the collaborators mocked. The background
 * task of "forgot" runs on the calling thread here ({@code Runnable::run}); the IT proves the real thread.
 */
class PasswordResetServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-17T03:00:00Z");
    private static final String EMAIL = "an@mail.com";
    private static final String HASH = "hash-of-an";

    private final OneTimeTokenService tokens = mock(OneTimeTokenService.class);
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final EmailRequestLimiter limiter = mock(EmailRequestLimiter.class);
    private final LinkMailer mailer = mock(LinkMailer.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final SessionService sessions = mock(SessionService.class);
    private final AuditWriter audit = mock(AuditWriter.class);
    private final EmailHasher hasher = new EmailHasher("unit-test-hash-key-0123456789");
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        when(encoder.encode(anyString())).thenReturn("new-hash");
        when(sessions.revokeAll(anyLong())).thenReturn(2);
        service = new PasswordResetService(tokens, users, limiter, hasher, mailer, encoder, new BcryptBulkhead(2),
                sessions, audit, new TransactionTemplate(mock(PlatformTransactionManager.class)), Runnable::run);
    }

    private static UserAccount account(String passwordHash, AccountStatus status) {
        UserAccount user = new UserAccount(EMAIL, passwordHash, "An", NOW);
        ReflectionTestUtils.setField(user, "id", 7L);
        user.setStatus(status);
        return user;
    }

    private void registered(UserAccount user) {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
    }

    private void issuesToken() {
        when(tokens.issue(anyLong(), eq(TokenPurpose.PASSWORD_RESET), any()))
                .thenReturn(new Issued("raw-token", NOW.plus(Duration.ofMinutes(30))));
    }

    // ---- forgot ----------------------------------------------------------------------------------------------

    @Test
    @DisplayName("FR-014/FR-015: an active account with a password gets a link that lives 30 minutes")
    void activeAccountGetsAThirtyMinuteLink() {
        registered(account(HASH, AccountStatus.ACTIVE));
        issuesToken();

        service.requestReset(EMAIL);

        verify(tokens).issue(7L, TokenPurpose.PASSWORD_RESET, Duration.ofMinutes(30));
        verify(mailer).sendPasswordReset(EMAIL, "An", "raw-token");
        assertThat(PasswordResetService.RESET_TTL).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("FR-017: the limit is counted on the address hash first, for registered and unknown addresses alike")
    void limitIsTakenBeforeTheAccountIsLookedAt() {
        service.requestReset("nobody@mail.com");

        verify(limiter).requireSlot(hasher.hash("nobody@mail.com"), TokenPurpose.PASSWORD_RESET);
    }

    @Test
    @DisplayName("FR-017: when the limit refuses, nothing is looked up, issued or sent")
    void refusedByTheLimitDoesNothingElse() {
        doThrow(AuthException.emailRateLimited(600)).when(limiter).requireSlot(anyString(), any());

        assertThatThrownBy(() -> service.requestReset(EMAIL)).isInstanceOfSatisfying(AuthException.class,
                e -> assertThat(e.code()).isEqualTo(AuthErrorCode.EMAIL_RATE_LIMITED));

        verifyNoInteractions(users, tokens, mailer);
    }

    @Test
    @DisplayName("FR-014/NFR-003: unknown, Google-only, suspended and banned all answer without error and get no link")
    void accountsThatMustNotGetALinkAnswerTheSameAndGetNone() {
        assertThatCode(() -> service.requestReset("nobody@mail.com")).doesNotThrowAnyException();
        when(users.findByEmail("nobody@mail.com")).thenReturn(Optional.empty());

        for (UserAccount user : new UserAccount[] {account(null, AccountStatus.ACTIVE),
                account(HASH, AccountStatus.SUSPENDED), account(HASH, AccountStatus.BANNED)}) {
            registered(user);
            assertThatCode(() -> service.requestReset(EMAIL)).doesNotThrowAnyException();
        }

        verify(tokens, never()).issue(anyLong(), any(), any());
        verify(mailer, never()).sendPasswordReset(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("FR-012: a locked account still gets a link, because reset does not unlock it")
    void lockedAccountStillGetsALink() {
        UserAccount locked = account(HASH, AccountStatus.ACTIVE);
        locked.setLockedUntil(NOW.plusSeconds(600));
        registered(locked);
        issuesToken();

        service.requestReset(EMAIL);

        verify(mailer).sendPasswordReset(EMAIL, "An", "raw-token");
    }

    @Test
    @DisplayName("Edge: a failure in the background task never reaches the caller, who has already got 202")
    void failureInTheBackgroundIsSwallowed() {
        registered(account(HASH, AccountStatus.ACTIVE));
        issuesToken();
        doThrow(new IllegalStateException("smtp down")).when(mailer).sendPasswordReset(anyString(), anyString(),
                anyString());

        assertThatCode(() -> service.requestReset(EMAIL)).doesNotThrowAnyException();
    }

    // ---- validate --------------------------------------------------------------------------------------------

    @Test
    @DisplayName("AU06: a live link answers with the account email and its expiry, and is not used up")
    void validateShowsTheEmailAndDoesNotConsume() {
        Instant expires = NOW.plus(Duration.ofMinutes(20));
        when(tokens.inspect("tok", TokenPurpose.PASSWORD_RESET)).thenReturn(new Inspection(Status.VALID, 7L, expires));
        registered(account(HASH, AccountStatus.ACTIVE));

        ResetTokenInfo info = service.validate("tok");

        assertThat(info.email()).isEqualTo(EMAIL);
        assertThat(info.expiresAt()).isEqualTo(expires);
        verify(tokens, never()).consume(anyString(), any());
    }

    @Test
    @DisplayName("FR-015: expired is TOKEN_EXPIRED; used, replaced and unknown are TOKEN_INVALID")
    void validateSeparatesExpiredFromInvalid() {
        when(tokens.inspect("old", TokenPurpose.PASSWORD_RESET)).thenReturn(new Inspection(Status.EXPIRED, 7L, NOW));
        when(tokens.inspect("used", TokenPurpose.PASSWORD_RESET)).thenReturn(new Inspection(Status.USED, 7L, NOW));
        when(tokens.inspect("nope", TokenPurpose.PASSWORD_RESET)).thenReturn(new Inspection(Status.UNKNOWN, null, null));

        assertThat(codeOf(() -> service.validate("old"))).isEqualTo(AuthErrorCode.TOKEN_EXPIRED);
        assertThat(codeOf(() -> service.validate("used"))).isEqualTo(AuthErrorCode.TOKEN_INVALID);
        assertThat(codeOf(() -> service.validate("nope"))).isEqualTo(AuthErrorCode.TOKEN_INVALID);
    }

    @Test
    @DisplayName("FR-014: a link issued before the account was suspended or banned is no longer valid")
    void linkOfASanctionedAccountIsInvalid() {
        when(tokens.inspect("tok", TokenPurpose.PASSWORD_RESET)).thenReturn(
                new Inspection(Status.VALID, 7L, NOW.plusSeconds(60)));
        registered(account(HASH, AccountStatus.BANNED));

        assertThat(codeOf(() -> service.validate("tok"))).isEqualTo(AuthErrorCode.TOKEN_INVALID);
    }

    // ---- reset -----------------------------------------------------------------------------------------------

    @Test
    @DisplayName("FR-015/FR-016: one transaction changes the hash, uses the token, ends every session, writes the audit")
    void resetDoesEverythingTogether() {
        UserAccount user = account(HASH, AccountStatus.ACTIVE);
        registered(user);
        when(tokens.inspect("tok", TokenPurpose.PASSWORD_RESET)).thenReturn(
                new Inspection(Status.VALID, 7L, NOW.plusSeconds(60)));
        when(tokens.consume("tok", TokenPurpose.PASSWORD_RESET)).thenReturn(new Consumption(Status.CONSUMED, 7L));

        service.reset("tok", "newpass123");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(users).save(user);
        verify(sessions).revokeAll(7L);
        ArgumentCaptor<Map<String, Object>> details = ArgumentCaptor.captor();
        verify(audit).write(eq("PASSWORD_RESET"), eq(7L), eq("USER"), eq(7L), details.capture());
        assertThat(details.getValue()).containsEntry("sessionsRevoked", 2).doesNotContainKeys("email", "password");
    }

    @Test
    @DisplayName("R6/FR-012: reset neither marks the email verified nor lifts a lock")
    void resetLeavesVerificationAndLockAlone() {
        UserAccount user = account(HASH, AccountStatus.ACTIVE);
        Instant lockedUntil = NOW.plusSeconds(900);
        user.setLockedUntil(lockedUntil);
        registered(user);
        when(tokens.inspect("tok", TokenPurpose.PASSWORD_RESET)).thenReturn(
                new Inspection(Status.VALID, 7L, NOW.plusSeconds(60)));
        when(tokens.consume("tok", TokenPurpose.PASSWORD_RESET)).thenReturn(new Consumption(Status.CONSUMED, 7L));

        service.reset("tok", "newpass123");

        assertThat(user.getEmailVerifiedAt()).isNull();
        assertThat(user.getLockedUntil()).isEqualTo(lockedUntil);
    }

    @Test
    @DisplayName("FR-004/AF 3.5: a password equal to the email (any case) is 400 PASSWORD_EQUALS_EMAIL and uses nothing up")
    void passwordEqualToTheEmailIsRefusedBeforeTheTokenIsUsed() {
        registered(account(HASH, AccountStatus.ACTIVE));
        when(tokens.inspect("tok", TokenPurpose.PASSWORD_RESET)).thenReturn(
                new Inspection(Status.VALID, 7L, NOW.plusSeconds(60)));

        assertThatThrownBy(() -> service.reset("tok", " An@Mail.com ")).isInstanceOfSatisfying(AuthException.class,
                e -> {
                    assertThat(e.code()).isEqualTo(AuthErrorCode.VALIDATION_ERROR);
                    assertThat(e.properties()).containsKey("errors");
                    assertThat(e.properties().get("errors").toString()).contains("newPassword")
                            .contains("PASSWORD_EQUALS_EMAIL");
                });

        verify(tokens, never()).consume(anyString(), any());
        verify(users, never()).save(any());
        verify(sessions, never()).revokeAll(anyLong());
        verify(audit, never()).write(anyString(), any(), anyString(), any(), anyMap());
    }

    @Test
    @DisplayName("US4-3: an expired or used link is 410 and nothing is hashed, saved or revoked")
    void deadLinkChangesNothing() {
        when(tokens.inspect("old", TokenPurpose.PASSWORD_RESET)).thenReturn(new Inspection(Status.EXPIRED, 7L, NOW));
        when(tokens.inspect("used", TokenPurpose.PASSWORD_RESET)).thenReturn(new Inspection(Status.USED, 7L, NOW));

        assertThat(codeOf(() -> service.reset("old", "newpass123"))).isEqualTo(AuthErrorCode.TOKEN_EXPIRED);
        assertThat(codeOf(() -> service.reset("used", "newpass123"))).isEqualTo(AuthErrorCode.TOKEN_INVALID);

        verifyNoInteractions(encoder, sessions, audit);
        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("FR-015: when another request used the link between the check and the change, this one is TOKEN_INVALID")
    void losingTheRaceForTheTokenChangesNothing() {
        UserAccount user = account(HASH, AccountStatus.ACTIVE);
        registered(user);
        when(tokens.inspect("tok", TokenPurpose.PASSWORD_RESET)).thenReturn(
                new Inspection(Status.VALID, 7L, NOW.plusSeconds(60)));
        when(tokens.consume("tok", TokenPurpose.PASSWORD_RESET)).thenReturn(new Consumption(Status.USED, 7L));

        assertThat(codeOf(() -> service.reset("tok", "newpass123"))).isEqualTo(AuthErrorCode.TOKEN_INVALID);

        assertThat(user.getPasswordHash()).isEqualTo(HASH);
        verify(users, never()).save(any());
        verify(sessions, never()).revokeAll(anyLong());
        verify(audit, never()).write(anyString(), any(), anyString(), any(), anyMap());
    }

    private static AuthErrorCode codeOf(Runnable action) {
        try {
            action.run();
        } catch (AuthException e) {
            return e.code();
        }
        throw new AssertionError("expected an AuthException");
    }
}
