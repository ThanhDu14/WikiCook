package com.wikicook.auth.api;

import java.util.OptionalLong;

/**
 * The closure link of a banned user (spec 010 FR-013, research R16). A banned user has no session, so the only way to
 * reach the account-closure page of 011 is a single-use token, valid for 10 minutes, that 010 hands out after the
 * password (or Google) was right. The token opens that one action and nothing else: it is not a bearer for any API.
 */
public interface ClosureTokens {

    /**
     * The user the token was issued to, if it is valid; it is used up at once. Runs in the transaction of the caller and
     * never starts its own ({@code MANDATORY}, not {@code REQUIRES_NEW}): when 011 rolls back, the token can be used
     * again (011 FR-037b, US4 AS9). Called outside a transaction it fails with an exception, so a token can never be
     * spent without the action it allows.
     */
    OptionalLong consume(String token);
}
