package com.wikicook.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailHasherTest {

    private final EmailHasher hasher = new EmailHasher("hash-key-one-0123456789");

    @Test
    void isA64CharacterLowerCaseHexString() {
        assertThat(hasher.hash("an@mail.com")).matches("[0-9a-f]{64}");
    }

    @Test
    void isStable() {
        assertThat(hasher.hash("an@mail.com")).isEqualTo(hasher.hash("an@mail.com"));
        assertThat(hasher.hash("an@mail.com")).isNotEqualTo(hasher.hash("binh@mail.com"));
    }

    @Test
    void edgeEmailWithSpacesOrMixedCaseHashesLikeTheNormalizedOne() {
        // Edge: Email with leading/trailing spaces or mixed case, FR-002
        assertThat(hasher.hash(" An@Mail.com ")).isEqualTo(hasher.hash("an@mail.com"));
    }

    @Test
    void differentKeyGivesDifferentHash() {
        EmailHasher other = new EmailHasher("hash-key-two-0123456789");

        assertThat(other.hash("an@mail.com")).isNotEqualTo(hasher.hash("an@mail.com"));
    }

    @Test
    void doesNotContainTheEmail() {
        assertThat(hasher.hash("an@mail.com")).doesNotContain("an@mail").doesNotContain("mail.com");
    }
}
