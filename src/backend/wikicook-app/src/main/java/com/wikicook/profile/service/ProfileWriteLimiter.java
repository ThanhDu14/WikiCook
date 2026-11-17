package com.wikicook.profile.service;

import com.wikicook.profile.config.ProfileProperties;
import com.wikicook.profile.domain.ProfileWriteKind;
import com.wikicook.profile.repository.ProfileWriteRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * NFR-008: at most {@code wikicook.profile.write-limit-per-hour} writes per hour, per person and per
 * {@link ProfileWriteKind} (research R13). It runs in a transaction of its own, before the image is decoded, so a
 * request refused for a fake avatar still spends its turn even though the caller's transaction rolls back. It only
 * orders the commands of {@link ProfileWriteRequestRepository}; the SQL lives there.
 */
@Service
public class ProfileWriteLimiter {

    private final ProfileWriteRequestRepository requests;
    private final ProfileProperties properties;

    public ProfileWriteLimiter(ProfileWriteRequestRepository requests, ProfileProperties properties) {
        this.requests = requests;
        this.properties = properties;
    }

    /**
     * Counts one write of {@code userId}.
     *
     * @throws ProfileException {@code PROFILE_RATE_LIMITED} with the wait in seconds when the allowance is spent
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ProfileException.class)
    public void acquire(long userId, ProfileWriteKind kind) {
        requests.lock(userId);
        requests.deleteExpired(userId);
        if (requests.count(userId, kind) >= properties.writeLimitPerHour()) {
            throw ProfileException.rateLimited(requests.secondsUntilOldestExpires(userId, kind));
        }
        requests.insert(userId, kind);
    }
}
