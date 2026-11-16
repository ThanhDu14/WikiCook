package com.wikicook.auth.validation;

/** A form with a password, its confirmation and the account email, checked as a whole by {@link ValidPassword}. */
public interface PasswordPair {

    String password();

    String confirmPassword();

    String email();
}
