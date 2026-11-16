package com.wikicook.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.auth.domain.TokenPurpose;
import com.wikicook.auth.service.OneTimeTokenService.Consumption;
import com.wikicook.auth.service.OneTimeTokenService.Issued;
import com.wikicook.auth.service.OneTimeTokenService.Status;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.MutableClock;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CountDownLatch;
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
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * FR-005, FR-015, research R6: a link token is 32 random bytes, only its SHA-256 is stored, it is consumed atomically
 * exactly once, it expires, and a newer token of the same user and purpose replaces the older ones.
 */
@SpringBootTest
@Import({AuthTestSupport.class, MutableClock.class})
class OneTimeTokenServiceIT extends AbstractPostgresIT {

    private static final Duration DAY = Duration.ofHours(24);

    @Autowired
    OneTimeTokenService tokens;

    @Autowired
    AuthTestSupport support;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MutableClock clock;

    private long user;

    @BeforeEach
    void setUp() {
        clock.reset();
        user = support.member();
    }

    @Test
    @DisplayName("FR-005: the token is 32 random bytes, the database has only its SHA-256, and the expiry is as asked")
    void tokenIs32BytesAndOnlyTheHashIsStored() {
        Issued issued = tokens.issue(user, TokenPurpose.EMAIL_VERIFICATION, DAY);

        assertThat(Base64.getUrlDecoder().decode(issued.token())).hasSize(32);
        assertThat(issued.expiresAt()).isEqualTo(clock.instant().plus(DAY));
        List<String> stored = jdbc.queryForList("SELECT token_hash FROM one_time_tokens WHERE user_id = ?",
                String.class, user);
        assertThat(stored).containsExactly(SessionService.hash(issued.token()));
        assertThat(stored.get(0)).doesNotContain(issued.token());
        assertThat(tokens.issue(user, TokenPurpose.EMAIL_VERIFICATION, DAY).token()).isNotEqualTo(issued.token());
    }

    @Test
    @DisplayName("FR-005: a valid token is consumed once; the second use says it was used")
    void consumeWorksOnce() {
        Issued issued = tokens.issue(user, TokenPurpose.EMAIL_VERIFICATION, DAY);

        Consumption first = tokens.consume(issued.token(), TokenPurpose.EMAIL_VERIFICATION);
        Consumption second = tokens.consume(issued.token(), TokenPurpose.EMAIL_VERIFICATION);

        assertThat(first.status()).isEqualTo(Status.CONSUMED);
        assertThat(first.userId()).isEqualTo(user);
        assertThat(second.status()).isEqualTo(Status.USED);
        assertThat(second.userId()).isEqualTo(user);
    }

    @Test
    @DisplayName("FR-005: two threads using the same token, exactly one wins")
    void consumeIsAtomic() throws Exception {
        Issued issued = tokens.issue(user, TokenPurpose.EMAIL_VERIFICATION, DAY);
        CountDownLatch start = new CountDownLatch(1);
        List<Status> results = new ArrayList<>();

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Status>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return tokens.consume(issued.token(), TokenPurpose.EMAIL_VERIFICATION).status();
                }));
            }
            start.countDown();
            for (Future<Status> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
        }

        assertThat(results.stream().filter(s -> s == Status.CONSUMED)).hasSize(1);
        assertThat(results.stream().filter(s -> s == Status.USED)).hasSize(1);
    }

    @Test
    @DisplayName("US2-6: an expired token cannot be used and is reported as expired, with its user")
    void expiredTokenIsRefused() {
        Issued issued = tokens.issue(user, TokenPurpose.EMAIL_VERIFICATION, DAY);
        clock.advance(DAY.plusSeconds(1));

        Consumption consumption = tokens.consume(issued.token(), TokenPurpose.EMAIL_VERIFICATION);

        assertThat(consumption.status()).isEqualTo(Status.EXPIRED);
        assertThat(consumption.userId()).isEqualTo(user);
        assertThat(jdbc.queryForObject("SELECT used_at FROM one_time_tokens WHERE token_hash = ?", Timestamp.class,
                SessionService.hash(issued.token()))).as("an expired token is not marked used").isNull();
    }

    @Test
    @DisplayName("FR-005: one second before the expiry the token still works")
    void tokenWorksUntilItExpires() {
        Issued issued = tokens.issue(user, TokenPurpose.EMAIL_VERIFICATION, DAY);
        clock.advance(DAY.minusSeconds(1));

        assertThat(tokens.consume(issued.token(), TokenPurpose.EMAIL_VERIFICATION).status())
                .isEqualTo(Status.CONSUMED);
    }

    @Test
    @DisplayName("TOKEN_INVALID: a token that was never issued is unknown")
    void unknownTokenIsUnknown() {
        Consumption consumption = tokens.consume("never-issued", TokenPurpose.EMAIL_VERIFICATION);

        assertThat(consumption.status()).isEqualTo(Status.UNKNOWN);
        assertThat(consumption.userId()).isNull();
    }

    @Test
    @DisplayName("FR-005, FR-015: a new token invalidates the older unused ones of the same user and purpose")
    void newTokenReplacesTheOlderOnes() {
        Issued first = tokens.issue(user, TokenPurpose.EMAIL_VERIFICATION, DAY);
        Issued second = tokens.issue(user, TokenPurpose.EMAIL_VERIFICATION, DAY);

        assertThat(tokens.consume(first.token(), TokenPurpose.EMAIL_VERIFICATION).status()).isEqualTo(Status.USED);
        assertThat(tokens.consume(second.token(), TokenPurpose.EMAIL_VERIFICATION).status())
                .isEqualTo(Status.CONSUMED);
    }

    @Test
    @DisplayName("FR-015: a token of another purpose or another user is untouched, and cannot be used for the wrong purpose")
    void purposeAndUserAreIsolated() {
        long other = support.member();
        Issued reset = tokens.issue(user, TokenPurpose.PASSWORD_RESET, Duration.ofMinutes(30));
        Issued otherUsers = tokens.issue(other, TokenPurpose.EMAIL_VERIFICATION, DAY);
        Issued verification = tokens.issue(user, TokenPurpose.EMAIL_VERIFICATION, DAY);

        assertThat(tokens.consume(verification.token(), TokenPurpose.PASSWORD_RESET).status())
                .as("a verification link is not a reset link").isEqualTo(Status.UNKNOWN);
        assertThat(tokens.consume(reset.token(), TokenPurpose.PASSWORD_RESET).status()).isEqualTo(Status.CONSUMED);
        assertThat(tokens.consume(otherUsers.token(), TokenPurpose.EMAIL_VERIFICATION).status())
                .isEqualTo(Status.CONSUMED);
    }

    @Test
    @DisplayName("Lookup: the user of a token is found whatever its state, for resending an expired link")
    void userOfATokenIsFoundInAnyState() {
        Issued issued = tokens.issue(user, TokenPurpose.EMAIL_VERIFICATION, DAY);
        clock.advance(DAY.plusSeconds(5));

        assertThat(tokens.userOf(issued.token(), TokenPurpose.EMAIL_VERIFICATION)).contains(user);
        assertThat(tokens.userOf("never-issued", TokenPurpose.EMAIL_VERIFICATION)).isEmpty();
        Instant expires = jdbc.queryForObject("SELECT expires_at FROM one_time_tokens WHERE token_hash = ?",
                Timestamp.class, SessionService.hash(issued.token())).toInstant();
        assertThat(expires).isBefore(clock.instant());
    }
}
