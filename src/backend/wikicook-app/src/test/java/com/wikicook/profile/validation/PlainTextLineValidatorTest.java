package com.wikicook.profile.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.auth.validation.MaxCodePoints;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** T011, Edge: Display name: one line of plain text, counted in code points. */
class PlainTextLineValidatorTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    record Name(@NotBlank @MaxCodePoints(50) @PlainTextLine String value) {
    }

    private static Set<ConstraintViolation<Name>> check(String value) {
        return VALIDATOR.validate(new Name(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Hiền", "Nguyễn Thị Bích Ngọc", "Chef 👩‍🍳 Lan", "O'Brien-Smith", "Bếp Việt 2026", "a"})
    @DisplayName("Edge: Display name - ordinary names, emoji and ZWJ sequences are accepted")
    void acceptsPlainText(String value) {
        assertThat(check(value)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Hi\nthere", "Hi\tthere", "Hi\rthere", "Hi\u0000", "Hi\u0007", "Hi\u007F", "Hi\u0085there",
            "Hi there", "Hi there", "Hi‮evil", "Hi​zero", "﻿Hi"})
    @DisplayName("Edge: Display name - control, line separator and invisible format characters are rejected")
    void rejectsControlCharacters(String value) {
        assertThat(check(value)).extracting(ConstraintViolation::getMessage).contains("DISPLAY_NAME_INVALID");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "     ", " ", "　"})
    @DisplayName("Edge: Display name - empty or whitespace only is rejected")
    void rejectsBlank(String value) {
        assertThat(check(value)).isNotEmpty();
    }

    @Test
    @DisplayName("null is left to @NotBlank, the validator does not add a second message")
    void nullIsNotThePlainTextValidatorsJob() {
        assertThat(check(null)).extracting(ConstraintViolation::getMessage).doesNotContain("DISPLAY_NAME_INVALID");
    }

    @Test
    @DisplayName("Edge: Display name - emoji count as one character: 50 pass, 51 fail")
    void countsCodePointsNotUtf16Units() {
        assertThat(check("😀".repeat(50))).isEmpty();
        assertThat(check("😀".repeat(51))).extracting(ConstraintViolation::getMessage).contains("TOO_LONG");
    }
}
