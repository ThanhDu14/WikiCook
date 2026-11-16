package com.wikicook.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;
import org.junit.jupiter.api.Test;

/** R3: BCrypt of 6.5.11 throws above 72 bytes, so the password is NFC-normalized and pre-hashed first. */
class PrehashedBCryptPasswordEncoderTest {

    /** Cost 4 keeps the suite fast; the default constructor is checked separately. */
    private final PrehashedBCryptPasswordEncoder encoder = new PrehashedBCryptPasswordEncoder(4);

    @Test
    void longPasswordWithMultiByteCharactersEncodesWithoutError() {
        // FR-004, NFR-001: 25 x "ế" is 75 bytes, 128 emoji is 512 bytes; both are valid and must not give a 500
        String vietnamese = "ế".repeat(25);
        String emoji = "😀".repeat(128);

        assertThat(encoder.matches(vietnamese, encoder.encode(vietnamese))).isTrue();
        assertThat(encoder.matches(emoji, encoder.encode(emoji))).isTrue();
    }

    @Test
    void everyCharacterCounts_passwordsDifferingOnlyAfter72BytesDoNotMatch() {
        String base = "a1".repeat(49);

        assertThat(encoder.matches(base + "y", encoder.encode(base + "x"))).isFalse();
    }

    @Test
    void passwordsDifferingOnlyAtCharacter100DoNotMatch() {
        String first = "a".repeat(99) + "1" + "tail";
        String second = "a".repeat(99) + "2" + "tail";

        assertThat(encoder.matches(second, encoder.encode(first))).isFalse();
        assertThat(encoder.matches(first, encoder.encode(first))).isTrue();
    }

    @Test
    void nfcAndNfdFormsOfTheSamePasswordMatch() {
        String composed = "mật khẩu 123";
        String decomposed = Normalizer.normalize(composed, Normalizer.Form.NFD);
        assertThat(decomposed).isNotEqualTo(composed);

        assertThat(encoder.matches(decomposed, encoder.encode(composed))).isTrue();
        assertThat(encoder.matches(composed, encoder.encode(decomposed))).isTrue();
    }

    @Test
    void resultIsA60CharacterBCryptString() {
        assertThat(encoder.encode("abc12345")).hasSize(60).startsWith("$2a$04$");
    }

    @Test
    void defaultCostIs12() {
        assertThat(new PrehashedBCryptPasswordEncoder().encode("abc12345")).hasSize(60).startsWith("$2a$12$");
    }

    @Test
    void sameInputGivesDifferentSalts() {
        assertThat(encoder.encode("abc12345")).isNotEqualTo(encoder.encode("abc12345"));
    }

    @Test
    void edgePasswordWithLeadingAndTrailingSpacesIsKeptExactlyAsTyped() {
        // Edge: Password with leading/trailing spaces
        String hash = encoder.encode("abc12345");

        assertThat(encoder.matches(" abc12345 ", hash)).isFalse();
        assertThat(encoder.matches("abc12345", encoder.encode(" abc12345 "))).isFalse();
        assertThat(encoder.matches(" abc12345 ", encoder.encode(" abc12345 "))).isTrue();
    }
}
