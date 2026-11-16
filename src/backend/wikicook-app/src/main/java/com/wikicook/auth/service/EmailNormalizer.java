package com.wikicook.auth.service;

import java.util.Locale;

/** FR-002: an email is compared and stored as {@code lower(trim(email))}, the same rule as the DB CHECK. */
public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    public static String normalize(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
