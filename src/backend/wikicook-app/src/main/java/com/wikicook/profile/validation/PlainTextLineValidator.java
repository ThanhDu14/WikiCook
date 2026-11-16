package com.wikicook.profile.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PlainTextLineValidator implements ConstraintValidator<PlainTextLine, CharSequence> {

    private static final int ZERO_WIDTH_NON_JOINER = 0x200C;
    private static final int ZERO_WIDTH_JOINER = 0x200D;

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        boolean visible = false;
        for (int i = 0; i < value.length();) {
            int codePoint = Character.codePointAt(value, i);
            i += Character.charCount(codePoint);
            if (!allowed(codePoint)) {
                return false;
            }
            if (!Character.isWhitespace(codePoint) && !Character.isSpaceChar(codePoint)) {
                visible = true;
            }
        }
        return visible;
    }

    private static boolean allowed(int codePoint) {
        if (Character.isISOControl(codePoint)) {
            return false;
        }
        return switch (Character.getType(codePoint)) {
            case Character.LINE_SEPARATOR, Character.PARAGRAPH_SEPARATOR -> false;
            case Character.FORMAT -> codePoint == ZERO_WIDTH_JOINER || codePoint == ZERO_WIDTH_NON_JOINER;
            case Character.SURROGATE, Character.PRIVATE_USE -> false; // not UNASSIGNED: emoji newer than the JDK stay valid
            default -> true;
        };
    }
}
