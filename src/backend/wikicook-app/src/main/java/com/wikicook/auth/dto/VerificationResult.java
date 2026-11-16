package com.wikicook.auth.dto;

/** {@code VERIFIED} now, or {@code ALREADY_VERIFIED} when the link was used before for a verified account. */
public record VerificationResult(Status status) {

    public enum Status {
        VERIFIED,
        ALREADY_VERIFIED
    }
}
