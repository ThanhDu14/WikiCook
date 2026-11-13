package com.wikicook.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.auth.domain.TokenPurpose;
import com.wikicook.auth.service.EmailRequestLimiter.Slot;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.MutableClock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * FR-017, research R5: three verification and three reset emails per address per 15 minutes, counted per address (not
 * per account), so an unknown address is limited exactly like a registered one and a 429 reveals nothing.
 */
@SpringBootTest
@Import({AuthTestSupport.class, MutableClock.class})
class EmailRequestLimiterIT extends AbstractPostgresIT {

    @Autowired
    EmailRequestLimiter limiter;

    @Autowired
    EmailHasher hasher;

    @Autowired
    AuthTestSupport support;

    @Autowired
    MutableClock clock;

    private String hash;

    @BeforeEach
    void setUp() {
        clock.reset();
        support.resetThrottle();
        hash = hasher.hash(AuthTestSupport.uniqueEmail()); // no account behind it: it is counted all the same
    }

    @Test
    @DisplayName("FR-017: the first three requests pass and the fourth is refused with the wait in seconds")
    void fourthRequestInFifteenMinutesIsRefused() {
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire(hash, TokenPurpose.EMAIL_VERIFICATION).allowed()).isTrue();
            clock.advance(Duration.ofMinutes(1));
        }

        Slot fourth = limiter.tryAcquire(hash, TokenPurpose.EMAIL_VERIFICATION);

        assertThat(fourth.allowed()).isFalse();
        // the oldest request was 3 minutes ago, so 12 minutes until it leaves the window
        assertThat(fourth.retryAfterSeconds()).isEqualTo(Duration.ofMinutes(12).toSeconds());
    }

    @Test
    @DisplayName("FR-017: requireSlot throws EMAIL_RATE_LIMITED with retryAfterSeconds for the resend endpoint")
    void requireSlotThrowsOnTheFourth() {
        for (int i = 0; i < 3; i++) {
            limiter.requireSlot(hash, TokenPurpose.EMAIL_VERIFICATION);
        }

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> limiter.requireSlot(hash, TokenPurpose.EMAIL_VERIFICATION))
                .isInstanceOfSatisfying(AuthException.class, e -> {
                    assertThat(e.code()).isEqualTo(AuthErrorCode.EMAIL_RATE_LIMITED);
                    assertThat(e.retryAfterSeconds()).isEqualTo(Duration.ofMinutes(15).toSeconds());
                });
    }

    @Test
    @DisplayName("FR-017: verification and reset are counted separately")
    void purposesAreCountedSeparately() {
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire(hash, TokenPurpose.EMAIL_VERIFICATION).allowed()).isTrue();
        }

        assertThat(limiter.tryAcquire(hash, TokenPurpose.EMAIL_VERIFICATION).allowed()).isFalse();
        assertThat(limiter.tryAcquire(hash, TokenPurpose.PASSWORD_RESET).allowed()).isTrue();
    }

    @Test
    @DisplayName("FR-017: another address is not affected")
    void otherAddressesAreNotAffected() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire(hash, TokenPurpose.EMAIL_VERIFICATION);
        }

        assertThat(limiter.tryAcquire(hasher.hash(AuthTestSupport.uniqueEmail()), TokenPurpose.EMAIL_VERIFICATION)
                .allowed()).isTrue();
    }

    @Test
    @DisplayName("FR-017: the window slides, a refused request is not counted and a slot frees after 15 minutes")
    void windowSlidesAndRefusedRequestsAreNotCounted() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire(hash, TokenPurpose.EMAIL_VERIFICATION);
        }
        clock.advance(Duration.ofMinutes(14));
        assertThat(limiter.tryAcquire(hash, TokenPurpose.EMAIL_VERIFICATION).allowed()).isFalse();
        assertThat(limiter.tryAcquire(hash, TokenPurpose.EMAIL_VERIFICATION).allowed()).isFalse();

        clock.advance(Duration.ofMinutes(1).plusSeconds(1));

        assertThat(limiter.tryAcquire(hash, TokenPurpose.EMAIL_VERIFICATION).allowed()).isTrue();
    }

    @Test
    @DisplayName("FR-017: only the two email purposes are counted, anything else is a programming error")
    void onlyEmailPurposesAreCounted() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> limiter.tryAcquire(hash, TokenPurpose.ACCOUNT_CLOSURE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("FR-017: eight parallel requests still let only three through")
    void parallelRequestsNeverExceedThree() throws Exception {
        List<Boolean> allowed = new ArrayList<>();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                futures.add(pool.submit(() -> limiter.tryAcquire(hash, TokenPurpose.EMAIL_VERIFICATION).allowed()));
            }
            for (Future<Boolean> future : futures) {
                allowed.add(future.get(30, TimeUnit.SECONDS));
            }
        }

        assertThat(allowed.stream().filter(a -> a)).hasSize(3);
    }
}
