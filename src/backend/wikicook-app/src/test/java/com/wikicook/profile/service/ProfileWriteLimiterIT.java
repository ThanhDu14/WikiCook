package com.wikicook.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wikicook.profile.domain.ProfileWriteKind;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/** T012 (NFR-008, research R13): 20 writes per hour per kind, counted in the database. */
@SpringBootTest
@Import(AuthTestSupport.class)
class ProfileWriteLimiterIT extends AbstractPostgresIT {

    @Autowired
    ProfileWriteLimiter limiter;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    AuthTestSupport support;

    private int rows(long user, ProfileWriteKind kind) {
        return jdbc.queryForObject("SELECT count(*) FROM profile_write_requests WHERE user_id = ? AND kind = ?",
                Integer.class, user, kind.name());
    }

    @Test
    @DisplayName("NFR-008: the 21st save in an hour is 429 PROFILE_RATE_LIMITED with retryAfterSeconds")
    void twentyFirstIsRefused() {
        long user = support.member();
        for (int i = 0; i < 20; i++) {
            limiter.acquire(user, ProfileWriteKind.PROFILE_SAVE);
        }

        assertThatThrownBy(() -> limiter.acquire(user, ProfileWriteKind.PROFILE_SAVE))
                .isInstanceOfSatisfying(ProfileException.class, e -> {
                    assertThat(e.code()).isEqualTo(ProfileErrorCode.PROFILE_RATE_LIMITED);
                    assertThat(e.retryAfterSeconds()).isBetween(1, 3600);
                });
        assertThat(rows(user, ProfileWriteKind.PROFILE_SAVE)).isEqualTo(20);
    }

    @Test
    @DisplayName("NFR-008: PROFILE_SAVE and AVATAR_UPLOAD are counted apart")
    void kindsAreCountedSeparately() {
        long user = support.member();
        for (int i = 0; i < 20; i++) {
            limiter.acquire(user, ProfileWriteKind.AVATAR_UPLOAD);
        }

        limiter.acquire(user, ProfileWriteKind.PROFILE_SAVE); // does not throw

        assertThatThrownBy(() -> limiter.acquire(user, ProfileWriteKind.AVATAR_UPLOAD))
                .isInstanceOf(ProfileException.class);
    }

    @Test
    @DisplayName("NFR-008: one user's writes do not use another user's allowance")
    void usersAreCountedSeparately() {
        long busy = support.member();
        long other = support.member();
        for (int i = 0; i < 20; i++) {
            limiter.acquire(busy, ProfileWriteKind.PROFILE_SAVE);
        }

        limiter.acquire(other, ProfileWriteKind.PROFILE_SAVE);

        assertThat(rows(other, ProfileWriteKind.PROFILE_SAVE)).isOne();
    }

    @Test
    @DisplayName("NFR-008: parallel requests never get past 20")
    void parallelRequestsDoNotExceedTheLimit() throws Exception {
        long user = support.member();
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();
        List<Callable<Void>> calls = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            calls.add(() -> {
                try {
                    limiter.acquire(user, ProfileWriteKind.PROFILE_SAVE);
                    accepted.incrementAndGet();
                } catch (ProfileException e) {
                    assertThat(e.code()).isEqualTo(ProfileErrorCode.PROFILE_RATE_LIMITED);
                    refused.incrementAndGet();
                }
                return null;
            });
        }
        try (ExecutorService pool = Executors.newFixedThreadPool(10)) {
            for (Future<Void> done : pool.invokeAll(calls)) {
                done.get();
            }
        }

        assertThat(accepted.get()).isEqualTo(20);
        assertThat(refused.get()).isEqualTo(20);
        assertThat(rows(user, ProfileWriteKind.PROFILE_SAVE)).isEqualTo(20);
    }

    @Test
    @DisplayName("R13: rows older than an hour are deleted while counting, and no longer count")
    void oldRowsAreDeletedAndNotCounted() {
        long user = support.member();
        for (int i = 0; i < 20; i++) {
            jdbc.update("INSERT INTO profile_write_requests (user_id, kind, created_at) VALUES (?, 'PROFILE_SAVE', now() - interval '61 minutes')",
                    user);
        }

        limiter.acquire(user, ProfileWriteKind.PROFILE_SAVE);

        assertThat(rows(user, ProfileWriteKind.PROFILE_SAVE)).isOne();
    }

    @Test
    @DisplayName("R13: the wait is counted from the oldest row still inside the hour")
    void retryAfterComesFromTheOldestRow() {
        long user = support.member();
        jdbc.update("INSERT INTO profile_write_requests (user_id, kind, created_at) VALUES (?, 'PROFILE_SAVE', now() - interval '50 minutes')",
                user);
        for (int i = 0; i < 19; i++) {
            limiter.acquire(user, ProfileWriteKind.PROFILE_SAVE);
        }

        assertThatThrownBy(() -> limiter.acquire(user, ProfileWriteKind.PROFILE_SAVE))
                .isInstanceOfSatisfying(ProfileException.class,
                        e -> assertThat(e.retryAfterSeconds()).isBetween(9 * 60, 10 * 60 + 5));
    }

    @Test
    @DisplayName("R13: the allowance is spent even when the caller's own transaction rolls back")
    void rolledBackTransactionStillCosts() {
        long user = support.member();

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            limiter.acquire(user, ProfileWriteKind.AVATAR_UPLOAD);
            throw new IllegalStateException("the fake avatar was rejected");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(rows(user, ProfileWriteKind.AVATAR_UPLOAD)).isOne();
    }
}
