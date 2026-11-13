package com.wikicook.auth.domain;

/** {@code user_suspensions.kind}: a suspension always has an end date, a ban never has one. */
public enum SanctionKind {
    SUSPENSION,
    BAN
}
