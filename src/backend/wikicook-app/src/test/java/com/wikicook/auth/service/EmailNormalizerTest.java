package com.wikicook.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class EmailNormalizerTest {

    @Test
    void edgeEmailWithLeadingTrailingSpacesOrMixedCaseIsTrimmedAndLowercased() {
        // Edge: Email with leading/trailing spaces or mixed case, FR-002
        assertThat(EmailNormalizer.normalize(" An@Mail.com ")).isEqualTo("an@mail.com");
        assertThat(EmailNormalizer.normalize("\tUSER@Example.COM\n")).isEqualTo("user@example.com");
    }

    @Test
    void alreadyNormalizedEmailIsUnchanged() {
        assertThat(EmailNormalizer.normalize("an@mail.com")).isEqualTo("an@mail.com");
    }

    @Test
    void lowercasingDoesNotDependOnTheDefaultLocale() {
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            assertThat(EmailNormalizer.normalize("INFO@MAIL.COM")).isEqualTo("info@mail.com");
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void nullStaysNull() {
        assertThat(EmailNormalizer.normalize(null)).isNull();
    }
}
