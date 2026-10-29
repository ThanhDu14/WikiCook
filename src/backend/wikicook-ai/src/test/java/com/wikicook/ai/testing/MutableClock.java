package com.wikicook.ai.testing;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock tests can move; always UTC, callers convert to Vietnam time themselves. */
public final class MutableClock extends Clock {

    private volatile Instant now;

    public MutableClock(Instant now) {
        this.now = now;
    }

    public void set(Instant now) {
        this.now = now;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(now, zone);
    }

    @Override
    public Instant instant() {
        return now;
    }
}
