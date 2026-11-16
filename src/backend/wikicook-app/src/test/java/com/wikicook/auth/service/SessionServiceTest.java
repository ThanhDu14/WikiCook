package com.wikicook.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wikicook.auth.api.AuditWriter;
import com.wikicook.auth.domain.UserSession;
import com.wikicook.auth.repository.UserSessionRepository;
import com.wikicook.auth.service.SessionService.NewSession;
import com.wikicook.auth.service.SessionService.RefreshResult;
import com.wikicook.auth.service.SessionService.Status;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/** FR-009, FR-010, research R2: session lifetime, token storage, rotation, the 10 second grace and reuse detection. */
class SessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-13T03:00:00Z");

    private final UserSessionRepository sessions = mock(UserSessionRepository.class);
    private final AuditWriter audit = mock(AuditWriter.class);
    private final MutableTestClock clock = new MutableTestClock(NOW);
    private final SessionService service = new SessionService(sessions, audit, clock);

    @BeforeEach
    void saveReturnsItsArgument() {
        when(sessions.save(any(UserSession.class))).thenAnswer(call -> call.getArgument(0));
    }

    /** A clock the test can move, local to this unit test. */
    static final class MutableTestClock extends java.time.Clock {
        private Instant now;

        MutableTestClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public java.time.ZoneId getZone() {
            return java.time.ZoneOffset.UTC;
        }

        @Override
        public java.time.Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private UserSession saved() {
        ArgumentCaptor<UserSession> captor = ArgumentCaptor.forClass(UserSession.class);
        verify(sessions).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void rememberMeSessionLastsThirtyDaysFromCreation() {
        NewSession created = service.create(7L, true, "JUnit");

        UserSession session = created.session();
        assertThat(session.isRememberMe()).isTrue();
        assertThat(session.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
        assertThat(session.getUserAgent()).isEqualTo("JUnit");
        assertThat(saved()).isSameAs(session);
    }

    @Test
    void sessionWithoutRememberMeSlidesFromLastActivityByTwentyFourHours() {
        NewSession created = service.create(7L, false, null);

        assertThat(created.session().isRememberMe()).isFalse();
        assertThat(created.session().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
        assertThat(created.session().getLastActivityAt()).isEqualTo(NOW);
    }

    @Test
    void refreshTokenIs256BitsAndOnlyItsSha256IsStored() {
        NewSession created = service.create(7L, true, null);

        byte[] raw = Base64.getUrlDecoder().decode(created.refreshToken());
        assertThat(raw).hasSize(32);
        assertThat(created.session().getTokenHash()).isEqualTo(SessionService.hash(created.refreshToken()))
                .matches("[0-9a-f]{64}").isNotEqualTo(created.refreshToken());
        assertThat(service.create(7L, true, null).refreshToken()).isNotEqualTo(created.refreshToken());
    }

    @Test
    void userAgentIsTruncatedTo255Characters() {
        assertThat(service.create(7L, true, "x".repeat(400)).session().getUserAgent()).hasSize(255);
    }

    private UserSession existing(boolean rememberMe) {
        UserSession session = new UserSession(7L, SessionService.hash("old-token"), rememberMe, NOW,
                NOW.plus(Duration.ofDays(30)));
        ReflectionTestUtils.setField(session, "id", 99L);
        when(sessions.lockByTokenHash(SessionService.hash("old-token"))).thenReturn(Optional.of(session));
        return session;
    }

    @Test
    void refreshRotatesTheTokenAndKeepsTheOldHashAsPrevious() {
        UserSession session = existing(true);
        String oldHash = session.getTokenHash();
        clock.advance(Duration.ofMinutes(20));

        RefreshResult result = service.refresh("old-token");

        assertThat(result.status()).isEqualTo(Status.ROTATED);
        assertThat(result.newRefreshToken()).isNotNull().isNotEqualTo("old-token");
        assertThat(session.getTokenHash()).isEqualTo(SessionService.hash(result.newRefreshToken()));
        assertThat(session.getPreviousTokenHash()).isEqualTo(oldHash);
        assertThat(session.getRotatedAt()).isEqualTo(NOW.plus(Duration.ofMinutes(20)));
    }

    @Test
    void refreshSlidesTheExpiryOfASessionWithoutRememberMeButNotTheRememberMeOne() {
        UserSession sliding = existing(false);
        ReflectionTestUtils.setField(sliding, "expiresAt", NOW.plus(Duration.ofHours(24)));
        clock.advance(Duration.ofHours(10));

        service.refresh("old-token");

        assertThat(sliding.getLastActivityAt()).isEqualTo(NOW.plus(Duration.ofHours(10)));
        assertThat(sliding.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(34)));
    }

    @Test
    void slidingExpiryIsCappedThirtyDaysAfterCreation() {
        UserSession sliding = existing(false);
        clock.advance(Duration.ofDays(29).plusHours(20));
        ReflectionTestUtils.setField(sliding, "expiresAt", clock.instant().plusSeconds(60));

        service.refresh("old-token");

        assertThat(sliding.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
    }

    @Test
    void rememberMeExpiryIsNeverExtended() {
        UserSession fixed = existing(true);
        Instant expires = fixed.getExpiresAt();
        clock.advance(Duration.ofDays(10));

        service.refresh("old-token");

        assertThat(fixed.getExpiresAt()).isEqualTo(expires);
    }

    @Test
    void expiredOrRevokedSessionIsRejected() {
        UserSession expired = existing(false);
        ReflectionTestUtils.setField(expired, "expiresAt", NOW.minusSeconds(1));
        assertThat(service.refresh("old-token").status()).isEqualTo(Status.REJECTED);

        UserSession revoked = existing(true);
        revoked.revoke(NOW);
        assertThat(service.refresh("old-token").status()).isEqualTo(Status.REJECTED);
    }

    @Test
    void unknownTokenIsRejected() {
        when(sessions.lockByTokenHash(any())).thenReturn(Optional.empty());
        when(sessions.lockByPreviousTokenHash(any())).thenReturn(Optional.empty());

        assertThat(service.refresh("nobody").status()).isEqualTo(Status.REJECTED);
    }

    private UserSession rotatedSession() {
        UserSession session = new UserSession(7L, SessionService.hash("new-token"), true, NOW,
                NOW.plus(Duration.ofDays(30)));
        ReflectionTestUtils.setField(session, "id", 99L);
        session.rotate(SessionService.hash("new-token"), NOW); // previous = hash of the token being replayed
        ReflectionTestUtils.setField(session, "previousTokenHash", SessionService.hash("old-token"));
        when(sessions.lockByTokenHash(SessionService.hash("old-token"))).thenReturn(Optional.empty());
        when(sessions.lockByPreviousTokenHash(SessionService.hash("old-token"))).thenReturn(Optional.of(session));
        return session;
    }

    @Test
    void replayingTheOldTokenWithinTenSecondsGivesTheSessionWithoutANewCookie() {
        UserSession session = rotatedSession();
        clock.advance(Duration.ofSeconds(9));

        RefreshResult result = service.refresh("old-token");

        assertThat(result.status()).isEqualTo(Status.GRACE_REPLAY);
        assertThat(result.session()).isSameAs(session);
        assertThat(result.newRefreshToken()).isNull();
        assertThat(session.getTokenHash()).isEqualTo(SessionService.hash("new-token"));
        assertThat(session.getRevokedAt()).isNull();
    }

    @Test
    void replayingTheOldTokenAfterTenSecondsRevokesTheSessionAndAudits() {
        UserSession session = rotatedSession();
        clock.advance(Duration.ofSeconds(11));

        RefreshResult result = service.refresh("old-token");

        assertThat(result.status()).isEqualTo(Status.REJECTED);
        assertThat(session.getRevokedAt()).isEqualTo(clock.instant());
        verify(audit).write(eq("REFRESH_TOKEN_REUSED"), eq(7L), eq("USER"), eq(7L), anyMap());
    }

    @Test
    void logoutRevokesTheSessionAndAuditsSignedOut() {
        UserSession session = existing(true);

        Optional<Long> userId = service.logout("old-token");

        assertThat(userId).contains(7L);
        assertThat(session.getRevokedAt()).isEqualTo(NOW);
        verify(audit).write(eq("SIGNED_OUT"), eq(7L), eq("USER"), eq(7L), anyMap());
    }

    @Test
    void logoutOfAnUnknownOrAlreadyEndedSessionIsQuiet() {
        when(sessions.lockByTokenHash(any())).thenReturn(Optional.empty());
        when(sessions.lockByPreviousTokenHash(any())).thenReturn(Optional.empty());
        assertThat(service.logout("nobody")).isEmpty();

        UserSession revoked = existing(true);
        revoked.revoke(NOW.minusSeconds(5));
        assertThat(service.logout("old-token")).isEmpty();
        assertThat(revoked.getRevokedAt()).isEqualTo(NOW.minusSeconds(5));
    }

    @Test
    void revokeAllEndsEveryOpenSessionOfTheUser() {
        when(sessions.revokeAllOf(7L, NOW)).thenReturn(3);

        assertThat(service.revokeAll(7L)).isEqualTo(3);
    }
}
