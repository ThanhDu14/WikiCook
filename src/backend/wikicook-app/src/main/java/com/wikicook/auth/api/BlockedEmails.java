package com.wikicook.auth.api;

/**
 * The Blocked Email list (FR-026): addresses of banned accounts that may not register again. Only the HMAC of the
 * normalized address is stored. Offered to 011 (a banned user deleting their account) and to registration.
 */
public interface BlockedEmails {

    boolean isBlocked(String email);

    /** Adds the address; blocking one that is already blocked changes nothing. */
    void block(String email);
}
