package com.wikicook.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Base64;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * BCrypt that accepts every password FR-004 allows (research R3). {@code BCrypt.hashpw} of Spring Security 6.5.11
 * throws above 72 bytes, so the password goes NFC, UTF-8, SHA-256, Base64 (44 ASCII characters, no NUL) first. The
 * stored value is still a plain 60-character BCrypt string. The password is never trimmed.
 */
public class PrehashedBCryptPasswordEncoder implements PasswordEncoder {

    public static final int DEFAULT_STRENGTH = 12;

    private final BCryptPasswordEncoder bcrypt;

    public PrehashedBCryptPasswordEncoder() {
        this(DEFAULT_STRENGTH);
    }

    public PrehashedBCryptPasswordEncoder(int strength) {
        this.bcrypt = new BCryptPasswordEncoder(strength);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(prehash(rawPassword));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return bcrypt.matches(prehash(rawPassword), encodedPassword);
    }

    private static String prehash(CharSequence rawPassword) {
        String nfc = Normalizer.normalize(rawPassword, Normalizer.Form.NFC);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(nfc.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
