package com.wikicook.auth.service;

import com.wikicook.auth.api.ClosureTokens;
import com.wikicook.auth.domain.TokenPurpose;
import com.wikicook.auth.service.OneTimeTokenService.Consumption;
import com.wikicook.auth.service.OneTimeTokenService.Status;
import java.time.Duration;
import java.util.OptionalLong;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ClosureTokens} on {@code one_time_tokens} with purpose {@code ACCOUNT_CLOSURE} (research R16): 32 random
 * bytes, only the SHA-256 stored, 10 minutes, one use, and a newer token replaces an older one of the same user.
 */
@Service
public class ClosureTokenService implements ClosureTokens {

    public static final Duration TTL = Duration.ofMinutes(10);

    private final OneTimeTokenService tokens;

    public ClosureTokenService(OneTimeTokenService tokens) {
        this.tokens = tokens;
    }

    /** The token for a banned user whose password (or Google) was just proven; joins the caller's transaction if any. */
    public String issue(long userId) {
        return tokens.issue(userId, TokenPurpose.ACCOUNT_CLOSURE, TTL).token();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public OptionalLong consume(String token) {
        if (token == null || token.isBlank()) {
            return OptionalLong.empty();
        }
        Consumption consumption = tokens.consume(token, TokenPurpose.ACCOUNT_CLOSURE);
        return consumption.status() == Status.CONSUMED ? OptionalLong.of(consumption.userId()) : OptionalLong.empty();
    }
}
