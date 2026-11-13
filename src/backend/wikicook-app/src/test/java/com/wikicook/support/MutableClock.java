package com.wikicook.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * A clock tests can move (spec 010: "mọi bước dùng Clock chung"). Import it with {@code @Import(MutableClock.class)}:
 * the {@code @Primary} bean replaces the application clock for that context, so locks, sessions and tokens all see the
 * same fake "now". Call {@link #reset()} in {@code @BeforeEach}, because the context is shared by the test class.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MutableClock extends Clock {

    // microseconds, like the TIMESTAMPTZ columns: a time stored and read back compares equal
    private volatile Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Bean
    @Primary
    MutableClock mutableClock() {
        return this;
    }

    public void reset() {
        now = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public void set(Instant instant) {
        now = instant;
    }

    public void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now;
    }
}
