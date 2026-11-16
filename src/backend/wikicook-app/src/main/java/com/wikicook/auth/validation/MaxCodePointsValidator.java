package com.wikicook.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.text.Normalizer;

public class MaxCodePointsValidator implements ConstraintValidator<MaxCodePoints, CharSequence> {

    private int max;

    @Override
    public void initialize(MaxCodePoints constraint) {
        this.max = constraint.value();
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // presence is @NotNull's job
        }
        String nfc = Normalizer.normalize(value, Normalizer.Form.NFC);
        return nfc.codePointCount(0, nfc.length()) <= max;
    }
}
