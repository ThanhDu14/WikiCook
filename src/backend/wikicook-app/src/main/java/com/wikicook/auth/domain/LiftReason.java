package com.wikicook.auth.domain;

/** How a sanction ended: ran out, lifted early by an Administrator, or closed because a ban replaced it. */
public enum LiftReason {
    EXPIRED,
    LIFTED,
    REPLACED_BY_BAN
}
