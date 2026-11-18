package com.wikicook.auth.api;

/**
 * Google sign-in takes over an unverified email and password account (FR-019, research R7): everything created before
 * verification must go. Published synchronously, inside the transaction that then deletes the {@code users} row, so a
 * listener of 011, 060 or 080 deletes its own data and files and any exception it throws rolls the whole takeover back
 * (all or nothing, OQ-010-07). Rows that reference {@code users} with {@code ON DELETE CASCADE} need no listener; files
 * and data without such a key do.
 *
 * @param userId the account that is about to be deleted; the new account gets another id
 */
public record AccountResetEvent(long userId) {
}
