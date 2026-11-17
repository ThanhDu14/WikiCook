package com.wikicook.auth.validation;

/** A form with a password, its confirmation and the account email, checked as a whole by {@link ValidPassword}. */
public interface PasswordPair {

    String password();

    String confirmPassword();

    /** Null when the form has no email; the comparison with the email is then left to the service. */
    String email();

    /** The name of the password field in the request, where {@code PASSWORD_EQUALS_EMAIL} is reported. */
    default String passwordField() {
        return "password";
    }
}
