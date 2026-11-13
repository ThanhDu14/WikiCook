package com.wikicook.common.security;

/** {@code users.status}: only ACTIVE accounts may use the API (FR-013, FR-024). */
public enum AccountStatus {
    ACTIVE,
    SUSPENDED,
    BANNED
}
