package com.wikicook.search.domain;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The search keyword (020 FR-006, research R3). {@link #text()} is what the user typed (trimmed, at most
 * {@value #MAX_LENGTH} characters) and is only ever echoed back as text; {@link #tokens()} contain letters and digits
 * only, so {@link #toTsQuery()} can never carry tsquery operators. Accents are removed in SQL with
 * {@code f_unaccent}, the same function that builds the document.
 *
 * @param text      trimmed keyword, "" when blank
 * @param tokens    lower-case words, at most {@value #MAX_TOKENS}
 * @param truncated the input was longer than {@value #MAX_LENGTH} characters
 */
public record Keyword(String text, List<String> tokens, boolean truncated) {

    public static final int MAX_LENGTH = 100;
    public static final int MAX_TOKENS = 10;
    private static final Pattern NOT_LETTER_OR_DIGIT = Pattern.compile("[^\\p{L}\\p{N}]+");

    public Keyword {
        tokens = List.copyOf(tokens);
    }

    public static Keyword of(String input) {
        if (input == null) {
            return new Keyword("", List.of(), false);
        }
        String text = Normalizer.normalize(input, Normalizer.Form.NFC).strip();
        boolean truncated = text.codePointCount(0, text.length()) > MAX_LENGTH;
        if (truncated) {
            text = text.substring(0, text.offsetByCodePoints(0, MAX_LENGTH)).strip();
        }
        List<String> tokens = Arrays.stream(NOT_LETTER_OR_DIGIT.split(text.toLowerCase(Locale.ROOT)))
                .filter(token -> !token.isEmpty())
                .limit(MAX_TOKENS)
                .toList();
        return new Keyword(text, tokens, truncated);
    }

    /** No letters or digits: browse all, newest first. */
    public boolean isBlank() {
        return tokens.isEmpty();
    }

    /** Tokens joined with OR, e.g. {@code "canh | chua | cá"}; bind it as a parameter. */
    public String toTsQuery() {
        return String.join(" | ", tokens);
    }
}
