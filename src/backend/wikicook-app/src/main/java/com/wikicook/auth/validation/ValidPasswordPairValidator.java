package com.wikicook.auth.validation;

import com.wikicook.auth.service.EmailNormalizer;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * The form-level rules of {@link ValidPassword}: the password is not the email (compared normalized, so case and
 * surrounding spaces do not hide it) and the confirmation is identical, spaces included.
 */
public class ValidPasswordPairValidator implements ConstraintValidator<ValidPassword, PasswordPair> {

    @Override
    public boolean isValid(PasswordPair form, ConstraintValidatorContext context) {
        if (form == null || form.password() == null) {
            return true; // @NotNull reports it
        }
        context.disableDefaultConstraintViolation();
        boolean valid = true;
        if (form.email() != null
                && EmailNormalizer.normalize(form.password()).equals(EmailNormalizer.normalize(form.email()))) {
            context.buildConstraintViolationWithTemplate("PASSWORD_EQUALS_EMAIL").addPropertyNode("password")
                    .addConstraintViolation();
            valid = false;
        }
        if (form.confirmPassword() != null && !form.password().equals(form.confirmPassword())) {
            context.buildConstraintViolationWithTemplate("PASSWORD_MISMATCH").addPropertyNode("confirmPassword")
                    .addConstraintViolation();
            valid = false;
        }
        return valid;
    }
}
