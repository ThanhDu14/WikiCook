package com.wikicook.profile.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PlainTextBlockValidator implements ConstraintValidator<PlainTextBlock, CharSequence> {

    private static final int LINE_FEED = '\n';
    private static final int ZERO_WIDTH_NON_JOINER = 0x200C;
    private static final int ZERO_WIDTH_JOINER = 0x200D;

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        for (int i = 0; i < value.length();) {
            int codePoint = Character.codePointAt(value, i);
            i += Character.charCount(codePoint);
            if (!allowed(codePoint)) {
                return false;
            }
        }
        return true;
    }

    private static boolean allowed(int codePoint) {
        if (codePoint == LINE_FEED) {
            return true;
        }
        if (Character.isISOControl(codePoint)) {
            return false; // includes tab and carriage return: the DTO has already turned CRLF into LF
        }
        return switch (Character.getType(codePoint)) {
            case Character.LINE_SEPARATOR, Character.PARAGRAPH_SEPARATOR -> false;
            case Character.FORMAT -> codePoint == ZERO_WIDTH_JOINER || codePoint == ZERO_WIDTH_NON_JOINER;
            case Character.SURROGATE, Character.PRIVATE_USE -> false;
            default -> true;
        };
    }
}
