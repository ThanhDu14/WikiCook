package com.wikicook.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** FR-018, AF 4.8 (T140): the display name of an account made by Google sign-in. */
class GoogleSignInServiceTest {

    @Test
    @DisplayName("the Google name is trimmed")
    void trimmed() {
        assertThat(GoogleSignInService.displayName("  Lê Thị Bếp \t", "a@b.c")).isEqualTo("Lê Thị Bếp");
    }

    @Test
    @DisplayName("a name over 50 code points is cut at 50 code points, not in the middle of an emoji")
    void cutAtFiftyCodePoints() {
        String cut = GoogleSignInService.displayName("😀".repeat(60), "a@b.c");

        assertThat(cut.codePointCount(0, cut.length())).isEqualTo(50);
        assertThat(cut).isEqualTo("😀".repeat(50));
    }

    @Test
    @DisplayName("a space left at the cut is stripped again")
    void stripsAfterCut() {
        String name = "a".repeat(49) + " bbbb";

        assertThat(GoogleSignInService.displayName(name, "x@y.z")).isEqualTo("a".repeat(49));
    }

    @Test
    @DisplayName("no name, or only blanks: the part of the email before the @")
    void emailLocalPart() {
        assertThat(GoogleSignInService.displayName(null, "nguyen.van.a@gmail.com")).isEqualTo("nguyen.van.a");
        assertThat(GoogleSignInService.displayName("   ", "bep@x.vn")).isEqualTo("bep");
    }

    @Test
    @DisplayName("a long local part is cut too; nothing usable at all falls back to a neutral name")
    void longLocalPartAndFallback() {
        String longLocal = "b".repeat(70) + "@x.vn";

        assertThat(GoogleSignInService.displayName("", longLocal)).hasSize(50);
        assertThat(GoogleSignInService.displayName("", "@x.vn")).isEqualTo("@x.vn");
        assertThat(GoogleSignInService.displayName("", " ")).isEqualTo("Thành viên");
    }
}
