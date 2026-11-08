package com.wikicook.search.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** 020 FR-006, NFR-004, research R3. */
class KeywordTest {

    @Test
    void trimsAndSplitsIntoLowerCaseTokens() {
        Keyword k = Keyword.of("  Canh  CHUA, cá!  ");

        assertThat(k.text()).isEqualTo("Canh  CHUA, cá!");
        assertThat(k.tokens()).containsExactly("canh", "chua", "cá");
        assertThat(k.isBlank()).isFalse();
        assertThat(k.truncated()).isFalse();
    }

    @Test
    void joinsTokensWithOrForTsQuery() {
        assertThat(Keyword.of("canh chua cá").toTsQuery()).isEqualTo("canh | chua | cá");
    }

    @Test
    void normalizesDecomposedVietnameseToNfcSoMarksDoNotSplitWords() {
        String decomposed = Normalizer.normalize("Phở bò", Normalizer.Form.NFD);

        assertThat(Keyword.of(decomposed).tokens()).containsExactly("phở", "bò");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "%*<>\"'&|!:()", "\t\n"})
    void blankOrSymbolOnlyIsBrowseAll(String input) {
        Keyword k = Keyword.of(input);

        assertThat(k.isBlank()).isTrue();
        assertThat(k.tokens()).isEmpty();
    }

    @Test
    void symbolsNeverReachTheTsQuery() {
        Keyword k = Keyword.of("gà & !bò :* 'x' <script>");

        assertThat(k.toTsQuery()).isEqualTo("gà | bò | x | script");
    }

    @Test
    void echoedTextKeepsSymbolsForDisplay() {
        assertThat(Keyword.of(" <b>gà</b> ").text()).isEqualTo("<b>gà</b>");
    }

    @Test
    void cutsAt100CharactersAndFlagsIt() {
        String longInput = "a".repeat(120);

        Keyword k = Keyword.of(longInput);

        assertThat(k.text()).hasSize(100);
        assertThat(k.truncated()).isTrue();
    }

    @Test
    void keepsAtMostTenTokens() {
        Keyword k = Keyword.of("a b c d e f g h i j k l");

        assertThat(k.tokens()).hasSize(10).endsWith("j");
    }

    @Test
    void digitsAreTokens() {
        assertThat(Keyword.of("bún 2 người").tokens()).containsExactly("bún", "2", "người");
    }
}
