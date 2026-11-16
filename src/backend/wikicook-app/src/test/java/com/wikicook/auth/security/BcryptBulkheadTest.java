package com.wikicook.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** R14: BCrypt is CPU work on virtual threads, so at most max(2, cores - 1) run at once. */
class BcryptBulkheadTest {

    @Test
    void defaultLimitIsMaxOfTwoAndCoresMinusOne() {
        int expected = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);

        assertThat(new BcryptBulkhead().permits()).isEqualTo(expected);
    }

    @Test
    void neverRunsMoreThanThePermitsAtTheSameTime() throws Exception {
        BcryptBulkhead bulkhead = new BcryptBulkhead(2);
        AtomicInteger running = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();
        CountDownLatch twoInside = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                futures.add(pool.submit(() -> bulkhead.run(() -> {
                    peak.accumulateAndGet(running.incrementAndGet(), Math::max);
                    twoInside.countDown();
                    try {
                        release.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    running.decrementAndGet();
                    return "done";
                })));
            }
            assertThat(twoInside.await(5, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(150); // give the other four a chance to get in (wrongly)
            assertThat(running.get()).isEqualTo(2);
            release.countDown();
            for (Future<String> future : futures) {
                assertThat(future.get(5, TimeUnit.SECONDS)).isEqualTo("done");
            }
        }

        assertThat(peak.get()).isEqualTo(2);
    }

    @Test
    void permitIsReturnedWhenTheWorkThrows() {
        BcryptBulkhead bulkhead = new BcryptBulkhead(1);

        assertThatThrownBy(() -> bulkhead.run(() -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(bulkhead.availablePermits()).isEqualTo(1);
        assertThat(bulkhead.run(() -> "next")).isEqualTo("next");
    }
}
