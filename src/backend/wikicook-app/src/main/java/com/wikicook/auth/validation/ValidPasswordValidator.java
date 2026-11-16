package com.wikicook.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/** The field rules of {@link ValidPassword}: length in code points after NFC, a letter, a digit. */
public class ValidPasswordValidator implements ConstraintValidator<ValidPassword, CharSequence> {

    public static final int MIN = 8;
    public static final int MAX = 128;

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // presence is @NotNull's job
        }
        List<String> problems = problems(value);
        if (problems.isEmpty()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        problems.forEach(code -> context.buildConstraintViolationWithTemplate(code).addConstraintViolation());
        return false;
    }

    /** The codes of every broken rule, empty for a good password. */
    public static List<String> problems(CharSequence password) {
        String nfc = Normalizer.normalize(password, Normalizer.Form.NFC);
        int length = nfc.codePointCount(0, nfc.length());
        List<String> problems = new ArrayList<>();
        if (length < MIN) {
            problems.add("PASSWORD_TOO_SHORT");
        }
        if (length > MAX) {
            problems.add("PASSWORD_TOO_LONG");
        }
        if (nfc.codePoints().noneMatch(Character::isLetter)) {
            problems.add("PASSWORD_NO_LETTER");
        }
        if (nfc.codePoints().noneMatch(cp -> Character.getType(cp) == Character.DECIMAL_DIGIT_NUMBER)) {
            problems.add("PASSWORD_NO_DIGIT");
        }
        return problems;
    }
}
