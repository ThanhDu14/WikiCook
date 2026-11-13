package com.wikicook.auth.security;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Caps concurrent BCrypt work at {@code max(2, cores - 1)} (research R14). BCrypt is CPU-bound and requests run on
 * virtual threads, so without a cap one burst of sign-ins could take every carrier thread from the rest of the API.
 * Wrap {@code encode}, {@code matches} and the dummy hash of the unknown-email path in {@link #run}.
 */
@Component
public class BcryptBulkhead {

    private final int permits;
    private final Semaphore semaphore;

    public BcryptBulkhead() {
        this(Math.max(2, Runtime.getRuntime().availableProcessors() - 1));
    }

    public BcryptBulkhead(int permits) {
        this.permits = permits;
        this.semaphore = new Semaphore(permits);
    }

    public <T> T run(Supplier<T> work) {
        semaphore.acquireUninterruptibly();
        try {
            return work.get();
        } finally {
            semaphore.release();
        }
    }

    public int permits() {
        return permits;
    }

    public int availablePermits() {
        return semaphore.availablePermits();
    }
}
